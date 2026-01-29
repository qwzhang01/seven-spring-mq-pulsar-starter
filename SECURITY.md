# Security Policy

[中文版](#安全政策)

## Supported Versions

We release patches for security vulnerabilities. Which versions are eligible for receiving such patches depend on the CVSS v3.0 Rating:

| Version | Supported          |
| ------- | ------------------ |
| 1.2.x   | :white_check_mark: |
| 1.1.x   | :white_check_mark: |
| 1.0.x   | :x:                |
| < 1.0   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability within this project, please send an email to **avinzhang@tencent.com**. All security vulnerabilities will be promptly addressed.

Please include the following information in your report:

- **Type of vulnerability** (e.g., buffer overflow, SQL injection, cross-site scripting)
- **Full paths of source file(s)** related to the vulnerability
- **Location of the affected source code** (tag/branch/commit or direct URL)
- **Any special configuration** required to reproduce the issue
- **Step-by-step instructions** to reproduce the issue
- **Proof-of-concept or exploit code** (if possible)
- **Impact of the vulnerability**, including how an attacker might exploit it

### What to Expect

- **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours.
- **Investigation**: We will investigate and determine the scope and severity of the issue.
- **Updates**: We will keep you informed of the progress towards a fix.
- **Resolution**: We will notify you when the vulnerability has been fixed.
- **Credit**: We will credit you in the release notes (unless you prefer to remain anonymous).

### Public Disclosure

Please do not publicly disclose the vulnerability until we have had a chance to address it. We aim to resolve critical security issues within 30 days.

## Security Best Practices

When using Seven Spring MQ Pulsar Starter in production:

1. **Authentication**: Always enable authentication in production environments
   ```yaml
   spring:
     pulsar:
       authentication:
         enabled: true
         token: ${PULSAR_JWT_TOKEN}
   ```

2. **TLS/SSL**: Use TLS encryption for all Pulsar connections
   ```yaml
   spring:
     pulsar:
       client:
         tls-trust-certs-file-path: /path/to/ca.crt
         tls-allow-insecure-connection: false
   ```

3. **Secrets Management**: Never hardcode credentials; use environment variables or secret management tools

4. **Network Security**: Restrict network access to Pulsar clusters using firewalls and security groups

5. **Regular Updates**: Keep dependencies up to date to receive security patches

---

# 安全政策

## 支持的版本

我们为安全漏洞发布补丁。哪些版本有资格获得此类补丁取决于 CVSS v3.0 评级：

| 版本    | 支持状态           |
| ------- | ------------------ |
| 1.2.x   | :white_check_mark: |
| 1.1.x   | :white_check_mark: |
| 1.0.x   | :x:                |
| < 1.0   | :x:                |

## 报告漏洞

如果您发现此项目中的安全漏洞，请发送电子邮件至 **avinzhang@tencent.com**。所有安全漏洞将得到及时处理。

请在您的报告中包含以下信息：

- **漏洞类型**（例如，缓冲区溢出、SQL 注入、跨站脚本）
- 与漏洞相关的**源文件完整路径**
- **受影响源代码的位置**（标签/分支/提交或直接 URL）
- 复现问题所需的**任何特殊配置**
- 复现问题的**分步说明**
- **概念验证或利用代码**（如可能）
- **漏洞的影响**，包括攻击者可能如何利用它

### 预期流程

- **确认收到**：我们将在 48 小时内确认收到您的漏洞报告。
- **调查**：我们将调查并确定问题的范围和严重性。
- **更新**：我们将随时通知您修复的进展。
- **解决**：我们将在漏洞修复后通知您。
- **致谢**：我们将在发布说明中感谢您（除非您希望保持匿名）。

### 公开披露

请不要在我们有机会解决漏洞之前公开披露。我们的目标是在 30 天内解决关键安全问题。

## 安全最佳实践

在生产环境中使用 Seven Spring MQ Pulsar Starter 时：

1. **身份验证**：始终在生产环境中启用身份验证
   ```yaml
   spring:
     pulsar:
       authentication:
         enabled: true
         token: ${PULSAR_JWT_TOKEN}
   ```

2. **TLS/SSL**：对所有 Pulsar 连接使用 TLS 加密
   ```yaml
   spring:
     pulsar:
       client:
         tls-trust-certs-file-path: /path/to/ca.crt
         tls-allow-insecure-connection: false
   ```

3. **密钥管理**：永远不要硬编码凭据；使用环境变量或密钥管理工具

4. **网络安全**：使用防火墙和安全组限制对 Pulsar 集群的网络访问

5. **定期更新**：保持依赖项最新以获取安全补丁
