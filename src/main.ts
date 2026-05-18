import {createApp} from 'vue';
import {createPinia} from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import './index.css';
import './styles/index.scss';
import App from './App.vue';
import router from './router';
import i18n from './i18n';

const app = createApp(App);
const pinia = createPinia();
const ROUTE_RELOAD_PREFIX = 'deepinsight:route-import-reload';

function isRecoverableRouteImportError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error ?? '');
  return /Failed to fetch dynamically imported module|Importing a module script failed|error loading dynamically imported module|Loading chunk [\d]+ failed|ChunkLoadError/i.test(message);
}

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

router.onError((error, to) => {
  if (typeof window !== 'undefined' && isRecoverableRouteImportError(error)) {
    const target = to?.fullPath || `${window.location.pathname}${window.location.search}${window.location.hash}`;
    const retryKey = `${ROUTE_RELOAD_PREFIX}:${target}`;

    // A single hard reload usually picks up the latest Vite chunk manifest after a redeploy.
    if (sessionStorage.getItem(retryKey) !== '1') {
      sessionStorage.setItem(retryKey, '1');
      window.location.reload();
      return;
    }
  }

  console.error('[router] navigation error:', error);
});

router.afterEach((to) => {
  if (typeof window === 'undefined') return;
  sessionStorage.removeItem(`${ROUTE_RELOAD_PREFIX}:${to.fullPath}`);
});

app.use(pinia);
app.use(i18n);
app.use(router);
app.use(ElementPlus);

app.mount('#app');
void router.isReady().catch((error) => {
  console.error('[bootstrap] initial navigation failed:', error);
});
