# 游戏社区平台后端 (D_game)

基于 Spring Boot 3.5 / Java 21 的游戏社区平台后端，遵循阿里巴巴 Java/MySQL 开发规范，持久层使用 MyBatis-Plus。

## 技术栈

- Spring Boot 3.5.14 / Java 21
- MyBatis-Plus 3.5.9 + MySQL 8
- Redis 7（未读通知计数等）
- JWT 鉴权（jjwt 0.12）
- Knife4j / OpenAPI3 接口文档
- Docker / docker-compose 部署

## 功能模块

- 用户与鉴权：注册、登录、刷新 Token、个人资料（密码 BCrypt 加密，JWT 无状态鉴权）
- 游戏库：游戏 CRUD、分类、标签、用户评分与综合评分聚合
- 社区帖子：版块、发帖（富文本文本入库）、列表/详情、计数维护
- 互动：评论（楼中楼）、点赞（多态）、收藏、关注
- 通知：互动/系统事件站内信，未读计数走 Redis
- 后台管理：RBAC 角色权限、内容审核状态机、用户封禁、操作审计日志
- 文件：图片上传，落本地目录，DB 仅存 key/url

## 目录结构

```
src/main/java/com/xiaoyang/d_game/
├── common/      统一返回、异常、枚举、BaseEntity
├── config/      MyBatis-Plus、Redis、Knife4j、WebMvc、JWT/File 配置
├── security/    JWT 工具、鉴权拦截器、@RequireLogin/@RequireRole
├── entity/      实体（对应数据库表）
├── dto/         请求/响应对象
├── mapper/      MyBatis-Plus Mapper
├── service/     业务接口
│   └── impl/    业务实现（事务边界）
└── controller/  控制层（REST 接口，前缀 /api/v1）
```

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
docker compose up -d --build
```

启动后会自动初始化数据库（执行 `schema.sql` 与 `data.sql`）。

### 方式二：本地运行

1. 准备 MySQL 8 与 Redis 7，创建数据库并执行：
   - `src/main/resources/db/schema.sql`
   - `src/main/resources/db/data.sql`
2. 通过环境变量或修改 `application.yml` 配置数据源、Redis、JWT 密钥。
3. 启动：

```bash
./mvnw spring-boot:run
```

## 主要环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库连接 | localhost / 3306 / d_game |
| `DB_USER` / `DB_PASSWORD` | 数据库账号 | root / 空 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 连接 | localhost / 6379 |
| `JWT_SECRET` | JWT 签名密钥，至少 32 字符 | 无，必须配置 |
| `FILE_UPLOAD_DIR` | 文件上传目录 | ./uploads |
| `FILE_BASE_URL` | 文件访问基础 URL | http://localhost:8080/files |

Docker Compose 启动前可复制 `.env.example` 为 `.env`，并填写本机数据库密码与随机 `JWT_SECRET`。`.env` 已被 `.gitignore` 忽略，请勿提交真实密钥。

## 接口文档

启动后访问：

- Knife4j 文档：http://localhost:8080/doc.html
- OpenAPI JSON：http://localhost:8080/v3/api-docs

需要登录的接口在文档右上角填入 `Bearer <accessToken>`（通过 `Authorization` 认证）。

## 默认管理员账号

| 用户名 | 密码 |
|--------|------|
| `admin` | `Admin@123456` |

> 生产环境请务必修改默认管理员密码，并使用高强度随机 `JWT_SECRET`。

## 主要接口一览

- 认证：`POST /api/v1/auth/register` `/login` `/refresh`
- 用户：`GET/PUT /api/v1/users/me`
- 游戏：`GET /api/v1/games` `/games/{id}` `/games/categories` `/games/tags`，`POST /api/v1/games`（管理员）、`POST /api/v1/games/{id}/rating`
- 版块/帖子：`GET /api/v1/boards`，`GET/POST /api/v1/posts` `/posts/{id}`
- 互动：`GET/POST /api/v1/comments`，`POST/DELETE /api/v1/likes` `/favorites`，`POST/DELETE /api/v1/follows/{followeeId}`
- 通知：`GET /api/v1/notifications` `/unread-count`，`PUT /api/v1/notifications/{id}/read` `/read-all`
- 后台：`/api/v1/admin/**`（封禁、审核、角色，需 ADMIN 角色）
- 文件：`POST /api/v1/files/upload`
