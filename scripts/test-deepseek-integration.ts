import { readFileSync } from 'node:fs';

const aiConfigSource = readFileSync('backend/src/main/java/com/deepinsight/backend/service/impl/AiConfigServiceImpl.java', 'utf-8');
const redeploySource = readFileSync('scripts/redeploy-local.ps1', 'utf-8');

const failures: string[] = [];

const requiredAiConfigSnippets = [
  'DEEPSEEK_API_KEY',
  'DEEPSEEK_API_URL',
  'DEEPSEEK_MODEL',
  'deepseek-chat',
  'https://api.deepseek.com',
  'findActiveConfig()',
  'envDeepSeekConfig()',
  'body.put("thinking", Map.of("type", "enabled"))',
  'body.put("reasoning_effort", normalizeDeepSeekReasoningEffort(options.reasoningLevel()))',
];

for (const snippet of requiredAiConfigSnippets) {
  if (!aiConfigSource.includes(snippet)) {
    failures.push(`DeepSeek backend integration missing "${snippet}"`);
  }
}

const requiredRedeploySnippets = [
  'Import-EnvFile',
  'Join-Path $repoRoot ".env.local"',
  'Join-Path $deployDir ".env"',
];

for (const snippet of requiredRedeploySnippets) {
  if (!redeploySource.includes(snippet)) {
    failures.push(`redeploy script must load local env files: missing "${snippet}"`);
  }
}

const sourceWithoutTest = aiConfigSource + redeploySource;
if (/sk-[A-Za-z0-9]{12,}/.test(sourceWithoutTest)) {
  failures.push('DeepSeek API key must not be hardcoded in source files');
}

if (failures.length > 0) {
  throw new Error(`DeepSeek integration contract failed:\n${failures.join('\n')}`);
}

console.log('DeepSeek integration contract passed');
