# 🚀 Railway 部署快速参考

## 一、推送代码到 GitHub

```bash
# 1. 初始化 Git（如果还没有）
git init
git add .
git commit -m "Ready for Railway deployment"

# 2. 关联远程仓库
git remote add origin https://github.com/你的用户名/仓库名.git

# 3. 推送
git branch -M main
git push -u origin main
```

---

## 二、Railway 部署步骤

### 1️⃣ 创建项目
- 访问：https://railway.app
- 点击：**New Project** → **Deploy from GitHub repo**
- 选择你的仓库

### 2️⃣ 添加数据库
- 点击：**+ New** → **Database** → **Add MySQL**
- Railway 会自动注入环境变量

### 3️⃣ 配置环境变量
在 **Variables** 标签添加：

```bash
# 必须设置
SPRING_PROFILES_ACTIVE=railway
JWT_SECRET=<生成的64位密钥>

# Railway 自动提供（无需手动配置）
DATABASE_URL
MYSQLUSER
MYSQLPASSWORD
MYSQLDATABASE
```

### 4️⃣ 获取域名
- **Settings** → **Generate Domain**
- 得到：`https://xxx.railway.app`

---

## 三、生成 JWT_SECRET

### Windows PowerShell
```powershell
$bytes = New-Object byte[] 64
(New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### Linux/Mac
```bash
openssl rand -base64 64
```

### 在线生成
https://generate-random.org/api-token-generator?count=1&length=64

---

## 四、验证部署

```bash
# 设置域名变量
$API="https://你的域名.railway.app"

# 1. 测试基础接口
curl $API/api/metadata/categories

# 2. 注册用户
curl -X POST $API/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"username":"test","email":"test@example.com","password":"Test123456"}'

# 3. 登录
curl -X POST $API/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"test","password":"Test123456"}'
```

---

## 五、默认管理员账号

```
用户名：admin
密码：Admin123456
```

⚠️ **部署后立即修改密码！**

---

## 六、常见问题速查

| 问题 | 可能原因 | 解决方法 |
|------|---------|---------|
| 构建失败 | 网络波动 | 点击 **Redeploy** 重试 |
| 启动崩溃 | JWT_SECRET 未设置 | 检查环境变量 |
| 数据库连接失败 | MySQL 服务未启动 | 检查 MySQL 服务状态 |
| 接口 404 | 应用未启动成功 | 查看 **Logs** 确认启动 |
| OOM 错误 | 内存不足 | 已配置 `-Xmx400m` 限制内存 |

---

## 七、监控与日志

### Railway Dashboard
- **Deployments**：查看构建历史
- **Logs**：实时日志输出
- **Metrics**：CPU/内存使用情况
- **Usage**：免费额度使用情况

### 关键日志标志
✅ 成功启动：`Started BlogApplication`
❌ 启动失败：查找 `Exception` 或 `Error`

---

## 八、免费额度

| 项目 | 免费额度 | 说明 |
|------|---------|------|
| 运行时间 | 500 小时/月 | 约 $5 |
| 内存 | 8GB | 单服务最大 |
| 存储 | 100GB | 包含数据库 |
| 带宽 | 100GB/月 | 出站流量 |

💡 **优化建议**：
- 启用 "Sleep on Idle"（无流量自动休眠）
- 监控每日使用量
- 超出后升级 Hobby Plan ($5/月)

---

## 九、自动部署

配置完成后，每次推送代码到 `main` 分支：
```bash
git add .
git commit -m "Update feature"
git push
```

Railway 会自动：
1. 检测到新提交
2. 触发构建
3. 部署新版本
4. 零停机切换

---

## 十、项目文件清单

✅ 必需文件：
- `pom.xml` - Maven 配置
- `src/main/resources/application.yml` - 应用配置
- `.gitignore` - Git 忽略规则
- `nixpacks.toml` - Railway 构建配置
- `railway.json` - Railway 部署配置

📄 文档文件：
- `DEPLOYMENT.md` - 详细部署指南
- `DEPLOYMENT_CHECKLIST.md` - 部署检查清单
- `QUICK_REFERENCE.md` - 本文件
- `deploy-check.sh` / `deploy-check.ps1` - 自动检查脚本

---

## 十一、紧急操作

### 回滚到上一个版本
1. Railway Dashboard → **Deployments**
2. 找到上一个成功的部署
3. 点击 **⋮** → **Redeploy**

### 重启服务
1. Railway Dashboard → 点击服务
2. **Settings** → **Restart**

### 查看实时日志
```bash
# 安装 Railway CLI
npm i -g @railway/cli

# 登录
railway login

# 查看日志
railway logs
```

### 暂停服务（节省额度）
1. Railway Dashboard → 服务
2. **Settings** → **Sleep Mode** → 启用

---

## 十二、生产环境建议

### 安全配置
- ✅ 使用强 JWT_SECRET（64+ 字符）
- ✅ 修改管理员默认密码
- ✅ 禁用 H2 Console（已配置）
- ✅ 关闭 SQL 日志（已配置）
- ✅ 隐藏堆栈信息（已配置）

### 性能优化
- ✅ JVM 内存限制（`-Xmx400m`）
- ✅ 数据库连接池配置
- ⚠️ 考虑添加 Redis 缓存
- ⚠️ 启用 GZIP 压缩

### 监控告警
- 接入 Sentry（错误监控）
- 配置健康检查端点
- 设置资源使用告警

---

## 十三、技术支持

### Railway 官方
- 文档：https://docs.railway.app
- 社区：https://discord.gg/railway
- 状态：https://status.railway.app

### 本项目
- GitHub Issues：提交问题反馈
- CLAUDE.md：查看项目规范
- DEPLOYMENT.md：详细部署指南

---

## 十四、下一步

部署成功后：

1. ✅ 测试所有接口功能
2. ✅ 修改管理员密码
3. ✅ 配置前端 API 地址
4. ✅ 设置自定义域名（可选）
5. ✅ 备份数据库（重要）
6. ✅ 添加监控告警
7. ✅ 编写 API 文档

---

**🎉 恭喜！你的博客系统已经上线了！**

记得把这个文件加入收藏，方便随时查阅。

---

**最后更新：** 2026-08-18
