<template>
  <div class="graph-container">
    <div ref="graphEl" class="graph-canvas"></div>
    
    <div class="controls">
      <div class="control-group">
        <button class="btn btn-sm" @click="refreshGraph" :disabled="loading">
          {{ loading ? 'Loading Galaxy...' : '🔄 Refresh Galaxy' }}
        </button>
        <button class="btn btn-sm" @click="rebuildBackend">
          🛠️ Rebuild Index
        </button>
      </div>
      <div class="legend">
        <div class="legend-item"><span class="dot topic"></span> Topic</div>
        <div class="legend-item"><span class="dot page"></span> Page</div>
      </div>
    </div>

    <div v-if="hoveredNode" class="node-tooltip" :style="tooltipStyle">
      <div class="tooltip-title">{{ hoveredNode.name }}</div>
      <div class="tooltip-desc">{{ hoveredNode.val }} interest points</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import ForceGraph3D from '3d-force-graph';

const graphEl = ref(null);
const loading = ref(false);
const hoveredNode = ref(null);
const tooltipStyle = ref({ top: '0px', left: '0px' });

let graphInstance = null;

const SETTINGS = {
  serverUrl: 'http://localhost:8080/api'
};

onMounted(async () => {
  // Load Server URL from storage if possible
  try {
    const stored = await chrome.storage.sync.get('serverUrl');
    if (stored.serverUrl) {
        SETTINGS.serverUrl = stored.serverUrl.includes('/ingest') 
            ? stored.serverUrl.substring(0, stored.serverUrl.indexOf('/ingest')) 
            : "http://localhost:8080/api";
    }
  } catch (e) {}

  initGraph();
  refreshGraph();
});

onUnmounted(() => {
  if (graphInstance) {
    graphInstance._destructor();
  }
});

function initGraph() {
  graphInstance = ForceGraph3D()(graphEl.value)
    .backgroundColor('#0f172a') // Dark slate background matching the UI
    .nodeLabel('name')
    .nodeColor(node => node.type === 'TOPIC' ? '#f472b6' : '#60a5fa') // Pink for Topics, Blue for Pages
    .nodeVal(node => Math.max(node.val / 10, 1)) // Scale node size
    .linkColor(() => 'rgba(255,255,255,0.2)')
    .onNodeClick(node => {
      // Aim at node from outside it
      const distance = 40;
      const distRatio = 1 + distance/Math.hypot(node.x, node.y, node.z);

      graphInstance.cameraPosition(
        { x: node.x * distRatio, y: node.y * distRatio, z: node.z * distRatio }, // new position
        node, // lookAt ({ x, y, z })
        3000  // ms transition duration
      );
    })
    .onNodeHover(node => {
        // 3d-force-graph handles raw canvas tooltip, but we can do custom UI state if needed
        // but for simplicity, we rely on its internal label or use a custom overlay
        // Hover state is tricky in 3D canvas for HTML overlay without projection
    });
}

async function refreshGraph() {
  try {
    loading.value = true;
    const res = await fetch(`${SETTINGS.serverUrl}/graph`);
    const data = await res.json();
    
    // Transform to match 3d-force-graph format { nodes, links }
    // Our backend returns { nodes: [], links: [] } where links have source/target IDs
    // 3d-force-graph needs node objects or IDs matching.
    
    // Backend returns: edgesData with 'source' and 'target' as IDs.
    // 3d-force-graph works with that.

    graphInstance.graphData(data);
  } catch (e) {
    console.error("Failed to load graph", e);
  } finally {
    loading.value = false;
  }
}

async function rebuildBackend() {
  try {
    loading.value = true;
    await fetch(`${SETTINGS.serverUrl}/graph/rebuild`, { method: 'POST' });
    setTimeout(refreshGraph, 1000); // Wait a bit then refresh
  } catch (e) {
    console.error("Failed to rebuild", e);
    loading.value = false;
  }
}
</script>

<style scoped>
.graph-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.graph-canvas {
  width: 100%;
  height: 100%;
}

.controls {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: rgba(15, 23, 42, 0.8);
  padding: 10px;
  border-radius: 8px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.control-group {
    display: flex;
    gap: 10px;
}

.btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
}

.legend {
  display: flex;
  gap: 10px;
  margin-top: 5px;
  font-size: 12px;
  color: #94a3b8;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot.topic { background: #f472b6; }
.dot.page { background: #60a5fa; }
</style>
