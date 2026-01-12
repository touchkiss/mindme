<template>
  <div class="newtab-app">
    <div class="bg-grid"></div>
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>
    <div class="bg-glow glow-3"></div>
    <div class="scanlines"></div>

    <div class="container">
      <header class="header">
        <div class="logo">
          <span class="logo-icon">🧠</span>
          <ShinyText class="logo-text">MINDME</ShinyText>
        </div>

        <div class="nav-actions">
           <button class="nav-btn icon-btn" @click="showGlobalSearch = true" title="Search (Cmd+K)">🔍</button>
        </div>
        
        <button class="nav-btn" :class="{ active: showGraph }" @click="showGraph = !showGraph">
          {{ showGraph ? '◈ MAIN VIEW' : '◐ GALAXY VIEW' }}
        </button>

        <div class="clock-wrapper">
          <div class="clock">{{ timeString }}</div>
          <div class="clock-date">{{ dateString }}</div>
        </div>
      </header>

      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon">📚</div>
          <div class="stat-value">{{ data.stats?.totalKnowledge || 0 }}</div>
          <div class="stat-label">知识条目</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📁</div>
          <div class="stat-value">{{ data.stats?.totalCategories || 0 }}</div>
          <div class="stat-label">分类</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🏷️</div>
          <div class="stat-value">{{ data.stats?.totalTags || 0 }}</div>
          <div class="stat-label">标签</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">⚡</div>
          <div class="stat-value">{{ data.knowledge?.length || 0 }}</div>
          <div class="stat-label">今日新增</div>
        </div>
      </div>

      <!-- Graph View -->
      <div v-if="showGraph" class="graph-view-wrapper">
        <KnowledgeGraph />
      </div>

      <div class="main-grid" v-if="!showGraph">
        <div class="left-col">
          <AiSuggestions />
          <div class="section">
            <div class="section-header">
              <div class="section-title">✨ 为您推荐</div>
            </div>
            
            <div v-if="loading" class="empty-state">加载中...</div>
            <div v-else-if="data.recommendations?.length" class="recommendation-list">
              <div v-for="(rec, i) in data.recommendations" :key="i" class="recommendation-card" :class="rec.type">
                <div class="rec-reason">{{ rec.reason }}</div>
                <a :href="rec.url" target="_blank" class="rec-title">{{ rec.title }}</a>
                <div class="rec-type-icon">{{ rec.type === 'queue' ? '🔖' : '🎲' }}</div>
              </div>
            </div>
            <div v-else class="empty-state">暂无推荐</div>
          </div>

          <div class="section">
            <div class="section-header">
              <div class="section-title">最新知识</div>
            </div>
            
            <div v-if="loading" class="empty-state">
              <div class="empty-icon">⏳</div>
              <div>加载中...</div>
            </div>
            
            <div v-else-if="error" class="empty-state">
               <div class="empty-icon">⚠️</div>
               <div>无法连接后端服务</div>
            </div>

            <div v-else-if="data.knowledge?.length" class="knowledge-list">
              <div v-for="(k, i) in data.knowledge" :key="i" class="knowledge-item">
                <div class="knowledge-title">{{ k.title }}</div>
                <div class="knowledge-content">{{ (k.content || '').substring(0, 80) }}...</div>
                <div class="knowledge-meta">
                  <span class="tag tag-category">{{ k.category }}</span>
                  <span v-for="t in (k.tags || []).slice(0, 2)" :key="t" class="tag tag-keyword">{{ t }}</span>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <div class="empty-icon">📖</div>
              <div>开始浏览网页来积累知识吧</div>
            </div>
          </div>
          <div class="section">
            <div class="section-header">
              <div class="section-title">阅读队列</div>
            </div>
            
            <div v-if="loadingQueue" class="empty-state">加载中...</div>
            <div v-else-if="queue.length" class="knowledge-list">
              <div v-for="item in queue" :key="item.id" class="knowledge-item queue-item">
                 <div class="knowledge-title">
                    <a :href="item.url" target="_blank" class="queue-link">{{ item.title || item.url }}</a>
                 </div>
                 <div class="knowledge-meta">
                    <span class="queue-date">{{ new Date(item.addedAt).toLocaleDateString() }}</span>
                    <button class="queue-action" @click.stop="markRead(item.id)">✅ 已读</button>
                    <button class="queue-action delete" @click.stop="deleteQueueItem(item.id)">🗑️</button>
                 </div>
              </div>
            </div>
            <div v-else class="empty-state">
               <div class="empty-icon">🔖</div>
               <div>队列为空</div>
            </div>
          </div>
        </div>

        <div class="right-col">
          <TrendRadar />
          <div class="section">
            <div class="section-header">
              <div class="section-title">热门标签</div>
            </div>
            <div v-if="loading" class="tags-cloud"><span class="cloud-tag">加载中...</span></div>
            <div v-else-if="data.tags?.length" class="tags-cloud">
              <span 
                v-for="t in data.tags" 
                :key="t.tag" 
                class="cloud-tag"
                :class="getTagClass(t)"
              >
                {{ t.tag }}
              </span>
            </div>
            <div v-else class="tags-cloud">
              <span class="cloud-tag">暂无标签</span>
            </div>
          </div>

          <div class="section">
            <div class="section-header">
              <div class="section-title">兴趣分布</div>
            </div>
            <div v-if="loading" class="empty-state">加载中...</div>
            <div v-else-if="data.interests?.length" class="interests-list">
              <div v-for="i in data.interests.slice(0, 6)" :key="i.category" class="interest-item">
                <span class="interest-name">{{ i.category }}</span>
                <div class="interest-bar">
                  <div class="interest-fill" :style="{ width: (i.weight / maxInterest * 100).toFixed(0) + '%' }"></div>
                </div>
                <span class="interest-value">{{ i.weight.toFixed(0) }}</span>
              </div>
            </div>
            <div v-else class="empty-state">暂无兴趣数据</div>
          </div>
        </div>
      </div>

      <div class="graph-view" v-else>
        <KnowledgeGraph />
      </div>

      <footer class="footer">
        <a href="#" class="footer-link" @click.prevent="openAdmin">📊 管理面板</a>
        <a href="#" class="footer-link" @click.prevent="exportData">💾 导出数据</a>
        <span>私人知识库 · AI 驱动</span>
      </footer>
    </div>
    <TargetCursor />
    <GlobalSearch v-model="showGlobalSearch" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import KnowledgeGraph from './components/KnowledgeGraph.vue'
