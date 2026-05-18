<template>
  <div ref="container" class="cyber-scene-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import * as THREE from 'three';
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/examples/jsm/postprocessing/OutputPass.js';
import { ShaderPass } from 'three/examples/jsm/postprocessing/ShaderPass.js';
import { AfterimagePass } from 'three/examples/jsm/postprocessing/AfterimagePass.js';
import { RGBShiftShader } from 'three/examples/jsm/shaders/RGBShiftShader.js';

const props = defineProps<{ scrollProgress: number }>();
const emit = defineEmits<{ ready: [] }>();

const container = ref<HTMLDivElement>();

// ── Core ──
let renderer: THREE.WebGLRenderer;
let scene: THREE.Scene;
let camera: THREE.PerspectiveCamera;
let composer: EffectComposer;
let bloomPass: UnrealBloomPass;
let rgbShiftPass: ShaderPass;
let afterimagePass: AfterimagePass;
let animId = 0;
let disposed = false;

// ── Collections ──
const screenPanels: THREE.Mesh[] = [];
const neonStrips: THREE.Mesh[] = [];
const corridorSegments: THREE.Group[] = [];
const volumetricBeams: THREE.Mesh[] = [];
let particleSystem: THREE.Points;
let rainSystem: THREE.Points;
let particlePositions: Float32Array;
let particleBasePos: Float32Array;
let rainPositions: Float32Array;
let rainVelocities: Float32Array;
const screenCanvasPool: { canvas: HTMLCanvasElement; ctx: CanvasRenderingContext2D; phase: number; mode: number; timer: number }[] = [];

// ── State ──
let mouseX = 0;
let mouseY = 0;
let targetMouseX = 0;
let targetMouseY = 0;
let currentZ = 0;
let targetZ = 0;
const CORRIDOR_LENGTH = 60;
const SEGMENT_LENGTH = 12;
const WALL_WIDTH = 14;
const WALL_HEIGHT = 8;
const PARTICLE_COUNT = 2800;
const RAIN_COUNT = 400;
let isMobile = false;
const CYBER_COLORS = [
  new THREE.Color('#00d4ff'),
  new THREE.Color('#a855f7'),
  new THREE.Color('#3b82f6'),
  new THREE.Color('#ec4899'),
  new THREE.Color('#22d3ee'),
];

// ═══════════════════════════════════════
//  BUILD
// ═══════════════════════════════════════

function createCorridorSegment(zOffset: number): THREE.Group {
  const g = new THREE.Group();
  const darkMat = new THREE.MeshStandardMaterial({ color: 0x050510, roughness: 0.7, metalness: 0.3 });
  const add = (w: number, h: number, rx: number, ry: number, px: number, py: number, pz: number) => {
    const m = new THREE.Mesh(new THREE.PlaneGeometry(w, h), darkMat);
    m.rotation.set(rx, ry, 0);
    m.position.set(px, py, pz);
    m.receiveShadow = true;
    g.add(m);
  };
  const z = zOffset - SEGMENT_LENGTH / 2;
  add(WALL_WIDTH, SEGMENT_LENGTH, -Math.PI / 2, 0, 0, -WALL_HEIGHT / 2, z);            // floor
  add(SEGMENT_LENGTH, WALL_HEIGHT, 0, Math.PI / 2, -WALL_WIDTH / 2, 0, z);              // left
  add(SEGMENT_LENGTH, WALL_HEIGHT, 0, -Math.PI / 2, WALL_WIDTH / 2, 0, z);              // right
  add(WALL_WIDTH, SEGMENT_LENGTH, Math.PI / 2, 0, 0, WALL_HEIGHT / 2, z);               // ceiling

  // Grid lines
  const gm = new THREE.MeshBasicMaterial({ color: 0x00d4ff, transparent: true, opacity: 0.05 });
  for (let x = -WALL_WIDTH / 2 + 2; x <= WALL_WIDTH / 2 - 2; x += 2) {
    const line = new THREE.Mesh(new THREE.PlaneGeometry(0.015, WALL_HEIGHT), gm);
    line.position.set(x, 0, z + 0.01);
    g.add(line);
  }
  return g;
}

function createNeonStrip(x: number, y: number, z: number, ry: number, color: THREE.Color, len: number): THREE.Mesh {
  const m = new THREE.Mesh(new THREE.PlaneGeometry(len, 0.06), new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.85 }));
  m.position.set(x, y, z);
  m.rotation.y = ry;
  m.userData = { baseX: x, baseY: y, baseZ: z, ry, len };
  neonStrips.push(m);
  return m;
}

