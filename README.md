# MindMe - Personal Knowledge OS 🧠

**MindMe** is an AI-powered personal knowledge management ecosystem that automatically builds a "Second Brain" from your browsing activity. It tracks your digital footprint, extracts insights using LLMs, and helps you rediscover knowledge through a Cyberpunk-styled Dashboard.

## ✨ Core Features

### 1. 🕸️ Knowledge Graph (Galaxy View)
- Visualize your browsing history as a 3D constellation.
- **Pink Nodes**: Search Topics & Domains.
- **Blue Nodes**: Individual Pages.
- **Links**: Automatically connected via browsing path (Referrer & Search Context).

### 2. 📡 TrendRadar
- Real-time tech news aggregation in your New Tab.
- Sources: **Hacker News**, **GitHub Trending**, **v2ex**. (Extensible)
- **AI Filtering**: Automatically ranks news based on your personal interest profile.

### 3. 🔍 Search Context Integration
- "Link your Thoughts": If you select text on a page and search for it, MindMe links the search query to the original page.
- Helps you retrace *why* you searched for something.

### 4. 📚 Bookmarks Integration
- **Import**: One-click import of your entire Chrome Bookmark tree.
- **Cold Start**: Uses bookmarks to instantly build your Interest Profile.

### 5. 📥 Data Ownership
- **Export**: Full JSON export of all your tracked activities and knowledge nodes.
- **Blacklist**: Privacy controls to ignore specific domains (e.g., banking, email).

### 6. 🎨 Cyberpunk Dashboard
- Replaces Chrome New Tab with a stunning, data-rich HUD.
- **Productivity Clock**: Date/Time.
- **Stats**: Knowledge count, tags, categories.
- **Reading Queue**: Save articles for later reading.

## 🛠️ Tech Stack

### Extension (Client)
- **Framework**: Vue 3 + Vite
- **UI**: Custom "Cyberpunk" CSS System (No external UI libs)
- **Visuals**: `3d-force-graph` (Galaxy), `motion-v` (Animations)

### Backend (Server)
- **Core**: Java 21 + Spring Boot 3.2
- **DB**: PostgreSQL 15+ (JPA/Hibernate)
- **Cache**: Redis (Trend Caching)
- **AI**: LangChain4j + OpenAI Compatible API
- **Migration**: Flyway

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Node.js 20+
- Docker (for Postgres/Redis)

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Start Backend
```bash
cd backend
export OPENAI_API_KEY=your_key
mvn spring-boot:run
```

### 3. Build & Install Extension
```bash
cd extension
npm install
npm run build
```
1. Open Chrome -> `chrome://extensions`
2. Enable "Developer Mode".
3. "Load Unpacked" -> Select `extension/dist`.

## 🔮 Future Roadmap
- [ ] **AI Chat (RAG)**: Chat with your browsing history.
- [ ] **Full Text Search**: Elasticsearch integration.
- [ ] **Cross-Device Sync**: Cloud sync support.

## License
MIT
