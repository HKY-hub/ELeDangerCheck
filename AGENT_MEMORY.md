# Agent Memory — 电力作业风险辨识与安全交底 Agent

> **角色：** AI 辅助开发助手  
> **项目路径：** `D:\AGENT\ELeDangerCheck`  
> **主项目：** `ELeDangerCheck/` (Spring Boot 3.2.5 + Java 17)  
> **数据库：** MySQL 8.0, 库名 `eledangercheck`（账号见 `secrets-local.yml`，不入库）  
> **服务器：** http://localhost:8081  
> **记忆更新：** 2026-07-15 10:00

---

## 项目定位

选题十七：电力作业风险辨识与安全交底 Agent。针对电力作业前必须完成的风险辨识与安全交底环节，自动识别危险点、生成控制措施、编制安全交底卡，支持语音播报和电子签名确认。

团队：第七组，PM 马哲宇，共 3 人。总体进度约 85~90%。

---

## 修改说明日志

### 2026-07-15 10:00 — 修复前端问题与完善电子签名功能

**修改内容：**
1. **TtsController.java** — 新增 `/api/tts/speak` 接口，返回真实 WAV 音频数据（模拟生成正弦波音频）
2. **disclosure-detail.js** — 修复语音播放功能，添加防御式 JSON 解析，避免空响应导致的解析错误
3. **dashboard.js** — 添加 `apiFetch()` 函数，自动携带 JWT Token，修复所有模块 API 请求缺少认证头问题
4. **disclosure-detail.js** — 添加 `apiFetch()` 函数，确保所有 API 请求携带认证头
5. **style.css** — 修改下拉菜单触发方式，从 hover 改为 click 触发，解决浮窗随鼠标移出立即消失的问题
6. **dashboard.js** — 添加 `initUserDropdown()` 函数，实现点击显示/隐藏下拉菜单，点击外部关闭
7. **style.css** — 添加模块切换动画（opacity + translateY），优化页面切换流畅度
8. **dashboard.js** — 修改 `showModule()` 函数，使用 CSS class 切换替代直接修改 display 属性
9. **disclosure-detail.html** — 添加电子签名功能，包含交底人签名、被交底人签名、时间选择器
10. **disclosure-detail.html** — 添加电子签名模态框，支持鼠标手写签名
11. **disclosure-detail.js** — 实现电子签名完整逻辑（签名绘制、清除、确认、保存）
12. **style.css** — 添加电子签名相关样式（签名框、模态框、时间输入框）
13. **dashboard.html** — 首页模块默认添加 active class，确保首次加载显示正常

**错误原因：**
- TTS 接口返回 JSON 而非音频数据，前端尝试解析为空 JSON 导致 SyntaxError
- 前端所有 API 请求缺少 Authorization 头，后端拦截返回 401
- 用户下拉菜单使用 hover 触发，鼠标从头像移出时浮窗立即消失
- 模块切换使用 display:none/block，无过渡动画，切换不流畅
- 电子签名功能缺失，只有占位符

**解决方法：**
- 修改 TTS 接口返回真实 WAV 音频数据
- 添加 apiFetch() 函数自动携带 JWT Token
- 将下拉菜单改为 click 触发，添加点击外部关闭逻辑
- 添加 CSS 过渡动画，使用 class 切换
- 实现完整电子签名功能（Canvas 绘制、保存、显示）

**验证结果：**
- ✅ TTS 语音播放正常
- ✅ 所有模块 API 请求正常（任务、危险点、交底、案例、字典）
- ✅ 用户下拉菜单点击显示/隐藏正常
- ✅ 页面切换流畅有过渡动画
- ✅ 电子签名功能完整可用

---

### 2026-07-13 20:26 — 升级至 Spring Boot 3.2.5 + Java 17

