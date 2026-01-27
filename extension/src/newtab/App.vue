<template>
  <div class="newtab-app">
    <!-- Background Effects -->
    <div class="bg-grid"></div>
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>
    <div class="bg-glow glow-3"></div>
    <div class="scanlines"></div>

    <div class="app-container">
      <!-- Top Status Bar -->
      <header class="status-bar">
        <div class="logo-section">
          <el-icon class="logo-icon"><Cpu /></el-icon>
          <span class="logo-text font-heading glitch-text" data-text="MINDME">MINDME</span>
          <span class="version-tag">OS v2.0</span>
        </div>

        <div class="system-stats">
          <div class="stat-item">
            <el-icon><Collection /></el-icon>
            <span class="stat-val">{{ data.stats?.totalKnowledge || 0 }}</span>
            <span class="stat-label">NODES</span>
          </div>
          <div class="stat-item">
            <el-icon><Connection /></el-icon>
            <span class="stat-val">{{ data.stats?.totalCategories || 0 }}</span>
            <span class="stat-label">CATS</span>
          </div>
          <div class="stat-item">
            <el-icon><PriceTag /></el-icon>
            <span class="stat-val">{{ data.stats?.totalTags || 0 }}</span>
            <span class="stat-label">TAGS</span>
          </div>
          <div class="stat-item highlight">
            <el-icon><Lightning /></el-icon>
            <span class="stat-val">+{{ data.knowledge?.length || 0 }}</span>
            <span class="stat-label">NEW</span>
          </div>
        </div>

        <div class="actions-section">
          <div class="clock-display font-mono">
            <span class="time">{{ timeString }}</span>
            <span class="date">{{ dateString }}</span>
          </div>
          
          <div class="action-buttons">
            <button class="icon-btn" @click="showGlobalSearch = true" title="Search (Cmd+K)">
              <el-icon><Search /></el-icon>
            </button>
            <button class="icon-btn" :class="{ active: showGraph }" @click="showGraph = !showGraph" title="Toggle Graph View">
              <el-icon><Share /></el-icon>
            </button>
            <button class="icon-btn warning" @click="toggleAdmin" :class="{ active: showAdmin }" title="Admin Panel">
              <el-icon><Setting /></el-icon>
            </button>
          </div>
        </div>
      </header>

      <!-- Admin Panel Overlay -->
      <div v-if="showAdmin" class="admin-overlay">
         <!-- Reusing existing Admin Layout logic but wrapped nicely -->
         <div class="admin-container">
            <aside class="admin-sidebar">
              <div class="sidebar-header">SYSTEM CONTROL</div>
              <nav class="admin-nav">
                <a @click="adminView = 'dashboard'" :class="{ active: adminView === 'dashboard' }"><el-icon><Odometer /></el-icon> Dashboard</a>
                <a @click="adminView = 'activities'" :class="{ active: adminView === 'activities' }"><el-icon><List /></el-icon> Activities</a>
                <a @click="adminView = 'knowledge'" :class="{ active: adminView === 'knowledge' }"><el-icon><Collection /></el-icon> Knowledge</a>
                <a @click="adminView = 'reports'" :class="{ active: adminView === 'reports' }"><el-icon><DataAnalysis /></el-icon> Reports</a>
                <a @click="adminView = 'sites'" :class="{ active: adminView === 'sites' }"><el-icon><Platform /></el-icon> Sites</a>
              </nav>
            </aside>
            <main class="admin-main">
               <AdminDashboard v-if="adminView === 'dashboard'" @navigate="adminView = $event" />
               <AdminActivities v-if="adminView === 'activities'" />
               <AdminKnowledge v-if="adminView === 'knowledge'" />
               <AdminReports v-if="adminView === 'reports'" />
               <AdminSites v-if="adminView === 'sites'" />
            </main>
         </div>
      </div>

      <!-- Main Dashboard Grid -->
      <div v-else class="bento-grid">
        
        <!-- Graph View Full Overlay within Grid if toggled -->
        <div v-if="showGraph" class="graph-full-cell">
           <KnowledgeGraph />
        </div>

        <template v-else>
            <!-- Left Column: Cognitive & Suggestions -->
            <div class="col-left">
                <!-- AI Suggestions Widget -->
                <div class="cell-suggestions">
                    <AiSuggestions />
                </div>
                
                <!-- Cognitive Widgets -->
                <div class="cell-gap">
                    <KnowledgeGap />
                </div>
                <div class="cell-horizon">
                    <CognitiveHorizon />
                </div>
            </div>

            <!-- Center Column: Main Feed -->
            <div class="col-center">
                
                <!-- Recommendations -->
                <WidgetContainer title="Discovery Feed" :icon="Compass" :loading="loading" class="feed-widget">
                    <div v-if="data.recommendations?.length" class="compact-list">
                        <div v-for="(rec, i) in data.recommendations" :key="i" class="list-item" :class="rec.type">
                            <div class="item-icon">
                                <el-icon v-if="rec.type === 'queue'"><Reading /></el-icon>
                                <el-icon v-else><MagicStick /></el-icon>
                            </div>
                            <div class="item-content">
                                <a :href="rec.url" target="_blank" class="item-title text-primary">{{ rec.title }}</a>
                                <div class="item-meta">{{ rec.reason }}</div>
                            </div>
                            <div class="item-action">
                                <el-icon class="arrow"><Right /></el-icon>
                            </div>
                        </div>
                    </div>
                    <div v-else class="empty-placeholder">NO SIGNAL</div>
                </WidgetContainer>

                <!-- Latest Knowledge -->
                <WidgetContainer title="Incoming Knowledge" :icon="Download" :loading="loading" :error="error" class="feed-widget hover-effect">
                    <div v-if="data.knowledge?.length" class="compact-list knowledge-feed">
                         <div v-for="(k, i) in data.knowledge" :key="i" class="list-item">
                            <div class="item-content">
                                <div class="item-header">
                                    <span class="item-title">{{ k.title }}</span>
                                    <span class="tag-pill">{{ k.category }}</span>
                                </div>
                                <div class="item-desc text-muted">{{ (k.content || '').substring(0, 120) }}...</div>
                                <div class="item-tags">
                                    <span v-for="t in (k.tags || []).slice(0, 3)" :key="t" class="tag-text">#{{ t }}</span>
                                </div>
                            </div>
                         </div>
                    </div>
                     <div v-else class="empty-placeholder">NO DATA</div>
                </WidgetContainer>
            </div>

            <!-- Right Column: Personal & Radar -->
            <div class="col-right">
                <div class="cell-radar">
                    <TrendRadar />
                </div>

                <WidgetContainer title="Reading Queue" :icon="Reading" :loading="loadingQueue" class="queue-widget">
                    <template #actions>
                        <span class="count-badge">{{ queue.length }}</span>
                    </template>
                    <div v-if="queue.length" class="queue-list">
                        <div v-for="item in queue" :key="item.id" class="queue-row">
                             <a :href="item.url" target="_blank" class="queue-link">{{ item.title || item.url }}</a>
                             <div class="row-actions">
                                 <button class="action-btn success" @click.stop="markRead(item.id)" title="Mark Read"><el-icon><Check /></el-icon></button>
                                 <button class="action-btn danger" @click.stop="deleteQueueItem(item.id)" title="Remove"><el-icon><Delete /></el-icon></button>
                             </div>
                        </div>
                    </div>
                     <div v-else class="empty-placeholder">QUEUE EMPTY</div>
                </WidgetContainer>

                <div class="cell-stats-group">
                     <WidgetContainer title="Tags Cloud" :icon="PriceTag" :loading="loading" class="tags-widget">
                        <div class="tags-cloud-flex">
                             <span v-for="t in data.tags?.slice(0, 20)" :key="t.tag" class="cyber-tag" :class="getTagClass(t)">
                                {{ t.tag }} <small>({{t.frequency}})</small>
                             </span>
                        </div>
                     </WidgetContainer>
                     
                     <WidgetContainer title="Interests" :icon="PieChart" :loading="loading" class="interests-widget">
                         <div class="bars-list">
                             <div v-for="i in data.interests?.slice(0, 5)" :key="i.category" class="bar-row">
                                 <div class="bar-label">
                                     <span>{{ i.category }}</span>
                                     <span>{{ i.weight.toFixed(0) }}</span>
                                 </div>
                                 <div class="progress-track">
                                     <div class="progress-fill" :style="{ width: (i.weight / maxInterest * 100) + '%' }"></div>
                                 </div>
                             </div>
                         </div>
                     </WidgetContainer>
                </div>
            </div>
        </template>
      </div>
      
      <footer class="system-footer">
         <div class="footer-msg">MINDME SYSTEM READY</div>
         <a href="#" @click.prevent="exportData" class="footer-action">EXPORT DATA_DUMP</a>
      </footer>

    </div>

    <TargetCursor />
    <GlobalSearch v-model="showGlobalSearch" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, defineAsyncComponent } from 'vue'
