<template>
  <div v-if="isLoading" class="glass-overlay">
    <div class="loading-content">
      <va-progress-circle indeterminate color="primary" size="large" />
      <span class="loading-text">{{ loadingText }}</span>
    </div>
  </div>
</template>

<script setup>
const { isLoading, loadingText } = useLoading()
</script>

<style scoped>
.glass-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(8px);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  background: rgba(255, 255, 255, 0.85);
  padding: 3rem 4rem;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.15);
  animation: fadeIn 0.3s ease;
  backdrop-filter: blur(10px);
}
.loading-text {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
}
@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}
</style>