**修改内容：**
1. **pom.xml** — 将 Spring Boot 版本从 2.7.18 升级到 3.2.5
2. **pom.xml** — 将 mybatis-plus-boot-starter 改为 mybatis-plus-spring-boot3-starter
3. **pom.xml** — 添加 spring-boot-starter-jdbc 依赖
4. **pom.xml** — 移除重复的 spring-boot-starter-web 依赖
5. **LoginRequest.java** — 将 javax.validation 改为 jakarta.validation
6. **RegisterRequest.java** — 将 javax.validation 改为 jakarta.validation
7. **AuthController.java** — 将 javax.validation.Valid 改为 jakarta.validation.Valid
8. **DatabaseInitializer.java** — 将 javax.annotation 改为 jakarta.annotation
9. **SecurityConfig.java** — 重写为 Spring Boot 3.x API（SecurityFilterChain + authorizeHttpRequests + requestMatchers）
10. **SecurityConfig.java** — 将 allowedOrigins 改为 allowedOriginPatterns（CORS）
11. **SecurityConfig.java** — 添加 @Lazy 注解解决循环依赖
12. **AgentService.java** — 将 determineRiskLevel 和 getLevelPriority 方法改回 Java 14+ switch 表达式

**升级原因：**
- 之前因 Java 8 限制，项目降级到 Spring Boot 2.7.x
- 现在已具备 JDK 17 环境，恢复到原始设计要求
- Spring Boot 3.x 提供更好的性能和安全性

**验证结果：**
- ✅ 服务启动成功（端口 8081）
- ✅ AI 分析接口正常返回数据
- ✅ "低压配电柜更换"任务解析正确
- ✅ 危险点识别：火灾(高)、触电(高)、机械伤害(中)

---

### 2026-07-13 18:52 — 修复对话显示问题与语音播报

**修改内容：**
1. **AgentService.java** — 增强任务解析能力，添加低压电压模式（低压、380V、220V）和配电柜设备类型识别
2. **chat.html** — 添加 `renderMarkdown()` 函数，支持渲染 markdown 表格、标题、加粗、列表等格式
3. **chat.html** — 重写 `displayCompleteFlow()` 为 `displayMergedResult()`，合并 AI 分析和作业任务解析，避免重复显示
4. **chat.html** — 修复 `playTts()` 函数，添加 Authorization 请求头和错误处理
5. **chat.html** — 改进作业任务解析显示为网格布局，危险点识别和控制措施显示为列表布局

**错误原因：**
- 任务解析规则不支持"低压"和"配电柜"的识别，导致解析结果为空
- AI 返回的 markdown 表格没有正确渲染，显示错乱
- AI 分析和作业任务解析分开显示，造成重复信息
- 语音播报缺少认证头导致请求被拒绝

**解决方法：**
- 添加低压电压模式和配电柜设备类型的正则表达式匹配
- 实现 markdown 渲染函数，支持表格、标题、列表等格式转换
- 合并 AI 分析和作业任务解析，统一显示为一个完整的消息卡片
- 在语音播报请求中添加 Authorization 头

**验证结果：**
- ✅ 服务启动成功（端口 8081）
- ✅ API 健康检查正常
- ✅ "低压配电柜更换"任务解析正常（电压等级：低压，设备类型：配电柜，作业类型：更换）
- ✅ markdown 表格正确渲染
- ✅ AI 分析和作业任务解析合并显示

---

### 2026-07-13 18:36 — 项目启动

**修改内容：**
1. 停止占用端口8081的进程（PID 2300）
2. 重新启动 Spring Boot 项目

**验证结果：**
- ✅ 服务启动成功（端口 8081，进程 PID 17760）
- ✅ API 健康检查正常：`{"service":"ELeDangerCheck","status":"up"}`
- ✅ 数据库初始化成功

---

### 2026-07-13 18:05 — 修复对话模型错误与导航切换问题

**修改内容：**
1. **pom.xml** — 添加 fastjson 1.2.83 依赖（缺少 JSON 解析库导致 `JSON.parseObject` 无法解析）
2. **AgentService.java** — 将两个 Java 14+ switch 表达式（箭头 case）改为 Java 8 兼容的 if-else 语句
3. **chat.html** — 修改导航链接，直接使用 `href="dashboard.html?module=xxx"` 跳转，解决切换到功能模块后无法切回 Agent 的问题
4. **dashboard.js** — 修改 `initNav()` 函数，跳过 agent 模块的默认行为阻止，允许跳转到 chat.html

