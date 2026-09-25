# 电力风险智能交底系统 - 部署指南

## 📋 目录
- [服务器配置要求](#服务器配置要求)
- [方式一：Docker 一键部署（推荐）](#方式一docker-一键部署推荐)
- [方式二：Jar 包手动部署](#方式二jar-包手动部署)
- [功能模块说明](#功能模块说明)
- [环境变量配置](#环境变量配置)
- [数据持久化与备份](#数据持久化与备份)
- [常见问题](#常见问题)

---

## 服务器配置要求

| 配置级别 | CPU | 内存 | 硬盘 | 说明 |
|----------|-----|------|------|------|
| **最低可跑** | 2核 | 2GB | 20GB | 可运行基础功能，本地YOLO检测可用，并发能力有限，适合小型项目/测试 |
| **生产入门** | 4核 | 4GB | 50GB | 日常使用流畅，支持10-20人同时在线，检测速度更快 |
| **推荐配置** | 4核 | 8GB | 100GB+ | 性能充裕，视觉检测更快，支持更多并发 |

> ✅ **重要说明**：
> - ✅ **2核2G 可运行**：使用轻量级 YOLO11-Nano 本地检测模型，PPE检测速度约200-300ms/张
> - ✅ AI 对话功能基于 DeepSeek API 实现，**不需要本地大模型**
> - ✅ 视觉检测基于本地 YOLO 模型实现，**不需要GPU，纯CPU即可运行**
> - 🎯 服务器配置要求低，2核2G即可运行全部功能

---

## 方式一：Docker 一键部署（推荐）

### 前置准备

服务器已安装 Docker 和 Docker Compose：
```bash
docker --version
docker compose version
```

### 部署步骤

#### 1. 上传项目文件

将项目目录（或 git clone）上传到服务器：
```bash
git clone <仓库地址>
cd ELeDangerCheck
```

#### 2. 配置环境变量

```bash
# 复制配置模板
cp .env.example .env

# 编辑配置（至少修改以下几项）
vim .env
```

**必须修改的配置**：
```env
# 数据库密码（建议修改默认密码）
MYSQL_ROOT_PASSWORD=你的强密码

# DeepSeek API Key（AI对话功能必需）
DEEPSEEK_API_KEY=sk-你的api-key

# JWT密钥（生产环境务必修改）
JWT_SECRET=你的自定义随机字符串
```

#### 3. 一键启动

```bash
docker compose up -d
```

首次启动会自动完成：
- ✅ 拉取 MySQL 8.0 镜像
- ✅ 构建 Spring Boot 应用镜像（含 Maven 编译）
- ✅ 自动创建数据库和所有表结构
- ✅ 初始化示例数据
- ✅ 连接腾讯云TIIA视觉检测API（需配置）

#### 4. 查看启动状态

```bash
# 查看容器状态
docker compose ps

# 查看启动日志
docker compose logs -f app
```

看到 `Started ELeDangerCheckApplication` 表示启动成功。

#### 5. 访问系统

打开浏览器访问：`http://服务器IP:8081`

默认账号：`admin` / `admin123`

> ⚠️ 首次登录后请立即修改默认密码！

### 常用命令

```bash
# 启动服务
docker compose up -d

# 停止服务
docker compose down

# 重启服务
docker compose restart

# 重新构建并启动（代码更新后）
docker compose up -d --build

# 查看应用日志
docker compose logs -f app

# 查看数据库日志
docker compose logs -f mysql
```

---

## 方式二：Jar 包手动部署

### 前置准备

- JDK 17+
- MySQL 8.0+
- 项目 Jar 包（`target/ELeDangerCheck-0.0.1-SNAPSHOT.jar`）

### 部署步骤

#### 1. 准备数据库

```sql
-- 创建数据库
CREATE DATABASE eledangercheck CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行建表脚本（在 src/main/resources/schema-mysql.sql）
mysql -uroot -p eledangercheck < schema-mysql.sql
```

#### 2. 上传 Jar 包

```bash
mkdir -p /opt/eledangercheck
cd /opt/eledangercheck
# 上传 ELeDangerCheck-0.0.1-SNAPSHOT.jar 到当前目录
```

#### 3. 创建配置文件

```bash
vim application-prod.yml
```

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/eledangercheck?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8
    username: root
    password: 你的数据库密码
    driver-class-name: com.mysql.cj.jdbc.Driver

deepseek:
  api-key: sk-你的api-key
  base-url: https://api.deepseek.com/v1

jwt:
  secret: 你的自定义JWT密钥
```

#### 4. 启动应用

```bash
# 前台启动（测试用）
java -jar ELeDangerCheck-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 后台启动（生产用）
nohup java -jar ELeDangerCheck-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > app.log 2>&1 &
```

#### 5. 配置开机自启（可选）

创建 systemd 服务：
```bash
vim /etc/systemd/system/eledangercheck.service
```

```ini
[Unit]
Description=ELeDangerCheck Application
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/eledangercheck
ExecStart=/usr/bin/java -jar ELeDangerCheck-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用并启动：
```bash
systemctl daemon-reload
systemctl enable eledangercheck
systemctl start eledangercheck
```

---

## 功能模块说明

| 模块 | 状态 | 说明 | 访问地址 |
|------|------|------|----------|
| 用户登录 | ✅ 可用 | 默认账号 admin/admin123 | /index.html |
| 控制台首页 | ✅ 可用 | 数据概览 | /dashboard.html |
| 任务管理 | ✅ 可用 | 作业任务管理 | /dashboard.html?module=tasks |
| 风险辨识 | ✅ 可用 | 危险点识别 | /dashboard.html?module=hazards |
| 安全交底 | ✅ 可用 | 交底卡生成与管理 | /dashboard.html?module=disclosures |
| 智能AGENT | ✅ 可用 | AI对话助手（需配置API Key） | /agent-entry.html |
| 视觉监控 | ✅ 可用 | 本地YOLO PPE检测（安全帽/反光衣等） | /vision-monitor.html |
| 天气预警 | ✅ 可用 | 恶劣天气检测 | /dashboard.html?module=weather |
| 事故案例 | ✅ 可用 | 案例库管理 | /dashboard.html?module=cases |
| 字典管理 | ✅ 可用 | 数据字典维护 | /dashboard.html?module=dict |
| 系统设置 | ✅ 可用 | 参数配置 | /settings.html |

> **关于视觉检测**：基于本地 YOLO11-Nano 模型实现，纯CPU即可运行，支持安全帽、反光衣等PPE装备检测，2核2G服务器约200-300ms/张。

---

## 环境变量配置

所有可配置项（`.env` 文件）：

```env
# ---------- 数据库配置 ----------
MYSQL_PORT=3306               # MySQL端口映射
MYSQL_ROOT_PASSWORD=          # MySQL root密码（必填，请自行设定强密码）
MYSQL_DATABASE=eledangercheck # 数据库名
MYSQL_USERNAME=root           # 数据库用户名

# ---------- 应用配置 ----------
APP_PORT=8081                 # 应用端口
JWT_SECRET=                   # JWT密钥（必填，32位以上随机字符串）

# ---------- DeepSeek API（必需）----------
DEEPSEEK_API_KEY=             # DeepSeek API Key（AI对话功能必需）
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1

# ---------- FastChat API（可选）----------
FASTCHAT_API_KEY=             # FastChat API Key（可选备用）
FASTCHAT_BASE_URL=https://api.fastchat.cn

# ---------- 腾讯云 TIIA（可选）----------
TENCENT_TIIA_ENABLED=false
TENCENT_TIIA_SECRET_ID=
TENCENT_TIIA_SECRET_KEY=
TENCENT_TIIA_REGION=ap-guangzhou
```

> 完整键位清单见项目根目录 `.env.example`。

---

## 数据持久化与备份

### Docker 数据卷

| 数据类型 | 卷名 | 说明 |
|----------|------|------|
| MySQL数据 | `eledangercheck_mysql-data` | 数据库文件 |
| 文档输出 | `eledangercheck_app-documents` | 生成的Word/PDF文件 |
| 上传文件 | `eledangercheck_app-uploads` | 用户上传的文件 |

### 数据库备份

```bash
# 备份（密码取自 .env 中的 MYSQL_ROOT_PASSWORD）
docker exec eledangercheck-mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" eledangercheck > backup_$(date +%Y%m%d).sql

# 恢复
docker exec -i eledangercheck-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" eledangercheck < backup.sql
```

---

## 常见问题

### 1. 启动后访问报 403 / 无法登录

检查 Spring Security 配置，确认静态资源路径已放行。查看应用日志：
```bash
docker compose logs app | grep -i error
```

### 2. AI 对话功能用不了

检查 DeepSeek API Key 是否正确配置：
```bash
docker compose exec app env | grep DEEPSEEK
```

确认 API Key 有效且账户有余额。

### 3. 视觉检测功能报错

检查本地YOLO模型是否加载成功：
```bash
# 查看应用日志中模型加载状态
docker compose logs app | grep -i "PPE\|YOLO\|模型"
```

确认模型文件存在：
- 模型文件路径：`models/ppe-yolo11n.onnx` 或项目根目录下的 `best.onnx`
- 如果模型文件缺失，请将PPE检测模型文件放到项目根目录

常见问题：
- **检测到0人**：可能是图片中目标太小或角度问题，可适当调低置信度阈值（默认0.25）
- **检测速度慢**：2核服务器约200-300ms/张，属正常范围
- **模型加载失败**：检查模型文件是否完整，文件大小应大于5MB

### 4. 内存不足 / OOM

- 2核2G服务器建议调整 JVM 内存参数：
  ```bash
  java -Xms256m -Xmx768m -jar app.jar
  ```
- 4G及以上内存可使用默认配置
- 确保 MySQL 也配置了合理的内存占用

### 5. 端口被占用

修改 `.env` 中的端口配置：
```env
APP_PORT=8082   # 改成其他端口
MYSQL_PORT=3307 # 改成其他端口
```

### 6. MySQL 启动慢 / 连接超时

首次启动 MySQL 需要初始化数据，等待 30-60 秒。应用会自动重试连接。

### 7. 中文显示乱码

已默认配置 utf8mb4 字符集。如仍有乱码，检查：
```bash
docker exec eledangercheck-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW VARIABLES LIKE 'character%';"
```

---

## 技术栈

- **后端**：Spring Boot 3.2 + Java 17
- **数据库**：MySQL 8.0
- **ORM**：MyBatis-Plus 3.5
- **安全**：Spring Security + JWT
- **AI大模型**：DeepSeek API（在线调用，无需本地部署）
- **视觉检测**：腾讯云TIIA图像分析API（在线调用，无需本地模型）
- **前端**：原生HTML + CSS + JavaScript
- **文档生成**：Apache POI (Word) + iText (PDF)
- **部署**：Docker + Docker Compose
