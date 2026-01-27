# MindMe - 个人知识操作系统 🧠

**MindMe** 是一个 AI 驱动的个人知识管理生态系统，它通过自动追踪你的浏览活动来构建“第二大脑”。它记录你的数字足迹，使用大模型提取见解，并通过赛博朋克风格的仪表盘帮助你重新发现知识。

## ✨ 核心功能

### 1. 🕸️ 知识图谱 (星系视图)
- 将你的浏览历史可视化为 3D 星座。
- **粉色节点**: 搜索主题与域名。
- **蓝色节点**: 独立网页。
- **链接**: 基于浏览路径（来源与搜索上下文）自动连接。

### 2. 📡 趋势雷达 (TrendRadar)
- 新标签页实时聚合技术新闻。
- 来源: **Hacker News**, **GitHub Trending**, **v2ex** (可扩展)。
- **AI 过滤**: 根据你的个人兴趣画像自动对新闻进行排名。

### 3. 🔍 搜索上下文集成
- "连接你的思绪": 如果你在页面上选中一段文本并进行搜索，MindMe 会将搜索查询与原始页面关联起来。
- 帮助你回溯 *为什么* 你要搜索某个内容。

### 4. 📚 书签集成
- **导入**: 一键导入整个 Chrome 书签树。
- **冷启动**: 使用书签即时构建你的兴趣画像。

### 5. 📥 数据所有权
- **导出**: 导出所有追踪活动和知识节点的完整 JSON 数据。
- **黑名单**: 隐私控制，忽略特定域名（如银行、邮件）。

### 6. 🎨 赛博朋克仪表盘
- 用令人惊叹的数据丰富 HUD 替换 Chrome 新标签页。
- **生产力时钟**: 日期/时间。
- **统计**: 知识数量、标签、分类。
- **阅读队列**: 保存文章以便稍后阅读。

## 📂 项目结构

本项目包含两个主要部分，详细文档请查阅各自目录：

- **[Backend (后端)](./backend/README.md)**: 基于 Java Spring Boot 的核心服务，负责数据处理和 AI 分析。
- **[Extension (扩展)](./extension/README.md)**: 基于 Vue 3 的 Chrome 浏览器扩展，负责数据采集和前端展示。

## 🛠️ 技术栈概览

### Extension (客户端)
- **框架**: Vue 3 + Vite
- **UI**: 自定义 "赛博朋克" CSS 系统 (无外部 UI 库)
- **可视化**: `3d-force-graph` (星系), `motion-v` (动画)

### Backend (服务端)
- **核心**: Java 17 + Spring Boot 3.2
- **数据库**: PostgreSQL 15+ (pgvector 向量支持)
- **缓存**: Redis
- **AI**: LangChain4j + OpenAI 兼容 API
- **迁移**: Flyway

## 🚀 快速开始

### 前置要求
- Java 17+
- Node.js 20+
- Docker (用于 Postgres/Redis)

### 1. 启动基础设施
```bash
docker-compose up -d
```

### 2. 启动后端
详细配置请参考 [Backend README](./backend/README.md)。

```bash
cd backend
export OPENAI_API_KEY=your_key
mvn spring-boot:run
```

### 3. 构建并安装扩展
详细开发指南请参考 [Extension README](./extension/README.md)。

```bash
cd extension
npm install
npm run build
```
1. 打开 Chrome -> `chrome://extensions`
2. 开启 "开发者模式" (Developer Mode)。
3. 点击 "加载已解压的扩展程序" (Load Unpacked) -> 选择本项目下的 `extension/dist` 目录。

## 🔮 未来路线图
- [ ] **AI 对话 (RAG)**: 与你的浏览历史对话。
- [ ] **全文搜索**: 集成 Elasticsearch。
- [ ] **多设备同步**: 支持云端同步。

## 许可证
MIT
