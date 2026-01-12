<template>
  <div>
    <header class="page-header">
      <h1 class="page-title">◑ SITE ANALYTICS</h1>
      <p class="page-subtitle">网站访问分析与知识汇总</p>
    </header>

    <div class="cyber-card">
      <div class="card-title">
        <span>◉</span> 网站访问统计
      </div>

      <div v-if="loading" class="cyber-loading">
        <div class="loading-spinner"></div>
        <span>LOADING...</span>
      </div>

      <el-table 
        v-else 
        :data="sites" 
        style="width: 100%"
        :default-sort="{ prop: 'visits', order: 'descending' }"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="domain" label="网站域名" min-width="200">
          <template #default="{ row }">
            <span class="domain-cell">{{ row.domain }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="visits" label="访问次数" sortable="custom" width="120">
          <template #default="{ row }">
            <span class="number-cell">{{ row.visits }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalDuration" label="总时长" sortable="custom" width="120">
          <template #default="{ row }">
            <span class="number-cell">{{ formatDuration(row.totalDuration) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="averageDuration" label="平均时长" width="120">
          <template #default="{ row }">
            <span class="number-cell">{{ formatDuration(row.averageDuration) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="analyzedCount" label="已分析" sortable="custom" width="100">
          <template #default="{ row }">
            <span class="number-cell">{{ row.analyzedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastVisit" label="最近访问" width="180">
          <template #default="{ row }">
            <span class="time-cell">{{ formatTime(row.lastVisit) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="cyber-card">
      <div class="card-title">
        <span>◈</span> 知识来源汇总
      </div>

      <div v-if="knowledgeLoading" class="cyber-loading">
        <div class="loading-spinner"></div>
      </div>

      <div v-else class="knowledge-sites">
        <Motion
          v-for="(site, index) in knowledgeSites"
          :key="site.domain"
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ delay: index * 0.1 }"
        >
          <div class="knowledge-site-card">
            <div class="site-header">
              <span class="site-domain">{{ site.domain }}</span>
              <span class="site-count">{{ site.knowledgeCount }} 条知识</span>
            </div>
            <div class="site-categories">
              <span 
                v-for="(count, cat) in site.categories" 
                :key="cat" 
                class="cyber-tag"
              >
                {{ cat }}: {{ count }}
              </span>
            </div>
            <div class="site-tags">
              <span v-for="tag in site.tags" :key="tag" class="mini-tag">{{ tag }}</span>
            </div>
            <div class="site-titles">
              <div v-for="title in site.titles" :key="title" class="title-item">
                {{ title }}
              </div>
            </div>
          </div>
        </Motion>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Motion } from 'motion-v'
import { sitesApi } from '../api'

const sites = ref([])
const knowledgeSites = ref([])
const loading = ref(true)
const knowledgeLoading = ref(true)
const sortBy = ref('visits')

const sortOptions = [
  { value: 'visits', label: '▶ 访问次数' },
  { value: 'duration', label: '▶ 总时长' },
  { value: 'analyzed', label: '▶ 分析数' }
]

async function loadData() {
  loading.value = true
  knowledgeLoading.value = true

  try {
    const [sitesRes, knowledgeRes] = await Promise.all([
      sitesApi.list(sortBy.value),
      sitesApi.knowledge()
    ])
    sites.value = sitesRes.data
    knowledgeSites.value = knowledgeRes.data
  } catch (err) {
    console.error('Failed to load site analytics:', err)
  } finally {
    loading.value = false
    knowledgeLoading.value = false
  }
}

function handleSortChange({ prop, order }) {
  if (!order) return
  if (prop === 'visits') sortBy.value = 'visits'
  else if (prop === 'totalDuration') sortBy.value = 'duration'
  else if (prop === 'analyzedCount') sortBy.value = 'analyzed'
  
  loadData()
}

function formatDuration(seconds) {
  if (!seconds) return '0s'
  if (seconds < 60) return `${seconds}s`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${seconds % 60}s`
  return `${Math.floor(seconds / 3600)}h ${Math.floor((seconds % 3600) / 60)}m`
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadData)
</script>

<style scoped>
.cyber-table {
  width: 100%;
  border-collapse: collapse;
}

.cyber-table th,
.cyber-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid var(--cyber-border);
}

.cyber-table th {
  background: rgba(0, 255, 255, 0.1);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--cyber-cyan);
}

.cyber-table tr:hover {
  background: rgba(0, 255, 255, 0.05);
}

.domain-cell {
  color: var(--cyber-cyan);
  font-weight: 600;
}

.number-cell {
  font-family: 'Orbitron', monospace;
  color: var(--cyber-green);
}

.time-cell {
  font-size: 12px;
  color: var(--text-secondary);
}

.knowledge-sites {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.knowledge-site-card {
  background: rgba(0, 20, 40, 0.8);
  border: 1px solid var(--cyber-border);
  padding: 20px;
  transition: all 0.3s;
}

.knowledge-site-card:hover {
  border-color: var(--cyber-cyan);
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.15);
}

.site-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.site-domain {
  font-family: 'Orbitron', monospace;
  font-size: 14px;
  color: var(--cyber-cyan);
}

.site-count {
  font-size: 12px;
  color: var(--cyber-green);
  padding: 4px 10px;
  background: rgba(0, 255, 136, 0.15);
  border-radius: 3px;
}

.site-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.site-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.mini-tag {
  font-size: 10px;
  padding: 2px 8px;
  background: rgba(0, 255, 255, 0.1);
  border: 1px solid rgba(0, 255, 255, 0.2);
  color: var(--cyber-cyan);
}

.site-titles {
  border-top: 1px solid var(--cyber-border);
  padding-top: 12px;
}

.title-item {
  font-size: 12px;
  color: var(--text-secondary);
  padding: 4px 0;
  border-bottom: 1px dotted rgba(0, 255, 255, 0.1);
}

.title-item:last-child {
  border-bottom: none;
}
</style>
