# Pulsar 消息拦截器使用指南

## 概述

Pulsar消息拦截器提供了在消息发送和接收过程中进行拦截处理的能力。通过实现`PulsarMessageInterceptor`接口，您可以在消息的生命周期中插入自定义逻辑。

## 拦截器接口

```java
public interface PulsarMessageInterceptor {

    // 发送消息前拦截
    default Object beforeSend(String topic, Object message) {
        return message;
    }

    // 发送消息后拦截
    default void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        // 默认空实现
    }

    // 接收消息前拦截
    default boolean beforeReceive(Message<?> message) {
        return true;
    }

    // 接收消息后拦截
    default void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        // 默认空实现
    }

    // 获取拦截器优先级
    default int getOrder() {
        return 0;
    }
}
```

## 拦截器执行时机

### 发送消息时的拦截点

1. **beforeSend**: 在消息序列化和发送到Pulsar之前执行
    - 可以修改消息内容
    - 返回`null`将阻止消息发送
    - 可以进行消息验证、加密、添加元数据等操作

2. **afterSend**: 在消息发送完成后执行（无论成功还是失败）
    - 可以记录发送日志
    - 可以进行发送后的清理工作
    - 可以处理发送异常

### 接收消息时的拦截点

1. **beforeReceive**: 在消息反序列化和传递给业务逻辑之前执行
    - 返回`false`将跳过该消息的处理
    - 可以进行消息过滤、解密、验证等操作

2. **afterReceive**: 在消息处理完成后执行（无论成功还是失败）
    - 可以记录处理日志
    - 可以进行处理后的清理工作
    - 可以处理业务异常

## 使用方法

### 1. 实现自定义拦截器

```java

@Component
public class MyCustomInterceptor implements PulsarMessageInterceptor {

    @Override
    public Object beforeSend(String topic, Object message) {
        // 发送前处理逻辑
        System.out.println("准备发送消息: " + message);
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        // 发送后处理逻辑
        if (exception == null) {
            System.out.println("消息发送成功: " + messageId);
        } else {
            System.err.println("消息发送失败: " + exception.getMessage());
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        // 接收前处理逻辑
        System.out.println("准备接收消息: " + message.getMessageId());
        return true; // 继续处理
    }

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        // 接收后处理逻辑
        System.out.println("消息处理完成: " + message.getMessageId());
    }

    @Override
    public int getOrder() {
        return 100; // 设置优先级
    }
}
```

### 2. 注册拦截器

拦截器会自动被Spring容器扫描和注册。您只需要将拦截器实现类标记为Spring Bean：

```java

@Configuration
public class InterceptorConfiguration {

    @Bean
    public PulsarMessageInterceptor loggingInterceptor() {
        return new LoggingPulsarMessageInterceptor();
    }

    @Bean
    public PulsarMessageInterceptor validationInterceptor() {
        return new ValidationPulsarMessageInterceptor();
    }
}
```

### 3. 拦截器优先级

拦截器按照`getOrder()`方法返回的数值进行排序，数值越小优先级越高：

```java

@Override
public int getOrder() {
    return 10; // 高优先级
}
```

## 常见使用场景

### 1. 日志记录

```java

@Component
public class LoggingInterceptor implements PulsarMessageInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public Object beforeSend(String topic, Object message) {
        logger.info("发送消息到主题: {}, 内容: {}", topic, message);
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        if (exception == null) {
            logger.info("消息发送成功: {}", messageId);
        } else {
            logger.error("消息发送失败", exception);
        }
    }
}
```

### 2. 消息验证

```java

@Component
public class ValidationInterceptor implements PulsarMessageInterceptor {

    @Override
    public Object beforeSend(String topic, Object message) {
        if (message == null) {
            throw new IllegalArgumentException("消息不能为空");
        }
        // 其他验证逻辑
        return message;
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        // 验证消息格式、来源等
        return isValidMessage(message);
    }

    private boolean isValidMessage(Message<?> message) {
        // 验证逻辑
        return true;
    }
}
```

### 3. 性能监控

```java

@Component
public class PerformanceInterceptor implements PulsarMessageInterceptor {
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public Object beforeSend(String topic, Object message) {
        startTime.set(System.currentTimeMillis());
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        Long start = startTime.get();
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            System.out.println("发送耗时: " + duration + "ms");
            startTime.remove();
        }
    }
}
```

### 4. 消息过滤

```java

@Component
public class FilterInterceptor implements PulsarMessageInterceptor {

    @Override
    public boolean beforeReceive(Message<?> message) {
        // 过滤掉过期消息
        long currentTime = System.currentTimeMillis();
        long messageTime = message.getPublishTime();
        long maxAge = 24 * 60 * 60 * 1000; // 24小时

        return currentTime - messageTime <= maxAge;
    }
}
```

## 注意事项

1. **异常处理**: 拦截器中的异常不会影响主流程，但会被记录到日志中
2. **性能影响**: 拦截器会增加消息处理的延迟，请谨慎使用
3. **线程安全**: 拦截器实例会被多个线程共享，请确保线程安全
4. **优先级**: 合理设置拦截器优先级，确保执行顺序符合预期
5. **返回值**: `beforeSend`返回`null`会阻止消息发送，`beforeReceive`返回`false`会跳过消息处理

## 默认拦截器

系统提供了一个默认的日志拦截器，如果您不需要可以通过以下方式禁用：

```java
@Bean
@Primary
public PulsarMessageInterceptor noOpInterceptor() {
    return new PulsarMessageInterceptor() {
        // 空实现，覆盖默认拦截器
    };
}