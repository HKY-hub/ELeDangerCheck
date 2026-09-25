# 项目改进日志

## v1.1.0 - 2026-07-12

### 新增功能

1. **完整后端API开发**
   - 新增用户实体类 (User.java) - 实现Spring Security UserDetails接口
   - 新增用户数据访问层 (UserRepository.java) - 使用Spring Data JPA
   - 新增用户业务逻辑层 (UserService.java) - 实现注册、登录功能
   - 新增认证控制器 (AuthController.java) - 提供REST API端点
   - 新增DTO类 (LoginRequest.java, LoginResponse.java, RegisterRequest.java)

2. **安全认证系统**
   - 集成Spring Security实现用户认证
   - 实现JWT令牌认证机制
   - 新增JwtUtil工具类处理令牌生成与验证
   - 配置密码加密 (BCryptPasswordEncoder)

3. **配置管理**
   - 新增CORS跨域配置 (CorsConfig.java)
   - 新增全局异常处理 (GlobalExceptionHandler.java)
   - 新增业务异常类 (BusinessException.java)
   - 支持多环境配置切换 (H2/MySQL)

4. **前端页面优化**
   - 重构登录页面 (login.html)
   - 重构注册页面 (register.html)
   - 新增静态资源目录结构

### 技术改进

1. **数据库支持**
   - 配置H2内存数据库用于开发测试
   - 配置MySQL生产环境支持
   - 新增schema-mysql.sql初始化脚本
   - 支持数据库配置热切换

2. **依赖管理**
   - 添加Spring Security依赖
   - 添加JWT依赖 (jjwt)
   - 添加MySQL驱动依赖
   - 添加Validation依赖
   - 添加H2数据库依赖

3. **问题修复**
   - 修复Lombok与JDK版本兼容性问题
   - 解决Spring Security循环依赖问题
   - 修复配置文件缩进错误

### API端点

| 端点 | 方法 | 描述 |
|------|------|------|
| /api/auth/register | POST | 用户注册 |
| /api/auth/login | POST | 用户登录 |
| /api/auth/health | GET | 健康检查 |

### 配置说明

- 默认使用H2内存数据库，可通过`spring.profiles.active=mysql`切换到MySQL
- MySQL配置：数据库名`eledangercheck`，用户名`root`，密码由环境变量 `SPRING_DATASOURCE_PASSWORD` 注入
- 服务端口：8081