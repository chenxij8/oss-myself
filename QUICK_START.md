# 快速参考指南 (Quick Reference)

## 🚀 快速启动 (5分钟)

### 前置准备
```bash
# 1. 确保已安装
- Java 17+ (java -version)
- MySQL 5.7+ (mysql --version)
- Redis 5.0+ (redis-cli ping)
- Node.js 16+ (node -v)
- Maven 3.6+ (mvn -version)

# 2. 初始化数据库
mysql -u root -p < docs/schema.sql
```

### 启动应用

#### 方式1: 命令行启动 (快速)
```bash
# 后端 (终端1)
cd backend/file-server
mvn clean package -DskipTests
java -jar file-server-app/target/file-server-app-1.0.0.jar

# 前端 (终端2)
cd frontend/file-share-admin
npm install
npm run dev

# 访问应用
# 后端: http://localhost:8080
# 前端: http://localhost:5173
```

#### 方式2: 使用部署脚本 (推荐)
```bash
chmod +x deploy.sh
./deploy.sh

# 脚本会自动
# 1. 检查环境
# 2. 编译后端
# 3. 编译前端
# 4. 生成部署包
```

## 🔐 默认登录凭证

```
用户名: admin
密码: admin123
```

## 📊 关键配置文件修改

### 数据库配置
**文件**: `backend/file-server/file-server-app/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/file_server
    username: root
    password: your_password  # ← 改这里
```

### 文件存储路径
```yaml
file:
  storage:
    path: D:/file-storage  # Windows
    # path: /data/file-storage  # Linux
```

### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:  # 如果Redis需要密码，改这里
    database: 0
```

## 🔗 主要API端点

| 功能 | HTTP方法 | 端点 | 需要认证 |
|------|---------|------|---------|
| 登录 | POST | `/api/auth/login` | ❌ |
| 获取监控数据 | GET | `/api/monitor/status` | ✅ |
| 检查文件 | POST | `/api/files/check` | ✅ |
| 上传文件 | POST | `/api/files/upload` | ✅ |
| 获取文件信息 | GET | `/api/files/{id}` | ✅ |
| 删除文件 | DELETE | `/api/files/{id}` | ✅ |
| 创建分享 | POST | `/api/shares` | ✅ |
| 获取分享信息 | GET | `/api/shares/info/{token}` | ❌ |
| 分享下载 | GET | `/api/shares/download/{token}` | ❌ |
| 删除分享 | DELETE | `/api/shares/{id}` | ✅ |

## 📝 常见操作

### 编译后端
```bash
cd backend/file-server
mvn clean package -DskipTests      # 快速编译
mvn clean package                  # 包含测试
mvn clean compile                  # 仅编译
```

### 构建前端
```bash
cd frontend/file-share-admin
npm run dev      # 开发模式
npm run build    # 生产构建
npm run preview  # 预览构建结果
```

### 查看日志
```bash
# 后端日志（实时）
tail -f logs/file-server.log

# Spring Boot运行时日志
# 查看terminal输出，日志格式:
# [时间] [线程] [日志级别] [类名] - [消息]
```

### 重启应用
```bash
# 停止后端
Ctrl+C  (在运行时的terminal中)

# 或杀死进程
ps aux | grep java
kill -9 <PID>

# 重新启动
java -jar file-server-app-1.0.0.jar
```

## 🆘 常见问题快速解决

| 问题 | 原因 | 解决方法 |
|------|------|---------|
| 8080端口被占用 | 另一个应用占用 | `netstat -ano \| grep 8080` 找出PID后kill |
| 数据库连接失败 | MySQL未启动/配置错误 | 检查MySQL服务 + 修改配置文件 |
| Redis连接失败 | Redis未启动 | `redis-server` 启动Redis服务 |
| 前端API请求403 | Token过期或无效 | 重新登录获取新Token |
| 文件上传失败 | 磁盘空间不足 | 检查file.storage.path目录权限和空间 |
| 分享链接无法下载 | 分享已过期/超过下载限制 | 重新创建分享链接 |

## 📱 跨平台支持

### uni-app 编译目标

```bash
# H5 网页
npm run dev:h5

# 微信小程序
npm run dev:mp-weixin

# 支付宝小程序
npm run dev:mp-alipay

# iOS App
npm run build:app-ios

# Android App
npm run build:app-android
```

## 🔒 安全性检查清单

在部署到生产环境前:

- [ ] 修改数据库密码（不要用root密码）
- [ ] 修改Redis密码
- [ ] 修改JWT secret密钥
- [ ] 配置HTTPS/SSL证书
- [ ] 限制文件存储目录权限（chmod 755）
- [ ] 配置防火墙规则
- [ ] 启用MySQL binlog备份
- [ ] 配置Redis持久化 (RDB或AOF)
- [ ] 审计日志定期输出
- [ ] 设置自动备份任务

## 📈 性能调优参数

### JVM参数优化
```bash
# 运行命令中添加
java -Xmx1024m -Xms512m -XX:+UseG1GC -jar file-server-app-1.0.0.jar
```

参数说明:
- `-Xmx1024m` - 最大堆内存
- `-Xms512m` - 初始堆内存
- `-XX:+UseG1GC` - 使用G1垃圾回收

### 数据库连接池
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 最大连接数
      minimum-idle: 5            # 最小空闲连接
      connection-timeout: 30000  # 连接超时(ms)
      idle-timeout: 600000       # 空闲超时(ms)
      max-lifetime: 1800000      # 最大生命周期(ms)
```

## 🔧 开发相关

### IDE配置 (IntelliJ IDEA)
```
1. File → Settings → Editor → Code Style → Java
2. 导入代码规范 (可选)
3. Plugins: Lombok插件启用
4. Build Tools → Maven → Runner
   ⚠️ Skip tests 打勾
```

### 前端开发调试
```javascript
// main.ts 中启用源映射
// 生产构建时会自动禁用
```

### 热更新配置
```yaml
# application.yml
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
```

## 📊 监控数据查询

### 获取系统实时数据
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/monitor/status

# 返回JSON包含
# - cpuUsage: CPU百分比
# - memoryUsagePercent: 内存百分比
# - diskUsagePercent: 磁盘百分比
# - loadAverage1m/5m/15m: 平均负载
```

## 🌐 Nginx配置速查

### 反向代理基础配置
```nginx
upstream backend {
    server localhost:8080;
}

server {
    listen 80;
    server_name example.com;
    
    # 上传大小限制
    client_max_body_size 2G;
    
    # 后端API
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # 前端应用
    location / {
        root /var/www/web;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

## 📚 相关资源

- Spring Boot文档: https://spring.io/projects/spring-boot
- uni-app文档: https://uniapp.dcloud.io/
- MyBatis-Plus: https://baomidou.com/
- OSHI库: https://github.com/oshi/oshi

## 📞 获取帮助

1. **查看日志**: 大多数问题都能在日志中找到线索
2. **查看文档**: 详见 README.md 和 PROJECT_SUMMARY.md
3. **API文档**: 查看 README.md 中的API部分
4. **代码注释**: 所有核心代码都有中文注释

---

**最后更新**: 2024年7月
**快速参考版**: 1.0
