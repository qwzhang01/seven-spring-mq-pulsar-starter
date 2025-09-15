# Pulsar Starter 使用示例

本文档提供了 Seven Spring MQ Pulsar Starter 的详细使用示例。

## 基础使用

### 1. 项目配置

首先在你的 Spring Boot 项目中添加依赖：

```xml

<dependency>
    <groupId>com.github.spring.mq</groupId>
    <artifactId>seven-spring-mq-pulsar-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 2. 启用 Pulsar

在主应用类上添加 `@EnablePulsar` 注解：

```java

@SpringBootApplication
@EnablePulsar
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 3. 基础配置

在 `application.yml` 中配置 Pulsar：

```yaml
spring:
  pulsar:
    service-url: pulsar://localhost:6650
    producer:
      default-topic: my-app-topic
    consumer:
      subscription-name: my-app-subscription
```

## 消息发送示例

### 简单消息发送

```java

@Service
public class OrderService {

    @Autowired
    private PulsarMessageSender messageSender;

    public void createOrder(Order order) {
        // 业务逻辑处理
        processOrder(order);

        // 发送订单创建事件
        messageSender.send("order-events", order);
    }

    public void sendNotification(String userId, String message) {
        // 发送通知消息
        messageSender.send("notifications", userId, message);
    }
}
```

### 异步消息发送

```java

@Service
public class EmailService {

    @Autowired
    private PulsarMessageSender messageSender;

    public CompletableFuture<Void> sendEmailAsync(EmailMessage email) {
        return messageSender.sendAsync("email-queue", email)
                .thenAccept(messageId -> {
                    log.info("Email queued successfully: {}", messageId);
                })
                .exceptionally(throwable -> {
                    log.error("Failed to queue email", throwable);
                    return null;
                });
    }
}
```

### 延迟消息发送

```java

@Service
public class SchedulerService {

    @Autowired
    private PulsarMessageSender messageSender;

    public void scheduleReminder(String userId, String message, long delayMinutes) {
        long delayMillis = delayMinutes * 60 * 1000;
        messageSender.sendDelayed("reminders",
                new ReminderMessage(userId, message), delayMillis);
    }
}
```

## 消息接收示例

### 基础消息监听

```java

@Component
public class OrderEventListener {

    @PulsarListener(topic = "order-events", subscription = "order-processor")
    public void handleOrderEvent(Order order) {
        log.info("Processing order: {}", order.getId());

        // 处理订单事件
        if ("CREATED".equals(order.getStatus())) {
            processNewOrder(order);
        } else if ("CANCELLED".equals(order.getStatus())) {
            processCancelledOrder(order);
        }
    }

    private void processNewOrder(Order order) {
        // 新订单处理逻辑
    }

    private void processCancelledOrder(Order order) {
        // 取消订单处理逻辑
    }
}
```

### 共享订阅模式

```java

@Component
public class PaymentProcessor {

    // 多个实例可以并行处理支付消息
    @PulsarListener(
            topic = "payment-requests",
            subscription = "payment-processor",
            subscriptionType = "Shared"
    )
    public void processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());

        try {
            // 调用支付网关
            PaymentResult result = paymentGateway.process(request);

            if (result.isSuccess()) {
                // 发送支付成功事件
                messageSender.send("payment-events",
                        new PaymentSuccessEvent(request.getOrderId(), result));
            } else {
                // 发送支付失败事件
                messageSender.send("payment-events",
                        new PaymentFailedEvent(request.getOrderId(), result.getError()));
            }
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            throw e; // 触发重试机制
        }
    }
}
```

### 复杂对象消息处理

```java

@Component
public class UserEventHandler {

    @PulsarListener(
            topic = "user-events",
            subscription = "user-analytics",
            messageType = UserEvent.class
    )
    public void handleUserEvent(UserEvent event) {
        switch (event.getEventType()) {
            case "USER_REGISTERED":
                handleUserRegistration(event);
                break;
            case "USER_LOGIN":
                handleUserLogin(event);
                break;
            case "USER_PURCHASE":
                handleUserPurchase(event);
                break;
            default:
                log.warn("Unknown user event type: {}", event.getEventType());
        }
    }

    private void handleUserRegistration(UserEvent event) {
        // 用户注册分析
        analyticsService.trackUserRegistration(event.getUserId());

        // 发送欢迎邮件
        emailService.sendWelcomeEmail(event.getUserId());
    }

    private void handleUserLogin(UserEvent event) {
        // 登录行为分析
        analyticsService.trackUserLogin(event.getUserId(), event.getTimestamp());
    }

    private void handleUserPurchase(UserEvent event) {
        // 购买行为分析
        analyticsService.trackPurchase(event.getUserId(), event.getData());

        // 推荐系统更新
        recommendationService.updateUserProfile(event.getUserId());
    }
}
```

## 高级功能示例

### 自定义消息拦截器

```java

@Component
public class MessageAuditInterceptor implements PulsarMessageInterceptor {

    @Autowired
    private AuditService auditService;

    @Override
    public Object beforeSend(String topic, Object message) {
        // 记录发送审计日志
        auditService.logMessageSent(topic, message);

        // 可以修改消息内容
        if (message instanceof AuditableMessage) {
            ((AuditableMessage) message).setAuditInfo(getCurrentUser(), System.currentTimeMillis());
        }

        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        if (exception != null) {
            auditService.logMessageSendFailed(topic, message, exception);
        } else {
            auditService.logMessageSendSuccess(topic, messageId);
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        // 检查消息是否应该被处理
        String topic = message.getTopicName();
        if (isBlacklistedTopic(topic)) {
            log.warn("Message from blacklisted topic ignored: {}", topic);
            return false;
        }

        auditService.logMessageReceived(message);
        return true;
    }

    @Override
    public int getOrder() {
        return 1; // 高优先级
    }
}
```

