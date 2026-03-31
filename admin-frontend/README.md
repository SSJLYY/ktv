# KTV后台管理系统 - 前端项目

## 项目概述

基于 React 18 + Ant Design 5 + Vite 构建的KTV后台管理系统前端项目。

## 技术栈

- **框架**: React 18.2.4
- **UI组件库**: Ant Design 5.29.3
- **路由**: React Router DOM 6.30.3
- **状态管理**: Zustand 5.0.12
- **HTTP客户端**: Axios 1.14.0
- **构建工具**: Vite 8.0.1
- **日期处理**: Day.js

## 项目结构

```
admin-frontend/
├── src/
│   ├── api/              # API接口模块
│   │   ├── request.js    # Axios封装(请求/响应拦截器)
│   │   ├── auth.js       # 认证接口
│   │   ├── singer.js     # 歌手管理接口
│   │   ├── song.js       # 歌曲管理接口
│   │   ├── category.js   # 分类管理接口
│   │   ├── room.js       # 包厢管理接口
│   │   └── order.js      # 订单管理接口
│   ├── components/       # 公共组件
│   │   └── SideMenu/     # 侧边栏菜单
│   ├── layouts/          # 布局组件
│   │   └── AdminLayout.jsx
│   ├── pages/            # 页面组件
│   │   ├── Login/        # 登录页面
│   │   ├── Singer/       # 歌手管理
│   │   ├── Song/         # 歌曲管理
│   │   ├── Category/     # 分类管理
│   │   ├── Room/         # 包厢管理
│   │   └── Order/        # 订单管理
│   ├── router/           # 路由配置
│   │   └── index.jsx
│   ├── store/            # 状态管理
│   │   └── userStore.js  # 用户状态(Zustand)
│   ├── utils/            # 工具函数
│   ├── App.jsx           # 根组件
│   ├── main.jsx          # 入口文件
│   └── index.css         # 全局样式
├── vite.config.js        # Vite配置
└── package.json          # 项目配置
```

## 功能模块

### 1. 认证模块
- 管理员登录(用户名+密码)
- JWT Token认证
- 自动Token续期和401跳转
- 登出功能

### 2. 歌手管理
- 歌手列表(分页)
- 按名称和地区筛选
- 新增歌手
- 编辑歌手
- 删除歌手(带歌曲数量检查)

### 3. 歌曲管理
- 歌曲列表(分页)
- 多条件搜索(歌名、歌手、分类、语种)
- 新增歌曲(歌手和分类下拉选择)
- 编辑歌曲
- 删除歌曲

### 4. 分类管理
- 分类列表
- 新增分类
- 编辑分类
- 删除分类(带歌曲数量检查)

### 5. 包厢管理
- 包厢列表(分页)
- 按类型和状态筛选
- 新增包厢
- 编辑包厢
- 删除包厢(仅空闲状态可删)
- 快速修改包厢状态

### 6. 订单管理
- 订单列表(分页)
- 按日期范围和状态筛选
- 开台(选择空闲包厢)
- 结账(计算时长和费用)
- 取消订单
- 查看订单详情

## 核心特性

### Axios请求封装
- 自动附加JWT Token到请求头
- 401响应自动跳转登录页
- 统一错误提示
- 请求/响应拦截器

### 路由守卫
- 未登录用户访问后台页自动跳转登录
- 已登录用户访问登录页自动跳转首页
- 路由懒加载优化首屏加载

### 状态管理
- Zustand轻量级状态管理
- Token和用户信息持久化到localStorage
- 登出时自动清除状态

### UI/UX
- Ant Design 5组件库
- 响应式侧边栏导航
- 表格分页和排序
- 操作成功/失败提示
- 确认对话框(删除操作)

## 开发指南

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```
默认端口: `http://localhost:3000`

### 构建生产版本
```bash
npm run build
```

### 代码检查
```bash
npm run lint
```

## 后端API配置

开发环境下,Vite代理配置将 `/api` 请求转发到 `http://localhost:8080`。

生产环境需配置Nginx反向代理或修改 `vite.config.js` 中的 `baseURL`。

## 默认账号

- 用户名: `admin`
- 密码: `admin123`

## 注意事项

1. **JWT Token**: 登录成功后Token存储在localStorage,所有请求自动附加到请求头
2. **路由懒加载**: 所有页面组件使用React.lazy实现懒加载,优化首屏性能
3. **状态持久化**: 使用Zustand的persist中间件持久化用户状态到localStorage
4. **表单验证**: 使用Ant Design Form的rules进行必填项和格式验证
5. **错误处理**: 所有API调用都有try-catch,失败时显示友好错误提示

## 待优化项

- [ ] 添加全局Loading状态
- [ ] 实现歌曲文件上传功能
- [ ] 添加批量操作功能
- [ ] 优化移动端适配
- [ ] 添加暗黑模式支持
- [ ] 实现WebSocket实时通知

## 更新日志

### v1.0.0 (2026-03-30)
- ✅ 完成项目初始化
- ✅ 实现登录页面
- ✅ 实现后台主布局
- ✅ 完成歌手管理CRUD
- ✅ 完成歌曲管理CRUD
- ✅ 完成包厢管理CRUD
- ✅ 完成订单管理(开台/结账/取消)

---

**开发团队**: AI Assistant  
**创建日期**: 2026-03-30
