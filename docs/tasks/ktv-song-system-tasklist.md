# KTV点歌系统 - 开发任务列表

> 版本：v1.3 | 更新日期：2026-03-30（前端改为 React + Ant Design；JDK升级21，Spring Boot升级3.2.x；纳入本地文件流媒体播放）
> 作者：shaun.sheng
> 参考文档：`docs/project-overview.md`

---

## 规格摘要

**原始需求**：KTV点歌管理系统，含后台管理（歌曲/歌手/包厢/订单）和包厢端点歌（检索/点歌/排队/实际播放）  
**技术栈**：**Java 21** + **Spring Boot 3.2.x** + MyBatis-Plus + MySQL 8 + Redis + React 18 + Ant Design 5  
**媒体播放**：本地MP3/FLAC/MP4文件，后端 HTTP Range 流式接口，前端 APlayer / react-player  
**前端架构**：前后端分离 · 两个独立React项目（`admin-frontend` + `room-frontend`）· Vite构建  
**目标时间线**：约24个工作日（+2天媒体播放模块）

---

## 阶段一：项目骨架搭建（M1）

### [x] 任务1：初始化后端Spring Boot项目 ✅ 已完成 (2026-03-30)
**描述**：使用Maven创建Spring Boot 3.2.x项目（JDK 21），配置基础依赖，并配置CORS支持前端跨域
**验收标准**：
- 项目能正常启动，端口8080可访问
- 包含所有必要依赖：Web、MyBatis-Plus、MySQL驱动、Redis、Lombok、jjwt
- 配置CORS允许 `localhost:3000`、`localhost:3001` 跨域访问
- 注意：Spring Boot 3 使用 `jakarta.*` 包名（不再是 `javax.*`）

**要创建/编辑的文件**：
- `pom.xml`（核心依赖配置）
- `src/main/resources/application.yml`（数据库+Redis配置）
- `config/CorsConfig.java`（跨域配置）
- `KtvApplication.java`（启动类）

**关键依赖**：
```xml
spring-boot-starter-web
mybatis-plus-boot-starter (3.5.7+，支持Spring Boot 3)
mysql-connector-j (8.0.x，注意artifactId从mysql-connector-java改为mysql-connector-j)
spring-boot-starter-data-redis
lombok
jjwt-api (0.11.x+，支持Jakarta EE)
com.github.houbb:pinyin (拼音转换，替代已停更的pinyin4j)
<!-- 媒体文件上传 - Spring Boot 自带，无需额外依赖 -->
<!-- 流媒体传输 - 使用 ResourceRegion，Spring MVC 原生支持 HTTP Range -->
```

**完成情况**：
- ✅ Maven项目结构创建完成
- ✅ pom.xml依赖配置完成（Spring Boot 3.2.5、MyBatis-Plus 3.5.7、mysql-connector-j等）
- ✅ application.yml配置完成（数据库、Redis、媒体路径等）
- ✅ KtvApplication.java启动类创建完成
- ✅ CorsConfig.java跨域配置完成
- ✅ HealthController.java健康检查接口创建完成
- ✅ README.md和SETUP.md文档创建完成

---

### [x] 任务2：数据库建表 ✅ 已完成 (2026-03-30)
**描述**：按数据库设计文档创建所有表，并插入基础测试数据  
**验收标准**：
- 7张表全部创建成功，无报错
- 测试数据：5个歌手、20首歌曲、5个分类、3个包厢
- 所有字段类型、索引与设计文档一致

**要创建/编辑的文件**：
- `sql/init-schema.sql`（建表语句）
- `sql/init-data.sql`（测试数据）

**参考**：`docs/project-overview.md` 第四章数据库设计

**完成情况**：
- ✅ `sql/init-schema.sql` - 8张表DDL（7张核心表+1张日志表）
  - t_singer：歌手表，含拼音首字母索引、地区索引
  - t_category：分类表，含排序索引
  - t_song：歌曲表，含歌手ID/分类ID/拼音/语种/热度等多索引，复合索引优化热门排行查询
  - t_room：包厢表，含状态/类型索引
  - t_order：订单表，含订单号唯一索引、包厢ID/状态/时间索引
  - t_order_song：点歌记录表，含订单ID/歌曲ID/状态索引，复合索引优化队列查询
  - t_sys_user：系统用户表，含用户名唯一索引
  - t_operation_log：操作日志表（审计用）
- ✅ `sql/init-data.sql` - 初始数据
  - 系统用户：admin/operator（密码 admin123，BCrypt加密）
  - 歌曲分类：8个（流行/经典/摇滚/民谣/电子/R&B/说唱/儿歌）
  - 歌手：23位（内地/港台/欧美/日韩）
  - 歌曲：64首（周杰伦/林俊杰/邓紫棋/五月天/Taylor Swift等热门歌曲）
  - 包厢：10个（小包3/中包3/大包2/豪华包2）
- ✅ Java实体类（9个）
  - BaseEntity、Singer、Category、Song、Room、Order、OrderSong、SysUser、OperationLog
- ✅ MyBatisPlusConfig.java - 分页插件+自动填充配置

---

### [x] 任务3：公共基础类搭建 ✅ 已完成 (2026-03-30)
**描述**：创建统一返回结果类、全局异常处理、MyBatis-Plus分页配置、Redis配置类
**验收标准**：
- `Result<T>` 类支持 success/fail 静态方法，包含 code/msg/data
- 全局异常拦截器能捕获 `BusinessException` 并返回标准格式
- MyBatis-Plus 分页插件已配置
- Redis StringRedisTemplate 和 RedisTemplate 均可注入使用

**要创建/编辑的文件**：
- `common/result/Result.java`
- `common/result/ResultCode.java`（枚举：SUCCESS/FAIL/NOT_FOUND等）
- `common/exception/BusinessException.java`
- `common/exception/GlobalExceptionHandler.java`
- `config/MyBatisPlusConfig.java`
- `config/RedisConfig.java`

**完成情况**：
- ✅ ResultCode.java - 统一返回状态码枚举（SUCCESS、FAIL、UNAUTHORIZED等）
- ✅ Result.java - 统一返回结果类（支持泛型，提供success/fail等静态方法）
- ✅ BusinessException.java - 业务异常类
- ✅ GlobalExceptionHandler.java - 全局异常处理器（处理业务异常、参数校验异常等）
- ✅ MyBatisPlusConfig.java - MyBatis-Plus配置（分页插件、乐观锁、防全表更新）
- ✅ RedisConfig.java - Redis配置（Jackson序列化、缓存管理器）
- ✅ JwtUtil.java - JWT工具类（额外添加，后续任务需要）
- ✅ PinyinUtil.java - 拼音工具类（额外添加，用于歌曲搜索）
- ✅ TestController.java - 测试控制器（用于验证基础类功能）

---

## 阶段二：后台管理模块（M2）

