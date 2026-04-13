# 文件共享管理系统

一个企业级的文件安全分享平台，支持物理隔离存储、无状态认证、实时监控等功能。

## 系统架构

```
File Server System
├── Backend (Spring Boot 3)
│   ├── file-server-common     # 公共模块
│   ├── file-server-system     # 业务模块
│   └── file-server-app        # 启动模块
└── Frontend (uni-app)
    ├── pages                  # 页面
    ├── api                    # API 调用
    ├── stores                 # 数据存储
    └── utils                  # 工具函数
```

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **ORM**: MyBatis-Plus 3.5.4
- **安全**: Spring Security + JWT
- **缓存**: Redis
- **数据库**: MySQL 5.7+
- **系统监控**: OSHI 6.4.10
- **工具类**: HuTool 5.8.21

### 前端
- **框架**: uni-app (Vue 3 + TypeScript)
- **构建**: Vite 4.3.0
- **HTTP**: Axios 1.4.0
- **状态管理**: Pinia 2.1.0
- **时间处理**: dayjs 1.11.0

## 功能模块

### 1. 用户认证（Authentication）
- JWT 无状态认证
- 用户登录/注册
- Token 自动刷新
- CORS 跨域处理

### 2. 文件上传与校验（File Upload & Integrity）
- 分块上传（支持断点续传）
- SHA-256 哈希校验
- 秒传功能（同一文件只保存一份）
- 物理隔离存储（不映射为静态资源）
- 流式下载

### 3. 文件分享（File Sharing）
- 生成唯一的分享令牌（ShortUUID）
- 支持设置过期时间
- 支持下载次数限制
- 限流保护（防止滥用）
- 分享管理和撤销

### 4. 系统监控（System Monitor）
- CPU 使用率实时监控
- 内存占用情况
- 磁盘空间使用
- 系统平均负载
- 运行时间统计

### 5. 权限管理（Permission）
- 基于角色的访问控制 (RBAC)
- ADMIN（管理员）：完全权限
- USER（普通用户）：通过分享链接下载

### 6. 日志管理（Logging）
- 操作审计日志
- 错误异常记录
- 访问日志统计

## 快速开始

### 环境要求
- JDK 17+
- MySQL 5.7+
- Redis 5.0+
- Node.js 16+
- Maven 3.6+

### 后端部署

#### 1. 创建数据库
```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE file_server DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据库脚本
SOURCE /path/to/schema.sql;
```

#### 2. 修改配置文件
编辑 `file-server-app/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/file_server
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password

file:
  storage:
    path: /data/file-storage  # Linux路径
    # path: D:/file-storage   # Windows路径
```

#### 3. 编译和运行
```bash
# 编译
mvn clean package -DskipTests

# 运行应用
java -jar file-server-app/target/file-server-app-1.0.0.jar
```

应用将在 `http://localhost:8080` 启动。

### 前端部署

#### 1. 安装依赖
```bash
cd frontend/file-share-admin
npm install
```

#### 2. 开发模式运行
```bash
npm run dev
```

访问 `http://localhost:5173`

#### 3. 生产构建
```bash
npm run build
```

生成的文件位于 `dist/` 目录。

### 测试账号
- **用户名**: admin
- **密码**: admin123

## API 文档

### 认证接口

#### 登录
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

### 文件接口

#### 检查文件（秒传）
```
POST /api/files/check
Authorization: Bearer {token}
Content-Type: application/json

{
  "fileHash": "abc123...",
  "fileName": "document.pdf",
  "fileSize": 1024000
}
```

#### 上传文件
```
POST /api/files/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: <binary data>
```

#### 删除文件
```
DELETE /api/files/{fileId}
Authorization: Bearer {token}
```

### 分享接口

#### 创建分享
```
POST /api/shares
Authorization: Bearer {token}
Content-Type: application/json

{
  "fileId": 1,
  "expireTime": "2024-07-01 23:59:59",
  "maxDownloads": 10
}
```

#### 获取分享信息（公开）
```
GET /api/shares/info/{shareToken}
```

#### 下载分享文件（公开，流式传输）
```
GET /api/shares/download/{shareToken}
```

### 监控接口

#### 获取系统状态
```
GET /api/monitor/status
Authorization: Bearer {token}

Response:
{
  "code": 0,
  "data": {
    "cpuUsage": 45.5,
    "memoryUsagePercent": 62.3,
    "memoryUsed": 8192,
    "memoryTotal": 16384,
    "diskUsagePercent": 78.5,
    "diskUsed": 500,
    "diskTotal": 640,
    "loadAverage1m": 2.5,
    "loadAverage5m": 2.3,
    "loadAverage15m": 2.1,
    "uptime": 864000,
    "timestamp": 1719835200000
  }
}
```

