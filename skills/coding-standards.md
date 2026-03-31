# 编码规范

本文档定义了 KTV 点歌系统的编码规范，确保代码风格统一、可维护。

---

## 后端编码规范

### 1. 命名规范

#### 1.1 包命名
- 全部小写，使用点号分隔
- 格式：`com.ktv.{模块}`

```java
// ✅ 正确
package com.ktv.controller.admin;
package com.ktv.service.impl;

// ❌ 错误
package com.KTV.controller;
package com.ktv.Controller;
```

#### 1.2 类命名
- 使用 PascalCase（首字母大写）
- Controller 后缀：`XxxController`
- Service 接口：`XxxService`
- Service 实现：`XxxServiceImpl`
- Mapper 接口：`XxxMapper`
- Entity 实体类：`Xxx`（使用表名去掉前缀）
- DTO 请求参数：`XxxDTO`
- VO 响应参数：`XxxVO`

```java
// ✅ 正确
public class SongController { }
public interface SongService { }
public class SongServiceImpl implements SongService { }
public interface SongMapper { }
public class Song { }
public class SongDTO { }
public class SongVO { }

// ❌ 错误
public class songController { }
public class songService { }
public class Song_Interface { }
```

#### 1.3 方法命名
- 使用 camelCase（首字母小写）
- 查询单个：`getXxx`、`findXxx`
- 查询列表：`listXxx`、`queryXxx`
- 新增：`createXxx`、`addXxx`、`insertXxx`
- 修改：`updateXxx`、`editXxx`、`modifyXxx`
- 删除：`deleteXxx`、`removeXxx`
- 分页查询：`pageXxx`

```java
// ✅ 正确
public Song getSongById(Long id);
public List<Song> listSongs();
public void createSong(SongDTO dto);
public void updateSong(SongDTO dto);
public void deleteSong(Long id);
public Page<Song> pageSongs(PageParam param);

// ❌ 错误
public Song GetSongById(Long id);
public List<Song> get_song_list();
public void AddSong(SongDTO dto);
```

#### 1.4 变量命名
- 使用 camelCase
- 常量使用 UPPER_SNAKE_CASE（全大写，下划线分隔）

```java
// ✅ 正确
private String songName;
private Integer totalCount;
private static final String TOKEN_PREFIX = "Bearer ";

// ❌ 错误
private String song_name;
private String SongName;
private static final String tokenPrefix = "Bearer ";
```

### 2. 代码结构

#### 2.1 Controller 层
- 只负责接收请求、参数校验、返回响应
- 不包含业务逻辑
- 使用 `@RestController` 注解

```java
@RestController
@RequestMapping("/api/admin/songs")
public class SongController {

    @Autowired
    private SongService songService;

    @GetMapping("/{id}")
    public Result<Song> getById(@PathVariable Long id) {
        Song song = songService.getById(id);
        return Result.success(song);
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody SongDTO dto) {
        songService.create(dto);
        return Result.success();
    }
}
```

#### 2.2 Service 层
- 接口定义业务方法
- 实现类包含业务逻辑
- 使用事务注解 `@Transactional`

```java
// 接口
public interface SongService extends IService<Song> {
    void create(SongDTO dto);
}

// 实现类
@Service
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements SongService {

    @Autowired
    private SingerService singerService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void create(SongDTO dto) {
        // 1. 参数校验
        if (dto.getSingerId() == null) {
            throw new BusinessException("歌手ID不能为空");
        }

        // 2. 业务逻辑
        Singer singer = singerService.getById(dto.getSingerId());
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }

        // 3. 保存数据
        Song song = BeanUtil.copyProperties(dto, Song.class);
        save(song);

        // 4. 更新歌手歌曲数
        singer.setSongCount(singer.getSongCount() + 1);
        singerService.updateById(singer);
    }
}
```

#### 2.3 Mapper 层
- 继承 `BaseMapper<T>`，使用 MyBatis-Plus 提供的方法
- 复杂查询使用 `@Select` 注解或 XML 映射文件

```java
@Mapper
public interface SongMapper extends BaseMapper<Song> {

    @Select("SELECT * FROM t_song WHERE singer_id = #{singerId}")
    List<Song> listBySingerId(@Param("singerId") Long singerId);
}
```

### 3. 注释规范