### [x] 任务4：歌手管理CRUD ✅ 已完成 (2026-03-30)
**描述**：实现歌手的增删改查接口，包含分页查询和按名称/地区筛选
**验收标准**：
- `GET /api/admin/singers` 返回分页数据，支持 name/region 参数筛选
- `POST /api/admin/singers` 新增歌手，参数校验（name必填）
- `PUT /api/admin/singers/{id}` 修改歌手信息
- `DELETE /api/admin/singers/{id}` 逻辑删除
- 自动填充 `create_time`、`update_time`

**要创建/编辑的文件**：
- `entity/Singer.java`
- `mapper/SingerMapper.java` + `SingerMapper.xml`
- `service/SingerService.java` + `impl/SingerServiceImpl.java`
- `controller/admin/SingerController.java`
- `dto/SingerDTO.java`（入参）、`vo/SingerVO.java`（出参）

**完成情况**：
- ✅ Singer.java - 歌手实体类（使用MyBatis-Plus注解）
- ✅ SingerDTO.java - 歌手DTO（新增/修改入参，包含参数校验）
- ✅ SingerVO.java - 歌手VO（查询出参）
- ✅ SingerMapper.java - Mapper接口
- ✅ SingerMapper.xml - Mapper XML（实现带筛选条件的分页查询）
- ✅ SingerService.java - Service接口
- ✅ SingerServiceImpl.java - Service实现类（包含拼音自动生成、重复校验、歌曲数量检查等业务逻辑）
- ✅ SingerController.java - Controller（提供5个RESTful API接口）
- ✅ MetaObjectHandlerConfig.java - 自动填充配置（自动填充createTime和updateTime）

**API接口列表**：
- GET /api/admin/singers - 分页查询歌手列表（支持name/region筛选）
- POST /api/admin/singers - 新增歌手（自动生成拼音）
- GET /api/admin/singers/{id} - 根据ID查询歌手详情
- PUT /api/admin/singers/{id} - 修改歌手信息
- DELETE /api/admin/singers/{id} - 逻辑删除歌手（检查是否有歌曲）

---

### [x] 任务5：歌曲分类管理CRUD ✅ 已完成 (2026-03-30)
**描述**：实现歌曲分类的增删改查（简单列表，不分页）  
**验收标准**：
- `GET /api/admin/categories` 返回所有启用分类，按 sort_order 排序
- `POST /api/admin/categories` 新增分类
- `PUT /api/admin/categories/{id}` 修改（含调整排序）
- `DELETE /api/admin/categories/{id}` 删除（若分类下有歌曲则提示不可删除）

**要创建/编辑的文件**：
- `entity/Category.java`（已存在）
- `mapper/CategoryMapper.java` + `CategoryMapper.xml`
- `service/CategoryService.java` + `impl/CategoryServiceImpl.java`
- `controller/admin/CategoryController.java`
- `dto/CategoryDTO.java`
- `vo/CategoryVO.java`

**完成情况**：
- ✅ CategoryMapper.java - Mapper接口
- ✅ CategoryMapper.xml - Mapper XML（实现检查分类下歌曲数量）
- ✅ CategoryDTO.java - 分类DTO（入参，含参数校验）
- ✅ CategoryVO.java - 分类VO（出参，含状态文本）
- ✅ CategoryService.java - Service接口
- ✅ CategoryServiceImpl.java - Service实现类（包含重复校验、歌曲数量检查等业务逻辑）
- ✅ CategoryController.java - Controller（提供6个RESTful API接口）

**API接口列表**：
- GET /api/admin/categories - 获取所有启用分类列表（按sort_order排序）
- GET /api/admin/categories/all - 获取所有分类列表（管理员用，包含禁用的）
- POST /api/admin/categories - 新增分类
- GET /api/admin/categories/{id} - 根据ID查询分类详情
- PUT /api/admin/categories/{id} - 修改分类（含调整排序）
- DELETE /api/admin/categories/{id} - 删除分类（检查是否有歌曲）

---

### [x] 任务6：歌曲管理CRUD ✅ 已完成 (2026-03-30)
**描述**：实现歌曲的增删改查，含拼音自动生成、关联歌手/分类
**验收标准**：
- `GET /api/admin/songs` 分页查询，支持 name/singerId/categoryId/language 筛选
- `POST /api/admin/songs` 新增时自动用 pinyin4j 生成 pinyin 和 pinyin_initial
- `PUT /api/admin/songs/{id}` 修改歌曲
- `DELETE /api/admin/songs/{id}` 软删除，同时更新歌手的 song_count
- 新增/修改后刷新Redis中该歌曲的缓存

**要创建/编辑的文件**：
- `entity/Song.java`
- `mapper/SongMapper.java` + `SongMapper.xml`（含关联查询）
- `service/SongService.java` + `impl/SongServiceImpl.java`
- `controller/admin/SongController.java`
- `dto/SongDTO.java`、`vo/SongVO.java`（含歌手名、分类名）
- `util/PinyinUtil.java`（封装pinyin4j转换）

**完成情况**：
- ✅ Song.java - 歌曲实体类（使用MyBatis-Plus注解）
- ✅ SongDTO.java - 歌曲DTO（新增/修改入参，包含参数校验）
- ✅ SongVO.java - 歌曲VO（查询出参，包含歌手名和分类名）
- ✅ SongMapper.java - Mapper接口
- ✅ SongMapper.xml - Mapper XML（实现带筛选条件的分页查询，关联查询歌手名和分类名）
- ✅ SongService.java - Service接口
- ✅ SongServiceImpl.java - Service实现类（包含拼音自动生成、业务校验、Redis缓存、歌手歌曲数量更新等业务逻辑）
- ✅ SongController.java - Controller（提供5个RESTful API接口）
- ✅ PinyinUtil.java - 拼音工具类（已在任务3中创建）

**API接口列表**：
- GET /api/admin/songs - 分页查询歌曲列表（支持name/singerId/categoryId/language筛选）
- POST /api/admin/songs - 新增歌曲（自动生成拼音，更新歌手歌曲数量）
- GET /api/admin/songs/{id} - 根据ID查询歌曲详情（从Redis缓存读取）
- PUT /api/admin/songs/{id} - 修改歌曲（更新歌手歌曲数量，刷新Redis缓存）
- DELETE /api/admin/songs/{id} - 逻辑删除歌曲（更新歌手歌曲数量，清除Redis缓存）

**Redis集成**：
- 新增/修改歌曲后自动刷新缓存
- 查询歌曲详情时优先从Redis读取
- 删除歌曲时清除缓存
- 缓存Key：song:{id}

---

### [x] 任务7：包厢管理CRUD ✅ 已完成 (2026-03-30)
**描述**：实现包厢的增删改查和状态管理  
**验收标准**：
- `GET /api/admin/rooms` 列表查询，支持按 status/type 筛选
- `POST /api/admin/rooms` 新增包厢
- `PUT /api/admin/rooms/{id}` 修改包厢信息
- `DELETE /api/admin/rooms/{id}` 仅允许删除状态为"空闲"的包厢
- `PUT /api/admin/rooms/{id}/status` 更新包厢状态（同步更新Redis中的状态快照）

