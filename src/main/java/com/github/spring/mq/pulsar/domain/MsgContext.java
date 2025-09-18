package com.github.spring.mq.pulsar.domain;

import java.time.LocalDateTime;

/**
 * 消息体上下文
 *
 * @author avinzhang
 */
public class MsgContext {
    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    public static LocalDateTime getTime() {
        Context context = HOLDER.get();
        if (context == null) {
            return null;
        }
        return context.getTime();
    }

    public static void setTime(LocalDateTime time) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setTime(time);
    }

    public static String getAppName() {
        Context context = HOLDER.get();
        if (context == null) {
            return null;
        }
        return context.getAppName();
    }

    public static void setAppName(String appName) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setAppName(appName);
    }

    public static String getCorpKey() {
        Context context = HOLDER.get();
        if (context == null) {
            return null;
        }
        return context.getCorpKey();
    }

    public static void setCorpKey(String corpKey) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setCorpKey(corpKey);
    }

    public static String getRequestId() {
        Context context = HOLDER.get();
        if (context == null) {
            return null;
        }
        return context.getRequestId();
    }

    public static void setRequestId(String requestId) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setRequestId(requestId);
    }

    public static String getMsgRoute() {
        Context context = HOLDER.get();
        if (context == null) {
            return null;
        }
        return context.getMsgRoute();
    }

    public static void setMsgRoute(String businessPath) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setMsgRoute(businessPath);
    }

    public static void remove() {
        HOLDER.remove();
    }

    private static class Context {
        private String corpKey;
        private String appName;
        private String requestId;
        private LocalDateTime time;
        private String msgRoute;

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

        public LocalDateTime getTime() {
            return time;
        }

        public void setTime(LocalDateTime time) {
            this.time = time;
        }

        public String getCorpKey() {
            return corpKey;
        }

        public void setCorpKey(String corpKey) {
            this.corpKey = corpKey;
        }

        public String getMsgRoute() {
            return msgRoute;
        }

        public void setMsgRoute(String msgRoute) {
            this.msgRoute = msgRoute;
        }
    }
}