#### 3.1 类注释
```java
/**
 * 歌曲管理控制器
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
@RestController
@RequestMapping("/api/admin/songs")
public class SongController {
    // ...
}
```

#### 3.2 方法注释
```java
/**
 * 根据ID查询歌曲
 *
 * @param id 歌曲ID
 * @return 歌曲信息
 */
@GetMapping("/{id}")
public Result<Song> getById(@PathVariable Long id) {
    // ...
}
```

#### 3.3 字段注释
```java
/**
 * 歌曲ID
 */
private Long songId;

/**
 * 歌曲名称（支持拼音搜索）
 */
private String songName;
```

#### 3.4 复杂逻辑注释
```java
// 步骤1：查询歌曲信息
Song song = getById(songId);

// 步骤2：增加播放次数
song.setPlayCount(song.getPlayCount() + 1);
updateById(song);

// 步骤3：更新热门排行榜
redisTemplate.opsForZSet().incrementScore("ktv:song:hot", songId, 1);
```

### 4. 异常处理

#### 4.1 业务异常
使用 `BusinessException` 抛出业务异常

```java
// 参数校验
if (StringUtils.isBlank(dto.getName())) {
    throw new BusinessException("歌曲名称不能为空");
}

// 数据不存在
Song song = getById(songId);
if (song == null) {
    throw new BusinessException("歌曲不存在");
}

// 业务规则校验
if (song.getStatus() == 0) {
    throw new BusinessException("歌曲已下架，无法点播");
}
```

#### 4.2 异常捕获
不在 Controller 中捕获异常，交由全局异常处理器处理

```java
// ✅ 正确
@GetMapping("/{id}")
public Result<Song> getById(@PathVariable Long id) {
    Song song = songService.getById(id);
    return Result.success(song);
}

// ❌ 错误
@GetMapping("/{id}")
public Result<Song> getById(@PathVariable Long id) {
    try {
        Song song = songService.getById(id);
        return Result.success(song);
    } catch (Exception e) {
        log.error("查询失败", e);
        return Result.fail("查询失败");
    }
}
```

---

## 前端编码规范

### 1. 命名规范

#### 1.1 文件命名
- 组件文件：PascalCase（如 `SongCard.jsx`）
- 页面文件：PascalCase（如 `SongList.jsx`）
- 工具文件：camelCase（如 `request.js`）
- 样式文件：kebab-case（如 `song-card.css`）

```jsx
// ✅ 正确
// components/SongCard.jsx
// pages/SongList.jsx
// utils/request.js
// styles/song-card.css

// ❌ 错误
// components/songcard.jsx
// pages/song_list.jsx
// utils/Request.js
// styles/songCard.css
```

#### 1.2 组件命名
- 使用 PascalCase
- 组件名与文件名保持一致

```jsx
// ✅ 正确
const SongCard = () => {
  return <div>...</div>;
};
export default SongCard;

// ❌ 错误
const songCard = () => {
  return <div>...</div>;
};
export default songCard;
```

#### 1.3 变量和函数命名
- 使用 camelCase
- 常量使用 UPPER_SNAKE_CASE

```jsx
// ✅ 正确
const songName = '青花瓷';
const playCount = 100;
const API_BASE_URL = 'http://localhost:8080';

const handlePlay = () => {
  // ...
};

// ❌ 错误
const song_name = '青花瓷';
const PlayCount = 100;
const api_base_url = 'http://localhost:8080';
```

### 2. 组件结构

#### 2.1 函数组件
- 使用函数组件 + Hooks
- 组件按顺序排列：导入 → 常量 → Hooks → 事件处理 → 渲染

```jsx
import { useState, useEffect } from 'react';
import { Button, message } from 'antd';
import { getSongs } from '@/api/song';

const SongList = () => {
  // 1. 常量
  const PAGE_SIZE = 10;

  // 2. Hooks
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);

  // 3. 生命周期
  useEffect(() => {
    loadData();
  }, []);

  // 4. 事件处理
  const loadData = async () => {
    setLoading(true);
    try {
      const res = await getSongs({ pageNum: 1, pageSize: PAGE_SIZE });
      setData(res.data.records);
    } catch (error) {
      message.error('加载失败');
    } finally {
      setLoading(false);
    }
  };

  const handlePlay = (song) => {
    // ...
  };

  // 5. 渲染
  return (
    <div>
      <Button onClick={loadData}>刷新</Button>
      {/* 列表渲染 */}
    </div>
  );
};

export default SongList;
```

