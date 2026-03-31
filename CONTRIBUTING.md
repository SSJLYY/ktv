# 贡献指南

感谢你对 KTV点歌系统 项目的关注！本文档将帮助你了解如何参与项目开发。

## 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发环境搭建](#开发环境搭建)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)

## 行为准则

- 尊重所有贡献者
- 保持建设性的讨论
- 关注问题本身，而非个人

## 如何贡献

### 报告 Bug

1. 搜索现有的 Issues，避免重复报告
2. 创建新 Issue，包含以下信息：
   - Bug 描述（清晰、具体）
   - 复现步骤
   - 期望行为 vs 实际行为
   - 环境信息（操作系统、Java版本、Node版本等）
   - 截图或日志（如有）

### 建议新功能

1. 创建 Feature Request Issue
2. 描述功能需求和使用场景
3. 说明为什么这个功能有价值

### 提交代码

详见 [Pull Request 流程](#pull-request-流程)

## 开发环境搭建

### 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | LTS版本，支持虚拟线程 |
| Node.js | 18+ | 前端开发 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.x+ | 缓存/队列 |
| Maven | 3.6.3+ | 后端构建 |

### 本地启动

```bash
# 1. 克隆项目
git clone <repository-url>
cd ktv

# 2. 初始化数据库
mysql -u root -p ktv < sql/init-schema.sql
mysql -u root -p ktv < sql/init-data.sql

# 3. 启动后端
cd ktv-backend
# 修改 application.yml 配置数据库和Redis
mvn spring-boot:run

# 4. 启动前端（新终端）
cd admin-frontend
npm install
npm run dev

# 5. 启动包厢端（新终端）
cd room-frontend
npm install
npm run dev
```

### IDE 推荐

- 后端：IntelliJ IDEA
- 前端：VS Code

## 代码规范

### Java 后端规范

#### 命名规范

```java
// 类名：PascalCase
public class SongService { }

// 方法名/变量名：camelCase
public Song getById(Long id) { }
private Song currentSong;

// 常量：UPPER_SNAKE_CASE
public static final String DEFAULT_STATUS = "active";

// 包名：全小写
package com.ktv.service;
```

#### 分层架构

```
Controller → Service → Mapper → Entity
    ↓          ↓
   DTO        VO
```

- **Controller**：仅处理请求响应，不写业务逻辑
- **Service**：业务逻辑层，接口+实现分离
- **Mapper**：MyBatis-Plus接口，数据访问
- **Entity**：数据库实体，与表一一对应

#### 统一返回格式

```java
// 成功
return Result.success(data);

// 失败
return Result.error("错误信息");
```

#### 异常处理

```java
// 使用业务异常
throw new BusinessException("歌曲不存在");

// 全局异常处理器会捕获并返回统一格式
```

#### 数据库规范

- 表名前缀：`t_`
- 字段名：`snake_case`
- 逻辑删除：`deleted` 字段（0未删 1已删）
- 必备字段：`id`、`create_time`、`update_time`

### React 前端规范

#### 组件规范

```jsx
// ✅ 使用函数组件 + Hooks
function SongList() {
  const [songs, setSongs] = useState([]);

  useEffect(() => {
    // 获取数据
  }, []);

  return <div>...</div>;
}

// ❌ 禁止 Class 组件
class SongList extends React.Component { }
```

#### 文件命名

- 组件文件：`PascalCase.jsx`（如 `SongCard.jsx`）
- 工具函数：`camelCase.js`（如 `formatDate.js`）
- 样式文件：`PascalCase.css`（如 `SongCard.css`）

#### 状态管理

```jsx
// 使用 Zustand
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useStore = create(
  persist(
    (set) => ({
      orderId: null,
      setOrderId: (id) => set({ orderId: id }),
    }),
    { name: 'ktv-order' }
  )
);
```

#### API 调用

```jsx
// 使用 axios 封装
import { songApi } from '@/api';

// 在 useEffect 中调用
useEffect(() => {
  songApi.search(keyword).then(res => {
    if (res.data.code === 200) {
      setSongs(res.data.data);
    }
  });
}, [keyword]);
```

### 分页参数规范

后端使用 `current/size`（当前页/每页条数），不是 `pageNum/pageSize`：

```java
// 分页查询
@GetMapping("/songs")
public Result<Page<Song>> list(
    @RequestParam(defaultValue = "1") Integer current,
    @RequestParam(defaultValue = "10") Integer size
) {
    Page<Song> page = songService.page(new Page<>(current, size));
    return Result.success(page);
}
```

## 提交规范

### Commit Message 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | Bug修复 |
| docs | 文档更新 |
| style | 代码格式（不影响功能） |
| refactor | 重构 |
| perf | 性能优化 |
| test | 测试相关 |
| chore | 构建/工具相关 |

### 示例

```bash
# 新功能
git commit -m "feat(song): 添加歌曲批量导入功能"

# Bug修复
git commit -m "fix(queue): 修复置顶后排序错误问题"

# 文档更新
git commit -m "docs: 更新API文档中的分页参数说明"
```

## Pull Request 流程

### 1. Fork 并克隆

```bash
# Fork 后克隆你的仓库
git clone https://github.com/<your-username>/ktv.git
cd ktv

# 添加上游仓库
git remote add upstream https://github.com/<original-owner>/ktv.git
```

### 2. 创建分支

```bash
# 同步上游
git fetch upstream

# 创建功能分支
git checkout -b feature/your-feature upstream/main
```

### 3. 开发并提交

```bash
# 编写代码...

# 提交更改
git add .
git commit -m "feat: 添加xxx功能"
```

### 4. 推送并创建 PR

```bash
# 推送到你的仓库
git push origin feature/your-feature
```

然后在 GitHub 上创建 Pull Request。

### 5. PR 检查清单

PR 提交前请确保：

- [ ] 代码通过编译，无错误警告
- [ ] 遵循项目的代码规范
- [ ] 新功能有对应的文档更新
- [ ] Commit Message 符合规范
- [ ] 相关 Issue 已关联

### 6. 代码审查

PR 会进行代码审查，可能需要：

- 修改代码
- 添加测试
- 更新文档

审查通过后会被合并。

## 文档贡献

文档位于 `docs/` 目录：

- `project-overview.md` - 项目设计文档
- `api-reference.md` - API参考文档
- `code-review-standards.md` - 代码审查规范

文档更新请保持：

- 内容准确、简洁
- 示例代码可运行
- 使用 Markdown 格式

## 联系方式

- 提交 Issue 进行讨论
- 作者：shaun.sheng

---

再次感谢你的贡献！🎉
