# Seven Spring MQ Pulsar Starter

[![构建状态](https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/actions/workflows/test.yml/badge.svg)](https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/actions/workflows/test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.qwzhang01/seven-spring-mq-pulsar-starter.svg)](https://central.sonatype.com/artifact/io.github.qwzhang01/seven-spring-mq-pulsar-starter)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0%2B-green.svg)](https://spring.io/projects/spring-boot)
[![Apache Pulsar](https://img.shields.io/badge/Apache%20Pulsar-3.2.4%2B-blue.svg)](https://pulsar.apache.org/)
[![codecov](https://codecov.io/gh/qwzhang01/seven-spring-mq-pulsar-starter/branch/main/graph/badge.svg)](https://codecov.io/gh/qwzhang01/seven-spring-mq-pulsar-starter)

[English](README.md)

一个功能丰富、易于使用的 Spring Boot Pulsar Starter，提供完整的 Pulsar 集成解决方案。

## 特性

- 🚀 **简单易用**: 通过单个 `@EnablePulsar` 注解启用 Pulsar 功能
- 🔧 **灵活配置**: 支持通过配置文件灵活控制各种功能
- 📨 **消息发送**: 提供同步/异步消息发送，支持延迟消息和事务消息
- 👂 **消息监听**: 通过 `@PulsarListener` 注解轻松创建消息监听器
- 🔄 **重试机制**: 内置消息处理失败重试机制，支持指数退避
- 💀 **死信队列**: 自动处理重试失败的消息到死信队列
- 🔍 **消息拦截**: 支持自定义消息拦截器，用于日志记录、监控等
- 💊 **健康检查**: 内置 Pulsar 连接健康检查
- 🎯 **事务支持**: 支持 Pulsar 事务消息
- 🗺️ **多实例配置**: 支持配置多个生产者和消费者实例
- 🛣️ **消息路由**: 增强按消息类型/业务类型做分支处理能力，发送时用 `msgRoute` 标记业务类型到元数据，消费消息时按照 `msgRoute` 反向找到真实的处理器
- 🔐 **租户上下文**: 增强消息消费拦截器，支持在拦截器里面根据消息元信息，初始化租户信息等

## 增强的消息路由能力

### 基于业务类型的分支处理

该starter提供了增强的消息路由能力，允许您：

1. **在元数据中标记业务类型**：发送消息时使用 `msgRoute` 标记业务类型
2. **根据业务类型路由到适当的处理器**：消费消息时根据业务类型找到对应的处理器
3. **支持多个消息处理器**：为不同的业务场景提供不同的处理器

### 消息路由配置

```yaml
spring:
  pulsar:
    consumer:
      # 用于路由的业务类型字段名
      business-key: businessPath
      
      # 消息路由配置
      msg-route: order.process
```

### 使用示例

#### 发送带路由信息的消息

```java
@Service
public class OrderService {
    
    @Autowired
    private PulsarTemplate pulsarTemplate;
    
    public void sendOrderEvent(OrderEvent event) {
        // 发送前设置消息路由
        MsgContext.setMsgRoute("order.created");
        
        // 发送消息 - 路由信息会自动包含在元数据中
        pulsarTemplate.send("order-events", event);
        
        // 清理上下文
        MsgContext.remove();
    }
}
```

#### 接收消息并基于路由处理

```java
@Component
public class OrderEventListener {
    
    // 处理订单创建事件
    @PulsarListener(
        topic = "order-events",
        subscription = "order-processor",
        msgRoute = "order.created"
    )
    public void handleOrderCreated(OrderEvent event) {
        // 处理订单创建逻辑
        orderService.processNewOrder(event);
    }
    
    // 处理订单取消事件
    @PulsarListener(
        topic = "order-events", 
        subscription = "order-processor",
        msgRoute = "order.cancelled"
    )
    public void handleOrderCancelled(OrderEvent event) {
        // 处理订单取消逻辑
        orderService.processCancelledOrder(event);
    }
}
```

## 增强的消息消费拦截器

### 租户信息初始化

该starter提供了强大的消息消费拦截器，支持：

1. **基于消息元数据的自动租户上下文传播**
2. **在拦截器中自定义租户切换逻辑**
3. **多租户场景的线程本地上下文管理**

### 拦截器实现

#### 自定义租户拦截器

```java
@Component
public class TenantContextInterceptor extends MetaMessageInterceptor {
    
    @Autowired
    private TenantService tenantService;
    
    @Override
    public void buildSendContext() {
        // 提取当前租户上下文并设置用于消息发送
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            MsgContext.setCorpKey(currentTenant);
        }
    }
    
    @Override
    public boolean buildReceiveContext(String corpKey) {
        // 基于消息元数据切换租户上下文
        if (corpKey != null && !corpKey.isEmpty()) {
            return tenantService.switchTenant(corpKey);
        }
        return true;
    }
    
    @Override
    public int getOrder() {
        return 1; // 租户上下文设置的高优先级
    }
}
```

#### 审计日志拦截器

```java
@Component
public class AuditLoggingInterceptor implements PulsarMessageInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingInterceptor.class);
    
    @Override
    public Object beforeSend(String topic, Object message) {
        logger.info("发送消息到主题: {}, 租户: {}, 消息: {}", 
            topic, MsgContext.getCorpKey(), message);
        return message;
    }
    
    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        if (exception != null) {
            logger.error("发送消息到主题失败: {}, 租户: {}", 
                topic, MsgContext.getCorpKey(), exception);
        } else {
            logger.info("成功发送消息到主题: {}, 消息ID: {}", 
                topic, messageId);
        }
    }
    
    @Override
    public boolean beforeReceive(Message<?> message) {
        String tenantId = message.getProperties().get(MsgMetaKey.CORP.getCode());
        logger.info("从主题接收消息: {}, 租户: {}, 消息ID: {}", 
            message.getTopicName(), tenantId, message.getMessageId());
        return true;
    }
    
    @Override
    public int getOrder() {
        return 2; // 比租户拦截器优先级低
    }
}
```

### 拦截器配置

```yaml
spring:
  pulsar:
    interceptor:
      # 启用消息拦截器
      enabled: true
      
      # 自定义拦截器（由Spring自动检测）
      custom-interceptors:
        - com.example.TenantContextInterceptor
        - com.example.AuditLoggingInterceptor
```

## 高级消息路由场景

### 多路由消息处理

```java
@Component
public class MultiRouteMessageProcessor {
    
    // 处理多个业务路由的消息
    @PulsarListener(
        topic = "business-events",
        subscription = "multi-route-processor",
        multiRoute = true
    )
    public void handleMultiRouteMessage(Message<BusinessEvent> message) {
        
        // 从消息属性中提取路由
        String msgRoute = message.getProperties().get(MsgMetaKey.MSG_ROUTE.getCode());
        
        // 根据业务类型路由到适当的处理器
        switch (msgRoute) {
            case "user.registration":
                handleUserRegistration(message.getValue());
                break;
            case "order.payment":
                handleOrderPayment(message.getValue());
                break;
            case "inventory.update":
                handleInventoryUpdate(message.getValue());
                break;
            default:
                logger.warn("未知消息路由: {}", msgRoute);
        }
    }
    
    private void handleUserRegistration(UserRegistrationEvent event) {
        // 用户注册逻辑
    }
    
    private void handleOrderPayment(OrderPaymentEvent event) {
        // 订单支付逻辑
    }
    
    private void handleInventoryUpdate(InventoryUpdateEvent event) {
        // 库存更新逻辑
    }
}
```

### 动态路由配置

```java
@Service
public class DynamicRouteService {
    
    @Autowired
    private PulsarTemplate pulsarTemplate;
    
    public void sendWithDynamicRoute(String businessType, Object message) {
        // 根据业务类型设置动态路由
        String route = determineRoute(businessType);
        MsgContext.setMsgRoute(route);
        
        // 发送消息
        pulsarTemplate.send("dynamic-events", message);
        
        MsgContext.remove();
    }
    
    private String determineRoute(String businessType) {
        // 动态路由确定逻辑
        switch (businessType) {
            case "HIGH_PRIORITY": return "priority.process";
            case "NORMAL": return "normal.process";
            case "BATCH": return "batch.process";
            default: return "default.process";
        }
    }
}
```

## 实际应用场景

### 多租户电商平台

```java
@Component
public class EcommerceMessageHandler {
    
    @Autowired
    private TenantService tenantService;
    
    // 处理带租户上下文的订单创建
    @PulsarListener(
        topic = "ecommerce.orders",
        subscription = "order-processor",
        msgRoute = "order.created"
    )
    public void handleOrderCreation(OrderEvent order) {
        // 租户上下文由拦截器自动设置
        String tenantId = MsgContext.getCorpKey();
        
        // 使用租户特定逻辑处理订单
        orderService.processOrder(order, tenantId);
        
        // 向客户发送通知
        Notification notification = createOrderNotification(order);
        sendNotification(notification);
    }
    
    // 处理带租户路由的支付事件
    @PulsarListener(
        topic = "ecommerce.payments",
        subscription = "payment-processor",
        msgRoute = "payment.success"
    )
    public void handlePaymentSuccess(PaymentEvent payment) {
        String tenantId = MsgContext.getCorpKey();
        
        // 更新订单状态
        orderService.updateOrderStatus(payment.getOrderId(), "PAID", tenantId);
        
        // 触发履约流程
        fulfillmentService.startFulfillment(payment.getOrderId(), tenantId);
    }
    
    private void sendNotification(Notification notification) {
        // 为通知设置租户上下文
        MsgContext.setCorpKey(MsgContext.getCorpKey());
        MsgContext.setMsgRoute("notification.send");
        
        pulsarTemplate.send("notifications", notification);
        
        MsgContext.remove();
    }
}
```

### 基于路由的微服务通信

```java
@Component
public class MicroserviceMessageRouter {
    
    // 基于业务类型在微服务之间路由消息
    @PulsarListener(
        topic = "microservice.events",
        subscription = "event-router",
        multiRoute = true
    )
    public void routeMicroserviceEvents(Message<ServiceEvent> message) {
        String msgRoute = message.getProperties().get(MsgMetaKey.MSG_ROUTE.getCode());
        String tenantId = message.getProperties().get(MsgMetaKey.CORP.getCode());
        
        // 为处理设置租户上下文
        MsgContext.setCorpKey(tenantId);
        
        try {
            // 路由到适当的微服务处理器
            switch (msgRoute) {
                case "user.service.create":
                    userService.createUser(message.getValue());
                    break;
                case "product.service.update":
                    productService.updateProduct(message.getValue());
                    break;
                case "inventory.service.reserve":
                    inventoryService.reserveInventory(message.getValue());
                    break;
                default:
                    logger.warn("未处理的消息路由: {}", msgRoute);
            }
        } finally {
            MsgContext.remove();
        }
    }
}
```

## 消息路由和拦截器的最佳实践

### 1. 一致的路由命名约定

使用分层命名约定来命名消息路由：

```java
// 良好的路由命名
MsgContext.setMsgRoute("order.payment.success");
MsgContext.setMsgRoute("user.registration.completed");
MsgContext.setMsgRoute("inventory.stock.updated");

// 避免模糊命名
MsgContext.setMsgRoute("payment"); // 太模糊
MsgContext.setMsgRoute("user_registration_completed"); // 格式不一致
```

### 2. 租户上下文管理

```java
@Component
public class TenantAwareInterceptor extends MetaMessageInterceptor {
    
    @Override
    public void buildSendContext() {
        // 有可用时始终包含租户上下文
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            MsgContext.setCorpKey(currentTenant);
        }
    }
    
    @Override
    public boolean buildReceiveContext(String corpKey) {
        // 处理前验证租户
        if (corpKey == null || corpKey.isEmpty()) {
            logger.warn("消息中缺少租户上下文");
            return false; // 拒绝没有租户上下文的消息
        }
        
        // 切换到适当的租户
        return tenantService.switchTenant(corpKey);
    }
}
```

### 3. 拦截器中的错误处理

```java
@Component
public class ErrorHandlingInterceptor implements PulsarMessageInterceptor {
    
    @Override
    public Object beforeSend(String topic, Object message) {
        try {
            // 发送前验证消息
            validateMessage(message);
            return message;
        } catch (ValidationException e) {
            logger.error("主题的消息验证失败: {}", topic, e);
            throw e; // 防止发送无效消息
        }
    }
    
    @Override
    public boolean beforeReceive(Message<?> message) {
        try {
            // 验证消息完整性
            validateMessageIntegrity(message);
            return true;
        } catch (CorruptedMessageException e) {
            logger.error("消息完整性检查失败", e);
            return false; // 跳过处理损坏的消息
        }
    }
    
    private void validateMessage(Object message) {
        // 自定义验证逻辑
        if (message == null) {
            throw new ValidationException("消息不能为空");
        }
    }
}
```

### 4. 性能监控拦截器

```java
@Component
public class PerformanceInterceptor implements PulsarMessageInterceptor {
    
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();
    private final MeterRegistry meterRegistry;
    
    public PerformanceInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
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
            
            // 记录指标
            meterRegistry.timer("pulsar.send.duration", "topic", topic)
                .record(duration, TimeUnit.MILLISECONDS);
            
            if (exception != null) {
                meterRegistry.counter("pulsar.send.errors", "topic", topic).increment();
            }
            
            startTime.remove();
        }
    }
    
    @Override
    public boolean beforeReceive(Message<?> message) {
        startTime.set(System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        Long start = startTime.get();
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            
            // 记录处理指标
            meterRegistry.timer("pulsar.process.duration", 
                "topic", message.getTopicName())
                .record(duration, TimeUnit.MILLISECONDS);
            
            if (exception != null) {
                meterRegistry.counter("pulsar.process.errors", 
                    "topic", message.getTopicName()).increment();
            }
            
            startTime.remove();
        }
    }
}
```

## 配置参考

### 完整的消息路由配置

```yaml
spring:
  pulsar:
    # 消息路由配置
    routing:
      enabled: true
      
      # 默认路由键字段名
      route-key: msgRoute
      
      # 租户上下文键字段名
      tenant-key: corpKey
      
      # 启用多路由支持
      multi-route: true
    
    # 拦截器配置
    interceptor:
      enabled: true
      
      # 自定义拦截器（由Spring自动检测）
      custom-interceptors:
        - com.example.TenantContextInterceptor
        - com.example.AuditLoggingInterceptor
        - com.example.PerformanceInterceptor
      
      # 拦截器执行顺序
      order:
        tenant-context: 1
        audit-logging: 2
        performance: 3
```

### 带路由的高级生产者配置

```yaml
spring:
  pulsar:
    producer-map:
      # 带路由支持的订单生产者
      order-producer:
        topic: persistent://public/default/order-events
        send-timeout: 30s
        batching-enabled: true
        
        # 路由配置
        route-key: orderType
        tenant-key: tenantId
      
      # 通知生产者
      notification-producer:
        topic: persistent://public/default/notifications
        send-timeout: 15s
        batching-enabled: false
        
        # 路由配置
        route-key: notificationType
        tenant-key: corpKey
```

### 带路由的高级消费者配置

```yaml
spring:
  pulsar:
    consumer-map:
      # 多路由消费者
      multi-route-consumer:
        topic: persistent://public/default/business-events
        subscription-name: multi-route-subscription
        subscription-type: Shared
        
        # 路由配置
        route-key: businessPath
        tenant-key: corpKey
        multi-route: true
        
        # 处理配置
        receiver-queue-size: 1000
        ack-timeout: 60s
        retry-time: 3
      
      # 租户特定消费者
      tenant-consumer:
        topic: persistent://public/default/tenant-events
        subscription-name: tenant-subscription
        subscription-type: Exclusive
        
        # 路由配置
        route-key: eventType
        tenant-key: tenantId
        
        # 租户特定处理
        tenant-aware: true
```

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

在 Spring Boot 应用主类上添加 `@EnablePulsar` 注解：

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
    admin-url: http://localhost:8080
    
    # 生产者配置
    producer:
      topic: my-default-topic
      send-timeout: 30s
      batching-enabled: true
    
    # 消费者配置
    consumer:
      subscription-name: my-subscription
      subscription-type: Shared
      ack-timeout: 30s
    
    # 认证配置
    authentication:
      enabled: false
      # token: your-jwt-token
    
    # 重试配置
    retry:
      enabled: true
      max-retries: 3
      initial-delay: 1s
    
    # 死信队列配置
    dead-letter:
      enabled: false
    
    # 健康检查配置
    health:
      enabled: true
```

## 配置详解

### 基础配置

```yaml
spring:
  pulsar:
    # 是否启用 Pulsar 功能
    enabled: true
    
    # Pulsar 服务地址，支持多个地址用逗号分隔
    service-url: pulsar://localhost:6650
    
    # Pulsar 管理接口地址
    admin-url: http://localhost:8080
```

### 认证配置

```yaml
spring:
  pulsar:
    authentication:
      # 是否启用认证
      enabled: false
      
      # JWT 认证令牌
      token: eyJhbGciOiJIUzI1NiJ9...
      
      # 认证插件类名
      auth-plugin-class-name: org.apache.pulsar.client.impl.auth.AuthenticationToken
      
      # 认证参数
      auth-params:
        key1: value1
        key2: value2
```

### 生产者配置

#### 单个生产者配置

```yaml
spring:
  pulsar:
    producer:
      # 生产者主题
      topic: my-default-topic
      
      # 发送超时时间
      send-timeout: 30s
      
      # 队列满时是否阻塞
      block-if-queue-full: false
      
      # 最大待发送消息数
      max-pending-messages: 1000
      
      # 跨分区最大待发送消息数
      max-pending-messages-across-partitions: 50000
      
      # 批量发送配置
      batching-enabled: true
      batching-max-messages: 1000
      batching-max-publish-delay: 10ms
      batching-max-bytes: 131072  # 128KB
      
      # 压缩类型：NONE, LZ4, ZLIB, ZSTD, SNAPPY
      compression-type: NONE
      
      # 路由模式：RoundRobinPartition, SinglePartition, CustomPartition
      routing-mode: RoundRobinPartition
```

#### 多个生产者配置

```yaml
spring:
  pulsar:
    # 注意：producer-map 与单个 producer 配置不能同时使用
    producer-map:
      # 订单生产者
      order-producer:
        topic: persistent://public/default/order-topic
        send-timeout: 60s
        block-if-queue-full: true
        max-pending-messages: 500
        batching-enabled: false
        compression-type: LZ4
        routing-mode: SinglePartition
      
      # 用户事件生产者
      user-event-producer:
        topic: persistent://public/default/user-event-topic
        send-timeout: 30s
        block-if-queue-full: false
        max-pending-messages: 2000
        batching-enabled: true
        batching-max-messages: 500
        batching-max-publish-delay: 50ms
        compression-type: ZLIB
        routing-mode: RoundRobinPartition
      
      # 通知生产者
      notification-producer:
        topic: persistent://public/default/notification-topic
        send-timeout: 15s
        max-pending-messages: 5000
        batching-enabled: true
        batching-max-messages: 1000
        batching-max-publish-delay: 20ms
        compression-type: SNAPPY
```

### 消费者配置

#### 单个消费者配置

```yaml
spring:
  pulsar:
    consumer:
      # 消费者主题
      topic: my-default-topic
      
      # 重试主题
      retry-topic: my-retry-topic
      
      # 死信主题
      dead-topic: my-dead-topic
      
      # 业务类型字段名
      business-key: businessPath
      
      # 订阅名称
      subscription-name: sub1
      
      # 死信主题订阅名称
      dead-topic-subscription-name: sub1
      
      # 订阅类型：Exclusive, Shared, Failover, Key_Shared
      subscription-type: Shared
      
      # 订阅初始位置：Earliest, Latest
      subscription-initial-position: Earliest
      
      # 是否自动确认消息
      auto-ack: true
      
      # 最大重试次数
      retry-time: 3
      
      # 消息确认超时时间
      ack-timeout: 30s
      
      # 确认超时检查间隔
      ack-timeout-tick-time: 1s
      
      # 负确认重新投递延迟（毫秒）
      negative-ack-redelivery-delay: 1000
      
      # 消息重新消费延迟时间（毫秒）
      time-to-reconsume-delay: 1000
      
      # 接收队列大小
      receiver-queue-size: 1000
      
      # 跨分区接收队列最大总大小
      max-total-receiver-queue-size-across-partitions: 50000
      
      # 消费者名称
      consumer-name: my-consumer
      
      # 是否读取压缩主题
      read-compacted: false
      
      # 模式自动发现周期
      pattern-auto-discovery-period: 1m
      
      # 消费者优先级
      priority-level: 0
      
      # 加密失败处理：FAIL, DISCARD, CONSUME
      crypto-failure-action: FAIL
      
      # 最大待处理分块消息数
      max-pending-chunked-message: 10
      
      # 队列满时是否自动确认最旧的分块消息
      auto-ack-oldest-chunked-message-on-queue-full: false
      
      # 不完整分块消息过期时间
      expire-time-of-incomplete-chunked-message: 1m
```

#### 多个消费者配置

```yaml
spring:
  pulsar:
    # 注意：consumer-map 与单个 consumer 配置不能同时使用
    consumer-map:
      # 订单消费者
      order-consumer:
        topic: persistent://public/default/order-topic
        subscription-name: order-subscription
        subscription-type: Exclusive  # 订单消息需要保证顺序
        subscription-initial-position: Earliest
        ack-timeout: 60s
        receiver-queue-size: 500
        business-key: orderType
        retry-time: 5
        auto-ack: false  # 手动确认
      
      # 用户消费者
      user-consumer:
        topic: persistent://public/default/user-topic
        subscription-name: user-subscription
        subscription-type: Shared  # 用户消息可以并行处理
        subscription-initial-position: Latest
        ack-timeout: 30s
        receiver-queue-size: 1000
        business-key: userAction
        retry-time: 3
        auto-ack: true  # 自动确认
      
      # 通知消费者
      notification-consumer:
        topic: persistent://public/default/notification-topic
        subscription-name: notification-subscription
        subscription-type: Key_Shared  # 按用户ID分组处理通知
        subscription-initial-position: Latest
        ack-timeout: 15s
        receiver-queue-size: 2000
        priority-level: 1  # 通知优先级较高
        business-key: userId
        retry-time: 2
        auto-ack: true
      
      # 日志消费者
      log-consumer:
        topic: persistent://public/default/log-topic
        subscription-name: log-subscription
        subscription-type: Shared
        subscription-initial-position: Latest
        ack-timeout: 10s
        receiver-queue-size: 5000  # 日志量大，增大队列
        business-key: logLevel
        retry-time: 1  # 日志消息重试次数少
        auto-ack: true
        read-compacted: true  # 日志可能需要读取压缩主题
```

### 客户端配置

```yaml
spring:
  pulsar:
    client:
      # 超时配置
      operation-timeout: 30s
      connection-timeout: 10s
      request-timeout: 60s
      
      # 线程配置
      num-io-threads: 1
      num-listener-threads: 1
      
      # 连接配置
      connections-per-broker: 1
      use-tcp-no-delay: true
      keep-alive-interval: 30s
      
      # TLS配置
      tls-trust-certs-file-path: /path/to/certs
      tls-allow-insecure-connection: false
      tls-hostname-verification-enable: false
      
      # 查找配置
      concurrent-lookup-request: 5000
      max-lookup-request: 50000
      max-lookup-redirects: 20
      max-number-of-rejected-request-per-connection: 50
```

### 重试机制配置

```yaml
spring:
  pulsar:
    retry:
      # 是否启用重试机制
      enabled: true
      
      # 最大重试次数
      max-retries: 3
      
      # 初始重试延迟
      initial-delay: 1s
      
      # 重试延迟倍数
      multiplier: 2.0
      
      # 最大重试延迟
      max-delay: 30s
      
      # 是否使用随机延迟
      use-random-delay: false
```

### 事务配置

```yaml
spring:
  pulsar:
    transaction:
      # 是否启用事务
      enabled: false
      
      # 事务协调器主题
      coordinator-topic: persistent://pulsar/system/transaction_coordinator_assign
      
      # 事务超时时间
      timeout: 1m
      
      # 事务缓冲区快照段大小（1MB）
      buffer-snapshot-segment-size: 1048576
      
      # 事务缓冲区快照最小时间间隔
      buffer-snapshot-min-time-in-millis: 5s
      
      # 事务缓冲区快照最大事务数
      buffer-snapshot-max-transaction-count: 1000
      
      # 事务日志存储大小（1GB）
      log-store-size: 1073741824
```

### 死信队列配置

```yaml
spring:
  pulsar:
    dead-letter:
      # 是否启用死信队列
      enabled: false
      
      # 死信队列主题后缀
      topic-suffix: "-DLQ"
      
      # 最大重试次数
      max-retries: 3
      
      # 重试策略配置
      retry:
        # 是否启用智能重试策略
        smart-strategy-enabled: true
        
        # 基础重试延迟
        base-delay: 1s
        
        # 最大重试延迟
        max-delay: 5m
        
        # 重试时间窗口
        retry-window: 24h
        
        # 是否启用抖动
        jitter-enabled: true
        
        # 抖动因子（0.0-1.0）
        jitter-factor: 0.2
      
      # 清理配置
      cleanup:
        # 是否启用自动清理
        auto-cleanup-enabled: true
        
        # 消息过期时间
        message-expiration: 7d
        
        # 清理执行时间（每天凌晨2点）
        cleanup-cron: "0 0 2 * * ?"
        
        # 重试信息过期时间
        retry-info-expiration: 24h
      
      # 监控配置
      monitoring:
        # 是否启用监控
        enabled: true
        
        # 监控间隔
        monitoring-interval: 5m
        
        # 健康检查超时时间
        health-check-timeout: 30s
        
        # 是否启用告警
        alert-enabled: false
        
        # 告警阈值（死信消息数量）
        alert-threshold: 100
      
      # 统计配置
      statistics:
        # 是否启用统计
        enabled: true
        
        # 统计数据保留时间
        retention-period: 30d
        
        # 是否启用详细统计
        detailed-enabled: false
```

### 健康检查配置

```yaml
spring:
  pulsar:
    health:
      # 是否启用健康检查
      enabled: true
      
      # 健康检查超时时间
      timeout: 10s
```

### 拦截器配置

```yaml
spring:
  pulsar:
    interceptor:
      # 是否启用消息拦截器
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
        System.out.println("消息已发送: " + messageId);
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

#### 使用多个生产者

```java
@Service
public class MultiProducerService {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    // 使用指定的生产者发送消息
    public void sendOrderMessage(Order order) {
        pulsarTemplate.send("order-producer", order);
    }

    public void sendUserEvent(UserEvent event) {
        pulsarTemplate.send("user-event-producer", event);
    }

    public void sendNotification(Notification notification) {
        pulsarTemplate.send("notification-producer", notification);
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
                    log.info("邮件已排队发送: {}", messageId);
                })
                .exceptionally(throwable -> {
                    log.error("邮件排队失败", throwable);
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

    // 简单消息监听器
    @PulsarListener(topic = "my-topic", subscription = "my-subscription")
    public void handleMessage(String message) {
        System.out.println("收到消息: " + message);
    }

    // 监听复杂对象
    @PulsarListener(
            topic = "user-events",
            subscription = "user-service",
            messageType = UserEvent.class
    )
    public void handleUserEvent(UserEvent event) {
        System.out.println("用户事件: " + event);
    }

    // 共享订阅模式
    @PulsarListener(
            topic = "shared-topic",
            subscription = "shared-subscription",
            subscriptionType = "Shared"
    )
    public void handleSharedMessage(String message) {
        // 多个消费者实例可以并行处理消息
        System.out.println("共享消息: " + message);
    }
}
```

#### 使用多个消费者

```java
@Component
public class MultiConsumerListener {

    // 使用指定的消费者配置
    @PulsarListener(consumerName = "order-consumer")
    public void handleOrderMessage(Order order) {
        log.info("处理订单消息: {}", order.getId());
        // 订单处理逻辑
    }

    @PulsarListener(consumerName = "user-consumer")
    public void handleUserMessage(UserEvent event) {
        log.info("处理用户事件: {}", event.getType());
        // 用户事件处理逻辑
    }

    @PulsarListener(consumerName = "notification-consumer")
    public void handleNotification(Notification notification) {
        log.info("处理通知: {}", notification.getTitle());
        // 通知处理逻辑
    }
}
```

#### 业务场景示例

```java
@Component
public class OrderEventListener {

    @PulsarListener(topic = "order-events", subscription = "order-processor")
    public void handleOrderEvent(Order order) {
        log.info("处理订单: {}", order.getId());

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
            log.error("支付处理失败", e);
            throw e; // 触发重试机制
        }
    }
}
```

## 消息拦截器

### 拦截器接口

消息拦截器提供在消息发送和接收过程中进行拦截处理的能力：

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
        logger.info("发送消息到主题: {}, 消息: {}", topic, message);
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        if (exception != null) {
            logger.error("消息发送失败到主题: {}", topic, exception);
        } else {
            logger.info("消息发送成功到主题: {}, 消息ID: {}", topic, messageId);
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        logger.info("接收消息: {}", message.getMessageId());
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
            log.warn("忽略黑名单主题的消息: {}", topic);
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

## 死信队列

### 死信队列处理器

```java
@Component
public class CustomDeadLetterHandler implements DeadLetterQueueHandler {

    @Override
    public void handleDeadLetter(String originalTopic, Message<?> message, Exception exception) {
        // 自定义死信处理逻辑
        System.out.println("死信来自主题: " + originalTopic);
        System.out.println("消息ID: " + message.getMessageId());
        System.out.println("错误: " + exception.getMessage());

        // 保存到数据库以便后续分析
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
                    "关键消息处理失败",
                    String.format("主题: %s, 错误: %s", originalTopic, exception.getMessage())
            );
        }
    }

    @Override
    public int getMaxRetries() {
        return 5; // 自定义最大重试次数
    }
}
```

## 事务支持

### 事务使用

#### 基于注解（推荐）

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

        // 如果方法正常完成，事务将自动提交
        // 如果抛出异常，事务将自动回滚
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
                throw new RuntimeException("事务失败", e);
            }
        });
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

#### 生产环境配置

```yaml
spring:
  pulsar:
    enabled: true
    service-url: pulsar://pulsar-cluster:6650
    admin-url: http://pulsar-cluster:8080

    # 生产者配置
    producer:
      send-timeout: 10s
      max-pending-messages: 2000
      max-pending-messages-across-partitions: 100000
      batching-enabled: true
      batching-max-messages: 1000
      batching-max-publish-delay: 5ms
      batching-max-bytes: 262144  # 256KB
      compression-type: LZ4
      routing-mode: RoundRobinPartition

    # 消费者配置
    consumer:
      subscription-name: ${spring.application.name}-${HOSTNAME:unknown}
      subscription-type: Shared
      subscription-initial-position: Latest
      receiver-queue-size: 2000
      max-total-receiver-queue-size-across-partitions: 100000
      ack-timeout: 60s
      ack-timeout-tick-time: 1s
      negative-ack-redelivery-delay: 2000
      time-to-reconsume-delay: 2000
      retry-time: 5
      priority-level: 0

    # 客户端配置
    client:
      operation-timeout: 60s
      connection-timeout: 30s
      request-timeout: 120s
      num-io-threads: 2
      num-listener-threads: 2
      connections-per-broker: 2
      concurrent-lookup-request: 10000
      max-lookup-request: 100000
      keep-alive-interval: 30s

    # 认证配置
    authentication:
      enabled: true
      token: ${PULSAR_JWT_TOKEN}
      auth-plugin-class-name: org.apache.pulsar.client.impl.auth.AuthenticationToken

    # 重试配置
    retry:
      enabled: true
      max-retries: 5
      initial-delay: 2s
      multiplier: 2.0
      max-delay: 60s
      use-random-delay: true

    # 死信队列配置
    dead-letter:
      enabled: true
      max-retries: 5
      retry:
        smart-strategy-enabled: true
        base-delay: 2s
        max-delay: 10m
        retry-window: 24h
        jitter-enabled: true
        jitter-factor: 0.1
      cleanup:
        auto-cleanup-enabled: true
        message-expiration: 30d
        cleanup-cron: "0 0 2 * * ?"
        retry-info-expiration: 7d
      monitoring:
        enabled: true
        monitoring-interval: 5m
        alert-enabled: true
        alert-threshold: 200
      statistics:
        enabled: true
        retention-period: 90d
        detailed-enabled: true

    # 健康检查配置
    health:
      enabled: true
      timeout: 30s

    # 事务配置
    transaction:
      enabled: false  # 根据需要启用
      timeout: 2m
      buffer-snapshot-segment-size: 2097152  # 2MB
      buffer-snapshot-max-transaction-count: 2000

logging:
  level:
    com.github.spring.mq.pulsar: INFO
    org.apache.pulsar: WARN
```

## 监控和健康检查

### 健康检查

Starter 提供内置的健康检查功能：

```java

@Autowired
private PulsarHealthIndicator healthIndicator;

public void checkHealth() {
    Map<String, Object> health = healthIndicator.health();
    System.out.println("Pulsar 健康状态: " + health);
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
// 良好的消息设计
public class OrderEvent {
    private String eventId;        // 唯一事件ID
    private String eventType;      // 事件类型
    private long timestamp;        // 时间戳
    private String orderId;        // 业务ID
    private Map<String, Object> data; // 事件数据
    // 构造函数、getter、setter
}

// 避免这样的消息设计
public class BadOrderEvent {
    private Order order; // 包含过多信息
    // 缺少事件元数据
}
```

### 2. 主题命名

使用有意义的主题名称，建议使用层次结构：

- `app.service.event` - 应用.服务.事件
- `order.payment.success` - 订单.支付.成功
- `user.registration.completed` - 用户.注册.完成

### 3. 订阅模式选择

- **Exclusive**: 单个消费者，保证消息顺序
- **Shared**: 多个消费者负载均衡，提高吞吐量
- **Failover**: 主备模式，高可用性
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
        log.error("订单业务处理错误: {}", order.getId(), e);
        // 不抛出异常以避免重试
    } catch (Exception e) {
        // 系统异常，可以重试
        log.error("订单系统处理错误: {}", order.getId(), e);
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
                .thenRun(() -> log.info("所有消息发送成功"))
                .exceptionally(throwable -> {
                    log.error("部分消息发送失败", throwable);
                    return null;
                });
    }
}
```

## 故障排除

### 常见问题

1. **连接失败**: 检查 Pulsar 服务是否正常运行，网络是否可达
2. **认证失败**: 确认认证配置是否正确
3. **消息丢失**: 检查消息确认机制是否正确实现
4. **性能问题**: 调整批处理、连接池等配置参数
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
    log.debug("事务ID: {}", tx.getTxnID());
    log.debug("事务状态: {}", tx.getState());
}
```

## 注意事项

1. **Pulsar 版本要求**: 确保使用支持事务的 Pulsar 版本（2.7.0+）
2. **集群配置**: Pulsar 集群需要启用事务协调器
3. **性能影响**: 事务和拦截器会带来一定的性能开销，请根据业务需求权衡使用
4. **错误恢复**: 正确处理事务失败和重试逻辑
5. **资源管理**: 及时释放事务资源，避免资源泄漏

## 版本要求

- Java 17+
- Spring Boot 3.0+
- Apache Pulsar 3.2.4+

## 贡献

我们欢迎贡献！请查看我们的 [贡献指南](CONTRIBUTING.md) 了解详情。

在贡献之前，请阅读我们的 [行为准则](CODE_OF_CONDUCT.md)。

## 变更日志

查看 [CHANGELOG.md](CHANGELOG.md) 了解变更列表和版本历史。

## 许可证

本项目采用 [MIT 许可证](LICENSE)。

## 增强功能总结

### 消息路由能力

增强的消息路由功能提供：

- **业务类型分类**：使用 `msgRoute` 对消息按业务类型进行分类
- **动态处理器路由**：根据业务类型自动将消息路由到适当的处理器
- **多路由支持**：单个监听器可以通过 `multiRoute=true` 处理多个业务路由
- **上下文传播**：路由信息通过消息元数据自动传播

### 消息拦截器增强

增强的拦截器系统支持：

- **租户上下文管理**：基于消息元数据的自动租户切换
- **自定义拦截器链**：灵活的拦截器排序和执行
- **性能监控**：内置消息处理指标收集
- **错误处理**：优雅的错误处理和恢复机制

### 主要优势

1. **简化的多租户架构**：自动租户上下文传播减少样板代码
2. **灵活的消息处理**：基于路由的处理支持复杂的业务逻辑场景
3. **增强的可观测性**：全面的监控和日志记录能力
4. **生产就绪**：强大的错误处理和恢复机制

## 功能对比

| 功能 | 基础 Pulsar | 增强版 Starter |
|------|------------|----------------|
| 消息路由 | 手动实现 | 自动基于路由的路由 |
| 租户上下文 | 手动上下文管理 | 自动租户切换 |
| 拦截器支持 | 有限 | 全面的拦截器链 |
| 多路由处理 | 不支持 | 内置多路由支持 |
| 性能监控 | 手动实现 | 内置指标收集 |
| 错误处理 | 基础重试机制 | 带死信队列的高级错误处理 |

## 使用增强功能入门

### 1. 启用增强路由

```yaml
spring:
  pulsar:
    routing:
      enabled: true
    interceptor:
      enabled: true
```

### 2. 实现自定义拦截器

```java
@Component
public class MyCustomInterceptor extends MetaMessageInterceptor {
    // 您的自定义拦截器逻辑
}
```

### 3. 配置基于路由的监听器

```java
@PulsarListener(
    topic = "my-topic",
    msgRoute = "my.business.route",
    multiRoute = true
)
public void handleMessage(MyMessage message) {
    // 基于路由的消息处理
}
```

## 支持和社区

- **问题反馈**：[GitHub Issues](https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/issues)
- **文档**：[GitHub Wiki](https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/wiki)
- **贡献指南**：请阅读我们的[贡献指南](CONTRIBUTING.md)

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

---

**注意**：此starter正在积极维护中，欢迎社区贡献。如有任何问题或建议，请在GitHub上提交issue。