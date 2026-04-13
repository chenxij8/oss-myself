#!/bin/bash

# 文件共享系统部署脚本
# 用途: 自动化构建和部署应用

set -e

echo "================================"
echo "文件共享管理系统 - 部署脚本"
echo "================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend/file-server"
FRONTEND_DIR="$PROJECT_ROOT/frontend/file-share-admin"

# 检查环境要求
check_requirements() {
    echo -e "\n${YELLOW}[1/5] 检查环境要求...${NC}"
    
    # 检查 Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}错误: 未找到 Java 环境${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Java 版本: $(java -version 2>&1 | grep version)${NC}"
    
    # 检查 Maven
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}错误: 未找到 Maven${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Maven 已安装${NC}"
    
    # 检查 Node.js
    if ! command -v node &> /dev/null; then
        echo -e "${RED}错误: 未找到 Node.js${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Node.js 版本: $(node -v)${NC}"
    
    # 检查 npm
    if ! command -v npm &> /dev/null; then
        echo -e "${RED}错误: 未找到 npm${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ npm 版本: $(npm -v)${NC}"
}

# 编译后端
build_backend() {
    echo -e "\n${YELLOW}[2/5] 编译后端...${NC}"
    
    cd "$BACKEND_DIR"
    
    echo "清理旧的构建..."
    mvn clean > /dev/null
    
    echo "开始编译..."
    mvn package -DskipTests
    
    if [ -f "file-server-app/target/file-server-app-1.0.0.jar" ]; then
        echo -e "${GREEN}✓ 后端编译完成${NC}"
    else
        echo -e "${RED}错误: 后端编译失败${NC}"
        exit 1
    fi
}

# 编译前端
build_frontend() {
    echo -e "\n${YELLOW}[3/5] 编译前端...${NC}"
    
    cd "$FRONTEND_DIR"
    
    if [ ! -d "node_modules" ]; then
        echo "安装依赖..."
        npm install
    fi
    
    echo "开始编译..."
    npm run build
    
    if [ -d "dist" ]; then
        echo -e "${GREEN}✓ 前端编译完成${NC}"
    else
        echo -e "${RED}错误: 前端编译失败${NC}"
        exit 1
    fi
}

# 生成部署包
create_deployment_package() {
    echo -e "\n${YELLOW}[4/5] 生成部署包...${NC}"
    
    DEPLOY_DIR="$PROJECT_ROOT/deploy"
    mkdir -p "$DEPLOY_DIR"
    
    # 复制后端 JAR
    cp "$BACKEND_DIR/file-server-app/target/file-server-app-1.0.0.jar" "$DEPLOY_DIR/"
    
    # 复制前端文件
    cp -r "$FRONTEND_DIR/dist" "$DEPLOY_DIR/web"
    
    # 复制配置文件
    cp "$BACKEND_DIR/file-server-app/src/main/resources/application.yml" "$DEPLOY_DIR/"
    
    # 生成启动脚本
    cat > "$DEPLOY_DIR/start.sh" << 'EOL'
#!/bin/bash
# 启动后端服务
java -Xmx512m -Xms256m -jar file-server-app-1.0.0.jar &
echo "后端服务已启动"

# 启动前端（需要 nginx 或其他 web 服务器）
# 可以使用 nginx 或 python -m http.server 等
EOL
    
    chmod +x "$DEPLOY_DIR/start.sh"
    
    echo -e "${GREEN}✓ 部署包已生成到: $DEPLOY_DIR${NC}"
}

# 生成摘要
print_summary() {
    echo -e "\n${YELLOW}[5/5] 部署摘要${NC}"
    
    echo -e "\n${GREEN}构建成功！${NC}"
    echo ""
    echo "后端 JAR: $BACKEND_DIR/file-server-app/target/file-server-app-1.0.0.jar"
    echo "前端文件: $FRONTEND_DIR/dist"
    echo "部署目录: $PROJECT_ROOT/deploy"
    echo ""
    echo "后续步骤:"
    echo "1. 确保 MySQL 和 Redis 服务正常运行"
    echo "2. 修改 application.yml 中的数据库配置"
    echo "3. 运行部署脚本启动应用"
    echo "4. 访问 http://localhost:8080"
    echo ""
    echo "默认测试账号: admin / admin123"
}

# 主流程
main() {
    # 验证项目结构
    if [ ! -d "$BACKEND_DIR" ] || [ ! -d "$FRONTEND_DIR" ]; then
        echo -e "${RED}错误: 项目结构异常${NC}"
        exit 1
    fi
    
    # 执行各个步骤
    check_requirements
    build_backend
    build_frontend
    create_deployment_package
    print_summary
}

# 运行主流程
main

echo ""
echo -e "${GREEN}部署完成！${NC}"