import { 
  Cpu, Collection, Connection, PriceTag, Lightning, 
  Search, Share, Setting, Odometer, List, DataAnalysis, Platform,
  Compass, Reading, MagicStick, Right, Download, Warning, Check, Delete, PieChart
} from '@element-plus/icons-vue'

import WidgetContainer from './components/WidgetContainer.vue'
import ShinyText from './components/ShinyText.vue';
import AiSuggestions from './components/AiSuggestions.vue';
import TrendRadar from './components/TrendRadar.vue';
import GlobalSearch from './components/GlobalSearch.vue';
import TargetCursor from './components/TargetCursor.vue';
import KnowledgeGap from './components/KnowledgeGap.vue';
import CognitiveHorizon from './components/CognitiveHorizon.vue';

// Admin views
import AdminDashboard from './views/AdminDashboard.vue';
import AdminActivities from './views/AdminActivities.vue';
import AdminKnowledge from './views/AdminKnowledge.vue';
import AdminReports from './views/AdminReports.vue';
import AdminSites from './views/AdminSites.vue';

const KnowledgeGraph = defineAsyncComponent(() => import('./components/KnowledgeGraph.vue'))

// State
const showAdmin = ref(false)
const adminView = ref('dashboard')
const showGlobalSearch = ref(false)
const showGraph = ref(false)
const loading = ref(true)
const error = ref(false)
const loadingQueue = ref(true)
const timeString = ref('--:--:--')
const dateString = ref('')
const data = ref({ stats: {}, tags: [], knowledge: [], interests: [], recommendations: [] })
const queue = ref([])

