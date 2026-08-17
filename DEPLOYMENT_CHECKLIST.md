# 🚀 部署前检查清单

在部署到 Railway/Render 之前，请逐项检查：

## ✅ 代码准备

- [ ] 项目可以本地正常运行（`mvn spring-boot:run`）
- [ ] 所有测试通过（或已跳过测试：`mvn clean package -DskipTests`）
- [ ] `.gitignore` 已配置，不会提交敏感信息
- [ ] `data/` 目录已被忽略（本地 H2 数据库文件）

## ✅ 配置文件检查

- [ ] `application.yml` 中 `railway` profile 已配置
- [ ] JWT_SECRET 使用环境变量（`${JWT_SECRET:...}`）
- [ ] 数据库连接使用环境变量（`${DATABASE_URL}`, `${MYSQLUSER}`, `${MYSQLPASSWORD}`）
- [ ] `server.port` 设置为 `${PORT:8080}`（Railway 会注入 PORT 环境变量）
- [ ] H2 Console 在 railway profile 中已禁用
- [ ] `show-sql: false` 和 `include-stacktrace: never` 已设置

## ✅ 部署配置文件

- [ ] `nixpacks.toml` 存在且正确配置
- [ ] `railway.json` 存在且正确配置
- [ ] `render.yaml` 存在（如果使用 Render）
- [ ] JVM 内存参数已优化（`-Xmx400m -Xms200m`）

## ✅ GitHub 准备

- [ ] GitHub 账号已注册
- [ ] 已创建远程仓库（可以是私有）
- [ ] 本地代码已提交：`git status` 显示干净
- [ ] 远程仓库已关联：`git remote -v` 显示 origin

## ✅ Railway/Render 账号

- [ ] 已注册 Railway 或 Render 账号
- [ ] 已用 GitHub 账号登录（推荐）
- [ ] 了解免费额度限制

## ✅ 安全准备

- [ ] 已准备好生产环境的 JWT_SECRET（至少 64 字符）
- [ ] 不使用仓库中的默认 JWT_SECRET
- [ ] 数据库密码将使用平台生成的强密码

---

## 🔑 JWT_SECRET 生成

选择以下任一方式生成安全的密钥：

### Linux/Mac
```bash
openssl rand -base64 64
```

### Windows PowerShell
```powershell
$bytes = New-Object byte[] 64
(New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### 在线生成
访问：https://generate-random.org/api-token-generator?count=1&length=64

**重要：** 生成后保存到安全的地方（密码管理器），部署时需要设置到环境变量。

---

## 📦 快速部署命令

### 1. 推送到 GitHub

```bash
# 检查当前状态
git status

# 如果有未提交的改动
git add .
git commit -m "Ready for deployment"

# 推送到 GitHub（第一次）
git remote add origin https://github.com/你的用户名/仓库名.git
git branch -M main
git push -u origin main

# 后续推送
git push
```

### 2. Railway 部署

1. 访问 https://railway.app
2. New Project → Deploy from GitHub repo
3. 选择你的仓库
4. 点击 + New → Database → Add MySQL
5. 设置环境变量：
   - `SPRING_PROFILES_ACTIVE=railway`
   - `JWT_SECRET=你生成的密钥`
6. 等待部署完成（约 2-3 分钟）
7. Settings → Generate Domain 获取访问地址

### 3. Render 部署

1. 访问 https://render.com
2. New → Web Service
3. 连接 GitHub 仓库
4. 配置：
   - Build Command: `mvn clean package -DskipTests`
   - Start Command: `java -Xmx400m -Xms200m -Dserver.port=$PORT -Dspring.profiles.active=railway -jar target/personal-blog-1.0.0.jar`
5. 添加环境变量（同上）
6. 创建外部数据库（PlanetScale 或 Railway MySQL）

---

## 🧪 部署后验证

部署成功后，逐项测试：

### 1. 基础接口测试

```bash
# 替换为你的实际域名
export API_URL="https://你的域名.railway.app"

# 测试分类接口（无需认证）
curl $API_URL/api/metadata/categories

# 应返回：[]（空数组）或默认分类
```

### 2. 注册测试

```bash
curl -X POST $API_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123456"
  }'

# 应返回：
# {
#   "token": "eyJhbGc...",
#   "username": "testuser",
#   "email": "test@example.com",
#   "role": "USER"
# }
```

### 3. 登录测试

```bash
curl -X POST $API_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456"
  }'