import ShinyText from './components/ShinyText.vue';
import AiSuggestions from './components/AiSuggestions.vue';
import TrendRadar from './components/TrendRadar.vue';
// ... imports
import GlobalSearch from './components/GlobalSearch.vue';

// ... state
const showGlobalSearch = ref(false)

const API_BASE = 'http://localhost:8080/api'

// ... exports
// Global shortcut listener is inside GlobalSearch, we just need to pass the model
// State
const loading = ref(true)
const error = ref(false)
const timeString = ref('--:--:--')
const dateString = ref('')
const showGraph = ref(false)
const data = ref({
  stats: {},
  tags: [],
  knowledge: [],
  interests: [],
  recommendations: []
})
const queue = ref([])
const loadingQueue = ref(true)

let clockInterval

const maxTagsFrequency = computed(() => {
  if (!data.value.tags?.length) return 0
  return Math.max(...data.value.tags.map(t => t.frequency))
})

const maxInterest = computed(() => {
  if (!data.value.interests?.length) return 1
  return Math.max(...data.value.interests.map(i => i.weight))
})

function updateClock() {
  const now = new Date()
  timeString.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  dateString.value = now.toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' })
}

function getTagClass(tag) {
  const max = maxTagsFrequency.value
  if (tag.frequency > max * 0.7) return 'hot'
  if (tag.frequency > max * 0.4) return 'warm'
  return ''
}

async function loadData() {
  try {
    loading.value = true
    const res = await fetch(`${API_BASE}/recommendations`)
    if (!res.ok) throw new Error('API error')
    data.value = await res.json()
    error.value = false
  } catch (e) {
    console.error(e)
    error.value = true
  } finally {
    loading.value = false
  }
}

async function loadQueue() {
  try {
    loadingQueue.value = true
    const res = await fetch(`${API_BASE}/reading-queue`)
    if (!res.ok) throw new Error('API error')
    const page = await res.json()
    queue.value = page.content
  } catch (e) {
    console.error("Failed to load queue", e)
  } finally {
    loadingQueue.value = false
  }
}

