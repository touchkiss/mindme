<template>
  <div>
    <header class="page-header">
      <h1 class="page-title">◉ ACTIVITIES</h1>
      <p class="page-subtitle">BROWSING HISTORY DATA STREAM</p>
    </header>

    <div class="cyber-search">
      <input
        v-model="search"
        type="text"
        class="cyber-input"
        placeholder="[ SEARCH QUERY... ]"
        @keyup.enter="loadActivities"
      />
      <button class="cyber-btn" @click="loadActivities">▶ SEARCH</button>
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
        <el-table :data="activities" style="width: 100%">
          <el-table-column prop="title" label="Title" min-width="250" show-overflow-tooltip />
          <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
          <el-table-column prop="durationSeconds" label="Duration" width="100" />
          <el-table-column label="Status" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.analyzed === 'ANALYZED' ? 'success' : 'info'" size="small">
                {{ scope.row.analyzed }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="Time" width="180">
            <template #default="scope">
              <span class="time-cell">{{ formatTime(scope.row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="100" fixed="right">
            <template #default="scope">
              <div class="actions-col">
                <el-button 
                  type="danger" 
                  size="small" 
                  circle 
                  plain
                  @click="deleteActivity(scope.row.id)"
                >✕</el-button>
              </div>
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
import { ref, onMounted, computed } from 'vue'
import { activitiesApi } from '../api'

const activities = ref([])
const loading = ref(true)
const search = ref('')
const page = ref(0)
const totalPages = ref(0)
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
    const res = await activitiesApi.list({
      query: search.value || undefined,
      page: page.value,
      size: 15
    })
    activities.value = res.data.content.map(a => ({
      ...a,
      analyzed: a.analyzed ? 'ANALYZED' : 'PENDING'
    }))
    totalPages.value = res.data.totalPages
  } catch (err) {
    console.error('Failed to load activities:', err)
  } finally {
    loading.value = false
  }
}

function handlePageChange(val) {
  page.value = val - 1
  loadActivities()
}

async function deleteActivity(id) {
  try {
    await activitiesApi.delete(id)
    await loadActivities()
  } catch (err) {
    console.error('Delete failed:', err)
  }
}

onMounted(loadActivities)
</script>
