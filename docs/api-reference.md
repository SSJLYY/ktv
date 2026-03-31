# API 参考文档

> KTV点歌系统 REST API 完整参考
> 版本：v1.0 | 更新时间：2026-03-31 | 作者：shaun.sheng

---

## 目录

- [概述](#概述)
- [认证](#认证)
- [通用说明](#通用说明)
- [后台管理接口](#后台管理接口)
  - [认证接口](#认证接口)
  - [歌曲管理](#歌曲管理)
  - [歌手管理](#歌手管理)
  - [分类管理](#分类管理)
  - [包厢管理](#包厢管理)
  - [订单管理](#订单管理)
- [包厢端接口](#包厢端接口)
  - [歌曲检索](#歌曲检索)
  - [点歌队列](#点歌队列)
  - [播放控制](#播放控制)
- [媒体接口](#媒体接口)
- [错误码](#错误码)

---

## 概述

### 基础信息

| 项目 | 值 |
|------|-----|
| 基础URL | `http://localhost:8080` |
| API前缀 | `/api` |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |

### 接口分组

| 分组 | 前缀 | 认证 | 说明 |
|------|------|------|------|
| 后台管理 | `/api/admin` | JWT | 管理员操作 |
| 包厢端 | `/api/room` | 无 | 点歌操作 |
| 媒体服务 | `/api/media` | 无 | 流媒体 |

---

## 认证

### JWT Token

后台管理接口需要 JWT 认证：

```http
Authorization: Bearer <your_token>
```

Token 通过登录接口获取，有效期 2 小时。

### 获取 Token

```http
POST /api/admin/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "admin",
    "realName": "管理员"
  }
}
```

---

## 通用说明

### 统一响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | integer | 状态码，200=成功 |
| msg | string | 提示信息 |
| data | object | 响应数据 |

### 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| current | integer | 1 | 当前页码 |
| size | integer | 10 | 每页条数 |

### 分页响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [ ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

## 后台管理接口

> 基础路径：`/api/admin`
> 认证方式：JWT Token

### 认证接口

#### 登录

```http
POST /api/admin/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 1,
    "username": "admin",
    "realName": "管理员"
  }
}
```

#### 退出登录

```http
POST /api/admin/logout
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

---

### 歌曲管理

#### 获取歌曲列表

```http
GET /api/admin/songs?current=1&size=10&name=告白气球&singerId=1&status=1
Authorization: Bearer <token>
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| current | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |
| name | string | 否 | 歌曲名（模糊搜索） |
| singerId | long | 否 | 歌手ID |
| categoryId | long | 否 | 分类ID |
| status | integer | 否 | 状态：0下架 1上架 |

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "告白气球",
        "singerId": 1,
        "singerName": "周杰伦",
        "categoryId": 1,
        "categoryName": "流行",
        "pinyin": "gao bai qi qiu",
        "pinyinInitial": "gbqq",
        "language": "国语",
        "duration": 215,
        "filePath": "/songs/jay/告白气球.mp3",
        "coverUrl": "/covers/jay/告白气球.jpg",
        "playCount": 12345,
        "isHot": 1,
        "isNew": 0,
        "status": 1,
        "createTime": "2026-03-01 10:00:00",
        "updateTime": "2026-03-15 14:30:00"
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

#### 获取歌曲详情

```http
GET /api/admin/songs/{id}
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "告白气球",
    "singerId": 1,
    "singerName": "周杰伦",
    "categoryId": 1,
    "categoryName": "流行",
    "pinyin": "gao bai qi qiu",
    "pinyinInitial": "gbqq",
    "language": "国语",
    "duration": 215,
    "filePath": "/songs/jay/告白气球.mp3",
    "coverUrl": "/covers/jay/告白气球.jpg",
    "playCount": 12345,
    "isHot": 1,
    "isNew": 0,
    "status": 1,
    "createTime": "2026-03-01 10:00:00",
    "updateTime": "2026-03-15 14:30:00"
  }
}
```

#### 新增歌曲

```http
POST /api/admin/songs
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "晴天",
  "singerId": 1,
  "categoryId": 1,
  "language": "国语",
  "duration": 270
}
```

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 歌曲名 |
| singerId | long | 是 | 歌手ID |
| categoryId | long | 否 | 分类ID |
| language | string | 否 | 语种，默认"国语" |
| duration | integer | 否 | 时长（秒） |

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 100,
    "name": "晴天",
    "singerId": 1,
    "pinyin": "qing tian",
    "pinyinInitial": "qt"
  }
}
```

#### 更新歌曲

```http
PUT /api/admin/songs/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "晴天（ remix版）",
  "categoryId": 2
}
```

#### 删除歌曲

```http
DELETE /api/admin/songs/{id}
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "msg": "删除成功",
  "data": null
}
```

---

### 歌手管理

#### 获取歌手列表

```http
GET /api/admin/singers?current=1&size=10&name=周杰伦&region=港台
Authorization: Bearer <token>
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| current | integer | 否 | 页码 |
| size | integer | 否 | 每页条数 |
| name | string | 否 | 歌手名（模糊搜索） |
| region | string | 否 | 地区：内地/港台/欧美/日韩/其他 |
| status | integer | 否 | 状态 |

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "周杰伦",
        "pinyin": "zhou jie lun",
        "pinyinInitial": "zjl",
        "gender": 1,
        "region": "港台",
        "avatar": "/avatars/jay.jpg",
        "songCount": 50,
        "status": 1,
        "createTime": "2026-01-01 00:00:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

#### 新增歌手

```http
POST /api/admin/singers
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "林俊杰",
  "gender": 1,
  "region": "港台"
}
```

**性别值：** 0未知 1男 2女 3组合

---

### 分类管理

#### 获取分类列表

```http
GET /api/admin/categories
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "流行",
      "sortOrder": 1,
      "status": 1
    },
    {
      "id": 2,
      "name": "经典",
      "sortOrder": 2,
      "status": 1
    }
  ]
}
```

---

### 包厢管理

#### 获取包厢列表

```http
GET /api/admin/rooms?current=1&size=10&status=0
Authorization: Bearer <token>
```

**状态值：** 0空闲 1使用中 2清洁中 3维修中

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "A01",
        "type": "小包",
        "capacity": 6,
        "pricePerHour": 88.00,
        "status": 0,
        "description": "舒适小包厢"
      }
    ],
    "total": 20,
    "size": 10,
    "current": 1,
    "pages": 2
  }
}
```

#### 更新包厢状态

```http
PUT /api/admin/rooms/{id}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": 2
}
```

---

### 订单管理

#### 获取订单列表

```http
GET /api/admin/orders?current=1&size=10&status=1
Authorization: Bearer <token>
```

**状态值：** 1消费中 2已结账 3已取消

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "orderNo": "ORD20260331001",
        "roomId": 1,
        "roomName": "A01",
        "startTime": "2026-03-31 14:00:00",
        "endTime": null,
        "durationMinutes": 0,
        "totalAmount": 0.00,
        "status": 1
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

#### 开台

```http
POST /api/admin/orders/open
Authorization: Bearer <token>
Content-Type: application/json

{
  "roomId": 1,
  "remark": "VIP客户"
}
```

**响应：**

```json
{
  "code": 200,
  "msg": "开台成功",
  "data": {
    "orderId": 1,
    "orderNo": "ORD20260331001",
    "roomId": 1
  }
}
```

#### 结账

```http
POST /api/admin/orders/{id}/close
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "msg": "结账成功",
  "data": {
    "orderId": 1,
    "totalAmount": 176.00,
    "durationMinutes": 120
  }
}
```

---

## 包厢端接口

> 基础路径：`/api/room`
> 认证方式：无（通过 orderId 标识会话）

### 歌曲检索

#### 搜索歌曲

```http
GET /api/room/songs/search?keyword=gbqq&current=1&size=20
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 是 | 搜索关键词（拼音/歌名） |
| current | integer | 否 | 页码 |
| size | integer | 否 | 每页条数 |

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "告白气球",
        "singerId": 1,
        "singerName": "周杰伦",
        "coverUrl": "/covers/jay/告白气球.jpg",
        "duration": 215,
        "playCount": 12345
      }
    ],
    "total": 1,
    "size": 20,
    "current": 1
  }
}
```

#### 按歌手查询

```http
GET /api/room/songs/by-singer/{singerId}?current=1&size=20
```

#### 按分类查询

```http
GET /api/room/songs/by-category/{categoryId}?current=1&size=20
```

#### 热门排行

```http
GET /api/room/songs/hot?limit=50
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "告白气球",
      "singerName": "周杰伦",
      "coverUrl": "/covers/jay/告白气球.jpg",
      "playCount": 12345,
      "rank": 1
    }
  ]
}
```

---

### 点歌队列

#### 点歌

```http
POST /api/room/{orderId}/queue/add
Content-Type: application/json