function createScreenPanel(x: number, y: number, z: number, ry: number, w: number, h: number, color: THREE.Color): THREE.Mesh {
  const canvas = document.createElement('canvas');
  canvas.width = 256;
  canvas.height = 144;
  const ctx = canvas.getContext('2d')!;
  screenCanvasPool.push({ canvas, ctx, phase: Math.random() * Math.PI * 2, mode: Math.floor(Math.random() * 4), timer: 0 });

  drawScreenContent(ctx, color, 0, 0);

  const tex = new THREE.CanvasTexture(canvas);
  tex.minFilter = THREE.LinearFilter;
  tex.magFilter = THREE.LinearFilter;
  const panel = new THREE.Mesh(
    new THREE.PlaneGeometry(w, h),
    new THREE.MeshBasicMaterial({ map: tex, transparent: true, opacity: 0.72, side: THREE.DoubleSide })
  );
  panel.position.set(x, y, z);
  panel.rotation.y = ry;
  panel.userData = { color, baseY: y, canvasIdx: screenCanvasPool.length - 1, tex };
  screenPanels.push(panel);
  return panel;
}

function drawScreenContent(ctx: CanvasRenderingContext2D, color: THREE.Color, t: number, mode: number) {
  const { width, height } = ctx.canvas;
  ctx.clearRect(0, 0, width, height);
  const r = Math.floor(color.r * 255), g = Math.floor(color.g * 255), b = Math.floor(color.b * 255);

  // Border
  ctx.strokeStyle = `rgba(${r},${g},${b},0.5)`;
  ctx.lineWidth = 1.5;
  ctx.strokeRect(3, 3, width - 6, height - 6);

  if (mode === 0) {
    // Pulsing waveform
    ctx.strokeStyle = `rgba(${r},${g},${b},0.4)`;
    ctx.lineWidth = 2;
    ctx.beginPath();
    for (let i = 0; i < width; i += 2) {
      const y = height / 2 + Math.sin(i * 0.03 + t * 2) * 30 + Math.cos(i * 0.07 + t) * 15;
      i === 0 ? ctx.moveTo(i, y) : ctx.lineTo(i, y);
    }
    ctx.stroke();
  } else if (mode === 1) {
    // Scanning bars
    for (let i = 0; i < 8; i++) {
      const barY = ((i * 18 + t * 40) % (height + 40)) - 20;
      const alpha = 0.1 + Math.abs(Math.sin(i + t)) * 0.25;
      ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`;
      ctx.fillRect(20, barY, width - 40, 4);
    }
  } else if (mode === 2) {
    // Expanding rings
    for (let i = 0; i < 5; i++) {
      const radius = ((i * 30 + t * 40) % 120);
      const alpha = 0.3 * (1 - radius / 120);
      ctx.strokeStyle = `rgba(${r},${g},${b},${alpha})`;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(width / 2, height / 2, radius, 0, Math.PI * 2);
      ctx.stroke();
    }
  } else {
    // Data matrix
    for (let i = 0; i < 30; i++) {
      const dx = 30 + (i % 6) * 35;
      const dy = 25 + Math.floor(i / 6) * 30;
      const alpha = 0.1 + Math.abs(Math.sin(dx * 0.1 + t + i * 0.5)) * 0.35;
      ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`;
      ctx.fillRect(dx, dy, 28, 3);
    }
  }
  // tiny header
  ctx.fillStyle = `rgba(${r},${g},${b},0.9)`;
  ctx.font = '10px monospace';
  ctx.fillText('DEEPINSIGHT', 16, 18);
}

function createVolumetricBeam(x: number, z: number, color: THREE.Color): THREE.Mesh {
  const height = 2 + Math.random() * 3;
  const geo = new THREE.CylinderGeometry(0.04, 0.3, height, 8, 1, true);
  const mat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.06, side: THREE.DoubleSide, depthWrite: false });
  const beam = new THREE.Mesh(geo, mat);
  beam.position.set(x, height / 2, z);
  beam.userData = { baseX: x, baseZ: z, speed: 0.3 + Math.random() * 0.7 };
  volumetricBeams.push(beam);
  return beam;
}

