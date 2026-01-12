<template>
  <span class="shiny-text" :class="{ 'animating': !disabled }">
    <slot></slot>
  </span>
</template>

<script setup>
defineProps({
  disabled: Boolean
})
</script>

<style scoped>
.shiny-text {
  position: relative;
  display: inline-block;
  color: #00ffff;
  background: linear-gradient(
    120deg,
    rgba(255, 255, 255, 0) 40%,
    rgba(255, 255, 255, 0.8) 50%,
    rgba(255, 255, 255, 0) 60%
  );
  background-size: 200% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent; /* Override just for the shine */
  color: #fff; /* Fallback */
}

.shiny-text.animating {
  animation: shine 3s infinite linear;
}

@keyframes shine {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
</style>
