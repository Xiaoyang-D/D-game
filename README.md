# 游戏社区平台后端 (D_game)

基于 Spring Boot 3.5 / Java 21 的游戏社区平台后端，遵循阿里巴巴 Java/MySQL 开发规范，持久层使用 MyBatis-Plus。

## 技术栈

- Spring Boot 3.5.14 / Java 21
- MyBatis-Plus 3.5.9 + MySQL 8
- Redis 7（签到 Bitmap、未读通知计数、Token 注销）
- RabbitMQ 3.13（异步站内通知、死信队列）
- Spring Security 6 + JWT 鉴权（jjwt 0.12）
- Knife4j / OpenAPI3 接口文档
- Docker / docker-compose 部署

## 功能模块

- 用户与鉴权：注册、登录、刷新 Token、登出、个人资料。密码使用 BCrypt 哈希，服务端使用无状态 Bearer JWT；退出登录后令牌会写入 Redis 注销名单。
- 游戏库：游戏创建、分类、标签、筛选、排行榜、用户评分和评测；游戏表维护平均分及评分人数汇总。
- 社区帖子：版块、富文本发帖、草稿、话题、个人合集、定时提交审核、关注流、排行榜和游戏关联讨论。
- 互动：楼中楼评论、帖子/评论点赞、帖子/游戏收藏和关注用户；计数采用事务与数据库原子更新维护。
- 通知：点赞、评论、关注和审核结果在主事务提交后投递 RabbitMQ，由消费者异步写入站内通知；未读数缓存在 Redis。
- 成长体系：Redis Bitmap 每日签到、积分流水和首次签到/连续签到徽章。
- 内容治理：举报处理、RBAC 角色权限、内容审核状态机、用户封禁和后台审计日志。
- 文件：仅允许 JPEG、PNG、GIF、WebP 图片，按日期和 UUID 保存到本地目录，数据库记录元数据及访问 URL。

## 目录结构

```
src/main/java/com/xiaoyang/d_game/
├── common/      统一返回、异常、枚举、BaseEntity
├── config/      MyBatis-Plus、Redis、Knife4j、WebMvc、JWT/File 配置
├── security/    JWT 工具、Spring Security 过滤器、当前用户上下文、令牌注销
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
2. 准备 RabbitMQ 3.13，并通过环境变量配置数据源、Redis、RabbitMQ 与 JWT 密钥。
3. 启动：

```bash
./mvnw spring-boot:run
```

## 配置环境

`application.yml` 只保留公共配置，并通过 `SPRING_PROFILES_ACTIVE` 选择具体环境，默认使用 `dev`：

| 环境 | 配置文件 | 说明 |
|------|----------|------|
| `dev` | `application-dev.yml` | 本地开发，默认连接本机 MySQL / Redis |
| `test` | `application-test.yml` | 自动化测试使用，测试类默认激活 |
| `prod` | `application-prod.yml` | 生产/容器部署，数据库、Redis、JWT、CORS 等必须通过环境变量提供 |

这三份 profile 文件按本地环境维护，已加入 `.gitignore`，不会上传到仓库。

示例：

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## 主要环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 配置环境 | dev（Docker Compose 默认 prod） |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库连接 | localhost / 3306 / d_game |
| `DB_USER` / `DB_PASSWORD` | 数据库账号 | root / 空 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 连接 | localhost / 6379 |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | RabbitMQ 连接 | localhost / 5672 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 账号 | guest / guest |
| `JWT_SECRET` | JWT 签名密钥，至少 32 字符 | 无，必须配置 |
| `FILE_UPLOAD_DIR` | 文件上传目录 | ./uploads |
| `FILE_BASE_URL` | 文件访问基础 URL | http://localhost:8080/files |

Docker Compose 启动前可复制 `.env.example` 为 `.env`，并填写数据库密码、RabbitMQ 账号密码与随机 `JWT_SECRET`。`.env` 已被 `.gitignore` 忽略，请勿提交真实密钥。

## 异步通知与并发安全

### 签到奖励失败补偿

升级已有数据库时，先执行 `src/main/resources/db/migrations/V5__check_in_reward_recovery.sql`，再启动新版后端。新建数据库的 `schema.sql` 已包含该表；项目不会自动运行这些迁移文件。

签到先将用户和业务日期写入 `check_in_reward_task`，再幂等写入 Redis Bitmap 并发放奖励。积分和徽章在同一个数据库事务内提交，提交成功后才删除补偿任务。任何一步失败都保留任务；即使奖励已提交而任务删除失败，重放也会由既有积分业务键与徽章唯一键防止重复发放。并发请求发生唯一键冲突时，奖励事务回滚，保留任务等待下一次重试。

后台默认每 60 秒处理最多 50 条到期任务，失败任务延后 60 秒再次处理；可用 `app.check-in.reward-retry-interval-ms` 调整轮询间隔。任务记录原始签到日期，因此跨天和服务重启后仍可补偿，不依赖用户再次登录。签到状态新增 `rewardPending`，前端据此保留“奖励待补发，点击重试”按钮。重复签到返回成功，本次未新增积分时为 0。

任务表只记录升级后收到的签到请求；升级前已丢失的奖励不会自动生成历史补偿任务。Redis Bitmap 仍负责签到历史与连续天数，应继续配置 Redis 持久化和备份；本机制不替代 Redis 数据恢复。

互动和审核操作不会同步写通知表：主事务提交后才投递 `NotificationEvent`，RabbitMQ 消费者在线程池中异步落库。消费者使用事件 ID 的 Redis 幂等标记防止重复消费，消费失败的消息会进入死信队列。

高频状态更新也具备并发保护：签到使用 Redis `setBit` 判断重复请求；互动关系和评分使用数据库唯一索引；帖子互动计数使用数据库原子更新，避免并发覆盖。

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

- 认证：`POST /api/v1/auth/register` `/login` `/refresh` `/logout`
- 用户：`GET/PUT /api/v1/users/me`
- 游戏：`GET /api/v1/games` `/games/{id}` `/games/categories` `/games/tags`，`POST /api/v1/games`（管理员）、`POST /api/v1/games/{id}/rating`
- 版块/帖子：`GET /api/v1/boards`，`GET/POST /api/v1/posts` `/posts/{id}`
- 互动：`GET/POST /api/v1/comments`，`POST/DELETE /api/v1/likes` `/favorites`，`POST/DELETE /api/v1/follows/{followeeId}`
- 通知：`GET /api/v1/notifications` `/unread-count`，`PUT /api/v1/notifications/{id}/read` `/read-all`
- 后台：`/api/v1/admin/**`（封禁、审核、角色，需 ADMIN 角色）
- 文件：`POST /api/v1/files/upload`
