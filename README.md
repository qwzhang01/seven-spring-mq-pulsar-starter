# Seven Spring MQ Pulsar Starter

一个功能丰富、易于使用的 Spring Boot Pulsar Starter，提供了完整的 Pulsar 集成解决方案。

## 特性

- 🚀 **简单易用**: 通过 `@EnablePulsar` 注解一键启用 Pulsar 功能
- 🔧 **灵活配置**: 支持通过配置文件灵活控制各种功能的启用/禁用
- 📨 **消息发送**: 提供同步/异步消息发送，支持延迟消息和事务消息
- 👂 **消息监听**: 通过 `@PulsarListener` 注解轻松创建消息监听器
- 🔄 **重试机制**: 内置消息处理失败重试机制，支持指数退避
- 💀 **死信队列**: 自动处理重试失败的消息到死信队列
- 🔍 **消息拦截**: 支持自定义消息拦截器，用于日志、监控等
- 💊 **健康检查**: 内置 Pulsar 连接健康检查
- 🎯 **事务支持**: 支持 Pulsar 事务消息

## 快速开始

### 1. 添加依赖

```xml

<dependency>
    <groupId>io.github.qwzhang01</groupId>
    <artifactId>seven-spring-mq-pulsar-starter</artifactId>
    <version>${pulsar-spring.version}</version>
</dependency>
```

### 2. 启用 Pulsar

在你的 Spring Boot 应用主类上添加 `@EnablePulsar` 注解：

```java

@SpringBootApplication
@EnablePulsar
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 基础配置

在 `application.yml` 中配置 Pulsar 连接信息：

```yaml
spring:
  pulsar:
    enabled: true
    service-url: pulsar://localhost:6650
    producer:
      default-topic: my-default-topic
      send-timeout: 30s
      batching-enabled: true
    consumer:
      subscription-name: my-subscription
      subscription-type: Exclusive
      ack-timeout: 30s
    authentication:
      enabled: false
      # token: your-jwt-token
    retry:
      enabled: true
      max-retries: 3
      initial-delay: 1000
      multiplier: 2.0
      max-delay: 30000
    dead-letter:
      enabled: false
    health:
      enabled: true
```

## 使用示例

### 消息发送

#### 基础消息发送

```java

@Service
public class MessageService {

    @Autowired
    private PulsarMessageSender messageSender;

    // 发送简单消息
    public void sendMessage(String message) {
        MessageId messageId = messageSender.send("my-topic", message);
        System.out.println("Message sent: " + messageId);
    }

    // 异步发送消息
    public CompletableFuture<MessageId> sendAsyncMessage(String message) {
        return messageSender.sendAsync("my-topic", message);
    }

    // 发送带键的消息
    public void sendKeyedMessage(String key, Object message) {
        messageSender.send("my-topic", key, message);
    }

    // 发送延迟消息
    public void sendDelayedMessage(String message, long delayMillis) {
        messageSender.sendDelayed("my-topic", message, delayMillis);
    }
}
```

#### 业务场景示例

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

    public void scheduleReminder(String userId, String message, long delayMinutes) {
        long delayMillis = delayMinutes * 60 * 1000;
        messageSender.sendDelayed("reminders",
                new ReminderMessage(userId, message), delayMillis);
    }
}
```

### 消息接收

#### 基础消息监听

```java

@Component
public class MessageListener {

    // 简单消息监听
    @PulsarListener(topic = "my-topic", subscription = "my-subscription")
    public void handleMessage(String message) {
        System.out.println("Received: " + message);
    }

    // 监听复杂对象
    @PulsarListener(
            topic = "user-events",
            subscription = "user-service",
            messageType = UserEvent.class
    )
    public void handleUserEvent(UserEvent event) {
        System.out.println("User event: " + event);
    }

    // 共享订阅模式
    @PulsarListener(
            topic = "shared-topic",
            subscription = "shared-subscription",
            subscriptionType = "Shared"
    )
    public void handleSharedMessage(String message) {
        // 多个消费者实例可以并行处理消息
        System.out.println("Shared message: " + message);
    }
}
```

#### 业务场景示例

