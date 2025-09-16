package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Pulsar 监听器容器
 *
 * @author avinzhang
 */
public class PulsarListenerContainer {

    private static final Logger logger = LoggerFactory.getLogger(PulsarListenerContainer.class);

    private final Consumer<byte[]> consumer;
    private final Object bean;

    private final ConcurrentHashMap<String, Handler> handlerMap = new ConcurrentHashMap<>();

    private final boolean autoAck;

    private final PulsarTemplate pulsarTemplate;

    private final ExecutorService executor;

    private volatile boolean running = false;

    public PulsarListenerContainer(Consumer<byte[]> consumer, Object bean,
                                   String businessPath, Method method, String businessKey, String dataKey,
                                   boolean autoAck, Class<?> messageType, PulsarTemplate pulsarTemplate) {
        this.consumer = consumer;
        this.bean = bean;
        this.handlerMap.put(businessPath, new Handler(businessKey, dataKey, method, messageType));
        this.autoAck = autoAck;
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
        logger.info("Started Pulsar listener for method: {}", handlerMap.keySet());
    }

    /**
     * 停止监听器
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

            String msgRoute = pulsarTemplate.deserializeMsgRoute(message.getData(), getBusinessMap());
            Handler handler = this.handlerMap.get(msgRoute);
            if (handler == null) {
                throw new UnsupportedOperationException(msgRoute + "业务类型不支持，没有对应的消费者，消息内容：" + new String(message.getData()));
            }
            Method method = handler.method;
            if (method == null) {
                throw new UnsupportedOperationException(msgRoute + "业务类型不支持，没有对应的消费者，消息内容：" + new String(message.getData()));
            }
            String dataKey = handler.dataKey;
            deserializedMessage = pulsarTemplate.deserialize(message.getData(), dataKey, handler.messageType);

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

    public void addMethod(String businessPath,
                          Method method,
                          String businessKey,
                          String dataKey,
                          Class<?> messageType) {
        this.handlerMap.put(businessPath, new Handler(businessKey, dataKey, method, messageType));
    }

    /**
     * 处理器信息
     *
     * @param msgRouteKey 消息业务键 映射 msgRouteKey 的字段名字
     * @param dataKey     数据键 映射 data 数据 的字段名字
     * @param method      方法 映射key 到 method
     * @param messageType 消息类型
     */
    private record Handler(String msgRouteKey, String dataKey, Method method, Class<?> messageType) {
    }

    private Map<String, String> businessMap = null;

    private Map<String, String> getBusinessMap() {
        if (businessMap != null) {
            return businessMap;
        }
        this.businessMap = handlerMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().msgRouteKey
                ));

        return businessMap;
    }
}