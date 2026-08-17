# 🚀 个人博客系统完整部署流程

> **目标：** 将本地的 Spring Boot 博客系统部署到云端，让任何人都可以通过互联网访问

---

## 📊 部署全景图

```
本地开发环境
    ↓ (git push)
GitHub 代码仓库
    ↓ (自动触发)
Railway 云平台
    ├─ 构建 Java 应用 (Maven + JDK 17)
    ├─ 启动 Spring Boot 服务
    └─ 连接 MySQL 数据库
    ↓
生成公网域名 (https://xxx.railway.app)
    ↓
用户通过浏览器访问
```

---

## 🛠️ 需要准备的工具和平台

### 必需工具（免费）

| 工具/平台 | 用途 | 成本 | 注册地址 |
|----------|------|------|---------|
| **GitHub** | 代码托管 + 版本控制 | 免费 | https://github.com/signup |
| **Railway** | 应用部署 + 数据库 | $5/月免费额度 | https://railway.app |
| **Git** | 代码推送工具 | 免费 | https://git-scm.com |
| **Java 17** | 编译运行环境 | 免费 | 已安装（项目需要）|
| **Maven** | 项目构建工具 | 免费 | 已安装（项目需要）|

### 不需要的东西（常见误区）

❌ **域名** - Railway 会自动分配一个免费域名（如 `your-app.railway.app`）  
❌ **DNS 服务器** - Railway 自动配置  
❌ **SSL 证书** - Railway 自动提供 HTTPS  
❌ **服务器** - Railway 提供托管环境  
❌ **Nginx/Apache** - Railway 自动处理反向代理  
❌ **Docker** - Railway 使用 Nixpacks 自动构建

### 可选工具（进阶）

| 工具 | 用途 | 何时需要 |
|-----|------|---------|
| **自定义域名** | 使用自己的域名（如 blog.com） | 正式运营时 |
| **Railway CLI** | 命令行管理部署 | 需要自动化脚本时 |
| **Postman/Insomnia** | API 测试工具 | 验证接口时更方便 |

---

## 📝 完整部署流程（分阶段详解）

### 阶段 0：前置检查（5 分钟）

**目标：** 确保本地环境和代码准备就绪

**使用工具：** 自动化脚本 `deploy-check.ps1` (Windows) 或 `deploy-check.sh` (Linux/Mac)

**操作步骤：**

```powershell
# Windows
cd C:\Users\86151\Desktop\blog
.\deploy-check.ps1

# Linux/Mac
cd ~/Desktop/blog
chmod +x deploy-check.sh
./deploy-check.sh
```

**脚本会做什么：**
1. ✅ 检查 Git、Maven、Java 是否安装
2. ✅ 验证 Java 版本 >= 17
3. ✅ 检查配置文件（`application.yml`, `nixpacks.toml`, `railway.json`）
4. ✅ 测试项目编译 (`mvn clean package`)
5. ✅ 检查 Git 状态（是否有未提交的改动）
6. ✅ 提示你生成 JWT_SECRET（后面会用）

**预期结果：**
```
✓ Java 版本: 17 (满足要求 >= 17)
✓ 项目编译成功
✓ Git 仓库已初始化
✓ 所有配置文件存在
```

**出问题怎么办：**
- `✗ Git 未安装` → 下载安装：https://git-scm.com
- `✗ Java 版本过低` → 安装 JDK 17：https://adoptium.net
- `✗ 项目编译失败` → 检查 `pom.xml` 和代码是否有错误

---

### 阶段 1：生成 JWT 密钥（2 分钟）

**目标：** 生成一个安全的密钥用于 JWT token 签名

**为什么需要：** JWT 需要密钥来加密用户登录 token，防止伪造

**使用工具：** 系统命令行

**操作步骤：**

