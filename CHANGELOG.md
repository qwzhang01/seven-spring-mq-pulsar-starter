# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Nothing yet

### Changed
- Nothing yet

### Fixed
- Nothing yet

## [1.2.22] - 2025-01-29

### Added
- Multi-instance producer and consumer configuration support
- Transaction support with `@PulsarTransactional` annotation
- Dead letter queue (DLQ) with smart retry strategy
- Message interceptor framework for logging, auditing, and monitoring
- Health check integration with Spring Boot Actuator
- Comprehensive test suite with unit, integration, and performance tests
- GitHub Actions CI/CD workflow

### Changed
- Upgraded Apache Pulsar client to version 3.2.4
- Improved error handling and retry mechanism
- Enhanced configuration validation

### Fixed
- Memory leak in consumer listener registration
- Thread safety issues in producer factory

## [1.2.0] - 2025-01-15

### Added
- Dead letter queue (DLQ) support with configurable retry policies
- Smart retry strategy with exponential backoff and jitter
- DLQ monitoring and alerting capabilities
- Auto-cleanup for expired dead letter messages
- Statistics collection for DLQ processing

### Changed
- Refactored retry mechanism for better reliability
- Improved message serialization/deserialization

## [1.1.0] - 2025-01-01

### Added
- Transaction support for Pulsar messages
- `@PulsarTransactional` annotation for declarative transactions
- Programmatic transaction API
- Transaction timeout configuration

### Changed
- Updated Spring Boot to 3.1.5
- Improved connection pooling

### Fixed
- Connection leak in certain failure scenarios

## [1.0.0] - 2024-12-15

### Added
- Initial release
- `@EnablePulsar` annotation to enable Pulsar functionality
- `@PulsarListener` annotation for message consumption
- `PulsarMessageSender` for message production
- Support for synchronous and asynchronous message sending
- Delayed message support
- Keyed message support
- Batch message sending
- Multiple subscription types (Exclusive, Shared, Failover, Key_Shared)
- Authentication support (JWT tokens)
- TLS/SSL configuration
- Retry mechanism with configurable policies
- Health check endpoint
- Comprehensive configuration properties

### Dependencies
- Java 17+
- Spring Boot 3.0+
- Apache Pulsar Client 3.2.4

---

## Version History Summary

| Version | Release Date | Highlights |
|---------|--------------|------------|
| 1.2.22  | 2025-01-29   | Multi-instance support, enhanced testing |
| 1.2.0   | 2025-01-15   | Dead letter queue support |
| 1.1.0   | 2025-01-01   | Transaction support |
| 1.0.0   | 2024-12-15   | Initial release |

[Unreleased]: https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/compare/v1.2.22...HEAD
[1.2.22]: https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/compare/v1.2.0...v1.2.22
[1.2.0]: https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/qwzhang01/seven-spring-mq-pulsar-starter/releases/tag/v1.0.0