**要创建/编辑的文件**：
- `entity/Room.java`（已存在）
- `mapper/RoomMapper.java`
- `service/RoomService.java` + `impl/RoomServiceImpl.java`
- `controller/admin/RoomController.java`
- `dto/RoomDTO.java`
- `vo/RoomVO.java`

**完成情况**：
- ✅ RoomMapper.java - Mapper接口
- ✅ RoomDTO.java - 包厢DTO（入参，含参数校验）
- ✅ RoomVO.java - 包厢VO（出参，含状态文本）
- ✅ RoomService.java - Service接口
- ✅ RoomServiceImpl.java - Service实现类（包含重复校验、状态检查、Redis状态同步等业务逻辑）
- ✅ RoomController.java - Controller（提供7个RESTful API接口）

**API接口列表**：
- GET /api/admin/rooms - 获取包厢列表（支持status/type筛选）
- GET /api/admin/rooms/available - 获取空闲包厢列表（用于开台）
- POST /api/admin/rooms - 新增包厢
- GET /api/admin/rooms/{id} - 根据ID查询包厢详情
- PUT /api/admin/rooms/{id} - 修改包厢信息
- DELETE /api/admin/rooms/{id} - 删除包厢（仅允许空闲状态）
- PUT /api/admin/rooms/{id}/status - 更新包厢状态（同步Redis）

---

### [x] 任务8：管理员登录与JWT鉴权 ✅ 已完成 (2026-03-30)
**描述**：实现后台管理员登录/登出，基于JWT Token的接口保护（前后端分离方案）
**验收标准**：
- `POST /api/admin/login` 用户名密码校验，密码BCrypt验证，登录成功返回JWT Token
- `POST /api/admin/logout` 前端清除Token即可（无需服务端操作）
- JWT拦截器拦截所有 `/api/admin/**`（除login外），Token无效返回401
- Token有效期2小时，包含 userId/username/role 载荷
- 初始化一个超级管理员账号（admin/admin123）

**要创建/编辑的文件**：
- `entity/SysUser.java`
- `mapper/SysUserMapper.java`
- `service/SysUserService.java` + `impl/SysUserServiceImpl.java`
- `controller/admin/AuthController.java`
- `config/WebMvcConfig.java`（注册JWT拦截器）
- `interceptor/JwtInterceptor.java`
- `util/JwtUtil.java`（Token生成/解析工具）

**完成情况**：
- ✅ SysUser.java - 系统用户实体类（已存在）
- ✅ LoginDTO.java - 登录DTO
- ✅ LoginVO.java - 登录响应VO
- ✅ JwtInterceptor.java - JWT拦截器
- ✅ WebMvcConfig.java - Web MVC配置（注册JWT拦截器）
- ✅ SysUserService.java - 系统用户Service接口
- ✅ SysUserServiceImpl.java - 系统用户Service实现（BCrypt密码验证、JWT生成、更新登录信息）
- ✅ AuthController.java - 认证Controller（登录/登出接口）
- ✅ JwtUtil.java - JWT工具类（已在任务3中创建）
- ✅ pom.xml - 添加spring-security-crypto依赖（BCrypt）

**API接口列表**：
- POST /api/admin/login - 用户登录（返回JWT Token）
- POST /api/admin/logout - 用户登出（前端清除Token即可）

**JWT鉴权**：
- 拦截所有 `/api/admin/**` 接口（除login、logout、health外）
- Token验证：Bearer Token格式
- Token无效或过期返回401 Unauthorized
- Token有效期：2小时
- Token载荷：userId、username、role
- 验证通过后将用户信息存入请求属性

**安全特性**：
- BCrypt密码加密
- JWT Token无状态认证
- 统一异常处理（401 UNAUTHORIZED）
- 获取客户端真实IP（支持X-Forwarded-For、X-Real-IP）
- 更新最后登录时间和IP

---

## 阶段三：订单管理模块（M3）

### [x] 任务9：开台与结账 ✅ 已完成 (2026-03-30)
**描述**：实现包厢开台创建订单和结账关闭订单的完整流程  
**验收标准**：
- `POST /api/admin/orders/open` 选择空闲包厢开台，创建订单，包厢状态→"使用中"
- `POST /api/admin/orders/{id}/close` 结账，计算时长和费用，包厢状态→"清洁中"
- 开台时订单编号自动生成（格式：`KTV` + 日期 + 6位序号）
- 开台时清空该包厢的Redis排队队列（防止历史数据残留）

**要创建/编辑的文件**：
- `entity/Order.java`（已存在）
- `mapper/OrderMapper.java` + `OrderMapper.xml`
- `service/OrderService.java` + `impl/OrderServiceImpl.java`
- `controller/admin/OrderController.java`
- `util/OrderNoUtil.java`（订单号生成工具）
- `dto/OrderOpenDTO.java`
- `vo/OrderVO.java`

**完成情况**：
- ✅ OrderMapper.java + OrderMapper.xml - Mapper层
- ✅ OrderOpenDTO.java - 开台入参DTO
- ✅ OrderVO.java - 订单VO（出参，含状态文本、包厢名、操作员名）
- ✅ OrderNoUtil.java - 订单号生成工具
- ✅ OrderService.java + OrderServiceImpl.java - 包含开台/结账核心逻辑
- ✅ OrderController.java - Controller

**API接口**：
- POST /api/admin/orders/open - 开台
- POST /api/admin/orders/{id}/close - 结账
- GET /api/admin/orders - 分页查询
- GET /api/admin/orders/{id} - 订单详情
- DELETE /api/admin/orders/{id} - 取消订单
- GET /api/admin/orders/room/{roomId}/active - 获取包厢进行中订单

**核心业务逻辑**：
- ✅ 开台前检查包厢状态（仅空闲可开台）
- ✅ 订单编号自动生成（KTVyyyyMMdd + 6位序号）
- ✅ 开台时清空Redis排队队列
- ✅ 结账时计算时长和费用
- ✅ 支持取消订单

---

### [x] 任务10：订单列表查询 ✅ 已完成 (2026-03-30)
**描述**：实现订单分页查询，支持按日期/包厢/状态筛选
**验收标准**：
- `GET /api/admin/orders` 分页返回订单列表
- 支持查询参数：startDate/endDate/roomId/status
- 返回数据包含：订单号、包厢名、开台时间、结账时间、时长、费用、状态

**要创建/编辑的文件**：
- `OrderMapper.xml`（多条件分页查询SQL）
- `vo/OrderVO.java`

**完成情况**：
- ✅ OrderVO.java - 订单VO（查询出参，包含包厢名、状态文本、时长描述）
- ✅ OrderMapper.xml - Mapper XML（实现带筛选条件的分页查询SQL，关联查询包厢名和操作员名）
- ✅ OrderService.java - Service接口
- ✅ OrderServiceImpl.java - Service实现类（实现订单分页查询）
- ✅ OrderController.java - Controller（提供订单分页查询API）

