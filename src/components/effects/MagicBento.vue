<template>
  <section
    ref="sectionRef"
    class="magic-bento"
    :class="{ 'magic-bento--disabled': shouldDisableAnimations }"
    @pointermove="handleSectionPointerMove"
    @pointerleave="handleSectionPointerLeave"
  >
    <button
      v-for="(item, index) in items"
      :key="`${item.title}-${index}`"
      :ref="(el) => setCardRef(el, index)"
      type="button"
      class="magic-bento-card"
      :class="[
        `magic-bento-card--${item.layout || 'normal'}`,
        {
          'magic-bento-card--text-autohide': textAutoHide,
          'magic-bento-card--border-glow': enableBorderGlow,
        },
      ]"
      :disabled="item.disabled"
      :style="cardStyle(item)"
      :aria-label="item.title"
      @pointerenter="handleCardEnter(index)"
      @pointermove="handleCardPointerMove($event, index)"
      @pointerleave="handleCardLeave(index)"
      @click="handleCardClick($event, index)"
    >
      <div class="magic-bento-card__header">
        <span class="magic-bento-card__label">{{ item.label }}</span>
        <span class="magic-bento-card__icon" aria-hidden="true">
          <component :is="item.icon" v-if="item.icon" />
          <span v-else>{{ item.title.slice(0, 1) }}</span>
        </span>
      </div>

      <div class="magic-bento-card__content">
        <span v-if="item.meta" class="magic-bento-card__meta">{{ item.meta }}</span>
        <h3 class="magic-bento-card__title">{{ item.title }}</h3>
        <p class="magic-bento-card__description">{{ item.description }}</p>
      </div>
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, type Component } from 'vue'
import { gsap } from 'gsap'

type MagicBentoItem = {
  title: string
  description: string
  label?: string
  meta?: string
  icon?: Component
  color?: string
  accent?: string
  layout?: 'normal' | 'wide' | 'tall' | 'feature'
  disabled?: boolean
}

const DEFAULT_PARTICLE_COUNT = 12
const DEFAULT_SPOTLIGHT_RADIUS = 300
const DEFAULT_GLOW_COLOR = '66, 230, 164'
const MOBILE_BREAKPOINT = 768

const props = withDefaults(defineProps<{
  items: MagicBentoItem[]
  textAutoHide?: boolean
  enableStars?: boolean
  enableSpotlight?: boolean
  enableBorderGlow?: boolean
  disableAnimations?: boolean
  spotlightRadius?: number
  particleCount?: number
  enableTilt?: boolean
  glowColor?: string
  clickEffect?: boolean
  enableMagnetism?: boolean
}>(), {
  textAutoHide: true,
  enableStars: true,
  enableSpotlight: true,
  enableBorderGlow: true,
  disableAnimations: false,
  spotlightRadius: DEFAULT_SPOTLIGHT_RADIUS,
  particleCount: DEFAULT_PARTICLE_COUNT,
  enableTilt: true,
  glowColor: DEFAULT_GLOW_COLOR,
  clickEffect: true,
  enableMagnetism: true,
})

const emit = defineEmits<{
  select: [item: MagicBentoItem, index: number]
}>()

const sectionRef = ref<HTMLElement | null>(null)
const isMobile = ref(false)
const cardRefs: HTMLElement[] = []
const particleStore = new Map<HTMLElement, { particles: HTMLElement[]; timers: number[] }>()

const shouldDisableAnimations = computed(() => props.disableAnimations || isMobile.value)

function setCardRef(el: Element | Component | null, index: number) {
  if (el instanceof HTMLElement) cardRefs[index] = el
}

function cardStyle(item: MagicBentoItem) {
  return {
    '--card-bg': item.color || 'rgba(var(--glass-bg-rgb), 0.34)',
    '--card-accent': item.accent || 'var(--primary-color)',
    '--glow-color': props.glowColor,
  }
}

function updateMobileState() {
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
}

function calculateSpotlightValues(radius: number) {
  return {
    proximity: radius * 0.5,
    fadeDistance: radius * 0.75,
  }
}

