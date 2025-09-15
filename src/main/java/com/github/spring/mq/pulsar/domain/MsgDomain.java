package com.github.spring.mq.pulsar.domain;

import java.time.LocalDateTime;

/**
 * 默认消息体
 *
 * @author avinzhang
 */
public class MsgDomain<T> {
    /**
     * 多租户的租户key
     */
    private String corpKey;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 请求id
     * 打印日志的traceId
     */
    private String requestId;
    /**
     * 消息体id
     * 做消息幂等的id
     */
    private String msgId;
    /**
     * 消息业务路径
     */
    private String businessPath;
    /**
     * 消息产生时间
     */
    private LocalDateTime time;
    /**
     * 消息体
     */
    private T data;

    public String getCorpKey() {
        return corpKey;
    }

    public void setCorpKey(String corpKey) {
        this.corpKey = corpKey;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getBusinessPath() {
        return businessPath;
    }

    public void setBusinessPath(String businessPath) {
        this.businessPath = businessPath;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
