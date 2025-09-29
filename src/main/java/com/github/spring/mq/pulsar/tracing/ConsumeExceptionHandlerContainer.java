package com.github.spring.mq.pulsar.tracing;

import com.github.spring.mq.pulsar.annotation.ConsumerExceptionHandler;
import com.github.spring.mq.pulsar.annotation.ConsumerExceptionResponse;
import com.github.spring.mq.pulsar.domain.ConsumerExceptionResponseAction;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Container for message consumption exception handlers
 * 
 * <p>This container manages exception handlers for message consumption failures.
 * It provides the following functionality:
 * <ul>
 *   <li>Scans annotations to create handler containers</li>
 *   <li>Retrieves appropriate handlers at runtime</li>
 *   <li>Executes exception handling logic</li>
 *   <li>Manages response actions (ACK, NACK, RECONSUME_LATER)</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class ConsumeExceptionHandlerContainer {
    private static final Logger logger = LoggerFactory.getLogger(ConsumeExceptionHandlerContainer.class);
    private final ConcurrentHashMap<Class<? extends Throwable>, Handler> handlerMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Class<? extends Throwable>, Handler> handlerMapCache = new ConcurrentHashMap<>();

    public void create(Object bean, Method method, ConsumerExceptionHandler annotation) {
        Class<? extends Throwable>[] exceptionList = annotation.value();

        ConsumerExceptionResponseAction action = null;
        ConsumerExceptionResponse returnValue = method.getAnnotation(ConsumerExceptionResponse.class);
        if (returnValue != null) {
            action = returnValue.value();
        }
        if (action == null) {
            action = ConsumerExceptionResponseAction.NACK;
        }
        if (exceptionList == null || exceptionList.length == 0) {
            if (!handlerMap.containsKey(Exception.class)) {
                handlerMap.put(Exception.class, new Handler(bean, method, action));
            }
        } else {
            for (Class<? extends Throwable> exception : exceptionList) {
                if (!handlerMap.containsKey(exception)) {
                    handlerMap.put(exception, new Handler(bean, method, action));
                }
            }
        }
    }

    public void handle(Consumer<byte[]> consumer, Message<byte[]> message, Throwable throwable) {
        Handler handler = getHandler(throwable);

        Method method = handler.method;

        ReflectionUtils.makeAccessible(method);
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args;
        if (parameterTypes.length == 1) {
            args = new Object[]{throwable};
        } else if (parameterTypes.length == 2) {
            args = new Object[]{throwable, message};
        } else if (parameterTypes.length == 3) {
            args = new Object[]{throwable, consumer, message};
        } else {
            args = new Object[]{};
        }

        try {
            method.invoke(handler.bean, args);
        } catch (Exception e) {
            logger.error("handle exception error", e);
        }

        if (ConsumerExceptionResponseAction.ACK.equals(handler.action)) {
            ignore(consumer, message);
        } else if (ConsumerExceptionResponseAction.NACK.equals(handler.action)) {
            negative(consumer, message);
        } else if (ConsumerExceptionResponseAction.RECONSUME_LATER.equals(handler.action)) {
            reconsume(consumer, message);
        } else {
            negative(consumer, message);
        }
    }

    private void negative(Consumer<byte[]> consumer, Message<byte[]> message) {
        logger.debug("Consumer re-consume message: {}", new String(message.getData()));
        try {
            consumer.negativeAcknowledge(message);
        } catch (Exception ackException) {
            logger.error("Error negative acknowledging message", ackException);
        }
    }

    private void reconsume(Consumer<byte[]> consumer, Message<byte[]> msg) {
        logger.debug("Consumer re-consume message: {}", new String(msg.getData()));
        try {
            consumer.reconsumeLater(msg, 60, TimeUnit.SECONDS);
        } catch (PulsarClientException ex) {
            consumer.negativeAcknowledge(msg);
            logger.error("Message retry exception", ex);
        }
    }

    /**
     * Ignore messages with format errors that cannot be parsed
     */
    private void ignore(Consumer<byte[]> consumer, Message<byte[]> message) {
        if (consumer.isConnected()) {
            try {
                consumer.acknowledge(message);
            } catch (PulsarClientException e) {
                consumer.negativeAcknowledge(message);
                logger.error("Message retry exception", e);
            }
        }
    }

    private Handler getHandler(Throwable throwable) {
        Handler handler = handlerMapCache.get(throwable.getClass());
        if (handler == null) {
            handler = handlerMap.get(throwable.getClass());
            if (handler == null) {
                for (Class<? extends Throwable> exception : handlerMap.keySet()) {
                    if (exception.isAssignableFrom(throwable.getClass())) {
                        handler = handlerMap.get(exception);
                        break;
                    }
                }
            }
            if (handler == null) {
                handler = handlerMap.get(Exception.class);
            }
            handlerMapCache.put(throwable.getClass(), handler);
        }

        return handler;
    }

    /**
     * Exception handler record
     * 
     * @param bean   The bean instance containing the handler method
     * @param method The handler method to invoke
     * @param action The response action to take after handling the exception
     */
    private record Handler(Object bean, Method method,
                           ConsumerExceptionResponseAction action) {
    }
}