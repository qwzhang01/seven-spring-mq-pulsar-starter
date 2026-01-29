# Contributing to Seven Spring MQ Pulsar Starter

[中文版](#中文版贡献指南)

First off, thank you for considering contributing to Seven Spring MQ Pulsar Starter! It's people like you that make this project such a great tool.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Pull Requests](#pull-requests)
- [Development Setup](#development-setup)
- [Coding Guidelines](#coding-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Testing Guidelines](#testing-guidelines)

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When you create a bug report, please include as many details as possible:

- **Use a clear and descriptive title** for the issue
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples** to demonstrate the steps
- **Describe the behavior you observed** and why it's a problem
- **Explain which behavior you expected** to see instead
- **Include logs, stack traces, or error messages** if applicable
- **Include your environment details**:
  - Java version
  - Spring Boot version
  - Pulsar version
  - Operating system

### Suggesting Enhancements

Enhancement suggestions are welcome! When suggesting an enhancement:

- **Use a clear and descriptive title** for the issue
- **Provide a detailed description** of the suggested enhancement
- **Explain why this enhancement would be useful** to most users
- **List any alternative solutions** you've considered
- **Include mockups or examples** if applicable

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Install dependencies** and set up the development environment
3. **Make your changes** following our coding guidelines
4. **Add or update tests** as needed
5. **Ensure all tests pass**
6. **Update documentation** if needed
7. **Submit your pull request**

## Development Setup

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker (for running Pulsar locally)
- Git

### Setting Up the Development Environment

1. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/seven-spring-mq-pulsar-starter.git
   cd seven-spring-mq-pulsar-starter
   ```

2. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/qwzhang01/seven-spring-mq-pulsar-starter.git
   ```

3. **Install dependencies**:
   ```bash
   mvn clean install -DskipTests
   ```

4. **Start local Pulsar** (for testing):
   ```bash
   docker run -d --name pulsar \
     -p 6650:6650 \
     -p 8080:8080 \
     apachepulsar/pulsar:3.2.4 \
     bin/pulsar standalone
   ```

5. **Run tests**:
   ```bash
   ./run-tests.sh
   ```

## Coding Guidelines

### Java Style Guide

- Follow standard Java naming conventions
- Use meaningful and descriptive names for classes, methods, and variables
- Keep methods small and focused on a single task
- Add JavaDoc comments for public APIs
- Use `@Override` annotation when overriding methods
- Prefer composition over inheritance
- Use Optional instead of returning null

### Code Formatting

- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Use braces for all control structures, even single-line
- One statement per line
- Blank line between method definitions

### Example

```java
/**
 * Sends a message to the specified topic.
 *
 * @param topic   the target topic name
 * @param message the message to send
 * @return the message ID of the sent message
 * @throws PulsarException if the message cannot be sent
 */
public MessageId send(String topic, Object message) {
    Objects.requireNonNull(topic, "Topic must not be null");
    Objects.requireNonNull(message, "Message must not be null");
    
    // Implementation
}
```

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that do not affect the meaning of the code
- **refactor**: A code change that neither fixes a bug nor adds a feature
- **perf**: A code change that improves performance
- **test**: Adding missing tests or correcting existing tests
- **chore**: Changes to the build process or auxiliary tools

### Examples

```
feat(producer): add support for delayed message delivery

fix(consumer): resolve memory leak in message listener

docs(readme): update configuration examples

test(transaction): add integration tests for transaction rollback
```

## Testing Guidelines

### Test Categories

1. **Unit Tests**: Test individual components in isolation
   - Use mocks for external dependencies
   - Fast execution, no external services needed
   - Located in `src/test/java`

2. **Integration Tests**: Test component interactions
   - Require running Pulsar instance
   - Class names end with `IntegrationTest`

3. **Performance Tests**: Test performance characteristics
   - Class names end with `PerformanceTest`
   - May take longer to execute

### Running Tests

```bash
# Run all tests
./run-tests.sh

# Run unit tests only
mvn test -Dtest="!**/*IntegrationTest,!**/*PerformanceTest"

# Run integration tests only
mvn test -Dtest="**/*IntegrationTest"

# Run with coverage report
mvn test jacoco:report
```

### Writing Tests

- Write tests for all new features and bug fixes
- Aim for high test coverage (>80%)
- Use descriptive test method names
- Follow the Arrange-Act-Assert pattern

```java
@Test
void shouldSendMessageSuccessfully() {
    // Arrange
    String topic = "test-topic";
    String message = "test-message";
    
    // Act
    MessageId messageId = messageSender.send(topic, message);
    
    // Assert
    assertNotNull(messageId);
}
```

## Questions?

If you have any questions, feel free to:

- Open an issue with the "question" label
- Reach out to the maintainers

Thank you for contributing! 🎉

---

# 中文版贡献指南

首先，感谢您考虑为 Seven Spring MQ Pulsar Starter 做出贡献！正是像您这样的人让这个项目变得如此出色。

## 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
  - [报告 Bug](#报告-bug)
  - [建议增强功能](#建议增强功能)
  - [提交 Pull Request](#提交-pull-request)
- [开发环境设置](#开发环境设置)
- [编码规范](#编码规范)
- [提交信息规范](#提交信息规范)
- [测试指南](#测试指南)

## 行为准则

本项目及其所有参与者均受我们的[行为准则](CODE_OF_CONDUCT.md)约束。参与本项目即表示您同意遵守此准则。

## 如何贡献

### 报告 Bug

在创建 Bug 报告之前，请先检查现有的 issue 以避免重复。创建 Bug 报告时，请尽可能包含详细信息：

- **使用清晰的描述性标题**
- **描述重现问题的确切步骤**
- **提供具体的示例**来演示这些步骤
- **描述您观察到的行为**以及为什么它是一个问题
- **解释您期望看到的行为**
- **包含日志、堆栈跟踪或错误消息**（如适用）
- **包含您的环境详细信息**：
  - Java 版本
  - Spring Boot 版本
  - Pulsar 版本
  - 操作系统

### 建议增强功能

欢迎提出增强功能建议！在建议增强功能时：

- **使用清晰的描述性标题**
- **提供增强功能的详细描述**
- **解释为什么这个增强功能对大多数用户有用**
- **列出您考虑过的替代方案**
- **包含模型或示例**（如适用）

### 提交 Pull Request

1. **Fork 仓库**并从 `main` 分支创建您的分支
2. **安装依赖**并设置开发环境
3. 按照我们的编码规范**进行更改**
4. **添加或更新测试**（如需要）
5. **确保所有测试通过**
6. **更新文档**（如需要）
7. **提交您的 Pull Request**

## 开发环境设置

### 前提条件

- Java 17 或更高版本
- Maven 3.6+
- Docker（用于本地运行 Pulsar）
- Git

### 设置开发环境

1. **克隆您的 fork**：
   ```bash
   git clone https://github.com/YOUR_USERNAME/seven-spring-mq-pulsar-starter.git
   cd seven-spring-mq-pulsar-starter
   ```

2. **添加上游远程仓库**：
   ```bash
   git remote add upstream https://github.com/qwzhang01/seven-spring-mq-pulsar-starter.git
   ```

3. **安装依赖**：
   ```bash
   mvn clean install -DskipTests
   ```

4. **启动本地 Pulsar**（用于测试）：
   ```bash
   docker run -d --name pulsar \
     -p 6650:6650 \
     -p 8080:8080 \
     apachepulsar/pulsar:3.2.4 \
     bin/pulsar standalone
   ```

5. **运行测试**：
   ```bash
   ./run-tests.sh
   ```

## 编码规范

### Java 代码风格

- 遵循标准 Java 命名约定
- 为类、方法和变量使用有意义的描述性名称
- 保持方法小巧并专注于单一任务
- 为公共 API 添加 JavaDoc 注释
- 重写方法时使用 `@Override` 注解
- 优先使用组合而非继承
- 使用 Optional 而不是返回 null

### 代码格式

- 使用 4 个空格进行缩进（不使用制表符）
- 最大行长度：120 个字符
- 为所有控制结构使用大括号，即使是单行
- 每行一个语句
- 方法定义之间留空行

## 提交信息规范

我们遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

### 格式

```
<类型>(<范围>): <主题>

<正文>

<页脚>
```

### 类型

- **feat**: 新功能
- **fix**: Bug 修复
- **docs**: 仅文档更改
- **style**: 不影响代码含义的更改
- **refactor**: 既不修复 bug 也不添加功能的代码更改
- **perf**: 提高性能的代码更改
- **test**: 添加缺失的测试或更正现有测试
- **chore**: 对构建过程或辅助工具的更改

### 示例

```
feat(producer): 添加延迟消息发送支持

fix(consumer): 修复消息监听器中的内存泄漏

docs(readme): 更新配置示例

test(transaction): 为事务回滚添加集成测试
```

## 测试指南

### 测试分类

1. **单元测试**：独立测试各个组件
   - 对外部依赖使用 mock
   - 快速执行，不需要外部服务
   - 位于 `src/test/java`

2. **集成测试**：测试组件交互
   - 需要运行的 Pulsar 实例
   - 类名以 `IntegrationTest` 结尾

3. **性能测试**：测试性能特征
   - 类名以 `PerformanceTest` 结尾
   - 可能需要更长的执行时间

### 运行测试

```bash
# 运行所有测试
./run-tests.sh

# 仅运行单元测试
mvn test -Dtest="!**/*IntegrationTest,!**/*PerformanceTest"

# 仅运行集成测试
mvn test -Dtest="**/*IntegrationTest"

# 带覆盖率报告运行
mvn test jacoco:report
```

## 有问题？

如果您有任何问题，请随时：

- 创建带有 "question" 标签的 issue
- 联系维护者

感谢您的贡献！🎉
