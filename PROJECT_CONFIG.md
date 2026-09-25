# 项目环境与配置记忆文件

> **创建时间：** 2026-07-15
> **项目名称：** 电力作业风险辨识与安全交底 Agent
> **项目路径：** d:\AGENT\eledangercheck

---

## 一、技术栈版本

### 核心框架
| 名称 | 版本 | 说明 |
|------|------|------|
| Java | 17.0.11 | JDK 版本 |
| Spring Boot | 3.2.5 | 主框架版本 |
| MyBatis-Plus | 3.5.5.1 | ORM 框架（Spring Boot 3 适配版） |

### 数据库
| 名称 | 版本 | 连接信息 |
|------|------|----------|
| MySQL | 8.0+ | jdbc:mysql://localhost:3306/eledangercheck |
| 用户名 | root | - |
| 密码 | 见 `secrets-local.yml`（本地，不入库） | - |
| 字符集 | utf8mb4 | 支持中文和 emoji |

### 安全组件
| 名称 | 版本 | 说明 |
|------|------|------|
| Spring Security | 6.2.5 | 随 Spring Boot 3.2.5 自动引入 |
| JJWT | 0.12.3 | JWT 认证 |
| BCrypt | Spring Security 内置 | 密码加密 |

### 文档生成
| 名称 | 版本 | 说明 |
|------|------|------|
| Apache POI | 5.2.5 | Word 文档生成 |
| iText Core | 8.0.2 | PDF 文档生成 |

### JSON 处理
| 名称 | 版本 | 说明 |
|------|------|------|
| FastJSON | 1.2.83 | JSON 解析 |
| Jackson | 2.16.5 | Spring Boot 默认 JSON 处理器 |

---

## 二、服务配置

### 端口配置
| 服务 | 端口 | 说明 |
|------|------|------|
| Spring Boot 应用 | 8081 | 主服务端口 |
| MySQL | 3306 | 数据库端口 |

### 应用配置文件
| 文件 | 作用 |
|------|------|
| application.yml | 默认配置 |
| application-mysql.yml | MySQL 环境配置（激活中） |

### 配置激活
- 激活 profile：`mysql`
- 通过 `spring.profiles.active=mysql` 指定

---

## 三、第三方服务配置

### DeepSeek AI API（模拟模式）
```yaml
deepseek:
  api-key: your_deepseek_api_key
  base-url: https://api.deepseek.com/v1/chat/completions
```

### 阿里云 TTS（模拟模式）
```yaml
aliyun:
  tts:
    access-key-id: your_aliyun_access_key_id
    access-key-secret: your_aliyun_access_key_secret
    region-id: cn-hangzhou
    voice-name: zh-CN-XiaoxiaoNeural
```

### e签宝（模拟模式）
```yaml
esign:
  app-id: your_esign_app_id
  app-secret: your_esign_app_secret
```

### 模拟模式开关
```yaml
simulation:
  enabled: true
  tts-enabled: true
  esign-enabled: true
```

---

## 四、项目结构

```
eledangercheck/
├── src/main/java/org/example/eledangercheck/
│   ├── controller/          # 11个 Controller
│   ├── service/             # 12个 Service
│   ├── mapper/              # 11个 Mapper
│   ├── entity/              # 11个 Entity
│   ├── dto/                 # 3个 DTO
│   ├── config/              # 11个配置类
│   ├── security/            # JWT Token Provider
│   ├── util/                # JWT 工具类
│   ├── exception/           # 业务异常类
│   └── ELeDangerCheckApplication.java
├── src/main/resources/
│   ├── static/              # 前端静态资源
│   │   ├── css/             # 样式文件
│   │   ├── js/              # JavaScript 文件
│   │   └── *.html           # 前端页面
│   ├── application.yml      # 默认配置
│   ├── application-mysql.yml # MySQL 配置
│   └── logback-spring.xml   # 日志配置
├── pom.xml                  # Maven 依赖管理
├── AGENT_MEMORY.md          # 项目记忆文件
├── PROJECT_CONFIG.md        # 环境配置记忆（本文件）
└── mvnw/mvnw.cmd            # Maven Wrapper
```

---

## 五、启动方式

### 开发环境启动
```bash
# Windows PowerShell
$env:JAVA_HOME="C:\Users\h'k'y\java\jdk-17.0.11+9"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
cd d:\AGENT\eledangercheck
.\mvnw.cmd spring-boot:run
```

### 打包构建
```bash
cd d:\AGENT\eledangercheck
.\mvnw.cmd clean package
```

### 运行打包后的 JAR
```bash
java -jar target/ELeDangerCheck-0.0.1-SNAPSHOT.jar
```

---

## 六、数据库表结构

共 11 张表：
1. **user** — 用户表
2. **task** — 作业任务表
3. **task_parse** — 任务解析结果表
4. **hazard** — 危险点表
5. **safety_measure** — 安全措施表
6. **disclosure** — 安全交底表
7. **disclosure_sign** — 交底签名表
8. **accident_case** — 事故案例表
9. **hazard_dict** — 危险点字典表
10. **measure_dict** — 控制措施字典表
11. **evaluation** — 评估结果表

---

## 七、API 接口前缀

| 模块 | 前缀 |
|------|------|
| 认证 | /api/auth |
| 任务管理 | /api/tasks |
| 危险点管理 | /api/hazards |
| 安全措施 | /api/safety-measures |
| 安全交底 | /api/disclosures |
| 事故案例 | /api/accident-cases |
| 字典管理 | /api/dict |
| Agent 智能对话 | /api/agent |
| 语音播报 | /api/tts |
| 文档下载 | /api/document |

---

## 八、安全配置

### 认证策略
- 认证方式：JWT 无状态认证（STATELESS）
- 密码加密：BCrypt
- CSRF：关闭

### 放行路径
- 静态资源：`/`, `/css/**`, `/js/**`, `/*.html`
- API 健康检查：`/api/auth/health`
- 注册登录：`/api/auth/register`, `/api/auth/login`

### CORS 配置
- 允许所有来源（开发环境）
- 允许方法：GET, POST, PUT, DELETE, OPTIONS
- 允许头：*

---

## 九、已知问题与限制

1. **第三方服务模拟模式**：TTS、e签宝、DeepSeek 当前为模拟模式，需配置真实 API 密钥才能启用真实服务
2. **Elasticsearch**：可选组件，当前未启用
3. **Drools 规则引擎**：占位配置，当前使用纯 Java 规则实现

---

## 十、规则备忘录

> **规则：** 当遇到无法解决的依赖、环境或配置冲突，工具无法自行下载或安装时，暂停任务并向用户主动要求提供应有的环境，由用户配置并导入供使用。
