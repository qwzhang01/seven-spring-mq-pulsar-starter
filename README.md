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
- 🎯 **事务支持**: 支持 Pulsar 事务消息（开发中）

## 快速开始

### 1. 添加依赖

```xml

<dependency>
    <groupId>com.github.spring.mq</groupId>
    <artifactId>seven-spring-mq-pulsar-starter</artifactId>
    <version>1.0.1</version>
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

### 3. 配置 Pulsar

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

### 发送消息

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

### 接收消息

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

### 自定义消息拦截器

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

### 自定义死信队列处理器

```java

@Component
public class CustomDeadLetterHandler implements DeadLetterQueueHandler {

    @Override
    public void handleDeadLetter(String originalTopic, Message<?> message, Exception exception) {
        // 自定义死信处理逻辑
        System.out.println("Dead letter from topic: " + originalTopic);
        System.out.println("Message ID: " + message.getMessageId());
        System.out.println("Error: " + exception.getMessage());

        // 可以发送到监控系统、数据库等
    }

    @Override
    public int getMaxRetries() {
        return 5; // 自定义最大重试次数
    }
}
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

```yaml
spring:
  pulsar:
    enabled: true
    service-url: pulsar://localhost:6650

    # 生产者配置
    producer:
      default-topic: default-topic
      send-timeout: 30s
      block-if-queue-full: false
      max-pending-messages: 1000
      batching-enabled: true
      batching-max-messages: 1000
      batching-max-publish-delay: 10ms

    # 消费者配置
    consumer:
      subscription-name: default-subscription
      subscription-type: Exclusive
      ack-timeout: 30s
      receiver-queue-size: 1000
      auto-ack-oldest-chunked-message-on-queue-full: false

    # 客户端配置
    client:
      operation-timeout: 30s
      connection-timeout: 10s
      num-io-threads: 1
      num-listener-threads: 1

    # 认证配置
    authentication:
      enabled: false
      token: your-jwt-token
      auth-plugin-class-name: org.apache.pulsar.client.impl.auth.AuthenticationToken
      auth-params:
        key1: value1
        key2: value2

    # 重试配置
    retry:
      enabled: true
      max-retries: 3
      initial-delay: 1000
      multiplier: 2.0
      max-delay: 30000
      use-random-delay: true

    # 死信队列配置
    dead-letter:
      enabled: false

    # 健康检查配置
    health:
      enabled: true

    # 事务配置
    transaction:
      enabled: false
```

## 监控和健康检查

Starter 提供了内置的健康检查功能，可以通过以下方式获取 Pulsar 连接状态：

```java

@Autowired
private PulsarHealthIndicator healthIndicator;

public void checkHealth() {
    Map<String, Object> health = healthIndicator.health();
    System.out.println("Pulsar health: " + health);
}
```

## 最佳实践

1. **主题命名**: 使用有意义的主题名称，建议使用分层结构，如 `app.service.event`
2. **订阅模式**: 根据业务需求选择合适的订阅模式（Exclusive、Shared、Failover、Key_Shared）
3. **消息确认**: 在消息处理完成后及时确认消息，避免消息重复消费
4. **异常处理**: 在消息监听器中妥善处理异常，避免消息丢失
5. **批量处理**: 对于高吞吐量场景，启用批量发送和接收
6. **监控告警**: 配置适当的监控和告警机制

## 故障排除

### 常见问题

1. **连接失败**: 检查 Pulsar 服务是否正常运行，网络是否可达
2. **认证失败**: 确认认证配置是否正确
3. **消息丢失**: 检查消息确认机制是否正确实现
4. **性能问题**: 调整批量处理、连接池等配置参数

### 日志配置

```yaml
logging:
  level:
    com.github.spring.mq.pulsar: DEBUG
    org.apache.pulsar: INFO
```

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目。

## 许可证

本项目采用 MIT 许可证。