**错误原因：**
- `JSON.parseObject` 无法解析：缺少 fastjson 依赖
- `Switch Expressions are supported from Java 14 onwards only`：使用了 Java 14+ 的 switch 表达式语法
- 导航无法切换：`initNav()` 阻止了所有导航链接的默认跳转行为，包括指向 chat.html 的链接

**解决方法：**
- 添加 fastjson 依赖到 pom.xml
- 将 switch 表达式改为 if-else 语句兼容 Java 8
- 修改导航链接使用直接 href 跳转而非 data-module 属性
- 在 dashboard.js 中为 agent 模块单独放行默认跳转行为

**验证结果：**
- ✅ Agent API `/api/agent/ai-chat` 返回完整 AI 分析报告
- ✅ 导航栏可自由切换各模块
- ✅ 从 dashboard 点击 Agent 可跳转到 chat.html

---

### 2026-07-13 17:10 — 修复 JSON parse error 错误

**修改内容：**
1. **LoginRequest.java** — 将 `jakarta.validation.constraints.*` 改为 `javax.validation.constraints.*`
2. **RegisterRequest.java** — 将 `jakarta.validation.constraints.*` 改为 `javax.validation.constraints.*`
3. **AuthController.java** — 将 `jakarta.validation.Valid` 改为 `javax.validation.Valid`
4. **DatabaseInitializer.java** — 将 `jakarta.annotation.PostConstruct` 改为 `javax.annotation.PostConstruct`

**错误原因：**
- Spring Boot 2.7.x 使用 `javax.*` 命名空间，而代码中仍使用 `jakarta.*`（Spring Boot 3.x 命名空间）

**解决方法：**
- 将所有 `import jakarta.*` 改为 `import javax.*`

**验证结果：**
- ✅ 注册接口正常：返回 `{"success": true, "message": "注册成功"}`
- ✅ 登录接口正常：返回 `{"success": false, "message": "用户名或密码错误"}`（业务逻辑正常）

---

### 2026-07-13 16:45 — Spring Boot 降级至 2.7.18

**修改内容：**
1. **pom.xml** — 将 Spring Boot 版本从 3.2.5 降级到 2.7.18
2. **pom.xml** — 将 Java 版本从 17 改为 8
3. **pom.xml** — 将 mybatis-plus-spring-boot3-starter 改为 mybatis-plus-boot-starter
4. **pom.xml** — 移除 LangChain4j 和 Elasticsearch 依赖（不兼容 Java 8）
5. **SecurityConfig.java** — 重写为 Spring Boot 2.7.x API（WebSecurityConfigurerAdapter）
6. **AiAgentService.java** — 重写使用 RestTemplate 直接调用 DeepSeek API（替代 LangChain4j）

**错误原因：**
- 当前环境只有 Java 8，Spring Boot 3.2.x 需要 Java 17
- LangChain4j 为 Java 17 编译，无法在 Java 8 运行
- Elasticsearch 8.x 不兼容 Java 8

**解决方法：**
- 降级 Spring Boot 到 2.7.18
- 移除不兼容依赖
- 使用 RestTemplate 直接调用 DeepSeek API

**验证结果：**
- ✅ 服务启动成功（端口 8081）
- ✅ API 健康检查正常：`{"service":"ELeDangerCheck","status":"up"}`
- ✅ 添加 @Lazy 注解解决循环依赖问题

---

### 2026-07-13 15:55 — 统一第三方密钥配置与模拟模式

**修改内容：**
1. **application-mysql.yml** — 集中管理所有第三方密钥（DeepSeek、FastChat、阿里云TTS、e签宝、ES）
2. **application-mysql.yml** — 添加模拟模式开关
3. **SimulationConfig.java** — 读取模拟模式配置
4. **DeepSeekConfig.java** — 读取 DeepSeek API 配置
5. **FastChatConfig.java** — 读取 FastChat API 配置
6. **AliyunTtsConfig.java** — 读取阿里云 TTS 配置
7. **EsignConfig.java** — 读取 e签宝配置
8. **doc/schema.sql** — 生成完整建表 SQL
9. **README.md** — 生成部署文档