```java

@Component
public class OrderEventListener {

    @PulsarListener(topic = "order-events", subscription = "order-processor")
    public void handleOrderEvent(Order order) {
        log.info("Processing order: {}", order.getId());

        if ("CREATED".equals(order.getStatus())) {
            processNewOrder(order);
        } else if ("CANCELLED".equals(order.getStatus())) {
            processCancelledOrder(order);
        }
    }

    // 支付处理 - 共享订阅模式
    @PulsarListener(
            topic = "payment-requests",
            subscription = "payment-processor",
            subscriptionType = "Shared"
    )
    public void processPayment(PaymentRequest request) {
        try {
            PaymentResult result = paymentGateway.process(request);

            if (result.isSuccess()) {
                messageSender.send("payment-events",
                        new PaymentSuccessEvent(request.getOrderId(), result));
            } else {
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

## 消息拦截器

### 拦截器接口

消息拦截器提供了在消息发送和接收过程中进行拦截处理的能力：

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

### 自定义拦截器示例

#### 日志拦截器

```java

@Component
public class LoggingInterceptor implements PulsarMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public Object beforeSend(String topic, Object message) {
        logger.info("Sending message to topic: {}, message: {}", topic, message);
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        if (exception != null) {
            logger.error("Failed to send message to topic: {}", topic, exception);
        } else {
            logger.info("Message sent successfully to topic: {}, messageId: {}", topic, messageId);
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        logger.info("Receiving message: {}", message.getMessageId());
        return true;
    }

    @Override
    public int getOrder() {
        return 1; // 设置拦截器优先级
    }
}
```

#### 消息审计拦截器

```java

@Component
public class MessageAuditInterceptor implements PulsarMessageInterceptor {

    @Autowired
    private AuditService auditService;

