# MindMe 后端模块架构

本文档描述了 MindMe 后端的内部模块和业务职责。

## 架构概览

MindMe 遵循标准的 Spring Boot 分层架构，并增强了 AI/向量处理流水线。

```text
扩展程序/代理 -> 接入层 -> 原始存储 (Postgres)
                                          |
                                   分析引擎 (AI/定时任务)
                                          |
                                  +-------+-------+
                                  |               |
                           向量存储          知识存储
                           (pgvector)        (Postgres)
```

---

## 核心业务模块

### 1. 分析引擎 (`ScheduledAnalysisService`)
MindMe 智能的核心。
- **触发机制：** 每 5 分钟运行一次（可配置）。
- **流程：**
    1. 获取未分析的 `ActivityRecord`（活动记录）条目。
    2. 使用大语言模型（LLM）提取关键见解、类别和标签。
    3. 为该活动计算“价值评分”（1-10分）。
    4. 将原始内容提炼为结构化的 `KnowledgeEntry`（知识条目）记录。
    5. 更新用户兴趣画像和标签频率。

### 2. AI 与向量处理 (`VectorSearchService`, `AiAnalysisService`)
处理结构化数据与 LLM 之间的接口。
- **VectorSearchService：**
    - 使用 LangChain4j 管理文本分块和嵌入（Embedding）生成。
    - 通过 `pgvector` 在 PostgreSQL 中处理向量相似度搜索。
- **AiAnalysisService：**
    - 为对话接口编排 RAG（检索增强生成）。
    - 执行多活动综合，生成每日/每周/每月报告。
    - 根据用户兴趣为外部趋势进行评分。

### 3. 趋势智能 (`TrendService`)
维护相关外部信息的实时馈送。
- **抓取：** 同时抓取 GitHub、HackerNews、知乎、V2EX、IT之家和掘金。
- **个性化：** 根据用户捕获的兴趣，使用 AI 模型自动为每个新趋势项评分。
- **调度：** 每 5 分钟更新一次趋势数据库。

### 4. 知识图谱模块 (`KnowledgeGraphService`)
将扁平记录转换为网络结构。
- **实体提取：** 识别活动中的主题和实体。
- **关系映射：** 基于共现、序列和 AI 检测到的语义链接创建节点间的边。
- **可视化：** 为 3D 力导向图渲染格式化数据。

### 5. 探索模块 (`CognitiveHorizonService`)
实现“反信息茧房”逻辑。
- **概念发现：** 主动推荐来自无关领域的高级思维模型和理论。
- **类比逻辑：** 使用 LLM 寻找特定原因，说明为什么某个领域（如生物学）的概念与开发者或金融分析师相关。

### 6. 监控与代理 (`PageWatchService`)
驱动自主后台任务。
- **变化检测：** 使用内容哈希检测书签或监控页面上有意义的更新。
- **代理编排：** 管理可分流给浏览器代理的任务，以进行更深入的分析或定期检查。

---

## 数据模型（关键实体）

- **ActivityRecord:** 页面访问的原始捕获（URL、标题、时间、滚动深度）。
- **KnowledgeEntry:** 从一个或多个活动中提炼出的见解。
- **KnowledgeNode/Edge:** 知识图谱的组成部分。
- **TrendItem:** 外部热点话题或新闻项。
- **UserInterest:** 代表用户偏好的加权类别。
- **WatchedPage:** 正在监控内容变化的 URL。

---

## 开发与升级指南 (Developer Guide)

### 1. 如何添加新的趋势来源 (Adding a New Trend Source)
1. 在 `TrendService.java` 中添加一个新的 `fetchXXX` 私有方法。
2. 使用 `OkHttpClient` 请求数据，并解析为 `TrendItem` 列表。
3. 在 `fetchAndProcessTrends` 方法的 `futures` 列表中加入该异步任务。
4. 在 `TrendItem` 构造函数中设置合理的默认分值，AI 随后会进行二次评分。

### 2. 如何自定义 AI 分析提示词 (Customizing AI Prompts)
AI 提示词主要分布在以下位置：
- `AiAnalysisService.java`: 报告生成和相关性评分提示词。
- `CognitiveHorizonService.java`: 认知视界破圈推荐提示词。
- `ScheduledAnalysisService.java`: 网页内容提取与摘要提示词。

### 3. 技术升级路径
- **向量数据库**: 目前使用 `pgvector`，若数据量达到百万级，可考虑迁移至 Qdrant 或 Milvus。
- **消息队列**: 当前异步任务通过 `ExecutorService` 和 Spring `Scheduled` 处理，未来可升级为 RabbitMQ 或 Kafka 以支持更复杂的分布式任务。
- **大模型**: LangChain4j 支持一键切换模型，可通过修改 `application.yml` 配置切换至 Claude 3.5 或本地运行的 Llama 3。
