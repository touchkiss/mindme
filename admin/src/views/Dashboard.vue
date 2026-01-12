<template>
  <div>
    <header class="page-header">
      <h1 class="page-title">◈ DASHBOARD</h1>
      <p class="page-subtitle">NEURAL NETWORK STATUS OVERVIEW</p>
    </header>

    <div class="stats-grid">
      <HudStat
        :value="activityStats.total"
        label="Total Activities"
        icon="◉"
        color="cyan"
      />
      <HudStat
        :value="activityStats.analyzed"
        label="Analyzed"
        icon="✓"
        color="green"
      />
      <HudStat
        :value="activityStats.pending"
        label="Pending"
        icon="◐"
        color="yellow"
      />
      <HudStat
        :value="knowledgeStats.total"
        label="Knowledge Entries"
        icon="◈"
        color="purple"
      />
    </div>

    <div class="cyber-card">
      <div class="card-title">
        <span>⚡</span> QUICK ACTIONS
      </div>
      <div style="display: flex; gap: 16px; flex-wrap: wrap;">
        <button class="cyber-btn primary" @click="triggerAnalysis" :disabled="analyzing">
          {{ analyzing ? '⟳ PROCESSING...' : '▶ TRIGGER ANALYSIS' }}
        </button>
        <router-link to="/reports" class="cyber-btn">
          ◎ GENERATE REPORT
        </router-link>
      </div>
      <p v-if="analysisResult" style="margin-top: 16px; color: var(--cyber-green);">
        {{ analysisResult }}
      </p>
    </div>

    <div class="cyber-card" v-if="knowledgeStats.categories.length">
      <div class="card-title">
        <span>◐</span> KNOWLEDGE CATEGORIES
      </div>
      <div style="display: flex; flex-wrap: wrap; gap: 8px;">
        <span v-for="cat in knowledgeStats.categories" :key="cat" class="cyber-tag">
          {{ cat }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import HudStat from '../components/HudStat.vue'
import { activitiesApi, knowledgeApi, analysisApi } from '../api'

const activityStats = ref({ total: 0, analyzed: 0, pending: 0 })
const knowledgeStats = ref({ total: 0, categoryCount: 0, categories: [] })
const analyzing = ref(false)
const analysisResult = ref('')

async function loadStats() {
  try {
    const [actRes, knowRes] = await Promise.all([
      activitiesApi.stats(),
      knowledgeApi.stats()
    ])
    activityStats.value = actRes.data
    knowledgeStats.value = knowRes.data
  } catch (err) {
    console.error('Failed to load stats:', err)
  }
}

async function triggerAnalysis() {
  analyzing.value = true
  analysisResult.value = ''
  try {
    const res = await analysisApi.trigger()
    analysisResult.value = `[SUCCESS] ${res.data.message}`
    await loadStats()
  } catch (err) {
    console.error('Analysis failed:', err)
    analysisResult.value = '[ERROR] Analysis failed'
  } finally {
    analyzing.value = false
  }
}

onMounted(loadStats)
</script>