function createGlowTexture(): THREE.Texture {
  const c = document.createElement('canvas');
  c.width = 32; c.height = 32;
  const ctx = c.getContext('2d')!;
  const grad = ctx.createRadialGradient(16, 16, 0, 16, 16, 16);
  grad.addColorStop(0, 'rgba(255,255,255,1)');
  grad.addColorStop(0.08, 'rgba(255,255,255,0.85)');
  grad.addColorStop(0.35, 'rgba(255,255,255,0.25)');
  grad.addColorStop(1, 'rgba(255,255,255,0)');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, 32, 32);
  return new THREE.CanvasTexture(c);
}

function createRain(): THREE.Points {
  const count = isMobile ? 80 : RAIN_COUNT;
  const pos = new Float32Array(count * 3);
  const colors = new Float32Array(count * 3);
  rainVelocities = new Float32Array(count);
  for (let i = 0; i < count; i++) {
    pos[i * 3] = (Math.random() - 0.5) * WALL_WIDTH * 0.85;
    pos[i * 3 + 1] = Math.random() * WALL_HEIGHT * 1.4 - WALL_HEIGHT * 0.7;
    pos[i * 3 + 2] = Math.random() * CORRIDOR_LENGTH;
    const c = CYBER_COLORS[Math.floor(Math.random() * CYBER_COLORS.length)];
    colors[i * 3] = c.r; colors[i * 3 + 1] = c.g; colors[i * 3 + 2] = c.b;
    rainVelocities[i] = 0.04 + Math.random() * 0.12;
  }
  rainPositions = pos;
  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3));
  const mat = new THREE.PointsMaterial({
    size: 0.04,
    map: createGlowTexture(),
    vertexColors: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    transparent: true,
    opacity: 0.5,
  });
  return new THREE.Points(geo, mat);
}

function createParticles(): THREE.Points {
  const count = isMobile ? 600 : PARTICLE_COUNT;
  const pos = new Float32Array(count * 3);
  const colors = new Float32Array(count * 3);
  for (let i = 0; i < count; i++) {
    pos[i * 3] = (Math.random() - 0.5) * WALL_WIDTH * 0.85;
    pos[i * 3 + 1] = (Math.random() - 0.5) * WALL_HEIGHT * 0.85;
    pos[i * 3 + 2] = Math.random() * CORRIDOR_LENGTH;
    const c = CYBER_COLORS[Math.floor(Math.random() * CYBER_COLORS.length)];
    colors[i * 3] = c.r; colors[i * 3 + 1] = c.g; colors[i * 3 + 2] = c.b;
  }
  particlePositions = pos;
  particleBasePos = new Float32Array(pos);
  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3));
  const mat = new THREE.PointsMaterial({
    size: 0.10,
    map: createGlowTexture(),
    vertexColors: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    transparent: true,
    opacity: 0.65,
  });
  return new THREE.Points(geo, mat);
}

// ═══════════════════════════════════════
//  INIT
// ═══════════════════════════════════════

