import assert from 'node:assert/strict';

import {
  buildPredictionAnalysisRecord,
  listPredictionAnalysisRecords,
  savePredictionAnalysisRecord,
  selectPredictionAnalysisRecord,
} from '../src/utils/predictionAnalysis';

type StorageShape = Record<string, string>;

const storageData: StorageShape = {};

const storage = {
  getItem(key: string) {
    return storageData[key] ?? null;
  },
  setItem(key: string, value: string) {
    storageData[key] = value;
  },
  removeItem(key: string) {
    delete storageData[key];
  },
};

Object.defineProperty(globalThis, 'localStorage', {
  value: storage,
  configurable: true,
});

Object.defineProperty(globalThis, 'sessionStorage', {
  value: storage,
  configurable: true,
});

const record = buildPredictionAnalysisRecord({
  modelName: 'BSARec-Job',
  request: {
    user_history: [7, 11, 18, 24],
    top_k: 5,
    include_job_info: true,
  },
  response: {
    status: 'success',
    message: 'ok',
    serviceUrl: 'http://127.0.0.1:5000/recommend',
  },
  elapsedMs: 842,
  recommendations: [
    { rank: 1, itemId: '101', company: 'A', position: 'ML Engineer', score: 0.91 },
    { rank: 2, itemId: '102', company: 'A', position: 'Data Scientist', score: 0.82 },
    { rank: 3, itemId: '103', company: 'B', position: 'Backend Engineer', score: 0.74 },
  ],
});

assert.equal(record.metrics.historyLength, 4);
assert.equal(record.metrics.recommendationCount, 3);
assert.equal(record.metrics.fillRate, 0.6);
assert.equal(record.metrics.detailCoverage, 1);
assert.equal(record.metrics.uniqueCompanyCount, 2);
assert.equal(record.metrics.latencyBucket, 'good');

const saved = savePredictionAnalysisRecord(record);
selectPredictionAnalysisRecord(saved.id);

assert.equal(listPredictionAnalysisRecords()[0].id, saved.id);
assert.equal(sessionStorage.getItem('deepinsight:selected-prediction-analysis-record'), saved.id);
