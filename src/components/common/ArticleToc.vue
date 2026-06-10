<template>
  <div v-if="visibleHeadings.length" ref="tocRootRef" class="article-toc-shell">
    <section class="toc-inline" aria-label="文章目录">
      <div class="toc-inline-head">
        <span>{{ title }}</span>
        <small>{{ visibleHeadings.length }} 个小节</small>
      </div>
      <div class="toc-inline-list">
        <button
          v-for="heading in visibleHeadings"
          :key="heading.id"
          type="button"
          class="toc-link"
          :class="[`level-${heading.level}`, { active: heading.id === activeId }]"
          @click="jumpTo(heading.id)"
        >
          {{ heading.text }}
        </button>
      </div>
    </section>

    <Teleport to="body">
      <Transition name="toc-float">
        <aside v-if="showFloating" class="toc-floating" aria-label="滚动文章目录">
          <div class="toc-floating-title">{{ title }}</div>
          <button
            v-for="heading in visibleHeadings"
            :key="`float-${heading.id}`"
            type="button"
            class="toc-floating-link"
            :class="[`level-${heading.level}`, { active: heading.id === activeId }]"
            @click="jumpTo(heading.id)"
          >
            <span>{{ heading.text }}</span>
          </button>
        </aside>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { Heading } from '@/utils/markdown'

const props = withDefaults(defineProps<{
  headings: Heading[]
  title?: string
  maxLevel?: number
  scrollThreshold?: number
  floating?: boolean
}>(), {
  title: '可跳转目录',
  maxLevel: 3,
  scrollThreshold: 360,
  floating: true,
})

const activeId = ref('')
const showFloating = ref(false)
const tocRootRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const visibleHeadings = computed(() =>
  props.headings
    .filter((heading) => heading.level <= props.maxLevel)
    .filter((heading) => heading.text && heading.id)
)

const findHeadingElement = (id: string) => {
  const scope = tocRootRef.value?.parentElement || document
  const headings = Array.from(scope.querySelectorAll<HTMLElement>('[id]'))
  return headings.find((element) => element.id === id) || null
}

const jumpTo = (id: string) => {
  const target = findHeadingElement(id)
  if (!target) return
  activeId.value = id
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const syncFloatingVisibility = () => {
  showFloating.value = props.floating && window.scrollY > props.scrollThreshold && window.innerWidth >= 1180
}

const scheduleFloatingSync = () => {
  syncFloatingVisibility()
  window.requestAnimationFrame(syncFloatingVisibility)
}

const setupObserver = async () => {
  observer?.disconnect()
  observer = null
  await nextTick()
  const elements = visibleHeadings.value
    .map((heading) => findHeadingElement(heading.id))
    .filter((element): element is HTMLElement => Boolean(element))

  if (!elements.length) {
    activeId.value = visibleHeadings.value[0]?.id || ''
    return
  }

  observer = new IntersectionObserver((entries) => {
    const visible = entries
      .filter((entry) => entry.isIntersecting)
      .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
    if (visible[0]?.target?.id) activeId.value = visible[0].target.id
  }, {
    rootMargin: '-92px 0px -64% 0px',
    threshold: [0, 0.2, 0.6],
  })

  elements.forEach((element) => observer?.observe(element))
  activeId.value = elements[0]?.id || ''
}

watch(visibleHeadings, () => {
  setupObserver()
  scheduleFloatingSync()
}, { immediate: true })

onMounted(() => {
  scheduleFloatingSync()
  window.addEventListener('scroll', syncFloatingVisibility, { passive: true })
  window.addEventListener('resize', syncFloatingVisibility, { passive: true })
})

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('scroll', syncFloatingVisibility)
  window.removeEventListener('resize', syncFloatingVisibility)
})
</script>

<style scoped>
.article-toc-shell {
  margin: 24px 0 30px;
}

.toc-inline {
  position: relative;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--primary-color) 16%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.08), transparent 58%),
    rgba(var(--glass-bg-rgb), 0.5);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(18px) saturate(140%);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
}

.toc-inline::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.toc-inline-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px 10px;
  border-bottom: 1px solid color-mix(in srgb, var(--primary-color) 10%, var(--border-color));
  color: var(--text-primary);
  font-size: 14px;
  font-weight: var(--font-weight-title);
}

.toc-inline-head small {
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-body);
}

.toc-inline-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 12px;
}

.toc-link,
.toc-floating-link {
  min-width: 0;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition:
    transform var(--motion-hover) var(--ease-liquid),
    border-color var(--motion-hover) ease,
    background var(--motion-hover) ease,
    color var(--motion-hover) ease;
}

.toc-link {
  min-height: 38px;
  padding: 9px 11px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  line-height: 1.35;
}

.toc-link.level-3 {
  padding-left: 18px;
  color: var(--text-muted);
}

.toc-link:hover,
.toc-link.active {
  border-color: rgba(var(--primary-rgb), 0.24);
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--primary-color);
  transform: translateY(-1px);
}

.toc-floating {
  position: fixed;
  top: 132px;
  right: max(22px, calc((100vw - 1440px) / 2 + 20px));
  z-index: 30;
  width: 220px;
  max-height: min(62vh, 560px);
  overflow: auto;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 15%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(var(--glass-bg-rgb), 0.78), rgba(var(--glass-bg-rgb), 0.62)),
    color-mix(in srgb, var(--surface-1) 70%, transparent);
  box-shadow: 0 22px 68px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(22px) saturate(150%);
  -webkit-backdrop-filter: blur(22px) saturate(150%);
}

:global(.light) .toc-floating,
:global(.light) .toc-inline {
  box-shadow: 0 18px 50px rgba(30, 41, 59, 0.09);
}

.toc-floating-title {
  margin: 0 0 8px;
  padding: 0 2px 8px;
  border-bottom: 1px solid color-mix(in srgb, var(--primary-color) 12%, var(--border-color));
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.toc-floating-link {
  position: relative;
  display: block;
  width: 100%;
  margin: 2px 0;
  padding: 7px 8px 7px 20px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  line-height: 1.38;
}

.toc-floating-link::before {
  content: "";
  position: absolute;
  left: 8px;
  top: 14px;
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--text-muted) 54%, transparent);
  transition: background var(--motion-hover) ease, box-shadow var(--motion-hover) ease;
}

.toc-floating-link.level-3 {
  padding-left: 28px;
  color: var(--text-muted);
}

.toc-floating-link.level-3::before {
  left: 16px;
  width: 4px;
  height: 4px;
}

.toc-floating-link:hover,
.toc-floating-link.active {
  border-color: rgba(var(--primary-rgb), 0.22);
  background: rgba(var(--primary-rgb), 0.09);
  color: var(--primary-color);
}

.toc-floating-link.active::before {
  background: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(var(--primary-rgb), 0.1);
}

.toc-floating-link span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-float-enter-active,
.toc-float-leave-active {
  transition: opacity 180ms ease, transform 220ms var(--ease-smooth), filter 180ms ease;
}

.toc-float-enter-from,
.toc-float-leave-to {
  opacity: 0;
  filter: blur(6px);
  transform: translate3d(10px, -4px, 0);
}

@media (max-width: 1180px) {
  .toc-floating {
    display: none;
  }
}

@media (max-width: 720px) {
  .toc-inline-list {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .toc-link,
  .toc-floating-link,
  .toc-float-enter-active,
  .toc-float-leave-active {
    transition: none;
  }

  .toc-link:hover {
    transform: none;
  }
}
</style>