**Windows PowerShell：**
```powershell
$bytes = New-Object byte[] 64
(New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

**Linux/Mac：**
```bash
openssl rand -base64 64
```

**在线生成（备选）：**
https://generate-random.org/api-token-generator?count=1&length=64

**预期输出：**
```
Kj8vX2pQ7wZ9... (一串64个字符的随机字符串)
```

**⚠️ 重要：** 
- 复制这个密钥，保存到记事本或密码管理器
- 不要提交到 Git 仓库
- 稍后在 Railway 配置环境变量时使用

---

### 阶段 2：推送代码到 GitHub（5 分钟）

**目标：** 把本地代码上传到 GitHub，供 Railway 部署使用

**为什么需要 GitHub：** Railway 从 GitHub 拉取代码进行自动部署

**使用平台：** GitHub (https://github.com)

#### 步骤 2.1：创建 GitHub 仓库

1. 登录 GitHub：https://github.com/login
2. 点击右上角 `+` → `New repository`
3. 填写信息：
   - Repository name: `personal-blog` (或其他名字)
   - Description: `个人博客系统后端 API`
   - **选择 Private**（推荐，防止代码泄露）
   - ❌ **不要勾选** "Initialize this repository with a README"
4. 点击 `Create repository`

**预期结果：** 得到一个空仓库地址
```
https://github.com/你的用户名/personal-blog.git
```

#### 步骤 2.2：关联远程仓库并推送

```bash
# 检查当前状态
git status

# 如果有未提交的改动
git add .
git commit -m "Ready for Railway deployment"

# 关联远程仓库（替换为你的仓库地址）
git remote add origin https://github.com/你的用户名/personal-blog.git

# 推送代码
git branch -M main
git push -u origin main
```

**第一次推送可能需要登录：**
- GitHub 会弹出登录窗口（使用浏览器认证）
- 或提示输入用户名/密码（密码需要使用 Personal Access Token）

**预期结果：**
```
Enumerating objects: 45, done.
Counting objects: 100% (45/45), done.
...
To https://github.com/你的用户名/personal-blog.git
 * [new branch]      main -> main
```

**出问题怎么办：**
- `Authentication failed` → 需要配置 Personal Access Token
  1. GitHub → Settings → Developer settings → Personal access tokens
  2. Generate new token (classic)
  3. 勾选 `repo` 权限
  4. 复制 token，用作密码
- `Remote already exists` → 删除旧的：`git remote remove origin`

---

### 阶段 3：在 Railway 创建项目（10 分钟）

**目标：** 创建云端部署环境，连接 GitHub 仓库

**使用平台：** Railway (https://railway.app)

#### 步骤 3.1：注册并登录 Railway

1. 访问：https://railway.app
2. 点击 `Login` → 选择 `Login with GitHub`（推荐）
3. 授权 Railway 访问你的 GitHub 账号

**为什么用 GitHub 登录：** 方便自动连接仓库，无需额外配置权限

#### 步骤 3.2：创建新项目

1. 进入 Railway Dashboard
2. 点击 `New Project`
3. 选择 `Deploy from GitHub repo`
4. 在列表中找到 `personal-blog` 仓库，点击

**Railway 会自动：**
- 克隆代码
- 检测到 Java 项目（通过 `pom.xml`）
- 读取 `nixpacks.toml` 和 `railway.json` 配置
- **开始第一次构建**

#### 步骤 3.3：观察首次构建（会失败，这是正常的）

**Deployments 标签页会显示：**
```
Building...
  └─ Setup: Installing Maven, JDK 17
  └─ Build: mvn clean package -DskipTests
  └─ Start: java -Xmx400m ... -jar target/personal-blog-1.0.0.jar
```

**预期结果：构建成功，但启动失败**
```
✅ BUILD SUCCESSFUL
❌ Application failed to start
   Error: JWT_SECRET environment variable is not set
