<template>
  <div
    ref="itemRef"
    class="flowing-menu__item"
    :style="{ borderColor }"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <a
      class="flowing-menu__item-link"
      :href="link"
      :style="{ color: textColor }"
      @focus="handleFocus"
      @blur="handleBlur"
    >
      {{ text }}
    </a>

    <div
      ref="marqueeRef"
      class="flowing-menu__marquee"
      :style="{ background: marqueeBgColor }"
    >
      <div class="flowing-menu__marquee-wrap">
        <div
          ref="marqueeInnerRef"
          class="flowing-menu__marquee-inner"
          aria-hidden="true"
        >
          <div
            v-for="index in repetitions"
            :key="index"
            class="flowing-menu__marquee-part"
            :style="{ color: marqueeTextColor }"
          >
            <span>{{ text }}</span>
            <div class="flowing-menu__marquee-img" :style="imageStyle"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { gsap } from 'gsap'

const props = withDefaults(defineProps<{
  link: string
  text: string
  image?: string
  speed?: number
  textColor?: string
  marqueeBgColor?: string
  marqueeTextColor?: string
  borderColor?: string
}>(), {
  image: '',
  speed: 15,
  textColor: 'var(--text-primary)',
  marqueeBgColor: 'var(--primary-color)',
  marqueeTextColor: 'var(--text-inverse)',
  borderColor: 'var(--border-color)',
})

const itemRef = ref<HTMLElement | null>(null)
const marqueeRef = ref<HTMLElement | null>(null)
const marqueeInnerRef = ref<HTMLElement | null>(null)
const repetitions = ref(4)
let marqueeTween: gsap.core.Tween | null = null
let setupTimer: ReturnType<typeof setTimeout> | null = null
let revealTimeline: gsap.core.Timeline | null = null

const imageStyle = computed(() => {
  const image = props.image.trim()
  if (!image) {
    return {
      backgroundImage: 'linear-gradient(135deg, var(--primary-color), var(--accent-glow))',
    }
  }

  return {
    backgroundImage: image.includes('gradient(') || image.startsWith('var(') ? image : `url("${image}")`,
  }
})

function distMetric(x: number, y: number, x2: number, y2: number) {
  const xDiff = x - x2
  const yDiff = y - y2
  return xDiff * xDiff + yDiff * yDiff
}

function findClosestEdge(mouseX: number, mouseY: number, width: number, height: number) {
  const topEdgeDist = distMetric(mouseX, mouseY, width / 2, 0)
  const bottomEdgeDist = distMetric(mouseX, mouseY, width / 2, height)
  return topEdgeDist < bottomEdgeDist ? 'top' : 'bottom'
}

function calculateRepetitions() {
  const marqueeInner = marqueeInnerRef.value
  if (!marqueeInner) return

  const marqueeContent = marqueeInner.querySelector<HTMLElement>('.flowing-menu__marquee-part')
  if (!marqueeContent) return

  const contentWidth = marqueeContent.offsetWidth
  if (!contentWidth) return

  const needed = Math.ceil(window.innerWidth / contentWidth) + 2
  repetitions.value = Math.max(4, needed)
}

function setupMarquee() {
  const marqueeInner = marqueeInnerRef.value
  if (!marqueeInner) return

  const marqueeContent = marqueeInner.querySelector<HTMLElement>('.flowing-menu__marquee-part')
  if (!marqueeContent) return

  const contentWidth = marqueeContent.offsetWidth
  if (!contentWidth) return

  marqueeTween?.kill()
  gsap.set(marqueeInner, { x: 0 })
  marqueeTween = gsap.to(marqueeInner, {
    x: -contentWidth,
    duration: props.speed,
    ease: 'none',
    repeat: -1,
  })
}

function scheduleSetup() {
  if (setupTimer) clearTimeout(setupTimer)
  setupTimer = setTimeout(() => {
    calculateRepetitions()
    nextTick(setupMarquee)
  }, 50)
}

function showMarquee(edge: 'top' | 'bottom') {
  if (!marqueeRef.value || !marqueeInnerRef.value) return

  revealTimeline?.kill()
  gsap.killTweensOf([marqueeRef.value, marqueeInnerRef.value])
  revealTimeline = gsap.timeline({ defaults: { duration: 0.42, ease: 'expo.out' } })
    .set(marqueeRef.value, { yPercent: edge === 'top' ? -101 : 101 }, 0)
    .set(marqueeInnerRef.value, { yPercent: edge === 'top' ? 101 : -101 }, 0)
    .to([marqueeRef.value, marqueeInnerRef.value], { yPercent: 0 }, 0)
}

function hideMarquee(edge: 'top' | 'bottom') {
  if (!marqueeRef.value || !marqueeInnerRef.value) return

  revealTimeline?.kill()
  gsap.killTweensOf([marqueeRef.value, marqueeInnerRef.value])
  gsap.set(marqueeRef.value, { yPercent: edge === 'top' ? -101 : 101 })
  gsap.set(marqueeInnerRef.value, { yPercent: edge === 'top' ? 101 : -101 })
}

function edgeFromEvent(ev: MouseEvent) {
  if (!itemRef.value) return 'bottom'

  const rect = itemRef.value.getBoundingClientRect()
  const x = ev.clientX - rect.left
  const y = ev.clientY - rect.top
  return findClosestEdge(x, y, rect.width, rect.height)
}

function handleMouseEnter(ev: MouseEvent) {
  showMarquee(edgeFromEvent(ev))
}

function handleMouseLeave(ev: MouseEvent) {
  hideMarquee(edgeFromEvent(ev))
}

function handleFocus() {
  showMarquee('top')
}

function handleBlur() {
  hideMarquee('bottom')
}

watch(() => [props.text, props.image, props.speed], () => {
  scheduleSetup()
})

watch(repetitions, () => {
  nextTick(setupMarquee)
})

onMounted(() => {
  gsap.set(marqueeRef.value, { yPercent: 101 })
  gsap.set(marqueeInnerRef.value, { yPercent: -101 })
  scheduleSetup()
  window.addEventListener('resize', scheduleSetup)
})

onBeforeUnmount(() => {
  if (setupTimer) clearTimeout(setupTimer)
  revealTimeline?.kill()
  marqueeTween?.kill()
  window.removeEventListener('resize', scheduleSetup)
})
</script>
