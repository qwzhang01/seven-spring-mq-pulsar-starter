# Pulsar Spring Starter - 测试指南

本文档描述了如何运行 Pulsar Spring Starter 的完整测试套件，确保每次发版前的质量保证。

## 测试套件概览

我们的测试套件包含以下几个层次的测试：

### 1. 单元测试 (Unit Tests)

- **配置测试**: 验证自动配置和属性绑定
- **核心功能测试**: 测试 PulsarTemplate 和消息发送功能
- **注解处理测试**: 验证 @PulsarListener 注解处理
- **异常处理测试**: 测试各种异常场景的处理
- **拦截器测试**: 验证消息拦截器功能
- **健康检查测试**: 测试健康指示器

### 2. 集成测试 (Integration Tests)

- **端到端消息传递**: 完整的发送和接收流程
- **多消息处理**: 批量消息处理能力
- **异步消息处理**: 异步发送和接收
- **延迟消息**: 定时消息功能
- **JSON序列化**: 复杂对象的序列化和反序列化

### 3. 性能测试 (Performance Tests)

- **高并发发送**: 大量消息的同步发送性能
- **异步发送性能**: 异步消息发送的吞吐量
- **多线程并发**: 多线程环境下的性能表现
- **大消息负载**: 大消息的处理能力
- **带键消息**: 分区消息的性能

## 快速开始

### 前置条件

1. **Java 11+**: 确保安装了 Java 11 或更高版本
2. **Maven 3.6+**: 用于构建和测试
3. **Docker**: 用于运行 Pulsar 测试容器（集成测试需要）

### 运行完整测试套件

#### Linux/macOS

```bash
# 运行完整测试套件（包括性能测试）
./run-tests.sh

# 跳过性能测试（适用于CI环境）
./run-tests.sh --skip-performance
```

#### Windows

```cmd
# 运行完整测试套件
run-tests.bat

# 跳过性能测试
run-tests.bat --skip-performance
```

### 分别运行不同类型的测试

#### 只运行单元测试

```bash
mvn test -Dtest="!**/*IntegrationTest,!**/*PerformanceTest"
```

#### 只运行集成测试

```bash
mvn test -Dtest="**/*IntegrationTest"
```

#### 只运行性能测试

```bash
mvn test -Dtest="**/*PerformanceTest"
```

#### 运行特定测试类

```bash
mvn test -Dtest="PulsarTemplateTest"
```

## 测试配置

### 测试环境配置

测试使用 `src/test/resources/application-test.yml` 配置文件：

```yaml
spring:
  pulsar:
    enabled: true
    service-url: pulsar://localhost:6650
    admin-url: http://localhost:8080
    producer:
      topic: test-topic
      send-timeout: 30s
      batching-enabled: true
    consumer:
      subscription-name: test-subscription
      subscription-type: Exclusive
      auto-ack: true
```

### 测试容器配置

集成测试使用 Testcontainers 自动启动 Pulsar 实例：

```java

@Testcontainers
class PulsarIntegrationTest {
    @Container
    static PulsarContainer pulsar = new PulsarContainer("apachepulsar/pulsar:latest")
            .withExposedPorts(6650, 8080);
}
```

## 测试报告

运行测试后，可以查看以下报告：

### 1. 测试结果报告

- **位置**: `target/site/surefire-report.html`
- **内容**: 详细的测试执行结果，包括通过/失败的测试

### 2. 代码覆盖率报告

- **位置**: `target/site/jacoco/index.html`
- **内容**: 代码覆盖率统计，包括行覆盖率、分支覆盖率等

### 3. 静态分析报告

- **位置**: `target/site/spotbugs.html`
- **内容**: 代码质量分析，潜在的bug和代码异味

## CI/CD 集成

### GitHub Actions

项目包含 GitHub Actions 工作流 (`.github/workflows/test.yml`)，会在以下情况自动运行测试：

