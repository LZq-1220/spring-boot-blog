#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "========================================="
echo "  个人博客系统 - 部署准备检查工具"
echo "========================================="
echo ""

# 检查函数
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 已安装"
        return 0
    else
        echo -e "${RED}✗${NC} $1 未安装"
        return 1
    fi
}

# 1. 检查必需工具
echo "1. 检查必需工具..."
check_command git || exit 1
check_command mvn || exit 1
check_command java || exit 1
echo ""

# 2. 检查 Java 版本
echo "2. 检查 Java 版本..."
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ge 17 ]; then
    echo -e "${GREEN}✓${NC} Java 版本: $JAVA_VERSION (满足要求 >= 17)"
else
    echo -e "${RED}✗${NC} Java 版本过低: $JAVA_VERSION (需要 >= 17)"
    exit 1
fi
echo ""

# 3. 检查配置文件
echo "3. 检查配置文件..."
files=(
    "src/main/resources/application.yml"
    "pom.xml"
    ".gitignore"
    "nixpacks.toml"
    "railway.json"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $file 存在"
    else
        echo -e "${RED}✗${NC} $file 不存在"
        exit 1
    fi
done
echo ""

# 4. 检查 railway profile 配置
echo "4. 检查 railway profile..."
if grep -q "on-profile: railway" src/main/resources/application.yml; then
    echo -e "${GREEN}✓${NC} railway profile 已配置"
else
    echo -e "${YELLOW}⚠${NC} 未找到 railway profile"
fi
echo ""

# 5. 测试编译
echo "5. 测试项目编译..."
echo "   执行: mvn clean package -DskipTests"
if mvn clean package -DskipTests > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} 项目编译成功"
else
    echo -e "${RED}✗${NC} 项目编译失败，请检查代码"
    exit 1
fi
echo ""

# 6. 检查 Git 状态
echo "6. 检查 Git 状态..."
if [ -d ".git" ]; then
    echo -e "${GREEN}✓${NC} Git 仓库已初始化"

    # 检查是否有未提交的改动
    if [ -n "$(git status --porcelain)" ]; then
        echo -e "${YELLOW}⚠${NC} 有未提交的改动："
        git status --short
        echo ""
        read -p "是否提交这些改动？(y/n): " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            read -p "请输入提交信息: " commit_msg
            git add .
            git commit -m "$commit_msg"
            echo -e "${GREEN}✓${NC} 已提交改动"
        fi
    else
        echo -e "${GREEN}✓${NC} 工作目录干净，无未提交改动"
    fi

    # 检查远程仓库
    if git remote -v | grep -q "origin"; then
        echo -e "${GREEN}✓${NC} 远程仓库已配置"
        git remote -v
    else
        echo -e "${YELLOW}⚠${NC} 未配置远程仓库"
        echo ""
        read -p "请输入 GitHub 仓库地址 (如: https://github.com/username/repo.git): " repo_url
        git remote add origin "$repo_url"
        echo -e "${GREEN}✓${NC} 已添加远程仓库: $repo_url"
    fi
else
    echo -e "${YELLOW}⚠${NC} Git 仓库未初始化"
    read -p "是否初始化 Git 仓库？(y/n): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git init
        git add .
        git commit -m "Initial commit for deployment"
        echo -e "${GREEN}✓${NC} Git 仓库初始化完成"

        read -p "请输入 GitHub 仓库地址: " repo_url
        git remote add origin "$repo_url"
        echo -e "${GREEN}✓${NC} 已添加远程仓库"
    fi
fi
echo ""

# 7. JWT Secret 检查
echo "7. JWT Secret 安全检查..."
echo -e "${YELLOW}⚠${NC} 请确保在 Railway/Render 中设置了安全的 JWT_SECRET"
echo "   生成命令: openssl rand -base64 64"
echo ""

# 8. 推送提示
echo "========================================="
echo "  准备工作完成！"
echo "========================================="
echo ""
echo "下一步操作："
echo ""
echo "1. 推送代码到 GitHub："
echo "   ${GREEN}git push -u origin main${NC}"
echo ""
echo "2. 部署到 Railway："
echo "   a. 访问 https://railway.app"
echo "   b. New Project → Deploy from GitHub repo"
echo "   c. 选择你的仓库"
echo "   d. 添加 MySQL 数据库 (+ New → Database → MySQL)"
echo "   e. 设置环境变量:"
echo "      - SPRING_PROFILES_ACTIVE=railway"
echo "      - JWT_SECRET=<your-secret-key>"
echo "   f. 等待部署完成"
echo "   g. Generate Domain 获取访问地址"
echo ""
echo "3. 验证部署："
echo "   ${GREEN}curl https://your-app.railway.app/api/metadata/categories${NC}"
echo ""

read -p "是否现在推送到 GitHub？(y/n): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "推送到 GitHub..."
    git push -u origin main
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} 推送成功！"
        echo ""
        echo "现在可以去 Railway 部署了："
        echo "https://railway.app/new"
    else
        echo -e "${RED}✗${NC} 推送失败，请检查："
        echo "   1. GitHub 仓库是否已创建"
        echo "   2. 是否有权限推送"
        echo "   3. 网络连接是否正常"
    fi
else
    echo "稍后可以手动推送: ${GREEN}git push -u origin main${NC}"
fi

echo ""
echo "========================================="
echo "  祝部署顺利！🚀"
echo "========================================="
