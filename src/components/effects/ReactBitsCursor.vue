<template>
  <div class="reactbits-field" :class="{ 'is-dark': dark }" aria-hidden="true">
    <ShaderField
      variant="global"
      :dark="dark"
      :paused="paused"
      :speed="0.82"
      :dot-spacing="dark ? 18 : 23"
      :dot-radius="dark ? 0.9 : 0.72"
      :bulge-strength="dark ? 40 : 28"
      :cursor-radius="410"
      :glow-radius="dark ? 210 : 180"
      :aurora-intensity="dark ? 0.18 : 0.036"
      :dot-opacity="dark ? 0.28 : 0.06"
      :dpr-cap="1.15"
    />
  </div>
</template>

<script setup lang="ts">
import ShaderField from './ShaderField.vue'

defineProps<{ dark?: boolean; paused?: boolean }>()
</script>

<style scoped>
.reactbits-field {
  position: fixed;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
  contain: paint;
  opacity: 0.88;
  mix-blend-mode: screen;
}

:global(html.light .reactbits-field) {
  opacity: 0.12;
  mix-blend-mode: normal;
}

@media (prefers-reduced-motion: reduce) {
  .reactbits-field {
    opacity: 0.42;
  }
}
</style>