const API_BASE = 'http://localhost:8091/api'
let clockInterval

// Computed
const maxTagsFrequency = computed(() => {
  if (!data.value.tags?.length) return 0
  return Math.max(...data.value.tags.map(t => t.frequency))
})

const maxInterest = computed(() => {
  if (!data.value.interests?.length) return 1
  return Math.max(...data.value.interests.map(i => i.weight))
})

// Methods
function toggleAdmin() {
  showAdmin.value = !showAdmin.value
  if (showAdmin.value) adminView.value = 'dashboard'
}

function updateClock() {
  const now = new Date()
  timeString.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  dateString.value = now.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', weekday: 'short' })
}

function getTagClass(tag) {
  const max = maxTagsFrequency.value
  if (tag.frequency > max * 0.7) return 'scale-110 text-primary'
  if (tag.frequency > max * 0.4) return 'text-secondary'
  return 'text-muted'
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
  } catch (e) { console.error("Failed to load queue", e) }
    finally { loadingQueue.value = false }
}

async function markRead(id) {
  try {
    await fetch(`${API_BASE}/reading-queue/${id}/read`, { method: 'PATCH' })
    await loadQueue()
  } catch (e) { console.error(e) }
}

async function deleteQueueItem(id) {
    if (!confirm('Delete item?')) return
    try {
        await fetch(`${API_BASE}/reading-queue/${id}`, { method: 'DELETE' })
        await loadQueue()
    } catch (e) { console.error(e) }
}

async function exportData() {
   /* same export logic */
   try {
    const res = await fetch(`${API_BASE}/export`)
    if (!res.ok) throw new Error('Export failed')
    const blob = await res.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `mindme-export.json`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  } catch (e) { alert('Export failed') }
}

