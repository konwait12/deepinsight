import { readFileSync } from 'node:fs';

const source = readFileSync('src/components/ai/FloatingAssistant.vue', 'utf-8');
const appSource = readFileSync('src/App.vue', 'utf-8');

const failures: string[] = [];

const forbiddenSnippets = [
  'assistant-route-strip',
  'routeShortcuts',
  'assistantNavigationTargets',
  'v-for="target in',
  'assistant-orb glass-panel',
  'assistant-panel glass-panel-heavy',
  'backdrop-filter: blur(18px)',
  '-webkit-backdrop-filter: blur(18px)',
  'background-color: #101827 !important',
  'background-color: #f4f7fb !important',
  ':global(html.light) .assistant-opaque-surface',
  'color: var(--text-primary);',
  'color: var(--text-secondary);',
  'color: var(--text-muted);',
];

for (const snippet of forbiddenSnippets) {
  if (source.includes(snippet)) {
    failures.push(`floating assistant should not expose direct route shortcuts: found "${snippet}"`);
  }
}

const requiredSnippets = [
  'resolveAssistantNavigation(message)',
  'msg.navigation',
  'message-action-card',
  'goToNavigation',
  'AI助手',
  'assistant-orb-brand',
  'assistant-opaque-surface',
  '--assistant-surface: #fbfdff;',
  '--assistant-text-primary: #1d1d1f;',
  "import { useThemeStore } from '@/stores/theme.store'",
  "'theme-dark': themeStore.isDarkMode",
  "'theme-light': !themeStore.isDarkMode",
  '.assistant-container.theme-dark',
  '--assistant-surface: #101827;',
  '--assistant-text-primary: #f8fafc;',
  'background-color: var(--assistant-surface) !important',
  'color: var(--assistant-text-primary);',
  '--assistant-messages-bg: #f7faff;',
  '--assistant-panel-outline: rgba(255, 255, 255, 0.92);',
  'outline: 1px solid var(--assistant-panel-outline);',
  'background-image: linear-gradient(180deg, #ffffff 0%, var(--assistant-surface) 100%) !important;',
];

for (const snippet of requiredSnippets) {
  if (!source.includes(snippet)) {
    failures.push(`floating assistant should keep intent-based navigation action: missing "${snippet}"`);
  }
}

const requiredAppSnippets = [
  "const isAiWorkspaceRoute = computed(() => route.path === '/ai')",
  'const showFloatingAssistant = computed(() => !isPublicPreview.value && !isAiWorkspaceRoute.value)',
];

for (const snippet of requiredAppSnippets) {
  if (!appSource.includes(snippet)) {
    failures.push(`floating assistant should hide on the full AI workspace route: missing "${snippet}"`);
  }
}

if (failures.length > 0) {
  throw new Error(`floating assistant ui contract failed:\n${failures.join('\n')}`);
}

console.log('floating assistant ui contract passed');
