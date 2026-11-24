# PC 客户端 / 图床 / 权限 功能说明

## 一、三端架构

| 端 | 目录 | 技术栈 | 说明 |
|---|---|---|---|
| PC 客户端 | `pc-frontend/` | Vue3 + Vite + TypeScript + Vue Router + Pinia | 本次新增，桌面浏览器使用 |
| 移动端 | `file-frontend/` | uni-app (Vue3 + TS) | H5 / 小程序 / App |
| 后端 | `file-server/` | Spring Boot 3 多模块 | 三端共用 |

三端功能对齐：登录/注册、文件托管（上传/秒传/下载/分享/删除）、图床、系统监控、用户管理。

## 二、PC 客户端运行

```bash
cd pc-frontend
npm install
npm run dev        # 开发，默认 http://localhost:5174
npm run build      # 类型检查 + 生产构建，产物 dist/
```

- 开发代理：`/api` → `http://localhost:8080`（在 `vite.config.ts` 修改）。
- 生产：设置 `VITE_API_BASE_URL` 指向后端完整地址后构建。
- 路由采用 hash 模式，可直接静态托管 `dist/`。

## 三、图床（图片/视频独立存储）

**与文件托管物理分离**：文件托管默认 `C:/IT`，图床默认 `C:/IT-Gallery`，由不同配置项与表（`file_info` / `gallery_asset`）管理，互不影响。

配置项（`application.yml`）：

```yaml
gallery:
  storage:
    path: ${GALLERY_STORAGE_PATH:C:/IT-Gallery}
```

API：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/gallery/upload` | 上传图片/视频，需登录 |
| GET | `/api/gallery?page=1&size=50&mediaType=IMAGE` | 分页列表，需登录 |
| GET | `/api/gallery/stats` | 素材统计，需登录 |
| GET | `/api/gallery/preview/{id}` | 内联预览（图片/视频），**公开**，支持 Range 拖动 |
| GET | `/api/gallery/raw/{id}` | 原始字节流，公开，支持 Range |
| DELETE | `/api/gallery/{id}` | 删除（校验归属），需登录 |

安全设计：
- 服务端按文件魔数嗅探真实类型，与扩展名交叉校验，仅放行图片/视频白名单，防止脚本伪装上传。
- 在线预览走 `206 Partial Content`，浏览器视频可拖动进度；预览接口公开以支持外链 `<img>/<video>` 直接引用，仅返回文件字节，不含任何用户信息。
- 秒传去重按「用户 + SHA-256」，删除后重传复用原记录。

建表：执行 `docs/schema.sql` 中的 `gallery_asset` 表（已有库只需执行新增段）。

## 四、注册与权限划分

- 注册接口 `POST /api/auth/register`：用户名、密码、邮箱；**角色由服务端强制为 USER**，不接受客户端传入，杜绝提权。
- 注册成功后自动登录并进入用户端。

| 角色 | 可见功能 |
|---|---|
| USER（用户端） | 文件管理、图床、我的分享 |
| ADMIN（管理端） | 上述全部 + 系统监控、用户管理 |

前后端双重校验：
- 后端：`/api/admin/**` 在 SecurityConfig 限制 `hasRole('ADMIN')`，Controller 类上再加 `@PreAuthorize("hasRole('ADMIN')")`。
- 前端：PC 端路由守卫按 `meta.section` 拦截；uniapp 首页监控区按 `userStore.isAdmin()` 渲染且不发起请求。

管理端能力（`AdminController`）：
- 用户列表/搜索、启用/禁用、改角色、重置密码。
- 保护规则：不能禁用或降级自己；系统至少保留一名启用的管理员。

## 五、迁移注意事项

1. 老库执行 `docs/schema.sql` 前，先按文件内的迁移注释清理重复记录、切换唯一键。
2. 生产环境务必通过环境变量覆盖：数据库密码、JWT_SECRET、APP_ADMIN_INITIAL_PASSWORD、APP_CORS_ORIGINS。
3. 修改用户角色后，旧 JWT 内仍带旧角色（有效期 1 天）；如需立即生效，让用户重新登录。
