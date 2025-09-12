package com.github.spring.mq.pulsar.interceptor;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志记录拦截器示例
 * 演示如何实现自定义的Pulsar消息拦截器
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class LoggingPulsarMessageInterceptor implements PulsarMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingPulsarMessageInterceptor.class);

    @Override
    public Object beforeSend(String topic, Object message) {
        logger.info("准备发送消息到主题: {}, 消息内容: {}", topic, message);
        // 可以在这里对消息进行预处理
        // 例如：添加时间戳、加密、验证等

        // 返回处理后的消息
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        if (exception == null) {
            logger.info("消息发送成功 - 主题: {}, 消息ID: {}", topic, messageId);
        } else {
            logger.error("消息发送失败 - 主题: {}, 错误: {}", topic, exception.getMessage(), exception);
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        logger.info("准备接收消息 - 主题: {}, 消息ID: {}, 发布时间: {}",
                message.getTopicName(), message.getMessageId(), message.getPublishTime());

        // 可以在这里进行消息过滤
        // 返回false将跳过该消息的处理

        // 继续处理消息
        return true;
    }

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        if (exception == null) {
            logger.info("消息处理成功 - 主题: {}, 消息ID: {}",
                    message.getTopicName(), message.getMessageId());
        } else {
            logger.error("消息处理失败 - 主题: {}, 消息ID: {}, 错误: {}",
                    message.getTopicName(), message.getMessageId(), exception.getMessage(), exception);
        }
    }

    @Override
    public int getOrder() {
        // 设置拦截器优先级，数值越小优先级越高
        return 100;
    }
}