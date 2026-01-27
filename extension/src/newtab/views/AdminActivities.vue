<template>
  <div>
    <header class="page-header">
      <h1 class="page-title">◉ ACTIVITIES</h1>
      <p class="page-subtitle">BROWSING HISTORY DATA STREAM</p>
    </header>

    <!-- Filter Bar -->
    <div class="cyber-filters">
        <el-input 
            v-model="filters.query" 
            placeholder="[ SEARCH QUERY... ]" 
            class="filter-item"
            clearable
            @keyup.enter="applyFilters"
        />
        <el-input 
            v-model="filters.hostname" 
            placeholder="[ DOMAIN ]" 
            class="filter-item" 
            clearable
            @keyup.enter="applyFilters"
        />
        <el-select v-model="filters.analyzed" placeholder="[ STATUS ]" class="filter-item" clearable>
            <el-option label="ANALYZED" :value="true" />
            <el-option label="PENDING" :value="false" />
        </el-select>
        
        <el-input v-model="filters.tag" placeholder="[ TAG ]" class="filter-item" clearable @keyup.enter="applyFilters" style="width:140px"/>
        <el-input v-model="filters.maxScore" placeholder="MAX SCORE" type="number" class="filter-item" clearable @keyup.enter="applyFilters" style="width:120px"/>
        <el-input v-model="filters.maxDuration" placeholder="MAX SEC" type="number" class="filter-item" clearable @keyup.enter="applyFilters" style="width:120px"/>

        <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="TO"
            start-placeholder="START DATE"
            end-placeholder="END DATE"
            class="filter-item date-picker"
        />
        <button class="cyber-btn" @click="applyFilters">▶ FILTER</button>
        <button class="cyber-btn secondary" @click="resetFilters">↺ RESET</button>
    </div>

    <!-- Batch Actions -->
    <div v-if="selectedIds.length > 0" class="batch-actions-bar">
        <span>{{ selectedIds.length }} ITEMS SELECTED</span>
        <div class="actions-group">
            <button class="cyber-btn small info" @click="batchBoost">⚡ BOOST (IMPORTANT)</button>
            <button class="cyber-btn small warning" @click="batchAnalyze">↻ RE-ANALYZE</button>
            <button class="cyber-btn small danger" @click="batchDelete">✕ DELETE</button>
        </div>
    </div>

    <div class="cyber-card">
      <div v-if="loading" class="cyber-loading">
        <div class="loading-spinner"></div>
        <span>LOADING DATA...</span>
      </div>

      <div v-else-if="activities.length === 0" class="cyber-empty">
        <div class="empty-icon">◎</div>
        <p>NO RECORDS FOUND</p>
      </div>

      <div v-else>
        <el-table 
            :data="activities" 
            style="width: 100%" 
            @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="title" label="Title" min-width="200" show-overflow-tooltip />
          <el-table-column label="Tags" width="150" show-overflow-tooltip>
            <template #default="{ row }">
               <div class="tags-cell">
                 <el-tag 
                    v-for="tag in (row.tags ? row.tags.split(',') : [])" 
                    :key="tag" 
                    size="small" 
                    class="cyber-tag clickable"
                    @click.stop="applyTagFilter(tag.trim())"
                 >
                   {{ tag }}
                 </el-tag>
               </div>
            </template>
          </el-table-column>
          <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
          <el-table-column prop="durationSeconds" label="Duration" width="100" />
          <el-table-column label="Score" width="90">
             <template #default="scope">
                <span :class="{ 'high-score': (scope.row.interestScore || 0) > 50 }">
                    {{ scope.row.interestScore || 0 }}
                </span>
             </template>
          </el-table-column>
          <el-table-column label="Status" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.analyzed ? 'success' : 'info'" size="small">
                {{ scope.row.analyzed ? 'ANALYZED' : 'PENDING' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="Time" width="180">
            <template #default="scope">
              <span class="time-cell">{{ formatTime(scope.row.visitTime || scope.row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="80" fixed="right">
            <template #default="scope">
                <el-button 
                  type="danger" 
                  size="small" 
                  circle 
                  plain
                  @click="deleteActivity(scope.row.id)"
                >✕</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="cyber-pagination" v-if="totalPages > 1">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="totalPages * 15"
            :page-size="15"
            v-model:current-page="currentPage"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { activitiesApi } from '../api' // Ensure this imports new batch methods
import { ElMessage, ElMessageBox } from 'element-plus'

const activities = ref([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(0)
const selectedIds = ref([])

const filters = reactive({
    query: '',
    hostname: '',
    analyzed: undefined,
    tag: '',
    maxScore: null,
    maxDuration: null,
    dateRange: null
})

const currentPage = computed({
  get: () => page.value + 1,
  set: (val) => page.value = val - 1
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

async function loadActivities() {
  loading.value = true
  try {
    const params = {
        query: filters.query || undefined,
        hostname: filters.hostname || undefined,
        analyzed: filters.analyzed,
        tag: filters.tag || undefined,
        maxScore: filters.maxScore || undefined,
        maxDuration: filters.maxDuration || undefined,
        page: page.value,
        size: 15
    }

    if (filters.dateRange && filters.dateRange.length === 2) {
        params.startDate = filters.dateRange[0].toISOString()
        params.endDate = filters.dateRange[1].toISOString()
    }

    const res = await activitiesApi.list(params)
    activities.value = res.data.content
    totalPages.value = res.data.totalPages
  } catch (err) {
    console.error('Failed to load activities:', err)
    ElMessage.error('Failed to load activities')
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(val) {
    selectedIds.value = val.map(item => item.id)
}

function applyFilters() {
    page.value = 0
    loadActivities()
}

function applyTagFilter(tag) {
    filters.tag = tag;
    applyFilters();
}

function resetFilters() {
    filters.query = ''
    filters.hostname = ''
    filters.analyzed = undefined
    filters.tag = ''
    filters.maxScore = null
    filters.maxDuration = null
    filters.dateRange = null
    applyFilters()
}

function handlePageChange(val) {
  page.value = val - 1
  loadActivities()
}

async function deleteActivity(id) {
    try {
        await activitiesApi.delete(id)
        ElMessage.success('Deleted')
        await loadActivities()
    } catch (err) {
        ElMessage.error('Delete failed')
    }
}

async function batchDelete() {
    try {
        await ElMessageBox.confirm(`Delete ${selectedIds.value.length} items?`, 'Warning', { type: 'warning' })
        await activitiesApi.batchDelete(selectedIds.value)
        ElMessage.success('Batch deleted')
        selectedIds.value = []
        await loadActivities()
    } catch (e) {
        // cancelled or failed
    }
}

async function batchAnalyze() {
    try {
        await activitiesApi.batchAnalyze(selectedIds.value)
        ElMessage.success('Marked for re-analysis')
        selectedIds.value = []
        await loadActivities()
    } catch (e) {
         ElMessage.error('Operation failed')
    }
}

async function batchBoost() {
     try {
        await activitiesApi.batchBoost(selectedIds.value)
        ElMessage.success('Interest boosted')
        selectedIds.value = []
        await loadActivities()
    } catch (e) {
         ElMessage.error('Operation failed')
    }
}

onMounted(loadActivities)
</script>

<style scoped>
.cyber-filters {
    display: flex;
    gap: 15px;
    margin-bottom: 20px;
    flex-wrap: wrap;
    background: rgba(0,255,255,0.02);
    padding: 15px;
    border: 1px solid rgba(0,255,255,0.1);
}

.tags-cell {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
}

.clickable {
    cursor: pointer;
}
.clickable:hover {
    text-decoration: underline;
}

.filter-item {
    width: 200px;
}

.date-picker {
    width: 320px;
}

.batch-actions-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: rgba(255, 255, 255, 0.05); /* Dark theme friendly */
    border: 1px solid var(--cyber-cyan);
    padding: 10px 20px;
    margin-bottom: 20px;
    animation: slideDown 0.3s ease-out;
}

.actions-group {
    display: flex;
    gap: 10px;
}

.cyber-btn.small {
    padding: 5px 15px;
    font-size: 12px;
}

.cyber-btn.info {
    border-color: var(--cyber-yellow);
    color: var(--cyber-yellow);
}
.cyber-btn.info:hover {
    background: rgba(255, 204, 0, 0.1);
}

.cyber-btn.warning {
    border-color: var(--cyber-cyan);
    color: var(--cyber-cyan);
}

.cyber-btn.danger {
    border-color: var(--cyber-pink);
    color: var(--cyber-pink);
}

.high-score {
    color: var(--cyber-yellow);
    font-weight: bold;
}

@keyframes slideDown {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
}

.secondary {
    opacity: 0.7;
}
.secondary:hover {
    opacity: 1;
}
</style>