### 自定义死信队列处理

```java

@Component
public class CustomDeadLetterHandler implements DeadLetterQueueHandler {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MessageRepository messageRepository;

    @Override
    public void handleDeadLetter(String originalTopic, Message<?> message, Exception exception) {
        log.error("Message sent to dead letter queue - Topic: {}, MessageId: {}",
                originalTopic, message.getMessageId(), exception);

        // 保存到数据库用于后续分析
        DeadLetterRecord record = new DeadLetterRecord();
        record.setOriginalTopic(originalTopic);
        record.setMessageId(message.getMessageId().toString());
        record.setMessageData(new String(message.getData()));
        record.setErrorMessage(exception.getMessage());
        record.setTimestamp(System.currentTimeMillis());

        messageRepository.saveDeadLetterRecord(record);

        // 发送告警通知
        if (isCriticalTopic(originalTopic)) {
            notificationService.sendAlert(
                    "Critical message failed processing",
                    String.format("Topic: %s, Error: %s", originalTopic, exception.getMessage())
            );
        }
    }

    @Override
    public boolean shouldSendToDeadLetter(Message<?> message, Exception exception, int retryCount) {
        // 某些异常类型不重试，直接进入死信队列
        if (exception instanceof IllegalArgumentException ||
                exception instanceof JsonProcessingException) {
            return true;
        }

        // 达到最大重试次数
        return retryCount >= getMaxRetries();
    }

    @Override
    public int getMaxRetries() {
        return 5; // 自定义最大重试次数
    }

    private boolean isCriticalTopic(String topic) {
        return topic.contains("payment") || topic.contains("order");
    }
}
```

### 健康检查集成

```java

@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private PulsarHealthIndicator pulsarHealthIndicator;

    @GetMapping("/pulsar")
    public ResponseEntity<Map<String, Object>> checkPulsarHealth() {
        Map<String, Object> health = pulsarHealthIndicator.health();

        if ("UP".equals(health.get("status"))) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
```

## 配置示例

### 生产环境配置

```yaml
spring:
  pulsar:
    service-url: pulsar://pulsar-cluster:6650

    producer:
      default-topic: ${app.name}-events
      send-timeout: 10s
      batching-enabled: true
      batching-max-messages: 100
      batching-max-publish-delay: 5ms

    consumer:
      subscription-name: ${app.name}-${app.instance}
      subscription-type: Shared
      ack-timeout: 60s
      receiver-queue-size: 500

    authentication:
      enabled: true
      token: ${PULSAR_JWT_TOKEN}

    retry:
      enabled: true
      max-retries: 5
      initial-delay: 2000
      multiplier: 1.5
      max-delay: 60000
      use-random-delay: true

    dead-letter:
      enabled: true

    health:
      enabled: true

logging:
  level:
    com.github.spring.mq.pulsar: INFO
    org.apache.pulsar: WARN
```

### 开发环境配置

```yaml
spring:
  pulsar:
    service-url: pulsar://localhost:6650

    producer:
      default-topic: dev-events
      batching-enabled: false # 开发环境禁用批量以便调试

    consumer:
      subscription-name: dev-subscription
      subscription-type: Exclusive

    authentication:
      enabled: false

    retry:
      enabled: true
      max-retries: 2 # 开发环境减少重试次数
      initial-delay: 500

    dead-letter:
      enabled: true

logging:
  level:
    com.github.spring.mq.pulsar: DEBUG
    org.apache.pulsar: INFO
```

## 最佳实践

### 1. 消息设计

```java
// 好的消息设计
public class OrderEvent {
    private String eventId;        // 唯一事件ID
    private String eventType;      // 事件类型
    private long timestamp;        // 时间戳
    private String orderId;        // 业务ID
    private Map<String, Object> data; // 事件数据

    // 构造函数、getter、setter
}

// 避免的消息设计
public class BadOrderEvent {
    private Order order; // 包含过多信息
    // 缺少事件元数据
}
```

### 2. 错误处理

```java

@PulsarListener(topic = "orders", subscription = "order-processor")
public void processOrder(Order order) {
    try {
        // 业务处理
        orderService.process(order);
    } catch (BusinessException e) {
        // 业务异常，记录日志但不重试
        log.error("Business error processing order: {}", order.getId(), e);
        // 不抛出异常，避免重试
    } catch (Exception e) {
        // 系统异常，可以重试
        log.error("System error processing order: {}", order.getId(), e);
        throw e; // 抛出异常触发重试
    }
}
```

### 3. 性能优化

```java

@Service
public class HighThroughputService {

    // 使用异步发送提高性能
    public void sendBatchMessages(List<Message> messages) {
        List<CompletableFuture<MessageId>> futures = messages.stream()
                .map(msg -> messageSender.sendAsync("batch-topic", msg))
                .collect(Collectors.toList());

        // 等待所有消息发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("All messages sent successfully"))
                .exceptionally(throwable -> {
                    log.error("Some messages failed to send", throwable);
                    return null;
                });
    }
}
```

这些示例展示了如何在实际项目中使用 Pulsar Starter 的各种功能。根据你的具体需求，可以选择合适的模式和配置。