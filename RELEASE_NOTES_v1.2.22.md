# Release v1.2.22

## 🎉 What's New

### Features
- **Multi-instance Configuration**: Support configuring multiple producer and consumer instances for complex scenarios
- **Transaction Support**: Full transaction support with `@PulsarTransactional` annotation
- **Dead Letter Queue (DLQ)**: Smart retry strategy with exponential backoff and jitter
- **Message Interceptors**: Framework for logging, auditing, and monitoring message flow
- **Health Check**: Built-in health check integration with Spring Boot Actuator

### CI/CD
- GitHub Actions workflow for automated testing
- Support for Java 17 and 21
- Code coverage reporting with Codecov

## 📈 Improvements
- Upgraded Apache Pulsar client to version 3.2.4
- Improved error handling and retry mechanism
- Enhanced configuration validation
- Better memory management in consumer listeners

## 📚 Documentation
- Added `CONTRIBUTING.md` - Contribution guidelines (bilingual)
- Added `CODE_OF_CONDUCT.md` - Community code of conduct
- Added `CHANGELOG.md` - Version history
- Added `SECURITY.md` - Security policy
- Added Issue templates (Bug Report, Feature Request, Question)
- Added Pull Request template
- Added Dependabot configuration for automated dependency updates
- Updated README with badges and improved navigation

## 📋 Requirements
- Java 17+
- Spring Boot 3.0+
- Apache Pulsar 3.2.4+

## 📦 Installation

### Maven
```xml
<dependency>
    <groupId>io.github.qwzhang01</groupId>
    <artifactId>seven-spring-mq-pulsar-starter</artifactId>
    <version>1.2.22</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.qwzhang01:seven-spring-mq-pulsar-starter:1.2.22'
```

## 🙏 Acknowledgments
Thanks to all contributors who helped make this release possible!

---

**Full Changelog**: https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/compare/v1.2.0...v1.2.22