```

**这是正常的！** 因为我们还没配置环境变量（下一步会配置）

---

### 阶段 4：添加 MySQL 数据库（3 分钟）

**目标：** 创建云端 MySQL 数据库

**为什么需要：** 生产环境不能用 H2 内存数据库，数据会在重启后丢失

**使用工具：** Railway 内置 MySQL 服务

#### 操作步骤

1. 在项目页面点击 `+ New`
2. 选择 `Database`
3. 点击 `Add MySQL`

**Railway 会自动：**
- 创建 MySQL 8.0 实例
- 生成随机密码
- 注入环境变量到你的应用：
  ```
  DATABASE_URL=jdbc:mysql://containers-us-west-xxx.railway.app:6379/railway
  MYSQLUSER=root
  MYSQLPASSWORD=随机生成的强密码
  MYSQLDATABASE=railway
  MYSQLHOST=containers-us-west-xxx.railway.app
  MYSQLPORT=6379
  ```

**预期结果：** 在项目视图中看到两个服务
```
📦 personal-blog (你的应用)
🗄️ MySQL (数据库)
```

---

### 阶段 5：配置环境变量（5 分钟）

**目标：** 告诉应用如何运行（使用哪个配置文件、JWT 密钥是什么）

**使用工具：** Railway 环境变量配置

#### 操作步骤

1. 点击你的应用服务（`personal-blog`）
2. 切换到 `Variables` 标签
3. 点击 `+ New Variable` 添加以下变量：

**必须设置的变量：**

| Variable Name | Value | 说明 |
|--------------|-------|------|
| `SPRING_PROFILES_ACTIVE` | `railway` | 激活生产环境配置 |
| `JWT_SECRET` | `<你在阶段1生成的密钥>` | JWT 签名密钥 |

**Railway 自动提供的变量（无需手动添加）：**
- `PORT` - Railway 分配的端口号
- `DATABASE_URL` - MySQL 连接地址（从 MySQL 服务注入）
- `MYSQLUSER` - 数据库用户名
- `MYSQLPASSWORD` - 数据库密码
- `MYSQLDATABASE` - 数据库名称

#### 配置后的效果

你的 `application.yml` 中的占位符会被替换：
```yaml
# 配置前（占位符）
url: ${DATABASE_URL:jdbc:mysql://localhost:3306/blog}
username: ${MYSQLUSER:root}
password: ${MYSQLPASSWORD:}

# 运行时（实际值）
url: jdbc:mysql://containers-us-west-xxx.railway.app:6379/railway
username: root
password: Kj8vX2pQ7wZ9...
```

#### 步骤 5.2：触发重新部署

1. 添加完环境变量后，点击右上角 `Deploy`
2. 或者直接推送一个新提交：
   ```bash
   git commit --allow-empty -m "Trigger redeploy"
   git push
   ```

**Railway 会自动：**
1. 检测到新提交或手动部署
2. 重新构建应用
3. 使用新的环境变量启动

---

### 阶段 6：等待部署成功（3-5 分钟）

**目标：** 应用成功启动并可以访问

#### 观察部署日志

1. 在 `Deployments` 标签查看构建进度
2. 点击最新的部署记录查看详细日志

**关键日志标志：**

```
✅ 成功标志：
   Started BlogApplication in 8.234 seconds

❌ 失败标志：
   Exception in thread "main"
   Error: ...
   Application failed to start
```

**预期完整日志：**
```
2026-08-18 10:23:45.123  INFO --- [main] com.blog.BlogApplication         : Starting BlogApplication
2026-08-18 10:23:46.456  INFO --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2026-08-18 10:23:47.789  INFO --- [main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-08-18 10:23:48.123  INFO --- [main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-08-18 10:23:49.456  INFO --- [main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation
2026-08-18 10:23:50.789  INFO --- [main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory
2026-08-18 10:23:52.123  INFO --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http)
2026-08-18 10:23:53.456  INFO --- [main] com.blog.BlogApplication                 : Started BlogApplication in 8.234 seconds
```

**看到 "Started BlogApplication" 就说明成功了！**

---

### 阶段 7：获取公网访问地址（1 分钟）

**目标：** 获得一个可以公开访问的 HTTPS 域名

**使用工具：** Railway 自动分配域名

#### 操作步骤

1. 点击你的应用服务
2. 切换到 `Settings` 标签
3. 滚动到 `Networking` 区域
4. 点击 `Generate Domain`

**Railway 会自动分配：**
```
https://personal-blog-production-xxxx.up.railway.app
```

**特点：**
- ✅ 自动 HTTPS（免费 SSL 证书）
- ✅ 全球 CDN 加速
- ✅ 永久有效（除非你删除项目）
- ✅ 可以随时重新生成

**自定义域名（可选）：**
如果你有自己的域名（如 `blog.com`），可以在这里添加：
1. 点击 `Custom Domain`
2. 输入你的域名：`api.blog.com`
3. 按照提示在域名服务商添加 CNAME 记录

---

### 阶段 8：验证部署（10 分钟）

**目标：** 确保所有功能正常工作

**使用工具：** curl 命令（或 Postman）

#### 测试 1：基础接口（无需认证）

```powershell
# 设置域名变量（替换为你的实际域名）
$API = "https://personal-blog-production-xxxx.up.railway.app"

# 测试分类接口
curl $API/api/metadata/categories
```

**预期返回：**
```json
[]
```
或
```json
[
  {"id": 1, "name": "技术", "description": "技术相关文章"},
  {"id": 2, "name": "生活", "description": "生活随笔"}
]
```

**如果返回 404 或 502：** 应用可能还没启动完成，等待 1 分钟后重试

#### 测试 2：注册新用户

```powershell
curl -X POST "$API/api/auth/register" `
  -H "Content-Type: application/json" `
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123456"
  }'
```

**预期返回：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTcyNDAxMjM0NSwiZXhwIjoxNzI0MDk4NzQ1fQ.abc123...",
  "username": "testuser",
  "email": "test@example.com",
  "role": "USER"
}
```

**保存返回的 token！** 后面的认证接口需要用

#### 测试 3：登录

```powershell
curl -X POST "$API/api/auth/login" `
  -H "Content-Type: application/json" `
  -d '{
    "username": "testuser",
    "password": "Test123456"
  }'
```

**预期返回：** 与注册相同的格式（token 会不同）

#### 测试 4：发表文章（需要认证）

```powershell
# 设置 token（替换为你实际收到的 token）
$TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTcyNDAxMjM0NSwiZXhwIjoxNzI0MDk4NzQ1fQ.abc123..."

curl -X POST "$API/api/articles" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $TOKEN" `
  -d '{
    "title": "测试文章",
    "summary": "这是一篇测试文章",
    "content": "# 标题\n\n这是内容...",
    "categoryId": 1,
    "tagIds": []
  }'
```

**预期返回：**
```json
{
  "id": 1,
  "title": "测试文章",
  "summary": "这是一篇测试文章",
  "content": "# 标题\n\n这是内容...",
  "author": {
    "id": 1,
    "username": "testuser"
  },
  "category": {
    "id": 1,
    "name": "技术"
  },
  "tags": [],
  "createdAt": "2026-08-18T10:30:00",
  "viewCount": 0,
  "commentCount": 0
}
```

#### 测试 5：管理员功能

```powershell
# 使用默认管理员账号登录
curl -X POST "$API/api/auth/login" `
  -H "Content-Type: application/json" `
  -d '{
    "username": "admin",
    "password": "Admin123456"
  }'

# 保存返回的 admin token
$ADMIN_TOKEN = "..."

# 测试创建分类（管理员权限）
curl -X POST "$API/api/metadata/categories" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -d '{
    "name": "新分类",
    "description": "这是新分类"
  }'