async function markRead(id) {
  try {
    await fetch(`${API_BASE}/reading-queue/${id}/read`, { method: 'PATCH' })
    await loadQueue()
  } catch (e) {
    console.error(e)
  }
}

async function deleteQueueItem(id) {
    if (!confirm('确定删除?')) return
    try {
        await fetch(`${API_BASE}/reading-queue/${id}`, { method: 'DELETE' })
        await loadQueue()
    } catch (e) {
        console.error(e)
    }
}

function openAdmin() {
  chrome.tabs.create({ url: 'http://localhost:5173' })
}

async function exportData() {
  try {
    const res = await fetch(`${API_BASE}/export`)
    if (!res.ok) throw new Error('Export failed')
    const blob = await res.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `mindme-export-${new Date().toISOString().split('T')[0]}.json`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  } catch (e) {
    console.error('Export error:', e)
    alert('导出失败，请检查后端服务')
  }
}

onMounted(() => {
  updateClock()
  clockInterval = setInterval(updateClock, 1000)
  loadData()
  loadQueue()
  // Refresh data every 5 minutes
  setInterval(() => {
    loadData()
    loadQueue()
  }, 300000)
})

onUnmounted(() => {
  clearInterval(clockInterval)
})
</script>

<style scoped>
.graph-view-wrapper {
  height: calc(100vh - 200px);
  width: 100%;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.98); }
  to { opacity: 1; transform: scale(1); }
}

.newtab-app {
    min-height: 100vh;
}

/* Reusing CSS from newtab.css but pasted here for simplicity */
/* Animated Background */
.bg-grid {
    position: fixed;
    inset: 0;
    background: 
        linear-gradient(90deg, rgba(0,255,255,0.03) 1px, transparent 1px),
        linear-gradient(rgba(0,255,255,0.03) 1px, transparent 1px);
    background-size: 60px 60px;
    animation: gridMove 20s linear infinite;
    z-index: 0;
}

@keyframes gridMove {
    0% { transform: translate(0, 0); }
    100% { transform: translate(60px, 60px); }
}

.bg-glow {
    position: fixed;
    width: 600px;
    height: 600px;
    border-radius: 50%;
    filter: blur(150px);
    opacity: 0.15;
    animation: float 10s ease-in-out infinite;
    z-index: 0;
}

.glow-1 {
    top: -200px;
    left: -100px;
    background: var(--cyber-cyan);
}

.glow-2 {
    bottom: -200px;
    right: -100px;
    background: var(--cyber-purple);
    animation-delay: -5s;
}

.glow-3 {
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: var(--cyber-pink);
    width: 400px;
    height: 400px;
    animation-delay: -2.5s;
}

@keyframes float {
    0%, 100% { transform: translate(0, 0) scale(1); }
    50% { transform: translate(30px, -30px) scale(1.1); }
}

.scanlines {
    position: fixed;
    inset: 0;
    background: repeating-linear-gradient(
        0deg, transparent, transparent 2px,
        rgba(0, 0, 0, 0.15) 2px, rgba(0, 0, 0, 0.15) 4px
    );
    pointer-events: none;
    z-index: 9999;
}

.container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 40px 40px;
    position: relative;
    z-index: 10;
}

/* Header */
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 50px;
    padding: 20px 30px;
    background: var(--cyber-panel);
    border: 1px solid var(--cyber-border);
    clip-path: polygon(0 0, calc(100% - 30px) 0, 100% 30px, 100% 100%, 30px 100%, 0 calc(100% - 30px));
}

.logo {
    display: flex;
    align-items: center;
    gap: 15px;
}

.logo-icon {
    font-size: 40px;
    animation: pulse 2s infinite;
}

@keyframes pulse {
    0%, 100% { transform: scale(1); filter: drop-shadow(0 0 10px var(--cyber-cyan)); }
    50% { transform: scale(1.1); filter: drop-shadow(0 0 20px var(--cyber-cyan)); }
}

.logo-text {
    font-family: 'Orbitron', monospace;
    font-size: 32px;
    font-weight: 700;
    background: linear-gradient(135deg, var(--cyber-cyan), var(--cyber-green), var(--cyber-purple));
    background-size: 200% 200%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    animation: gradient 3s ease infinite;
}

@keyframes gradient {
    0%, 100% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
}

