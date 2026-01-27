<template>
  <WidgetContainer title="Trend Radar" :icon="Odometer" :loading="loading" class="trend-radar-widget">
      <template #actions>
          <button class="action-icon" @click="refresh" title="Refresh" :disabled="loading">
              <el-icon :class="{ 'is-loading': loading }"><Refresh /></el-icon>
          </button>
          <label class="toggle-switch">
              <input type="checkbox" v-model="aiEnabled" @change="loadTrends">
              <span class="slider"></span>
              <span class="label-text">AI FILTER</span>
          </label>
      </template>

      <div v-if="trends.length" class="trend-list">
        <a 
          v-for="(item, i) in trends" 
          :key="item.url" 
          :href="item.url"
          target="_blank"
          class="trend-item"
          :style="{ animationDelay: `${i * 50}ms` }"
        >
          <span class="trend-rank font-mono">{{ i + 1 }}</span>
          <span class="trend-info">
              <div class="trend-title text-primary">{{ item.title }}</div>
              <div class="trend-meta">
                  <span class="badge" :class="item.source?.toLowerCase()">{{ item.source || 'WEB' }}</span>
                  <span class="score font-mono">{{ item.score }}</span>
              </div>
          </span>
          <div class="trend-graph">
              <div class="bar" :style="{ width: Math.min(item.score / 100, 100) + '%' }"></div>
          </div>
        </a>
      </div>
      
      <div v-else class="empty-placeholder">
        <el-icon class="empty-icon"><Warning /></el-icon>
        <span>NO SIGNAL DETECTED</span>
      </div>

  </WidgetContainer>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, Odometer, Warning } from '@element-plus/icons-vue'
import WidgetContainer from './WidgetContainer.vue'

const API_BASE = 'http://localhost:8091/api'
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

async function refresh() {
    try {
        loading.value = true
        await fetch(`${API_BASE}/trends/refresh`, { method: 'POST' })
        await loadTrends()
    } catch (e) { console.error(e) } 
    finally { loading.value = false }
}

onMounted(() => {
  loadTrends()
})
</script>

<style scoped>
.trend-radar-widget {
    height: 100%;
}

.action-icon {
    background: none;
    border: none;
    color: var(--cyber-primary);
    cursor: pointer;
    padding: 4px;
    display: flex;
    align-items: center;
}
.action-icon:hover { color: #fff; }

.toggle-switch {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    margin-left: 10px;
}
.toggle-switch input { display: none; }
.slider {
    width: 24px;
    height: 12px;
    background: rgba(255,255,255,0.1);
    border-radius: 6px;
    position: relative;
    transition: 0.3s;
}
.slider::after {
    content: '';
    position: absolute;
    top: 1px; left: 1px;
    width: 10px; height: 10px;
    background: #aaa;
    border-radius: 50%;
    transition: 0.3s;
}
.toggle-switch input:checked + .slider { background: rgba(6, 182, 212, 0.3); }
.toggle-switch input:checked + .slider::after { left: 13px; background: var(--cyber-primary); }
.label-text { font-size: 9px; color: var(--text-secondary); font-weight: bold; }

.trend-list {
    padding: 10px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.trend-item {
    display: grid;
    grid-template-columns: 20px 1fr;
    gap: 10px;
    text-decoration: none;
    padding: 6px;
    border-radius: 2px;
    transition: background 0.2s;
    animation: slideIn 0.3s backwards;
}
.trend-item:hover { background: rgba(255,255,255,0.03); }

.trend-rank { color: var(--text-muted); font-size: 12px; text-align: center; }

.trend-info { display: flex; flex-direction: column; gap: 2px; width: 100%; overflow: hidden; }
.trend-title { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.trend-meta { display: flex; align-items: center; gap: 8px; font-size: 10px; color: var(--text-muted); }

.badge { padding: 1px 4px; border-radius: 2px; background: #333; color: #fff; text-transform: uppercase; font-weight: bold; font-size: 9px; }
.badge.hackernews { background: #ff6600; }
.badge.ithome { background: #d22222; }
.badge.zhihu { background: #0084ff; }
.badge.github { background: #24292e; }

.trend-graph {
    grid-column: 2;
    height: 2px;
    background: rgba(255,255,255,0.05);
    margin-top: 4px;
    position: relative;
}
.trend-graph .bar {
    height: 100%;
    background: var(--cyber-secondary);
    box-shadow: 0 0 5px var(--cyber-secondary);
}

@keyframes slideIn {
    from { opacity: 0; transform: translateX(10px); }
    to { opacity: 1; transform: translateX(0); }
}

.empty-placeholder {
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    height: 100%; color: var(--text-muted); font-size: 12px; gap: 10px; opacity: 0.6;
}
.empty-icon { font-size: 24px; }
</style>
