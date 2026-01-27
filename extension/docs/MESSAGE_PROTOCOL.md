# MindMe 浏览器扩展消息通信协议

本文档定义了扩展程序不同部分（内容脚本 Content Scripts、后台工作线程 Background Worker 和 UI 页面）之间的通信标准。

## 消息结构

所有消息都遵循标准格式：
```json
{
  "type": "MESSAGE_TYPE",
  "data": { ... payload ... }
}
```

---

## 1. 内容脚本 (Content Script) -> 后台 (Background)

这些消息用于报告用户活动和捕获的内容。

| 类型 | 载荷 (Payload) | 描述 |
| :--- | :--- | :--- |
| `ENGAGEMENT_UPDATE` | `{ scrollDepth, interaction, activeSeconds }` | 更新用于兴趣评分的实时指标。 |
| `SELECTION_UPDATE` | `{ text }` | 报告用户当前选中的文本。 |
| `AI_CONVERSATION` | `{ platform, summary, fullConversation, ... }` | 发送捕获的 AI 聊天会话以进行归档。 |
| `SAVE_NOTE` | `{ content, tags, url, title }` | 通过快速笔记 UI 保存用户笔记。 |
| `WATCH_PAGE` | `{ url, title }` | 请求后台代理监控某个页面。 |

---

## 2. 后台 (Background) -> 内容脚本 (Content Script)

这些消息用于触发浏览器标签页中的 UI 更改。

| 类型 | 载荷 (Payload) | 描述 |
| :--- | :--- | :--- |
| `SHOW_QUICK_NOTE` | `null` | 打开快速笔记输入模态框。 |
| `NOTIFY` | `{ message, style }` | 在页面上显示吐司（Toast）通知。 |

---

## 3. UI (弹出窗口 Popup/新标签页 New Tab) -> 后台 (Background)

这些消息用于控制系统行为并获取状态。

| 类型 | 载荷 (Payload) | 描述 |
| :--- | :--- | :--- |
| `GET_STATS` | `null` | 返回当前队列大小和今日活动计数。 |
| `FORCE_SYNC` | `null` | 触发立即向后端同步数据。 |
| `AGENT_CONTROL` | `{ action: "start"\|"stop" }` | 控制后台浏览器代理。 |
| `AGENT_STATUS` | `null` | 返回代理队列的当前状态。 |

---

## 4. 后台 (Background) -> 后端 API

扩展程序与 MindMe 服务器之间的通信。

| 目的 | 方法 | 接口 (Endpoint) |
| :--- | :--- | :--- |
| 同步活动记录 | `POST` | `/api/ingest/activity` |
| 发送代理结果 | `POST` | `/api/ingest/agent-result` |
| 拉取配置 | `GET` | `/api/config/extension_settings` |
| 保存至阅读队列 | `POST` | `/api/reading-queue` |
