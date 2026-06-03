const API_ROOT = 'http://localhost:8080/api/v1';

async function requestJson(path: string, init?: RequestInit) {
  const response = await fetch(`${API_ROOT}${path}`, init);
  const text = await response.text();
  let payload: any;
  try {
    payload = text ? JSON.parse(text) : {};
  } catch {
    throw new Error(`invalid JSON from ${path}: ${text.slice(0, 120)}`);
  }
  if (!response.ok || payload.code !== 200) {
    throw new Error(`${path} failed with HTTP ${response.status}, code ${payload.code}: ${payload.message || text}`);
  }
  return payload;
}

const login = await requestJson('/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'ChangeMe-Admin-2026!' }),
});

const token = login.data?.token;
if (!token) throw new Error('login token missing');

const overview = await requestJson('/data-assets', {
  headers: { Authorization: `Bearer ${token}` },
});

const data = overview.data || {};
const assets: any[] = Array.isArray(data.assets) ? data.assets : [];
const trainingAssets = assets.filter((asset) => asset.type === 'training');
const runnableTraining = trainingAssets.filter((asset) => String(asset.route || '') === '/training');

if ((data.summary?.training || 0) < 1) {
  throw new Error(`data asset center should expose training assets; got summary.training=${data.summary?.training || 0}`);
}

if (trainingAssets.length < 1 || runnableTraining.length < 1) {
  throw new Error(`data asset center should include routeable training rows; got training=${trainingAssets.length}, routeable=${runnableTraining.length}`);
}

console.log('data asset center contract passed');
