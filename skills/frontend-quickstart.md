# 前端快速上手指南

本文档帮助你快速上手 KTV 点歌系统的前端开发（包括 admin-frontend 和 room-frontend）。

## 环境准备

### 必需软件

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| Node.js | 18+ | 必须是 Node.js 18 或更高版本 |
| npm | 9+ | 包管理器 |
| 浏览器 | Chrome 90+ | 开发调试 |

### 安装检查

```bash
# 检查 Node.js 版本
node -v

# 检查 npm 版本
npm -v
```

---

## 项目结构

### admin-frontend（后台管理端）

```
admin-frontend/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API 接口封装
│   │   ├── index.js       # Axios 实例配置
│   │   ├── auth.js        # 认证接口
│   │   ├── song.js        # 歌曲接口
│   │   ├── singer.js      # 歌手接口
│   │   ├── room.js        # 包厢接口
│   │   └── order.js       # 订单接口
│   ├── pages/             # 页面组件
│   │   ├── Login/         # 登录页
│   │   │   └── index.jsx
│   │   ├── Song/          # 歌曲管理
│   │   │   ├── index.jsx
│   │   │   └── Form.jsx
│   │   ├── Singer/        # 歌手管理
│   │   ├── Room/          # 包厢管理
│   │   └── Order/         # 订单管理
│   ├── components/        # 公共组件
│   ├── layouts/           # 布局组件
│   │   └── MainLayout.jsx
│   ├── router/            # 路由配置
│   │   └── index.jsx
│   ├── store/             # Zustand 状态管理
│   │   └── useAuthStore.js
│   ├── utils/             # 工具函数
│   ├── App.jsx            # 根组件
│   └── main.jsx           # 入口文件
├── vite.config.js         # Vite 配置
└── package.json
```

### room-frontend（包厢点歌端）

```
room-frontend/
├── public/
├── src/
│   ├── api/               # API 接口封装
│   │   ├── index.js
│   │   ├── song.js
│   │   ├── queue.js
│   │   └── play.js
│   ├── pages/             # 页面组件
│   │   ├── Join/          # 加入包厢页
│   │   ├── Search/        # 歌曲检索页
│   │   │   └── index.jsx
│   │   └── Queue/         # 已点/已唱列表
│   │       └── index.jsx
│   ├── components/        # 公共组件
│   │   ├── PlayBar.jsx    # 底部播放控制条（全局）
│   │   ├── SongCard.jsx   # 歌曲卡片
│   │   └── SingerCard.jsx # 歌手卡片
│   ├── router/            # 路由配置
│   │   └── index.jsx
│   ├── store/             # Zustand 状态管理
│   │   └── useRoomStore.js
│   ├── utils/             # 工具函数
│   ├── App.jsx
│   └── main.jsx
├── vite.config.js
└── package.json
```

---

## 启动项目

### 1. 安装依赖

```bash
# 进入后台前端目录
cd admin-frontend

# 安装依赖
npm install

# 或使用 pnpm（推荐，速度更快）
pnpm install
```

### 2. 配置 API 代理

编辑 `vite.config.js`：

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

### 3. 启动开发服务器

```bash
# 后台前端（端口 3000）
npm run dev

# 包厢前端（端口 3001）
cd ../room-frontend
npm run dev
```

### 4. 访问应用

- 后台管理端：http://localhost:3000
- 包厢点歌端：http://localhost:3001

---

## 开发流程

### 创建新页面（示例：添加"歌单管理"页面）

#### 1. 创建 API 接口文件

```javascript
// admin-frontend/src/api/playlist.js
import request from './index';

// 查询列表
export function getPlaylists(params) {
  return request({
    url: '/api/admin/playlists',
    method: 'get',
    params,
  });
}

// 新增
export function createPlaylist(data) {
  return request({
    url: '/api/admin/playlists',
    method: 'post',
    data,
  });
}

// 修改
export function updatePlaylist(id, data) {
  return request({
    url: `/api/admin/playlists/${id}`,
    method: 'put',
    data,
  });
}

// 删除
export function deletePlaylist(id) {
  return request({
    url: `/api/admin/playlists/${id}`,
    method: 'delete',
  });
}
```

#### 2. 创建页面组件

