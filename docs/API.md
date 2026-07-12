# 游戏社区平台 API 接口文档

> 版本：v1.0.0  
> 基础地址：`http://localhost:8080`  
> 接口前缀：`/api/v1`  
> 在线文档（Knife4j）：`http://localhost:8080/doc.html`

---

## 1. 通用约定

### 1.1 请求头

| Header | 必填 | 说明 |
|--------|------|------|
| `Content-Type` | 是（JSON 请求） | `application/json` |
| `Authorization` | 需登录接口 | `Bearer <accessToken>` |

### 1.2 统一响应结构

所有接口均返回 `Result<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": null
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 0 表示成功，非 0 表示失败 |
| message | string | 提示信息 |
| data | T | 业务数据，失败时通常为 null |
| requestId | string | 请求追踪 ID（预留） |

> HTTP 状态码说明：业务异常(`BizException`)通常返回 `HTTP 200 OK`，以 `code` 判断成败；参数校验失败为 `HTTP 400`；系统异常为 `HTTP 500`。

### 1.3 分页结构

分页接口的 `data` 为 `PageResult<T>`：

```json
{
  "page": 1,
  "size": 10,
  "total": 100,
  "records": []
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| page | long | 当前页码（从 1 开始） |
| size | long | 每页条数 |
| total | long | 总记录数 |
| records | T[] | 当前页数据列表 |

### 1.4 鉴权说明

| 标记 | 含义 |
|------|------|
| 公开 | 无需 Token |
| 需登录 | 请求头携带 `Authorization: Bearer <accessToken>` |
| 需管理员 | 需登录且用户角色包含 `ADMIN` |

Token 有效期：
- `accessToken`：默认 2 小时（7200 秒）
- `refreshToken`：默认 7 天

Token 过期后调用 `POST /api/v1/auth/refresh` 刷新。

### 1.5 时间格式

所有时间字段使用 ISO 8601 格式：`yyyy-MM-ddTHH:mm:ss`（如 `2026-06-05T14:30:00`）。

日期字段使用：`yyyy-MM-dd`（如 `2026-06-05`）。

### 1.6 ID 字段说明（重要）

后端主键使用雪花算法（`Long`），**JSON 响应中所有 `Long` 类型字段均序列化为字符串**，例如：

```json
{
  "id": "2062801749048246274",
  "userId": "1"
}
```

前端必须将 ID 作为 **字符串** 处理，禁止转为 JavaScript `Number`，否则会发生精度丢失导致「资源不存在」：

```javascript
// 错误：精度丢失
const id = Number(response.data.data); // 2062801749048246300 ≠ 2062801749048246274

// 正确：保持字符串
const id = String(response.data.data);
```

---

## 2. 错误码

### 2.1 通用错误码

| code | message | 说明 |
|------|---------|------|
| 0 | success | 成功 |
| 400 | 请求参数错误 | 参数校验失败 |
| 401 | 未登录或登录已过期 | 缺少 Token 或 Token 无效 |
| 403 | 无权限访问 | 角色权限不足 |
| 404 | 资源不存在 | 目标数据不存在 |
| 409 | 资源冲突 | 如邮箱/手机号已被占用 |
| 500 | 系统异常 | 服务端内部错误 |

### 2.2 业务错误码

| code | message | 说明 |
|------|---------|------|
| 1001 | 用户已存在 | 注册时用户名重复 |
| 1002 | 用户不存在 | 目标用户不存在 |
| 1003 | 用户名或密码错误 | 登录失败 |
| 1004 | 账号已被封禁 | 用户状态为封禁 |
| 1005 | Token无效 | refreshToken 格式错误 |
| 1006 | Token已过期 | Token 已过期 |
| 2001 | 游戏不存在 | 游戏 ID 无效 |
| 3001 | 帖子不存在 | 帖子 ID 无效 |
| 3002 | 版块不存在 | 版块 ID 无效 |
| 4001 | 评论不存在 | 评论 ID 无效 |
| 4002 | 已点赞 | 重复点赞 |
| 4003 | 未点赞 | 取消未点赞的内容 |
| 4004 | 已收藏 | 重复收藏 |
| 4005 | 未收藏 | 取消未收藏的内容 |
| 4006 | 已关注 | 重复关注 |
| 4007 | 未关注 | 取消未关注的用户 |
| 4008 | 不能关注自己 | 关注目标为自己 |
| 4009 | 今日已签到 | 重复签到 |
| 5001 | 文件上传失败 | 上传异常 |
| 5002 | 文件类型不允许 | 仅支持图片格式 |

---

## 3. 枚举值

### 3.1 内容状态 `status`（帖子/评论）

| 值 | 含义 |
|----|------|
| 0 | 草稿 |
| 1 | 待审核 |
| 2 | 已通过 |
| 3 | 已拒绝 |

> 帖子列表仅返回 `status=2`（已通过）的帖子。

### 3.2 用户状态

| 值 | 含义 |
|----|------|
| 0 | 封禁 |
| 1 | 正常 |

### 3.3 互动目标类型 `targetType`

| 值 | 含义 | 适用场景 |
|----|------|----------|
| 1 | 帖子 | 点赞、收藏 |
| 2 | 评论 | 点赞 |
| 3 | 游戏 | 点赞、收藏 |

### 3.4 通知类型 `type`

| 值 | 含义 |
|----|------|
| 1 | 点赞 |
| 2 | 评论 |
| 3 | 关注 |
| 4 | 系统通知 |
| 5 | 审核通知 |

### 3.5 用户角色 `roles`

| 值 | 含义 |
|----|------|
| ADMIN | 管理员 |
| USER | 普通用户 |

---

## 4. 认证模块

### 4.1 用户注册

- **URL**：`POST /api/v1/auth/register`
- **鉴权**：公开
- **Content-Type**：`application/json`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，3-64 字符 |
| password | string | 是 | 密码，6-64 字符 |
| nickname | string | 否 | 昵称，最长 64 字符，默认等于用户名 |
| email | string | 否 | 邮箱 |
| mobile | string | 否 | 手机号 |

**请求示例**

```json
{
  "username": "player01",
  "password": "123456",
  "nickname": "游戏玩家",
  "email": "player01@example.com"
}
```

**响应 data**：`TokenResp`

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | string | 访问令牌 |
| refreshToken | string | 刷新令牌 |
| expiresIn | long | accessToken 有效期（秒） |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 7200
  }
}
```

---

### 4.2 用户登录

- **URL**：`POST /api/v1/auth/login`
- **鉴权**：公开

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**请求示例**

```json
{
  "username": "admin",
  "password": "Admin@123456"
}
```

**响应 data**：`TokenResp`（同注册）

---

### 4.3 刷新 Token

- **URL**：`POST /api/v1/auth/refresh`
- **鉴权**：公开（使用 refreshToken，非 accessToken）

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | string | 是 | 刷新令牌 |

**请求示例**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**响应 data**：`TokenResp`（返回新的 accessToken 和 refreshToken）

---

## 5. 用户模块

### 5.1 获取当前用户信息

- **URL**：`GET /api/v1/users/me`
- **鉴权**：需登录

**响应 data**：`UserResp`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 用户 ID |
| username | string | 用户名 |
| nickname | string | 昵称 |
| email | string | 邮箱 |
| mobile | string | 手机号 |
| avatarUrl | string | 头像 URL |
| bio | string | 个人简介 |
| status | int | 用户状态（0 封禁 / 1 正常） |
| roles | string[] | 角色列表，如 `["USER"]` |
| gmtCreate | string | 注册时间 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "email": "admin@dgame.com",
    "mobile": null,
    "avatarUrl": null,
    "bio": "",
    "status": 1,
    "roles": ["ADMIN"],
    "gmtCreate": "2026-06-05T10:00:00"
  }
}
```

