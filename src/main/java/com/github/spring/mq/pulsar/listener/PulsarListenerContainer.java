package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.domain.ListenerType;
import com.github.spring.mq.pulsar.exception.JacksonException;
import com.github.spring.mq.pulsar.exception.PulsarConsumeReceiveException;
import com.github.spring.mq.pulsar.exception.PulsarConsumerLatterException;
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

    private final ListenerType listenerType;

    private volatile boolean running = false;
    private Map<String, String> businessMap = null;


    public PulsarListenerContainer(Consumer<byte[]> consumer,
                                   Object bean,
                                   String businessPath,
                                   Method method,
                                   String businessKey,
                                   String dataKey,
                                   boolean autoAck,
                                   Class<?> messageType,
                                   PulsarTemplate pulsarTemplate,
                                   ListenerType listenerType) {
        this.consumer = consumer;
        this.bean = bean;
        this.handlerMap.put(businessPath, new Handler(businessKey, dataKey, method, messageType));
        this.autoAck = autoAck;
        this.pulsarTemplate = pulsarTemplate;
        this.listenerType = listenerType;
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
     * 处理消息
     */
    private void processMessage(CompletableFuture<Message<byte[]>> receiveAsync) throws ExecutionException, InterruptedException {
        Message<byte[]> message = receiveAsync.get();

        long publishTime = message.getPublishTime();
        long eventTime = message.getEventTime();

        logger.info("Received pulsar message publishTime: {}, eventTime: {}", publishTime, eventTime);

        processMessage(this.consumer, message);
    }

    /**
     * 处理消息
     */
    public void processMessage(Consumer<byte[]> consumer, Message<byte[]> message) {

        Object deserializedMessage = null;
        Exception processException = null;

        try {
            // 执行接收前拦截器
            if (!pulsarTemplate.applyBeforeReceiveInterceptors(message)) {
                logger.debug("Message filtered by beforeReceive interceptor");
                if (autoAck) {
                    ignore(consumer, message);
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
            if (autoAck && consumer.isConnected()) {
                consumer.acknowledge(message);
            }
        } catch (UnsupportedOperationException | JacksonException e) {
            logger.error("Error processing message", e);
            ignore(consumer, message);
        } catch (PulsarConsumerLatterException e) {
            logger.error("Error processing message", e);
            reconsume(consumer, message);
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

    /**
     * 忽略格式错误，无法解析的消息
     *
     * @param consumer
     * @param message
     */
    private void ignore(Consumer<byte[]> consumer, Message<byte[]> message) {
        if (consumer.isConnected()) {
            try {
                consumer.acknowledge(message);
            } catch (PulsarClientException e) {
                throw new PulsarConsumeReceiveException("消费消息异常", e);
            }
        }
    }

    private void reconsume(Consumer<byte[]> consumer, Message<byte[]> msg) {
        logger.debug("Consumer re-consume message: {}", new String(msg.getData()));
        try {
            consumer.reconsumeLater(msg, 60, TimeUnit.SECONDS);
        } catch (PulsarClientException ex) {
            consumer.negativeAcknowledge(msg);
            logger.error("消息重试异常", ex);
        }
    }

    public void addMethod(PulsarListener annotation,
                          Method method) {
        this.handlerMap.put(annotation.businessPath(),
                new Handler(annotation.businessKey(),
                        annotation.dataKey(),
                        method,
                        annotation.messageType()));
    }

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
}