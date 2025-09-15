package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
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
    /**
     * 映射key 到 method
     */
    private final ConcurrentHashMap<String, Method> methodMap = new ConcurrentHashMap<>();
    /**
     * 映射 businessKey 的字段名字
     */
    private final ConcurrentHashMap<String, String> businessMap = new ConcurrentHashMap<>();
    /**
     * 映射 data 数据 的字段名字
     */
    private final ConcurrentHashMap<String, String> dataMap = new ConcurrentHashMap<>();

    private final boolean autoAck;
    private final Class<?> messageType;
    private final PulsarTemplate pulsarTemplate;
    private final ExecutorService executor;
    private volatile boolean running = false;

    public PulsarListenerContainer(Consumer<byte[]> consumer, Object bean,
                                   String businessPath, Method method, String businessKey,String dataKey,
                                   boolean autoAck, Class<?> messageType, PulsarTemplate pulsarTemplate) {
        this.consumer = consumer;
        this.bean = bean;
        this.methodMap.put(businessPath, method);
        this.businessMap.put(businessPath, businessKey);
        this.dataMap.put(businessPath, dataKey);
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
        logger.info("Started Pulsar listener for method: {}", methodMap.keySet());
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
        logger.info("Stopped Pulsar listener for method: {}", methodMap.keySet());
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

            String business = pulsarTemplate.deserializeBusinessType(message.getData(), businessMap, messageType);
            Method method = this.methodMap.get(business);
            if (method == null) {
                throw new UnsupportedOperationException(business + "业务类型不支持，没有对应的消费者");
            }
            String dataKey = this.dataMap.get(business);
            deserializedMessage = pulsarTemplate.deserialize(message.getData(), dataKey, messageType);

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

    public void addMethod(String businessPath, Method method, String businessKey, String dataKey) {
        this.methodMap.put(businessPath, method);
        this.businessMap.put(businessPath, businessKey);
        this.dataMap.put(businessPath, dataKey);
    }
}