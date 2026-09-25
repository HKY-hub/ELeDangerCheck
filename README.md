# 电力风险智能交底系统（ELeDangerCheck）

基于YOLO与大模型的电力作业安全智能检测平台

## 🚀 快速部署

```bash
# 克隆项目
git clone <仓库地址>
cd ELeDangerCheck

# 配置环境变量（修改 DeepSeek API Key）
cp .env.example .env
# 编辑 .env，填入 DEEPSEEK_API_KEY

# 一键启动
docker compose up -d

# 访问 http://服务器IP:8081
# 默认账号：admin / admin123
```

> 详细部署说明见 [DEPLOY.md](./DEPLOY.md)

## 项目简介

电力风险智能交底系统是一个面向电力行业的智能安全管理平台，集成了AI视觉检测、大模型智能交底、气象风险评估等功能，旨在提升电力作业现场的安全管理水平。

## 技术栈

### 前端技术
- **HTML5 + CSS3 + JavaScript** - 原生前端技术栈
- **Canvas API** - 检测结果可视化绘制
- **pc-style 设计系统** - 统一的UI设计规范

### 后端技术
- **Spring Boot 3.x** - 后端主框架
- **Java 17** - 开发语言
- **MyBatis** - ORM数据访问框架
- **Spring Security + JWT** - 安全认证框架

### 数据库
- **MySQL 8.0** - 关系型数据库

### AI 能力
- **YOLOv11 ONNX** - 目标检测模型（本地部署）
- **PPE-YOLO11-Nano** - 个人防护装备检测模型（本地部署）
- **OpenCV** - 计算机视觉处理库
- **DeepSeek** - 大模型API接口

### 第三方服务
- 腾讯云PPE API（可选，作为fallback）
- 阿里云TTS语音合成（可选）

## 环境要求

### 操作系统
- Windows 10/11 或 Windows Server 2019+
- Linux（CentOS 7+ / Ubuntu 20.04+）

### 软件环境
- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 硬件要求

| 配置级别 | CPU | 内存 | 硬盘 | 说明 |
|----------|-----|------|------|------|
| **最低可跑** | 2核 | 2GB | 20GB | 可运行基础功能，本地YOLO检测可用，并发能力有限，适合小型项目/测试 |
| **生产入门** | 4核 | 4GB | 50GB | 日常使用流畅，支持10-20人同时在线，检测速度更快 |
| **推荐配置** | 4核 | 8GB | 100GB+ | 性能充裕，视觉检测更快，支持更多并发 |

> ✅ **2核2G可运行**：使用轻量级 YOLO11-Nano 本地检测模型，PPE检测速度约200-300ms/张，完全满足日常使用需求。
>
> 大模型对话走云端API（DeepSeek），服务器不需要GPU。

## 项目结构

```
ELeDangerCheck/
├── src/main/java/org/example/eledangercheck/
│   ├── controller/          # 控制层（14个Controller）
│   ├── service/             # 业务层（16个Service）
│   ├── mapper/              # 数据访问层（12个Mapper）
│   ├── entity/              # 实体类（12个Entity）
│   ├── dto/                 # 数据传输对象
│   ├── config/              # 配置类
│   ├── security/            # 安全相关（JWT认证）
│   ├── util/                # 工具类
│   ├── exception/           # 异常处理
│   └── ELeDangerCheckApplication.java
├── src/main/resources/
│   ├── static/              # 前端静态文件
│   │   ├── index.html       # 登录页
│   │   ├── register.html    # 注册页
│   │   ├── dashboard.html   # 首页仪表盘
│   │   ├── chat.html        # 智能交底AGENT
│   │   ├── vision-monitor.html  # 视觉检测
│   │   ├── weather-risk.html    # 天气预警
│   │   ├── accident-cases.html  # 事故案例
│   │   ├── disclosure-detail.html # 交底详情
│   │   ├── profile.html     # 个人中心
│   │   ├── css/             # 样式文件
│   │   └── js/              # 脚本文件
│   ├── models/              # AI模型文件（已内置）
│   │   ├── yolo11x.onnx
│   │   ├── ppe-yolo11n.onnx
│   │   └── ...
│   ├── mapper/              # MyBatis XML映射
│   └── application.yml      # 配置文件
└── pom.xml                  # Maven依赖
```

