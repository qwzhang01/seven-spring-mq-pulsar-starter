package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.domain.ListenerType;
import com.github.spring.mq.pulsar.tracing.ConsumeExceptionHandlerContainer;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.github.spring.mq.pulsar.tracing.PulsarMessageHeadersPropagator.extractMsgRoute;

/**
 * Pulsar listener container
 *
 * <p>This class manages the lifecycle of Pulsar message consumers and handles
 * message processing for methods annotated with @PulsarListener. It provides:
 * <ul>
 *   <li>Consumer lifecycle management (start/stop)</li>
 *   <li>Message routing and processing</li>
 *   <li>Error handling and message acknowledgment</li>
 *   <li>Support for multiple message handlers per topic</li>
 *   <li>Interceptor chain execution</li>
 * </ul>
 *
 * <p>The container supports different listener types:
 * <ul>
 *   <li>LOOP: Polling-based message consumption</li>
 *   <li>EVENT: Event-driven message consumption</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarListenerContainer {

    private static final Logger logger = LoggerFactory.getLogger(PulsarListenerContainer.class);

    private final Consumer<byte[]> consumer;
    /**
     * Handler container
     * key: message route
     * value: message handler
     */
    private final ConcurrentHashMap<String, Handler> handlerMap = new ConcurrentHashMap<>();

    private final boolean autoAck;

    private final PulsarTemplate pulsarTemplate;

    private final ExecutorService executor;

    private final ListenerType listenerType;
    private final ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer;
    private volatile boolean running = false;
    /**
     * key: message route
     * value: routeKey
     */
    private Map<String, String> routeToKey = null;


    public PulsarListenerContainer(Consumer<byte[]> consumer,
                                   Object bean,
                                   String route,
                                   Method method,
                                   String routeKey,
                                   String dataKey,
                                   boolean autoAck,
                                   Class<?> messageType,
                                   PulsarTemplate pulsarTemplate,
                                   ListenerType listenerType,
                                   ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer) {
        this.consumer = consumer;
        this.handlerMap.put(route, new Handler(routeKey, dataKey, bean, method, messageType));
        this.autoAck = autoAck;
        this.pulsarTemplate = pulsarTemplate;
        this.listenerType = listenerType;
        this.consumeExceptionHandlerContainer = consumeExceptionHandlerContainer;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "pulsar-listener-" + method.getName());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Start the listener
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::listen);
        logger.info("Started Pulsar listener for method: {}", handlerMap.keySet());
    }

    /**
     * Stop the listener
     */
    public void stop() {
        running = false;
        this.executor.shutdown();
        try {
            consumer.close();
        } catch (PulsarClientException e) {
            logger.error("Error closing consumer", e);
        }
        logger.info("Stopped Pulsar listener for method: {}", handlerMap.keySet());
    }

    /**
     * Listen for messages
     */
    private void listen() {
        if (ListenerType.EVENT.equals(listenerType)) {
            return;
        }
        while (running) {
            try {
                CompletableFuture<Message<byte[]>> receiveAsync = consumer.receiveAsync();
                processMessage(receiveAsync);
            } catch (Exception e) {
                if (running) {
                    logger.error("Error receiving message", e);
                }
            }
        }
    }

    /**
     * Process message
     */
    private void processMessage(CompletableFuture<Message<byte[]>> receiveAsync) throws ExecutionException, InterruptedException {
        Message<byte[]> message = receiveAsync.get();

        long publishTime = message.getPublishTime();
        long eventTime = message.getEventTime();

        logger.info("Received pulsar message publishTime: {}, eventTime: {}", publishTime, eventTime);

        processMessage(this.consumer, message);
    }

    /**
     * Process message with consumer and message
     */
    public void processMessage(Consumer<byte[]> consumer, Message<byte[]> message) {

        Object deserializedMessage = null;
        Exception processException = null;

        try {
            // Execute before-receive interceptors
            if (!pulsarTemplate.applyBeforeReceiveInterceptors(message)) {
                logger.debug("Message filtered by beforeReceive interceptor");
                if (autoAck) {
                    consumer.acknowledge(message);
                }
                return;
            }

            String msgRoute = extractMsgRoute(message.getProperties());
            if (msgRoute == null) {
                msgRoute = pulsarTemplate.deserializeMsgRoute(message.getData(), getRouteToKey());
            }
            Handler handler = this.handlerMap.get(msgRoute);
            if (handler == null) {
                throw new UnsupportedOperationException("Business type not supported for route: " + msgRoute + ", no corresponding consumer, message content: " + new String(message.getData()));
            }
            Method method = handler.method;
            if (method == null) {
                throw new UnsupportedOperationException("Business type not supported for route: " + msgRoute + ", no corresponding consumer, message content: " + new String(message.getData()));
            }
            String dataKey = handler.dataKey;
            deserializedMessage = pulsarTemplate.deserialize(message.getData(), dataKey, handler.messageType);

            // Invoke listener method
            ReflectionUtils.makeAccessible(method);

            // Decide what to pass based on method parameters
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args;

            if (parameterTypes.length == 1) {
                args = new Object[]{deserializedMessage};
            } else if (parameterTypes.length == 2) {
                args = new Object[]{deserializedMessage, message};
            } else {
                args = new Object[]{};
            }

            method.invoke(handler.bean, args);

            // Auto-acknowledge message
            if (autoAck && consumer.isConnected()) {
                consumer.acknowledge(message);
            }
        } catch (Exception e) {
            processException = e;
            logger.error("Error processing message", e);
            consumeExceptionHandlerContainer.handle(consumer, message, processException);
        } finally {
            // Execute after-receive interceptors
            pulsarTemplate.applyAfterReceiveInterceptors(message, deserializedMessage, processException);
        }
    }

    public void addMethod(Object bean, Method method,
                          PulsarListener annotation) {
        this.handlerMap.put(annotation.msgRoute(),
                new Handler(annotation.routeKey(),
                        annotation.dataKey(),
                        bean, method,
                        annotation.messageType()));
    }

    /**
     * Get route key based on route
     */
    private Map<String, String> getRouteToKey() {
        if (routeToKey != null) {
            return routeToKey;
        }
        this.routeToKey = handlerMap.entrySet()
                .stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().msgRouteKey));
        return routeToKey;
    }

    /**
     * Handler information
     *
     * @param msgRouteKey Message business key mapping to msgRouteKey field name
     * @param dataKey     Data key mapping to data field name
     * @param method      Method mapping key to method
     * @param messageType Message type
     */
    private record Handler(String msgRouteKey, String dataKey,
                           Object bean,
                           Method method,
                           Class<?> messageType) {
    }
}