```javascript
// admin-frontend/src/pages/Playlist/index.jsx
import { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getPlaylists, createPlaylist, updatePlaylist, deletePlaylist } from '@/api/playlist';

const Playlist = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form] = Form.useForm();

  // 加载列表
  const loadData = async () => {
    setLoading(true);
    try {
      const res = await getPlaylists({ pageNum: 1, pageSize: 10 });
      setData(res.data.records);
    } catch (error) {
      message.error('加载失败');
    } finally {
      setLoading(false);
    }
  };

  // 新增/修改
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingId) {
        await updatePlaylist(editingId, values);
        message.success('修改成功');
      } else {
        await createPlaylist(values);
        message.success('新增成功');
      }
      setModalVisible(false);
      loadData();
    } catch (error) {
      message.error('操作失败');
    }
  };

  // 删除
  const handleDelete = async (id) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除吗？',
      onOk: async () => {
        try {
          await deletePlaylist(id);
          message.success('删除成功');
          loadData();
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  useEffect(() => {
    loadData();
  }, []);

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '歌单名称', dataIndex: 'name' },
    { title: '描述', dataIndex: 'description' },
    { title: '歌曲数量', dataIndex: 'songCount' },
    {
      title: '操作',
      width: 150,
      render: (text, record) => (
        <div>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => {
              setEditingId(record.id);
              form.setFieldsValue(record);
              setModalVisible(true);
            }}
          >
            编辑
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditingId(null);
            form.resetFields();
            setModalVisible(true);
          }}
        >
          新增歌单
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
      />

      <Modal
        title={editingId ? '编辑歌单' : '新增歌单'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="歌单名称"
            name="name"
            rules={[{ required: true, message: '请输入歌单名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Playlist;
```

#### 3. 配置路由

编辑 `router/index.jsx`：

```javascript
import { lazy } from 'react';

const Playlist = lazy(() => import('@/pages/Playlist'));

const routes = [
  // ... 其他路由
  {
    path: '/playlist',
    element: <Playlist />,
  },
];
```

#### 4. 在菜单中添加入口

编辑 `layouts/MainLayout.jsx`：

```jsx
<Menu.Item key="playlist" icon={<UnorderedListOutlined />}>
  <Link to="/playlist">歌单管理</Link>
</Menu.Item>
```

---

## 常用代码片段

### 表格列表

```javascript
<Table
  columns={columns}
  dataSource={data}
  loading={loading}
  rowKey="id"
  pagination={{
    current: pageNum,
    pageSize: pageSize,
    total: total,
    onChange: (page, size) => {
      setPageNum(page);
      setPageSize(size);
    },
  }}
/>
```

### 表单提交

```javascript
const [form] = Form.useForm();

const handleSubmit = async () => {
  try {
    const values = await form.validateFields();
    await createData(values);
    message.success('提交成功');
    form.resetFields();
  } catch (error) {
    message.error('提交失败');
  }
};
```

### Modal 弹窗

```javascript
const [modalVisible, setModalVisible] = useState(false);

<Modal
  title="标题"
  open={modalVisible}
  onOk={handleOk}
  onCancel={() => setModalVisible(false)}
>
  {/* 内容 */}
</Modal>
```

### Zustand 状态管理

```javascript
// store/useAuthStore.js
import { create } from 'zustand';

const useAuthStore = create((set) => ({
  token: localStorage.getItem('token') || '',
  setToken: (token) => {
    localStorage.setItem('token', token);
    set({ token });
  },
  clearToken: () => {
    localStorage.removeItem('token');
    set({ token: '' });
  },
}));

export default useAuthStore;

// 在组件中使用
const { token, setToken } = useAuthStore();
```

---

## 调试技巧

### 1. 查看网络请求

打开浏览器开发者工具（F12）→ Network 标签页：
- 查看请求 URL、方法、参数
- 查看响应数据
- 查看请求耗时

### 2. 查看控制台日志

```javascript
console.log('调试信息', data);
console.error('错误信息', error);
```

### 3. 断点调试

在 VS Code 中：
1. 在代码行号左侧点击，设置断点
2. 按 F5 启动调试
3. 触发断点后，使用调试工具栏查看变量值

---

## 常见错误

### 1. `CORS error`

**原因**：跨域问题

**解决**：
- 检查 `vite.config.js` 中的 `proxy` 配置
- 检查后端 `CorsConfig.java` 是否允许前端域名

### 2. `401 Unauthorized`

**原因**：Token 过期或未登录

**解决**：
- 检查 `localStorage` 中是否有 `token`
- 重新登录获取新 Token

### 3. `Module not found`

**原因**：依赖未安装或路径错误

**解决**：
- 运行 `npm install`
- 检查 import 路径是否正确

---

## 下一步

- 阅读 [项目设计文档](../docs/project-overview.md)
- 查看 [开发任务列表](../docs/tasks/ktv-song-system-tasklist.md)
- 了解 Ant Design 组件库：https://ant.design/components/overview-cn/
- 了解 Ant Design Mobile 组件库：https://mobile.ant.design/zh/components/overview/

---

**作者**：shaun.sheng

祝你编码愉快！🎨
