<script setup>
import { ref, onMounted, computed } from 'vue'
import { Motion } from 'motion-v'

const props = defineProps({
  value: { type: Number, required: true },
  label: { type: String, default: '' },
  icon: { type: String, default: '' },
  color: { type: String, default: 'cyan' }
})

const displayValue = ref(0)

const colorClass = computed(() => `stat-${props.color}`)

onMounted(() => {
  const animateValue = () => {
    const step = Math.ceil(props.value / 30)
    const interval = setInterval(() => {
      if (displayValue.value < props.value) {
        displayValue.value = Math.min(displayValue.value + step, props.value)
      } else {
        clearInterval(interval)
      }
    }, 30)
  }
  setTimeout(animateValue, 300)
})
</script>

<template>
  <Motion
    :initial="{ scale: 0.8, opacity: 0 }"
    :animate="{ scale: 1, opacity: 1 }"
    :transition="{ type: 'spring', stiffness: 300, damping: 20 }"
  >
    <div class="hud-stat" :class="colorClass">
      <div class="hud-border"></div>
      <div class="hud-content">
        <div class="hud-icon" v-if="icon">{{ icon }}</div>
        <div class="hud-value">{{ displayValue }}</div>
        <div class="hud-label">{{ label }}</div>
      </div>
      <div class="hud-glow"></div>
    </div>
  </Motion>
</template>

<style scoped>
.hud-stat {
  position: relative;
  background: rgba(0, 20, 40, 0.8);
  border: 1px solid rgba(0, 255, 255, 0.3);
  padding: 20px;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
  transition: all 0.3s ease;
}

.hud-stat:hover {
  transform: translateY(-3px);
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.3);
}

.hud-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border: 1px solid rgba(0, 255, 255, 0.5);
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
  animation: border-pulse 2s infinite;
}

@keyframes border-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.hud-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.hud-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.hud-value {
  font-size: 36px;
  font-weight: 700;
  font-family: 'Orbitron', monospace;
  letter-spacing: 0.05em;
}

.hud-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.2em;
  opacity: 0.7;
  margin-top: 8px;
}

.hud-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 80%;
  height: 80%;
  transform: translate(-50%, -50%);
  background: radial-gradient(ellipse, rgba(0, 255, 255, 0.1) 0%, transparent 70%);
  pointer-events: none;
}

/* Color variants */
.stat-cyan .hud-value { color: #00ffff; }
.stat-cyan .hud-border { border-color: rgba(0, 255, 255, 0.5); }

.stat-green .hud-value { color: #00ff88; }
.stat-green .hud-stat { border-color: rgba(0, 255, 136, 0.3); }
.stat-green .hud-glow { background: radial-gradient(ellipse, rgba(0, 255, 136, 0.1) 0%, transparent 70%); }

.stat-yellow .hud-value { color: #ffcc00; }
.stat-yellow .hud-stat { border-color: rgba(255, 204, 0, 0.3); }

.stat-purple .hud-value { color: #bf5fff; }
.stat-purple .hud-stat { border-color: rgba(191, 95, 255, 0.3); }
</style>
