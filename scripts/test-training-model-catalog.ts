import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const trainingView = readFileSync(resolve('src/views/training/index.vue'), 'utf8');

const requiredModels = [
  { id: 'bsarec-job', label: 'BSARec Job', tag: '在线 / 推荐模型' },
  { id: 'sasrec-base', label: 'SASRec Base', tag: '序列推荐 / 基线模型' },
  { id: 'bert4rec', label: 'BERT4Rec', tag: 'Transformer / 推荐模型' },
  { id: 'tisasrec', label: 'TiSASRec', tag: '时间感知 / 序列推荐' },
  { id: 'fmlp-rec', label: 'FMLP-Rec', tag: '轻量模型 / 高效推荐' },
];

const excludedModels = ['RecBole', 'DuoRec', 'FEARec'];

const missing = requiredModels.flatMap((model) => {
  const checks = [
    trainingView.includes(model.id) ? '' : `missing id ${model.id}`,
    trainingView.includes(model.label) ? '' : `missing label ${model.label}`,
    trainingView.includes(model.tag) ? '' : `missing tag ${model.tag}`,
  ];

  return checks.filter(Boolean);
});

const modelCatalogMatch = trainingView.match(/const modelCatalog = computed\(\(\) => \[([\s\S]*?)\]\);/);
const catalogBlock = modelCatalogMatch?.[1] ?? '';
const catalogErrors = requiredModels
  .map((model) => `${model.id.replace(/-([a-z])/g, (_, char) => char.toUpperCase()).replace('bsarecJob', 'bsarecModel').replace('sasrecBase', 'sasrecModel').replace('bert4rec', 'bert4RecModel').replace('tisasrec', 'tisasRecModel').replace('fmlpRec', 'fmlpRecModel')}.value`)
  .filter((entry) => !catalogBlock.includes(entry))
  .map((entry) => `modelCatalog does not include ${entry}`);

const excludedErrors = excludedModels
  .filter((modelName) => catalogBlock.includes(modelName))
  .map((modelName) => `modelCatalog should not include ${modelName}`);

const failures = [...missing, ...catalogErrors, ...excludedErrors];

if (failures.length > 0) {
  throw new Error(`training model catalog regression failed:\n${failures.join('\n')}`);
}

console.log('training model catalog regression passed');
