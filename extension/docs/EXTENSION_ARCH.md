# MindMe 浏览器扩展架构

本文档描述了 MindMe 浏览器扩展的内部结构、组件和业务逻辑。

## 概览

该扩展程序充当 MindMe 生态系统的“外周神经系统”。它负责捕获用户行为，从网页和 AI 聊天中提取内容，并为知识管理提供一个集中的仪表盘。

---

## 1. 后台层 (The Coordinator)

后台 Service Worker 作为状态管理和通信的中心枢纽。

### 核心服务 (`background/index.js`)
- **活动监控：** 监听 `chrome.webNavigation` 和 `chrome.tabs` 事件。
- **兴趣评分算法：** 根据以下维度为每次访问计算 0-100 的分数：
    - **意图：** 用户到达页面的方式（直接输入 URL > 搜索结果 > 链接跳转）。
    - **参与度：** 标签页停留时间、滚动深度以及键盘/鼠标交互。
    - **上下文：** 该访问是否由另一个标签页中的特定文本选择触发。
- **同步引擎：** 管理 `activityQueue`（活动队列）和 `notesQueue`（笔记队列）。实现与后端的定期同步，并通过恢复队列来处理离线或失败的情况。

### 自主代理 (`background/browserAgent.js`)
- **任务队列：** 管理后台任务（例如：定期页面检查、深度爬取）。
- **标签页管理：** 以编程方式在后台打开标签页，以在不干扰用户的情况下提取纯净内容。
- **质量分析：** 评估页面是包含有价值的内容，还是仅为着陆页/错误页面。

---

## 2. 内容层 (The Sensors)

内容脚本（Content Scripts）被注入到网页中以监控交互并提取数据。

### 交互追踪器 (`content/index.js`)
- **参与度指标：** 追踪实时交互信号（滚动、点击、按键）。
- **选择捕获：** 监控文本选择，以进行上下文感知的搜索追踪。
- **UI 组件：** 注入“快速笔记”模态框和悬浮操作按钮 (FAB)。

### AI 聊天捕捉 (`content/aiChatCapture.js`)
- **平台支持：** 针对 ChatGPT、DeepSeek 和 Gemini 的专用抓取器。
- **流式检测：** 使用 `MutationObserver` 在捕捉前检测 AI 响应是否已完成流式传输。
- **时序重构：** 确保用户和助手的交替消息按正确的年代顺序排列。

---

## 3. 新标签页仪表盘 (The UI)

“新标签页”是一个功能齐全的 Vue 3 应用程序。

### Bento Grid 布局
- **挂件（Widgets）：** 专注于特定功能的小组件（趋势雷达、认知视界、阅读队列）。
- **可视化：** 使用 `3d-force-graph` 渲染用户的个人知识图谱。
- **管理界面：** 提供浏览历史、提炼知识和系统设置的管理门户。

### 组件结构
- **`newtab/components/`**: 原子 UI 部件（挂件、雷达、图表）。
- **`newtab/views/`**: 用于管理界面的页面级视图（活动、报告、站点分析）。

---

## 4. 配置与状态管理

- **持久化：** 使用 `chrome.storage.sync` 存储用户设置（服务器 URL、黑名单、阈值）。
- **远程同步：** 启动时自动从后端拉取配置，以确保多个浏览器实例之间的一致性。
- **安全性：** 实现域名黑名单，并在数据离开浏览器之前过滤掉敏感模式。

---

## 扩展与技术升级指南 (Developer & Upgrade Guide)

### 1. 如何添加新的 AI 平台捕捉 (Adding a New AI Platform)
1. 在 `content/aiChatCapture.js` 的 `AI_PLATFORMS` 对象中添加新平台的配置。
2. **hostPatterns**: 匹配平台的域名。
3. **selectors**: 找到用户消息和助手消息的 CSS 选择器。
4. **idExtractor**: 定义如何从 URL 中提取对话 ID。
5. 测试 `MutationObserver` 是否能正确触发。

### 2. 如何添加新的 New Tab 挂件 (Adding a New Widget)
1. 在 `newtab/components/` 下创建新的 Vue 组件。
2. 推荐使用 `WidgetContainer.vue` 作为包装器，以保持 UI 风格一致。
3. 在 `newtab/App.vue` 中引入并放入相应的 Bento Grid 格子中。

### 3. 技术升级路径
- **状态管理**: 目前直接在组件间通信或通过 `chrome.storage` 同步。若复杂度增加，建议引入 **Pinia**。
- **构建工具**: 现有 Vite 配置已支持热重载。若需支持更多浏览器（如 Firefox），可引入 `webextension-polyfill`。
- **UI 风格**: 赛博朋克风格由 `newtab.css` 中的 CSS 变量定义，修改变量即可实现整体换肤。
