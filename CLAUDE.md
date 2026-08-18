# Personal Blog System - Claude 工作规范

## 项目概述

这是一个前后端分离的个人博客系统后端项目，基于 Spring Boot 3.2.5 + Spring Data JPA + Spring Security + JWT 构建。

**核心功能：**
- 用户注册、登录（JWT 认证）
- 文章管理（发表、编辑、删除、分类、标签）
- 评论系统（支持两层嵌套回复）
- 权限控制（普通用户 vs 管理员）

**技术栈：**
- Java 17
- Spring Boot 3.2.5 (Web, Data JPA, Security, Validation)
- JWT (jjwt 0.12.5)
- MySQL / H2 (开发环境)
- Lombok
- Maven

## 工作原则

### 1. 理解优先，行动在后

**在执行任何指令前，必须确保：**
- 理解需求的真实意图，而非字面含义
- 清楚这个改动对现有代码的影响范围
- 知道需要修改哪些文件、涉及哪些层（Controller/Service/Repository/Entity）
- 明确改动的边界条件和潜在风险

**何时需要提问：**
- 需求描述不明确或存在歧义
- 有多种实现方案且没有明显最优解
- 改动会影响多个模块或破坏现有功能
- 涉及数据库 schema 变更或数据迁移
- 需求与现有架构设计存在冲突

**何时可以直接执行：**
- 需求清晰、范围明确、实现方案唯一
- 改动是局部的、影响范围可控
- 符合现有架构和代码风格
- 不涉及破坏性变更

### 2. 架构约束

**分层架构（严格遵守）：**
```
Controller 层 → 处理 HTTP 请求/响应，不写业务逻辑
Service 层    → 核心业务逻辑、事务管理、权限校验
Repository 层 → 数据库操作，不写业务逻辑
Entity 层     → 数据模型，与数据库表映射
DTO 层        → 数据传输对象，与前端交互
```

**禁止跨层调用：**
- Controller 不能直接调用 Repository
- Repository 不能调用 Service
- Entity 和 DTO 必须分离，不能混用

### 3. 代码风格规范

**必须遵循的规范：**
- 使用 Lombok 注解减少样板代码（`@Data`, `@Builder`, `@RequiredArgsConstructor` 等）
- 使用 Builder 模式构建实体对象
- Repository 方法优先使用方法命名约定，复杂查询使用 `@Query` + JPQL
- 异常处理：业务异常使用自定义 `ApiException` 及其子类
- RESTful API 设计：
  - URL 使用名词，不使用动词
  - 使用正确的 HTTP 方法（GET/POST/PUT/DELETE）
  - 返回合适的 HTTP 状态码
- 事务边界在 Service 层，使用 `@Transactional`
- 敏感操作（删除、更新权限）需要权限校验
- 密码必须使用 BCrypt 加密存储

**命名约定：**
- Entity: `User`, `Article`, `Comment`
- DTO Request: `LoginRequest`, `ArticleRequest`
- DTO Response: `AuthResponse`, `ArticleResponse`
- Service: `UserService`, `ArticleService`
- Repository: `UserRepository`, `ArticleRepository`
- Controller: `AuthController`, `ArticleController`

### 4. 依赖管理规则

**引入新依赖前必须：**
1. 说明引入该依赖的理由（解决什么问题、为什么选它）
2. 检查是否与现有依赖冲突（版本兼容性、功能重复）
3. 确认依赖的维护状态（是否活跃、安全性）
4. 评估对项目的影响（包大小、性能、学习成本）

**已有依赖清单：**
```xml
Spring Boot 3.2.5
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - spring-boot-starter-validation
MySQL Connector
H2 Database (开发环境)
JWT (io.jsonwebtoken:jjwt 0.12.5)
Lombok
```

**原则：能用现有依赖解决的，不引入新依赖。**

### 5. 数据库操作规范

**关键约束：**
- 关闭了 `open-in-view`，必须在 Service 层完成所有数据加载
- 注意 N+1 查询问题，使用 `JOIN FETCH` 或批量查询优化
- 并发计数器（如 `viewCount`）使用数据库原子更新：
  ```java
  // ❌ 错误：读改写，有并发问题
  article.setViewCount(article.getViewCount() + 1);
  
  // ✅ 正确：数据库原子操作
  @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
  void incrementViewCount(@Param("id") Long id);
  ```
