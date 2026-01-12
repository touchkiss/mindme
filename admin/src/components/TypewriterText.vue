<script setup>
import { ref, onMounted } from 'vue'
import { Motion } from 'motion-v'

const props = defineProps({
  text: { type: String, required: true },
  speed: { type: Number, default: 50 },
  class: { type: String, default: '' }
})

const displayedText = ref('')
const currentIndex = ref(0)

onMounted(() => {
  const typeText = () => {
    if (currentIndex.value < props.text.length) {
      displayedText.value += props.text[currentIndex.value]
      currentIndex.value++
      setTimeout(typeText, props.speed)
    }
  }
  typeText()
})
</script>

<template>
  <Motion
    :initial="{ opacity: 0 }"
    :animate="{ opacity: 1 }"
    :transition="{ duration: 0.5 }"
  >
    <span :class="props.class" class="cyber-text">
      {{ displayedText }}<span class="cursor">_</span>
    </span>
  </Motion>
</template>

<style scoped>
.cyber-text {
  font-family: 'Orbitron', 'Courier New', monospace;
  letter-spacing: 0.1em;
}

.cursor {
  animation: blink 0.8s infinite;
  color: #00ff88;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
