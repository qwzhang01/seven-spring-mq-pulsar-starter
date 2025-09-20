package com.github.spring.mq.pulsar.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spring.mq.pulsar.domain.MsgContext;
import com.github.spring.mq.pulsar.domain.MsgDomain;
import com.github.spring.mq.pulsar.exception.JacksonException;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.UUID;

/**
 * Default multi-tenant interceptor
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

    private final ObjectMapper objectMapper;

    protected DefaultMultipleTenantInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object beforeSend(String topic, Object message) {
        buildSendContext();
        try {
            MsgDomain<Object> domain = new MsgDomain<>();
            domain.setCorpKey(MsgContext.getCorpKey());
            domain.setAppName(MsgContext.getAppName());
            domain.setRequestId(MsgContext.getRequestId());
            domain.setMsgId(UUID.randomUUID().toString().replaceAll("-", "").toLowerCase(Locale.ROOT));
            domain.setTime(MsgContext.getTime());
            domain.setData(message);
            domain.setMsgRoute(MsgContext.getMsgRoute());
            return domain;
        } finally {
            MsgContext.remove();
        }
    }


    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        // ignore
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        MsgDomain<?> domain = deserialize(message.getData(), MsgDomain.class);
        MsgContext.setCorpKey(domain.getCorpKey());
        MsgContext.setAppName(domain.getAppName());
        MsgContext.setRequestId(domain.getRequestId());
        MsgContext.setTime(domain.getTime());
        MsgContext.setMsgRoute(domain.getMsgRoute());
        return buildReceiveContext(domain.getCorpKey());
    }

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        // ignore
    }

    @Override
    public int getOrder() {
        // Highest priority to ensure proper context setup
        return 10;
    }

    /**
     * Deserialize object from byte array
     */
    private <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            if (clazz == String.class) {
                return clazz.cast(data);
            } else if (clazz == byte[].class) {
                return clazz.cast(data);
            } else {
                return objectMapper.readValue(data, clazz);
            }
        } catch (Exception e) {
            throw new JacksonException("Failed to deserialize object", e);
        }
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