function initScene() {
  if (!container.value) return;
  isMobile = window.innerWidth < 768;

  // Renderer
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.1;
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  container.value.appendChild(renderer.domElement);

  // Scene
  scene = new THREE.Scene();
  scene.fog = new THREE.FogExp2(0x000000, isMobile ? 0.005 : 0.0025);

  // Camera
  camera = new THREE.PerspectiveCamera(62, window.innerWidth / window.innerHeight, 0.5, 80);
  camera.position.set(0, 0, 14);
  camera.lookAt(0, 0, -8);

  // Lights
  scene.add(new THREE.AmbientLight(0x111133, 0.35));
  for (let z = 1; z < CORRIDOR_LENGTH; z += 6) {
    const c = CYBER_COLORS[Math.floor(Math.random() * CYBER_COLORS.length)];
    const pt = new THREE.PointLight(c, isMobile ? 8 : 14, 10, 1.5);
    pt.position.set((Math.random() - 0.5) * WALL_WIDTH * 0.7, (Math.random() - 0.5) * WALL_HEIGHT * 0.6, z);
    scene.add(pt);
  }

  // Corridor
  const segs = isMobile ? 3 : Math.ceil(CORRIDOR_LENGTH / SEGMENT_LENGTH);
  for (let i = 0; i < segs; i++) {
    const seg = createCorridorSegment(i * SEGMENT_LENGTH);
    scene.add(seg);
    corridorSegments.push(seg);
  }

  // Neon strips
  for (let z = 1; z < CORRIDOR_LENGTH; z += 3) {
    const side = Math.random() > 0.5 ? 1 : -1;
    const c = CYBER_COLORS[Math.floor(Math.random() * CYBER_COLORS.length)];
    scene.add(createNeonStrip(side * (WALL_WIDTH / 2 - 0.12), (Math.random() - 0.5) * WALL_HEIGHT * 0.6, z, side > 0 ? -Math.PI / 2 : Math.PI / 2, c, 2));
    if (Math.random() > 0.5) {
      const hc = CYBER_COLORS[Math.floor(Math.random() * CYBER_COLORS.length)];
      const hs = createNeonStrip(0, WALL_HEIGHT / 2 - 0.1, z, 0, hc, 2.5);
      hs.rotation.x = -Math.PI / 2;
      scene.add(hs);
    }
  }

  // Screens
  const scCount = isMobile ? 4 : 10;
  for (let i = 0; i < scCount; i++) {
    const side = i % 2 === 0 ? 1 : -1;
    const z = 3 + i * (CORRIDOR_LENGTH - 6) / scCount;
    const y = (Math.random() - 0.5) * (WALL_HEIGHT - 2.5);
    const c = CYBER_COLORS[i % CYBER_COLORS.length];
    scene.add(createScreenPanel(side * (WALL_WIDTH / 2 - 0.25), y, z, side > 0 ? -Math.PI / 2 : Math.PI / 2, isMobile ? 1.4 : 2.2, isMobile ? 0.8 : 1.24, c));
  }

  // Volumetric beams
  for (let i = 0; i < (isMobile ? 3 : 8); i++) {
    const c = CYBER_COLORS[i % CYBER_COLORS.length];
    scene.add(createVolumetricBeam((Math.random() - 0.5) * WALL_WIDTH * 0.7, 2 + Math.random() * (CORRIDOR_LENGTH - 4), c));
  }

  // Particles
  particleSystem = createParticles();
  scene.add(particleSystem);

  // Rain
  rainSystem = createRain();
  scene.add(rainSystem);

  // ── Post-processing ──
  const renderPass = new RenderPass(scene, camera);
  renderPass.clearAlpha = 0;

  bloomPass = new UnrealBloomPass(new THREE.Vector2(window.innerWidth, window.innerHeight), isMobile ? 0.8 : 1.4, 0.4, 0.15);
  bloomPass.threshold = 0.15;
  bloomPass.smoothWidth = 0.5;

  rgbShiftPass = new ShaderPass(RGBShiftShader);
  rgbShiftPass.uniforms.amount.value = 0.0012;

  afterimagePass = new AfterimagePass(0.15);
  afterimagePass.uniforms.damp.value = 0.92;

  const outputPass = new OutputPass();

  composer = new EffectComposer(renderer);
  composer.addPass(renderPass);
  composer.addPass(bloomPass);
  composer.addPass(afterimagePass);
  composer.addPass(rgbShiftPass);
  composer.addPass(outputPass);

  emit('ready');
}

// ═══════════════════════════════════════
//  ANIMATE
// ═══════════════════════════════════════

