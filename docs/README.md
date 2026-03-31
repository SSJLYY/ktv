# KTV点歌系统 - 文档中心

> 欢迎来到KTV点歌系统文档中心。这里包含项目的所有技术文档、设计文档和开发指南。

## 📚 文档索引

### 核心文档

| 文档 | 说明 | 更新时间 |
|------|------|----------|
| [项目概览](project-overview.md) | 项目背景、技术架构、数据库设计、接口设计 | 2026-03-31 |
| [API参考文档](api-reference.md) | 完整的API接口文档，包含请求/响应示例 | 2026-03-31 |
| [代码审查规范](code-review-standards.md) | 代码质量标准与审查checklist | 2026-03-31 |

### 代码审查报告

| 报告 | 说明 | 时间 |
|------|------|------|
| [第一轮审查](code-review-report-2026-03-31.md) | 全量代码审查，发现47项问题 | 2026-03-31 |
| [第二轮审查](code-review-report-round2-2026-03-31.md) | 深度审查，发现4项问题 | 2026-03-31 |

## 🚀 快速开始

### 新手入门

1. 阅读 [项目概览](project-overview.md) 了解系统架构
2. 查看 [根目录 README](../README.md) 快速启动项目
3. 参考 [API参考文档](api-reference.md) 了解接口规范

### 开发者参考

- 数据库表结构：[project-overview.md - 四、数据库设计](project-overview.md#四数据库设计)
- API接口定义：[API参考文档](api-reference.md)
- Redis数据结构：[project-overview.md - 4.3 Redis数据结构设计](project-overview.md#43-redis数据结构设计)
- 技术选型说明：[project-overview.md - 二、技术架构](project-overview.md#二技术架构)

### 运维参考

- 环境要求：[根目录 README - 环境要求](../README.md#环境要求)
- 配置说明：[根目录 README - 配置说明](../README.md#配置说明)
- 开发里程碑：[project-overview.md - 七、开发里程碑](project-overview.md#七开发里程碑)

## 📖 按角色浏览

### 后端开发者

| 文档 | 重点章节 |
|------|----------|
| [项目概览](project-overview.md) | 二、技术架构；四、数据库设计；五、接口设计概要 |
| [API参考文档](api-reference.md) | 全部 |
| [代码审查规范](code-review-standards.md) | 后端规范部分 |

### 前端开发者

| 文档 | 重点章节 |
|------|----------|
| [项目概览](project-overview.md) | 二、技术架构（前端部分）；三、功能模块设计 |
| [API参考文档](api-reference.md) | 接口调用示例 |
| [代码审查规范](code-review-standards.md) | 前端规范部分 |

### 测试人员

| 文档 | 重点章节 |
|------|----------|
| [项目概览](project-overview.md) | 三、功能模块设计（测试用例依据） |
| [API参考文档](api-reference.md) | 接口测试用例编写 |

## 🔧 技术栈速查

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 LTS |
| 框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 6.x+ |
| 后台前端 | React + Ant Design | 18.x / 5.x |
| 包厢前端 | React + Ant Design Mobile | 18.x / 5.x |
| 状态管理 | Zustand | 4.x |
| 构建工具 | Vite | 5.x |

## 📁 文档结构

```
docs/
├── README.md                        # 文档索引（本文件）
├── project-overview.md              # 项目设计文档
├── api-reference.md                 # API参考文档
├── code-review-standards.md         # 代码审查规范
├── code-review-report-2026-03-31.md # 第一轮审查报告
├── code-review-report-round2-2026-03-31.md # 第二轮审查报告
└── tasks/                           # 任务文档目录
```

## 📝 文档更新

文档会随着项目开发持续更新。

- **最后更新时间**：2026-03-31
- **文档版本**：v1.4
- **作者**：shaun.sheng

## 🤝 贡献

如果您发现文档有错误或需要补充，请：

1. 提交 Issue 描述问题
2. 或直接提交 Pull Request 修复

详见 [贡献指南](../CONTRIBUTING.md)
