<template>
  <div class="flowing-menu-wrap" :style="{ backgroundColor: bgColor }">
    <nav class="flowing-menu" aria-label="DeepInsight capabilities menu">
      <FlowingMenuItem
        v-for="(item, index) in items"
        :key="`${item.text}-${index}`"
        :link="item.link"
        :text="item.text"
        :image="item.image"
        :speed="speed"
        :text-color="textColor"
        :marquee-bg-color="marqueeBgColor"
        :marquee-text-color="marqueeTextColor"
        :border-color="borderColor"
      />
    </nav>
  </div>
</template>

<script setup lang="ts">
import FlowingMenuItem from './FlowingMenuItem.vue'

export type FlowingMenuEntry = {
  link: string
  text: string
  image: string
}

withDefaults(defineProps<{
  items: FlowingMenuEntry[]
  speed?: number
  textColor?: string
  bgColor?: string
  marqueeBgColor?: string
  marqueeTextColor?: string
  borderColor?: string
}>(), {
  speed: 15,
  textColor: 'var(--text-primary)',
  bgColor: 'var(--bg-color)',
  marqueeBgColor: 'var(--primary-color)',
  marqueeTextColor: 'var(--text-inverse)',
  borderColor: 'var(--border-color)',
})
</script>

<style>
.flowing-menu-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  isolation: isolate;
}

.flowing-menu {
  display: flex;
  flex-direction: column;
  height: 100%;
  margin: 0;
  padding: 0;
}

.flowing-menu__item {
  flex: 1;
  position: relative;
  overflow: hidden;
  text-align: center;
  border-top: 1px solid;
  min-height: 0;
}

.flowing-menu__item:first-child {
  border-top: none;
}

.flowing-menu__item-link {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  position: relative;
  cursor: pointer;
  text-decoration: none;
  white-space: nowrap;
  text-transform: uppercase;
  font-weight: 700;
  font-size: clamp(46px, 5.4vh, 72px);
  letter-spacing: 0;
}

.flowing-menu__item-link:hover {
  color: inherit;
}

.flowing-menu__item-link:focus:not(:focus-visible) {
  color: inherit;
}

.flowing-menu__marquee {
  position: absolute;
  top: 0;
  left: 0;
  overflow: hidden;
  width: 100%;
  height: 100%;
  pointer-events: none;
  transform: translate3d(0, 101%, 0);
  backdrop-filter: blur(14px) saturate(138%);
}

.flowing-menu__marquee-wrap {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.flowing-menu__marquee-inner {
  display: flex;
  align-items: center;
  position: relative;
  width: fit-content;
  height: 100%;
  will-change: transform;
}

.flowing-menu__marquee-part {
  display: flex;
  flex-shrink: 0;
  align-items: center;
}

.flowing-menu__marquee-part span {
  white-space: nowrap;
  text-transform: uppercase;
  font-weight: 400;
  font-size: clamp(42px, 5.1vh, 66px);
  line-height: 1;
  letter-spacing: 0;
  padding: 0 1.2vw;
}

.flowing-menu__marquee-img {
  width: clamp(220px, 21vw, 340px);
  height: clamp(74px, 8.4vh, 108px);
  margin: 1.15em 2vw;
  border-radius: 999px;
  background-position: center;
  background-size: 170% 170%;
  box-shadow:
    0 14px 26px rgba(16, 18, 24, 0.16),
    inset 0 0 0 1px rgba(255, 255, 255, 0.22);
}

@media (max-width: 760px) {
  .flowing-menu__item-link,
  .flowing-menu__marquee-part span {
    font-size: clamp(22px, 3.4vh, 30px);
  }

  .flowing-menu__marquee-img {
    width: clamp(120px, 28vw, 160px);
    height: clamp(36px, 6vh, 52px);
    margin: 1.2em 4vw;
  }
}
</style>