**API接口列表**：
- GET /api/admin/orders - 分页查询订单列表（支持startDate/endDate/roomId/status筛选）

**关联查询**：
- LEFT JOIN包厢表（t_room）获取包厢名和类型
- LEFT JOIN系统用户表（t_sys_user）获取操作员姓名
- 一次查询获取所有关联信息，避免N+1查询问题

**筛选条件**：
- startDate：开台开始日期
- endDate：开台结束日期
- roomId：按包厢ID筛选
- status：按订单状态筛选（1消费中 2已结账 3已取消）

**返回字段**：
- 订单号、包厢名、包厢类型
- 开台时间、结账时间、消费时长、时长描述
- 包厢费用、总费用
- 状态、状态描述
- 操作员ID、操作员姓名
- 创建时间

---

## 阶段四：包厢点歌核心（M4）

### [x] 任务11：歌曲检索接口 ✅ 已完成 (2026-03-30)
**描述**：实现点歌界面的多维度歌曲搜索功能  
**验收标准**：
- `GET /api/room/songs/search?keyword=xxx` 按歌曲名或拼音首字母模糊搜索
- `GET /api/room/songs/by-singer/{singerId}` 返回该歌手所有歌曲（分页）
- `GET /api/room/songs/by-category/{categoryId}` 按分类返回歌曲（分页）
- `GET /api/room/singers` 返回所有歌手列表，支持按 pinyin_initial 筛选
- `GET /api/room/categories` 返回所有分类

**要创建/编辑的文件**：
- `controller/room/SongSearchController.java`
- `service/SongSearchService.java` + `impl/SongSearchServiceImpl.java`
- `SongMapper.java`（添加检索方法）
- `SongMapper.xml`（补充检索SQL）

**完成情况**：
- ✅ SongSearchController.java - Controller（5个API接口）
- ✅ SongSearchService.java - Service接口
- ✅ SongSearchServiceImpl.java - Service实现类
- ✅ SongMapper.java - 添加3个检索方法
- ✅ SongMapper.xml - 添加3个检索SQL

**API接口列表**：
- GET /api/room/songs/search - 按歌曲名/拼音首字母搜索（分页）
- GET /api/room/songs/by-singer/{singerId} - 按歌手查询歌曲（分页）
- GET /api/room/songs/by-category/{categoryId} - 按分类查询歌曲（分页）
- GET /api/room/singers - 获取所有歌手列表
- GET /api/room/categories - 获取所有分类列表

**搜索功能特点**：
- ✅ 支持歌曲名模糊搜索
- ✅ 支持拼音首字母搜索
- ✅ 支持拼音全拼搜索
- ✅ 搜索结果按匹配度排序（开头匹配优先）
- ✅ 仅返回已上架(status=1)的歌曲
- ✅ 歌手和分类支持按拼音首字母筛选

---

### [x] 任务12：点歌与排队管理 ✅ 已完成 (2026-03-30)
**描述**：实现点歌加队列、置顶、取消点歌等核心操作（Redis List队列）
**验收标准**：
- `POST /api/room/{orderId}/queue/add` 点歌，写入DB(`t_order_song`)，同时将ID推入Redis List
- `POST /api/room/{orderId}/queue/top/{orderSongId}` 置顶：从List移除后插入List头部
- `DELETE /api/room/{orderId}/queue/remove/{orderSongId}` 取消：从Redis List和DB中移除
- `GET /api/room/{orderId}/queue` 返回当前排队列表（含歌曲名、歌手名）
- `GET /api/room/{orderId}/played` 返回已唱列表

**要创建/编辑的文件**：
- `entity/OrderSong.java`
- `mapper/OrderSongMapper.java`
- `service/PlayQueueService.java` + `impl/PlayQueueServiceImpl.java`
- `controller/room/PlayQueueController.java`

**完成情况**：
- ✅ OrderSong.java - 点歌记录实体类（已存在，包含状态判断方法）
- ✅ OrderSongMapper.java - Mapper接口
- ✅ OrderSongMapper.xml - Mapper XML（关联查询歌曲时长和文件路径）
- ✅ PlayQueueService.java - Service接口
- ✅ PlayQueueServiceImpl.java - Service实现类（实现Redis List队列管理）
- ✅ PlayQueueController.java - Controller（提供5个RESTful API接口）

**API接口列表**：
- POST /api/room/{orderId}/queue/add - 点歌（写入DB和Redis List）
- POST /api/room/{orderId}/queue/top/{orderSongId} - 置顶（移到队列头部）
- DELETE /api/room/{orderId}/queue/remove/{orderSongId} - 取消点歌（从队列和DB移除）
- GET /api/room/{orderId}/queue - 查询排队列表（状态=0等待中）
- GET /api/room/{orderId}/queue/played - 查询已唱列表（状态=2已播放/3已跳过）

**Redis队列管理**：
- 队列Key格式：ktv:queue:{orderId}
- 队列过期时间：24小时
- 队列操作：rightPush（点歌）、leftPush（置顶）、remove（取消）
- 事务支持：@Transactional确保DB和Redis数据一致性

**业务逻辑**：
- 点歌：查询歌曲信息 → 获取队列序号 → 创建点歌记录 → 推入Redis队列
- 置顶：验证记录状态 → 从队列移除 → 插入队列头部 → 更新数据库序号
- 取消：验证记录归属 → 从队列移除 → 删除数据库记录
- 查询：按订单ID和状态分页查询

**数据冗余**：
- song_name：歌曲名冗余，防止歌曲被删除后无法显示
- singer_name：歌手名冗余

---

### [x] 任务13：播放控制接口 ✅ 已完成 (2026-03-30)
**描述**：实现切歌、重唱、暂停/继续和当前播放状态查询
**验收标准**：
- `POST /api/room/{orderId}/play/next` 将当前歌曲标记为"已播放"，取队列下一首设为"播放中"，更新Redis
- `POST /api/room/{orderId}/play/replay` 重置当前歌曲状态为"播放中"（重唱不换歌）
- `POST /api/room/{orderId}/play/pause` 更新Redis中播放状态为"已暂停"
- `POST /api/room/{orderId}/play/resume` 恢复播放状态
- `GET /api/room/{orderId}/play/current` 返回当前播放歌曲信息、队列剩余数量

**要创建/编辑的文件**：
- `service/PlayControlService.java` + `impl/PlayControlServiceImpl.java`
- `controller/room/PlayControlController.java`
- Redis Key设计：`ktv:playing:{orderId}` (String)、`ktv:play:status:{orderId}` (String: PLAYING/PAUSED)

**完成情况**：
- ✅ PlayControlService.java - Service接口
- ✅ PlayControlServiceImpl.java - Service实现类（实现切歌、重唱、暂停、恢复、状态查询）
- ✅ CurrentPlayVO.java - 当前播放状态VO（含播放状态、歌曲信息、队列剩余数量）
- ✅ PlayControlController.java - Controller（提供5个RESTful API接口）
- ✅ OrderSongMapper.java - 新增findSongInfoById方法
- ✅ OrderSongMapper.xml - 新增findSongInfoById SQL（关联查询歌曲时长和文件路径）