**错误原因：**
- 第三方密钥硬编码在代码中
- 无模拟模式，缺少 API 密钥时启动报错

**解决方法：**
- 所有密钥统一配置到 application-mysql.yml
- 添加模拟模式开关，无 API 密钥时使用本地逻辑

**验证结果：**
- ✅ 配置文件集中管理所有密钥
- ✅ 模拟模式正常工作

---

### 2026-07-13 11:30 — 修复数据库表结构不一致

**修改内容：**
1. **DatabaseInitializer.java** — 重建完整表结构，添加缺失字段（task_type、similarity、dict_desc、pdf_path、word_path）
2. **DatabaseInitializer.java** — 预置字典数据（5个危险点、5个控制措施）
3. **GlobalExceptionHandler.java** — 改进全局异常处理，输出真实错误信息

**错误原因：**
- 数据库表结构与实体类不匹配，缺少多个字段
- MyBatis 查询失败导致 500 错误

**解决方法：**
- 重建表结构匹配实体类
- 预置初始化数据

**验证结果：**
- ✅ 所有 API 返回有效数组
- ✅ 用户注册成功
- ✅ 任务 API 返回空数组正常

---

### 2026-07-13 09:30 — 开发 dashboard 页面与修复 Java 版本问题

**修改内容：**
1. **dashboard.html** — 创建功能模块管理页面，包含导航栏和用户头像
2. **dashboard.js** — 实现页面切换和 CRUD 操作
3. **style.css** — 添加导航栏、统计卡片、表格样式
4. **login.js** — 修改登录成功后跳转到 dashboard.html
5. **SecurityConfig.java** — 使用 Spring Boot 2.7.x API（authorizeRequests() 和 antMatchers()）

**错误原因：**
- 前端缺少功能模块页面
- Java 8 不兼容 Spring Boot 3.2.x API

**解决方法：**
- 创建 dashboard 页面
- 修改 SecurityConfig 使用兼容 Java 8 的 API

**验证结果：**
- ✅ 项目启动成功
- ✅ API 测试通过

---

### 2026-07-13 09:00 — 初始后端模块开发

**修改内容：**
1. 创建 11 个数据库表（user、task、hazard、safety_measure、disclosure、disclosure_sign、accident_case、hazard_dict、measure_dict、task_parse、evaluation）
2. 创建 10 个实体类
3. 创建 9 个 Mapper
4. 创建 5 个 Service
5. 创建 5 个 Controller
6. 添加 API 端点（/api/tasks、/api/hazards、/api/disclosures、/api/accident-cases、/api/dict）

**验证结果：**
- ✅ 服务运行在端口 8081
- ✅ MySQL 连接正常
- ✅ 所有模块可用

---

## 已完成清单

### 后端接口

**认证模块**
- POST `/api/auth/register` — 注册（username, email, password）
- POST `/api/auth/login` — 登录（username, password）
- GET `/api/auth/health` — 健康检查

**任务管理**
- GET `/api/tasks` — 获取所有任务
- POST `/api/tasks` — 创建任务
- GET `/api/tasks/{id}` — 获取任务详情
- PUT `/api/tasks/{id}` — 更新任务
- DELETE `/api/tasks/{id}` — 删除任务

**危险源管理**
- GET `/api/hazards` — 获取所有危险点
- POST `/api/hazards` — 创建危险点
- GET `/api/hazards/{id}` — 获取危险点详情
- PUT `/api/hazards/{id}` — 更新危险点
- DELETE `/api/hazards/{id}` — 删除危险点

**安全交底**
- GET `/api/disclosures` — 获取所有交底记录
- POST `/api/disclosures` — 创建交底记录
- GET `/api/disclosures/{id}` — 获取交底详情
- PUT `/api/disclosures/{id}` — 更新交底记录
- DELETE `/api/disclosures/{id}` — 删除交底记录

