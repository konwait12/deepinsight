<template>
  <main class="login-page">
    <section class="login-card glass-panel-heavy">
      <header class="login-header">
        <DeepLogo :scale="1.08" />
        <p>{{ isRegisterMode ? t('login.registerSubtitle') : t('login.subtitle') }}</p>
      </header>

      <form class="login-form" @submit.prevent="handleSubmit">
        <label>
          <span>{{ t('login.username') }}</span>
          <input v-model="loginForm.username" type="text" placeholder="User ID / Username" autocomplete="username" />
        </label>

        <label>
          <span>{{ t('login.password') }}</span>
          <input v-model="loginForm.password" type="password" placeholder="Password" autocomplete="current-password" />
        </label>

        <div class="verify-track" :class="{ done: isVerified }">
          <span>{{ isVerified ? t('login.sliderSuccess') : t('login.sliderLabel') }}</span>
          <div class="verify-fill" :style="{ width: `${sliderValue}%` }"></div>
          <input
            v-model="sliderValue"
            type="range"
            min="0"
            max="100"
            :disabled="isVerified"
            @input="checkSlider"
            @change="finalizeSlider"
          />
          <div class="verify-handle" :style="{ left: `calc(${sliderValue}% - ${40 * (sliderValue / 100)}px)` }">
            <Check v-if="isVerified" :size="17" stroke-width="2.5" />
            <ArrowRight v-else :size="17" stroke-width="2.5" />
          </div>
        </div>

        <button class="submit-btn" type="submit" :disabled="!isVerified || isLoading">
          <LoaderCircle v-if="isLoading" :size="17" class="spin" />
          <span>{{ isLoading ? t('login.processing') : (isRegisterMode ? t('login.registerSubmit') : t('login.submit')) }}</span>
        </button>
      </form>

      <footer class="login-footer">
        <button type="button" @click="toggleMode">
          {{ isRegisterMode ? t('login.hasAccount') : t('login.noAccount') }}
        </button>
      </footer>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Check, LoaderCircle } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth.store'
import DeepLogo from '@/components/common/DeepLogo.vue'

const { t } = useI18n()
const authStore = useAuthStore()
const router = useRouter()

const isRegisterMode = ref(false)
const loginForm = reactive({
  username: '',
  password: '',
})
const sliderValue = ref(0)
const isVerified = ref(false)
const isLoading = ref(false)

function checkSlider() {
  if (sliderValue.value >= 100) {
    isVerified.value = true
    sliderValue.value = 100
  }
}

function finalizeSlider() {
  if (!isVerified.value) sliderValue.value = 0
}

function toggleMode() {
  isRegisterMode.value = !isRegisterMode.value
  isVerified.value = false
  sliderValue.value = 0
}

async function handleSubmit() {
  if (!isVerified.value) return

  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning(t('login.errorEmpty'))
    return
  }

  isLoading.value = true
  try {
    if (isRegisterMode.value) {
      await authStore.register(loginForm)
      ElMessage.success(t('login.registerSuccess'))
    } else {
      await authStore.login(loginForm)
      ElMessage.success(t('login.success'))
    }
    router.push('/dashboard')
  } catch (error: any) {
    console.error('Auth request failed:', error)
    const msg = error.response?.data?.message || t('login.errorFailed')
    ElMessage.error(msg)
    isVerified.value = false
    sliderValue.value = 0
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 104px 18px 32px;
  background:
    radial-gradient(520px circle at var(--pointer-x, 50vw) var(--pointer-y, 50vh), rgba(var(--primary-rgb), 0.16), transparent 48%),
    var(--bg-color);
}

.login-card {
  width: min(100%, 430px);
  padding: 30px;
  overflow: hidden;
}

.login-card::before {
  content: "";
  position: absolute;
  inset: -1px;
  pointer-events: none;
  background:
    radial-gradient(180px circle at 84% 8%, rgba(var(--primary-rgb), 0.18), transparent 58%),
    radial-gradient(220px circle at 4% 100%, color-mix(in srgb, var(--accent-glow) 24%, transparent), transparent 62%);
}

.login-header,
.login-form,
.login-footer {
  position: relative;
  z-index: 1;
}

.login-header {
  display: grid;
  justify-items: center;
  gap: 18px;
  margin-bottom: 32px;
  text-align: center;
}

.login-header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-label);
  line-height: 1.6;
}

.login-form {
  display: grid;
  gap: 18px;
}

.login-form label {
  display: grid;
  gap: 8px;
}

.login-form label span {
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.login-form input[type="text"],
.login-form input[type="password"] {
  width: 100%;
  height: 50px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 0 14px;
  outline: none;
  background: var(--bg-input);
  color: var(--text-primary);
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.login-form input:focus {
  border-color: rgba(var(--primary-rgb), 0.44);
  box-shadow: 0 0 0 3px rgba(var(--primary-rgb), 0.1);
}

.verify-track {
  position: relative;
  height: 48px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.verify-track > span {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: grid;
  place-items: center;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  pointer-events: none;
}

.verify-track.done > span {
  color: var(--primary-color);
}

.verify-fill {
  position: absolute;
  inset: 0 auto 0 0;
  z-index: 1;
  background: rgba(var(--primary-rgb), 0.16);
}

.verify-track input[type="range"] {
  position: absolute;
  inset: 0;
  z-index: 4;
  width: 100%;
  opacity: 0;
  cursor: pointer;
}

.verify-handle {
  position: absolute;
  top: 4px;
  z-index: 3;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-solid);
  color: var(--text-secondary);
  box-shadow: var(--shadow-soft);
  pointer-events: none;
}

.verify-track.done .verify-handle {
  border-color: var(--primary-color);
  background: var(--primary-color);
  color: #06100c;
}

.submit-btn {
  height: 50px;
  border: 1px solid var(--primary-color);
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  background: var(--primary-color);
  color: #06100c;
  font-size: 13px;
  font-weight: var(--font-weight-title);
  cursor: pointer;
  box-shadow: 0 16px 44px rgba(var(--primary-rgb), 0.28);
  transition: transform 170ms ease, opacity 170ms ease;
}

.submit-btn:not(:disabled):hover {
  transform: translateY(-2px);
}

.submit-btn:disabled {
  opacity: 0.42;
  cursor: not-allowed;
  box-shadow: none;
}

.spin {
  animation: spin 900ms linear infinite;
}

.login-footer {
  margin-top: 24px;
  text-align: center;
}

.login-footer button {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-label);
  cursor: pointer;
  transition: color 150ms ease;
}

.login-footer button:hover {
  color: var(--primary-color);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
