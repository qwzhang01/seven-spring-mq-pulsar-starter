/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.spring.mq.pulsar.domain;

import java.time.LocalDateTime;

/**
 * Message context holder
 *
 * <p>This class provides a thread-local context for storing message-related information
 * during message processing. It allows different components to access contextual data
 * without explicitly passing it through method parameters.
 *
 * <p>The context includes:
 * <ul>
 *   <li>Processing time</li>
 *   <li>Application name</li>
 *   <li>Corporation key</li>
 *   <li>Request ID for tracing</li>
 *   <li>Message routing information</li>
 * </ul>
 *
 * <p><strong>Important:</strong> Always call {@link #remove()} after message processing
 * to prevent memory leaks in thread pools.
 *
 * @author avinzhang
 * @since 1.0.0
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

    public static boolean isMultiRoute() {
        Context context = HOLDER.get();
        if (context == null) {
            return false;
        }
        return context.isMultiRoute();
    }

    public static void setMultiRoute(boolean multiRoute) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setMultiRoute(multiRoute);
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

    public static String getTraceId() {
        Context context = HOLDER.get();
        if (context == null) {
            return null;
        }
        return context.getTraceId();
    }

    public static void setTraceId(String traceId) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setTraceId(traceId);
    }

    public static String getSpanId() {
        Context context = HOLDER.get();
        if (context == null) {
            return null;
        }
        return context.getSpanId();
    }

    public static void setSpanId(String spanId) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setSpanId(spanId);
    }

    public static boolean isMultiTenant() {
        Context context = HOLDER.get();
        if (context == null) {
            return false;
        }
        return context.isMultiTenant();
    }

    public static void setMultiTenant(boolean isMultiTenant) {
        Context context = HOLDER.get();
        if (context == null) {
            context = new Context();
            HOLDER.set(context);
        }
        context.setMultiTenant(isMultiTenant);
    }

    public static void remove() {
        HOLDER.remove();
    }

    private static class Context {
        private boolean isMultiTenant;
        private String corpKey;
        private String appName;
        private LocalDateTime time;
        /**
         * Whether it is multi-route
         */
        private boolean isMultiRoute;
        private String msgRoute;

        private String traceId;
        private String spanId;

        public boolean isMultiRoute() {
            return isMultiRoute;
        }

        public void setMultiRoute(boolean multiRoute) {
            isMultiRoute = multiRoute;
        }

        public String getSpanId() {
            return spanId;
        }

        public void setSpanId(String spanId) {
            this.spanId = spanId;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public boolean isMultiTenant() {
            return isMultiTenant;
        }

        public void setMultiTenant(boolean multiTenant) {
            isMultiTenant = multiTenant;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
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