- 推送到 `main` 或 `develop` 分支
- 创建 Pull Request
- 每日定时运行（UTC 2:00 AM）

工作流会在多个 Java 版本（11, 17, 21）上运行测试，确保兼容性。

### 本地 CI 模拟

可以使用 [act](https://github.com/nektos/act) 在本地模拟 GitHub Actions：

```bash
# 安装 act
brew install act  # macOS
# 或
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash  # Linux

# 运行 GitHub Actions
act
```

## 性能基准

### 预期性能指标

以下是在标准开发环境中的预期性能基准：

| 测试场景  | 最低要求     | 推荐性能       |
|-------|----------|------------|
| 同步发送  | 10 msg/s | 100+ msg/s |
| 异步发送  | 50 msg/s | 500+ msg/s |
| 并发发送  | 5 msg/s  | 50+ msg/s  |
| 大消息处理 | 0.1 MB/s | 1+ MB/s    |

### 性能调优建议

1. **启用批处理**: 设置 `batching-enabled=true`
2. **调整批处理参数**: 根据消息大小调整 `batching-max-messages` 和 `batching-max-publish-delay`
3. **使用异步发送**: 对于高吞吐量场景使用 `sendAsync()`
4. **合理设置超时**: 根据网络环境调整 `send-timeout`

## 故障排除

### 常见问题

#### 1. Docker 相关问题

```
Error: Could not start Pulsar container
```

**解决方案**: 确保 Docker 正在运行，并且有足够的内存分配给 Docker

#### 2. 端口冲突

```
Error: Port 6650 is already in use
```

**解决方案**: 停止其他使用 6650 端口的服务，或修改测试配置使用不同端口

#### 3. 内存不足

```
OutOfMemoryError during performance tests
```

**解决方案**: 增加 JVM 内存设置：

```bash
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
```

#### 4. 网络超时

```
TimeoutException: Message send timeout
```

**解决方案**: 增加超时设置或检查网络连接

### 调试技巧

#### 1. 启用详细日志

在 `application-test.yml` 中添加：

```yaml
logging:
  level:
    com.github.spring.mq.pulsar: DEBUG
    org.apache.pulsar: INFO
```

#### 2. 单独运行失败的测试

```bash
mvn test -Dtest="FailingTestClass#failingTestMethod"
```

#### 3. 跳过特定测试

```bash
mvn test -Dtest="!ProblematicTest"
```

## 贡献测试

### 添加新测试

1. **单元测试**: 在相应的包下创建 `*Test.java` 文件
2. **集成测试**: 创建 `*IntegrationTest.java` 文件
3. **性能测试**: 创建 `*PerformanceTest.java` 文件

### 测试命名规范

- 测试类: `ClassNameTest`
- 测试方法: `shouldDoSomethingWhenCondition()`
- 显示名称: `@DisplayName("Should do something when condition")`

### 测试最佳实践

1. **使用 AssertJ**: 使用 `assertThat()` 而不是 JUnit 的断言
2. **Mock 外部依赖**: 使用 Mockito 模拟外部服务
3. **测试隔离**: 确保测试之间不相互影响
4. **清理资源**: 在 `@AfterEach` 中清理测试资源
5. **有意义的断言**: 使用描述性的断言消息

## 发版前检查清单

在发布新版本前，请确保：

- [ ] 所有单元测试通过
- [ ] 所有集成测试通过
- [ ] 性能测试结果在可接受范围内
- [ ] 代码覆盖率 > 80%
- [ ] 静态分析无严重问题
- [ ] 文档已更新
- [ ] 变更日志已更新

## 联系方式

如果在运行测试时遇到问题，请：

1. 查看本文档的故障排除部分
2. 检查 GitHub Issues 中是否有类似问题
3. 创建新的 Issue 并提供详细的错误信息和环境信息

---

**注意**: 性能测试结果可能因硬件环境而异。在生产环境部署前，建议在目标环境中运行性能测试以获得准确的基准数据。