{
  "songId": 1
}
```

**响应：**

```json
{
  "code": 200,
  "msg": "点歌成功",
  "data": {
    "orderSongId": 101,
    "songId": 1,
    "songName": "告白气球",
    "singerName": "周杰伦",
    "sortOrder": 5,
    "status": 0
  }
}
```

#### 获取已点列表

```http
GET /api/room/{orderId}/queue
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 101,
      "songId": 1,
      "songName": "告白气球",
      "singerName": "周杰伦",
      "sortOrder": 1,
      "status": 0,
      "createTime": "2026-03-31 14:30:00"
    }
  ]
}
```

**状态值：** 0等待中 1播放中 2已播放 3已跳过

#### 置顶歌曲

```http
POST /api/room/{orderId}/queue/top/{orderSongId}
```

#### 取消点歌

```http
DELETE /api/room/{orderId}/queue/remove/{orderSongId}
```

#### 获取已唱列表

```http
GET /api/room/{orderId}/played
```

---

### 播放控制

#### 获取当前播放状态

```http
GET /api/room/{orderId}/play/current
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "playing": true,
    "orderSongId": 101,
    "songId": 1,
    "songName": "告白气球",
    "singerName": "周杰伦",
    "coverUrl": "/covers/jay/告白气球.jpg",
    "duration": 215
  }
}
```

#### 切歌（下一首）

```http
POST /api/room/{orderId}/play/next
```

#### 重唱（重新播放）

```http
POST /api/room/{orderId}/play/replay
```

#### 暂停

```http
POST /api/room/{orderId}/play/pause
```

#### 恢复播放

```http
POST /api/room/{orderId}/play/resume
```

---

## 媒体接口

> 基础路径：`/api/media`

### 流式传输

```http
GET /api/media/stream/{songId}
```

支持 HTTP Range 请求，实现进度拖拽：

```http
GET /api/media/stream/1
Range: bytes=0-1023
```

**响应头：**

```
Content-Type: audio/mpeg
Accept-Ranges: bytes
Content-Length: 1024
Content-Range: bytes 0-1023/5120000
```

### 获取封面

```http
GET /api/media/cover/{songId}
```

**响应：** 图片二进制数据

### 获取媒体信息

```http
GET /api/media/info/{songId}
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "songId": 1,
    "contentType": "audio/mpeg",
    "fileSize": 5120000,
    "duration": 215
  }
}
```

---

## 错误码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或Token过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 错误响应示例

```json
{
  "code": 400,
  "msg": "歌曲名不能为空",
  "data": null
}
```

```json
{
  "code": 401,
  "msg": "Token已过期，请重新登录",
  "data": null
}
```

---

## 附录

### 歌曲状态

| 值 | 说明 |
|-----|------|
| 0 | 下架 |
| 1 | 上架 |

### 包厢状态

| 值 | 说明 |
|-----|------|
| 0 | 空闲 |
| 1 | 使用中 |
| 2 | 清洁中 |
| 3 | 维修中 |

### 订单状态

| 值 | 说明 |
|-----|------|
| 1 | 消费中 |
| 2 | 已结账 |
| 3 | 已取消 |

### 点歌状态

| 值 | 说明 |
|-----|------|
| 0 | 等待播放 |
| 1 | 播放中 |
| 2 | 已播放 |
| 3 | 已跳过 |

### 歌手性别

| 值 | 说明 |
|-----|------|
| 0 | 未知 |
| 1 | 男 |
| 2 | 女 |
| 3 | 组合 |

### 语种类型

- 国语
- 粤语
- 英语
- 日语
- 韩语
- 其他

---

_文档结束_

**作者：shaun.sheng**
**更新时间：2026-03-31**