## 安全性考虑

### 1. 请求认证
- 所有 API 请求（除登录和公开分享下载）都需要有效的 JWT Token
- Token 通过 `Authorization: Bearer {token}` 头传递

### 2. 文件隔离
- 上传的文件存储在物理隔离的目录中
- 文件目录不映射为静态资源，所有下载都经过控制器的流式传输
- 防止了目录遍历攻击

### 3. 密码安全
- 所有密码使用 BCrypt 算法加密存储
- 不支持明文密码查询

### 4. 分享链接安全
- 分享令牌使用 UUID 算法生成，不可预测
- 支持设置过期时间和下载次数限制
- Redis 缓存加速分享验证

### 5. CORS 保护
- 明确配置跨域策略
- 生产环境应限制允许的来源

### 6. 日志记录
- 所有关键操作都有审计日志
- 包括登录、文件上传/下载、分享等

## Nginx 配置示例

```nginx
upstream file_server {
    server localhost:8080;
}

server {
    listen 80;
    server_name your-domain.com;

    # 启用 HTTPS（推荐）
    # listen 443 ssl http2;
    # ssl_certificate /path/to/cert.crt;
    # ssl_certificate_key /path/to/private.key;

    # 上传文件大小限制
    client_max_body_size 2G;

    # 后端 API 代理
    location /api/ {
        proxy_pass http://file_server;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 大文件上传超时
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    # 前端应用代理
    location / {
        proxy_pass http://localhost:5173;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    # 启用 gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;
    gzip_vary on;
    gzip_min_length 1024;
}
```

## 性能优化建议

1. **数据库**
   - 启用 MySQL 查询缓存
   - 定期分析表和优化索引
   - 配置适当的连接池大小

2. **Redis**
   - 使用 Redis 集群以提高可靠性
   - 定期清理过期的分享令牌

3. **文件存储**
   - 使用 SSD 存储以提高 I/O 性能
   - 定期清理已删除的文件

4. **前端**
   - 启用资源压缩和 CDN 分发
   - 使用全量缓存策略

5. **应用层**
   - 启用 JVM 垃圾回收优化
   - 配置适当的线程池大小

## 故障排除

### 问题 1：数据库连接失败
- 检查 MySQL 是否正常运行
- 验证数据库配置（主机、端口、用户名、密码）
- 确认数据库创建成功

### 问题 2：文件上传失败
- 检查文件存储目录权限
- 确保磁盘空间充足
- 查看应用日志了解具体错误

### 问题 3：分享链接无法访问
- 确认分享链接未过期
- 检查下载次数是否超过限制
- 验证 Redis 连接状态

### 问题 4：前端无法连接后端
- 检查后端服务是否启动
- 验证 API 代理配置
- 查看浏览器控制台的错误消息

## 项目结构说明

```
file-server/
├── backend/
│   └── file-server/
│       ├── file-server-common/        # 公共模块
│       │   └── src/main/java/com/oss/file/common/
│       │       ├── response/          # 统一响应体
│       │       ├── exception/         # 异常处理
│       │       └── util/              # 工具类
│       ├── file-server-system/        # 业务模块
│       │   └── src/main/java/com/oss/file/system/
│       │       ├── entity/            # 数据库实体
│       │       ├── mapper/            # MyBatis Mapper
│       │       ├── service/           # 业务接口和实现
│       │       └── dto/               # 数据传输对象
│       └── file-server-app/           # 启动模块
│           ├── src/main/java/com/oss/file/app/
│           │   ├── config/            # 配置类
│           │   ├── controller/        # 控制器
│           │   ├── exception/         # 异常处理
│           │   ├── security/          # 安全相关
│           │   └── FileServerApplication.java
│           └── src/main/resources/
│               └── application.yml    # 配置文件
├── frontend/
│   └── file-share-admin/
│       ├── src/
│       │   ├── pages/                 # 页面组件
│       │   ├── api/                   # API 调用
│       │   ├── stores/                # Pinia 存储
│       │   ├── utils/                 # 工具函数
│       │   ├── App.vue
│       │   ├── main.ts
│       │   └── uni.config.ts
│       ├── package.json
│       ├── vite.config.ts
│       └── tsconfig.json
└── docs/
    └── schema.sql                     # 数据库脚本
```

## 许可证

MIT License

## 联系方式

For questions or support, please contact: support@example.com

---

**最后更新**: 2024年7月
