# MindMe Extension

MindMe Chrome 浏览器扩展，作为系统的感知终端，负责自动追踪用户的工作流、捕捉 AI 对话灵感，并提供沉浸式的个人知识管理入口。

## 技术栈

- **前端框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **UI 组件库**: Element Plus
- **可视化库**:
  - **3d-force-graph**: 用于展示动态 3D 知识图谱。
  - **D3.js**: 辅助数据处理。
- **通信**: Axios / Fetch (与后端 API 交互)
- **渲染**: Marked (支持 Markdown 格式摘要显示)

## 核心功能

### 1. 智能活动追踪 (Intelligent Tracking)
- **兴趣度算法**: `background/index.js` 实时计算用户对网页的兴趣得分（基于停留时长、滚动深度、交互频率及来源类型）。
- **上下文感知**: 自动关联搜索查询与点击结果，构建知识链路。
- **隐私保护**: 内置域名黑名单过滤功能。

### 2. AI 对话捕捉 (AI Chat Capture)
- **多平台支持**: 自动识别并捕捉 **ChatGPT**, **DeepSeek**, **Gemini** 的对话内容。
- **自动归档**: 在对话结束或切换页面时，自动将对话摘要和全文发送至后端知识库。
- **无感体验**: 通过 `MutationObserver` 实现后台静默捕捉，不干扰正常对话。

### 3. New Tab 沉浸式仪表盘
- **Trend Radar (趋势雷达)**: 聚合多平台热点，支持 AI 相关性过滤。
- **Cognitive Horizon (认知视界)**: 每日推荐跨学科思维模型，助你打破信息茧房。
- **3D 知识图谱**: 直观展示个人知识库的结构与关联。
- **Admin 管理后台**: 在新标签页中直接管理活动记录、知识点和系统配置。

### 4. 高效交互工具
- **快捷键支持**: `Cmd/Ctrl + Shift + M` 快速呼出笔记窗口。
- **上下文菜单**:
  - 👁️ **页面监控**: 监控网页更新。
  - 📥 **加入阅读队列**: 快速标记待读内容。
  - 📝 **保存选段**: 选中文字直接存入知识库。

## 开发指南

### 开发者文档
- [插件架构说明](docs/EXTENSION_ARCH.md): 详细描述了 Background、Content Scripts 及 New Tab 的逻辑架构。
- [消息通信协议](docs/MESSAGE_PROTOCOL.md): 插件内部各组件（Content, Background, UI）及与后端 API 的通信标准。

### 环境准备
- Node.js 18+
- npm 或 pnpm

### 安装与运行
1. **安装依赖**:
   ```bash
   npm install
   ```
2. **开发模式 (热重载)**:
   ```bash
   npm run dev
   ```
3. **构建生产版本**:
   ```bash
   npm run build
   ```

### 加载扩展程序
1. 打开 Chrome，进入 `chrome://extensions/`。
2. 开启 "开发者模式"。
3. 点击 "加载已解压的扩展程序"，选择项目下的 `dist` 目录。

## 配置说明
默认后端地址为 `http://localhost:8091`。如需修改，请在扩展程序的“选项”页面或 `background/index.js` 中调整 `DEFAULT_SERVER_URL`。