# 保存返回的 token
export TOKEN="返回的token值"
```

### 4. 认证接口测试

```bash
# 测试发表文章（需要认证）
curl -X POST $API_URL/api/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "测试文章",
    "summary": "这是测试摘要",
    "content": "这是测试内容",
    "categoryId": 1,
    "tagIds": [1]
  }'

# 应返回：创建成功的文章对象
```

### 5. 管理员功能测试

```bash
# 使用管理员账号登录
curl -X POST $API_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin123456"
  }'

# 测试创建分类（需要管理员权限）
curl -X POST $API_URL/api/metadata/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "技术",
    "description": "技术相关文章"
  }'
```

---

## ⚠️ 常见部署问题

### 问题1：构建超时或失败

**症状：** 构建过程中断，日志显示 Maven 依赖下载缓慢

**解决：**
1. 重新部署（很多时候网络波动导致）
2. 检查 `pom.xml` 是否有语法错误
3. Railway 通常重试即可成功

### 问题2：应用启动后立即崩溃

**症状：** 日志显示启动，但随即退出

**可能原因：**
- JWT_SECRET 未设置或过短
- 数据库连接失败
- 内存不足（OOM）

**解决：**
```bash
# 检查环境变量
echo $JWT_SECRET  # 应该有值且长度 > 64
echo $DATABASE_URL  # 应该是正确的 MySQL 连接串

# 检查日志中的错误信息
# Railway: View Logs
# Render: Logs 标签
```

### 问题3：数据库连接超时

**症状：** 日志显示 `Communications link failure`

**解决：**
1. 确认 MySQL 服务已启动（Railway 中查看服务状态）
2. 检查 `DATABASE_URL` 格式是否正确
3. Railway 中确保 Web 服务和 MySQL 在同一个 Project

### 问题4：接口 404

**症状：** 访问接口返回 404

**检查：**
1. 应用是否已启动成功（日志中看到 "Started BlogApplication"）
2. 使用正确的路径（`/api/...`）
3. HTTP 方法是否正确（GET/POST/PUT/DELETE）

### 问题5：CORS 跨域错误

**症状：** 前端访问提示跨域错误

**解决：** 需要在 `SecurityConfig` 中配置 CORS（如果还没配置）

---

## 💡 部署最佳实践

1. **先在本地验证**：确保 `railway` profile 可以正常运行
   ```bash
   export SPRING_PROFILES_ACTIVE=railway
   export DATABASE_URL=jdbc:mysql://localhost:3306/blog?useSSL=false
   export MYSQLUSER=root
   export MYSQLPASSWORD=root
   export JWT_SECRET=test-secret-key-at-least-64-chars-long-xxxxxxxxxxxxxxxxxxxxxxxxx
   mvn spring-boot:run
   ```

2. **使用 staging 环境**：创建 `dev` 分支，先部署到测试环境

3. **监控资源使用**：Railway Dashboard → Metrics 查看 CPU/内存

4. **保留部署日志**：出问题时方便排查

5. **设置告警**：可以接入 Sentry 等监控服务

---

## 📊 成本优化建议

### Railway 免费额度优化

**免费层：** $5/月 ≈ 500 小时运行时间

**优化策略：**
1. 使用 "Sleep on Idle" 功能（无流量时自动休眠）
2. 避免高频轮询接口
3. 监控每日使用量，避免超额

**升级时机：**
- 当免费额度不够用时
- 需要更高性能时
- Hobby Plan $5/月，包含 $5 使用额度

### Render 免费层注意事项

**限制：**
- 750 小时/月（约 31 天）
- 15 分钟无请求自动休眠
- 冷启动时间较长（首次请求需等待 30-60 秒）

**应对：**
- 使用定时任务保持活跃（例如每 10 分钟 ping 一次）
- 或升级到付费套餐（$7/月）

---

## ✅ 最终检查

部署完成后，确认：

- [ ] 应用可以正常访问（打开域名无 502/504）
- [ ] 注册/登录功能正常
- [ ] 管理员账号可以登录（admin/Admin123456）
- [ ] 数据库数据持久化（重启后数据不丢失）
- [ ] 日志无频繁错误
- [ ] 响应时间合理（< 2秒）
- [ ] 已修改管理员默认密码（生产环境必做）

---

**准备好了吗？开始部署吧！🚀**

有任何问题随时反馈。
