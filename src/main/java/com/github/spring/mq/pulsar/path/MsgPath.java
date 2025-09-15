package com.github.spring.mq.pulsar.path;

import com.github.spring.mq.pulsar.domain.MsgContext;

/**
 * 消息路径构建工具
 *
 * @author avinzhang
 */
public class MsgPath {

    public static void setPath(String path) {
        MsgContext.setBusinessPath(path);
    }
}