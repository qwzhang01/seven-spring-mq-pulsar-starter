package com.github.spring.mq.pulsar.route;

import com.github.spring.mq.pulsar.domain.MsgContext;

/**
 * 消息路径构建工具
 *
 * @author avinzhang
 */
public class MsgRoute {

    public static void setMsgRoute(String route) {
        MsgContext.setMsgRoute(route);
    }
}