**API接口列表**：
- POST /api/room/{orderId}/play/next - 切歌（下一首）
- POST /api/room/{orderId}/play/replay - 重唱
- POST /api/room/{orderId}/play/pause - 暂停播放
- POST /api/room/{orderId}/play/resume - 恢复播放
- GET /api/room/{orderId}/play/current - 查询当前播放状态

**Redis Key设计**：
- ktv:playing:{orderId} - 存储当前播放的点歌记录ID（String，过期24小时）
- ktv:play:status:{orderId} - 存储播放状态（String：PLAYING/PAUSED，过期24小时）

**业务逻辑**：
- 切歌：标记当前歌曲为"已播放" → 从队列取下一首 → 设为"播放中" → 更新Redis → 恢复播放状态
- 重唱：重置当前歌曲播放时间 → 恢复播放状态（不换歌）
- 暂停：更新Redis播放状态为"已暂停"
- 恢复：检查是否有播放歌曲 → 更新Redis播放状态为"播放中"
- 状态查询：获取播放状态 → 获取当前歌曲信息 → 获取队列剩余数量

**数据一致性**：
- 切歌使用@Transactional确保DB和Redis操作原子性
- Redis Key设置24小时过期，自动清理

---

## 阶段五：Redis集成优化（M5）

### [x] 任务14：热门排行榜 ✅ 已完成 (2026-03-30)
**描述**：用Redis ZSet维护热门歌曲排行，每次点歌自动+1分  
**验收标准**：
- `GET /api/room/songs/hot?limit=20` 返回热门歌曲TOP列表（从Redis ZSet读取）
- 每次点歌调用 `zincrby ktv:song:hot 1 {songId}`
- 每天00:00自动同步Redis ZSet分数到DB的 `play_count` 字段（Spring @Scheduled）
- 若Redis中热门榜为空，从DB读取 play_count TOP 50 预热

**完成情况**：
- ✅ HotSongService.java - Service接口
- ✅ HotSongServiceImpl.java - Service实现类（ZSet操作、预热、同步）
- ✅ HotSongController.java - Controller（GET /api/room/songs/hot）
- ✅ PlayCountSyncTask.java - 定时任务（每天00:00同步到DB）
- ✅ PlayQueueServiceImpl.java - 点歌时调用incrementHotScore
- ✅ KtvApplication.java - 添加@EnableScheduling启用定时任务

**Redis Key设计**：
- `ktv:song:hot` - 热门歌曲ZSet（songId为member，playCount为score）

**API接口**：
- GET /api/room/songs/hot?limit=20 - 获取热门歌曲排行榜

**定时任务**：
- 每天凌晨00:00将Redis ZSet分数同步到数据库play_count字段

**业务逻辑**：
- 点歌时：zincrby ktv:song:hot 1 {songId}
- 查询热门榜：优先读Redis，ZSet为空则预热
- 预热：从DB读取play_count TOP 50写入Redis
- 同步：每天凌晨将Redis分数写回数据库

---

### [x] 任务15：歌曲缓存 ✅ 已完成 (2026-03-30)
**描述**：对频繁读取的歌曲详情做Redis缓存，减少DB压力  
**验收标准**：
- 查询单首歌曲时，优先读Redis缓存（`song:{songId}`），缓存未命中才查DB
- 歌曲信息被修改/删除时，主动删除对应Redis缓存
- 缓存TTL设置为1小时

**完成情况**：
- ✅ SongServiceImpl.java - 添加CACHE_TTL_HOURS常量（1小时）
- ✅ getSongById方法 - 缓存查询结果设置1小时TTL
- ✅ refreshSongCache方法 - 刷新缓存也设置1小时TTL
- ✅ deleteSong方法 - 删除歌曲时清除Redis缓存
- ✅ RedisConfig.java - Jackson序列化已配置（任务3完成）

**缓存Key设计**：
- `song:{id}` - 歌曲详情缓存（TTL: 1小时）

**业务逻辑**：
- 查询：优先读Redis缓存，未命中查DB并写入缓存
- 新增/修改：调用refreshSongCache刷新缓存
- 删除：删除数据库记录时清除Redis缓存

---

## 阶段六：联调测试（M6）

### [ ] 任务16：初始化测试数据集
**描述**：补充充足的测试数据，覆盖常见测试场景  
**验收标准**：
- 歌手 ≥ 20条，覆盖内地/港台/欧美/日韩
- 歌曲 ≥ 100条，覆盖各分类和语种
- 包厢 ≥ 5个，覆盖小/中/大/豪华各类型

**要创建/编辑的文件**：
- `sql/test-data.sql`

---

### [ ] 任务17：接口整体联调与Bug修复
**描述**：按业务流程（开台→点歌→排队→切歌→结账）完整联调，修复发现的问题  
**验收标准**：
- 完整业务流程无报错
- 边界场景测试：空队列切歌、重复点同一首歌、并发点歌
- 所有P0接口响应时间达标（搜索≤200ms，点歌≤100ms）

---

## 质量要求

- [ ] 所有后端接口统一返回 `Result<T>` 格式
- [ ] 数据库操作使用MyBatis-Plus，避免手写基础CRUD
- [ ] Redis操作做好异常降级（Redis不可用时走DB）
- [ ] 分页接口统一使用 pageNum + pageSize 参数
- [ ] 逻辑删除字段 deleted=1，MyBatis-Plus @TableLogic 注解
- [ ] 拼音搜索支持多音字（使用 com.github.houbb:pinyin 库，取第一音）
- [ ] 图片/封面 URL 来自实际可访问地址（或留空使用默认图）
- [ ] React 组件使用函数组件 + Hooks，禁用 Class 组件
- [ ] Axios 统一封装请求/响应拦截器（自动附加 Token，处理401跳转登录）
- [ ] 前端表单提交时做必填校验（Ant Design Form校验规则）

---

## 前端开发任务

### 阶段七：后台管理前端（M3）

#### [x] 任务F1：admin-frontend 项目初始化 ✅ 已完成 (2026-03-30)
**描述**：使用 Vite + React 18 初始化后台管理前端项目，配置基础工程化环境  
**验收标准**：
- `npm run dev` 能在 `:3000` 正常启动
- Vite devServer配置API代理：`/api` → `http://localhost:8080`
- 已安装依赖：antd 5.x、axios、react-router-dom v6、zustand
- 配置 Axios 封装（`src/api/request.js`）：自动读取localStorage的Token并放入请求头，401时跳转登录

**要创建/编辑的文件**：
- `admin-frontend/package.json`
- `admin-frontend/vite.config.js`（含代理配置）
- `admin-frontend/src/api/request.js`（Axios封装）
- `admin-frontend/src/router/index.jsx`（路由配置）
- `admin-frontend/src/store/userStore.js`（Zustand用户状态）

