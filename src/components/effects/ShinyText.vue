<template>
  <span
    class="shiny-text"
    :class="[
      className,
      {
        'is-disabled': disabled,
        'is-paused-on-hover': pauseOnHover,
        'is-yoyo': yoyo,
      },
    ]"
    :style="shinyStyle"
  >
    {{ text }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    text: string
    color?: string
    shineColor?: string
    speed?: number
    delay?: number
    spread?: number
    yoyo?: boolean
    pauseOnHover?: boolean
    direction?: 'left' | 'right'
    disabled?: boolean
    className?: string
  }>(),
  {
    color: '#b5b5b5',
    shineColor: '#ffffff',
    speed: 2,
    delay: 0,
    spread: 120,
    yoyo: false,
    pauseOnHover: false,
    direction: 'left',
    disabled: false,
    className: '',
  },
)

const shinyStyle = computed(() => ({
  '--shiny-color': props.color,
  '--shiny-shine-color': props.shineColor,
  '--shiny-speed': `${Math.max(0.2, props.speed)}s`,
  '--shiny-delay': `${Math.max(0, props.delay)}s`,
  '--shiny-spread': `${props.spread}deg`,
  '--shiny-direction': props.direction === 'left' ? 'normal' : 'reverse',
}))
</script>

<style scoped>
.shiny-text {
  display: inline-block;
  color: var(--shiny-color);
  background-image: linear-gradient(
    var(--shiny-spread),
    var(--shiny-color) 0%,
    var(--shiny-color) 28%,
    color-mix(in srgb, var(--shiny-shine-color) 78%, var(--shiny-color)) 43%,
    var(--shiny-shine-color) 50%,
    color-mix(in srgb, var(--shiny-shine-color) 78%, var(--shiny-color)) 57%,
    var(--shiny-color) 72%,
    var(--shiny-color) 100%
  );
  background-size: 220% auto;
  background-position: 150% center;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: shiny-text-sweep calc(var(--shiny-speed) + var(--shiny-delay)) linear infinite;
  animation-direction: var(--shiny-direction);
}

.shiny-text.is-yoyo {
  animation-direction: alternate;
  animation-timing-function: ease-in-out;
}

.shiny-text.is-paused-on-hover:hover {
  animation-play-state: paused;
}

.shiny-text.is-disabled {
  animation: none;
  background: none;
  color: var(--shiny-color);
  -webkit-text-fill-color: currentColor;
}

@keyframes shiny-text-sweep {
  0% {
    background-position: 150% center;
  }

  72% {
    background-position: -50% center;
  }

  100% {
    background-position: -50% center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shiny-text {
    animation: none;
    background-position: 50% center;
  }
}
</style>
