<template>
  <div class="knowledge-view">
    <header class="page-header">
      <h1 class="page-title">◐ KNOWLEDGE GRAPH</h1>
      <div class="header-actions">
        <button class="cyber-btn" @click="rebuildGraph" :disabled="rebuilding">
          {{ rebuilding ? 'REBUILDING...' : '▶ REBUILD GRAPH' }}
        </button>
      </div>
    </header>

    <div class="view-content">
      <div ref="graphContainer" class="graph-container"></div>
      
      <div v-if="loading" class="cyber-loading">
        <div class="loading-spinner"></div>
        <span>LOADING NEURAL NETWORK...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import ForceGraph3D from '3d-force-graph'

const graphContainer = ref(null)
const loading = ref(true)
const rebuilding = ref(false)
let graph = null

// Server URL
const API_BASE = 'http://localhost:8091/api'

async function loadGraph() {
  loading.value = true
  try {
    const res = await fetch(`${API_BASE}/graph`)
    const data = await res.json()
    
    initGraph(data)
  } catch (e) {
    console.error("Failed to load graph", e)
  } finally {
    loading.value = false
  }
}

async function rebuildGraph() {
  if (!confirm('Rebuild graph from activity logs? This may take a while.')) return
  
  rebuilding.value = true
  try {
    await fetch(`${API_BASE}/graph/rebuild`, { method: 'POST' })
    await loadGraph()
  } catch (e) {
    console.error(e)
    alert('Rebuild failed')
  } finally {
    rebuilding.value = false
  }
}

function initGraph(data) {
  if (graph) {
    graph.graphData(data)
    return
  }

  // Calculate container dimensions
  const width = graphContainer.value.clientWidth
  const height = window.innerHeight - 200 // Approx header height

  graph = ForceGraph3D()(graphContainer.value)
    .width(width)
    .height(height)
    .graphData(data)
    .nodeAutoColorBy('type')
    .nodeLabel('name')
    .linkWidth(link => link.weight || 1)
    .backgroundColor('#000510') // Deep cyber dark
    .onNodeClick(node => {
      // Focus on node
      const distance = 40
      const distRatio = 1 + distance/Math.hypot(node.x, node.y, node.z)
      
      graph.cameraPosition(
        { x: node.x * distRatio, y: node.y * distRatio, z: node.z * distRatio },
        node,
        3000
      )
    })
}

onMounted(() => {
  loadGraph()
  
  window.addEventListener('resize', () => {
    if (graph && graphContainer.value) {
      graph.width(graphContainer.value.clientWidth)
      graph.height(window.innerHeight - 200)
    }
  })
})

onUnmounted(() => {
  // Cleanup if needed
})
</script>

<style scoped>
.knowledge-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid var(--cyber-border);
}

.view-content {
  flex: 1;
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #000510;
  min-height: 500px;
}

.graph-container {
  width: 100%;
  height: 100%;
}

.cyber-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: var(--cyber-cyan);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 255, 255, 0.1);
  border-top-color: var(--cyber-cyan);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
