<template>
  <div class="section trend-radar">
    <div class="section-header">
      <div class="section-title">📡 Trend Radar</div>
      <div class="radar-scan"></div>
    </div>
    
    <div class="radar-header-controls">
        <label class="ai-toggle">
            <input type="checkbox" v-model="aiEnabled" @change="loadTrends">
            <span class="ai-label">AI Filter</span>
        </label>
    </div>
    
    <div v-if="loading" class="radar-loading">
        <div class="scan-line"></div>
        扫描全网热点...
    </div>
    
    <div v-else-if="trends.length" class="trend-list">
      <a 
        v-for="(item, i) in trends" 
        :key="item.url" 
        :href="item.url"
        target="_blank"
        class="trend-item"
        :style="{ animationDelay: `${i * 100}ms` }"
      >
        <span class="trend-badge" :class="item.source.toLowerCase()">{{ item.source }}</span>
        <span class="trend-title" :title="item.title">{{ item.title }}</span>
        <span class="trend-score">{{ item.score }}</span>
      </a>
    </div>
    
    <div v-else class="empty-state">
      <div class="empty-icon">📵</div>
      暂无信号
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const API_BASE = 'http://localhost:8080/api'
const trends = ref([])
const loading = ref(true)
const aiEnabled = ref(false)

async function loadTrends() {
  try {
    loading.value = true
    const res = await fetch(`${API_BASE}/trends?aiFilter=${aiEnabled.value}`)
    if (!res.ok) throw new Error('Failed to fetch trends')
    trends.value = await res.json()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTrends()
})
</script>

<style scoped>
.trend-radar {
    position: relative;
    overflow: hidden;
    border-color: var(--cyber-pink);
}

.radar-scan {
    width: 20px;
    height: 20px;
    border: 2px solid var(--cyber-pink);
    border-radius: 50%;
    position: absolute;
    right: 20px;
    top: 20px;
    opacity: 0.5;
}
.radar-scan::after {
    content: '';
    position: absolute;
    top: 50%; left: 50%;
    width: 2px; height: 50%;
    background: var(--cyber-pink);
    transform-origin: top center;
    animation: radarSpin 2s linear infinite;
}

@keyframes radarSpin {
    from { transform: translate(-50%, 0) rotate(0deg); }
    to { transform: translate(-50%, 0) rotate(360deg); }
}

.trend-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.trend-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px;
    background: rgba(255, 0, 128, 0.05);
    border: 1px solid rgba(255, 0, 128, 0.1);
    color: var(--text-primary);
    text-decoration: none;
    transition: all 0.2s;
    animation: slideIn 0.5s ease backwards;
}

@keyframes slideIn {
    from { opacity: 0; transform: translateX(20px); }
    to { opacity: 1; transform: translateX(0); }
}

.trend-badge {
    font-size: 10px;
    padding: 2px 4px;
    border-radius: 2px;
    background: #444;
    color: #fff;
    min-width: 45px;
    text-align: center;
}
.trend-badge.hackernews { background: #ff6600; }
.trend-badge.ithome { background: #d22222; }
.trend-badge.zhihu { background: #0084ff; }
.trend-badge.github { background: #333; }

.trend-item:hover {
    background: rgba(255, 0, 128, 0.15);
    border-color: var(--cyber-pink);
    transform: translateX(-5px);
}

.trend-score {
    font-family: 'Orbitron', monospace;
    font-size: 12px;
    color: var(--cyber-pink);
    min-width: 25px;
    text-align: right;
}

.trend-title {
    flex: 1;
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.radar-loading {
    padding: 20px;
    text-align: center;
    color: var(--cyber-pink);
    font-family: 'Orbitron', monospace;
    font-size: 12px;
    animation: pulse 1s infinite alternate;
}
</style>