## 核心功能模块

### 1. 用户管理
- 用户注册与登录（JWT认证）
- 个人信息管理
- 权限控制

### 2. 智能安全交底
- AI对话AGENT（DeepSeek API驱动）
- 交底单自动生成
- 交底记录管理
- 电子签名
- 语音播报（可选）

### 3. 视觉违章检测
- 图片检测
- 视频抽帧检测
- 6类违章识别：
  - 安全帽检测
  - 安全带检测
  - 反光衣检测
  - 玩手机检测
  - 吸烟检测
  - 火灾检测
- 检测类型独立开关
- 实时可视化标注

### 4. 天气风险预警
- 城市天气搜索
- 实时天气展示
- 7天天气预报
- 气象风险智能评估
- 作业建议

### 5. 事故案例库
- 案例列表与分类
- 案例详情查看
- 关键词搜索

### 6. 任务管理
- 任务CRUD操作
- 任务状态管理
- 任务查询筛选

### 7. 危险源管理
- 危险源辨识与管理
- LEC风险评估法
- 控制措施管理

## 快速开始

### 方式一：Docker 一键部署（推荐）

使用 Docker Compose 一键启动所有服务，无需手动配置环境。

```bash
# 1. 克隆项目
git clone <仓库地址>
cd ELeDangerCheck

# 2. 复制环境变量配置文件（可选，默认配置即可运行）
cp .env.example .env
# 编辑 .env 文件，填入你的 API Key 等配置

# 3. 一键启动
docker compose up -d

# 4. 访问系统
# 打开浏览器访问：http://localhost:8081
```

**包含的服务**：
- MySQL 8.0 数据库（自动初始化表结构）
- Spring Boot 应用（自动构建并启动）

**常用命令**：
```bash
# 查看日志
docker compose logs -f app

# 停止服务
docker compose down

# 重启服务
docker compose restart

# 重新构建并启动（代码更新后）
docker compose up -d --build
```

### 方式二：本地开发部署

#### 1. 数据库准备

创建MySQL数据库并导入初始数据：

```sql
CREATE DATABASE eledangercheck CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置文件修改

修改 `src/main/resources/application.yml` 中的数据库连接配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/eledangercheck?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

### 3. AI服务配置

所有AI能力均基于云API实现，无需本地部署模型，服务器配置要求低：

- **大模型对话**：DeepSeek API
- **视觉违章检测**：腾讯云TIIA图像分析API

配置方式见 [DEPLOY.md](./DEPLOY.md) 中的环境变量说明。

### 4. 编译运行

```bash
# 编译项目
mvn clean package

# 运行
mvn spring-boot:run
```

或运行打包后的jar：

```bash
java -jar target/ELeDangerCheck-0.0.1-SNAPSHOT.jar
```

### 5. 访问系统

打开浏览器访问：http://localhost:8081

默认服务端口：8081

## 数据库表结构

共12张核心数据表：

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| task | 任务表 |
| hazard | 危险源表 |
| hazard_dict | 危险源字典表 |
| measure_dict | 措施字典表 |
| safety_measure | 安全措施表 |
| disclosure | 交底记录表 |
| disclosure_sign | 交底签名表 |
| accident_case | 事故案例表 |
| evaluation | 风险评估表 |
| weather_city | 城市天气表 |
| task_parse | 任务解析表 |

## 注意事项

1. **大模型**：智能交底AGENT功能基于DeepSeek API实现，需配置有效的API Key
2. **视觉检测**：违章检测功能基于腾讯云TIIA API实现，需配置有效的SecretId/SecretKey
3. **轻量部署**：所有AI能力均走云端API，服务器不需要GPU，2核4G即可运行
4. **语音播报**：阿里云TTS等为可选配置，不配置时使用浏览器语音合成

## 开发团队

- 项目经理
- 产品经理
- 前端开发工程师
- 后端开发工程师
- 测试工程师