**事故案例**
- GET `/api/accident-cases` — 获取所有事故案例
- POST `/api/accident-cases` — 创建事故案例
- GET `/api/accident-cases/{id}` — 获取案例详情
- PUT `/api/accident-cases/{id}` — 更新案例
- DELETE `/api/accident-cases/{id}` — 删除案例

**字典管理**
- GET `/api/dict/hazard` — 获取危险点字典
- POST `/api/dict/hazard` — 添加危险点字典
- GET `/api/dict/measure` — 获取控制措施字典
- POST `/api/dict/measure` — 添加控制措施字典

**Agent 智能对话**
- POST `/api/agent/complete-flow` — 完整流程（任务解析→危险点识别→措施生成→交底卡）
- POST `/api/agent/ai-chat` — AI 对话接口
- POST `/api/agent/task/{taskId}/hazards` — 识别危险点
- POST `/api/agent/hazard/{hazardId}/measures` — 生成控制措施
- POST `/api/agent/task/{taskId}/disclosure` — 生成交底卡
- GET `/api/agent/task/{taskId}/disclosure` — 获取交底卡

**语音播报**
- POST `/api/tts/play` — 语音播报
- POST `/api/tts/generate` — 生成语音文件

**电子签章**
- POST `/api/esign/sign` — 签署电子签名
- POST `/api/esign/verify` — 验证签名

**交底评估**
- POST `/api/evaluation/generate` — 生成评估报告
- GET `/api/evaluation/{id}` — 获取评估报告

### 实体类

- `User` — 用户实体
- `Task` — 作业任务实体
- `TaskParse` — 任务解析结果实体
- `Hazard` — 危险点实体
- `SafetyMeasure` — 安全措施实体
- `Disclosure` — 安全交底实体
- `DisclosureSign` — 交底签名实体
- `AccidentCase` — 事故案例实体
- `HazardDict` — 危险点字典实体
- `MeasureDict` — 控制措施字典实体
- `Evaluation` — 评估结果实体

### 配置类

- `SecurityConfig` — Spring Security 配置（BCrypt、CORS、无状态）
- `DatabaseInitializer` — 数据库初始化（自动建表）
- `SimulationConfig` — 模拟模式配置
- `DeepSeekConfig` — DeepSeek API 配置
- `FastChatConfig` — FastChat API 配置
- `AliyunTtsConfig` — 阿里云 TTS 配置
- `EsignConfig` — e签宝配置
- `ElasticsearchConfig` — Elasticsearch 配置（可选）
- `DroolsConfig` — Drools 规则引擎配置（占位）
- `GlobalExceptionHandler` — 全局异常处理
- `WebConfig` — Web 配置

### 前端页面

- `index.html` — 登录页
- `register.html` — 注册页
- `dashboard.html` — 功能模块管理页
- `chat.html` — Agent 智能对话页
- `profile.html` — 个人信息页面
- `disclosure-detail.html` — 交底记录详情页面（含电子签名）

### 安全配置

- BCrypt 密码加密
- JWT 无状态认证
- CORS 跨域放行
- CSRF 关闭
- 静态资源 `/`, `/css/**`, `/js/**`, `/*.html` 放行

---

## 前端页面

### 页面文件

- `index.html` — 登录页（username + password 表单）
- `register.html` — 注册页（username + email + password + confirmPassword）
- `dashboard.html` — 功能模块管理页（任务管理、危险源管理、安全交底、事故案例、字典管理）
- `chat.html` — Agent 智能对话页（自然语言输入、AI分析、语音播报、电子签名）

### 跳转逻辑

- 登录成功 → 跳转 `chat.html`（Agent 对话首页）
- 注册成功 → 跳转 `index.html`（登录页）
- dashboard 导航栏点击 Agent → 跳转 `chat.html`
- chat 导航栏点击其他模块 → 跳转 `dashboard.html?module=xxx`

### 文件存放规则

前端文件存放于 `src/main/resources/static/` 目录下，由 Spring Boot 托管。修改时直接编辑该目录下的文件即可。

### 未完成页面

- 个人信息页面（待开发）

---

## 数据库