**完成情况**：
- ✅ Vite + React 18 项目创建完成，端口3000
- ✅ vite.config.js 配置 API 代理 `/api` → `http://localhost:8080`
- ✅ 安装 antd 5.x、axios、react-router-dom v6、zustand、dayjs
- ✅ src/api/request.js Axios 封装（自动 Token 附加、401 跳转）
- ✅ src/store/userStore.js Zustand 持久化状态管理
- ✅ src/router/index.jsx 路由配置（路由守卫 + 懒加载）
- ✅ src/api/ 所有 API 模块（auth、singer、song、category、room、order）
- ✅ 创建完整目录结构（api、pages、components、store、router、layouts）

---

#### [x] 任务F2：登录页面 ✅ 已完成 (2026-03-30)
**描述**：实现后台登录页，调用登录接口，成功后保存Token并跳转  
**验收标准**：
- 页面有用户名、密码输入框和登录按钮
- 调用 `POST /api/admin/login`，成功后将Token存入localStorage
- 登录失败显示错误提示
- 已登录用户直接访问登录页时自动跳转到首页

**要创建/编辑的文件**：
- `admin-frontend/src/pages/Login/index.jsx`
- `admin-frontend/src/api/auth.js`

**完成情况**：
- ✅ 完整登录页面（用户名+密码表单、渐变背景样式）
- ✅ 调用 POST /api/admin/login，成功保存 JWT Token 到 localStorage
- ✅ 登录失败显示 Ant Design Message 错误提示
- ✅ 已登录用户访问登录页自动跳转首页

---

#### [x] 任务F3：后台主布局（侧边栏导航） ✅ 已完成 (2026-03-30)
**描述**：实现带侧边栏导航的主布局，包含菜单项和顶部用户信息  
**验收标准**：
- 使用 Ant Design `Layout + Menu + Sider` 组件
- 侧边菜单包含：歌曲管理、歌手管理、分类管理、包厢管理、订单管理
- 顶部显示当前登录用户名和退出按钮
- 路由切换时菜单高亮对应项

**要创建/编辑的文件**：
- `admin-frontend/src/layouts/AdminLayout.jsx`
- `admin-frontend/src/components/SideMenu/index.jsx`

**完成情况**：
- ✅ Ant Design Layout + Menu + Sider 侧边栏布局
- ✅ 侧边菜单含歌曲/歌手/分类/包厢/订单管理，路由切换高亮
- ✅ 顶部显示当前登录用户名和退出按钮
- ✅ 退出登录清除 Token，跳转登录页

---

#### [x] 任务F4：歌手管理页面 ✅ 已完成 (2026-03-30)
**描述**：实现歌手列表（搜索+分页）、新增/编辑（弹窗表单）、删除功能  
**验收标准**：
- 表格展示歌手列表，支持按名称和地区筛选
- 点击"新增"弹出Modal表单，填写后调用新增接口
- 点击"编辑"回填当前行数据，提交调用编辑接口
- 点击"删除"弹出确认框，确认后调用删除接口，刷新列表
- 操作成功/失败显示 Ant Design Message 提示

**要创建/编辑的文件**：
- `admin-frontend/src/pages/Singer/index.jsx`
- `admin-frontend/src/api/singer.js`

**完成情况**：
- ✅ 歌手列表表格（分页，支持名称/地区筛选）
- ✅ 新增/编辑弹窗表单（参数校验）
- ✅ 删除歌手（确认对话框）
- ✅ 操作成功/失败 Ant Design Message 提示

---

#### [x] 任务F5：歌曲管理页面 ✅ 已完成 (2026-03-30)
**描述**：实现歌曲列表（多条件搜索+分页）、新增/编辑（含歌手/分类下拉选择）、删除  
**验收标准**：
- 表格展示歌曲（歌曲名、歌手、分类、语种、状态）
- 搜索栏支持歌曲名、歌手、分类、语种多条件筛选
- 新增/编辑弹窗中歌手、分类为下拉选择（调接口获取列表）
- 表格中状态列用 Badge/Tag 区分上架/下架
- 支持单行删除

**要创建/编辑的文件**：
- `admin-frontend/src/pages/Song/index.jsx`
- `admin-frontend/src/api/song.js`

**完成情况**：
- ✅ 歌曲列表表格（分页，多条件搜索：名称/歌手/分类/语种）
- ✅ 新增/编辑弹窗，歌手和分类为可搜索下拉选择
- ✅ 状态列 Tag 区分上架/下架
- ✅ 支持单行删除

---

#### [x] 任务F6：包厢管理页面 ✅ 已完成 (2026-03-30)
**描述**：实现包厢列表、新增/编辑、删除，以及包厢状态的快速切换  
**验收标准**：
- 表格展示包厢（名称、类型、容量、价格、状态）
- 状态列用不同颜色Tag区分：空闲（绿）、使用中（红）、清洁中（橙）
- 支持新增/编辑/删除操作
- 操作列提供"修改状态"快捷按钮

**要创建/编辑的文件**：
- `admin-frontend/src/pages/Room/index.jsx`
- `admin-frontend/src/api/room.js`

**完成情况**：
- ✅ 包厢列表表格（分页，支持名称/状态/类型筛选）
- ✅ 状态 Tag 颜色区分（空闲绿/使用中红/清洁中橙/维修中灰）
- ✅ 新增/编辑/删除操作
- ✅ 操作列"修改状态"快捷按钮

---

#### [x] 任务F7：订单管理页面 ✅ 已完成 (2026-03-30)
**描述**：实现订单列表查询、开台操作（选包厢）、结账操作  
**验收标准**：
- 表格展示订单列表，支持按日期范围和状态筛选
- "开台"按钮弹出包厢选择框（仅展示空闲包厢），确认后调用开台接口
- 消费中订单的操作列显示"结账"按钮，点击确认后结账
- 已结账订单显示时长和金额

**要创建/编辑的文件**：
- `admin-frontend/src/pages/Order/index.jsx`
- `admin-frontend/src/api/order.js`

**完成情况**：
- ✅ 订单列表表格（分页，支持日期范围/状态筛选）
- ✅ "开台"弹窗选择空闲包厢，确认后调用开台接口
- ✅ 消费中订单显示"结账"按钮，确认后结账
- ✅ 已结账订单展示时长和费用
- ✅ 取消订单功能
- ✅ 查看订单详情弹窗

---

### 阶段八：包厢点歌前端（M6）

#### [x] 任务F8：room-frontend 项目初始化 ✅ 已完成 (2026-03-31)
**描述**：使用 Vite + React 18 初始化包厢端前端项目，配置触摸友好的UI环境  
**验收标准**：
- `npm run dev` 能在 `:3001` 正常启动
- 安装依赖：antd-mobile 5.x（移动端组件库）、axios、react-router-dom v6、zustand
- Vite配置API代理：`/api` → `http://localhost:8080`
- 整体样式适配大屏触摸操作（字体≥16px，按钮高度≥48px）