---

### 5.2 更新个人资料

- **URL**：`PUT /api/v1/users/me`
- **鉴权**：需登录

**请求体**（所有字段可选，传什么改什么）

| 字段 | 类型 | 说明 |
|------|------|------|
| nickname | string | 昵称，最长 64 |
| email | string | 邮箱 |
| mobile | string | 手机号 |
| avatarUrl | string | 头像 URL（可先通过文件上传接口获取） |
| bio | string | 简介，最长 500 |

**请求示例**

```json
{
  "nickname": "新昵称",
  "avatarUrl": "http://localhost:8080/files/2026/06/05/abc123.jpg",
  "bio": "热爱游戏的玩家"
}
```

**响应 data**：`UserResp`（更新后的用户信息）

---

### 5.3 获取当前用户成长信息

- **URL**：`GET /api/v1/users/me/growth`
- **鉴权**：需登录

**响应 data**：`UserGrowthResp`

| 字段 | 类型 | 说明 |
|------|------|------|
| totalPoints | int | 累计积分 |
| streakDays | int | 连续签到天数 |
| checkedInToday | boolean | 今日是否已签到 |
| badges | BadgeResp[] | 已获得徽章列表 |

`BadgeResp` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| badgeCode | string | 徽章编码（如 FIRST_CHECK_IN、STREAK_7） |
| badgeName | string | 徽章名称 |
| description | string | 徽章描述 |
| obtainedAt | string | 获得时间 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalPoints": 30,
    "streakDays": 3,
    "checkedInToday": true,
    "badges": [
      {
        "badgeCode": "FIRST_CHECK_IN",
        "badgeName": "初来乍到",
        "description": "完成首次签到",
        "obtainedAt": "2026-06-13T08:00:00"
      }
    ]
  }
}
```

---

## 6. 游戏模块

### 6.1 分页查询游戏

- **URL**：`GET /api/v1/games`
- **鉴权**：公开

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| categoryId | long | 否 | - | 按分类筛选 |
| tagId | long | 否 | - | 按标签筛选 |
| keyword | string | 否 | - | 按游戏名称模糊搜索 |
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

**请求示例**

```
GET /api/v1/games?categoryId=1&keyword=星际&page=1&size=10
```

**响应 data**：`PageResult<GameResp>`

`GameResp` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 游戏 ID |
| name | string | 游戏名称 |
| categoryId | long | 分类 ID |
| categoryName | string | 分类名称 |
| coverUrl | string | 封面 URL |
| description | string | 简介 |
| developer | string | 开发商 |
| releaseDate | string | 发行日期（yyyy-MM-dd） |
| avgRating | number | 平均评分（0.00-10.00） |
| ratingCount | int | 评分人数 |
| tags | string[] | 标签名称列表 |

---

### 6.2 游戏详情

- **URL**：`GET /api/v1/games/{id}`
- **鉴权**：公开

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 游戏 ID |

**响应 data**：`GameResp`

---

### 6.3 游戏分类列表

- **URL**：`GET /api/v1/games/categories`
- **鉴权**：公开

**响应 data**：`GameCategory[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 分类 ID |
| name | string | 分类名称 |
| sortOrder | int | 排序值 |
| gmtCreate | string | 创建时间 |
| gmtModified | string | 修改时间 |
| isDeleted | int | 逻辑删除标记：0否/1是（MyBatis-Plus） |

---

### 6.4 标签列表

- **URL**：`GET /api/v1/games/tags`
- **鉴权**：公开

**响应 data**：`Tag[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 标签 ID |
| name | string | 标签名称 |
| gmtCreate | string | 创建时间 |
| gmtModified | string | 修改时间 |
| isDeleted | int | 逻辑删除标记：0否/1是（MyBatis-Plus） |

---

### 6.5 游戏评分/评测

- **URL**：`POST /api/v1/games/{id}/rating`
- **鉴权**：需登录

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 游戏 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| score | int | 是 | 评分，1-10 |
| summary | string | 否 | 一句短评，最长 300 字 |
| pros | string | 否 | 优点，最长 500 字 |
| cons | string | 否 | 缺点，最长 500 字 |
| playtimeHours | int | 否 | 游玩时长（小时），非负整数 |

**请求示例**

```json
{
  "score": 9,
  "summary": "节奏扎实，联机体验很上头",
  "pros": "关卡设计丰富，手感稳定",
  "cons": "后期刷取略重复",
  "playtimeHours": 42
}
```

**响应 data**：`null`（成功时 code=0）

> 同一用户对同一游戏重复评分/评测会覆盖之前的内容。

---

### 6.6 游戏评测列表

- **URL**：`GET /api/v1/games/{id}/reviews`
- **鉴权**：公开

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

**响应 data**：`PageResult<GameReviewResp>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 评测 ID |
| userId | long | 用户 ID |
| userNickname | string | 用户昵称 |
| score | int | 评分，1-10 |
| summary | string | 一句短评 |
| pros | string | 优点 |
| cons | string | 缺点 |
| playtimeHours | int | 游玩时长（小时） |
| gmtCreate | string | 创建时间 |

---

### 6.7 创建游戏（管理员）

- **URL**：`POST /api/v1/games`
- **鉴权**：需管理员（ADMIN）

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 游戏名称 |
| categoryId | long | 是 | 分类 ID |
| coverUrl | string | 否 | 封面 URL |
| description | string | 否 | 简介 |
| developer | string | 否 | 开发商 |
| releaseDate | string | 否 | 发行日期（yyyy-MM-dd） |
| tagIds | long[] | 否 | 标签 ID 列表 |

**响应 data**：`long`（新创建的游戏 ID）

---

### 6.8 游戏排行榜

- **URL**：`GET /api/v1/games/ranking`
- **鉴权**：公开

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| type | string | 否 | rating | 排行类型：`rating`（评分榜）/ `popular`（人气榜） |
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

**响应 data**：`PageResult<GameResp>`

---

## 7. 社区模块

### 7.1 版块列表

- **URL**：`GET /api/v1/boards`
- **鉴权**：公开

**响应 data**：`Board[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 版块 ID |
| name | string | 版块名称 |
| description | string | 描述 |
| sortOrder | int | 排序值 |
| gmtCreate | string | 创建时间 |
| gmtModified | string | 修改时间 |
| isDeleted | int | 逻辑删除标记：0否/1是（MyBatis-Plus） |

---

### 7.2 分页查询帖子

- **URL**：`GET /api/v1/posts`
- **鉴权**：公开

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| boardId | long | 否 | - | 按版块筛选 |
| gameId | long | 否 | - | 按关联游戏筛选 |
| keyword | string | 否 | - | 按标题模糊搜索 |
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

**响应 data**：`PageResult<PostResp>`

`PostResp` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 帖子 ID |
| boardId | long | 版块 ID |
| boardName | string | 版块名称 |
| gameId | long | 关联游戏 ID |
| gameName | string | 关联游戏名称 |
| userId | long | 作者 ID |
| authorNickname | string | 作者昵称 |
| title | string | 标题 |
| content | string | 正文（HTML/Markdown 文本） |
| status | int | 内容状态 |
| viewCount | int | 浏览数 |
| likeCount | int | 点赞数 |
| commentCount | int | 评论数 |
| favoriteCount | int | 收藏数 |
| gmtCreate | string | 发布时间 |

---

### 7.3 帖子详情

- **URL**：`GET /api/v1/posts/{id}`
- **鉴权**：公开

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 帖子 ID |

**响应 data**：`PostResp`

> 每次访问详情会自动 `viewCount + 1`。

> 帖子详情接口不会过滤 `status`（当前代码按 ID 直接查询），因此可能返回 `status != 2` 的帖子。前端如需只展示已通过内容，请以 `status==2` 进行二次过滤。

---

### 7.4 发帖

- **URL**：`POST /api/v1/posts`
- **鉴权**：需登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| boardId | long | 是 | 版块 ID |
| gameId | long | 否 | 关联游戏 ID |
| title | string | 是 | 标题，最长 200 |
| content | string | 是 | 正文内容 |

**请求示例**

```json
{
  "boardId": 1,
  "gameId": 1001,
  "title": "《星际探险》新手攻略",
  "content": "<p>本文分享新手入门技巧...</p>"
}
```

**响应 data**：`long`（新帖子 ID）

> 新帖默认 `status=1`（待审核），管理员审核通过后才会出现在列表中。

---

### 7.5 帖子排行榜

- **URL**：`GET /api/v1/posts/ranking`
- **鉴权**：公开

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| type | string | 否 | hot | 排行类型：`hot`（热度榜）/ `latest`（最新榜） |
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

> 热度计算公式：`view_count + like_count * 3 + comment_count * 5 + favorite_count * 2`

**响应 data**：`PageResult<PostResp>`

---

## 8. 评论模块

### 8.1 帖子评论列表

- **URL**：`GET /api/v1/comments`
- **鉴权**：公开

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| postId | long | 是 | - | 帖子 ID |
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

**请求示例**

```
GET /api/v1/comments?postId=1&page=1&size=20
```

**响应 data**：`PageResult<CommentResp>`

`CommentResp` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 评论 ID |
| postId | long | 帖子 ID |
| parentId | long | 父评论 ID，0 表示顶级评论 |
| userId | long | 评论者 ID |
| userNickname | string | 评论者昵称 |
| content | string | 评论内容 |
| likeCount | int | 点赞数 |
| gmtCreate | string | 评论时间 |

---

### 8.2 发表评论

- **URL**：`POST /api/v1/comments`
- **鉴权**：需登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | long | 是 | 帖子 ID |
| parentId | long | 否 | 父评论 ID，默认 0（顶级评论） |
| content | string | 是 | 评论内容，最长 2000 |

**请求示例（顶级评论）**

```json
{
  "postId": 1,
  "content": "写得很棒！"
}
```

**请求示例（楼中楼回复）**

```json
{
  "postId": 1,
  "parentId": 10,
  "content": "同意，补充一点..."
}
```

**响应 data**：`long`（新评论 ID）

---

## 9. 互动模块

### 9.1 点赞

- **URL**：`POST /api/v1/likes`
- **鉴权**：需登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| targetType | int | 是 | 目标类型：1 帖子 / 2 评论 / 3 游戏 |
| targetId | long | 是 | 目标 ID |

**请求示例**

```json
{
  "targetType": 1,
  "targetId": 100
}
```

---

### 9.2 取消点赞

- **URL**：`DELETE /api/v1/likes`
- **鉴权**：需登录

**请求体**：同点赞

---

### 9.3 收藏

- **URL**：`POST /api/v1/favorites`
- **鉴权**：需登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| targetType | int | 是 | 目标类型：1 帖子 / 3 游戏 |
| targetId | long | 是 | 目标 ID |

---

### 9.4 取消收藏

- **URL**：`DELETE /api/v1/favorites`
- **鉴权**：需登录

**请求体**：同收藏

---

### 9.5 关注用户

- **URL**：`POST /api/v1/follows/{followeeId}`
- **鉴权**：需登录

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| followeeId | long | 被关注用户 ID |

---

### 9.6 取消关注

- **URL**：`DELETE /api/v1/follows/{followeeId}`
- **鉴权**：需登录

**路径参数**：同关注

---

## 10. 通知模块

### 10.1 通知列表

- **URL**：`GET /api/v1/notifications`
- **鉴权**：需登录

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

**响应 data**：`PageResult<Notification>`

`Notification` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 通知 ID |
| receiverId | long | 接收者 ID |
| senderId | long | 发送者 ID |
| type | int | 通知类型（见枚举 3.4） |
| title | string | 标题 |
| content | string | 内容 |
| targetType | int | 关联目标类型 |
| targetId | long | 关联目标 ID |
| isRead | int | 是否已读：0 否 / 1 是 |
| gmtCreate | string | 通知时间 |
| gmtModified | string | 最后更新时间 |
| isDeleted | int | 逻辑删除标记：0否/1是（MyBatis-Plus） |

---

### 10.2 未读通知数量

- **URL**：`GET /api/v1/notifications/unread-count`
- **鉴权**：需登录

**响应 data**：`long`（未读数量）

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": 5
}
```

---

### 10.3 标记单条已读

- **URL**：`PUT /api/v1/notifications/{id}/read`
- **鉴权**：需登录

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 通知 ID |

---

### 10.4 全部标记已读

- **URL**：`PUT /api/v1/notifications/read-all`
- **鉴权**：需登录

---

## 11. 签到模块

> 签到数据存储于 Redis Bitmap（Key：`checkin:bitmap:{userId}:{yyyy}`），按 Asia/Shanghai 时区计算日期。

### 11.1 查询签到状态

- **URL**：`GET /api/v1/check-ins/status`
- **鉴权**：需登录

**响应 data**：`CheckInStatusResp`

| 字段 | 类型 | 说明 |
|------|------|------|
| checkedInToday | boolean | 今日是否已签到 |
| streakDays | int | 连续签到天数 |
| lastCheckInDate | string \| null | 最近签到日期（yyyy-MM-dd） |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "checkedInToday": false,
    "streakDays": 3,
    "lastCheckInDate": "2026-06-14"
  }
}
```

---

### 11.2 每日签到

- **URL**：`POST /api/v1/check-ins`
- **鉴权**：需登录

**响应 data**：`CheckInResultResp`

| 字段 | 类型 | 说明 |
|------|------|------|
| checkInDate | string | 签到日期（yyyy-MM-dd） |
| streakDays | int | 签到后的连续天数 |
| pointsEarned | int | 本次获得积分（重复签到为 0） |
| newBadges | string[] | 本次新获得的徽章编码列表 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "checkInDate": "2026-06-15",
    "streakDays": 4,
    "pointsEarned": 10,
    "newBadges": []
  }
}
```

