path = r'D:\project\0721\Deep2\src\views\knowledge\KnowledgeGraph3D.vue'
with open(path,'r',encoding='utf-8') as f: c=f.read()

# 1. Add findHovered function and update onClick
old_click = '''const onClick = (e: MouseEvent) => {
  if (!cv.value) return;
  const r = cv.value.getBoundingClientRect(); const mx = e.clientX - r.left, my = e.clientY - r.top;
  let best: RNode | null = null, bd = Infinity;
  for (const rn of rnodes) { const d = Math.hypot(mx - rn.sx, my - rn.sy); if (d < rn.sr + 14 && d < bd) { bd = d; best = rn; } }
  if (best) { sel.value = getNodeInfo(best.id); panelOpen.value = true; fetchArticles(best.id); selectedNodeId = best.id; }
  else { closePanel(); selectedNodeId = null; }
};'''

new_click = '''const findHovered = (): string|null => {
  if (!cv.value) return null;
  const r = cv.value.getBoundingClientRect();
  const mx = mouseX - r.left, my = mouseY - r.top;
  let best: RNode|null = null, bd = Infinity;
  for (const rn of rnodes) { const d = Math.hypot(mx - rn.sx, my - rn.sy); if (d < rn.sr + 18 && d < bd) { bd = d; best = rn; } }
  return best ? best.id : null;
};

const onClick = (e: MouseEvent) => {
  if (!cv.value) return;
  const r = cv.value.getBoundingClientRect(); const mx = e.clientX - r.left, my = e.clientY - r.top;
  let best: RNode | null = null, bd = Infinity;
  for (const rn of rnodes) { const d = Math.hypot(mx - rn.sx, my - rn.sy); if (d < rn.sr + 18 && d < bd) { bd = d; best = rn; } }
  if (best) {
    selectedNodeId = best.id;
    if (focusNodeId === best.id) {
      // 再次点击同一星球: 打开文章面板
      sel.value = getNodeInfo(best.id); panelOpen.value = true; fetchArticles(best.id);
    } else if (focusNodeId && best.id !== focusNodeId) {
      // 点击不同星球: 切换焦点
      focusNodeId = best.id; sel.value = null; panelOpen.value = false;
    } else {
      // 首次点击: 聚焦 + 弹出选择
      focusNodeId = best.id;
      sel.value = getNodeInfo(best.id);
    }
  } else {
    closePanel(); selectedNodeId = null; focusNodeId = null;
  }
};

const goBack = () => { focusNodeId = null; closePanel(); };'''

c = c.replace(old_click, new_click)

# 2. Add mouse tracking to onMv
old_mv = '''const onMv = (e: MouseEvent) => { if (!drag) return; try_ = sry + (e.clientX - dx) * 0.005; trx = Math.max(-1.0, Math.min(1.2, srx - (e.clientY - dy) * 0.005)); };'''
new_mv = '''const onMv = (e: MouseEvent) => {
  mouseX = e.clientX; mouseY = e.clientY;
  if (!drag) { hoveredNodeId = findHovered(); return; }
  try_ = sry + (e.clientX - dx) * 0.005; trx = Math.max(-1.0, Math.min(1.2, srx - (e.clientY - dy) * 0.005));
};'''
c = c.replace(old_mv, new_mv)

# 3. Add focus zoom to projection rendering - use focusZoom in projection
old_zm = 'let d = 6 * zm, sc = d / (d + rz2);'
new_zm = 'let effZm = zm * focusZoom;\n  let d = 6 * effZm, sc = d / (d + rz2);'
c = c.replace(old_zm, new_zm)

# 4. Add hover scale effect in node rendering
old_node_loop = '''  [...rnodes].sort((a, b) => b.sr - a.sr).forEach(rn => {
    const { sx: x, sy: y, sr: r, color: cl } = rn; if (r < 2) return;'''
new_node_loop = '''  [...rnodes].sort((a, b) => b.sr - a.sr).forEach(rn => {
    const { sx: x, sy: y, sr: r0, color: cl } = rn; if (r0 < 2) return;
    const hoverScale = (hoveredNodeId === rn.id && !focusNodeId) ? 1.25 : 1;
    const focusScale = (focusNodeId === rn.id) ? 1.3 : 1;
    const r = r0 * hoverScale * focusScale;'''
c = c.replace(old_node_loop, new_node_loop)

# 5. Add back button template in the panel
old_panel_header = '''    <div class="kg-article-panel" :class="{ open: panelOpen }">
      <div class="panel-header">'''
new_panel_header = '''    <div class="kg-article-panel" :class="{ open: panelOpen }">
      <div v-if="focusNodeId && !panelOpen" class="focus-menu">
        <div class="focus-label">{{ sel?.label || '' }}</div>
        <button class="focus-btn" @click.stop="toggleNode(focusNodeId!)">{{ sel?.expanded ? '收起子节点' : '展开子知识点' }}</button>
        <button class="focus-btn primary" @click.stop="sel=sel; panelOpen=true; fetchArticles(focusNodeId!)">查看相关文章</button>
        <button class="focus-back" @click.stop="goBack">← 返回全局</button>
      </div>
      <div class="panel-header">'''
c = c.replace(old_panel_header, new_panel_header)

with open(path,'w',encoding='utf-8') as f: f.write(c)
print('OK')
