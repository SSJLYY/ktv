# 常见问题排查指南

本文档汇总了 KTV 点歌系统开发和使用过程中的常见问题及解决方案。

---

## 后端问题

### 1. 启动报错：`java.lang.NoClassDefFoundError: jakarta/...`

**错误信息**：
```
java.lang.NoClassDefFoundError: jakarta/persistence/Entity
```

**原因**：依赖版本不兼容，Spring Boot 3 使用 `jakarta.*` 包名（不再是 `javax.*`）

**解决方案**：
1. 检查 `pom.xml` 中的 MyBatis-Plus 版本是否 >= 3.5.7
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.7</version>
</dependency>
```

2. 检查 jjwt 版本是否 >= 0.11.5
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
```

3. 删除 `.m2/repository` 中的旧依赖，重新下载
```bash
mvn clean install -U
```

---

### 2. 数据库连接失败：`Communications link failure`

**错误信息**：
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago
```

**原因**：MySQL 服务未启动或配置错误

**解决方案**：
1. 检查 MySQL 服务是否启动
```bash
# Windows
net start mysql

# 或使用服务管理器检查
```

2. 检查 `application.yml` 中的数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ktv_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 检查密码是否正确
```

3. 测试数据库连接
```bash
mysql -u root -p
```

---

### 3. Redis 连接失败：`Unable to connect to Redis`

**错误信息**：
```
Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException
```

**原因**：Redis 服务未启动或配置错误

**解决方案**：
1. 检查 Redis 服务是否启动
```bash
# Windows
redis-server.exe

# 或启动 Redis 服务
net start redis
```

2. 测试 Redis 连接
```bash
redis-cli ping
# 期望返回：PONG
```

3. 检查 `application.yml` 中的 Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:  # 如果有密码需要填写
```

---

### 4. JWT Token 过期或无效

**错误信息**：
```json
{
  "code": 401,
  "msg": "Token无效或已过期"
}
```

**原因**：Token 过期时间过短或 Token 格式错误

**解决方案**：
1. 检查 `JwtUtil.java` 中的过期时间设置
```java
// 通常设置为 2 小时
long expiration = 2 * 60 * 60 * 1000;
```

2. 检查前端请求头格式
```javascript
// 正确格式
headers: {
  'Authorization': 'Bearer ' + token
}

// 错误格式（不要加引号）
headers: {
  'Authorization': 'Bearer "' + token + '"'
}
```

3. 检查 `localStorage` 中是否有 Token
```javascript
console.log(localStorage.getItem('token'));
```

---

### 5. MyBatis-Plus 分页不生效

**现象**：分页查询返回所有数据，没有分页

**原因**：未配置分页插件

**解决方案**：
检查 `MyBatisPlusConfig.java` 是否正确配置分页插件
```java
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

---

### 6. 文件上传失败

**错误信息**：
```
java.io.FileNotFoundException: D:\ktv-media\song.mp3 (系统找不到指定的路径)
```

**原因**：媒体文件存储路径不存在或无权限

**解决方案**：
1. 检查 `application.yml` 中的媒体路径配置
```yaml
media:
  base-path: D:/ktv-media
```

2. 创建媒体目录并赋予写入权限
```bash
# Windows
mkdir D:\ktv-media
icacls D:\ktv-media /grant Everyone:F
```

3. 检查磁盘空间是否充足

---

## 前端问题

### 1. CORS 跨域错误

**错误信息**：
```
Access to XMLHttpRequest at 'http://localhost:8080/api/admin/songs' from origin 'http://localhost:3000' has been blocked by CORS policy
```

**原因**：后端未配置 CORS 或配置错误

**解决方案**：

#### 开发环境：
检查 `vite.config.js` 中的代理配置
```javascript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
```

#### 生产环境：
检查后端 `CorsConfig.java` 配置
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:3001")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

### 2. 依赖安装失败

**错误信息**：
```
npm ERR! code ERESOLVE
npm ERR! ERESOLVE unable to resolve dependency tree
```

**原因**：依赖版本冲突

**解决方案**：
1. 删除 `node_modules` 和 `package-lock.json`
```bash
rm -rf node_modules package-lock.json
# Windows
rmdir /s /q node_modules
del package-lock.json
```

2. 清除 npm 缓存
```bash
npm cache clean --force
```

3. 重新安装依赖
```bash
npm install

# 或使用 pnpm（推荐）
pnpm install
```

4. 如果仍有问题，尝试指定版本
```bash
npm install antd@5.x react@18.x
```

---

### 3. 组件导入失败

**错误信息**：
```
Module not found: Can't resolve '@/components/PlayBar'
```

**原因**：路径别名配置错误或路径写错

**解决方案**：
1. 检查 `vite.config.js` 中的路径别名配置
```javascript
import path from 'path';

export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
})
```

2. 检查文件路径是否正确
```javascript
// 正确
import PlayBar from '@/components/PlayBar';

// 错误
import PlayBar from '@/component/PlayBar';  // components 拼写错误
```

---

### 4. 状态丢失（刷新页面后）

**现象**：刷新页面后，状态（如 orderId）丢失

**原因**：状态未持久化

**解决方案**：
使用 Zustand 的 persist 中间件
```javascript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useRoomStore = create(
  persist(
    (set) => ({
      orderId: '',
      setOrderId: (orderId) => set({ orderId }),
    }),
    {
      name: 'ktv-room-storage',  // localStorage key
    }
  )
);

export default useRoomStore;
```

---

### 5. 音视频无法播放

**现象**：点击播放无反应或报错

**原因**：文件路径错误或格式不支持

**解决方案**：
1. 检查文件是否存在
```bash
ls -la D:/ktv-media/song.mp3
```

