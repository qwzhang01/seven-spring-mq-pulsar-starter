package com.github.spring.mq.pulsar.domain;

/**
 * 监听器类型
 *
 * @author avinzhang
 */
public enum ListenerType {
    /**
     * 循环监听
     */
    LOOP,

    /**
     * 事件监听
     */
    EVENT
}
