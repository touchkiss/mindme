<script setup>
import { ref } from 'vue'
import { marked } from 'marked'

const reportType = ref('weekly')
const selectedDate = ref(new Date().toISOString().split('T')[0])
const report = ref(null)
const period = ref('')
const loading = ref(false)

// Use server URL from env or default
const API_BASE = 'http://localhost:8080/api'

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
        <div class="controls">
            <select v-model="reportType">
                <option value="daily">Daily Report</option>
                <option value="weekly">Weekly Report</option>
                <option value="monthly">Monthly Report</option>
            </select>
            <input type="date" v-model="selectedDate">
            <button @click="generateReport" :disabled="loading">
                {{ loading ? 'Generating...' : 'Generate Report' }}
            </button>
        </div>

        <div v-if="report" class="report-content">
            <div class="report-header">
                <h2>{{ reportType.toUpperCase() }} REPORT</h2>
                <span class="period">{{ period }}</span>
            </div>
            <div class="markdown-body" v-html="report"></div>
        </div>
        
        <div v-else-if="!loading" class="empty-state">
            Select type and date to generate a report.
        </div>
    </div>
</template>

<style scoped>
.reports-view {
    padding: 2rem;
    max-width: 900px;
    margin: 0 auto;
}

.controls {
    display: flex;
    gap: 1rem;
    margin-bottom: 2rem;
    background: var(--surface-card);
    padding: 1rem;
    border-radius: 8px;
    border: 1px solid var(--border-color);
}

select, input, button {
    padding: 0.5rem 1rem;
    border-radius: 4px;
    border: 1px solid var(--border-color);
    background: var(--surface-bg);
    color: var(--text-primary);
}

button {
    background: var(--primary-color);
    color: white;
    border: none;
    cursor: pointer;
    margin-left: auto;
}

button:disabled {
    opacity: 0.7;
    cursor: not-allowed;
}

.report-content {
    background: var(--surface-card);
    padding: 2rem;
    border-radius: 8px;
    border: 1px solid var(--border-color);
    box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.report-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
    border-bottom: 1px solid var(--border-color);
    padding-bottom: 1rem;
}

.period {
    color: var(--text-secondary);
    font-size: 0.9rem;
}

.empty-state {
    text-align: center;
    color: var(--text-secondary);
    margin-top: 4rem;
}
</style>