2. 检查后端文件路径配置
```yaml
media:
  base-path: D:/ktv-media
```

3. 检查前端播放器配置
```javascript
// APlayer 配置
const audio = {
  name: '歌曲名',
  artist: '歌手名',
  url: '/api/media/stream/1',  // 使用流媒体接口
  cover: '/api/media/cover/1'
};
```

4. 检查浏览器控制台网络请求
- 查看请求 URL 是否正确
- 查看响应状态码（200、206、404等）
- 查看 `Content-Type` 是否正确（audio/mpeg、video/mp4等）

---

## 媒体播放问题

### 1. 进度条拖动失败

**现象**：拖动进度条后无法跳转，或者从头开始播放

**原因**：后端未支持 HTTP Range 请求

**解决方案**：
检查 `MediaServiceImpl.java` 是否正确实现 Range 支持
```java
@GetMapping("/stream/{songId}")
public ResponseEntity<ResourceRegion> streamMedia(
    @PathVariable Long songId,
    @RequestHeader HttpHeaders headers
) {
    // 检查 Range 头
    if (!headers.containsKey(HttpHeaders.RANGE)) {
        // 不支持 Range，返回完整文件
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(inputStream));
    }

    // 支持 Range，返回指定范围
    ResourceRegion region = resourceRegion;
    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .body(region);
}
```

---

### 2. FLAC 文件无法播放

**现象**：MP3 文件正常播放，但 FLAC 文件无法播放

**原因**：浏览器或播放器不支持 FLAC 格式

**解决方案**：
1. 使用 APlayer 播放器（支持 FLAC）
```javascript
import APlayer from 'aplayer';

const ap = new APlayer({
  element: document.getElementById('aplayer'),
  audio: [{
    name: '歌曲名',
    artist: '歌手名',
    url: '/api/media/stream/1',  // FLAC 文件
    cover: '/api/media/cover/1'
  }]
});
```

2. 确保后端返回正确的 `Content-Type`
```java
String contentType = "audio/flac";
```

---

### 3. 视频无法播放

**现象**：MP4 视频文件无法播放

**原因**：视频编码格式不支持或路径错误

**解决方案**：
1. 使用 react-player 播放视频
```javascript
import ReactPlayer from 'react-player';

<ReactPlayer
  url='/api/media/stream/1'
  playing={true}
  controls={true}
  width='100%'
  height='100%'
/>
```

2. 确保视频编码为 H.264 + AAC（浏览器广泛支持）
```bash
# 检查视频编码
ffprobe video.mp4
```

3. 确保后端返回正确的 `Content-Type`
```java
String contentType = "video/mp4";
```

---

## 数据库问题

### 1. 字符编码错误

**错误信息**：
```
Incorrect string value: '\xE6\xAD\x8C...' for column 'name' at row 1
```

**原因**：数据库或表字符集不是 `utf8mb4`

**解决方案**：
1. 检查数据库字符集
```sql
SHOW VARIABLES LIKE 'character_set%';
```

2. 修改数据库字符集
```sql
ALTER DATABASE ktv_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 修改表字符集
```sql
ALTER TABLE t_song CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### 2. 查询性能慢

**现象**：搜索歌曲响应时间超过 1 秒

**原因**：未建立索引或索引失效

**解决方案**：
1. 检查表索引
```sql
SHOW INDEX FROM t_song;
```

2. 分析查询执行计划
```sql
EXPLAIN SELECT * FROM t_song WHERE pinyin_initial = 'ZJL';
```

3. 添加缺失的索引
```sql
CREATE INDEX idx_pinyin_initial ON t_song(pinyin_initial);
```

4. 使用 Redis 缓存热门歌曲
```java
// 缓存热门歌曲
String cacheKey = "ktv:song:hot:" + songId;
redisTemplate.opsForValue().set(cacheKey, song, 1, TimeUnit.HOURS);
```

---

## 性能优化建议

### 1. 后端优化

- **数据库连接池**：使用 HikariCP（Spring Boot 默认）
- **分页查询**：避免一次性查询大量数据
- **Redis 缓存**：缓存热门歌曲、歌手、分类等常用数据
- **异步处理**：耗时操作使用异步处理（如日志记录）

### 2. 前端优化

- **代码分割**：使用路由懒加载
```javascript
const Song = lazy(() => import('@/pages/Song'));
```
- **防抖节流**：搜索输入使用防抖
```javascript
const debouncedSearch = debounce(keyword => {
  // 搜索逻辑
}, 300);
```
- **虚拟滚动**：长列表使用虚拟滚动
- **图片懒加载**：封面图片使用懒加载

---

## 调试技巧

### 1. 后端调试

- **查看日志**：使用 `log.info()` 输出关键信息
- **断点调试**：在 IDE 中设置断点，使用 Debug 模式运行
- **SQL 日志**：开启 MyBatis-Plus SQL 日志
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 2. 前端调试

- **浏览器控制台**：使用 `console.log()` 输出调试信息
- **React DevTools**：安装浏览器插件，查看组件状态和 Props
- **网络请求**：使用浏览器开发者工具的 Network 标签页
- **断点调试**：在 VS Code 中设置断点，使用调试模式运行

---

## 获取帮助

如果遇到上述文档未涵盖的问题：

1. 检查项目文档
   - [项目设计文档](../docs/project-overview.md)
   - [开发任务列表](../docs/tasks/ktv-song-system-tasklist.md)

2. 查看错误日志
   - 后端日志：`logs/ktv.log`
   - 浏览器控制台日志

3. 搜索错误信息
   - 使用搜索引擎搜索错误信息
   - 查看 GitHub Issues

4. 联系团队
   - 提交 Issue
   - 联系项目负责人

---

**作者**：shaun.sheng

祝你开发顺利！🔧
