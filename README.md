# Personal Blog System

一个基于 Spring Boot + Spring Security + JWT 的前后端分离博客系统。

## 功能特性

- ✅ 用户注册、登录（JWT 认证）
- ✅ 文章管理（发表、编辑、删除）
- ✅ 分类和标签系统
- ✅ 评论系统（支持两层嵌套回复）
- ✅ 权限控制（普通用户/管理员）
- ✅ 响应式前端界面

## 技术栈

**后端：**
- Java 17
- Spring Boot 3.2.5
- Spring Data JPA
- Spring Security
- JWT (jjwt 0.12.5)
- MySQL / H2

**前端：**
- 原生 HTML/CSS/JavaScript
- 响应式设计

## 快速开始

### 本地运行

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd blog
   ```

2. **运行项目（使用 H2 内存数据库）**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **访问应用**
   - 前端：http://localhost:8080/（Spring Boot 直接提供 `static/` 下的页面）
   - 后端 API：http://localhost:8080/api
   - H2 控制台：http://localhost:8080/h2-console

### 使用 MySQL

1. **创建数据库**
   ```sql
   CREATE DATABASE blog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **修改配置**
   ```bash
   # 设置环境变量
   export MYSQL_PASSWORD=your_password
   
   # 或直接修改 application.yml 中的 mysql profile
   ```

3. **启动应用**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

## 部署到 Railway

详细部署指南请查看 [DEPLOYMENT.md](./DEPLOYMENT.md)

**简要步骤：**

1. 推送代码到 GitHub
2. 在 Railway 创建项目并关联仓库
3. 添加 MySQL 数据库
4. 配置环境变量（JWT_SECRET）
5. 自动部署完成

## 项目结构

```
blog/
├── src/main/java/com/blog/
│   ├── config/           # 配置类
│   ├── controller/       # REST API 控制器
│   ├── dto/             # 数据传输对象
│   ├── entity/          # JPA 实体
│   ├── exception/       # 异常处理
│   ├── repository/      # 数据访问层
│   ├── security/        # 安全配置
│   └── service/         # 业务逻辑层
├── src/main/resources/
│   ├── application.yml  # 应用配置
│   └── static/          # 前端文件（index/post/admin/login.html + css/js）
├── CLAUDE.md           # AI 助手工作规范
├── DEPLOYMENT.md       # 部署指南
└── pom.xml            # Maven 配置
```

## API 文档

### 认证接口

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 文章接口

- `GET /api/articles` - 获取文章列表
- `GET /api/articles/{id}` - 获取文章详情
- `POST /api/articles` - 创建文章（需登录）
- `PUT /api/articles/{id}` - 更新文章（需登录）
- `DELETE /api/articles/{id}` - 删除文章（需登录）

### 评论接口

- `GET /api/articles/{id}/comments` - 获取文章评论
- `POST /api/articles/{id}/comments` - 发表评论（需登录）
- `DELETE /api/articles/{id}/comments/{commentId}` - 删除评论（需登录）

### 元数据接口

- `GET /api/categories` - 获取分类列表
- `GET /api/tags` - 获取标签列表

## 开发指南

详细的开发指南和架构说明请查看：
- [CLAUDE.md](./CLAUDE.md) - 代码规范和工作流程

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `JWT_SECRET` | JWT 签名密钥（必须修改） | 默认测试密钥 |
| `MYSQL_PASSWORD` | MySQL 密码 | root |
| `PORT` | 应用端口 | 8080 |

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

如有问题或建议，欢迎通过 GitHub Issues 联系。
