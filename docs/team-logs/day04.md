# 项目经理日报 day04

## 团队信息

- **团队名称**：第七组
- **项目经理**：马哲宇

---

## 组员任务完成情况与项目进度综述

| 成员姓名 | 任务描述 | 完成度 |
|----------|----------|--------|
| 马哲宇 | DeepSeek LLM API 集成（LlmService + LlmConfig 双引擎架构）；TaskParserService LLM→正则降级解析；SecurityConfig 安全策略修复；项目文件结构整理（docs/ + infra/ 重构）；API Key 安全隔离（application-local.yaml gitignore）；完成 Phase 1 规则引擎 15+ 条危险点识别规则。 | 100% |
| 田天乐 | 前端 TaskDetail 任务详情页全状态开发（加载/错误/空/数据态）；危险点卡片组件与控制措施列表渲染；与后端 API 联调测试；修复前端路由配置；完成任务历史表格与详情页跳转交互。 | 100% |
| 黄开元 | Elasticsearch RAG Phase 2 基础设施搭建（docker-compose.yml + es-data 导入脚本）；ES 事故案例库 JSON 数据准备；解决开发环境依赖冲突（Jackson 版本补全）；参与 LLM API 接口测试与调试。 | 80% |

---

## 出现的问题

1. **Jackson 依赖缺失**：ElasticsearchService 和 LlmService 使用 ObjectMapper 但 pom.xml 未引入 jackson-databind，导致编译报错。已通过添加 jackson-databind 和 jackson-core 依赖修复。
2. **Spring Security 阻塞 SPA 路由**：SecurityConfig 中 `.anyRequest().authenticated()` 拦截了 `/task/{id}` 等 Vue Router 前端路由，导致 403 错误。已改为 `.anyRequest().permitAll()`，同时保持 API 路径的安全控制。
3. **Windows curl 中文编码问题**：Git Bash curl 在发送含中文的 JSON 请求时出现乱码，影响 API 测试。解决方案为改用 Python requests 库进行测试，已完成测试脚本适配。
4. **前端 heredoc 创建含模板字面量的文件时转义问题**：Vue 组件中包含 JavaScript 模板字符串（反引号），与 bash heredoc 冲突，需改用 Python 写入或逐文件创建。

---

## 项目进度

-  DeepSeek LLM API 集成 — LlmService + LlmConfig，支持 `@ConditionalOnProperty` 开关控制，API Key 安全隔离
-  TaskParserService 双引擎架构 — LLM 主解析 → Jackson 结构化 → 正则表达式 fallback，4 个测试用例全部通过
-  危险点识别 Phase 1 — 15+ 条规则引擎覆盖 8 大类别（触电/高坠/机械伤害/有限空间/误操作/火灾/物体打击/环境伤害）
-  前端 TaskDetail 详情页 — 覆盖 loading/error/empty/data 四态，玻璃拟态暗色主题
-  SPA 路由修复 — 前端 `/task/:id` 正确渲染，Vue Router 正常接管
-  项目结构整理 — 根目录清理，docs/ + infra/ 分层归档
-  ES RAG Phase 2 — 代码就绪（docker-compose.yml + es-data），待启动 Docker Desktop

**总体项目进度：约 30%。**

---

## 今天遇到的问题及解决过程描述

今天主要完成了三大任务：DeepSeek LLM API 集成、前端任务详情页开发、以及项目结构整理。

**LLM API 集成**方面，设计了全新的双引擎解析架构。主引擎通过 LlmService 调用 DeepSeek API（OpenAI 兼容接口），将作业任务自然语言描述解析为结构化 JSON（电压等级、设备类型、作业类型、环境条件）；当 LLM 服务不可用或返回异常时，自动降级到 TaskParserService 的正则表达式引擎进行规则匹配。该架构通过 `Optional<LlmService>` 注入 + `@ConditionalOnProperty` 实现优雅降级，无需硬编码开关。测试结果：4 个测试用例（更换绝缘子、变压器试验、电缆抢修、有限空间作业）全部正确解析。

**任务详情页开发**过程中，遇到了 Spring Security 拦截 SPA 路由的问题。`/task/{id}` 路由被 Spring Security 拦截返回 403，原因是 `.anyRequest().authenticated()` 对所有未明确 permitAll 的路径强制认证。通过修改为 `.anyRequest().permitAll()`，同时保留 API 层面的 `@AuthenticationPrincipal` 控制，解决了前端路由访问问题。详情页采用 Vue 3 Composition API，覆盖了 loading（加载动画）、error（错误提示 + 重试按钮）、empty（无危险点提示）、data（完整信息展示）四种 UI 状态。

**项目结构整理**方面，根目录原本散落着多个 SQL 截图、项目描述、旧版日志等文件，按照工程标准重新组织为 `docs/`（文档）、`infra/`（基础设施）、`ELeDangerCheck/`（后端）、`frontend/`（前端）四层结构。API Key 被安全地隔离到 `application-local.yaml`（已加入 `.gitignore`），防止密钥泄露。

---

## 收获与感想综述

今天团队的开发效率有了显著提升，主要得益于 AI 辅助开发工具与团队分工的进一步磨合。LLM API 的成功集成为系统注入了智能解析能力，使得自然语言输入到结构化数据的转换准确率大幅提升。双引擎架构的设计为系统的鲁棒性提供了保障——即使 LLM 服务不稳定，正则引擎仍能保证基础功能可用。前端详情页的完成使得系统具备了完整的"任务输入→解析→危险点识别→详情查看"闭环交互体验。

通过今天的工作，团队对 Spring Boot 4.x + Spring Security 7.x 的 API 变化有了更深的认识，也积累了 SPA + 后端分离架构下的路由治理经验。Phase 1 规则引擎的完善为 Phase 2 ES RAG 上线提供了坚实的能力基线，后续在 ES 上线后，危险点识别将叠加检索增强能力，进一步提升覆盖率和准确率。

---

## 组员考勤

- 全勤。
- 本组全体成员于 **8:30 前** 到达实验室，按时开展软件工程实训工作。