### 连接信息

- MySQL 连接：`jdbc:mysql://localhost:3306/eledangercheck?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8`
- 用户名：`root`
- 密码：见 `secrets-local.yml`（本地，不入库）
- 字符集：`utf8mb4`

### 自动建表策略

通过 `DatabaseInitializer` 组件在应用启动时自动创建表结构（DROP + CREATE），预置危险点字典和控制措施字典数据。

### 已存在数据表

#### user 表
- id (BIGINT, PK, AUTO_INCREMENT)
- username (VARCHAR(20), UNIQUE, NOT NULL)
- password (VARCHAR(255), NOT NULL)
- email (VARCHAR(100))
- real_name (VARCHAR(50))
- status (INT, DEFAULT 1)
- create_time (DATETIME)
- update_time (DATETIME)

#### task 表
- id (BIGINT, PK, AUTO_INCREMENT)
- task_name (VARCHAR(200), NOT NULL)
- task_type (VARCHAR(50))
- task_desc (TEXT)
- voltage_level (VARCHAR(20))
- equipment_type (VARCHAR(100))
- work_type (VARCHAR(50))
- env_conditions (VARCHAR(200))
- principal_id (BIGINT)
- status (VARCHAR(20), DEFAULT 'pending')
- create_time (DATETIME)
- update_time (DATETIME)

#### task_parse 表
- id (BIGINT, PK, AUTO_INCREMENT)
- task_id (BIGINT, NOT NULL)
- original_text (TEXT)
- voltage_level (VARCHAR(20))
- equipment_type (VARCHAR(100))
- work_type (VARCHAR(50))
- env_conditions (VARCHAR(200))
- location (VARCHAR(200))
- parse_result (TEXT)
- create_time (DATETIME)

#### hazard 表
- id (BIGINT, PK, AUTO_INCREMENT)
- task_id (BIGINT, NOT NULL)
- hazard_name (VARCHAR(200), NOT NULL)
- category (VARCHAR(50))
- hazard_level (VARCHAR(20), DEFAULT 'medium')
- description (TEXT)
- source_case (VARCHAR(200))
- location (VARCHAR(200))
- similarity (DOUBLE)
- create_time (DATETIME)

#### safety_measure 表
- id (BIGINT, PK, AUTO_INCREMENT)
- hazard_id (BIGINT, NOT NULL)
- measure_code (VARCHAR(50))
- measure_name (VARCHAR(200), NOT NULL)
- measure_desc (TEXT)
- priority (INT, DEFAULT 1)
- status (VARCHAR(20), DEFAULT 'pending')
- create_time (DATETIME)

#### disclosure 表
- id (BIGINT, PK, AUTO_INCREMENT)
- task_id (BIGINT, NOT NULL)
- title (VARCHAR(200), NOT NULL)
- disclosure_no (VARCHAR(50))
- disclosure_type (VARCHAR(50))
- content (TEXT)
- pdf_path (VARCHAR(500))
- word_path (VARCHAR(500))
- emergency_route (VARCHAR(500))
- emergency_contact (VARCHAR(100))
- disclosure_status (VARCHAR(20), DEFAULT 'draft')
- sign_status (VARCHAR(20), DEFAULT 'unsigned')
- create_time (DATETIME)
- update_time (DATETIME)

#### disclosure_sign 表
- id (BIGINT, PK, AUTO_INCREMENT)
- disclosure_id (BIGINT, NOT NULL)
- user_id (BIGINT, NOT NULL)
- sign_image (TEXT)
- sign_time (DATETIME)

#### accident_case 表
- id (BIGINT, PK, AUTO_INCREMENT)
- case_name (VARCHAR(200), NOT NULL)
- accident_type (VARCHAR(50))
- voltage_level (VARCHAR(20))
- work_type (VARCHAR(50))
- equipment_type (VARCHAR(100))
- case_desc (TEXT)
- cause_analysis (TEXT)
- lessons_learned (TEXT)
- hazard_points (TEXT)
- occur_time (DATETIME)
- severity (VARCHAR(20), DEFAULT 'medium')
- create_time (DATETIME)

