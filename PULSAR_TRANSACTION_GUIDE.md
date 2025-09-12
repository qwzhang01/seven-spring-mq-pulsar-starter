# Pulsar 事务配置指南

## 概述

本指南介绍如何在 Spring Boot 应用中使用 Pulsar 事务功能。Pulsar 事务提供了跨多个主题和分区的原子性消息发送和确认能力。

## 配置

### 1. 启用事务功能

在 `application.yml` 或 `application.properties` 中配置：

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

或使用 properties 格式：

```properties
spring.pulsar.service-url=pulsar://localhost:6650
spring.pulsar.transaction.enabled=true
spring.pulsar.transaction.timeout=PT1M
spring.pulsar.transaction.coordinator-topic=persistent://pulsar/system/transaction_coordinator_assign
```

### 2. 依赖配置

确保在 `pom.xml` 中包含必要的依赖：

```xml
<dependency>
    <groupId>com.github.spring.mq</groupId>
    <artifactId>seven-spring-mq-pulsar-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 使用方式

### 1. 注解方式（推荐）

使用 `@PulsarTransactional` 注解：

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

### 2. 编程式事务

使用 `TransactionTemplate`：

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

### 3. 手动事务管理

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

## 事务工具类

### PulsarTransactionUtils

提供便捷的事务操作方法：

```java
// 获取当前事务
Transaction currentTx = PulsarTransactionUtils.getCurrentTransaction();

// 检查是否存在活跃事务
boolean isActive = PulsarTransactionUtils.isTransactionActive();

// 获取当前事务ID
String txnId = PulsarTransactionUtils.getCurrentTransactionId();
```

## 配置参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `enabled` | `false` | 是否启用事务功能 |
| `timeout` | `PT1M` | 事务超时时间 |
| `coordinator-topic` | `persistent://pulsar/system/transaction_coordinator_assign` | 事务协调器主题 |
| `buffer-snapshot-segment-size` | `1048576` | 事务缓冲区快照段大小（字节） |
| `buffer-snapshot-min-time-in-millis` | `PT5S` | 事务缓冲区快照最小时间间隔 |
| `buffer-snapshot-max-transaction-count` | `1000` | 事务缓冲区快照最大事务数 |
| `log-store-size` | `1073741824` | 事务日志存储大小（字节） |

## 最佳实践

### 1. 事务范围

- 保持事务范围尽可能小
- 避免在事务中执行长时间运行的操作
- 合理设置事务超时时间

### 2. 异常处理

```java
@PulsarTransactional(rollbackFor = Exception.class)
public void processMessages() {
    try {
        // 业务逻辑
    } catch (BusinessException e) {
        // 业务异常处理
        throw e; // 会触发事务回滚
    } catch (Exception e) {
        // 其他异常处理
        throw new RuntimeException("Processing failed", e);
    }
}
```

### 3. 事务传播

```java
@PulsarTransactional(propagation = Propagation.REQUIRED)
public void method1() {
    // 如果存在事务则加入，否则创建新事务
}

@PulsarTransactional(propagation = Propagation.REQUIRES_NEW)
public void method2() {
    // 总是创建新事务
}

@PulsarTransactional(propagation = Propagation.SUPPORTS)
public void method3() {
    // 如果存在事务则加入，否则以非事务方式执行
}
```

## 监控和调试

### 1. 日志配置

```yaml
logging:
  level:
    com.github.spring.mq.pulsar.config.PulsarTransactionConfiguration: DEBUG
    org.apache.pulsar.client.impl.transaction: DEBUG
```

### 2. 事务状态检查

```java
@Component
public class TransactionMonitor {
    
    @EventListener
    public void handleTransactionEvent(TransactionEvent event) {
        if (PulsarTransactionUtils.isTransactionActive()) {
            String txnId = PulsarTransactionUtils.getCurrentTransactionId();
            log.info("Transaction active: {}", txnId);
        }
    }
}
```

## 注意事项

1. **Pulsar 版本要求**：确保使用支持事务的 Pulsar 版本（2.7.0+）
2. **集群配置**：Pulsar 集群需要启用事务协调器
3. **性能影响**：事务会带来一定的性能开销，请根据业务需求权衡使用
4. **错误恢复**：合理处理事务失败和重试逻辑
5. **资源管理**：及时释放事务资源，避免资源泄漏

## 故障排除

### 常见问题

1. **事务超时**
   - 检查 `transaction.timeout` 配置
   - 优化业务逻辑执行时间

2. **事务协调器不可用**
   - 检查 Pulsar 集群配置
   - 确认事务协调器服务正常运行

3. **消息发送失败**
   - 检查网络连接
   - 确认主题和权限配置

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

## 示例代码

完整的示例代码请参考：
- `PulsarTransactionExample.java` - 各种事务使用方式的示例
- `PulsarTransactionUtils.java` - 事务工具类
- `PulsarTransactional.java` - 事务注解定义