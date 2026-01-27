<template>
  <div class="widget-container" :class="{ 'loading': loading, 'error': error }">
    <div class="widget-header">
      <div class="widget-title">
        <el-icon v-if="icon" class="widget-icon"><component :is="icon" /></el-icon>
        <span class="title-text">{{ title }}</span>
      </div>
      <div class="widget-actions">
        <slot name="actions"></slot>
      </div>
    </div>
    
    <div class="widget-content">
      <div v-if="loading" class="loading-state">
         <span class="loading-scanner"></span>
         <span class="loading-text">SCANNING...</span>
      </div>
      <div v-else-if="error" class="error-state">
         <el-icon class="error-icon"><Warning /></el-icon>
         <span>CONNECTION LOST</span>
      </div>
      <slot v-else></slot>
    </div>
    
    <!-- Decorative Corners -->
    <div class="corner corner-tl"></div>
    <div class="corner corner-tr"></div>
    <div class="corner corner-bl"></div>
    <div class="corner corner-br"></div>
  </div>
</template>

<script setup>
import { Warning } from '@element-plus/icons-vue'

defineProps({
  title: { type: String, required: true },
  icon: { type: Object, default: null }, // Component
  loading: Boolean,
  error: Boolean
})
</script>

<style scoped>
.widget-container {
  background: var(--cyber-panel);
  border: 1px solid var(--cyber-panel-border);
  backdrop-filter: blur(8px);
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
}

.widget-container:hover {
  border-color: rgba(6, 182, 212, 0.4);
  box-shadow: 0 0 15px rgba(6, 182, 212, 0.1);
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: rgba(15, 23, 42, 0.4);
  border-bottom: 1px solid var(--cyber-panel-border);
  flex-shrink: 0;
}

.widget-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--cyber-primary);
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.title-text {
    position: relative;
}

.widget-icon {
    font-size: 16px;
    filter: drop-shadow(0 0 5px var(--cyber-primary));
}

.widget-content {
  flex: 1;
  overflow: auto;
  min-height: 0;
  position: relative;
  padding: 0; /* content should handle its own padding if needed, or use slot wrapper */
}

/* Loading State */
.loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: 10px;
}
.loading-text {
    font-family: var(--font-mono);
    font-size: 10px;
    color: var(--text-secondary);
    animation: blink 1s infinite;
}
.loading-scanner {
    width: 40px;
    height: 2px;
    background: var(--cyber-primary);
    box-shadow: 0 0 10px var(--cyber-primary);
    animation: scan 1s infinite alternate;
}

@keyframes scan {
    from { width: 10px; opacity: 0.5; }
    to { width: 60px; opacity: 1; }
}
@keyframes blink {
    50% { opacity: 0.3; }
}

/* Error State */
.error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--cyber-accent);
    gap: 8px;
    font-family: var(--font-mono);
    font-size: 11px;
}
.error-icon {
    font-size: 24px;
}

/* Corners */
.corner {
    position: absolute;
    width: 6px;
    height: 6px;
    border-color: var(--cyber-primary);
    border-style: solid;
    border-width: 0;
    pointer-events: none;
    opacity: 0.6;
    transition: all 0.3s;
}
.widget-container:hover .corner {
    opacity: 1;
    width: 10px;
    height: 10px;
}
.corner-tl { top: 0; left: 0; border-top-width: 2px; border-left-width: 2px; }
.corner-tr { top: 0; right: 0; border-top-width: 2px; border-right-width: 2px; }
.corner-bl { bottom: 0; left: 0; border-bottom-width: 2px; border-left-width: 2px; }
.corner-br { bottom: 0; right: 0; border-bottom-width: 2px; border-right-width: 2px; }
</style>
