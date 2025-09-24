package com.github.spring.mq.pulsar.interceptor;

import com.github.spring.mq.pulsar.domain.MsgContext;
import com.github.spring.mq.pulsar.domain.MsgDomain;
import com.github.spring.mq.pulsar.domain.MsgMetaKey;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Default multi-tenant interceptor
 * <p>
 * 多租户等元信息在消息的 property 里面
 * <p>
 * 发送消息的时候，将租住信息放置消息的 property 里面，具体实现在 PulsarTemplate
 * <p>
 * 接受消息的时候，解析元信息，将租住信息放置当前线程共享变量
 *
 * <p>This abstract interceptor provides multi-tenancy support for Pulsar messages.
 * It automatically wraps outgoing messages with tenant context information and
 * extracts tenant context from incoming messages.
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic message wrapping with {@link MsgDomain}</li>
 *   <li>Tenant context propagation</li>
 *   <li>Request tracing support</li>
 *   <li>Thread-local context management</li>
 * </ul>
 *
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #buildSendContext()} - Set up context before sending</li>
 *   <li>{@link #buildReceiveContext(String)} - Handle tenant switching on receive</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public abstract class DefaultMultipleTenantInterceptor implements PulsarMessageInterceptor {

    private final static Logger logger = LoggerFactory.getLogger(DefaultMultipleTenantInterceptor.class);
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public Object beforeSend(String topic, Object message) {
        buildSendContext();
        MsgContext.setMultiTenant(true);
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        MsgContext.remove();
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        Map<String, String> properties = message.getProperties();
        String corpKey = properties.get(MsgMetaKey.CORP.getCode());
        String appName = properties.get(MsgMetaKey.APP.getCode());
        String time = properties.get(MsgMetaKey.TIME.getCode());

        MsgContext.setCorpKey(corpKey);
        MsgContext.setAppName(appName);
        MsgContext.setTime(LocalDateTime.parse(time, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));

        return buildReceiveContext(corpKey);
    }

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        MsgContext.remove();
    }

    @Override
    public int getOrder() {
        // Highest priority to ensure proper context setup
        return 10;
    }

    /**
     * Build request context before sending message
     *
     * <p>Implementations should set up the necessary context information
     * in {@link MsgContext} before message sending.
     */
    public abstract void buildSendContext();

    /**
     * Handle multi-tenant context switching after receiving message
     *
     * <p>Implementations should handle tenant switching based on the
     * corporation key extracted from the message.
     *
     * @param corpKey Corporation key for tenant identification
     * @return true if context switching is successful, false otherwise
     */
    public abstract boolean buildReceiveContext(String corpKey);
}
