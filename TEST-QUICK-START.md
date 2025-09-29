# 快速测试指南

## 🚀 一键运行所有测试

### 验证测试环境
```bash
# 检查测试环境是否正确配置
./verify-test-setup.sh
```

### 运行完整测试套件
```bash
# Linux/macOS - 完整测试（包括性能测试）
./run-tests.sh

# Linux/macOS - 跳过性能测试（推荐CI环境）
./run-tests.sh --skip-performance

# Windows - 完整测试
run-tests.bat

# Windows - 跳过性能测试
run-tests.bat --skip-performance
```

## 📊 测试覆盖范围

| 测试类型 | 测试数量 | 覆盖功能 |
|---------|---------|---------|
| **单元测试** | ~50个 | 配置、核心功能、注解处理、异常处理、拦截器、健康检查 |
| **集成测试** | ~15个 | 端到端消息传递、异步处理、JSON序列化、延迟消息 |
| **性能测试** | ~10个 | 高并发、大消息、多线程、吞吐量基准 |

## ⚡ 快速命令

```bash
# 只运行单元测试（最快）
mvn test -Dtest="!**/*IntegrationTest,!**/*PerformanceTest"

# 只运行集成测试
mvn test -Dtest="**/*IntegrationTest"

# 只运行性能测试
mvn test -Dtest="**/*PerformanceTest"

# 运行特定测试类
mvn test -Dtest="PulsarTemplateTest"

# 生成测试报告
mvn surefire-report:report

# 生成覆盖率报告
mvn jacoco:report
```

## 📈 预期性能基准

| 场景 | 最低要求 | 推荐性能 |
|-----|---------|---------|
| 同步发送 | 10 msg/s | 100+ msg/s |
| 异步发送 | 50 msg/s | 500+ msg/s |
| 并发发送 | 5 msg/s | 50+ msg/s |
| 大消息处理 | 0.1 MB/s | 1+ MB/s |

## 🔧 故障排除

### Docker 问题
```bash
# 检查 Docker 状态
docker info

# 手动启动 Pulsar（如果需要）
docker run -d --name pulsar -p 6650:6650 -p 8080:8080 apachepulsar/pulsar:latest bin/pulsar standalone
```

### 内存问题
```bash
# 增加 Maven 内存
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
```

### 端口冲突
```bash
# 检查端口占用
lsof -i :6650
lsof -i :8080

# 停止占用端口的进程
kill -9 <PID>
```

## 📋 发版前检查清单

运行以下命令确保发版质量：

```bash
# 1. 验证环境
./verify-test-setup.sh

# 2. 运行完整测试
./run-tests.sh

# 3. 检查测试报告
open target/site/surefire-report.html

# 4. 检查覆盖率报告
open target/site/jacoco/index.html

# 5. 确认构建成功
ls -la target/*.jar
```

## 🎯 成功标准

测试通过的标准：
- ✅ 所有单元测试通过
- ✅ 所有集成测试通过  
- ✅ 性能测试在可接受范围内
- ✅ 代码覆盖率 > 80%
- ✅ 无严重静态分析问题
- ✅ 构建产物生成成功

## 🆘 需要帮助？

1. 查看详细文档：[TESTING.md](TESTING.md)
2. 检查 GitHub Issues
3. 联系维护者

---

**💡 提示**: 首次运行可能需要下载依赖，请耐心等待。建议在发版前至少运行一次完整测试套件。