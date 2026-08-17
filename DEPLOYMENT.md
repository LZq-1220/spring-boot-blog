# Railway 部署指南

## 前置准备

1. **注册账号**
   - 访问 [Railway.app](https://railway.app)
   - 使用 GitHub 账号登录（推荐）

2. **准备 GitHub 仓库**
   - 注册 [GitHub](https://github.com) 账号（如果还没有）
   - 创建一个新的仓库（可以是私有或公开）

## 部署步骤

### 第一步：将项目推送到 GitHub

在项目根目录执行：

```bash
# 初始化 Git 仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "Initial commit for Railway deployment"

# 关联远程仓库（替换 YOUR_USERNAME 和 YOUR_REPO）
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git

# 推送到 GitHub
git branch -M main
git push -u origin main
```

### 第二步：在 Railway 创建项目

1. 登录 [Railway Dashboard](https://railway.app/dashboard)
2. 点击 **"New Project"**
3. 选择 **"Deploy from GitHub repo"**
4. 授权 Railway 访问你的 GitHub
5. 选择刚才推送的仓库

### 第三步：添加 MySQL 数据库

1. 在项目页面点击 **"+ New"**
2. 选择 **"Database"** → **"Add MySQL"**
3. Railway 会自动创建一个 MySQL 实例并注入环境变量

### 第四步：配置环境变量

在项目的 **Variables** 标签页添加：

```
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-min-512-bits
```

**生成安全的 JWT_SECRET（推荐）：**
- 访问 [随机字符串生成器](https://www.random.org/strings/?num=1&len=64&digits=on&upperalpha=on&loweralpha=on&unique=on&format=plain)
- 或者在本地执行：`openssl rand -base64 64`

### 第五步：部署

1. Railway 会自动检测到配置文件并开始部署
2. 等待构建完成（约 3-5 分钟）
3. 部署成功后，点击 **"Settings"** → **"Generate Domain"** 获取访问地址

### 第六步：验证部署

访问生成的域名（例如：`https://your-app.railway.app`），测试接口：

```bash
# 测试健康检查
curl https://your-app.railway.app/api/metadata/categories

# 注册测试账号
curl -X POST https://your-app.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

## 前端配置

修改 `frontend/js/api.js` 中的 API 地址：

```javascript
// 本地开发
// const API_BASE_URL = 'http://localhost:8080';

// Railway 生产环境
const API_BASE_URL = 'https://your-app.railway.app';
```

## 常见问题

### 1. 构建失败

**检查 Railway 日志：**
- 点击 **Deployments** 查看构建日志
- 常见原因：Maven 依赖下载失败、内存不足

**解决方案：**
- 重新部署（点击 **Redeploy**）
- 检查 `pom.xml` 配置是否正确

### 2. 数据库连接失败

**检查环境变量：**
- 确认 MySQL 服务已添加并运行
- Railway 会自动注入这些变量：
  - `DATABASE_URL`
  - `MYSQLUSER`
  - `MYSQLPASSWORD`
  - `MYSQLHOST`
  - `MYSQLPORT`
  - `MYSQLDATABASE`

### 3. 应用启动失败

**查看运行日志：**
- 点击 **View Logs**
- 查找错误信息

**常见原因：**
- 端口配置错误（已自动配置 `$PORT`）
- JWT_SECRET 未设置
- 数据库未就绪

### 4. 访问 404

**确认：**
- 应用已成功启动（日志中看到 "Started BlogApplication"）
- 域名已生成并绑定
- 使用正确的 API 路径（`/api/...`）

## 免费额度说明

Railway 免费计划：
- **$5** 免费额度/月
- **500 小时** 运行时间
- **100GB** 出站流量
- **1GB** 内存

**省钱技巧：**
- 使用睡眠模式（无活动时自动休眠）
- 监控使用量：Dashboard → Usage

## 下一步优化

部署成功后可以考虑：

1. **绑定自定义域名**
   - Railway 支持绑定自己的域名
   - Settings → Custom Domain

2. **设置环境隔离**
   - 创建 `dev` 和 `prod` 分支
   - 分别部署到不同环境

3. **添加监控**
   - Railway 自带基础监控
   - 可接入 Sentry 等第三方服务

4. **配置 CI/CD**
   - 推送代码自动部署
   - 添加自动测试

5. **迁移到服务器**
   - 当访问量增大时
   - 考虑迁移到云服务器获得更好性能

## 故障排查流程

1. **查看 Deployment 状态**：是否构建成功？
2. **查看 Runtime 日志**：应用是否正常启动？
3. **测试数据库连接**：MySQL 服务是否正常？
4. **检查环境变量**：JWT_SECRET 等是否正确配置？
5. **验证网络**：域名是否可访问？

## 需要帮助？

- Railway 文档：https://docs.railway.app
- 本项目问题：查看 GitHub Issues
- 联系项目维护者

---

**祝部署顺利！🚀**
