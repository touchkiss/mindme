<template>
  <WidgetContainer title="AI INSIGHTS" :icon="Cpu" :loading="loading" :error="error" class="ai-suggestions-widget">
    <div v-if="data" class="ai-content">
      <div class="ai-summary font-mono">
        "{{ data.summary }}"
      </div>

      <div class="suggestions-grid">
        <div 
          v-for="(s, i) in data.suggestions" 
          :key="i" 
          class="suggestion-pill"
          @click="googleSearch(s)"
        >
          <span class="pill-icon">➤</span> {{ s }}
        </div>
      </div>

      <div v-if="data.recommendedContent" class="ai-rec-card">
         <div class="rec-header">
             <span class="rec-label">RELEVANT SIGNAL</span>
             <el-icon class="rec-icon"><Link /></el-icon>
         </div>
         <a :href="data.recommendedContent.title ? googleSearchUrl(data.recommendedContent.title) : '#'" target="_blank" class="rec-link">
           {{ data.recommendedContent.title }}
         </a>
         <div class="rec-reason text-muted">{{ data.recommendedContent.reason }}</div>
      </div>
    </div>
    <div v-else-if="!loading && !error" class="empty-placeholder">
        <span>AWAITING INPUT...</span>
    </div>
  </WidgetContainer>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Cpu, Link } from '@element-plus/icons-vue'
import WidgetContainer from './WidgetContainer.vue'

const data = ref(null)
const loading = ref(true)
const error = ref(false)

const API_BASE = 'http://localhost:8091/api'

function googleSearch(query) {
  window.open(`https://www.google.com/search?q=${encodeURIComponent(query)}`, '_blank')
}

function googleSearchUrl(query) {
    return `https://www.google.com/search?q=${encodeURIComponent(query)}`
}

onMounted(() => {
  setTimeout(async () => {
    try {
      const res = await fetch(`${API_BASE}/recommendations/daily-insight`)
      if (!res.ok) throw new Error('Failed to fetch')
      const json = await res.json()
      data.value = typeof json === 'string' ? JSON.parse(json) : json
      
      if (data.value.error) error.value = true;
    } catch (e) {
      console.error(e)
      error.value = true
    } finally {
      loading.value = false
    }
  }, 1000)
})
</script>

<style scoped>
.ai-suggestions-widget {
    height: auto;
}

.ai-content {
    padding: 15px;
    display: flex;
    flex-direction: column;
    gap: 15px;
}

.ai-summary {
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-secondary);
    border-left: 2px solid var(--cyber-secondary);
    padding-left: 10px;
    font-style: italic;
}

.suggestions-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.suggestion-pill {
    padding: 4px 10px;
    background: rgba(139, 92, 246, 0.1);
    border: 1px solid rgba(139, 92, 246, 0.3);
    color: var(--cyber-secondary);
    font-size: 11px;
    cursor: pointer;
    border-radius: 2px;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 4px;
}

.suggestion-pill:hover {
    background: rgba(139, 92, 246, 0.2);
    border-color: var(--cyber-secondary);
    transform: translateY(-1px);
    box-shadow: 0 0 10px rgba(139, 92, 246, 0.2);
}
.pill-icon { font-size: 8px; opacity: 0.7; }

.ai-rec-card {
    background: rgba(0,0,0,0.2);
    padding: 10px;
    border: 1px solid var(--cyber-panel-border);
    transition: border-color 0.3s;
}
.ai-rec-card:hover { border-color: var(--cyber-primary); }

.rec-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.rec-label { font-size: 9px; color: var(--cyber-primary); letter-spacing: 0.1em; font-weight: bold; }
.rec-icon { font-size: 12px; color: var(--cyber-primary); }

.rec-link {
    display: block;
    color: var(--text-primary);
    font-size: 13px;
    text-decoration: none;
    margin-bottom: 4px;
    font-weight: 500;
}
.rec-link:hover { color: var(--cyber-secondary); text-decoration: underline; }

.rec-reason { font-size: 11px; line-height: 1.3; }

.empty-placeholder {
    padding: 20px; text-align: center; color: var(--text-muted); font-family: var(--font-mono); font-size: 11px;
}
</style>
