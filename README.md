# Seven Spring MQ Pulsar Starter

[![Build Status](https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/actions/workflows/test.yml/badge.svg)](https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/actions/workflows/test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.qwzhang01/seven-spring-mq-pulsar-starter.svg)](https://central.sonatype.com/artifact/io.github.qwzhang01/seven-spring-mq-pulsar-starter)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0%2B-green.svg)](https://spring.io/projects/spring-boot)
[![Apache Pulsar](https://img.shields.io/badge/Apache%20Pulsar-3.2.4%2B-blue.svg)](https://pulsar.apache.org/)
[![codecov](https://codecov.io/gh/qwzhang01/seven-spring-mq-pulsar-starter/branch/main/graph/badge.svg)](https://codecov.io/gh/qwzhang01/seven-spring-mq-pulsar-starter)

[中文文档](README-zh.md)

A feature-rich, easy-to-use Spring Boot Pulsar Starter that provides a complete Pulsar integration solution.

## Summary of Enhanced Features

### Message Routing Capability

The enhanced message routing feature provides:

- **Business Type Classification**: Messages can be categorized by business types using `msgRoute`
- **Dynamic Handler Routing**: Messages are automatically routed to appropriate handlers based on their business type
- **Multi-Route Support**: Single listener can handle multiple business routes with `multiRoute=true`
- **Context Propagation**: Route information is automatically propagated through message metadata

### Message Interceptor Enhancement

The enhanced interceptor system supports:

- **Tenant Context Management**: Automatic tenant switching based on message metadata
- **Custom Interceptor Chains**: Flexible interceptor ordering and execution
- **Performance Monitoring**: Built-in metrics collection for message processing
- **Error Handling**: Graceful error handling and recovery mechanisms

### Key Benefits

1. **Simplified Multi-Tenant Architecture**: Automatic tenant context propagation reduces boilerplate code
2. **Flexible Message Processing**: Route-based processing enables complex business logic scenarios
3. **Enhanced Observability**: Comprehensive monitoring and logging capabilities
4. **Production-Ready**: Robust error handling and recovery mechanisms

## Feature Comparison

| Feature | Basic Pulsar | Enhanced Starter |
|---------|-------------|------------------|
| Message Routing | Manual implementation | Automatic route-based routing |
| Tenant Context | Manual context management | Automatic tenant switching |
| Interceptor Support | Limited | Comprehensive interceptor chain |
| Multi-Route Processing | Not supported | Built-in multi-route support |
| Performance Monitoring | Manual implementation | Built-in metrics collection |
| Error Handling | Basic retry mechanisms | Advanced error handling with DLQ |

## Getting Started with Enhanced Features

### 1. Enable Enhanced Routing

```yaml
spring:
  pulsar:
    routing:
      enabled: true
    interceptor:
      enabled: true
```

### 2. Implement Custom Interceptors

```java
@Component
public class MyCustomInterceptor extends MetaMessageInterceptor {
    // Your custom interceptor logic
}
```

### 3. Configure Route-Based Listeners

```java
@PulsarListener(
    topic = "my-topic",
    msgRoute = "my.business.route",
    multiRoute = true
)
public void handleMessage(MyMessage message) {
    // Route-based message processing
}
```

## Support and Community

- **Issues**: [GitHub Issues](https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/issues)
- **Documentation**: [GitHub Wiki](https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/wiki)
- **Contributing**: Please read our [Contributing Guide](CONTRIBUTING.md)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Note**: This starter is actively maintained and welcomes contributions from the community. For any questions or suggestions, please open an issue on GitHub.

## Features

- 🚀 **Easy to Use**: Enable Pulsar functionality with a single `@EnablePulsar` annotation
- 🔧 **Flexible Configuration**: Support flexible control of various functions through configuration files
- 📨 **Message Sending**: Provide synchronous/asynchronous message sending, support delayed messages and transactional messages
- 👂 **Message Listening**: Easily create message listeners through `@PulsarListener` annotation
- 🔄 **Retry Mechanism**: Built-in message processing failure retry mechanism with exponential backoff support
- 💀 **Dead Letter Queue**: Automatically handle retry-failed messages to dead letter queue
- 🔍 **Message Interception**: Support custom message interceptors for logging, monitoring, etc.
- 💊 **Health Check**: Built-in Pulsar connection health check
- 🎯 **Transaction Support**: Support Pulsar transactional messages
- 🗺️ **Multi-Instance Configuration**: Support configuring multiple producer and consumer instances
- 🛣️ **Message Routing**: Enhanced branch processing capability by message type/business type, using `msgRoute` to mark business types in metadata
- 🔐 **Tenant Context**: Enhanced message consumption interceptors supporting tenant information initialization based on message metadata

## Practical Application Scenarios

### Multi-Tenant E-commerce Platform

```java
@Component
public class EcommerceMessageHandler {
    
    @Autowired
    private TenantService tenantService;
    
    // Handle order creation with tenant context
    @PulsarListener(
        topic = "ecommerce.orders",
        subscription = "order-processor",
        msgRoute = "order.created"
    )
    public void handleOrderCreation(OrderEvent order) {
        // Tenant context is automatically set by interceptor
        String tenantId = MsgContext.getCorpKey();
        
        // Process order with tenant-specific logic
        orderService.processOrder(order, tenantId);
        
        // Send notification to customer
        Notification notification = createOrderNotification(order);
        sendNotification(notification);
    }
    
    // Handle payment events with tenant routing
    @PulsarListener(
        topic = "ecommerce.payments",
        subscription = "payment-processor",
        msgRoute = "payment.success"
    )
    public void handlePaymentSuccess(PaymentEvent payment) {
        String tenantId = MsgContext.getCorpKey();
        
        // Update order status
        orderService.updateOrderStatus(payment.getOrderId(), "PAID", tenantId);
        
        // Trigger fulfillment process
        fulfillmentService.startFulfillment(payment.getOrderId(), tenantId);
    }
    
    private void sendNotification(Notification notification) {
        // Set tenant context for notification
        MsgContext.setCorpKey(MsgContext.getCorpKey());
        MsgContext.setMsgRoute("notification.send");
        
        pulsarTemplate.send("notifications", notification);
        
        MsgContext.remove();
    }
}
```

### Microservices Communication with Route-Based Processing

```java
@Component
public class MicroserviceMessageRouter {
    
    // Route messages between microservices based on business type
    @PulsarListener(
        topic = "microservice.events",
        subscription = "event-router",
        multiRoute = true
    )
    public void routeMicroserviceEvents(Message<ServiceEvent> message) {
        String msgRoute = message.getProperties().get(MsgMetaKey.MSG_ROUTE.getCode());
        String tenantId = message.getProperties().get(MsgMetaKey.CORP.getCode());
        
        // Set tenant context for processing
        MsgContext.setCorpKey(tenantId);
        
        try {
            // Route to appropriate microservice handler
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
                    logger.warn("Unhandled message route: {}", msgRoute);
            }
        } finally {
            MsgContext.remove();
        }
    }
}
```

## Best Practices for Message Routing and Interceptors

### 1. Consistent Route Naming Convention

Use a hierarchical naming convention for message routes:

```java
// Good route naming
MsgContext.setMsgRoute("order.payment.success");
MsgContext.setMsgRoute("user.registration.completed");
MsgContext.setMsgRoute("inventory.stock.updated");

// Avoid ambiguous naming
MsgContext.setMsgRoute("payment"); // Too vague
MsgContext.setMsgRoute("user_registration_completed"); // Inconsistent format
```

### 2. Tenant Context Management

```java
@Component
public class TenantAwareInterceptor extends MetaMessageInterceptor {
    
    @Override
    public void buildSendContext() {
        // Always include tenant context when available
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            MsgContext.setCorpKey(currentTenant);
        }
    }
    
    @Override
    public boolean buildReceiveContext(String corpKey) {
        // Validate tenant before processing
        if (corpKey == null || corpKey.isEmpty()) {
            logger.warn("Missing tenant context in message");
            return false; // Reject messages without tenant context
        }
        
        // Switch to appropriate tenant
        return tenantService.switchTenant(corpKey);
    }
}
```

### 3. Error Handling in Interceptors

```java
@Component
public class ErrorHandlingInterceptor implements PulsarMessageInterceptor {
    
    @Override
    public Object beforeSend(String topic, Object message) {
        try {
            // Validate message before sending
            validateMessage(message);
            return message;
        } catch (ValidationException e) {
            logger.error("Message validation failed for topic: {}", topic, e);
            throw e; // Prevent invalid messages from being sent
        }
    }
    
    @Override
    public boolean beforeReceive(Message<?> message) {
        try {
            // Validate message integrity
            validateMessageIntegrity(message);
            return true;
        } catch (CorruptedMessageException e) {
            logger.error("Message integrity check failed", e);
            return false; // Skip processing corrupted messages
        }
    }
    
    private void validateMessage(Object message) {
        // Custom validation logic
        if (message == null) {
            throw new ValidationException("Message cannot be null");
        }
    }
}
```

### 4. Performance Monitoring Interceptor

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
            
            // Record metrics
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
            
            // Record processing metrics
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

## Configuration Reference

### Complete Message Routing Configuration

```yaml
spring:
  pulsar:
    # Message routing configuration
    routing:
      enabled: true
      
      # Default route key field name
      route-key: msgRoute
      
      # Tenant context key field name
      tenant-key: corpKey
      
      # Enable multi-route support
      multi-route: true
    
    # Interceptor configuration
    interceptor:
      enabled: true
      
      # Custom interceptors (auto-detected by Spring)
      custom-interceptors:
        - com.example.TenantContextInterceptor
        - com.example.AuditLoggingInterceptor
        - com.example.PerformanceInterceptor
      
      # Interceptor execution order
      order:
        tenant-context: 1
        audit-logging: 2
        performance: 3
```

### Advanced Producer Configuration with Routing

```yaml
spring:
  pulsar:
    producer-map:
      # Order producer with routing support
      order-producer:
        topic: persistent://public/default/order-events
        send-timeout: 30s
        batching-enabled: true
        
        # Route configuration
        route-key: orderType
        tenant-key: tenantId
      
      # Notification producer
      notification-producer:
        topic: persistent://public/default/notifications
        send-timeout: 15s
        batching-enabled: false
        
        # Route configuration
        route-key: notificationType
        tenant-key: corpKey
```

### Advanced Consumer Configuration with Route-Based Processing

```yaml
spring:
  pulsar:
    consumer-map:
      # Multi-route consumer
      multi-route-consumer:
        topic: persistent://public/default/business-events
        subscription-name: multi-route-subscription
        subscription-type: Shared
        
        # Route configuration
        route-key: businessPath
        tenant-key: corpKey
        multi-route: true
        
        # Processing configuration
        receiver-queue-size: 1000
        ack-timeout: 60s
        retry-time: 3
      
      # Tenant-specific consumer
      tenant-consumer:
        topic: persistent://public/default/tenant-events
        subscription-name: tenant-subscription
        subscription-type: Exclusive
        
        # Route configuration
        route-key: eventType
        tenant-key: tenantId
        
        # Tenant-specific processing
        tenant-aware: true
```

## Enhanced Message Routing Capability

### Business Type-Based Branch Processing

The starter provides enhanced message routing capabilities that allow you to:

1. **Mark business types in metadata** during message sending using `msgRoute`
2. **Route messages to appropriate handlers** based on business types during consumption
3. **Support multiple message processors** for different business scenarios

### Message Routing Configuration

```yaml
spring:
  pulsar:
    consumer:
      # Business type field name for routing
      business-key: businessPath
      
      # Message routing configuration
      msg-route: order.process
```

### Usage Examples

#### Sending Messages with Route Information

```java
@Service
public class OrderService {
    
    @Autowired
    private PulsarTemplate pulsarTemplate;
    
    public void sendOrderEvent(OrderEvent event) {
        // Set message route before sending
        MsgContext.setMsgRoute("order.created");
        
        // Send message - route info will be automatically included in metadata
        pulsarTemplate.send("order-events", event);
        
        // Clean up context
        MsgContext.remove();
    }
}
```

#### Receiving Messages with Route-Based Processing

```java
@Component
public class OrderEventListener {
    
    // Handle order creation events
    @PulsarListener(
        topic = "order-events",
        subscription = "order-processor",
        msgRoute = "order.created"
    )
    public void handleOrderCreated(OrderEvent event) {
        // Process order creation logic
        orderService.processNewOrder(event);
    }
    
    // Handle order cancellation events
    @PulsarListener(
        topic = "order-events", 
        subscription = "order-processor",
        msgRoute = "order.cancelled"
    )
    public void handleOrderCancelled(OrderEvent event) {
        // Process order cancellation logic
        orderService.processCancelledOrder(event);
    }
}
```

## Enhanced Message Consumption Interceptors

### Tenant Information Initialization

The starter provides powerful message consumption interceptors that support:

1. **Automatic tenant context propagation** based on message metadata
2. **Custom tenant switching logic** in interceptors
3. **Thread-local context management** for multi-tenant scenarios

### Interceptor Implementation

#### Custom Tenant Interceptor

```java
@Component
public class TenantContextInterceptor extends MetaMessageInterceptor {
    
    @Autowired
    private TenantService tenantService;
    
    @Override
    public void buildSendContext() {
        // Extract current tenant context and set it for message sending
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            MsgContext.setCorpKey(currentTenant);
        }
    }
    
    @Override
    public boolean buildReceiveContext(String corpKey) {
        // Switch tenant context based on message metadata
        if (corpKey != null && !corpKey.isEmpty()) {
            return tenantService.switchTenant(corpKey);
        }
        return true;
    }
    
    @Override
    public int getOrder() {
        return 1; // High priority for tenant context setup
    }
}
```

#### Audit Logging Interceptor

```java
@Component
public class AuditLoggingInterceptor implements PulsarMessageInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingInterceptor.class);
    
    @Override
    public Object beforeSend(String topic, Object message) {
        logger.info("Sending message to topic: {}, tenant: {}, message: {}", 
            topic, MsgContext.getCorpKey(), message);
        return message;
    }
    
    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        if (exception != null) {
            logger.error("Failed to send message to topic: {}, tenant: {}", 
                topic, MsgContext.getCorpKey(), exception);
        } else {
            logger.info("Successfully sent message to topic: {}, message ID: {}", 
                topic, messageId);
        }
    }
    
    @Override
    public boolean beforeReceive(Message<?> message) {
        String tenantId = message.getProperties().get(MsgMetaKey.CORP.getCode());
        logger.info("Receiving message from topic: {}, tenant: {}, message ID: {}", 
            message.getTopicName(), tenantId, message.getMessageId());
        return true;
    }
    
    @Override
    public int getOrder() {
        return 2; // Lower priority than tenant interceptor
    }
}
```

### Interceptor Configuration

```yaml
spring:
  pulsar:
    interceptor:
      # Enable message interceptors
      enabled: true
      
      # Custom interceptors (auto-detected by Spring)
      custom-interceptors:
        - com.example.TenantContextInterceptor
        - com.example.AuditLoggingInterceptor
```

## Advanced Message Routing Scenarios

### Multi-Route Message Processing

```java
@Component
public class MultiRouteMessageProcessor {
    
    // Process messages for multiple business routes
    @PulsarListener(
        topic = "business-events",
        subscription = "multi-route-processor",
        multiRoute = true
    )
    public void handleMultiRouteMessage(Message<BusinessEvent> message) {
        
        // Extract route from message properties
        String msgRoute = message.getProperties().get(MsgMetaKey.MSG_ROUTE.getCode());
        
        // Route to appropriate handler based on business type
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
                logger.warn("Unknown message route: {}", msgRoute);
        }
    }
    
    private void handleUserRegistration(UserRegistrationEvent event) {
        // User registration logic
    }
    
    private void handleOrderPayment(OrderPaymentEvent event) {
        // Order payment logic
    }
    
    private void handleInventoryUpdate(InventoryUpdateEvent event) {
        // Inventory update logic
    }
}
```

### Dynamic Route Configuration

```java
@Service
public class DynamicRouteService {
    
    @Autowired
    private PulsarTemplate pulsarTemplate;
    
    public void sendWithDynamicRoute(String businessType, Object message) {
        // Set dynamic route based on business type
        String route = determineRoute(businessType);
        MsgContext.setMsgRoute(route);
        
        // Send message
        pulsarTemplate.send("dynamic-events", message);
        
        MsgContext.remove();
    }
    
    private String determineRoute(String businessType) {
        // Dynamic route determination logic
        switch (businessType) {
            case "HIGH_PRIORITY": return "priority.process";
            case "NORMAL": return "normal.process";
            case "BATCH": return "batch.process";
            default: return "default.process";
        }
    }
}
```

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.qwzhang01</groupId>
    <artifactId>seven-spring-mq-pulsar-starter</artifactId>
    <version>${pulsar-spring.version}</version>
</dependency>
```

### 2. Enable Pulsar

Add the `@EnablePulsar` annotation to your Spring Boot application main class:

```java
@SpringBootApplication
@EnablePulsar
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. Basic Configuration

Configure Pulsar connection information in `application.yml`:

```yaml
spring:
  pulsar:
    enabled: true
    service-url: pulsar://localhost:6650
    admin-url: http://localhost:8080
    
    # Producer configuration
    producer:
      topic: my-default-topic
      send-timeout: 30s
      batching-enabled: true
    
    # Consumer configuration
    consumer:
      subscription-name: my-subscription
      subscription-type: Shared
      ack-timeout: 30s
    
    # Authentication configuration
    authentication:
      enabled: false
      # token: your-jwt-token
    
    # Retry configuration
    retry:
      enabled: true
      max-retries: 3
      initial-delay: 1s
    
    # Dead letter queue configuration
    dead-letter:
      enabled: false
    
    # Health check configuration
    health:
      enabled: true
```

## Configuration Details

### Basic Configuration

```yaml
spring:
  pulsar:
    # Whether to enable Pulsar functionality
    enabled: true
    
    # Pulsar service URL, supports multiple addresses separated by commas
    service-url: pulsar://localhost:6650
    
    # Pulsar admin interface URL
    admin-url: http://localhost:8080
```

### Authentication Configuration

```yaml
spring:
  pulsar:
    authentication:
      # Whether to enable authentication
      enabled: false
      
      # JWT authentication token
      token: eyJhbGciOiJIUzI1NiJ9...
      
      # Authentication plugin class name
      auth-plugin-class-name: org.apache.pulsar.client.impl.auth.AuthenticationToken
      
      # Authentication parameters
      auth-params:
        key1: value1
        key2: value2
```

### Producer Configuration

#### Single Producer Configuration

```yaml
spring:
  pulsar:
    producer:
      # Producer topic
      topic: my-default-topic
      
      # Send timeout
      send-timeout: 30s
      
      # Whether to block when queue is full
      block-if-queue-full: false
      
      # Maximum pending messages
      max-pending-messages: 1000
      
      # Maximum pending messages across partitions
      max-pending-messages-across-partitions: 50000
      
      # Batching configuration
      batching-enabled: true
      batching-max-messages: 1000
      batching-max-publish-delay: 10ms
      batching-max-bytes: 131072  # 128KB
      
      # Compression type: NONE, LZ4, ZLIB, ZSTD, SNAPPY
      compression-type: NONE
      
      # Routing mode: RoundRobinPartition, SinglePartition, CustomPartition
      routing-mode: RoundRobinPartition
```

#### Multiple Producers Configuration

```yaml
spring:
  pulsar:
    # Note: producer-map cannot be used together with single producer configuration
    producer-map:
      # Order producer
      order-producer:
        topic: persistent://public/default/order-topic
        send-timeout: 60s
        block-if-queue-full: true
        max-pending-messages: 500
        batching-enabled: false
        compression-type: LZ4
        routing-mode: SinglePartition
      
      # User event producer
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
      
      # Notification producer
      notification-producer:
        topic: persistent://public/default/notification-topic
        send-timeout: 15s
        max-pending-messages: 5000
        batching-enabled: true
        batching-max-messages: 1000
        batching-max-publish-delay: 20ms
        compression-type: SNAPPY
```

### Consumer Configuration

#### Single Consumer Configuration

```yaml
spring:
  pulsar:
    consumer:
      # Consumer topic
      topic: my-default-topic
      
      # Retry topic
      retry-topic: my-retry-topic
      
      # Dead letter topic
      dead-topic: my-dead-topic
      
      # Business type field name
      business-key: businessPath
      
      # Subscription name
      subscription-name: sub1
      
      # Dead letter topic subscription name
      dead-topic-subscription-name: sub1
      
      # Subscription type: Exclusive, Shared, Failover, Key_Shared
      subscription-type: Shared
      
      # Subscription initial position: Earliest, Latest
      subscription-initial-position: Earliest
      
      # Whether to auto-acknowledge messages
      auto-ack: true
      
      # Maximum retry times
      retry-time: 3
      
      # Message acknowledgment timeout
      ack-timeout: 30s
      
      # Acknowledgment timeout tick time
      ack-timeout-tick-time: 1s
      
      # Negative acknowledgment redelivery delay (milliseconds)
      negative-ack-redelivery-delay: 1000
      
      # Message reconsume delay time (milliseconds)
      time-to-reconsume-delay: 1000
      
      # Receiver queue size
      receiver-queue-size: 1000
      
      # Maximum total receiver queue size across partitions
      max-total-receiver-queue-size-across-partitions: 50000
      
      # Consumer name
      consumer-name: my-consumer
      
      # Whether to read compacted topic
      read-compacted: false
      
      # Pattern auto discovery period
      pattern-auto-discovery-period: 1m
      
      # Consumer priority level
      priority-level: 0
      
      # Crypto failure action: FAIL, DISCARD, CONSUME
      crypto-failure-action: FAIL
      
      # Maximum pending chunked messages
      max-pending-chunked-message: 10
      
      # Auto-acknowledge oldest chunked message on queue full
      auto-ack-oldest-chunked-message-on-queue-full: false
      
      # Expire time of incomplete chunked message
      expire-time-of-incomplete-chunked-message: 1m
```

#### Multiple Consumers Configuration

```yaml
spring:
  pulsar:
    # Note: consumer-map cannot be used together with single consumer configuration
    consumer-map:
      # Order consumer
      order-consumer:
        topic: persistent://public/default/order-topic
        subscription-name: order-subscription
        subscription-type: Exclusive  # Order messages need to guarantee order
        subscription-initial-position: Earliest
        ack-timeout: 60s
        receiver-queue-size: 500
        business-key: orderType
        retry-time: 5
        auto-ack: false  # Manual acknowledgment
      
      # User consumer
      user-consumer:
        topic: persistent://public/default/user-topic
        subscription-name: user-subscription
        subscription-type: Shared  # User messages can be processed in parallel
        subscription-initial-position: Latest
        ack-timeout: 30s
        receiver-queue-size: 1000
        business-key: userAction
        retry-time: 3
        auto-ack: true  # Auto acknowledgment
      
      # Notification consumer
      notification-consumer:
        topic: persistent://public/default/notification-topic
        subscription-name: notification-subscription
        subscription-type: Key_Shared  # Process notifications grouped by user ID
        subscription-initial-position: Latest
        ack-timeout: 15s
        receiver-queue-size: 2000
        priority-level: 1  # Higher priority for notifications
        business-key: userId
        retry-time: 2
        auto-ack: true
      
      # Log consumer
      log-consumer:
        topic: persistent://public/default/log-topic
        subscription-name: log-subscription
        subscription-type: Shared
        subscription-initial-position: Latest
        ack-timeout: 10s
        receiver-queue-size: 5000  # Large queue for high log volume
        business-key: logLevel
        retry-time: 1  # Fewer retries for log messages
        auto-ack: true
        read-compacted: true  # Logs may need to read compacted topics
```

### Client Configuration

```yaml
spring:
  pulsar:
    client:
      # Timeout configuration
      operation-timeout: 30s
      connection-timeout: 10s
      request-timeout: 60s
      
      # Thread configuration
      num-io-threads: 1
      num-listener-threads: 1
      
      # Connection configuration
      connections-per-broker: 1
      use-tcp-no-delay: true
      keep-alive-interval: 30s
      
      # TLS configuration
      tls-trust-certs-file-path: /path/to/certs
      tls-allow-insecure-connection: false
      tls-hostname-verification-enable: false
      
      # Lookup configuration
      concurrent-lookup-request: 5000
      max-lookup-request: 50000
      max-lookup-redirects: 20
      max-number-of-rejected-request-per-connection: 50
```

### Retry Mechanism Configuration

```yaml
spring:
  pulsar:
    retry:
      # Whether to enable retry mechanism
      enabled: true
      
      # Maximum retry times
      max-retries: 3
      
      # Initial retry delay
      initial-delay: 1s
      
      # Retry delay multiplier
      multiplier: 2.0
      
      # Maximum retry delay
      max-delay: 30s
      
      # Whether to use random delay
      use-random-delay: false
```

### Transaction Configuration

```yaml
spring:
  pulsar:
    transaction:
      # Whether to enable transactions
      enabled: false
      
      # Transaction coordinator topic
      coordinator-topic: persistent://pulsar/system/transaction_coordinator_assign
      
      # Transaction timeout
      timeout: 1m
      
      # Transaction buffer snapshot segment size (1MB)
      buffer-snapshot-segment-size: 1048576
      
      # Transaction buffer snapshot minimum time interval
      buffer-snapshot-min-time-in-millis: 5s
      
      # Transaction buffer snapshot maximum transaction count
      buffer-snapshot-max-transaction-count: 1000
      
      # Transaction log store size (1GB)
      log-store-size: 1073741824
```

### Dead Letter Queue Configuration

```yaml
spring:
  pulsar:
    dead-letter:
      # Whether to enable dead letter queue
      enabled: false
      
      # Dead letter queue topic suffix
      topic-suffix: "-DLQ"
      
      # Maximum retry times
      max-retries: 3
      
      # Retry strategy configuration
      retry:
        # Whether to enable smart retry strategy
        smart-strategy-enabled: true
        
        # Base retry delay
        base-delay: 1s
        
        # Maximum retry delay
        max-delay: 5m
        
        # Retry time window
        retry-window: 24h
        
        # Whether to enable jitter
        jitter-enabled: true
        
        # Jitter factor (0.0-1.0)
        jitter-factor: 0.2
      
      # Cleanup configuration
      cleanup:
        # Whether to enable auto cleanup
        auto-cleanup-enabled: true
        
        # Message expiration time
        message-expiration: 7d
        
        # Cleanup execution time (2 AM daily)
        cleanup-cron: "0 0 2 * * ?"
        
        # Retry info expiration time
        retry-info-expiration: 24h
      
      # Monitoring configuration
      monitoring:
        # Whether to enable monitoring
        enabled: true
        
        # Monitoring interval
        monitoring-interval: 5m
        
        # Health check timeout
        health-check-timeout: 30s
        
        # Whether to enable alerts
        alert-enabled: false
        
        # Alert threshold (dead letter message count)
        alert-threshold: 100
      
      # Statistics configuration
      statistics:
        # Whether to enable statistics
        enabled: true
        
        # Statistics data retention period
        retention-period: 30d
        
        # Whether to enable detailed statistics
        detailed-enabled: false
```

### Health Check Configuration

```yaml
spring:
  pulsar:
    health:
      # Whether to enable health check
      enabled: true
      
      # Health check timeout
      timeout: 10s
```

### Interceptor Configuration

```yaml
spring:
  pulsar:
    interceptor:
      # Whether to enable message interceptors
      enabled: true
```

## Usage Examples

### Message Sending

#### Basic Message Sending

```java

@Service
public class MessageService {

    @Autowired
    private PulsarMessageSender messageSender;

    // Send simple message
    public void sendMessage(String message) {
        MessageId messageId = messageSender.send("my-topic", message);
        System.out.println("Message sent: " + messageId);
    }

    // Send message asynchronously
    public CompletableFuture<MessageId> sendAsyncMessage(String message) {
        return messageSender.sendAsync("my-topic", message);
    }

    // Send keyed message
    public void sendKeyedMessage(String key, Object message) {
        messageSender.send("my-topic", key, message);
    }

    // Send delayed message
    public void sendDelayedMessage(String message, long delayMillis) {
        messageSender.sendDelayed("my-topic", message, delayMillis);
    }
}
```

#### Using Multiple Producers

```java
@Service
public class MultiProducerService {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    // Send message using specified producer
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

#### Business Scenario Examples

```java

@Service
public class OrderService {

    @Autowired
    private PulsarMessageSender messageSender;

    public void createOrder(Order order) {
        // Business logic processing
        processOrder(order);

        // Send order creation event
        messageSender.send("order-events", order);
    }

    public CompletableFuture<Void> sendEmailAsync(EmailMessage email) {
        return messageSender.sendAsync("email-queue", email)
                .thenAccept(messageId -> {
                    log.info("Email queued for sending: {}", messageId);
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

### Message Receiving

#### Basic Message Listening

```java
@Component
public class MessageListener {

    // Simple message listener
    @PulsarListener(topic = "my-topic", subscription = "my-subscription")
    public void handleMessage(String message) {
        System.out.println("Received message: " + message);
    }

    // Listen to complex objects
    @PulsarListener(
            topic = "user-events",
            subscription = "user-service",
            messageType = UserEvent.class
    )
    public void handleUserEvent(UserEvent event) {
        System.out.println("User event: " + event);
    }

    // Shared subscription mode
    @PulsarListener(
            topic = "shared-topic",
            subscription = "shared-subscription",
            subscriptionType = "Shared"
    )
    public void handleSharedMessage(String message) {
        // Multiple consumer instances can process messages in parallel
        System.out.println("Shared message: " + message);
    }
}
```

#### Using Multiple Consumers

```java
@Component
public class MultiConsumerListener {

    // Use specified consumer configuration
    @PulsarListener(consumerName = "order-consumer")
    public void handleOrderMessage(Order order) {
        log.info("Processing order message: {}", order.getId());
        // Order processing logic
    }

    @PulsarListener(consumerName = "user-consumer")
    public void handleUserMessage(UserEvent event) {
        log.info("Processing user event: {}", event.getType());
        // User event processing logic
    }

    @PulsarListener(consumerName = "notification-consumer")
    public void handleNotification(Notification notification) {
        log.info("Processing notification: {}", notification.getTitle());
        // Notification processing logic
    }
}
```

#### Business Scenario Examples

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

    // Payment processing - Shared subscription mode
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
            throw e; // Trigger retry mechanism
        }
    }
}
```

## Message Interceptors

### Interceptor Interface

Message interceptors provide the ability to intercept processing during message sending and receiving:

```java
public interface PulsarMessageInterceptor {

    // Intercept before sending message
    default Object beforeSend(String topic, Object message) {
        return message;
    }

    // Intercept after sending message
    default void afterSend(String topic, Object message, MessageId messageId, Exception exception) {
        // Default empty implementation
    }

    // Intercept before receiving message
    default boolean beforeReceive(Message<?> message) {
        return true;
    }

    // Intercept after receiving message
    default void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        // Default empty implementation
    }

    // Get interceptor priority
    default int getOrder() {
        return 0;
    }
}
```

### Custom Interceptor Examples

#### Logging Interceptor

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
            logger.info("Successfully sent message to topic: {}, message ID: {}", topic, messageId);
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        logger.info("Receiving message: {}", message.getMessageId());
        return true;
    }

    @Override
    public int getOrder() {
        return 1; // Set interceptor priority
    }
}
```

#### Message Audit Interceptor

```java

@Component
public class MessageAuditInterceptor implements PulsarMessageInterceptor {

    @Autowired
    private AuditService auditService;

    @Override
    public Object beforeSend(String topic, Object message) {
        auditService.logMessageSent(topic, message);

        // Can modify message content
        if (message instanceof AuditableMessage) {
            ((AuditableMessage) message).setAuditInfo(getCurrentUser(), System.currentTimeMillis());
        }

        return message;
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        // Check if message should be processed
        String topic = message.getTopicName();
        if (isBlacklistedTopic(topic)) {
            log.warn("Ignoring message from blacklisted topic: {}", topic);
            return false;
        }

        auditService.logMessageReceived(message);
        return true;
    }

    @Override
    public int getOrder() {
        return 1; // High priority
    }
}
```

#### Performance Monitoring Interceptor

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
            System.out.println("Send duration: " + duration + "ms");
            startTime.remove();
        }
    }
}
```

## Dead Letter Queue

### Dead Letter Queue Handler

```java
@Component
public class CustomDeadLetterHandler implements DeadLetterQueueHandler {

    @Override
    public void handleDeadLetter(String originalTopic, Message<?> message, Exception exception) {
        // Custom dead letter handling logic
        System.out.println("Dead letter from topic: " + originalTopic);
        System.out.println("Message ID: " + message.getMessageId());
        System.out.println("Error: " + exception.getMessage());

        // Save to database for later analysis
        DeadLetterRecord record = new DeadLetterRecord();
        record.setOriginalTopic(originalTopic);
        record.setMessageId(message.getMessageId().toString());
        record.setMessageData(new String(message.getData()));
        record.setErrorMessage(exception.getMessage());
        record.setTimestamp(System.currentTimeMillis());

        messageRepository.saveDeadLetterRecord(record);

        // Send alert notification
        if (isCriticalTopic(originalTopic)) {
            notificationService.sendAlert(
                    "Critical message processing failed",
                    String.format("Topic: %s, Error: %s", originalTopic, exception.getMessage())
            );
        }
    }

    @Override
    public int getMaxRetries() {
        return 5; // Custom maximum retry times
    }
}
```

## Transaction Support

### Transaction Usage

#### Annotation-based (Recommended)

```java

@Service
public class MessageService {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    @PulsarTransactional
    public void sendMessages(String topic, List<String> messages) {
        // Get current transaction
        Transaction transaction = PulsarTransactionUtils.getCurrentTransaction();

        for (String message : messages) {
            // Send message in transaction
            pulsarTemplate.send(topic, message, transaction);
        }

        // If method completes normally, transaction will be automatically committed
        // If exception is thrown, transaction will be automatically rolled back
    }
}
```

#### Programmatic Transaction

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

## Advanced Configuration

### EnablePulsar Annotation Options

```java
@EnablePulsar(
        enabled = true,                    // Whether to enable Pulsar
        enableTransaction = false,         // Whether to enable transaction support
        enableHealthCheck = true,          // Whether to enable health check
        enableInterceptor = true,          // Whether to enable message interceptors
        enableDeadLetterQueue = false,     // Whether to enable dead letter queue
        enableRetry = true                 // Whether to enable message retry
)
@SpringBootApplication
public class Application {
    // ...
}
```

### Complete Configuration Example

#### Production Environment Configuration

```yaml
spring:
  pulsar:
    enabled: true
    service-url: pulsar://pulsar-cluster:6650
    admin-url: http://pulsar-cluster:8080

    # Producer configuration
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

    # Consumer configuration
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

    # Client configuration
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

    # Authentication configuration
    authentication:
      enabled: true
      token: ${PULSAR_JWT_TOKEN}
      auth-plugin-class-name: org.apache.pulsar.client.impl.auth.AuthenticationToken

    # Retry configuration
    retry:
      enabled: true
      max-retries: 5
      initial-delay: 2s
      multiplier: 2.0
      max-delay: 60s
      use-random-delay: true

    # Dead letter queue configuration
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

    # Health check configuration
    health:
      enabled: true
      timeout: 30s

    # Transaction configuration
    transaction:
      enabled: false  # Enable as needed
      timeout: 2m
      buffer-snapshot-segment-size: 2097152  # 2MB
      buffer-snapshot-max-transaction-count: 2000

logging:
  level:
    com.github.spring.mq.pulsar: INFO
    org.apache.pulsar: WARN
```

## Monitoring and Health Check

### Health Check

The starter provides built-in health check functionality:

```java

@Autowired
private PulsarHealthIndicator healthIndicator;

public void checkHealth() {
    Map<String, Object> health = healthIndicator.health();
    System.out.println("Pulsar health status: " + health);
}
```

### Health Check Integration

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

## Best Practices

### 1. Message Design

```java
// Good message design
public class OrderEvent {
    private String eventId;        // Unique event ID
    private String eventType;      // Event type
    private long timestamp;        // Timestamp
    private String orderId;        // Business ID
    private Map<String, Object> data; // Event data
    // Constructors, getters, setters
}

// Avoid this message design
public class BadOrderEvent {
    private Order order; // Contains too much information
    // Missing event metadata
}
```

### 2. Topic Naming

Use meaningful topic names, recommend using hierarchical structure:

- `app.service.event` - Application.Service.Event
- `order.payment.success` - Order.Payment.Success
- `user.registration.completed` - User.Registration.Completed

### 3. Subscription Mode Selection

- **Exclusive**: Single consumer, guarantees message order
- **Shared**: Multiple consumers load balancing, improves throughput
- **Failover**: Active-standby mode, high availability
- **Key_Shared**: Partition by key, balances order and concurrency

### 4. Exception Handling

```java
@PulsarListener(topic = "orders", subscription = "order-processor")
public void processOrder(Order order) {
    try {
        // Business processing
        orderService.process(order);
    } catch (BusinessException e) {
        // Business exception, log but don't retry
        log.error("Order business processing error: {}", order.getId(), e);
        // Don't throw exception to avoid retry
    } catch (Exception e) {
        // System exception, can retry
        log.error("Order system processing error: {}", order.getId(), e);
        throw e; // Throw exception to trigger retry
    }
}
```

### 5. Performance Optimization

```java
@Service
public class HighThroughputService {

    // Use async sending to improve performance
    public void sendBatchMessages(List<Message> messages) {
        List<CompletableFuture<MessageId>> futures = messages.stream()
                .map(msg -> messageSender.sendAsync("batch-topic", msg))
                .collect(Collectors.toList());

        // Wait for all messages to be sent
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("All messages sent successfully"))
                .exceptionally(throwable -> {
                    log.error("Some messages failed to send", throwable);
                    return null;
                });
    }
}
```

## Troubleshooting

### Common Issues

1. **Connection Failed**: Check if Pulsar service is running normally and network is reachable
2. **Authentication Failed**: Confirm authentication configuration is correct
3. **Message Loss**: Check if message acknowledgment mechanism is correctly implemented
4. **Performance Issues**: Adjust batching, connection pool and other configuration parameters
5. **Transaction Timeout**: Check `transaction.timeout` configuration, optimize business logic execution time
6. **Transaction Coordinator Unavailable**: Check Pulsar cluster configuration, confirm transaction coordinator service
   is running normally

### Logging Configuration

```yaml
logging:
  level:
    com.github.spring.mq.pulsar: DEBUG
    org.apache.pulsar: INFO
    com.github.spring.mq.pulsar.config.PulsarTransactionConfiguration: DEBUG
    org.apache.pulsar.client.impl.transaction: DEBUG
```

### Debugging Tips

```java
// Enable detailed logging
@PulsarTransactional
public void debugTransaction() {
    Transaction tx = PulsarTransactionUtils.getCurrentTransaction();
    log.debug("Transaction ID: {}", tx.getTxnID());
    log.debug("Transaction state: {}", tx.getState());
}
```

## Notes

1. **Pulsar Version Requirements**: Ensure using Pulsar version that supports transactions (2.7.0+)
2. **Cluster Configuration**: Pulsar cluster needs to enable transaction coordinator
3. **Performance Impact**: Transactions and interceptors will bring certain performance overhead, please weigh usage
   according to business needs
4. **Error Recovery**: Properly handle transaction failures and retry logic
5. **Resource Management**: Release transaction resources in time to avoid resource leaks

## Version Requirements

- Java 17+
- Spring Boot 3.0+
- Apache Pulsar 3.2.4+

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

Before contributing, please read our [Code of Conduct](CODE_OF_CONDUCT.md).

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes and version history.

## License

This project is licensed under the [MIT License](LICENSE).