function distanceToCardEdge(rect: DOMRect, clientX: number, clientY: number) {
  const insideX = clientX >= rect.left && clientX <= rect.right
  const insideY = clientY >= rect.top && clientY <= rect.bottom
  if (insideX && insideY) {
    return Math.min(clientX - rect.left, rect.right - clientX, clientY - rect.top, rect.bottom - clientY)
  }

  const dx = clientX < rect.left ? rect.left - clientX : clientX > rect.right ? clientX - rect.right : 0
  const dy = clientY < rect.top ? rect.top - clientY : clientY > rect.bottom ? clientY - rect.bottom : 0
  return Math.hypot(dx, dy)
}

function updateCardGlow(card: HTMLElement, clientX: number, clientY: number, glow: number) {
  const rect = card.getBoundingClientRect()
  const relativeX = ((clientX - rect.left) / rect.width) * 100
  const relativeY = ((clientY - rect.top) / rect.height) * 100
  card.style.setProperty('--glow-x', `${relativeX}%`)
  card.style.setProperty('--glow-y', `${relativeY}%`)
  card.style.setProperty('--glow-intensity', glow.toString())
  card.style.setProperty('--glow-radius', `${props.spotlightRadius}px`)
}

function resetCardGlow(card: HTMLElement | null | undefined) {
  if (!card) return
  card.style.setProperty('--glow-x', '50%')
  card.style.setProperty('--glow-y', '50%')
  card.style.setProperty('--glow-intensity', '0')
}

function resetCardPose(card: HTMLElement, animated: boolean) {
  gsap.killTweensOf(card)
  const pose = {
    x: 0,
    y: 0,
    rotateX: 0,
    rotateY: 0,
    overwrite: true,
  }

  if (animated) {
    gsap.to(card, {
      ...pose,
      duration: 0.26,
      ease: 'power2.out',
    })
    return
  }

  gsap.set(card, pose)
}

function resetBentoState(animated = false) {
  cardRefs.forEach((card) => {
    if (!card?.isConnected) return
    resetCardGlow(card)
    clearParticles(card)
    resetCardPose(card, animated)
  })
}

function handleSectionPointerMove(event: PointerEvent) {
  if (shouldDisableAnimations.value || !sectionRef.value) return
  if (!props.enableSpotlight && !props.enableBorderGlow) return

  const { proximity, fadeDistance } = calculateSpotlightValues(props.spotlightRadius)

  cardRefs.forEach((card) => {
    if (!card?.isConnected) return
    const rect = card.getBoundingClientRect()
    const effectiveDistance = distanceToCardEdge(rect, event.clientX, event.clientY)

    const rawGlowIntensity = effectiveDistance <= proximity
      ? 1
      : effectiveDistance <= fadeDistance
        ? (fadeDistance - effectiveDistance) / (fadeDistance - proximity)
        : 0
    const glowIntensity = Math.pow(rawGlowIntensity, 0.9)

    updateCardGlow(card, event.clientX, event.clientY, glowIntensity)
  })
}

function handleSectionPointerLeave() {
  resetBentoState(true)
}

function createParticle(card: HTMLElement) {
  const rect = card.getBoundingClientRect()
  const particle = document.createElement('span')
  particle.className = 'magic-bento-particle'
  particle.style.left = `${Math.random() * rect.width}px`
  particle.style.top = `${Math.random() * rect.height}px`
  particle.style.setProperty('--particle-color', props.glowColor)
  card.appendChild(particle)
  return particle
}

function ensureParticleState(card: HTMLElement) {
  if (!particleStore.has(card)) {
    particleStore.set(card, { particles: [], timers: [] })
  }
  return particleStore.get(card)!
}

function animateParticles(card: HTMLElement) {
  if (shouldDisableAnimations.value || !props.enableStars) return
  const state = ensureParticleState(card)

  for (let index = 0; index < props.particleCount; index += 1) {
    const timer = window.setTimeout(() => {
      if (!card.matches(':hover')) return
      const particle = createParticle(card)
      state.particles.push(particle)

      gsap.fromTo(particle, { scale: 0, opacity: 0 }, { scale: 1, opacity: 1, duration: 0.28, ease: 'back.out(1.7)' })
      gsap.to(particle, {
        x: (Math.random() - 0.5) * 110,
        y: (Math.random() - 0.5) * 110,
        rotation: Math.random() * 360,
        duration: 2 + Math.random() * 1.6,
        ease: 'none',
        repeat: -1,
        yoyo: true,
      })
      gsap.to(particle, {
        opacity: 0.28,
        duration: 1.2 + Math.random() * 0.8,
        ease: 'power2.inOut',
        repeat: -1,
        yoyo: true,
      })
    }, index * 80)
    state.timers.push(timer)
  }
}

