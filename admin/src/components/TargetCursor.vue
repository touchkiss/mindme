<template>
  <div class="target-cursor" ref="cursor">
    <div class="cursor-center"></div>
    <div class="cursor-ring"></div>
    <div class="cursor-lines">
      <div class="line horizontal"></div>
      <div class="line vertical"></div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const cursor = ref(null)

function updateCursor(e) {
  if (!cursor.value) return
  const x = e.clientX
  const y = e.clientY
  
  // Use CSS transform for performance
  cursor.value.style.transform = `translate(${x}px, ${y}px)`
}

function handleMouseDown() {
  if (cursor.value) cursor.value.classList.add('active')
}

function handleMouseUp() {
  if (cursor.value) cursor.value.classList.remove('active')
}

onMounted(() => {
  window.addEventListener('mousemove', updateCursor, { passive: true })
  window.addEventListener('mousedown', handleMouseDown)
  window.addEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = 'none' // Hide default cursor
})

onUnmounted(() => {
  window.removeEventListener('mousemove', updateCursor)
  window.removeEventListener('mousedown', handleMouseDown)
  window.removeEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = 'auto'
})
</script>

<style scoped>
.target-cursor {
  position: fixed;
  top: 0;
  left: 0;
  width: 40px;
  height: 40px;
  pointer-events: none;
  z-index: 9999;
  transform: translate(-100px, -100px); /* Hide until moved */
  margin-left: -20px; /* Center alignment */
  margin-top: -20px;
  mix-blend-mode: difference;
  transition: transform 0.05s ease-out; /* Slight lag for smooth feel */
}

.cursor-center {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 4px;
  height: 4px;
  background: #00ffff;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 0 10px #00ffff;
}

.cursor-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 1px solid rgba(0, 255, 255, 0.5);
  border-radius: 50%;
  box-sizing: border-box;
  animation: spin 4s linear infinite;
}

.cursor-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.line {
  position: absolute;
  background: rgba(0, 255, 255, 0.3);
}

.line.horizontal {
  top: 50%;
  left: -10px;
  right: -10px;
  height: 1px;
}

.line.vertical {
  left: 50%;
  top: -10px;
  bottom: -10px;
  width: 1px;
}

/* Click Animation */
.target-cursor.active .cursor-ring {
  border-color: #ff0055;
  transform: scale(0.8);
  transition: all 0.1s;
}

.target-cursor.active .cursor-center {
  background: #ff0055;
  box-shadow: 0 0 15px #ff0055;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
