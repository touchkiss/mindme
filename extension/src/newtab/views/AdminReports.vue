<script setup>
import { ref } from 'vue'
import { marked } from 'marked'

const reportType = ref('weekly')
const selectedDate = ref(new Date().toISOString().split('T')[0])
const report = ref(null)
const period = ref('')
const loading = ref(false)

// Use server URL from env or default
const API_BASE = 'http://localhost:8091/api'

async function generateReport() {
    loading.value = true
    report.value = null
    try {
        const res = await fetch(`${API_BASE}/reports/${reportType.value}?date=${selectedDate.value}`)
        if (!res.ok) throw new Error('Failed to generate report')
        const data = await res.json()
        report.value = marked(data.report)
        period.value = data.period
    } catch (e) {
        console.error(e)
        alert('Report generation failed')
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="reports-view">
        <header class="page-header">
            <h1 class="page-title">◎ REPORTS</h1>
            <p class="page-subtitle">AI-POWERED ACTIVITY ANALYSIS</p>
        </header>

        <div class="controls">
            <select v-model="reportType" class="cyber-select">
                <option value="daily">Daily Report</option>
                <option value="weekly">Weekly Report</option>
                <option value="monthly">Monthly Report</option>
            </select>
            <input type="date" v-model="selectedDate" class="cyber-input">
            <button class="cyber-btn primary" @click="generateReport" :disabled="loading">
                {{ loading ? '⟳ GENERATING...' : '▶ GENERATE REPORT' }}
            </button>
        </div>

        <div v-if="report" class="report-content cyber-card">
            <div class="report-header">
                <h2>{{ reportType.toUpperCase() }} REPORT</h2>
                <span class="period">{{ period }}</span>
            </div>
            <div class="markdown-body" v-html="report"></div>
        </div>
        
        <div v-else-if="!loading" class="cyber-empty">
            <div class="empty-icon">◎</div>
            <p>Select type and date to generate a report.</p>
        </div>
    </div>
</template>

<style scoped>
.reports-view {
    max-width: 900px;
}

.page-header {
    margin-bottom: 24px;
}

.controls {
    display: flex;
    gap: 1rem;
    margin-bottom: 2rem;
    flex-wrap: wrap;
}

.cyber-select {
    padding: 12px 16px;
    background: rgba(0, 255, 255, 0.05);
    border: 1px solid var(--cyber-border);
    color: var(--text-primary);
    font-family: 'Rajdhani', sans-serif;
    font-size: 14px;
    cursor: pointer;
}

.cyber-select:focus {
    outline: none;
    border-color: var(--cyber-cyan);
}

.report-content {
    margin-top: 24px;
}

.report-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
    border-bottom: 1px solid var(--cyber-border);
    padding-bottom: 1rem;
}

.report-header h2 {
    font-family: 'Orbitron', monospace;
    font-size: 18px;
    color: var(--cyber-cyan);
}

.period {
    color: var(--text-secondary);
    font-size: 0.9rem;
}

.markdown-body {
    line-height: 1.8;
    color: var(--text-primary);
}

.markdown-body h1, .markdown-body h2, .markdown-body h3 {
    color: var(--cyber-cyan);
    margin: 1.5em 0 0.5em;
}

.markdown-body ul, .markdown-body ol {
    padding-left: 1.5em;
}

.markdown-body li {
    margin: 0.5em 0;
}

.markdown-body code {
    background: rgba(0, 255, 255, 0.1);
    padding: 0.2em 0.4em;
    border-radius: 3px;
    font-family: monospace;
}

.cyber-empty {
    text-align: center;
    padding: 80px 20px;
    color: var(--text-secondary);
}

.empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
}
</style>