function clearParticles(card: HTMLElement) {
  const state = particleStore.get(card)
  if (!state) return
  state.timers.forEach(window.clearTimeout)
  state.timers = []
  state.particles.forEach((particle) => {
    gsap.killTweensOf(particle)
    gsap.to(particle, {
      scale: 0,
      opacity: 0,
      duration: 0.22,
      ease: 'back.in(1.7)',
      onComplete: () => particle.remove(),
    })
  })
  state.particles = []
}

function handleCardEnter(index: number) {
  const card = cardRefs[index]
  if (!card || shouldDisableAnimations.value) return
  animateParticles(card)
}

function handleCardPointerMove(event: PointerEvent, index: number) {
  const card = cardRefs[index]
  if (!card || shouldDisableAnimations.value) return
  const rect = card.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  const centerX = rect.width / 2
  const centerY = rect.height / 2
  const motion: gsap.TweenVars = {
    duration: 0.2,
    ease: 'power3.out',
    overwrite: true,
    force3D: true,
    transformPerspective: 1000,
  }

  if (props.enableTilt) {
    motion.rotateX = ((y - centerY) / centerY) * -7
    motion.rotateY = ((x - centerX) / centerX) * 7
  }

  if (props.enableMagnetism) {
    motion.x = (x - centerX) * 0.03
    motion.y = (y - centerY) * 0.03
  }

  gsap.to(card, motion)
}

function handleCardLeave(index: number) {
  const card = cardRefs[index]
  if (!card) return
  clearParticles(card)
  resetCardPose(card, true)
}

function handleCardClick(event: MouseEvent, index: number) {
  const item = props.items[index]
  const card = cardRefs[index]
  if (!item || item.disabled) return

  if (card && props.clickEffect && !shouldDisableAnimations.value) {
    const rect = card.getBoundingClientRect()
    const x = event.clientX - rect.left
    const y = event.clientY - rect.top
    const maxDistance = Math.max(
      Math.hypot(x, y),
      Math.hypot(x - rect.width, y),
      Math.hypot(x, y - rect.height),
      Math.hypot(x - rect.width, y - rect.height),
    )
    const ripple = document.createElement('span')
    ripple.className = 'magic-bento-ripple'
    ripple.style.left = `${x - maxDistance}px`
    ripple.style.top = `${y - maxDistance}px`
    ripple.style.width = `${maxDistance * 2}px`
    ripple.style.height = `${maxDistance * 2}px`
    ripple.style.setProperty('--ripple-color', props.glowColor)
    card.appendChild(ripple)
    gsap.fromTo(ripple, { scale: 0, opacity: 1 }, {
      scale: 1,
      opacity: 0,
      duration: 0.75,
      ease: 'power2.out',
      onComplete: () => ripple.remove(),
    })
  }

  emit('select', item, index)
}

onMounted(() => {
  updateMobileState()
  window.addEventListener('resize', updateMobileState)
  window.addEventListener('scroll', handleWindowScroll, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateMobileState)
  window.removeEventListener('scroll', handleWindowScroll, true)
  resetBentoState(false)
})

function handleWindowScroll() {
  resetBentoState(false)
}
</script>

<style scoped>
.magic-bento {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  grid-auto-rows: minmax(138px, auto);
  gap: 10px;
  overflow: hidden;
  isolation: isolate;
  perspective: 1200px;
}

.magic-bento-card {
  --glow-x: 50%;
  --glow-y: 50%;
  --glow-intensity: 0;
  --glow-radius: 260px;

  position: relative;
  z-index: 5;
  min-height: 180px;
  grid-column: span 2;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 22px;
  background:
    radial-gradient(circle at 12% 0%, color-mix(in srgb, var(--card-accent) 18%, transparent), transparent 38%),
    linear-gradient(145deg, rgba(255,255,255,0.06), transparent 36%),
    var(--card-bg);
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  transform-style: preserve-3d;
  will-change: transform;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.09),
    0 20px 52px rgba(0, 0, 0, 0.14);
  transition:
    border-color 220ms ease,
    background 260ms ease,
    box-shadow 260ms ease,
    filter 220ms ease;
}

