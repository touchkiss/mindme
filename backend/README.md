# MindMe Backend

MindMe 后端服务，作为个人知识库的核心引擎，负责数据存储、向量检索、知识图谱构建以及 AI 分析。它通过捕获用户的浏览行为，自动构建个人知识体系，并提供智能化的发现与回顾功能。

## 技术栈

- **核心框架**: Java 17, Spring Boot 3.2
- **数据库**:
    - **PostgreSQL**: 存储结构化业务数据。
    - **pgvector**: 扩展插件，用于存储高维向量数据并实现语义搜索。
    - **Redis**: 用于高速缓存、任务频率限制及 `CognitiveHorizon` 的定时缓存。
- **AI & LLM**:
    - **LangChain4j**: 强大的 Java LLM 集成框架，支持多种模型适配。
    - **OpenAI / 兼容接口**: 用于文本嵌入 (Embeddings) 和智能对话 (Chat) 模型。
- **工具与集成**:
    - **Flyway**: 数据库版本管理与自动迁移。
    - **Lombok**: 减少冗余代码。
    - **OkHttp**: 高效的 HTTP 客户端，用于抓取外部趋势数据。

## 核心功能模块

### 1. 数据摄入与处理 (Ingestion)
- **活动追踪**: 接收扩展端发送的网页停留时长、转换类型及搜索查询 (`ActivityController`)。
- **内容清洗**: 自动过滤无关信息，提取网页核心内容进行持久化。

### 2. 向量检索与语义搜索 (Vector Search)
- **Embedding 转换**: 使用 `VectorSearchService` 调用 LLM 模型将文本转化为向量。
- **语义关联**: 支持跨语言、跨文档的语义相似度检索，打破关键词匹配的局限。

### 3. 知识图谱 (Knowledge Graph)
- **动态图谱**: 基于 AI 提取的实体 (`KnowledgeNode`) 及其关系 (`KnowledgeEdge`) 自动构建图谱。
- **关联发现**: 自动识别不同知识点之间的潜在联系，辅助形成系统化思维。

### 4. 认知视界 (Cognitive Horizon)
- **破圈推荐**: `CognitiveHorizonService` 根据用户近期兴趣点，通过 AI 推荐跨学科的思维模型或理论（如第一性原理、帕累托法则）。
- **过滤气泡打破**: 旨在帮助用户跳出信息茧房，扩展认知边界。

### 5. 趋势雷达 (Trend Radar)
- **多源聚合**: `TrendService` 定时抓取 GitHub, HackerNews, V2EX, 知乎, ITHome, 掘金等平台的实时热点。
- **AI 评分**: 利用 AI 对趋势内容进行相关性评分，优先展示符合用户知识背景的高价值信息。

### 6. 阅读队列与自动化分析
- **待办管理**: 自动管理待阅读队列，支持一键加入与状态追踪。
- **异步分析**: `ScheduledAnalysisService` 定期汇总分析历史活动，生成每日回顾与深度报告。

## 快速开始

### 开发文档
- [REST API 接口文档](docs/API.md): 详细的接口路径、方法及功能描述。
- [模块架构说明](docs/MODULES.md): 后端服务模块划分、核心 Service 逻辑及数据模型。

### 前置要求
- JDK 17+
- Maven 3.8+
- PostgreSQL (需安装并启用 `pgvector` 扩展)
- Redis

### 配置步骤
1. 修改 `src/main/resources/application.yml`：
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/mindme
       username: your_username
       password: your_password

   langchain4j:
     open-ai:
       chat-model:
         api-key: ${OPENAI_API_KEY}
         base-url: https://api.openai.com/v1
   ```

### 运行
```bash
mvn spring-boot:run
```

## API 测试
项目根目录提供 `api_test.sh` 用于快速验证核心接口可用性。