.clock {
    font-family: 'Orbitron', monospace;
    font-size: 24px;
    color: var(--cyber-cyan);
    text-shadow: 0 0 20px var(--cyber-cyan);
}

.clock-date {
    font-size: 14px;
    color: var(--text-secondary);
    text-align: right;
    margin-top: 5px;
}

/* Stats */
.stats-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 40px;
}

.stat-card {
    background: var(--cyber-panel);
    border: 1px solid var(--cyber-border);
    padding: 25px;
    text-align: center;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;
}

.stat-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(0,255,255,0.1), transparent);
    transition: 0.5s;
}

.stat-card:hover::before {
    left: 100%;
}

.stat-card:hover {
    border-color: var(--cyber-cyan);
    box-shadow: 0 0 30px rgba(0,255,255,0.2), inset 0 0 30px rgba(0,255,255,0.05);
    transform: translateY(-5px);
}

.stat-icon {
    font-size: 30px;
    margin-bottom: 10px;
}

.stat-value {
    font-family: 'Orbitron', monospace;
    font-size: 42px;
    font-weight: 700;
    background: linear-gradient(180deg, var(--cyber-cyan) 0%, var(--cyber-green) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.stat-label {
    font-size: 13px;
    color: var(--text-secondary);
    letter-spacing: 0.15em;
    text-transform: uppercase;
    margin-top: 8px;
}

/* Main Grid */
.main-grid {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 30px;
}

.left-col, .right-col {
  min-width: 0; /* Crucial for text-overflow to work in grid */
}

/* Section */
.section {
    background: var(--cyber-panel);
    border: 1px solid var(--cyber-border);
    padding: 25px;
    margin-bottom: 30px;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid var(--cyber-border);
}

.section-title {
    font-family: 'Orbitron', monospace;
    font-size: 16px;
    color: var(--cyber-cyan);
    letter-spacing: 0.1em;
    display: flex;
    align-items: center;
    gap: 10px;
}

.section-title::before {
    content: '';
    width: 4px;
    height: 20px;
    background: var(--cyber-cyan);
    box-shadow: 0 0 10px var(--cyber-cyan);
}

/* Knowledge Cards */
.queue-item {
    border-color: rgba(255, 204, 0, 0.2);
    background: rgba(255, 204, 0, 0.02);
}
.queue-item:hover {
    border-color: var(--cyber-yellow);
    background: rgba(255, 204, 0, 0.05);
}
.queue-link {
    color: var(--text-primary);
    text-decoration: none;
    transition: color 0.2s;
}
.queue-link:hover {
    color: var(--cyber-yellow);
}
.queue-date {
    font-size: 11px;
    color: var(--text-secondary);
    margin-right: auto;
}
.queue-action {
    background: none;
    border: 1px solid var(--cyber-border);
    color: var(--cyber-cyan);
    font-size: 11px;
    padding: 2px 6px;
    cursor: pointer;
    transition: all 0.2s;
}
.queue-action:hover {
    background: rgba(0, 255, 255, 0.1);
}
.queue-action.delete {
    color: var(--cyber-pink);
    border-color: rgba(255, 0, 128, 0.3);
}
.queue-action.delete:hover {
    background: rgba(255, 0, 128, 0.1);
}

.knowledge-list {
    display: flex;
    flex-direction: column;
    gap: 15px;
    max-height: 400px;
    overflow-y: auto;
}

.knowledge-item {
    background: rgba(0,255,255,0.03);
    border: 1px solid rgba(0,255,255,0.15);
    padding: 18px;
    transition: all 0.3s;
    cursor: pointer;
}

.knowledge-item:hover {
    border-color: var(--cyber-cyan);
    background: rgba(0,255,255,0.08);
    transform: translateX(5px);
}

.knowledge-title {
    font-size: 15px;
    color: var(--text-primary);
    margin-bottom: 8px;
    font-weight: 600;
}

.knowledge-content {
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.5;
    margin-bottom: 10px;
}

.knowledge-meta {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
}

.tag {
    padding: 4px 10px;
    font-size: 11px;
    border: 1px solid;
}

.tag-category {
    border-color: rgba(191,95,255,0.4);
    color: var(--cyber-purple);
    background: rgba(191,95,255,0.1);
}

.tag-keyword {
    border-color: rgba(0,255,255,0.3);
    color: var(--cyber-cyan);
    background: rgba(0,255,255,0.05);
}

/* Tags Cloud */
.tags-cloud {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.cloud-tag {
    padding: 8px 16px;
    background: rgba(0,255,255,0.05);
    border: 1px solid rgba(0,255,255,0.2);
    color: var(--cyber-cyan);
    font-size: 13px;
    transition: all 0.2s;
    cursor: pointer;
}

.cloud-tag:hover {
    background: rgba(0,255,255,0.15);
    transform: scale(1.05);
    box-shadow: 0 0 15px rgba(0,255,255,0.3);
}

.cloud-tag.hot {
    border-color: var(--cyber-pink);
    color: var(--cyber-pink);
    background: rgba(255,0,128,0.1);
}

.cloud-tag.warm {
    border-color: var(--cyber-yellow);
    color: var(--cyber-yellow);
    background: rgba(255,204,0,0.1);
}

/* Interests */
.interest-item {
    display: flex;
    align-items: center;
    gap: 15px;
    margin-bottom: 12px;
}

.interest-name {
    width: 100px;
    font-size: 13px;
    color: var(--text-primary);
}

.interest-bar {
    flex: 1;
    height: 10px;
    background: rgba(0,255,255,0.1);
    border-radius: 5px;
    overflow: hidden;
}

.interest-fill {
    height: 100%;
    background: linear-gradient(90deg, var(--cyber-cyan), var(--cyber-green));
    border-radius: 5px;
    transition: width 1s ease;
    box-shadow: 0 0 10px var(--cyber-cyan);
}

.interest-value {
    width: 40px;
    text-align: right;
    font-family: 'Orbitron', monospace;
    font-size: 12px;
    color: var(--cyber-cyan);
}

/* Footer */
.footer {
    text-align: center;
    padding: 30px;
    color: var(--text-secondary);
    font-size: 13px;
}

.footer-link {
    color: var(--cyber-cyan);
    text-decoration: none;
    transition: all 0.2s;
    padding: 8px 20px;
    border: 1px solid var(--cyber-border);
    margin: 0 10px;
}

.footer-link:hover {
    background: rgba(0,255,255,0.1);
    box-shadow: 0 0 15px rgba(0,255,255,0.3);
}

/* Scrollbar */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: rgba(0,255,255,0.05); }
::-webkit-scrollbar-thumb { background: var(--cyber-cyan); border-radius: 3px; }

.empty-state {
    text-align: center;
    padding: 40px;
    color: var(--text-secondary);
}

.empty-icon {
    font-size: 48px;
    margin-bottom: 15px;
    opacity: 0.5;
}

.nav-btn {
    background: rgba(0,255,255,0.05);
    border: 1px solid var(--cyber-border);
    color: var(--cyber-cyan);
    font-family: 'Orbitron', monospace;
    font-size: 14px;
    padding: 8px 16px;
    cursor: pointer;
    transition: all 0.3s;
    margin-right: auto;
    margin-left: 30px;
}

.nav-btn:hover, .nav-btn.active {
    background: rgba(0,255,255,0.15);
    box-shadow: 0 0 15px rgba(0,255,255,0.3);
}

.graph-view {
    height: 600px;
    background: rgba(0, 10, 20, 0.5);
    border: 1px solid var(--cyber-border);
    position: relative;
}

/* Recommendations */
.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.recommendation-card {
  background: rgba(0, 255, 255, 0.05);
  border: 1px solid var(--cyber-border);
  padding: 15px;
  position: relative;
  transition: all 0.3s;
  overflow: hidden;
}

.recommendation-card:hover {
  transform: translateX(5px);
  border-color: var(--cyber-yellow);
  box-shadow: 0 0 15px rgba(255, 204, 0, 0.1);
}

.recommendation-card.queue {
  border-left: 3px solid var(--cyber-yellow);
}

.recommendation-card.rediscover {
  border-left: 3px solid var(--cyber-pink);
}

.rec-reason {
  font-size: 11px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 5px;
}

.rec-title {
  display: block;
  font-size: 14px;
  color: var(--text-primary);
  text-decoration: none;
  font-weight: 600;
  padding-right: 30px;
}

.rec-title:hover {
  color: var(--cyber-yellow);
}

.rec-type-icon {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  opacity: 0.5;
}
</style>