**要创建/编辑的文件**：
- `room-frontend/package.json`
- `room-frontend/vite.config.js`
- `room-frontend/src/api/request.js`
- `room-frontend/src/store/roomStore.js`（存储当前orderId）

**完成情况**：
- ✅ Vite + React 18 项目创建完成，端口3001
- ✅ vite.config.js 配置 API 代理 `/api` → `http://localhost:8080`
- ✅ 安装 antd-mobile 5.x、axios、react-router-dom v6、zustand
- ✅ src/api/request.js Axios 封装（统一 Toast 错误提示）
- ✅ src/store/roomStore.js Zustand + persist（orderId 持久化到 localStorage）
- ✅ src/router/index.jsx 路由配置（懒加载）
- ✅ src/layouts/MainLayout.jsx 主布局（TabBar + PlayBar 底部常驻，useEffect 修复 navigate-in-render）
- ✅ src/pages/Join/index.jsx 加入包厢页（输入 orderId）
- ✅ index.html 禁止缩放，适配触摸大屏
- ✅ 编译验证通过（vite build 零错误，built in 803ms）

---

#### [x] 任务F9：歌曲检索页面 ✅ 已完成 (2026-03-31)
**描述**：实现包厢端的多维度歌曲搜索页面  
**验收标准**：
- 顶部有搜索输入框，输入拼音首字母或歌名实时搜索
- 搜索结果以卡片列表展示（歌曲名、歌手名、语种）
- 提供"按歌手"、"按分类"、"热门榜"三个入口Tab
- "按歌手"先展示歌手字母索引（A-Z），选歌手后展示其歌曲
- 每首歌曲卡片有"点歌"按钮，点击即加入队列并提示成功

**要创建/编辑的文件**：
- `room-frontend/src/pages/Search/index.jsx`
- `room-frontend/src/pages/Search/SingerList.jsx`
- `room-frontend/src/api/song.js`

**完成情况**：
- ✅ src/pages/Search/index.jsx 四个Tab（搜索/歌手/分类/热门）
  - 搜索Tab：300ms 防抖，实时调用 GET /api/room/songs/search
  - 歌手Tab：点击歌手展开 SingerSongs 子组件加载歌曲
  - 分类Tab：Tag 列表切换，按分类过滤歌曲
  - 热门Tab：排行榜序号高亮（前3名金银铜色）
- ✅ src/pages/Search/SingerList.jsx 歌手列表（按拼音首字母分组+右侧字母索引快速跳转）
- ✅ src/api/song.js 歌曲检索接口封装
- ✅ 每首歌曲卡片"点歌"按钮，调用 POST /api/room/{orderId}/queue/add，Toast 提示

---

#### [x] 任务F10：已点/已唱列表页面 ✅ 已完成 (2026-03-31)
**描述**：实现排队列表展示，支持置顶和取消点歌操作  
**验收标准**：
- 展示当前排队中的歌曲（带序号，第1首标注"即将播放"）
- 每行操作：置顶（移到第一位）、删除（取消点歌）
- Tab切换查看"已唱"历史列表
- 列表实时刷新（每10秒轮询或手动下拉刷新）

**要创建/编辑的文件**：
- `room-frontend/src/pages/Queue/index.jsx`
- `room-frontend/src/api/queue.js`

**完成情况**：
- ✅ src/pages/Queue/index.jsx 待唱/已唱双Tab
  - 待唱列表：序号显示，第1首高亮标注"即将播放"
  - SwipeAction 滑动操作：左滑显示置顶（UpOutline）和取消（CloseCircleOutline）
  - 10秒定时轮询自动刷新
  - PullToRefresh 下拉手动刷新
  - 已唱历史列表展示（已播放/已跳过）
- ✅ src/api/queue.js 队列接口封装（获取队列/获取已唱/置顶/取消）

---

#### [x] 任务F11：播放控制页面 ✅ 已完成 (2026-03-31)
**描述**：实现底部常驻的播放控制栏，显示当前播放歌曲并提供控制操作  
**验收标准**：
- 页面底部常驻播放栏：显示当前歌曲名+歌手名、队列剩余数量
- 提供三个大按钮：暂停/继续、切歌（下一首）、重唱
- 点击后调用对应接口，操作结果通过Toast提示
- 无歌曲播放时显示"暂无歌曲，快去点歌吧"

**要创建/编辑的文件**：
- `room-frontend/src/components/PlayBar/index.jsx`（全局底部组件）
- `room-frontend/src/api/play.js`

**完成情况**：
- ✅ src/components/PlayBar/index.jsx 底部常驻播放控制栏
  - 三柱动态播放指示器（CSS 波形动画）
  - 歌曲名+歌手名+队列剩余数量展示
  - 重唱（AudioFill）、暂停/继续（SoundMuteOutline/PlayOutline）、切歌（RightOutline）三按钮
  - 5秒轮询同步后端播放状态
  - 无歌曲时显示"暂无歌曲，快去点歌吧"
- ✅ src/api/play.js 播放控制接口封装（当前状态/暂停/恢复/切歌/重唱）
- ✅ PlayBar 高度56px，固定在 TabBar（50px）上方，main-content 预留 padding-bottom

---

### 阶段九：媒体播放模块（新增，约2天）

#### [x] 任务B1：后端媒体流接口 ✅ 已完成 (2026-03-30)
**描述**：实现 HTTP Range 流式传输接口，支持 MP3/FLAC/MP4 文件的边播边传和进度拖拽  
**验收标准**：
- `GET /api/media/stream/{songId}` 返回正确的 Content-Type（audio/mpeg、audio/flac、video/mp4）
- 支持 `Range: bytes=start-end` 请求头，返回 206 Partial Content
- 前端播放器拖拽进度条时不需要重新下载整个文件
- 文件路径从数据库 `t_song.file_path` 字段读取，基础目录从 `application.yml` 配置的 `media.base-path` 读取
- `GET /api/media/cover/{songId}` 返回封面图

**要创建/编辑的文件**：
- `controller/MediaStreamController.java`（流媒体接口）
- `service/MediaService.java` + `impl/MediaServiceImpl.java`
- `src/main/resources/application.yml`（新增 `media.base-path` 配置）

**核心实现要点**：
```java
// 使用 Spring MVC 原生 ResourceRegion，不要手动读全部字节到内存
@GetMapping("/stream/{songId}")
public ResponseEntity<ResourceRegion> streamMedia(
    @PathVariable Long songId,
    @RequestHeader HttpHeaders headers) {
    // 1. 查DB拿 file_path
    // 2. new FileSystemResource(filePath)
    // 3. ResourceRegionFactory.getResourceRegions(resource, headers)
    // 4. 返回206 + Content-Type
}
```

**参考**：`docs/project-overview.md` 5.3节

