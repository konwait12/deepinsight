<template>
  <div 
    ref="assistantRef"
    class="ai-assistant shadow-2xl shadow-[var(--primary-color)]/20 rounded-2xl overflow-hidden border-2 border-[var(--primary-color)] bg-slate-900 transition-all duration-300"
    :style="style"
    :class="{ 'is-collapsed': isCollapsed }"
  >
    <!-- Header/Handle -->
    <div 
      class="header h-9 flex items-center justify-between bg-white text-slate-900 cursor-move select-none px-3"
    >
      <div class="flex items-center">
        <span class="font-black text-[10px] uppercase tracking-tighter">DeepInsight Assistant</span>
      </div>
      <div class="flex items-center space-x-1">
        <el-button 
          link 
          size="small"
          @click="isCollapsed = !isCollapsed"
        >
          <el-icon class="text-slate-900">
            <component :is="isCollapsed ? 'Expand' : 'SemiSelect'" />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- Content -->
    <div v-show="!isCollapsed" class="content flex flex-col h-[280px] w-[300px]">
      <div class="chat-area flex-1 overflow-auto p-4 space-y-3">
        <div class="message assistant flex">
          <div class="bg-white/5 border border-white/10 p-3 rounded-xl text-[11px] leading-relaxed text-slate-300 max-w-[90%]">
            I noticed your <span class="text-[var(--primary-color)] font-bold">Validation Loss</span> has plateaued. 
            Suggesting a <span class="underline decoration-[var(--primary-color)]">Learning Rate Decay</span> of factor 0.1 at Epoch 15.
          </div>
        </div>
      </div>

      <div class="input-area p-3 border-t border-white/5 bg-slate-800/50">
        <div class="flex items-center gap-2">
          <input 
            v-model="userInput"
            type="text" 
            placeholder="Ask about model params..." 
            class="flex-1 bg-white/5 border border-white/10 rounded-lg py-1.5 px-3 text-xs text-white focus:outline-none focus:border-[var(--primary-color)]/50 transition-colors"
            @keyup.enter="handleSend"
          >
          <button @click="handleSend" class="w-8 h-8 bg-[var(--primary-color)] rounded flex items-center justify-center text-slate-900 hover:scale-105 active:scale-95 transition-all">
            <el-icon size="14"><Promotion /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useDraggable } from '@vueuse/core';
import { Expand, SemiSelect, Promotion } from '@element-plus/icons-vue';

const assistantRef = ref<HTMLElement | null>(null);
const isCollapsed = ref(false);
const userInput = ref('');

// Use Draggable from VueUse
const { x, y, style } = useDraggable(assistantRef, {
  initialValue: { x: window.innerWidth - 320, y: window.innerHeight - 350 },
});

const handleSend = () => {
  if (!userInput.value) return;
  userInput.value = '';
};
</script>

<style scoped lang="scss">
.ai-assistant {
  position: fixed;
  z-index: 2000;
  
  &.is-collapsed {
    height: 36px;
    width: 220px;
  }
}

.chat-area {
  &::-webkit-scrollbar {
    width: 3px;
  }
}
</style>