```

**所有测试通过 = 部署成功！🎉**

---

## 🔍 问题排查指南

### 问题分类决策树

```
部署失败
  ├─ 构建阶段失败
  │   ├─ Maven 依赖下载超时 → 重新部署（网络波动）
  │   ├─ 编译错误 → 检查代码和 pom.xml
  │   └─ 内存不足 → 检查 JVM 参数配置
  │
  ├─ 启动阶段失败
  │   ├─ JWT_SECRET 未设置 → 检查环境变量
  │   ├─ 数据库连接失败 → 检查 MySQL 服务状态
  │   ├─ 端口冲突 → 检查 PORT 环境变量
  │   └─ 内存溢出 (OOM) → 已配置 -Xmx400m，联系 Railway
  │
  └─ 运行阶段问题
      ├─ 接口 404 → 检查路径和应用是否启动
      ├─ 接口 500 → 查看日志中的异常堆栈
      ├─ 认证失败 → 检查 JWT_SECRET 是否一致
      └─ 数据库操作失败 → 检查 SQL 和 JPA 配置
```

### 详细排查步骤

#### 问题 1：构建超时或失败

**症状：**
```
Error: Build failed
Maven: Connection timeout downloading dependency
```

**可能原因：**
- Railway 网络波动导致 Maven 依赖下载失败
- `pom.xml` 有语法错误
- 依赖版本不存在

**排查步骤：**

1. **查看完整构建日志**
   - Railway → Deployments → 点击失败的部署 → 查看 Build Logs
   
2. **检查错误类型**
   ```
   # 网络超时（重试即可）
   Could not resolve dependencies
   Connection timed out
   
   # 配置错误（需要修复）
   Non-parseable POM
   Compilation failure
   ```

3. **解决方案**
   ```bash
   # 方案 A：重新部署（90% 情况有效）
   Railway Dashboard → 点击 "Redeploy"
   
   # 方案 B：本地验证
   mvn clean package -DskipTests
   # 如果本地编译失败，检查 pom.xml 和代码
   
   # 方案 C：检查依赖版本
   # 访问 Maven Central 确认依赖存在：
   # https://search.maven.org/
   ```

#### 问题 2：应用启动后立即崩溃

**症状：**
```
Started BlogApplication in 8.234 seconds
Application exited with code 1
```

**可能原因：**
- JWT_SECRET 未设置或太短
- 数据库连接失败
- 环境变量配置错误

**排查步骤：**

1. **查看启动日志中的异常**
   ```
   # JWT 密钥问题
   Error: JWT secret is too short
   Error: JWT_SECRET environment variable is not set
   
   # 数据库连接问题
   Communications link failure
   Unable to create initial connections
   Access denied for user
   
   # 配置问题
   Could not resolve placeholder 'DATABASE_URL'
   ```

2. **检查环境变量**
   ```
   Railway → Variables 标签
   
   必须存在：
   ✅ SPRING_PROFILES_ACTIVE = railway
   ✅ JWT_SECRET = (64+ 字符的随机字符串)
   
   自动注入（不需手动添加）：
   ✅ DATABASE_URL
   ✅ MYSQLUSER
   ✅ MYSQLPASSWORD
   ✅ PORT
   ```

3. **验证 MySQL 服务**
   ```
   Railway Dashboard
   → 确认 MySQL 服务状态为 "Active"
   → 点击 MySQL 服务 → Variables 标签
   → 确认 MYSQLPASSWORD 已生成
   ```

4. **解决方案**
   ```bash
   # 问题：JWT_SECRET 未设置
   # 解决：添加环境变量
   Variables → New Variable
   Name: JWT_SECRET
   Value: <使用阶段1生成的64位密钥>
   
   # 问题：数据库连接失败
   # 解决：确保 MySQL 服务在同一个 Project
   # Railway 只会自动注入同一项目内的服务变量
   ```

#### 问题 3：接口返回 404

**症状：**
```bash
curl https://your-app.railway.app/api/metadata/categories
# 返回：404 Not Found
```

**可能原因：**
- 应用未成功启动
- URL 路径错误
- 端口配置问题

**排查步骤：**

1. **确认应用已启动**
   ```
   Railway → Deployments → 最新部署
   → 状态应为 "Active" 或 "Running"
   → 日志中应有 "Started BlogApplication"
   ```

2. **检查 URL 路径**
   ```bash
   # ✅ 正确路径
   /api/metadata/categories
   /api/auth/login
   
   # ❌ 错误路径
   /metadata/categories  # 缺少 /api
   /api/categories       # 路径不完整
   ```

3. **验证端口配置**
   ```yaml
   # application.yml 中应该有
   server:
     port: ${PORT:8080}
   
   # Railway 会自动设置 PORT 环境变量
   ```

4. **测试根路径**
   ```bash
   # 如果根路径返回 404，说明应用根本没启动
   curl https://your-app.railway.app/
   
   # 如果返回 Whitelabel Error Page，说明应用已启动
   # （只是没有配置根路径的 Controller）
   ```

#### 问题 4：接口返回 500 Internal Server Error

**症状：**
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Something went wrong"
}
```

