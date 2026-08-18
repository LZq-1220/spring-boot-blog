# 部署指南（Railway）

本文件是唯一的部署文档，整合了部署步骤、Railway 常见坑、验证方法和故障排查。

## 前置准备

1. 注册 [Railway](https://railway.app)（用 GitHub 账号登录即可）。
2. 准备好一个 GitHub 仓库（私有即可，部署无需公开）。

## 部署步骤

### 1. 推送代码到 GitHub

```bash
git init
git add .
git commit -m "Ready for deployment"
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git branch -M main
git push -u origin main
```

### 2. 在 Railway 创建项目

1. 登录 Railway Dashboard → **New Project** → **Deploy from GitHub repo**。
2. 授权 Railway 访问 GitHub，选择上面的仓库。

### 3. 添加 MySQL 数据库

点击 **+ New** → **Database** → **Add MySQL**。Railway 会自动创建实例并注入环境变量。

### 4. 配置环境变量（关键）

在项目的 **Variables** 标签添加：

```
SPRING_PROFILES_ACTIVE=railway
JWT_SECRET=<生成的64位密钥>
```

> ⚠️ **Railway MySQL 变量命名坑**：插件注入的变量名**不带下划线**（`MYSQLHOST`/`MYSQLPORT`/`MYSQLDATABASE`/`MYSQLUSER`/`MYSQLPASSWORD`），而本项目 `application.yml` 用的是带下划线的 `MYSQL_HOST` 等。需要手动添加 5 个引用变量：

```
MYSQL_HOST     = ${{MySQL.MYSQLHOST}}
MYSQL_PORT     = ${{MySQL.MYSQLPORT}}
MYSQL_DATABASE = ${{MySQL.MYSQLDATABASE}}
MYSQL_USER     = ${{MySQL.MYSQLUSER}}
MYSQL_PASSWORD = ${{MySQL.MYSQLPASSWORD}}
```

生成 JWT_SECRET：

```bash
# Linux / macOS / Git Bash
openssl rand -base64 64

# Windows PowerShell
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
```

### 5. 部署与域名

1. Railway 检测到仓库后自动构建（约 3–5 分钟）。
2. **Settings** → **Generate Domain** 获取访问地址。
3. 前端已打进 jar（`src/main/resources/static/`），同域访问，无需额外配置 CORS 或前端地址。

## 验证部署

```bash
API="https://你的域名.up.railway.app"

# 基础接口
curl "$API/api/categories"

# 注册
curl -X POST "$API/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"Test123456"}'

# 登录
curl -X POST "$API/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123456"}'
```

成功标志：日志里出现 `Started BlogApplication`，服务状态绿色 Online。

## Railway 关键点（踩过的坑）

1. **用 Dockerfile，不要用 railway.json 的 startCommand 执行 shell 命令**——它不会展开 `$(ls ...)` 之类的 shell 语法。
2. **Railway 只负责注入环境变量**，不会替换配置文件内容；`${...}` 占位符由 Spring Boot 自己解析。
3. **变量名必须带下划线**，与 Spring Boot 宽松绑定规则一致（见上文变量配置）。
4. **前端必须放在 `src/main/resources/static/`** 才能被打进 jar；Spring Security 已放开 `/*.html`、`/css/**`、`/js/**` 的访问。
5. Dockerfile 使用 `COPY --from=build /app/target/*.jar app.jar` 通配符匹配，避免文件名硬编码。

## 常见问题

| 问题 | 可能原因 | 解决方法 |
|------|---------|---------|
| 构建失败 | Maven 依赖下载失败 / 网络波动 | 点击 **Redeploy** 重试 |
| 启动崩溃 / 502 | JWT_SECRET 未设置或数据库未就绪 | 检查环境变量、MySQL 服务状态 |
| 数据库连接失败 | 变量名没带下划线 | 按上文手动添加 `MYSQL_HOST` 等引用变量 |
| 页面/接口 404 | 应用未启动成功 | 查看 **Logs**，确认 `Started BlogApplication` |
| OOM | 内存不足 | 已配置 `-Xmx400m`，必要时调大 |

## 日常运维

- **自动部署**：推送到 `main` 分支后 Railway 自动构建部署。
- **回滚**：Deployments → 上一个成功部署 → **⋮** → **Redeploy**。
- **重启**：Settings → **Restart**。
- **看日志**：Deployments → **View Logs**，或 `railway logs`（需 `npm i -g @railway/cli`）。
- **省钱**：开启 Sleep Mode（无流量自动休眠），关注 Dashboard → Usage。

## 生产环境建议

- 使用强 JWT_SECRET（64+ 字符），不要用仓库里的开发默认值。
- 生产 profile（railway）已关闭 H2 Console 和 SQL 日志、隐藏堆栈信息。
- 定期备份数据库。

## 参考

- Railway 文档：https://docs.railway.app
- Railway 状态：https://status.railway.app
