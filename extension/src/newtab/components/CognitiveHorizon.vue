<template>
  <div class="horizon-card">
    <div class="card-header">
       <span class="icon">🪐</span>
       <span class="title">认知视野</span>
    </div>
    <div v-if="loading" class="loading">
        <span class="scanning-text">Expanding Horizons...</span>
    </div>
    <div v-else class="horizon-list">
       <div 
         v-for="(card, i) in cards" 
         :key="i" 
         class="horizon-item"
         :class="{ active: activeIndex === i }"
         @click="toggleCard(i)"
       >
          <div class="horizon-main">
              <div class="horizon-title">{{ card.title }}</div>
              <div class="horizon-type">{{ card.type }}</div>
          </div>
          <div class="horizon-desc">{{ card.description }}</div>
          
          <!-- Expanded Content -->
          <div v-if="activeIndex === i" class="horizon-details">
              <div class="detail-label">Thinking Bridge:</div>
              <div class="detail-text">{{ card.relevance }}</div>
          </div>
          
          <div class="expand-hint" v-else>
              <span>▼</span>
          </div>
       </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { cognitionApi } from '../api'

const cards = ref([])
const loading = ref(true)
const activeIndex = ref(null)

async function load() {
    try {
        const res = await cognitionApi.horizon()
        cards.value = res.data
    } catch (e) {
        console.error('Cognitive Horizon load error:', e)
        // Show fallback cards on error
        cards.value = [
            { title: '第一性原理', description: '将复杂问题分解为最基本的元素，然后从头开始重建。', type: '思维模型', relevance: '帮助你跳出现有框架，找到问题的本质解决方案。' },
            { title: '帕累托法则', description: '80%的结果来自20%的原因。专注于真正重要的少数事物。', type: '经济学', relevance: '优化你的时间和精力分配，事半功倍。' },
            { title: '逆向思维', description: '与其追求成功，不如先避免愚蠢。将问题倒过来思考。', type: '心理学', relevance: '通过排除法找到更可靠的决策路径。' }
        ]
    } finally {
        loading.value = false
    }
}

function toggleCard(index) {
    activeIndex.value = activeIndex.value === index ? null : index
}

onMounted(load)
</script>

<style scoped>
.horizon-card {
    background: rgba(50, 100, 255, 0.05); /* Blueish tint */
    border: 1px solid rgba(50, 100, 255, 0.2);
    padding: 20px;
    height: 100%;
    display: flex;
    flex-direction: column;
}

.card-header {
    display: flex;
    gap: 10px;
    align-items: center;
    margin-bottom: 15px;
    color: #4facfe;
    font-family: 'Orbitron', monospace;
    flex-shrink: 0;
}

.horizon-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex: 1;
    overflow-y: auto;
    padding-right: 5px;
}

/* Custom Scrollbar */
.horizon-list::-webkit-scrollbar {
    width: 4px;
}
.horizon-list::-webkit-scrollbar-thumb {
    background: rgba(79, 172, 254, 0.3);
    border-radius: 2px;
}
.horizon-list::-webkit-scrollbar-track {
    background: rgba(0,0,0,0.1);
}

.horizon-item {
    background: rgba(0,0,0,0.2);
    padding: 12px;
    border: 1px solid rgba(79, 172, 254, 0.3);
    position: relative;
    overflow: hidden;
    flex-shrink: 0;
    cursor: pointer;
    transition: all 0.3s ease;
}

.horizon-item:hover {
    background: rgba(79, 172, 254, 0.1);
    transform: translateX(2px);
}

.horizon-item.active {
    background: rgba(79, 172, 254, 0.15);
    border-color: #4facfe;
    box-shadow: 0 0 15px rgba(79, 172, 254, 0.1);
}

.horizon-item::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 3px;
    height: 100%;
    background: linear-gradient(to bottom, #4facfe 0%, #00f2fe 100%);
}

.horizon-main {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 5px;
}

.horizon-title {
    font-weight: bold;
    color: #e0f7fa;
    font-size: 14px;
}

.horizon-type {
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 1px;
    color: #4facfe;
    background: rgba(79, 172, 254, 0.1);
    padding: 2px 6px;
    border-radius: 4px;
}

.horizon-desc {
    font-size: 12px;
    line-height: 1.4;
    color: rgba(255,255,255,0.7);
    margin-bottom: 5px;
}

.horizon-details {
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px dashed rgba(79, 172, 254, 0.3);
    animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
    from { opacity: 0; transform: translateY(-5px); }
    to { opacity: 1; transform: translateY(0); }
}

.detail-label {
    font-size: 10px;
    color: #4facfe;
    margin-bottom: 4px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.detail-text {
    font-size: 12px;
    color: #e0f7fa;
    line-height: 1.5;
    font-style: italic;
}

.expand-hint {
    text-align: center;
    color: rgba(79, 172, 254, 0.5);
    font-size: 8px;
    margin-top: 2px;
    opacity: 0;
    transition: opacity 0.2s;
}

.horizon-item:hover .expand-hint {
    opacity: 1;
}

.loading {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #4facfe;
    font-family: 'Orbitron', monospace;
    font-size: 12px;
    animation: pulse 1.5s infinite;
}
</style>
