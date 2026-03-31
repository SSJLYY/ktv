# API 接口参考

本文档提供 KTV 点歌系统所有 API 接口的详细说明。

## 接口规范

### 基础信息

- **Base URL**：`http://localhost:8080`
- **Content-Type**：`application/json`
- **字符编码**：UTF-8

### 统一响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Number | 响应码，200 表示成功，其他表示失败 |
| msg | String | 响应消息 |
| data | Object | 响应数据 |

### 常用响应码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（Token 无效或过期） |
| 403 | 禁止访问（权限不足） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 认证方式

后台管理接口需要 JWT Token 认证：

```http
Authorization: Bearer {token}
```

---

## 后台管理接口

### 认证模块

#### 登录

```http
POST /api/admin/login
```

**请求参数**：
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应示例**：
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "role": "super_admin"
    }
  }
}
```

#### 退出登录

```http
POST /api/admin/logout
```

**请求头**：
```http
Authorization: Bearer {token}
```

---

### 歌曲管理

#### 查询歌曲列表

```http
GET /api/admin/songs
```

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Number | 否 | 页码，默认 1 |
| pageSize | Number | 否 | 每页条数，默认 10 |
| keyword | String | 否 | 搜索关键词（歌曲名/拼音） |
| singerId | Number | 否 | 歌手 ID |
| categoryId | Number | 否 | 分类 ID |

**响应示例**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "青花瓷",
        "singerId": 1,
        "singerName": "周杰伦",
        "categoryId": 1,
        "categoryName": "流行",
        "pinyin": "qinghuaci",
        "pinyinInitial": "QHC",
        "language": "国语",
        "duration": 234,
        "filePath": "/songs/qinghuaci.mp3",
        "coverUrl": "/covers/qinghuaci.jpg",
        "playCount": 1250,
        "isHot": 1,
        "isNew": 0,
        "status": 1,
        "createTime": "2026-03-30T10:00:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

#### 新增歌曲

```http
POST /api/admin/songs
```

**请求参数**：
```json
{
  "name": "歌曲名",
  "singerId": 1,
  "categoryId": 1,
  "pinyin": "全拼",
  "pinyinInitial": "QHZM",
  "language": "国语",
  "duration": 180,
  "isHot": 0,
  "isNew": 1,
  "status": 1
}
```

#### 修改歌曲

```http
PUT /api/admin/songs/{id}
```

**请求参数**：
```json
{
  "name": "歌曲名",
  "singerId": 1,
  "categoryId": 1,
  "pinyin": "全拼",
  "pinyinInitial": "QHZM",
  "language": "国语",
  "duration": 180,
  "isHot": 0,
  "isNew": 1,
  "status": 1
}
```

#### 删除歌曲

```http
DELETE /api/admin/songs/{id}
```

---

### 歌手管理

#### 查询歌手列表

```http
GET /api/admin/singers
```

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Number | 否 | 页码，默认 1 |
| pageSize | Number | 否 | 每页条数，默认 10 |
| keyword | String | 否 | 搜索关键词（歌手名/拼音） |
| region | String | 否 | 地区（内地/港台/欧美/日韩） |

#### 新增歌手

```http
POST /api/admin/singers
```

**请求参数**：
```json
{
  "name": "周杰伦",
  "pinyin": "zhoujielun",
  "pinyinInitial": "ZJL",
  "gender": 1,
  "region": "内地",
  "avatar": "/avatars/zhoujielun.jpg",
  "status": 1
}
```

#### 修改歌手

```http
PUT /api/admin/singers/{id}
```

#### 删除歌手

```http
DELETE /api/admin/singers/{id}
```

---

### 分类管理

#### 查询分类列表

```http
GET /api/admin/categories
```

#### 新增分类

```http
POST /api/admin/categories
```

**请求参数**：
```json
{
  "name": "流行",
  "sortOrder": 1,
  "status": 1
}
```

---

### 包厢管理

#### 查询包厢列表

```http
GET /api/admin/rooms
```

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Number | 否 | 页码，默认 1 |
| pageSize | Number | 否 | 每页条数，默认 10 |
| status | Number | 否 | 状态（0空闲 1使用中 2清洁中 3维修中） |

#### 新增包厢

```http
POST /api/admin/rooms
```

**请求参数**：
```json
{
  "name": "A01",
  "type": "小包",
  "capacity": 4,
  "pricePerHour": 88.00,
  "status": 0,
  "description": "小包厢"
}
```

#### 更新包厢状态

```http
PUT /api/admin/rooms/{id}/status
```

**请求参数**：
```json
{
  "status": 1
}
```

---

### 订单管理

#### 查询订单列表

```http
GET /api/admin/orders
```

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Number | 否 | 页码，默认 1 |
| pageSize | Number | 否 | 每页条数，默认 10 |
| roomId | Number | 否 | 包厢 ID |
| status | Number | 否 | 状态（1消费中 2已结账 3已取消） |
| startDate | String | 否 | 开始日期 |
| endDate | String | 否 | 结束日期 |

#### 开台

```http
POST /api/admin/orders/open
```

**请求参数**：
```json
{
  "roomId": 1,
  "remark": "备注"
}
```

**响应示例**：
```json
{
  "code": 200,
  "msg": "开台成功",
  "data": {
    "orderId": 1,
    "orderNo": "ORD20260330100001",
    "roomName": "A01",
    "startTime": "2026-03-30T10:00:00"
  }
}
```

#### 结账

```http
POST /api/admin/orders/{id}/close
```

**响应示例**：
```json
{
  "code": 200,
  "msg": "结账成功",
  "data": {
    "orderId": 1,
    "orderNo": "ORD20260330100001",
    "durationMinutes": 120,
    "totalAmount": 176.00
  }
}
```

---

## 包厢端接口

### 歌曲检索

#### 搜索歌曲

```http
GET /api/room/songs/search
```

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 是 | 搜索关键词（拼音/名称） |
| pageNum | Number | 否 | 页码，默认 1 |
| pageSize | Number | 否 | 每页条数，默认 20 |

#### 按歌手查询

```http
GET /api/room/songs/by-singer/{singerId}
```

#### 按分类查询

```http
GET /api/room/songs/by-category/{categoryId}
```

#### 热门排行

```http
GET /api/room/songs/hot
```

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Number | 否 | 返回数量，默认 20 |

---

### 点歌队列

#### 加入队列

```http
POST /api/room/{orderId}/queue/add
```

**请求参数**：
```json
{
  "songId": 1
}
```

#### 置顶歌曲

```http
POST /api/room/{orderId}/queue/top/{orderSongId}
```

#### 取消点歌

```http
DELETE /api/room/{orderId}/queue/remove/{orderSongId}
```

#### 获取已点列表

```http
GET /api/room/{orderId}/queue
```

**响应示例**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "songId": 1,
      "songName": "青花瓷",
      "singerName": "周杰伦",
      "sortOrder": 1,
      "status": 0,
      "createTime": "2026-03-30T10:00:00"
    }
  ]
}
```

