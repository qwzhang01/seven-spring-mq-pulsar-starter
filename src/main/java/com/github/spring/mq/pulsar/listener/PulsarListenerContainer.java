package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pulsar 监听器容器
 *
 * @author avinzhang
 */
public class PulsarListenerContainer {

    private static final Logger logger = LoggerFactory.getLogger(PulsarListenerContainer.class);

    private final Consumer<byte[]> consumer;
    private final Object bean;
    private final Method method;
    private final boolean autoAck;
    private final Class<?> messageType;
    private final PulsarTemplate pulsarTemplate;
    private final ExecutorService executor;
    private volatile boolean running = false;

    public PulsarListenerContainer(Consumer<byte[]> consumer, Object bean, Method method,
                                   boolean autoAck, Class<?> messageType, PulsarTemplate pulsarTemplate) {
        this.consumer = consumer;
        this.bean = bean;
        this.method = method;
        this.autoAck = autoAck;
        this.messageType = messageType;
        this.pulsarTemplate = pulsarTemplate;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "pulsar-listener-" + method.getName());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 启动监听器
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::listen);
        logger.info("Started Pulsar listener for method: {}", method.getName());
    }

    /**
     * 停止监听器
     */
    public void stop() {
        running = false;
        executor.shutdown();
        try {
            consumer.close();
        } catch (PulsarClientException e) {
            logger.error("Error closing consumer", e);
        }
        logger.info("Stopped Pulsar listener for method: {}", method.getName());
    }

    /**
     * 监听消息
     */
    private void listen() {
        while (running) {
            try {
                Message<byte[]> message = consumer.receive();
                processMessage(message);
            } catch (Exception e) {
                if (running) {
                    logger.error("Error receiving message", e);
                }
            }
        }
    }

    /**
     * 处理消息
     */
    private void processMessage(Message<byte[]> message) {
        Object deserializedMessage = null;
        Exception processException = null;

        try {
            // 执行接收前拦截器
            if (!pulsarTemplate.applyBeforeReceiveInterceptors(message)) {
                logger.debug("Message filtered by beforeReceive interceptor");
                if (autoAck) {
                    consumer.acknowledge(message);
                }
                return;
            }

            deserializedMessage = pulsarTemplate.deserialize(message.getData(), messageType);

            // 调用监听器方法
            ReflectionUtils.makeAccessible(method);

            // 根据方法参数决定传递什么
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args;

            if (parameterTypes.length == 1) {
                args = new Object[]{deserializedMessage};
            } else if (parameterTypes.length == 2) {
                args = new Object[]{deserializedMessage, message};
            } else {
                args = new Object[]{};
            }

            method.invoke(bean, args);

            // 自动确认消息
            if (autoAck) {
                consumer.acknowledge(message);
            }

        } catch (Exception e) {
            processException = e;
            logger.error("Error processing message", e);
            try {
                consumer.negativeAcknowledge(message);
            } catch (Exception ackException) {
                logger.error("Error negative acknowledging message", ackException);
            }
        } finally {
            // 执行接收后拦截器
            pulsarTemplate.applyAfterReceiveInterceptors(message, deserializedMessage, processException);
        }
    }
}