#### hazard_dict 表
- id (BIGINT, PK, AUTO_INCREMENT)
- dict_code (VARCHAR(50), UNIQUE, NOT NULL)
- dict_name (VARCHAR(100), NOT NULL)
- dict_desc (TEXT)
- level (VARCHAR(20), DEFAULT 'medium')
- priority (INT, DEFAULT 1)
- create_time (DATETIME)

#### measure_dict 表
- id (BIGINT, PK, AUTO_INCREMENT)
- dict_code (VARCHAR(50), UNIQUE, NOT NULL)
- dict_name (VARCHAR(100), NOT NULL)
- dict_desc (TEXT)
- hazard_code (VARCHAR(50))
- priority (INT, DEFAULT 1)
- create_time (DATETIME)

#### evaluation 表
- id (BIGINT, PK, AUTO_INCREMENT)
- disclosure_id (BIGINT, NOT NULL)
- score (INT)
- violation_count (INT, DEFAULT 0)
- accident_count (INT, DEFAULT 0)
- evaluation_desc (TEXT)
- evaluation_time (DATETIME)

---

## 后端架构

```
controller/
├── AuthController          # 认证接口（注册、登录、健康检查）
├── TaskController          # 任务管理接口
├── HazardController        # 危险点管理接口
├── SafetyMeasureController # 安全措施接口
├── DisclosureController    # 安全交底接口
├── AccidentCaseController  # 事故案例接口
├── DictController          # 字典管理接口
├── AgentController         # Agent 智能对话接口
├── TtsController           # 语音播报接口
├── EsignController         # 电子签章接口
└── EvaluationController    # 评估接口

service/
├── UserService             # 用户服务
├── TaskService             # 任务服务
├── HazardService           # 危险点服务
├── SafetyMeasureService    # 安全措施服务
├── DisclosureService       # 交底服务
├── AccidentCaseService     # 事故案例服务
├── DictService             # 字典服务
├── AgentService            # Agent 业务服务
├── AiAgentService          # AI Agent 服务（调用 DeepSeek API）
├── RuleEngineService       # 规则引擎服务（纯Java实现）
├── TtsService              # 语音播报服务
├── EsignService            # 电子签章服务
└── EvaluationService       # 评估服务

mapper/
├── UserMapper
├── TaskMapper
├── TaskParseMapper
├── HazardMapper
├── SafetyMeasureMapper
├── DisclosureMapper
├── DisclosureSignMapper
├── AccidentCaseMapper
├── HazardDictMapper
├── MeasureDictMapper
└── EvaluationMapper

entity/
├── User
├── Task
├── TaskParse
├── Hazard
├── SafetyMeasure
├── Disclosure
├── DisclosureSign
├── AccidentCase
├── HazardDict
├── MeasureDict
└── Evaluation

dto/
├── LoginRequest
├── RegisterRequest
└── LoginResponse

config/
├── SecurityConfig
├── DatabaseInitializer
├── SimulationConfig
├── DeepSeekConfig
├── FastChatConfig
├── AliyunTtsConfig
├── EsignConfig
├── ElasticsearchConfig
├── DroolsConfig
├── GlobalExceptionHandler
└── WebConfig

security/
└── JwtTokenProvider

util/
└── JwtUtil

exception/
└── BusinessException
```

---

## 关键技术决策

1. **密码加密：** BCrypt (Spring Security)
2. **认证策略：** JWT 无状态认证 (`STATELESS`)，API 前缀 `/api/**` 放行，静态资源放行
3. **数据库：** 开发阶段用 `DatabaseInitializer` 自动建表，上线前需调整为手动建表
4. **AI 服务：** 使用 RestTemplate 直接调用 DeepSeek API
5. **规则引擎：** 使用纯 Java 实现
6. **前端：** 原生 HTML/CSS/JavaScript，支持移动端 H5
7. **启动方式：** `mvn spring-boot:run`（使用 Maven Wrapper）
8. **第三方服务：** 支持模拟模式，无 API 密钥时使用本地逻辑

---

## 下一步工作（按优先级）