- `@ManyToMany` 关联集合使用 `clear() + addAll()`，不要直接替换引用
- 软删除优于硬删除（保留数据完整性）

### 6. 安全规范

**认证与授权：**
- 使用 JWT 无状态认证（不使用 Session）
- token 有效期 24 小时，过期后需重新登录
- 密码使用 BCrypt 加密，不可逆
- 敏感接口需要 `@PreAuthorize` 或 Spring Security 配置保护

**输入校验：**
- 所有接收的 DTO 使用 `@Valid` 触发校验
- 在 DTO 类上使用 `@NotBlank`, `@Email`, `@Size` 等注解
- 业务逻辑层做二次校验（如用户名唯一性）

### 7. 错误处理

**统一异常处理：**
- 所有业务异常继承 `ApiException`
- 已定义异常：
  - `BadRequestException` → 400
  - `NotFoundException` → 404
  - `ForbiddenException` → 403
  - `ConflictException` → 409
- `GlobalExceptionHandler` 统一捕获并返回 JSON 格式错误响应
- 不要在 Controller 层 catch 业务异常，让全局处理器处理

### 8. 测试要求

**何时需要测试：**
- 新增核心业务逻辑
- 修改涉及数据一致性的代码
- 修复 bug 后需要回归测试

**测试原则：**
- Service 层是测试重点（业务逻辑集中地）
- 使用 Mock 隔离外部依赖
- 关键业务流程需要集成测试

### 9. 环境配置

**Profile 管理：**
- `h2`: 开发环境，内存数据库，快速启动，重启数据消失
- `mysql`: 生产环境，持久化存储

**配置文件：**
- `application.yml` 为主配置文件
- 使用 `---` 分隔不同 profile
- 敏感信息（数据库密码、JWT 密钥）不要硬编码，使用环境变量

### 10. 改动前的检查清单

**在实施任何代码改动前，确认：**

- [ ] 我理解了需求的真实意图
- [ ] 我知道这个改动会影响哪些文件和模块
- [ ] 我清楚改动的边界条件（输入、输出、异常情况）
- [ ] 改动符合现有架构和代码风格
- [ ] 不会引入安全风险或性能问题
- [ ] 不会破坏现有功能（如需破坏性变更，必须明确说明）
- [ ] 如需引入新依赖，已说明理由并确认无冲突
- [ ] 如需修改数据库 schema，已评估数据迁移方案

**如果任何一项不确定，先提问澄清，再动手。**

## 项目结构参考

```
src/main/java/com/blog/
├── config/           # 配置类（SecurityConfig, DataInitializer）
├── controller/       # REST API 控制器
├── dto/             # 数据传输对象（Request/Response）
├── entity/          # 数据库实体（User, Article, Comment, Category, Tag）
├── exception/       # 自定义异常和全局异常处理器
├── repository/      # 数据访问层（JPA Repository）
├── security/        # 安全相关（JwtUtil, JwtAuthFilter, AuthPrincipal）
└── service/         # 业务逻辑层

src/main/resources/static/  # 前端静态文件（HTML/CSS/JS，随 jar 打包）
```

## 常见任务参考

### 添加新功能
1. 先明确需求：输入、输出、业务规则、权限要求
2. 评估影响：是否需要新表、新字段、新接口
3. 自底向上实现：Entity → Repository → Service → Controller → DTO
4. 添加异常处理和参数校验
5. 测试验证

### 修改现有功能
1. 先理解现有实现逻辑
2. 评估改动影响范围（会不会影响其他调用方）
3. 保持向后兼容，或明确标注破坏性变更
4. 回归测试

### 修复 Bug
1. 先复现问题
2. 定位根因（是逻辑错误、并发问题、还是边界条件未覆盖）
3. 最小化改动范围
4. 添加测试用例防止回归

### 性能优化
1. 先定位瓶颈（数据库查询、N+1 问题、缓存缺失）
2. 评估优化收益和复杂度
3. 优化后验证功能正确性

---

**核心理念：理解清楚再动手，代码质量优于速度，安全和稳定性是第一优先级。**