onMounted(() => {
  updateClock()
  clockInterval = setInterval(updateClock, 1000)
  loadData()
  loadQueue()
  setInterval(() => { loadData(); loadQueue() }, 300000)
})

onUnmounted(() => clearInterval(clockInterval))
</script>

<style scoped>
/* Main Layout */
.app-container {
    height: 100vh;
    display: flex;
    flex-direction: column;
    padding: 10px;
    position: relative;
    z-index: 10;
}

/* Header */
.status-bar {
    height: 48px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: var(--cyber-panel);
    border: 1px solid var(--cyber-panel-border);
    padding: 0 20px;
    margin-bottom: 10px;
}

.logo-section {
    display: flex;
    align-items: center;
    gap: 12px;
}
.logo-icon { font-size: 24px; color: var(--cyber-primary); animation: pulse 2s infinite; }
.logo-text { font-size: 20px; font-weight: 700; color: var(--text-primary); letter-spacing: 0.1em; }
.version-tag { font-size: 10px; background: rgba(255,255,255,0.1); padding: 2px 6px; border-radius: 2px; }

.system-stats {
    display: flex;
    gap: 30px;
    align-items: center;
}
.stat-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--text-secondary);
    font-size: 13px;
}
.stat-val { font-family: var(--font-mono); color: var(--text-primary); font-weight: bold; }
.stat-label { font-size: 10px; opacity: 0.7; }
.stat-item.highlight .stat-val { color: var(--cyber-success); }

.actions-section {
    display: flex;
    gap: 20px;
    align-items: center;
}
.clock-display {
    text-align: right;
    line-height: 1.1;
}
.time { display: block; font-size: 16px; color: var(--cyber-primary); }
.date { display: block; font-size: 10px; color: var(--text-secondary); }

.icon-btn {
    background: transparent;
    border: 1px solid transparent;
    color: var(--text-secondary);
    cursor: pointer;
    padding: 6px;
    font-size: 16px;
    border-radius: 4px;
    transition: all 0.2s;
}
.icon-btn:hover, .icon-btn.active {
    color: var(--cyber-primary);
    background: rgba(6, 182, 212, 0.1);
    border-color: rgba(6, 182, 212, 0.3);
}
.icon-btn.warning:hover, .icon-btn.warning.active {
    color: var(--cyber-warning);
    background: rgba(245, 158, 11, 0.1);
    border-color: rgba(245, 158, 11, 0.3);
}

/* Bento Grid */
.bento-grid {
    flex: 1;
    display: grid;
    grid-template-columns: 280px 1fr 340px;
    gap: 10px;
    min-height: 0; /* essential for nested scrolling */
}

/* Columns */
.col-left, .col-center, .col-right {
    display: flex;
    flex-direction: column;
    gap: 10px;
    min-height: 0;
    overflow-y: auto;
    padding-right: 2px;
}

.cell-suggestions { height: auto; }
.cell-gap, .cell-horizon { 
    flex: 1; 
    /* Force specific height distribution if needed, or let flex handle it */
}

.feed-widget {
    flex: 1; /* Split center column space */
    min-height: 300px;
}

.cell-radar { height: 320px; flex-shrink: 0; }
.queue-widget { flex: 1; min-height: 250px; }
.cell-stats-group { display: flex; gap: 10px; height: 200px; flex-shrink: 0; }
.tags-widget, .interests-widget { flex: 1; }

.graph-full-cell {
    grid-column: 1 / -1;
    height: 100%;
    background: rgba(0,0,0,0.5);
    border: 1px solid var(--cyber-panel-border);
}

