import { resolveAssistantNavigation, assistantNavigationTargets } from '../src/utils/assistantNavigation';

type Case = {
  input: string
  path?: string
  label?: string
}

const cases: Case[] = [
  { input: '帮我打开预测推理页面', path: '/prediction', label: '预测推理' },
  { input: '我想去可视化分析看看模型预测结果', path: '/viz', label: '可视化分析' },
  { input: '看看模型总览里的 BSARec', path: '/training', label: '模型总览' },
  { input: '进入云空间找我保存的素材', path: '/cloud', label: '云空间' },
  { input: '跳到数据管理，我要上传数据集', path: '/data', label: '数据管理' },
  { input: '打开 AI 工作室继续长对话', path: '/ai', label: 'AI 工作室' },
  { input: '训练损失为什么震荡' },
]

const failures: string[] = [];

if (assistantNavigationTargets.length < 8) {
  failures.push(`expected at least 8 navigation targets, got ${assistantNavigationTargets.length}`);
}

for (const testCase of cases) {
  const result = resolveAssistantNavigation(testCase.input);
  if (!testCase.path) {
    if (result) failures.push(`expected no route for "${testCase.input}", got ${result.path}`);
    continue;
  }

  if (!result) {
    failures.push(`expected ${testCase.path} for "${testCase.input}", got null`);
    continue;
  }

  if (result.path !== testCase.path) {
    failures.push(`expected ${testCase.path} for "${testCase.input}", got ${result.path}`);
  }

  if (!result.label.includes(testCase.label || '')) {
    failures.push(`expected label containing ${testCase.label} for "${testCase.input}", got ${result.label}`);
  }

  if (!result.reply.includes(result.label) || !result.promptHint) {
    failures.push(`expected reply and prompt hint for "${testCase.input}"`);
  }
}

if (failures.length > 0) {
  throw new Error(`assistant navigation regression failed:\n${failures.join('\n')}`);
}

console.log('assistant navigation regression passed');