    @Override
    public Object beforeSend(String topic, Object message) {
        auditService.logMessageSent(topic, message);

        // 可以修改消息内容
        if (message instanceof AuditableMessage) {
            ((AuditableMessage) message).setAuditInfo(getCurrentUser(), System.currentTimeMillis());
        }

        return message;
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

#### 性能监控拦截器

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

### 死信队列处理

### 死信队列配置

在 `application.yml` 中配置死信队列：

```yaml
spring:
  pulsar:
    dead-letter:
      topic-suffix: "-DLQ"                        # 死信队列主题后缀
      max-retries: 3                              # 最大重试次数

      # 重试配置
      retry:
        smart-strategy-enabled: true              # 是否启用智能重试策略
        base-delay: PT1S                          # 基础重试延迟
        max-delay: PT5M                           # 最大重试延迟
        retry-window: PT24H                       # 重试时间窗口
        jitter-enabled: true                      # 是否启用抖动
        jitter-factor: 0.2                       # 抖动因子

      # 清理配置
      cleanup:
        auto-cleanup-enabled: true                # 是否启用自动清理
        message-expiration: P7D                   # 消息过期时间
        cleanup-cron: "0 0 2 * * ?"              # 清理执行时间

      # 监控配置
      monitoring:
        enabled: true                             # 是否启用监控
        monitoring-interval: PT5M                 # 监控间隔
        alert-enabled: false                      # 是否启用告警
        alert-threshold: 100                      # 告警阈值

      # 统计配置
      statistics:
        enabled: true                             # 是否启用统计
        retention-period: P30D                    # 统计数据保留时间
        detailed-enabled: false                   # 是否启用详细统计
```

### 死信队列处理器

```java

@Component
public class CustomDeadLetterHandler implements DeadLetterQueueHandler {

    @Override
    public void handleDeadLetter(String originalTopic, Message<?> message, Exception exception) {
        // 自定义死信处理逻辑
        System.out.println("Dead letter from topic: " + originalTopic);
        System.out.println("Message ID: " + message.getMessageId());
        System.out.println("Error: " + exception.getMessage());

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
    public int getMaxRetries() {
        return 5; // 自定义最大重试次数
    }
}
```

### 死信队列管理

#### REST API 管理

```java

@RestController
@RequestMapping("/api/dead-letter")
public class DeadLetterController {

    @Autowired
    private DeadLetterQueueManager deadLetterQueueManager;

    // 获取死信队列统计信息
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        // 实现统计信息获取
    }

    // 读取死信消息
    @GetMapping("/queues/{topic}/messages")
    public ResponseEntity<List<DeadLetterMessage>> readMessages(
            @PathVariable String topic,
            @RequestParam(defaultValue = "10") int maxMessages) {
        // 实现消息读取
    }

    // 重新处理死信消息
    @PostMapping("/reprocess")
    public ResponseEntity<Map<String, Object>> reprocessMessage(
            @RequestBody ReprocessRequest request) {
        // 实现消息重新处理
    }

    // 清理过期消息
    @PostMapping("/queues/{topic}/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredMessages(
            @PathVariable String topic,
            @RequestParam(defaultValue = "7") int expirationDays) {
        // 实现消息清理
    }
}
```

#### 编程式管理

```java

@Service
public class DeadLetterManagementService {

    @Autowired
    private DeadLetterQueueManager deadLetterQueueManager;

    @Autowired
    private DeadLetterMessageProcessor deadLetterMessageProcessor;

    // 监控死信队列
    public void monitorDeadLetterQueues() {
        List<DeadLetterQueueInfo> queueInfoList = deadLetterQueueManager.getAllQueueInfo();

        for (DeadLetterQueueInfo queueInfo : queueInfoList) {
            if (queueInfo.getMessageCount() > 100) {
                // 发送告警
                sendAlert("Dead letter queue " + queueInfo.getTopicName() +
                        " has " + queueInfo.getMessageCount() + " messages");
            }
        }
    }

    // 批量重新处理死信消息
    public void batchReprocessMessages(String deadLetterTopic) {
        List<DeadLetterMessage> messages = deadLetterQueueManager
                .readDeadLetterMessages(deadLetterTopic, 50);

        BatchReprocessResult result = deadLetterMessageProcessor
                .batchReprocessDeadLetterMessages(messages, getOriginalTopic(deadLetterTopic));

        log.info("Batch reprocess result: success={}, failed={}",
                result.getSuccessCount(), result.getFailureCount());
    }

    // 定期清理过期消息
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupExpiredMessages() {
        List<DeadLetterQueueInfo> queueInfoList = deadLetterQueueManager.getAllQueueInfo();

        for (DeadLetterQueueInfo queueInfo : queueInfoList) {
            int cleanedCount = deadLetterQueueManager
                    .cleanupExpiredMessages(queueInfo.getTopicName(), 7);

            if (cleanedCount > 0) {
                log.info("Cleaned {} expired messages from {}",
                        cleanedCount, queueInfo.getTopicName());
            }
        }
    }
}
```

### 死信队列最佳实践

#### 1. 异常分类处理

```java

@Component
public class SmartDeadLetterHandler implements DeadLetterQueueHandler {

    @Override
    public boolean shouldSendToDeadLetter(Message<?> message, Exception exception, int retryCount) {
        // 业务异常不重试，直接进入死信队列
        if (exception instanceof BusinessException) {
            return true;
        }

        // 系统异常根据重试次数决定
        if (exception instanceof SystemException) {
            return retryCount >= getMaxRetries();
        }

        // 网络异常增加重试次数
        if (exception instanceof NetworkException) {
            return retryCount >= (getMaxRetries() * 2);
        }

        return retryCount >= getMaxRetries();
    }

    @Override
    public String getDeadLetterTopic(String originalTopic) {
        // 根据主题类型使用不同的死信队列策略
        if (originalTopic.startsWith("critical-")) {
            return "critical-dlq"; // 关键业务使用专用死信队列
        } else if (originalTopic.startsWith("batch-")) {
            return "batch-dlq"; // 批处理使用专用死信队列
        } else {
            return originalTopic + "-dlq"; // 默认策略
        }
    }
}
```

#### 2. 死信消息分析

```java

@Service
public class DeadLetterAnalysisService {

    // 分析死信消息模式
    public DeadLetterAnalysisReport analyzeDeadLetterPatterns() {
        DeadLetterStatistics stats = deadLetterQueueHandler.getStatistics();

        DeadLetterAnalysisReport report = new DeadLetterAnalysisReport();

        // 分析各主题的死信率
        for (Map.Entry<String, Long> entry : stats.getTopicDeadLetterCount().entrySet()) {
            String topic = entry.getKey();
            Long deadLetterCount = entry.getValue();

            // 计算死信率
            long totalMessages = getTotalMessageCount(topic);
            double deadLetterRate = (double) deadLetterCount / totalMessages * 100;

            if (deadLetterRate > 5.0) { // 死信率超过5%
                report.addHighRiskTopic(topic, deadLetterRate);
            }
        }

        return report;
    }

    // 生成死信处理建议
    public List<String> generateRecommendations(String deadLetterTopic) {
        List<DeadLetterMessage> messages = deadLetterQueueManager
                .readDeadLetterMessages(deadLetterTopic, 100);

        Map<String, Integer> exceptionCounts = new HashMap<>();

        for (DeadLetterMessage message : messages) {
            String exceptionClass = message.getExceptionClass();
            exceptionCounts.merge(exceptionClass, 1, Integer::sum);
        }

        List<String> recommendations = new ArrayList<>();

        // 根据异常类型生成建议
        for (Map.Entry<String, Integer> entry : exceptionCounts.entrySet()) {
            String exceptionClass = entry.getKey();
            Integer count = entry.getValue();

            if (exceptionClass.contains("TimeoutException") && count > 10) {
                recommendations.add("考虑增加消息处理超时时间或优化处理逻辑");
            } else if (exceptionClass.contains("ValidationException") && count > 5) {
                recommendations.add("检查消息格式验证逻辑，可能存在数据质量问题");
            } else if (exceptionClass.contains("DatabaseException") && count > 3) {
                recommendations.add("检查数据库连接和性能，考虑增加重试机制");
            }
        }

        return recommendations;
    }
}
```

## 事务支持

### 事务配置

在 `application.yml` 中启用事务功能：

```yaml
spring:
  pulsar:
    service-url: pulsar://localhost:6650
    transaction:
      enabled: true                                    # 启用事务
      timeout: PT1M                                   # 事务超时时间（1分钟）
      coordinator-topic: persistent://pulsar/system/transaction_coordinator_assign
      buffer-snapshot-segment-size: 1048576           # 1MB
      buffer-snapshot-min-time-in-millis: PT5S        # 5秒
      buffer-snapshot-max-transaction-count: 1000
      log-store-size: 1073741824                      # 1GB
```

### 事务使用方式

#### 注解方式（推荐）

```java

@Service
public class MessageService {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    @PulsarTransactional
    public void sendMessages(String topic, List<String> messages) {
        // 获取当前事务
        Transaction transaction = PulsarTransactionUtils.getCurrentTransaction();

        for (String message : messages) {
            // 在事务中发送消息
            pulsarTemplate.send(topic, message, transaction);
        }

        // 如果方法正常结束，事务会自动提交
        // 如果抛出异常，事务会自动回滚
    }
}
```

#### 编程式事务

```java

@Service
public class MessageService {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    @Autowired
    private TransactionTemplate pulsarTransactionTemplate;

    public void sendMessages(String topic, List<String> messages) {
        pulsarTransactionTemplate.execute(status -> {
            try {
                Transaction transaction = PulsarTransactionUtils.getCurrentTransaction();

                for (String message : messages) {
                    pulsarTemplate.send(topic, message, transaction);
                }

                return "Success";
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException("Transaction failed", e);
            }
        });
    }
}
```

#### 手动事务管理

```java

@Service
public class MessageService {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    @Autowired
    private PulsarTransactionBuilderFactory transactionBuilderFactory;

    public void sendMessages(String topic, List<String> messages) {
        Transaction transaction = null;
        try {
            // 创建事务
            transaction = transactionBuilderFactory.newTransactionBuilder().build().get();

            for (String message : messages) {
                pulsarTemplate.send(topic, message, transaction);
            }

            // 提交事务
            transaction.commit().get();

        } catch (Exception e) {
            if (transaction != null) {
                try {
                    transaction.abort().get();
                } catch (Exception rollbackException) {
                    // 处理回滚异常
                }
            }
            throw new RuntimeException("Transaction failed", e);
        }
    }
}
```

### 事务工具类

```java
// 获取当前事务
Transaction currentTx = PulsarTransactionUtils.getCurrentTransaction();

// 检查是否存在活跃事务
boolean isActive = PulsarTransactionUtils.isTransactionActive();

// 获取当前事务ID
String txnId = PulsarTransactionUtils.getCurrentTransactionId();
```

## 高级配置

### EnablePulsar 注解选项

```java

@EnablePulsar(
        enabled = true,                    // 是否启用 Pulsar
        enableTransaction = false,         // 是否启用事务支持
        enableHealthCheck = true,          // 是否启用健康检查
        enableInterceptor = true,          // 是否启用消息拦截器
        enableDeadLetterQueue = false,     // 是否启用死信队列
        enableRetry = true                 // 是否启用消息重试
)
@SpringBootApplication
public class Application {
    // ...
}
```

### 完整配置示例

#### 生产环境配置

```yaml
spring:
  pulsar:
    enabled: true
    service-url: pulsar://pulsar-cluster:6650

    # 生产者配置
    producer:
      default-topic: ${app.name}-events
      send-timeout: 10s
      block-if-queue-full: false
      max-pending-messages: 1000
      batching-enabled: true
      batching-max-messages: 100
      batching-max-publish-delay: 5ms

    # 消费者配置
    consumer:
      subscription-name: ${app.name}-${app.instance}
      subscription-type: Shared
      ack-timeout: 60s
      receiver-queue-size: 500
      auto-ack-oldest-chunked-message-on-queue-full: false

    # 客户端配置
    client:
      operation-timeout: 30s
      connection-timeout: 10s
      num-io-threads: 1
      num-listener-threads: 1

    # 认证配置
    authentication:
      enabled: true
      token: ${PULSAR_JWT_TOKEN}
      auth-plugin-class-name: org.apache.pulsar.client.impl.auth.AuthenticationToken
      auth-params:
        key1: value1
        key2: value2

    # 重试配置
    retry:
      enabled: true
      max-retries: 5
      initial-delay: 2000
      multiplier: 1.5
      max-delay: 60000
      use-random-delay: true

    # 死信队列配置
    dead-letter:
      enabled: true

    # 健康检查配置
    health:
      enabled: true

    # 事务配置
    transaction:
      enabled: false

logging:
  level:
    com.github.spring.mq.pulsar: INFO
    org.apache.pulsar: WARN
```

#### 开发环境配置

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

## 监控和健康检查

### 健康检查

Starter 提供了内置的健康检查功能：

```java

@Autowired
private PulsarHealthIndicator healthIndicator;

public void checkHealth() {
    Map<String, Object> health = healthIndicator.health();
    System.out.println("Pulsar health: " + health);
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

### 2. 主题命名

使用有意义的主题名称，建议使用分层结构：

- `app.service.event` - 应用.服务.事件
- `order.payment.success` - 订单.支付.成功
- `user.registration.completed` - 用户.注册.完成

### 3. 订阅模式选择

- **Exclusive**: 单一消费者，保证消息顺序
- **Shared**: 多消费者负载均衡，提高吞吐量
- **Failover**: 主备模式，高可用
- **Key_Shared**: 按键分区，兼顾顺序和并发

### 4. 异常处理

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

### 5. 性能优化

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

### 6. 事务最佳实践

- 保持事务范围尽可能小
- 避免在事务中执行长时间运行的操作
- 合理设置事务超时时间
- 正确处理事务异常和回滚

### 7. 拦截器注意事项

- 拦截器会增加消息处理延迟，请谨慎使用
- 确保拦截器实现的线程安全性
- 合理设置拦截器优先级
- 拦截器中避免执行耗时操作
- 异常处理要得当，不要影响主流程

## 故障排除

### 常见问题

1. **连接失败**: 检查 Pulsar 服务是否正常运行，网络是否可达
2. **认证失败**: 确认认证配置是否正确
3. **消息丢失**: 检查消息确认机制是否正确实现
4. **性能问题**: 调整批量处理、连接池等配置参数
5. **事务超时**: 检查 `transaction.timeout` 配置，优化业务逻辑执行时间
6. **事务协调器不可用**: 检查 Pulsar 集群配置，确认事务协调器服务正常运行

### 日志配置

```yaml
logging:
  level:
    com.github.spring.mq.pulsar: DEBUG
    org.apache.pulsar: INFO
    com.github.spring.mq.pulsar.config.PulsarTransactionConfiguration: DEBUG
    org.apache.pulsar.client.impl.transaction: DEBUG
```

### 调试技巧

```java
// 启用详细日志
@PulsarTransactional
public void debugTransaction() {
    Transaction tx = PulsarTransactionUtils.getCurrentTransaction();
    log.debug("Transaction ID: {}", tx.getTxnID());
    log.debug("Transaction state: {}", tx.getState());
}
```

## 注意事项

1. **Pulsar 版本要求**: 确保使用支持事务的 Pulsar 版本（2.7.0+）
2. **集群配置**: Pulsar 集群需要启用事务协调器
3. **性能影响**: 事务和拦截器会带来一定的性能开销，请根据业务需求权衡使用
4. **错误恢复**: 合理处理事务失败和重试逻辑
5. **资源管理**: 及时释放事务资源，避免资源泄漏

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目。

## 待办

- 死信队列
- 重试队列

## 许可证

本项目采用 MIT 许可证。