function animate() {
  if (disposed) return;
  animId = requestAnimationFrame(animate);

  const t = performance.now() * 0.001;

  // Mouse smooth
  mouseX += (targetMouseX - mouseX) * 0.06;
  mouseY += (targetMouseY - mouseY) * 0.06;

  // Camera
  targetZ = -props.scrollProgress * 26;
  currentZ += (targetZ - currentZ) * 0.065;
  camera.position.set(mouseX * 0.4, mouseY * 0.5, 14 + currentZ);
  camera.lookAt(new THREE.Vector3(mouseX * 1.2, mouseY * 0.8, camera.position.z - 14));

  const camZ = camera.position.z;

  // Corridor wrap
  for (const seg of corridorSegments) {
    if (seg.position.z - camZ < -SEGMENT_LENGTH) seg.position.z += SEGMENT_LENGTH * corridorSegments.length;
  }

  // Neon strips wrap + pulse
  for (const strip of neonStrips) {
    if (strip.position.z - camZ < -4) strip.position.z += CORRIDOR_LENGTH;
    const mat = strip.material as THREE.MeshBasicMaterial;
    mat.opacity = 0.55 + Math.sin(t * 2 + strip.position.z * 0.5) * 0.35;
  }

  // Screens animate + wrap
  for (let i = 0; i < screenPanels.length; i++) {
    const panel = screenPanels[i];
    if (panel.position.z - camZ < -4) panel.position.z += CORRIDOR_LENGTH;
    panel.position.y = (panel.userData.baseY as number) + Math.sin(t + panel.position.z * 0.3) * 0.12;

    // Refresh screen texture every 2s
    const sc = screenCanvasPool[i];
    if (sc) {
      sc.timer += 0.016;
      if (sc.timer > 2.0 + sc.phase * 2) {
        sc.timer = 0;
        sc.mode = (sc.mode + 1) % 4;
      }
      drawScreenContent(sc.ctx, panel.userData.color as THREE.Color, t + sc.phase, sc.mode);
      (panel.userData.tex as THREE.Texture).needsUpdate = true;
    }
  }

  // Volumetric beams wobble
  for (const beam of volumetricBeams) {
    if (beam.position.z - camZ < -3) beam.position.z += CORRIDOR_LENGTH;
    beam.position.x = (beam.userData.baseX as number) + Math.sin(t * beam.userData.speed) * 0.6;
    beam.material.opacity = (0.03 + Math.sin(t * 1.5 + beam.position.z) * 0.02);
  }

  // Particle float + mouse repel
  if (particleSystem && particlePositions) {
    const count = isMobile ? 600 : PARTICLE_COUNT;
    for (let i = 0; i < count; i++) {
      const i3 = i * 3;
      const bx = particleBasePos[i3], by = particleBasePos[i3 + 1], bz = particleBasePos[i3 + 2];
      particlePositions[i3] = bx + Math.sin(t * 0.6 + i * 0.3) * 0.25;
      particlePositions[i3 + 1] = by + Math.cos(t * 0.55 + i * 0.4) * 0.2;
      particlePositions[i3 + 2] = (bz + t * 0.25) % CORRIDOR_LENGTH;
      const mdx = particlePositions[i3] - mouseX * WALL_WIDTH * 0.3;
      const mdy = particlePositions[i3 + 1] - mouseY * WALL_HEIGHT * 0.3;
      const md = Math.hypot(mdx, mdy);
      if (md < 2.5 && md > 0) { particlePositions[i3] += (mdx / md) * 0.012; particlePositions[i3 + 1] += (mdy / md) * 0.012; }
    }
    (particleSystem.geometry as THREE.BufferGeometry).attributes.position.needsUpdate = true;
  }

  // Rain fall
  if (rainSystem && rainPositions) {
    const rc = isMobile ? 80 : RAIN_COUNT;
    for (let i = 0; i < rc; i++) {
      const i3 = i * 3;
      rainPositions[i3 + 1] -= rainVelocities[i];
      if (rainPositions[i3 + 1] < -WALL_HEIGHT * 0.7) {
        rainPositions[i3 + 1] = WALL_HEIGHT * 0.7;
        rainPositions[i3] = (Math.random() - 0.5) * WALL_WIDTH * 0.85;
        rainPositions[i3 + 2] = Math.random() * CORRIDOR_LENGTH;
      }
    }
    (rainSystem.geometry as THREE.BufferGeometry).attributes.position.needsUpdate = true;
  }

  composer.render();
}

// ═══════════════════════════════════════
//  LIFECYCLE
// ═══════════════════════════════════════

function onResize() {
  if (!camera || !renderer) return;
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
  composer.setSize(window.innerWidth, window.innerHeight);
}

function dispose() {
  disposed = true;
  cancelAnimationFrame(animId);
  const dm = (m: THREE.Mesh) => { m.geometry?.dispose(); if (Array.isArray(m.material)) m.material.forEach(d => d.dispose()); else (m.material as THREE.Material)?.dispose(); if ((m.material as THREE.MeshBasicMaterial)?.map) (m.material as THREE.MeshBasicMaterial).map!.dispose(); };
  corridorSegments.forEach(g => g.traverse(c => { if (c instanceof THREE.Mesh) dm(c); }));
  neonStrips.forEach(dm);
  screenPanels.forEach(dm);
  volumetricBeams.forEach(dm);
  if (particleSystem) { particleSystem.geometry.dispose(); (particleSystem.material as THREE.Material).dispose(); }
  if (rainSystem) { rainSystem.geometry.dispose(); (rainSystem.material as THREE.Material).dispose(); }
  bloomPass?.dispose?.();
  composer?.renderer?.dispose?.();
  renderer?.dispose();
  scene?.clear();
}

function setMouse(cx: number, cy: number) {
  targetMouseX = (cx / window.innerWidth) * 2 - 1;
  targetMouseY = -(cy / window.innerHeight) * 2 + 1;
}

defineExpose({ setMouse });

onMounted(() => {
  initScene();
  animId = requestAnimationFrame(animate);
  window.addEventListener('resize', onResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', onResize);
  dispose();
});
</script>

<style scoped>
.cyber-scene-container {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
.cyber-scene-container :deep(canvas) {
  display: block;
}
</style>