.magic-bento-card--wide {
  grid-column: span 3;
}

.magic-bento-card--feature {
  grid-column: span 2;
  grid-row: span 2;
}

.magic-bento-card--tall {
  grid-row: span 2;
}

.magic-bento-card:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.magic-bento-card::before,
.magic-bento-card::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
}

.magic-bento-card::before {
  z-index: 0;
  background:
    radial-gradient(220px circle at var(--glow-x) var(--glow-y), rgba(var(--glow-color), calc(var(--glow-intensity) * 0.08)), transparent 66%),
    linear-gradient(120deg, rgba(255,255,255,0.08), transparent 32%);
  opacity: calc(0.44 + var(--glow-intensity) * 0.24);
  transition: opacity 240ms ease;
}

.magic-bento-card--border-glow::after {
  z-index: 1;
  padding: 1.4px;
  background: radial-gradient(
    var(--glow-radius) circle at var(--glow-x) var(--glow-y),
    rgba(255, 255, 255, calc(var(--glow-intensity) * 0.88)) 0%,
    color-mix(in srgb, var(--card-accent) calc(var(--glow-intensity) * 100%), transparent) 18%,
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.48)) 34%,
    transparent 64%
  );
  filter:
    drop-shadow(0 0 calc(8px + 14px * var(--glow-intensity)) rgba(var(--glow-color), 0.36))
    saturate(1.42)
    brightness(1.12);
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  mask-composite: exclude;
}

.magic-bento-card:hover {
  border-color: color-mix(in srgb, var(--card-accent) 52%, rgba(255,255,255,0.2));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.15),
    0 18px 42px rgba(0, 0, 0, 0.16);
  filter: saturate(1.04) brightness(1.02);
}

.magic-bento-card__header,
.magic-bento-card__content {
  position: relative;
  z-index: 2;
}

.magic-bento-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.magic-bento-card__label,
.magic-bento-card__meta {
  color: var(--card-accent);
  font-size: 10px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.magic-bento-card__icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--card-accent) 28%, transparent);
  border-radius: 16px;
  background: color-mix(in srgb, var(--card-accent) 12%, transparent);
  color: var(--card-accent);
  font-size: 22px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.12);
}

.magic-bento-card__meta {
  display: block;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 9px;
}

.magic-bento-card__title {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: clamp(17px, 1.35vw, 23px);
  font-weight: var(--font-weight-title);
  letter-spacing: 0;
}

.magic-bento-card__description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.58;
}

.magic-bento-card--text-autohide .magic-bento-card__title,
.magic-bento-card--text-autohide .magic-bento-card__description {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  text-overflow: ellipsis;
}

.magic-bento-card--text-autohide .magic-bento-card__title {
  -webkit-line-clamp: 1;
  line-clamp: 1;
}

.magic-bento-card--text-autohide .magic-bento-card__description {
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

:global(.magic-bento-particle) {
  position: absolute;
  z-index: 8;
  width: 4px;
  height: 4px;
  border-radius: 999px;
  pointer-events: none;
  background: rgba(var(--particle-color), 1);
  box-shadow:
    0 0 6px rgba(var(--particle-color), 0.74),
    0 0 14px rgba(var(--particle-color), 0.28);
}

:global(.magic-bento-particle::before) {
  content: "";
  position: absolute;
  inset: -3px;
  border-radius: inherit;
  background: rgba(var(--particle-color), 0.18);
}

:global(.magic-bento-ripple) {
  position: absolute;
  z-index: 7;
  border-radius: 999px;
  pointer-events: none;
  background: radial-gradient(circle, rgba(var(--ripple-color), 0.34) 0%, rgba(var(--ripple-color), 0.16) 32%, transparent 70%);
}

@media (max-width: 1280px) {
  .magic-bento {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .magic-bento-card,
  .magic-bento-card--feature,
  .magic-bento-card--wide {
    grid-column: span 2;
  }
}

@media (max-width: 760px) {
  .magic-bento {
    grid-template-columns: 1fr;
  }

  .magic-bento-card,
  .magic-bento-card--feature,
  .magic-bento-card--wide,
  .magic-bento-card--tall {
    grid-column: span 1;
    grid-row: span 1;
    min-height: 160px;
  }
}
</style>
