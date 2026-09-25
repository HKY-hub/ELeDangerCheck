# Ele_Agent 项目 Code Wiki

> 项目名称：**Ele_DangerCheck (电力作业安全交底系统)**
> 技术栈：Spring Boot 4.1.0 (Java 17) + Vue 3 + MySQL + Elasticsearch + DeepSeek LLM
> 仓库路径：[c:\Users\Nor\Desktop\Code\Ele_Agent](file:///c:/Users/Nor/Desktop/Code/Ele_Agent)

---

## 目录

1. [项目概述](#1-项目概述)
2. [整体架构](#2-整体架构)
3. [目录结构](#3-目录结构)
4. [后端模块详解](#4-后端模块详解)
   - [4.1 启动入口](#41-启动入口)
   - [4.2 控制器层 (Controller)](#42-控制器层-controller)
   - [4.3 服务层 (Service)](#43-服务层-service)
   - [4.4 数据实体层 (Entity)](#44-数据实体层-entity)
   - [4.5 数据传输对象 (DTO)](#45-数据传输对象-dto)
   - [4.6 仓储层 (Repository)](#46-仓储层-repository)
   - [4.7 配置层 (Config)](#47-配置层-config)
5. [前端模块详解](#5-前端模块详解)
   - [5.1 工程配置](#51-工程配置)
   - [5.2 路由与页面](#52-路由与页面)
   - [5.3 API 封装](#53-api-封装)
6. [基础设施模块](#6-基础设施模块)
7. [核心业务调用链](#7-核心业务调用链)
8. [依赖关系](#8-依赖关系)
9. [项目运行方式](#9-项目运行方式)
10. [数据库表结构](#10-数据库表结构)

---

## 1. 项目概述

**Ele_DangerCheck** 是一个面向电力行业的安全作业辅助系统，核心功能为：

- **作业任务解析**：通过自然语言描述（如"在 10kV 高压柜前进行检修作业"），自动解析出电压等级、设备类型、作业类型等结构化字段。
- **危险点识别**：基于内置规则引擎 + Elasticsearch RAG 增强检索 + DeepSeek LLM，自动识别作业中的安全隐患点及对应的控制措施。
- **安全交底卡生成**：根据识别出的危险点及其控制措施，自动生成标准化的安全技术交底卡。
- **对话式 Agent**：提供多轮对话界面，引导用户逐步完成"任务描述 → 字段补全 → 确认危险点 → 生成交底卡"的完整流程。

项目面向两类用户入口：

- **表单模式**：在 Dashboard 填写任务描述 → 自动解析与识别 → 查看结果详情
- **对话模式**：通过 Agent 多轮对话逐步完成上述流程，体验更自然

---

## 2. 整体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Ele_DangerCheck 全栈应用                       │
│                                                                         │
│   ┌─────────────────────────────┐      ┌───────────────────────────┐   │
│   │   前端 (Vue 3 + Vite)       │      │  后端 (Spring Boot 4.1)   │   │
│   │                             │      │                           │   │
│   │  ┌─ Login.vue              │      │  ┌─ Controllers          │   │
│   │  ├─ Register.vue           │─/api→│  ├─ Services             │   │
│   │  ├─ Dashboard.vue          │      │  ├─ Repositories         │   │
│   │  ├─ TaskDetail.vue         │      │  └─ Configs              │   │
│   │  └─ AgentConversation.vue  │      │                           │   │
│   └──────────┬──────────────────┘      └────┬──────────────────────┘   │
│              │                               │                          │
│              │  (开发态代理)                  │                          │
│              │  Vite :5173 → :8081           │                          │
│              │                               │                          │
│   ┌──────────▼────────────────────────────────▼──────────────────────┐  │
│   │                   数据层 / 外部依赖                                │  │
│   │                                                                    │  │
│   │  ┌──────────────┐     ┌────────────────┐     ┌───────────────┐   │  │
│   │  │   MySQL       │     │  Elasticsearch │     │  DeepSeek API │   │  │
│   │  │   (持久化)     │     │  (RAG 案例检索) │     │  (LLM 推理)    │   │  │
│   │  └──────────────┘     └────────────────┘     └───────────────┘   │  │
│   └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 架构要点

| 维度 | 说明 |
|------|------|
| **前后端关系** | 开发态前后端分离（Vite 代理），生产态前端构建产物嵌入后端 `static/` 目录统一部署 |
| **持久化** | MySQL + JPA（`ddl-auto: update` 自动建表/更新） |
| **检索增强** | Elasticsearch 存储历史事故案例，通过 RAG 方式增强危险点识别 |
| **AI 能力** | DeepSeek Chat API，用于任务解析、交底卡生成等（可选，关闭后降级为规则引擎） |
| **认证方案** | 简化方案：`user-id` 请求头标识用户，Spring Security 基础框架已搭但未严格鉴权 |

### 启用/禁用外部依赖

| 组件 | 配置项 | 关闭影响 |
|------|--------|----------|
| DeepSeek LLM | `deepseek.enabled: true` → `false` | 任务解析回退正则，交底卡回退模板，Agent 对话降级规则 |
| Elasticsearch | `es.enabled: true` → `false` | 危险点识别跳过 RAG 检索，仅依赖规则引擎 |

---

## 3. 目录结构

```
Ele_Agent/
├── ELeDangerCheck/                  # 后端工程 (Spring Boot)
│   ├── pom.xml                      # Maven 构建配置
│   ├── mvnw / mvnw.cmd              # Maven Wrapper
│   └── src/
│       ├── main/
│       │   ├── java/org/example/eledangercheck/
│       │   │   ├── ELeDangerCheckApplication.java    # 启动入口
│       │   │   ├── config/          # 配置类
│       │   │   │   ├── LlmConfig.java                # DeepSeek 配置绑定
│       │   │   │   ├── SecurityConfig.java           # Spring Security 配置
│       │   │   │   └── SpaFallbackFilter.java        # SPA 路由兜底
│       │   │   ├── controller/      # REST 控制器
│       │   │   │   ├── AuthController.java           # 认证接口
│       │   │   │   ├── TaskController.java           # 任务 CRUD 接口
│       │   │   │   └── ConversationAgentController.java # Agent 对话接口
│       │   │   ├── dto/             # 数据传输对象
│       │   │   │   ├── ApiResponse.java              # 统一响应包装
│       │   │   │   ├── LoginRequest.java             # 登录请求
│       │   │   │   ├── RegisterRequest.java          # 注册请求
│       │   │   │   ├── UserInfo.java                 # 用户信息响应
│       │   │   │   ├── TaskCreateRequest.java        # 创建任务请求
│       │   │   │   ├── TaskResponse.java             # 任务详情响应
│       │   │   │   ├── AgentMessageRequest.java      # Agent 消息请求
│       │   │   │   └── AgentMessageResponse.java     # Agent 回复响应
│       │   │   ├── entity/          # JPA 实体
│       │   │   │   ├── User.java                     # 用户实体
│       │   │   │   ├── Task.java                     # 作业任务实体
│       │   │   │   ├── HazardPoint.java              # 危险点实体
│       │   │   │   └── ControlMeasure.java           # 控制措施实体
│       │   │   ├── repository/      # JPA 数据仓储
│       │   │   │   ├── UserRepository.java
│       │   │   │   ├── TaskRepository.java
│       │   │   │   ├── HazardPointRepository.java
│       │   │   │   └── ControlMeasureRepository.java
│       │   │   └── service/         # 业务服务
│       │   │       ├── AuthService.java              # 认证服务
│       │   │       ├── TaskService.java              # 任务服务
│       │   │       ├── TaskParserService.java        # 任务解析服务
│       │   │       ├── HazardIdentificationService.java # 危险点识别服务
│       │   │       ├── BriefingService.java          # 交底卡生成服务
│       │   │       ├── ConversationAgentService.java # 对话 Agent 编排服务
│       │   │       ├── LlmService.java               # DeepSeek API 调用
│       │   │       └── ElasticsearchService.java     # ES 检索服务
│       │   └── resources/
│       │       ├── application.yaml                  # 主配置
│       │       └── static/          # 前端构建产物 (Vite build 输出)
│       │           ├── index.html
│       │           └── assets/
│       └── test/                    # 单元测试
│
├── frontend/                        # 前端工程 (Vue 3)
│   ├── package.json
│   ├── vite.config.js               # Vite 配置 (代理 + 构建输出)
│   ├── index.html
│   └── src/
│       ├── main.js                  # 入口
│       ├── App.vue                  # 根组件
│       ├── api/index.js             # Axios API 封装
│       ├── router/index.js          # Vue Router 路由
│       ├── components/              # 通用组件
│       │   └── ElectricBackground.vue
│       └── views/                   # 页面
│           ├── Login.vue
│           ├── Register.vue
│           ├── Dashboard.vue
│           ├── TaskDetail.vue
│           └── AgentConversation.vue
│
├── infra/                           # 基础设施
│   ├── docker-compose.yml           # ES + Kibana 容器编排
│   └── es-data/
│       ├── accident_cases.json      # 事故案例种子数据
│       └── setup_es.py              # ES 索引初始化脚本
│
└── docs/                            # 项目文档
    ├── wiki/                        # Code Wiki (本文档)
    ├── sql/SQL_table.txt            # 数据库设计参考
    ├── list/                        # 待办列表
    └── team-logs/                   # 团队日报
```

---

## 4. 后端模块详解

### 4.1 启动入口

#### [ELeDangerCheckApplication.java](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/ELeDangerCheckApplication.java)

```java
@SpringBootApplication
public class ELeDangerCheckApplication {
    public static void main(String[] args) {
        SpringApplication.run(ELeDangerCheckApplication.class, args);
    }
}
```

标准 Spring Boot 入口，`@SpringBootApplication` 自动扫描当前包及子包的所有组件。

---

### 4.2 控制器层 (Controller)

#### [AuthController](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/controller/AuthController.java)

用户注册与登录接口。

| 方法 | 路由 | 请求体 | 响应 | 说明 |
|------|------|--------|------|------|
| `POST` | `/api/auth/register` | `RegisterRequest` | `ApiResponse<UserInfo>` | 注册新用户 |
| `POST` | `/api/auth/login` | `LoginRequest` | `ApiResponse<UserInfo>` | 用户登录 |

> 当前无 JWT Token 返回，仅返回用户基本信息，前端存入 `sessionStorage` 并通过 `user-id` 请求头传递。

---

#### [TaskController](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/controller/TaskController.java)

任务 CRUD 接口（表单模式入口）。

| 方法 | 路由 | 请求头/请求体 | 响应 | 说明 |
|------|------|--------------|------|------|
| `POST` | `/api/tasks` | Header: `user-id` + Body: `TaskCreateRequest` | `ApiResponse<TaskResponse>` | 创建任务并同步完成解析与危险点识别 |
| `GET` | `/api/tasks` | Header: `user-id` | `ApiResponse<List<TaskResponse>>` | 查询当前用户所有任务 |
| `GET` | `/api/tasks/{taskId}` | — | `ApiResponse<TaskResponse>` | 查询单个任务详情（含危险点） |

> **关键函数：`createTask`**：接收 `taskDescription` 文本，同步调用 `TaskService.createTask`，后者依次执行：解析 → 保存 → 识别危险点 → 返回完整结果。

---

#### [ConversationAgentController](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/controller/ConversationAgentController.java)

对话式 Agent 接口。

| 方法 | 路由 | 请求头/请求体 | 响应 | 说明 |
|------|------|--------------|------|------|
| `POST` | `/api/agent/message` | Header: `user-id` + Body: `AgentMessageRequest` | `ApiResponse<AgentMessageResponse>` | 发送消息给 Agent |
| `GET` | `/api/agent/session` | Header: `user-id` | `ApiResponse<AgentMessageResponse>` | 获取当前会话状态 |
| `POST` | `/api/agent/reset` | Header: `user-id` | `ApiResponse<Void>` | 重置当前会话 |

> **关键函数：`sendMessage`**：将用户输入交给 `ConversationAgentService.processMessage`，后者根据当前会话阶段执行不同逻辑（字段提取 / 信息补全 / 危险点识别 / 交底卡生成）。

---

### 4.3 服务层 (Service)

#### [AuthService](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/service/AuthService.java)

| 函数 | 说明 |
|------|------|
| `register(RegisterRequest)` | 校验邮箱/用户名唯一性 → BCrypt 加密密码 → 保存用户 → 返回 `UserInfo` |
| `login(LoginRequest)` | 按邮箱查询用户 → 校验密码 → 返回 `UserInfo` (无 Token 机制) |

**依赖**：`UserRepository`, `PasswordEncoder`

---

#### [TaskService](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/service/TaskService.java)

| 函数 | 说明 |
|------|------|
| `createTask(Long userId, String taskDescription)` | ① 查找用户 → ② `TaskParserService.parse` 解析字段 → ③ 保存 `Task` → ④ `HazardIdentificationService.identifyHazards` 识别危险点 → ⑤ 组装 `TaskResponse` 返回 |
| `getUserTasks(Long userId)` | 查询用户所有任务，按创建时间倒序 |
| `getTask(Long taskId)` | 查询单个任务 + 组装危险点与控制措施 |
| `getTaskEntity(Long taskId)` | 获取纯实体（供 Agent 服务内部调用） |

**依赖**：`TaskRepository`, `UserRepository`, `TaskParserService`, `HazardIdentificationService`

---

#### [TaskParserService](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/service/TaskParserService.java)

| 函数 | 说明 |
|------|------|
| `parse(String taskDescription)` | 解析策略：优先调用 `llmParse()`（DeepSeek JSON 提取），失败则降级 `regexParse()`（正则 + 词典） |
| `llmParse(String)` | 调用 `LlmService.chatAsJson`，要求 LLM 返回结构化 JSON |
| `regexParse(String)` | 基于正则表达式和关键词词典抽取：电压等级 (`10kV/35kV/110kV`)、设备类型 (`变压器/开关柜/电缆`)、作业类型 (`检修/试验/安装`)、环境 (`室内/室外/井下`) |

**解析字段**：
| 字段 | 说明 | 示例值 |
|------|------|--------|
| `taskName` | 任务名称 | "10kV 高压柜检修作业" |
| `voltageLevel` | 电压等级 | "10kV" |
| `deviceType` | 设备类型 | "高压开关柜" |
| `workType` | 作业类型 | "检修" |
| `environment` | 作业环境 | "室内" |

---

#### [HazardIdentificationService](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/service/HazardIdentificationService.java)

核心危险点识别引擎。

| 函数 | 说明 |
|------|------|
| `identifyHazards(Task)` | ① 清除该任务旧危险点 → ② 遍历规则引擎 `RULES` 命中后保存危险点+措施 → ③ 若 ES 启用则检索相似案例 → ④ 合并 ES 结果 |
| `getHazardsWithMeasures(Long taskId)` | 查询任务的所有危险点及其控制措施，组装成结构化的 Map 列表 |

**规则引擎覆盖的危险类型**：
`触电`、`高处坠落`、`物体打击`、`有限空间`、`变压器油泄漏`、`火灾`、`误操作`、`雷击`、`滑倒`、`中暑`、`冻伤`、`燃气泄漏爆炸` 等。

每条规则包含：
- `keywordPatterns`：命中的关键词列表（如 `["高压", "10kV", "35kV", "带电"]` 命中"触电"）
- `hazardName` / `riskLevel`：危险点名称与风险等级
- `description`：危险描述模板
- `measures`：对应的控制措施列表（含优先级排序）

**RAG 增强**：当 `es.enabled=true` 时，调用 `ElasticsearchService.searchSimilarCases` 补充长尾场景的危险点。

---

#### [ConversationAgentService](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/service/ConversationAgentService.java)

对话式 Agent 编排服务，以 `ConcurrentHashMap<Long, Session>` 管理每个用户的内存会话。

| 函数 | 说明 |
|------|------|
| `processMessage(Long userId, String message)` | 主入口：根据会话阶段分发处理 |
| `getSessionState(Long userId)` | 返回当前会话快照 |
| `resetSession(Long userId)` | 清除用户的会话数据 |

**会话状态机**：

```
INITIAL ──→ COLLECTING ──→ REVIEW ──→ COMPLETED
  │             │              │
  └─ 收集任务    ├─ 追问字段    ├─ 确认/解释危险点
                 │              │
                 └─ 调用        └─ 生成交底卡
                    TaskService
                    .createTask
```

| 阶段 | 触发条件 | 行为 |
|------|----------|------|
| `INITIAL` | 用户首次输入任务描述 | 调用 `TaskParserService.parse` 提取字段；若有缺失字段则进入 `COLLECTING` 追问；若完整则直接进入识别 |
| `COLLECTING` | 存在缺失字段 | 从用户输入中尝试提取缺失字段；补全后调用 `executeIdentification` → `TaskService.createTask` → 进入 `REVIEW` |
| `REVIEW` | 危险点已识别完成 | 展示危险点列表及控制措施；用户可选择"生成交底卡"→ `generateBriefing` → `BRIEFING/COMPLETED`，或"解释危险点" |
| `COMPLETED` | 交底卡已生成 | 返回交底卡完整内容，提供"开始新任务"和"回仪表板"选项 |

---

#### [BriefingService](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/service/BriefingService.java)

| 函数 | 说明 |
|------|------|
| `generate(Task task, List<HazardPoint> hazards)` | 生成标准化安全技术交底卡文本 |

**生成策略**：
- **LLM 模式**（`deepseek.enabled=true` 且有危险点）：调用 DeepSeek 生成定制化交底条款
- **回退模式**：使用固定模板拼接危险点名称及控制措施

**典型交底卡内容包含**：
- 作业任务基本信息
- 危险点清单（含风险等级）
- 控制措施列表
- 安全注意事项

---

#### [LlmService](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/service/LlmService.java)

DeepSeek API 调用封装。

| 函数 | 说明 |
|------|------|
| `chat(String systemPrompt, String userMessage)` | 普通文本对话，返回 LLM 回复文本 |
| `chatAsJson(String systemPrompt, String userMessage)` | JSON 模式对话，强制 LLM 输出 JSON，失败返回 `null` |

**调用方式**：通过 `RestTemplate` 向 `https://api.deepseek.com/chat/completions` 发送 HTTP POST 请求。

**生效条件**：`deepseek.enabled=true`（默认开启）

> API Key 应通过 `application-local.yaml` 配置，不提交到仓库。如未配置或 LLM 不可用，系统自动降级到规则/模板方案。

---

#### [ElasticsearchService](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/service/ElasticsearchService.java)

| 函数 | 说明 |
|------|------|
| `searchSimilarCases(String voltageLevel, String deviceType, String workType, String environment)` | 按任务要素组合检索 `accident_cases` 索引中相似的事故案例 |

**索引名**：`accident_cases`

**生效条件**：`es.enabled=true`（默认开启）

**数据来源**：通过 [setup_es.py](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/infra/es-data/setup_es.py) 将 [accident_cases.json](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/infra/es-data/accident_cases.json) 中的事故案例种子数据批量导入。

---

### 4.4 数据实体层 (Entity)

#### [User](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/entity/User.java)

| 表 | `users` |
|----|---------|
| ID | `id` (Long, 自增) |
| 字段 | `username` (唯一), `email` (唯一), `password` (BCrypt 加密) |
| 审计 | `created_at`, `updated_at` (自动填充) |

---

#### [Task](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/entity/Task.java)

| 表 | `tasks` |
|----|---------|
| ID | `id` (Long, 自增) |
| 关联 | `user_id` → `users.id` (ManyToOne) |
| 字段 | `task_name`, `task_description`(TEXT), `voltage_level`, `device_type`, `work_type`, `environment` |
| 状态 | `status` (Enum: `PENDING` / `PARSED`) |
| 审计 | `created_at`, `updated_at` |

---

#### [HazardPoint](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/entity/HazardPoint.java)

| 表 | `hazard_points` |
|----|-----------------|
| ID | `id` (Long, 自增) |
| 关联 | `task_id` → `tasks.id` |
| 字段 | `hazard` (危险点名称), `risk_level` (风险等级), `description`(TEXT), `based_on` (识别依据: `RULE` / `ES_RAG`) |

---

#### [ControlMeasure](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/entity/ControlMeasure.java)

| 表 | `control_measures` |
|----|--------------------|
| ID | `id` (Long, 自增) |
| 关联 | `hazard_point_id` → `hazard_points.id` |
| 字段 | `measure` (措施内容), `priority` (优先级序号) |

---

### 4.5 数据传输对象 (DTO)

| DTO | 用途 | 关键字段 |
|-----|------|----------|
| `ApiResponse<T>` | 统一 REST 响应包装 | `code`, `message`, `data`；提供静态工厂 `success()`, `error()` |
| `LoginRequest` | 登录请求体 | `email` (校验), `password` |
| `RegisterRequest` | 注册请求体 | `username`(3-20), `email`, `password`(6-30) |
| `UserInfo` | 用户信息响应 | `id`, `username`, `email` |
| `TaskCreateRequest` | 创建任务请求体 | `taskDescription`(4-500 字符) |
| `TaskResponse` | 任务详情响应 | `id`, `taskName`, `voltageLevel`, `deviceType`, `workType`, `environment`, `status`, `hazardPoints` |
| `AgentMessageRequest` | Agent 消息请求体 | `message`(1-1000 字符) |
| `AgentMessageResponse` | Agent 回复响应 | `reply`, `sessionState`, `parsedFields`, `missingFields`, `suggestions`, `hazardPoints`, `briefingContent` |

**AgentMessageResponse 嵌套类**：
- `ParsedFields`：`voltageLevel`, `deviceType`, `workType`, `environment`
- `HazardItem`：`name`, `riskLevel`, `description`, `controlMeasures`(List)
- 静态工厂：`collecting()`, `reviewing()`, `reply()`, `completed()`

---

### 4.6 仓储层 (Repository)

| 仓储 | 继承 | 自定义方法 |
|------|------|-----------|
| `UserRepository` | `JpaRepository<User, Long>` | `existsByUsername()`, `existsByEmail()`, `findByEmail()` |
| `TaskRepository` | `JpaRepository<Task, Long>` | `findByUserIdOrderByCreatedAtDesc()` |
| `HazardPointRepository` | `JpaRepository<HazardPoint, Long>` | `findByTaskId()`, `deleteByTaskId()` |
| `ControlMeasureRepository` | `JpaRepository<ControlMeasure, Long>` | `findByHazardPointIdOrderByPriorityAsc()` |

---

### 4.7 配置层 (Config)

#### [SecurityConfig](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/config/SecurityConfig.java)

| Bean | 说明 |
|------|------|
| `PasswordEncoder` | BCrypt 密码编码器 |
| `SecurityFilterChain` | 无状态 Session、关闭 CSRF、全面放行所有 API、允许跨域 |

**安全现状**：当前所有 `/api/**` 均 `permitAll()`，实际身份识别依赖 `user-id` 请求头，未实现 JWT 或 Session 鉴权。这是一个待完善的简化方案。

---

#### [LlmConfig](file:///c:/Users/Nor/Preview/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/config/LlmConfig.java)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `deepseek.api-key` | String | `""` | API Key，通过 `application-local.yaml` 注入 |
| `deepseek.model` | String | `deepseek-chat` | 模型名称 |
| `deepseek.timeout` | Duration | `30s` | HTTP 调用超时 |

---

#### [SpaFallbackFilter](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/java/org/example/eledangercheck/config/SpaFallbackFilter.java)

`OncePerRequestFilter` 实现，用于支持 Vue Router 的 `createWebHistory()` 模式。

**路由策略**：
| 路径模式 | 处理方式 |
|----------|----------|
| `/api/*` | 放行 → `@RestController` |
| `/assets/*`, `/`, `*.xxx` | 放行 → 静态资源处理器 |
| 其他所有路径（如 `/dashboard`, `/agent`） | 转发到 `/index.html` → Vue Router 接管 |

---

## 5. 前端模块详解

### 5.1 工程配置

#### [vite.config.js](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/frontend/vite.config.js)

```js
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8081', changeOrigin: true } },
  },
  build: {
    outDir: '../ELeDangerCheck/src/main/resources/static',  // 构建产物输出到后端静态目录
    emptyOutDir: true,
  },
})
```

| 配置项 | 说明 |
|--------|------|
| 开发端口 | `5173` |
| API 代理 | `/api` → `localhost:8081` |
| 构建输出 | 直接写入后端 `static/` 目录，实现生产环境前后端合并部署 |

#### [package.json](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/frontend/package.json)

```json
{
  "dependencies": {
    "axios": "^1.18.1",
    "vue": "^3.5.39",
    "vue-router": "^4.6.4"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.7",
    "vite": "^8.1.1"
  }
}
```

---

### 5.2 路由与页面

#### [路由定义](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/frontend/src/router/index.js)

| 路径 | 页面组件 | 说明 |
|------|----------|------|
| `/` | — | 重定向到 `/login` |
| `/login` | `Login.vue` | 用户登录 |
| `/register` | `Register.vue` | 用户注册 |
| `/dashboard` | `Dashboard.vue` | 任务仪表板（需认证） |
| `/agent` | `AgentConversation.vue` | Agent 对话界面（需认证，懒加载） |
| `/task/:id` | `TaskDetail.vue` | 任务详情页（需认证，懒加载） |

**路由守卫**：`beforeEach` 检查 `sessionStorage` 中是否有 `user` 信息，无则跳转 `/login`。

#### 页面功能说明

| 页面 | 功能 |
|------|------|
| **Login.vue** | 邮箱+密码登录，调用 `login()` API，成功后存储用户信息到 `sessionStorage` |
| **Register.vue** | 用户名+邮箱+密码注册，调用 `register()` API |
| **Dashboard.vue** | 展示用户所有任务列表（卡片形式），提供"新建任务"输入框（表单模式）和"进入对话模式"入口 |
| **TaskDetail.vue** | 展示单个任务的解析字段、危险点列表（含控制措施）、交底卡内容 |
| **AgentConversation.vue** | 多轮聊天界面，支持消息发送、会话状态展示、危险点确认、交底卡一键生成 |

---

### 5.3 API 封装

#### [api/index.js](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/frontend/src/api/index.js)

```js
const api = axios.create({ baseURL: '/api', headers: { 'Content-Type': 'application/json' } })

api.interceptors.request.use((config) => {
  const user = JSON.parse(sessionStorage.getItem('user') || 'null')
  if (user?.id) config.headers['user-id'] = user.id   // 自动注入 user-id
  return config
})
```

| 函数 | 对应后端 API |
|------|-------------|
| `login(email, password)` | `POST /api/auth/login` |
| `register(username, email, password)` | `POST /api/auth/register` |
| `createTask(taskDescription)` | `POST /api/tasks` |
| `getTasks()` | `GET /api/tasks` |
| `getTask(id)` | `GET /api/tasks/{id}` |
| `sendAgentMessage(message)` | `POST /api/agent/message` |
| `getAgentSession()` | `GET /api/agent/session` |
| `resetAgentSession()` | `POST /api/agent/reset` |

---

## 6. 基础设施模块

### [docker-compose.yml](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/infra/docker-compose.yml)

```yaml
services:
  elasticsearch:
    image: elasticsearch:8.17.4
    container_name: ele-es
    ports: ["9200:9200", "9300:9300"]
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false    # 禁用安全认证（简化开发配置）
    volumes:
      - ele-es-data:/usr/share/elasticsearch/data

  kibana:
    image: kibana:8.17.4
    container_name: ele-kibana
    ports: ["5601:5601"]
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
```

| 容器 | 说明 |
|------|------|
| `ele-es` | Elasticsearch 8.17.4，单节点，端口 9200 |
| `ele-kibana` | Kibana 8.17.4，端口 5601，用于 ES 数据可视化 |

### ES 索引与数据初始化

- **索引名**：`accident_cases`
- **初始化脚本**：[setup_es.py](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/infra/es-data/setup_es.py)
- **种子数据**：[accident_cases.json](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/infra/es-data/accident_cases.json) (约 29 条事故案例记录)

**初始化方式**：
```bash
cd infra
python es-data/setup_es.py   # 先建索引 → bulk 导入 → 校验总数
```

---

## 7. 核心业务调用链

### 7.1 表单模式创建任务

```
用户在前端填写任务描述
    │
    ▼
POST /api/tasks  (user-id + taskDescription)
    │
    ▼
TaskController.createTask()
    │
    ▼
TaskService.createTask()
    ├── UserRepository.findById(userId)           ── 查找用户
    ├── TaskParserService.parse(taskDescription)   ── 解析任务字段
    │   ├── [LLM enabled]  llmParse()  → LlmService.chatAsJson()
    │   └── [fallback]     regexParse()            ── 正则+词典抽取
    ├── TaskRepository.save(task)                  ── 保存到 MySQL
    ├── HazardIdentificationService.identifyHazards()
    │   ├── HazardPointRepository.deleteByTaskId() ── 清除旧数据
    │   ├── 遍历 RULES 规则列表
    │   │   └── 命中 → saveHazardWithMeasures()
    │   └── [ES enabled] ElasticsearchService.searchSimilarCases()
    │       └── 合并 ES 检索结果到危险点列表
    └── getHazardsWithMeasures(taskId)              ── 组装完整返回
    │
    ▼
返回 TaskResponse (含危险点+控制措施)
```

### 7.2 对话 Agent 模式

```
用户通过聊天界面发送消息
    │
    ▼
POST /api/agent/message  (user-id + message)
    │
    ▼
ConversationAgentController.sendMessage()
    │
    ▼
ConversationAgentService.processMessage()
    │
    ├── [INITIAL阶段] 首次输入任务描述
    │   ├── TaskParserService.parse() 提取字段
    │   ├── 有缺失字段 → 返回 COLLECTING 状态 + 追问提示
    │   └── 无缺失字段 → 执行 executeIdentification()
    │
    ├── [COLLECTING阶段] 补充缺失字段
    │   ├── 从用户回复中提取缺失信息
    │   └── 补全后 → 执行 executeIdentification()
    │   │   └── executeIdentification()
    │   │       └── TaskService.createTask()   ← 同表单模式调用链
    │   │       └── 进入 REVIEW 阶段
    │
    ├── [REVIEW阶段] 展示危险点
    │   ├── 用户可选："生成交底卡" / "解释危险点"
    │   └── "生成交底卡" → generateBriefing()
    │       ├── TaskService.getTaskEntity() + HazardPointRepository
    │       └── BriefingService.generate()
    │           ├── [LLM enabled] 定制生成
    │           └── [fallback]    模板生成
    │
    └── [COMPLETED阶段] 交底卡完成
        └── 用户可选："开始新任务" / "回仪表板"
```

---

## 8. 依赖关系

### 8.1 后端依赖

| 依赖 | 用途 | Maven GroupId / ArtifactId |
|------|------|---------------------------|
| Spring Boot Web | REST API 服务 | `spring-boot-starter-web` |
| Spring Data JPA | 数据库 ORM | `spring-boot-starter-data-jpa` |
| Spring Security | 认证与安全框架 | `spring-boot-starter-security` |
| Validation | 请求参数校验 | `spring-boot-starter-validation` |
| MySQL Connector | MySQL 数据库驱动 | `mysql-connector-j` (runtime) |
| Jackson Databind | JSON 序列化/反序列化 | `jackson-databind` |
| Jackson Core | JSON 核心库 | `jackson-core` |

**外部服务依赖**：
| 服务 | 依赖方式 | 默认地址 | 是否必需 |
|------|----------|----------|----------|
| MySQL | JDBC | `localhost:3306` | **是** |
| Elasticsearch | HTTP (RestTemplate) | `localhost:9200` | 否（可关闭） |
| DeepSeek API | HTTP (RestTemplate) | `api.deepseek.com` | 否（可关闭） |

### 8.2 前端依赖

| 依赖 | 用途 |
|------|------|
| Vue 3.5 | UI 框架 |
| Vue Router 4 | 前端路由 (History 模式) |
| Axios 1.x | HTTP 客户端 |
| Vite 8.x | 构建工具 |
| @vitejs/plugin-vue | Vite Vue 插件 |

### 8.3 基础设施依赖

| 组件 | 版本 | 用途 |
|------|------|------|
| Docker Desktop | — | 容器运行环境 |
| Elasticsearch | 8.17.4 | RAG 案例检索 |
| Kibana | 8.17.4 | ES 可视化面板 |
| Python 3 | — | ES 数据初始化脚本 |

---

## 9. 项目运行方式

### 9.1 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | **17+** |
| Node.js | 18+ (推荐 20+) |
| MySQL | 8.0+ |
| Docker Desktop | 最新版（可选，仅 ES 需要） |
| Python 3 | 3.8+（可选，仅 ES 初始化需要） |

### 9.2 启动顺序

#### 第一步：启动 MySQL

确保本地 MySQL 运行在 `localhost:3306`，有 `root` 账号。数据库 `ele_danger_check` 会自动创建（`createDatabaseIfNotExist=true`），表结构由 JPA `ddl-auto: update` 自动维护。

#### 第二步：启动 Elasticsearch（可选）

```bash
cd infra
docker compose up -d
# 验证：curl http://localhost:9200
```

#### 第三步：初始化 ES 数据（可选）

```bash
cd infra
python es-data/setup_es.py
# 脚本会在 ES 中创建 accident_cases 索引并导入种子数据
```

#### 第四步：配置 DeepSeek API Key（可选）

在 `ELeDangerCheck/src/main/resources/` 下创建 `application-local.yaml`：

```yaml
deepseek:
  api-key: "sk-your-api-key-here"
```

如果不使用 LLM，可将 `application.yaml` 中的 `deepseek.enabled` 改为 `false`。

#### 第五步：启动后端

```bash
cd ELeDangerCheck
mvnw.cmd spring-boot:run
# 服务启动在 http://localhost:8081
```

#### 第六步：启动前端（开发模式）

```bash
cd frontend
npm install
npm run dev
# 开发服务器启动在 http://localhost:5173
# 浏览器访问 http://localhost:5173
```

#### 第七步（可选）：构建前端生产包

```bash
cd frontend
npm run build
# 构建产物自动输出到 ../ELeDangerCheck/src/main/resources/static/
# 重启后端后，访问 http://localhost:8081 即可使用完整应用
```

### 9.3 配置速查

**[application.yaml](file:///c:/Users/Nor/Desktop/Code/Ele_Agent/ELeDangerCheck/src/main/resources/application.yaml)** 关键配置项：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8081` | 后端服务端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/ele_danger_check?...` | MySQL 连接串 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 自动建表/更新 |
| `deepseek.enabled` | `true` | DeepSeek LLM 启用开关 |
| `deepseek.model` | `deepseek-chat` | 模型名称 |
| `es.enabled` | `true` | ES RAG 启用开关 |

### 9.4 典型场景

| 场景 | 最小依赖 | 命令 |
|------|----------|------|
| 基础功能（表单任务+规则识别） | MySQL + 后端 | `mvnw.cmd spring-boot:run` |
| 完整体验（含对话 Agent） | MySQL + 后端 + 前端开发服务器 | 后端 + `npm run dev` |
| RAG 增强（ES 案例检索） | 上述 + Docker + ES | `docker compose up -d` + `setup_es.py` |
| LLM 增强（智能解析+交底） | 上述 + DeepSeek API Key | 配置 `application-local.yaml` |
| 生产部署 | MySQL + 后端（含前端静态资源） | `npm run build` + 重启后端 |

---

## 10. 数据库表结构

### `users` 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户 ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 邮箱 |
| password | VARCHAR(255) | NOT NULL | 密码 (BCrypt) |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### `tasks` 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 任务 ID |
| user_id | BIGINT | NOT NULL, FK → users.id | 所属用户 |
| task_name | VARCHAR(200) | — | 任务名称（解析后） |
| task_description | TEXT | NOT NULL | 任务描述（原始输入） |
| voltage_level | VARCHAR(50) | — | 电压等级 |
| device_type | VARCHAR(100) | — | 设备类型 |
| work_type | VARCHAR(100) | — | 作业类型 |
| environment | VARCHAR(200) | — | 作业环境 |
| status | VARCHAR(20) | — | 状态: PENDING / PARSED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### `hazard_points` 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 危险点 ID |
| task_id | BIGINT | NOT NULL, FK → tasks.id | 所属任务 |
| hazard | VARCHAR(100) | NOT NULL | 危险点名称 |
| risk_level | VARCHAR(20) | — | 风险等级 |
| description | TEXT | — | 描述 |
| based_on | VARCHAR(255) | — | 识别依据 (RULE / ES_RAG) |

### `control_measures` 表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 措施 ID |
| hazard_point_id | BIGINT | NOT NULL, FK → hazard_points.id | 所属危险点 |
| measure | VARCHAR(255) | NOT NULL | 控制措施内容 |
| priority | INT | NOT NULL | 优先级序号 |

---

> 本文档由 Code Wiki Generator 于 2026-07-20 自动生成，基于项目源代码分析。