**业务错误**

| code | 说明 |
|------|------|
| 4009 | 今日已签到 |

---

## 12. 搜索模块

### 12.1 统一搜索

- **URL**：`GET /api/v1/search`
- **鉴权**：公开

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | string | 否 | - | 搜索关键词 |
| type | string | 否 | ALL | 搜索范围：`GAME` / `POST` / `ALL` |
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

**响应 data**：`SearchResp`

| 字段 | 类型 | 说明 |
|------|------|------|
| games | PageResult\<GameResp\> \| null | 游戏搜索结果（type=GAME 或 ALL 时返回） |
| posts | PageResult\<PostResp\> \| null | 帖子搜索结果（type=POST 或 ALL 时返回） |

**请求示例**

```
GET /api/v1/search?keyword=星际&type=ALL&page=1&size=10
```

---

## 13. 文件模块

### 13.1 上传图片

- **URL**：`POST /api/v1/files/upload`
- **鉴权**：需登录
- **Content-Type**：`multipart/form-data`

**表单参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 图片文件，最大 10MB |

**支持格式**：`image/jpeg`、`image/png`、`image/gif`、`image/webp`

**响应 data**：`FileResp`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 文件记录 ID |
| fileKey | string | 文件存储 Key |
| fileUrl | string | 文件访问 URL |
| fileName | string | 原始文件名 |
| fileSize | long | 文件大小（字节） |
| contentType | string | MIME 类型 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "fileKey": "2026/06/05/abc123def456.jpg",
    "fileUrl": "http://localhost:8080/files/2026/06/05/abc123def456.jpg",
    "fileName": "avatar.jpg",
    "fileSize": 102400,
    "contentType": "image/jpeg"
  }
}
```

**前端上传示例（axios）**

```javascript
const formData = new FormData();
formData.append('file', file);