#### 2.2 Props 命名
- 使用 camelCase
- 必需参数添加 `isRequired` 校验

```jsx
// ✅ 正确
const SongCard = ({ song, onPlay }) => {
  return (
    <div onClick={() => onPlay(song)}>
      <h3>{song.name}</h3>
    </div>
  );
};

SongCard.propTypes = {
  song: PropTypes.object.isRequired,
  onPlay: PropTypes.func.isRequired,
};

// ❌ 错误
const SongCard = ({ Song, On_Play }) => {
  // ...
};
```

### 3. 代码风格

#### 3.1 JSX 语法
- 自闭合标签使用自闭合语法
- 属性换行时每个属性一行
- 使用片段（`<>...</>`）替代多余的外层 div

```jsx
// ✅ 正确
const SongCard = ({ song }) => {
  return (
    <>
      <h3>{song.name}</h3>
      <p>{song.singerName}</p>
      <Button onClick={handlePlay}>播放</Button>
    </>
  );
};

// ❌ 错误
const SongCard = ({ song }) => {
  return (
    <div>
      <h3>{song.name}</h3>
      <p>{song.singerName}</p>
      <Button onClick={handlePlay}>播放</Button>
    </div>
  );
};
```

#### 3.2 条件渲染
- 使用逻辑与（`&&`）或三元运算符（`?:`）

```jsx
// ✅ 正确
{loading && <Spin />}
{song?.status === 1 ? <PlayButton /> : <DisableButton />}

// ❌ 错误
{loading === true ? <Spin /> : null}
{if (loading) return <Spin />}
```

#### 3.3 列表渲染
- 使用 `map()` 渲染列表
- 必须提供 `key` 属性

```jsx
// ✅ 正确
{data.map((song) => (
  <SongCard key={song.id} song={song} onPlay={handlePlay} />
))}

// ❌ 错误
{data.map((song, index) => (
  <SongCard key={index} song={song} />
))}
```

### 4. 注释规范

#### 4.1 组件注释
```jsx
/**
 * 歌曲卡片组件
 * @param {Object} song - 歌曲信息
 * @param {Function} onPlay - 播放回调函数
 */
const SongCard = ({ song, onPlay }) => {
  // ...
};
```

#### 4.2 复杂逻辑注释
```jsx
// 步骤1：验证 Token
if (!token) {
  message.warning('请先登录');
  return;
}

// 步骤2：发送点歌请求
try {
  await addToQueue(song.id);
  message.success('点歌成功');
} catch (error) {
  message.error('点歌失败');
}
```

---

## 通用规范

### 1. Git 提交规范
使用 Conventional Commits 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型**：
- `feat`：新功能
- `fix`：Bug 修复
- `docs`：文档更新
- `style`：代码格式（不影响功能）
- `refactor`：重构
- `perf`：性能优化
- `test`：测试相关
- `chore`：构建过程或工具变更

**示例**：
```
feat(song): 添加歌曲点歌功能

- 实现点歌队列管理
- 支持置顶和删除操作
- 添加 WebSocket 实时推送

Closes #123
```

### 2. 代码审查清单
- [ ] 代码符合命名规范
- [ ] 方法/函数职责单一
- [ ] 无魔法数字，使用常量
- [ ] 异常处理完善
- [ ] 关键业务逻辑有注释
- [ ] 无未使用的变量和导入
- [ ] 无 console.log 调试代码

### 3. 性能优化
- [ ] 避免不必要的重新渲染（使用 `useMemo`、`useCallback`）
- [ ] 大列表使用虚拟滚动
- [ ] 图片使用懒加载
- [ ] 接口请求使用防抖/节流
- [ ] 避免深层组件嵌套
- [ ] 使用代码分割

---

## 工具推荐

### 后端
- **IDE**：IntelliJ IDEA
- **代码检查**：Alibaba Java Coding Guidelines（插件）
- **格式化**：Google Java Format
- **Maven**：构建工具

### 前端
- **IDE**：VS Code
- **代码检查**：ESLint + Prettier
- **格式化**：Prettier
- **包管理器**：pnpm（推荐）

---

## 相关文档

- [项目设计文档](../docs/project-overview.md)
- [开发任务列表](../docs/tasks/ktv-song-system-tasklist.md)

---

**作者**：shaun.sheng

祝你编码愉快！💻