**完成情况**：
- ✅ `controller/MediaStreamController.java` - 实现 `GET /api/media/stream/{songId}`，使用 Spring MVC `ResourceRegion` 返回 206 Partial Content，前端拖拽进度条无需重下整个文件
- ✅ 支持多种媒体 Content-Type 自动识别：`audio/mpeg`（mp3）、`audio/flac`、`audio/wav`、`audio/ogg`、`audio/m4a`、`video/mp4`
- ✅ 文件路径从 `t_song.file_path` 读取，相对路径自动拼接 `media.base-path`（application.yml 配置）
- ✅ `GET /api/media/cover/{songId}` - 封面图接口，支持本地文件 + 默认封面兜底
- ✅ `GET /api/media/info/{songId}` - 媒体元信息查询接口
- ✅ `service/MediaService.java` + `impl/MediaServiceImpl.java` - 媒体文件服务层实现

---

#### [x] 任务B2：后台管理端 - 歌曲文件上传 ✅ 已完成 (2026-03-31)
**描述**：在后台歌曲管理页面新增文件上传功能，将 MP3/FLAC/MP4 上传到服务器磁盘  
**验收标准**：
- 新增/编辑歌曲表单中有文件上传按钮（Ant Design Upload 组件）
- 支持格式：`.mp3`、`.flac`、`.mp4`，大小限制可配置（默认500MB）
- 上传成功后自动填充歌曲时长（前端读取 audio 元素 duration）
- 后端保存文件到 `{media.base-path}/{singer_id}/{song_id}.{ext}` 路径
- 接口：`POST /api/admin/songs/{id}/upload`

**要创建/编辑的文件**：
- `admin-frontend/src/pages/Song/index.jsx`（在歌曲表单中添加上传组件）
- `controller/admin/SongController.java`（新增 upload 接口）

**完成情况**：
- ✅ `SongController.java` - 新增 `POST /api/admin/songs/{songId}/upload` 接口（文件保存到 `{media.base-path}/{singer_id}/{song_id}.{ext}`，支持 mp3/flac/wav/ogg/m4a/mp4/avi/mkv/webm，大小限制 500MB）
- ✅ `SongController.java` - 新增 `POST /api/admin/songs/{songId}/cover` 接口（封面图保存到 `{media.base-path}/covers/{song_id}.{ext}`）
- ✅ `admin-frontend/src/api/song.js` - 封装 `uploadSongFile` 和 `uploadCoverImage` 两个上传函数
- ✅ `admin-frontend/src/pages/Song/index.jsx` - 歌曲列表操作列新增"上传"按钮，弹出专用上传对话框
  - Ant Design Upload.Dragger 拖拽上传区域
  - 媒体文件区（音视频）+ 封面图区分别独立上传
  - 上传进度条（实时 onUploadProgress 回调）
  - 上传完成后自动调用 `getAudioDuration()` 读取时长并更新歌曲记录
  - 文件类型校验 + 大小限制
  - 表格新增"文件"列：用 Tag 标注音频/视频/未上传状态
  - 编译验证通过（vite build 零错误，865ms）

---

#### [x] 任务F12：包厢端 - 集成 APlayer 播放器 ✅ 已完成 (2026-03-31)
**描述**：在包厢点歌端集成 APlayer，点歌后实际播放音频文件，替换原来的模拟状态  
**验收标准**：
- 安装 `aplayer` + `react-aplayer`（或直接用 `react-player` 统一处理音视频）
- 播放器显示在 PlayBar 区域：封面图、歌曲名、歌手名、进度条、时长
- 点歌后队列第一首自动加载并播放（调用 `/api/media/stream/{songId}`）
- 切歌/重唱操作同步控制播放器（调用后端接口 + 前端播放器 seek/reload）
- MP4 视频文件用 `react-player` 渲染为全屏视频，MP3/FLAC 用 APlayer 音频模式
- 格式判断：根据 `t_song.file_path` 后缀区分

**要创建/编辑的文件**：
- `room-frontend/package.json`（新增 aplayer、react-player 依赖）
- `room-frontend/src/components/PlayBar/index.jsx`（集成播放器）
- `room-frontend/src/components/VideoPlayer/index.jsx`（MP4专用，全屏视频）
- `room-frontend/src/api/play.js`（同步播放状态到后端）

**注意**：
- FLAC 格式浏览器原生支持有限，APlayer 依赖浏览器解码，Chrome/Edge 支持较好
- 播放器音量、静音状态存 localStorage，包厢重启后恢复

**完成情况**：
- ✅ `room-frontend/package.json` - 新增 `aplayer@1.10.1` + `react-player@3.4.0` 依赖
- ✅ `room-frontend/src/components/PlayBar/index.jsx` - 集成 APlayer mini 模式：
  - 音频文件（mp3/flac/wav/ogg/m4a）：APlayer 播放，显示封面图、歌曲名、歌手名、进度条
  - 5秒轮询后端 `/api/room/play/status` 同步 PLAYING/PAUSED 状态
  - 播放结束自动调用 nextSong 接口切到下一首
  - 音量/静音状态持久化到 localStorage，刷新后恢复
  - 歌曲切换时重新加载 APlayer 实例（destroyAPlayer + 重建）
- ✅ `room-frontend/src/components/VideoPlayer/index.jsx` - 视频文件（mp4/avi/mkv/webm）用 react-player 全屏渲染：
  - 点击屏幕显示控制栏（3秒无操作自动隐藏）
  - 重唱（seekTo(0)）+ 切歌 按钮
  - 调用后端 nextSong 接口同步状态
- ✅ 格式判断：根据 `file_path` 后缀区分音频/视频，分别路由到 APlayer 或 VideoPlayer
- ✅ 编译验证通过（vite build 零错误，444ms）

## 技术说明

| 项目 | 说明 |
|------|------|
| **JDK版本** | Java 21 LTS，支持虚拟线程 |
| **Spring Boot** | 3.2.x，使用 `jakarta.*` 包名（非 `javax.*`） |
| **MyBatis-Plus** | 3.5.7+，兼容 Spring Boot 3 |
| **MySQL驱动** | `mysql-connector-j`（Spring Boot 3 已更名） |
| **JWT库** | jjwt 0.11.x+（支持 Jakarta EE） |
| **Redis队列** | 使用 List 类型实现点歌排队（LPUSH/RPUSH/LREM） |
| **定时任务** | Spring @Scheduled，无需引入额外调度框架 |
| **拼音库** | `com.github.houbb:pinyin`（替代已停更的 pinyin4j） |
| **认证方式** | JWT Token（前后端分离，替代Session） |
| **媒体流接口** | Spring MVC `ResourceRegion`，支持 HTTP Range（进度拖拽/断点续传） |
| **前端播放器** | APlayer（音频 MP3/FLAC）+ react-player（视频 MP4） |
| **媒体文件路径** | 配置在 `application.yml` 的 `media.base-path`，不硬编码 |
| **后台前端** | React 18 + Ant Design 5 + Vite，端口3000 |
| **包厢前端** | React 18 + Ant Design Mobile 5 + Vite，端口3001 |
| **状态管理** | Zustand（轻量，无需Redux样板代码） |
| **时间线** | 约24个工作日（含前端+媒体播放模块） |
