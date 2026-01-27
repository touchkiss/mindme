<template>
  <div class="knowledge-gap-card">
    <div class="card-header">
       <span class="icon">🔍</span>
       <span class="title">知识漏洞</span>
    </div>
    <div v-if="loading" class="loading">Scanning...</div>
    <div v-else class="gap-list">
       <div v-for="(gap, i) in gaps" :key="i" class="gap-item" @click="searchGap(gap.topic)">
          <div class="gap-info">
             <span class="gap-topic">{{ gap.topic }}</span>
             <span class="gap-category">{{ gap.category }}</span>
          </div>
          <div class="gap-reason">{{ gap.reason }}</div>
       </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { cognitionApi } from '../api'

const gaps = ref([])
const loading = ref(true)

async function load() {
    try {
        const res = await cognitionApi.gaps()
        gaps.value = res.data
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

function searchGap(topic) {
    window.open(`https://www.google.com/search?q=${encodeURIComponent(topic)}`, '_blank')
}

onMounted(load)
</script>

<style scoped>
.knowledge-gap-card {
    background: rgba(255, 50, 50, 0.05); /* Reddish tint for "Alert/Gap" */
    border: 1px solid rgba(255, 50, 50, 0.2);
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
    color: #ff6b6b;
    font-family: 'Orbitron', monospace;
    flex-shrink: 0;
}

.gap-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex: 1;
    overflow-y: auto;
    padding-right: 5px; /* Space for scrollbar */
}

/* Custom Scrollbar */
.gap-list::-webkit-scrollbar {
    width: 4px;
}
.gap-list::-webkit-scrollbar-thumb {
    background: rgba(255, 107, 107, 0.3);
    border-radius: 2px;
}
.gap-list::-webkit-scrollbar-track {
    background: rgba(0,0,0,0.1);
}

.gap-item {
    background: rgba(0,0,0,0.2);
    padding: 10px;
    border-left: 2px solid #ff6b6b;
    cursor: pointer;
    transition: all 0.2s;
    flex-shrink: 0;
}

.gap-item:hover {
    background: rgba(255, 50, 50, 0.1);
    transform: translateX(5px);
}

.gap-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
}

.gap-topic {
    font-weight: bold;
    color: #ffdce0;
}

.gap-category {
    font-size: 10px;
    opacity: 0.7;
    border: 1px solid #ff6b6b;
    padding: 1px 4px;
    border-radius: 4px;
}

.gap-reason {
    font-size: 11px;
    color: rgba(255,255,255,0.5);
}
</style>