**排查步骤：**

1. **查看应用日志**
   ```
   Railway → 点击你的服务 → Logs 标签
   → 查找 ERROR 或 Exception 关键词
   ```

2. **常见异常类型**
   ```java
   // 数据库异常
   org.hibernate.exception.SQLGrammarException
   → 检查实体类和数据库表结构是否匹配
   
   // 空指针异常
   java.lang.NullPointerException
   → 检查数据加载是否完整（注意 open-in-view=false）
   
   // 类型转换异常
   org.springframework.core.convert.ConversionFailedException
   → 检查请求参数类型是否匹配
   ```

3. **调试技巧**
   ```yaml
   # 临时开启详细日志（在 Railway Variables 添加）
   LOGGING_LEVEL_COM_BLOG=DEBUG
   
   # 重新部署后查看详细日志
   ```

#### 问题 5：认证失败（401 Unauthorized）

**症状：**
```bash
curl -H "Authorization: Bearer <token>" ...
# 返回：401 Unauthorized
```

**可能原因：**
- token 过期（24 小时有效期）
- JWT_SECRET 不一致
- token 格式错误

**排查步骤：**

1. **检查 token 格式**
   ```bash
   # ✅ 正确格式
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWI...
   
   # ❌ 错误格式
   Authorization: eyJhbGciOiJIUzI1NiJ9.eyJzdWI...  # 缺少 Bearer
   Authorization: Bearer <token>                    # 忘记替换占位符
   ```

2. **重新登录获取新 token**
   ```bash
   curl -X POST "$API/api/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","password":"Test123456"}'
   ```

3. **验证 JWT_SECRET**
   ```
   # 如果修改过 JWT_SECRET，旧 token 会全部失效
   # 需要所有用户重新登录
   ```

#### 问题 6：数据库数据丢失

**症状：**
- 重启后之前创建的文章/用户不见了

