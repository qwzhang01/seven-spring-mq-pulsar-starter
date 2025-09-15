package com.github.spring.mq.pulsar.interceptor;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.domain.MsgContext;
import com.github.spring.mq.pulsar.domain.MsgDomain;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.UUID;

/**
 * 多租户拦截器
 *
 * @author avinzhang
 */
public abstract class DefaultMultipleTenantInterceptor implements PulsarMessageInterceptor {

    private final static Logger logger = LoggerFactory.getLogger(DefaultMultipleTenantInterceptor.class);

    private PulsarTemplate pulsarTemplate;

    public void setPulsarTemplate(PulsarTemplate pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    @Override
    public Object beforeSend(String topic, Object message) {
        MsgDomain<Object> domain = new MsgDomain<>();
        domain.setCorpKey(MsgContext.getCorpKey());
        domain.setAppName(MsgContext.getAppName());
        domain.setRequestId(MsgContext.getRequestId());
        domain.setMsgId(UUID.randomUUID().toString().replaceAll("-", "").toLowerCase(Locale.ROOT));
        domain.setTime(MsgContext.getTime());
        domain.setData(message);
        domain.setBusinessPath(MsgContext.getBusinessPath());
        return domain;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        MsgDomain<?> domain = pulsarTemplate.deserialize(message, MsgDomain.class);
        handleMultiTenant(domain.getCorpKey());
    }

    @Override
    public abstract boolean beforeReceive(Message<?> message);

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        // ignore
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保能准确测量时间
        return 10;
    }

    /**
     * 处理多租户逻辑
     *
     * @param corpKey
     */
    public abstract void handleMultiTenant(String corpKey);
}