/* List Styles */
.compact-list {
    display: flex;
    flex-direction: column;
    padding: 8px;
}
.list-item {
    display: flex;
    gap: 12px;
    padding: 10px;
    border-bottom: 1px solid rgba(255,255,255,0.03);
    align-items: center;
    transition: background 0.2s;
}
.list-item:hover { background: rgba(255,255,255,0.03); }
.item-icon { padding: 8px; background: rgba(255,255,255,0.05); border-radius: 4px; display: flex; }
.item-content { flex: 1; min-width: 0; }
.item-title { display: block; font-size: 14px; font-weight: 600; text-decoration: none; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-meta { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.item-desc { font-size: 12px; margin: 4px 0; line-height: 1.4; }
.tag-pill { font-size: 10px; background: rgba(6, 182, 212, 0.1); color: var(--cyber-primary); padding: 1px 6px; border-radius: 2px; margin-left: 8px; }

.knowledge-feed .list-item { align-items: flex-start; }
.tag-text { font-size: 11px; color: var(--text-muted); margin-right: 8px; }

/* Queue Specific */
.queue-list { padding: 4px; }
.queue-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 10px;
    border-bottom: 1px solid rgba(255,255,255,0.03);
    font-size: 13px;
}
.queue-row:hover { background: rgba(255,255,255,0.02); }
.queue-link { color: var(--text-secondary); text-decoration: none; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-right: 10px; }
.queue-link:hover { color: var(--cyber-primary); }
.row-actions { display: flex; gap: 4px; opacity: 0; transition: opacity 0.2s; }
.queue-row:hover .row-actions { opacity: 1; }
.action-btn { background: none; border: none; cursor: pointer; color: var(--text-muted); padding: 4px; }
.action-btn:hover { color: #fff; }
.action-btn.success:hover { color: var(--cyber-success); }
.action-btn.danger:hover { color: var(--cyber-accent); }

.count-badge { font-family: var(--font-mono); font-size: 11px; background: var(--cyber-primary); color: #000; padding: 0 6px; border-radius: 2px; }

/* Tags Cloud */
.tags-cloud-flex { display: flex; flex-wrap: wrap; gap: 6px; padding: 10px; }
.cyber-tag { font-size: 11px; cursor: pointer; transition: 0.2s; color: var(--text-muted); }
.cyber-tag:hover { color: var(--cyber-primary); text-shadow: 0 0 8px var(--cyber-primary); }

/* Bars */
.bars-list { padding: 10px; display: flex; flex-direction: column; gap: 12px; }
.bar-row { display: flex; flex-direction: column; gap: 4px; }
.bar-label { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-secondary); }
.progress-track { height: 4px; background: rgba(255,255,255,0.05); border-radius: 2px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--cyber-secondary); }

.empty-placeholder { display: flex; align-items: center; justify-content: center; height: 100%; color: var(--text-muted); font-family: var(--font-mono); font-size: 12px; letter-spacing: 0.1em; opacity: 0.5; }

/* Footer */
.system-footer { height: 24px; display: flex; justify-content: space-between; align-items: center; padding: 0 10px; font-size: 10px; color: var(--text-muted); font-family: var(--font-mono); }
.footer-action { color: var(--text-muted); text-decoration: none; }
.footer-action:hover { color: var(--cyber-primary); }

/* Admin Overlay */
.admin-overlay {
    position: absolute;
    inset: 60px 10px 10px 10px;
    background: rgba(2, 6, 23, 0.95);
    backdrop-filter: blur(10px);
    border: 1px solid var(--cyber-panel-border);
    z-index: 50;
    display: flex;
}
.admin-container { display: flex; width: 100%; height: 100%; }
.admin-sidebar { width: 220px; border-right: 1px solid var(--cyber-panel-border); display: flex; flex-direction: column; }
.sidebar-header { height: 50px; display: flex; align-items: center; padding-left: 20px; color: var(--cyber-primary); font-family: var(--font-heading); font-size: 12px; letter-spacing: 0.1em; border-bottom: 1px solid var(--cyber-panel-border); }
.admin-nav { padding: 10px; display: flex; flex-direction: column; gap: 4px; }
.admin-nav a { display: flex; align-items: center; gap: 10px; padding: 12px; color: var(--text-secondary); font-size: 13px; cursor: pointer; transition: all 0.2s; border-radius: 4px; }
.admin-nav a:hover, .admin-nav a.active { background: rgba(6, 182, 212, 0.1); color: var(--cyber-primary); }
.admin-main { flex: 1; padding: 20px; overflow: auto; }

/* Animation Utility */
@keyframes pulse {
    0%, 100% { opacity: 1; filter: drop-shadow(0 0 5px var(--cyber-primary)); }
    50% { opacity: 0.7; filter: drop-shadow(0 0 15px var(--cyber-primary)); }
}
</style>