**可能原因：**
- 错误使用了 H2 内存数据库
- MySQL 服务被删除
- 使用了错误的 profile

**排查步骤：**

1. **检查当前使用的数据库**
   ```
   Railway → Variables 标签
   → 确认 SPRING_PROFILES_ACTIVE=railway
   
   如果是 h2，数据会在重启后丢失
   ```

2. **检查数据库配置**
   ```yaml
   # application.yml railway profile 应该是
   datasource:
     url: ${DATABASE_URL:...}  # 使用 MySQL
     driver-class-name: com.mysql.cj.jdbc.Driver
   
   # 而不是
   datasource:
     url: jdbc:h2:file:./data/blog  # H2 内存数据库
   ```

3. **验证 MySQL 连接**
   ```bash
   # Railway CLI 连接数据库
   railway run mysql -u root -p
   
   # 查看表
   SHOW TABLES;
   SELECT COUNT(*) FROM user;
   ```

---

## 📊 监控与维护

### 日常监控清单

#### 1. 资源使用监控（每周检查）

**在 Railway Dashboard：**
```
项目页面 → Metrics 标签

关注指标：
- CPU 使用率（应 < 80%）
- 内存使用量（应 < 400MB）
- 磁盘使用量（数据库大小）
- 网络流量（月度累计）
```

**免费额度警告：**
```
Usage 标签
- 运行时间：< 500 小时/月
- 估算成本：< $5/月
- 如果接近上限，考虑启用 Sleep on Idle
```

#### 2. 应用健康检查（每天检查）

**自动化脚本：**
```powershell
# health-check.ps1
$API = "https://your-app.railway.app"

# 测试基础接口
$response = curl -s -o /dev/null -w "%{http_code}" "$API/api/metadata/categories"

if ($response -eq 200) {
    Write-Host "✅ 服务正常" -ForegroundColor Green
} else {
    Write-Host "❌ 服务异常: HTTP $response" -ForegroundColor Red
    # 发送告警邮件或短信
}
```

#### 3. 日志审查（出问题时查看）

**关键日志模式：**
```
# 正常日志
INFO  --- Started BlogApplication
INFO  --- HikariPool-1 - Starting...

# 警告日志（需要关注）
WARN  --- Connection pool exhausted
WARN  --- Slow query detected: 2.5s

# 错误日志（需要立即处理）
ERROR --- Exception in thread "main"
ERROR --- Unable to connect to database
```

### 性能优化建议

#### 1. 数据库优化

```java
// 添加索引（如果查询慢）
@Entity
@Table(name = "article", indexes = {
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_category", columnList = "category_id")
})
public class Article { ... }
```

#### 2. 启用查询缓存（进阶）

```yaml
# application.yml railway profile
spring:
  cache:
    type: simple  # 或接入 Redis
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          use_query_cache: true
```

#### 3. 限流保护（防止滥用）

```java
// 使用 Bucket4j 或 Spring Security 限流
@RateLimiter(name = "default")
@GetMapping("/api/articles")
public List<ArticleResponse> getArticles() { ... }
```

---

## 🎯 部署成功后的下一步

### 必做事项

- [x] ✅ 测试所有 API 接口
- [ ] 🔐 修改管理员默认密码
- [ ] 📝 配置前端项目的 API 地址
- [ ] 💾 设置数据库备份计划
- [ ] 📊 接入错误监控（Sentry）

### 可选事项

- [ ] 🌐 绑定自定义域名
- [ ] 📧 配置邮件发送服务（注册验证、找回密码）
- [ ] 🖼️ 接入图片存储服务（七牛云、阿里云 OSS）
- [ ] 🚀 设置 CI/CD 自动测试
- [ ] 📄 编写 API 文档（Swagger/Postman）

---

## 📞 获取帮助

### Railway 官方支持

- 📖 官方文档：https://docs.railway.app
- 💬 Discord 社区：https://discord.gg/railway
- 📊 系统状态：https://status.railway.app

### 本项目文档

- `QUICK_REFERENCE.md` - 快速参考卡片
- `DEPLOYMENT_CHECKLIST.md` - 详细检查清单
- `DEPLOYMENT.md` - 原始部署指南
- `deploy-check.ps1` / `deploy-check.sh` - 自动化检查脚本

---

**🎉 祝你部署顺利！**

有任何问题随时翻阅本文档，或查看日志进行排查。

---

**文档版本：** v1.0  
**最后更新：** 2026-08-18  
**适用平台：** Railway (Render 类似流程)