const res = await axios.post('/api/v1/files/upload', formData, {
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'multipart/form-data'
  }
});
const imageUrl = res.data.data.fileUrl;
```

---

## 14. 后台管理模块

> 以下接口均需登录且角色为 `ADMIN`。

### 14.1 封禁用户

- **URL**：`PUT /api/v1/admin/users/{userId}/ban`

### 14.2 解封用户

- **URL**：`PUT /api/v1/admin/users/{userId}/unban`

### 14.3 审核帖子

- **URL**：`POST /api/v1/admin/posts/{postId}/audit`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approved | boolean | 是 | true 通过 / false 拒绝 |
| reason | string | 否 | 审核备注 |

**请求示例**

```json
{
  "approved": true,
  "reason": "内容合规"
}
```

---

### 14.4 审核评论

- **URL**：`POST /api/v1/admin/comments/{commentId}/audit`

**请求体**：同审核帖子

---

### 14.5 角色列表

- **URL**：`GET /api/v1/admin/roles`

**响应 data**：`SysRole[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 角色 ID |
| roleCode | string | 角色编码（ADMIN / USER） |
| roleName | string | 角色名称 |
| description | string | 描述 |
| gmtCreate | string | 创建时间 |
| gmtModified | string | 修改时间 |
| isDeleted | int | 逻辑删除标记：0否/1是（MyBatis-Plus） |

---

### 14.6 分配角色

- **URL**：`POST /api/v1/admin/roles/assign`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | long | 是 | 用户 ID |
| roleId | long | 是 | 角色 ID |

---

### 14.7 审计日志列表

- **URL**：`GET /api/v1/admin/audit-logs`
- **鉴权**：需管理员（ADMIN）

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| operatorId | long | 否 | - | 操作人 ID |
| action | string | 否 | - | 操作动作（如 BAN_USER、AUDIT_POST） |
| targetType | string | 否 | - | 目标类型（如 USER、POST） |
| targetId | long | 否 | - | 目标 ID |
| startTime | string | 否 | - | 开始时间（ISO 8601） |
| endTime | string | 否 | - | 结束时间（ISO 8601） |
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页条数 |

**响应 data**：`PageResult<AuditLogResp>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 日志 ID |
| operatorId | long | 操作人 ID |
| operatorNickname | string | 操作人昵称 |
| action | string | 操作动作 |
| targetType | string | 目标类型 |
| targetId | long | 目标 ID |
| detail | string | 操作详情 |
| gmtCreate | string | 操作时间 |

---

## 15. 前端接入指南

### 15.1 Axios 封装示例

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 10000,
});

// 请求拦截：自动附加 Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截：统一错误处理 + Token 刷新
api.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data;
    if (code === 0) return data;
    return Promise.reject(new Error(message));
  },
  async (error) => {
    if (error.response?.status === 401) {
      // Token 过期，尝试刷新
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const res = await axios.post('/api/v1/auth/refresh', { refreshToken });
          const { accessToken, refreshToken: newRefresh } = res.data.data;
          localStorage.setItem('accessToken', accessToken);
          localStorage.setItem('refreshToken', newRefresh);
          error.config.headers.Authorization = `Bearer ${accessToken}`;
          return api(error.config);
        } catch {
          // 刷新失败，跳转登录
          localStorage.clear();
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

### 15.2 典型业务流程

```
注册/登录 → 获取 Token → 存储到 localStorage
    ↓
浏览游戏库（公开）/ 浏览帖子列表（公开）
    ↓
发帖（需登录）→ 等待审核 → 管理员审核通过 → 帖子出现在列表
    ↓
评论 / 点赞 / 收藏 / 关注 → 触发通知给目标用户
    ↓
查看通知列表 / 未读数角标
```

### 15.3 默认测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | Admin@123456 | ADMIN |

---

## 16. 接口速查表

| 模块 | 方法 | 路径 | 鉴权 |
|------|------|------|------|
| 认证 | POST | /api/v1/auth/register | 公开 |
| 认证 | POST | /api/v1/auth/login | 公开 |
| 认证 | POST | /api/v1/auth/refresh | 公开 |
| 用户 | GET | /api/v1/users/me | 登录 |
| 用户 | PUT | /api/v1/users/me | 登录 |
| 用户 | GET | /api/v1/users/me/growth | 登录 |
| 游戏 | GET | /api/v1/games | 公开 |
| 游戏 | GET | /api/v1/games/ranking | 公开 |
| 游戏 | GET | /api/v1/games/{id} | 公开 |
| 游戏 | GET | /api/v1/games/{id}/reviews | 公开 |
| 游戏 | GET | /api/v1/games/categories | 公开 |
| 游戏 | GET | /api/v1/games/tags | 公开 |
| 游戏 | POST | /api/v1/games | 管理员 |
| 游戏 | POST | /api/v1/games/{id}/rating | 登录 |
| 版块 | GET | /api/v1/boards | 公开 |
| 帖子 | GET | /api/v1/posts | 公开 |
| 帖子 | GET | /api/v1/posts/ranking | 公开 |
| 帖子 | GET | /api/v1/posts/{id} | 公开 |
| 帖子 | POST | /api/v1/posts | 登录 |
| 评论 | GET | /api/v1/comments | 公开 |
| 评论 | POST | /api/v1/comments | 登录 |
| 互动 | POST | /api/v1/likes | 登录 |
| 互动 | DELETE | /api/v1/likes | 登录 |
| 互动 | POST | /api/v1/favorites | 登录 |
| 互动 | DELETE | /api/v1/favorites | 登录 |
| 互动 | POST | /api/v1/follows/{followeeId} | 登录 |
| 互动 | DELETE | /api/v1/follows/{followeeId} | 登录 |
| 通知 | GET | /api/v1/notifications | 登录 |
| 通知 | GET | /api/v1/notifications/unread-count | 登录 |
| 通知 | PUT | /api/v1/notifications/{id}/read | 登录 |
| 通知 | PUT | /api/v1/notifications/read-all | 登录 |
| 签到 | GET | /api/v1/check-ins/status | 登录 |
| 签到 | POST | /api/v1/check-ins | 登录 |
| 搜索 | GET | /api/v1/search | 公开 |
| 文件 | POST | /api/v1/files/upload | 登录 |
| 后台 | PUT | /api/v1/admin/users/{userId}/ban | 管理员 |
| 后台 | PUT | /api/v1/admin/users/{userId}/unban | 管理员 |
| 后台 | POST | /api/v1/admin/posts/{postId}/audit | 管理员 |
| 后台 | POST | /api/v1/admin/comments/{commentId}/audit | 管理员 |
| 后台 | GET | /api/v1/admin/roles | 管理员 |
| 后台 | POST | /api/v1/admin/roles/assign | 管理员 |
| 后台 | GET | /api/v1/admin/audit-logs | 管理员 |