### 1️⃣ 核心 AI 业务优化 ✅ 已完成

- ✅ 优化 AiAgentService 中的 DeepSeek API 调用，支持更复杂的对话上下文
- ✅ 实现 RAG 检索逻辑（基于事故案例库）
- ✅ 完善规则引擎规则库，覆盖更多电力作业场景

### 2️⃣ 文档生成功能 ✅ 已完成

- ✅ 实现 Apache POI 生成 Word 交底卡
- ✅ 实现 iText 生成 PDF 交底卡

### 3️⃣ 语音播报功能

- ✅ 模拟模式正常工作（生成模拟音频）
- [ ] 接入阿里云 TTS 真实 API

### 4️⃣ 电子签章功能 ✅ 已完成基础功能

- ✅ 实现前端电子签名（Canvas 手写）
- ✅ 签名保存和显示
- [ ] 接入 e签宝真实 API（可选）

### 5️⃣ 前端完善 ✅ 已完成

- ✅ 个人信息页面（profile.html）
- ✅ 交底记录详情页面（disclosure-detail.html，含电子签名）
- ✅ 响应式布局优化
- ✅ 页面切换动画优化
- ✅ 下拉菜单交互优化

### 6️⃣ 可选功能

- [ ] 接入 Elasticsearch 实现事故案例 RAG 检索
- [ ] 接入 FastChat NLU 服务

---

## 已知坑点 / 备忘

- **前端静态资源：** 所有静态资源存放于 `src/main/resources/static/`，Spring Security 需显式放行
- **数据库字符集：** 使用 `utf8mb4` 支持中文和 emoji
- **跨域配置：** CORS 已配置允许所有来源，生产环境需限制
- **模拟模式：** 部分第三方服务（TTS、e签宝）当前为模拟模式，需填入真实 API 密钥后启用
- **Jakarta 命名空间：** Spring Boot 3.x 使用 `jakarta.*` 命名空间，与 Spring Boot 2.x 的 `javax.*` 不兼容
- **SecurityFilterChain：** Spring Boot 3.x 使用 `SecurityFilterChain` 替代 `WebSecurityConfigurerAdapter`，使用 `authorizeHttpRequests` 和 `requestMatchers` 替代旧 API

---

## 联络信息

- 用户/项目发起人：huang-kaiyuan666
- 通信语言：中文
- 用户偏好：代码完成后暂停推送到 Gitee，仅在明确要求时执行推送操作
- Gitee 仓库：https://gitee.com/three-gorges-university_11/maple.bb
- 默认分支：develop

## 已完成的升级与优化（2026-07-14）

### 任务1：AI Agent 增强
1. **对话上下文支持**：优化 AiAgentService，使用 HashMap 存储对话历史，支持多轮对话
2. **RAG 检索逻辑**：基于事故案例库实现相关案例检索，增强 AI 分析的准确性
3. **规则引擎完善**：增强任务解析能力，支持低压（380V/220V/0.4kV）和配电柜识别
4. **添加位置参数**：identifyHazardsByRules 方法增加 location 参数，支持密闭空间风险识别

### 任务2：文档生成功能
1. **Word 交底卡生成**：使用 Apache POI 生成电力作业安全交底卡的 Word 文档
2. **PDF 交底卡生成**：使用 iText 生成电力作业安全交底卡的 PDF 文档
3. **文档 Controller**：创建 DocumentController，提供 Word/PDF 下载和保存接口

### 环境升级
1. **JDK 升级**：从 Java 8 升级到 Java 17
2. **Spring Boot 升级**：从 2.7.18 升级到 3.2.5
3. **命名空间迁移**：javax → jakarta
4. **Security 配置**：WebSecurityConfigurerAdapter → SecurityFilterChain

### 编译修复
1. AiAgentService：修复 location 参数缺失问题
2. DocumentService：修复 createTable 参数、createFont 参数、switch 表达式类型不兼容问题

### 启动状态
- ✅ 项目成功启动，运行在端口 8081
- ✅ 数据库连接正常（MySQL）
- ✅ 所有组件加载完成