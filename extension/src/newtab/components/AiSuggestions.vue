<template>
  <div class="ai-suggestions-section">
    <div class="section-header">
      <div class="section-title">🔮 每日 AI 洞察</div>
    </div>

    <div v-if="loading" class="ai-loading">
      <div class="loading-bar"></div>
      <div class="loading-text">正在分析今日浏览行为...</div>
    </div>

    <div v-else-if="error" class="empty-state">
      暂无分析数据
    </div>

    <div v-else class="ai-content">
      <div class="ai-summary">
        "{{ data.summary }}"
      </div>

      <div class="suggestions-grid">
        <div 
          v-for="(s, i) in data.suggestions" 
          :key="i" 
          class="suggestion-pill"
          @click="googleSearch(s)"
        >
          {{ s }}
        </div>
      </div>

      <div v-if="data.recommendedContent" class="ai-rec-card">
         <div class="rec-label">推荐阅读</div>
         <a :href="data.recommendedContent.title ? googleSearchUrl(data.recommendedContent.title) : '#'" target="_blank" class="rec-link">
           {{ data.recommendedContent.title }}
         </a>
         <div class="rec-reason">{{ data.recommendedContent.reason }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const data = ref(null)
const loading = ref(true)
const error = ref(false)

const API_BASE = 'http://localhost:8080/api'

function googleSearch(query) {
  window.open(`https://www.google.com/search?q=${encodeURIComponent(query)}`, '_blank')
}

function googleSearchUrl(query) {
    return `https://www.google.com/search?q=${encodeURIComponent(query)}`
}

onMounted(() => {
  // Delay fetching to save tokens until needed
  setTimeout(async () => {
    try {
      const res = await fetch(`${API_BASE}/recommendations/daily-insight`)
      if (!res.ok) throw new Error('Failed to fetch')
      const json = await res.json()
      // Fix potential JSON string inside JSON string issue if generic response is stringified
      data.value = typeof json === 'string' ? JSON.parse(json) : json
      
      // Handle empty/error case from Backend
      if (data.value.error) {
          error.value = true;
      }
    } catch (e) {
      console.error(e)
      error.value = true
    } finally {
      loading.value = false
    }
  }, 2000)
})
</script>

<style scoped>
.ai-suggestions-section {
    background: rgba(191, 95, 255, 0.05);
    border: 1px solid rgba(191, 95, 255, 0.2);
    padding: 20px;
    margin-bottom: 30px;
    position: relative;
    overflow: hidden;
}

.ai-suggestions-section::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 2px;
    height: 100%;
    background: #bf5fff;
    box-shadow: 0 0 10px #bf5fff;
}

.section-title {
    color: #bf5fff !important;
}

.section-title::before {
    background: #bf5fff !important;
    box-shadow: 0 0 10px #bf5fff !important;
}

.ai-loading {
    padding: 20px;
    text-align: center;
}

.loading-bar {
    height: 2px;
    background: rgba(191, 95, 255, 0.2);
    position: relative;
    overflow: hidden;
    margin-bottom: 10px;
}

.loading-bar::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 30%;
    height: 100%;
    background: #bf5fff;
    animation: loading 1.5s infinite ease-in-out;
}

@keyframes loading {
    0% { left: -30%; }
    100% { left: 100%; }
}

.loading-text {
    font-size: 12px;
    color: #bf5fff;
    opacity: 0.8;
}

.ai-summary {
    font-style: italic;
    color: #e0e0e0;
    margin-bottom: 20px;
    font-size: 14px;
    line-height: 1.5;
}

.suggestions-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 20px;
}

.suggestion-pill {
    padding: 6px 12px;
    background: rgba(191, 95, 255, 0.1);
    border: 1px solid rgba(191, 95, 255, 0.3);
    color: #bf5fff;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
}

.suggestion-pill:hover {
    background: rgba(191, 95, 255, 0.2);
    box-shadow: 0 0 10px rgba(191, 95, 255, 0.3);
    transform: translateY(-2px);
}

.ai-rec-card {
    background: rgba(0, 0, 0, 0.2);
    padding: 15px;
    border-top: 1px solid rgba(191, 95, 255, 0.2);
}

.rec-label {
    font-size: 10px;
    color: rgba(191, 95, 255, 0.8);
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 5px;
}

.rec-link {
    display: block;
    color: #fff;
    font-weight: 600;
    text-decoration: none;
    margin-bottom: 5px;
    transition: color 0.2s;
}

.rec-link:hover {
    color: #bf5fff;
}

.rec-reason {
    font-size: 12px;
    color: #888;
}
</style>