#### 获取已唱列表

```http
GET /api/room/{orderId}/played
```

---

### 播放控制

#### 切歌

```http
POST /api/room/{orderId}/play/next
```

#### 重唱

```http
POST /api/room/{orderId}/play/replay
```

#### 暂停

```http
POST /api/room/{orderId}/play/pause
```

#### 继续播放

```http
POST /api/room/{orderId}/play/resume
```

#### 当前播放状态

```http
GET /api/room/{orderId}/play/current
```

**响应示例**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "orderSongId": 1,
    "songId": 1,
    "songName": "青花瓷",
    "singerName": "周杰伦",
    "duration": 234,
    "status": "playing",
    "playTime": "2026-03-30T10:05:00"
  }
}
```

---

## 媒体接口

### 流式传输音视频

```http
GET /api/media/stream/{songId}
```

**说明**：
- 支持 HTTP Range 请求，实现断点续传和进度拖拽
- 返回 `Content-Type: audio/mpeg`（MP3）或 `video/mp4`（MP4）
- 支持浏览器原生播放器播放

**请求头**：
```http
Range: bytes=0-1023
```

**响应头**：
```http
Content-Range: bytes 0-1023/1234567
Content-Length: 1024
Accept-Ranges: bytes
```

---

### 获取封面图片

```http
GET /api/media/cover/{songId}
```

**响应**：
- 直接返回图片二进制数据
- Content-Type: `image/jpeg` 或 `image/png`

---

### 上传歌曲文件

```http
POST /api/admin/songs/{songId}/upload
Content-Type: multipart/form-data
```

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 音视频文件（MP3/FLAC/MP4） |
| cover | File | 否 | 封面图片（JPG/PNG） |

---

## 错误码说明

| code | msg | 说明 |
|------|-----|------|
| 400 | 请求参数错误 | 参数缺失或格式错误 |
| 401 | 未授权 | Token 无效或过期 |
| 403 | 禁止访问 | 权限不足 |
| 404 | 资源不存在 | 查询的资源不存在 |
| 500 | 服务器内部错误 | 后端处理异常 |

---

## 测试工具

推荐使用以下工具测试接口：

- **Postman**：https://www.postman.com/
- **Apifox**：https://www.apifox.cn/
- **curl**：命令行工具

### curl 示例

```bash
# 登录
curl -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 查询歌曲列表（需要 Token）
curl http://localhost:8080/api/admin/songs \
  -H "Authorization: Bearer YOUR_TOKEN"

# 新增歌曲（需要 Token）
curl -X POST http://localhost:8080/api/admin/songs \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"歌曲名","singerId":1,"categoryId":1}'
```

---

## 相关文档

- [项目设计文档](../docs/project-overview.md)
- [开发任务列表](../docs/tasks/ktv-song-system-tasklist.md)

---

**作者**：shaun.sheng

祝你开发愉快！🚀
