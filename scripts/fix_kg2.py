path = r'D:\project\0721\Deep2\src\views\knowledge\KnowledgeGraph3D.vue'
with open(path,'r',encoding='utf-8') as f: c=f.read()

# 1. Add selectedNodeId after readingArticle
c = c.replace(
    'const readingArticle = ref<any>(null);\nconst rotSpeed = ref(1.0); const physicsOn = ref(true);',
    'const readingArticle = ref<any>(null);\nlet selectedNodeId: string|null = null;\nconst rotSpeed = ref(1.0); const physicsOn = ref(true);'
)

# 2. Update onClick to set selectedNodeId
c = c.replace(
    'if (best) { sel.value = getNodeInfo(best.id); panelOpen.value = true; fetchArticles(best.id); }\n  else closePanel();',
    'if (best) { sel.value = getNodeInfo(best.id); panelOpen.value = true; fetchArticles(best.id); selectedNodeId = best.id; }\n  else { closePanel(); selectedNodeId = null; }'
)

# 3. Update closePanel to clear selectedNodeId
c = c.replace(
    'const closePanel = () => { panelOpen.value = false; sel.value = null; articles.value = []; };',
    'const closePanel = () => { panelOpen.value = false; sel.value = null; articles.value = []; selectedNodeId = null; };'
)

# 4. Modify node rendering to dim non-selected nodes
# Find the node rendering section and add selection logic
old_node_render = '''  // 节点
  [...rnodes].sort((a, b) => b.sr - a.sr).forEach(rn => {'''
new_node_render = '''  // 节点(选中星球高亮, 其他半透明, 子节点保持不透明)
  const selId = selectedNodeId;
  const selectedNode = selId ? nodeMap.get(selId) : null;
  const isChildOfSelected = (rn: RNode) => {
    if (!selectedNode) return false;
    if (rn.id === selId) return false;
    return selectedNode.childIds.includes(rn.id);
  };
  [...rnodes].sort((a, b) => b.sr - a.sr).forEach(rn => {'''
c = c.replace(old_node_render, new_node_render)

# 5. After the existing glow/ball rendering, add opacity logic
# Find the gradient rendering section and add alpha modifier
old_gradient = '''    const g = c!.createRadialGradient(x - r * 0.2, y - r * 0.2, r * 0.05, x, y, r);
    g.addColorStop(0, '#fff'); g.addColorStop(0.25, rn.depth === 0 ? '#4dc9f0' : cl);
    g.addColorStop(1, (rn.depth === 0 ? '#4dc9f0' : cl) + '44');
    c!.beginPath(); c!.arc(x, y, r, 0, Math.PI * 2); c!.fillStyle = g; c!.fill();'''
new_gradient = '''    // 选中态: 其他星球半透明, 子节点保持正常
    let nodeAlpha = 1;
    if (selId && rn.id !== selId && !isChildOfSelected(rn)) nodeAlpha = 0.25;
    c!.globalAlpha = nodeAlpha;
    const g = c!.createRadialGradient(x - r * 0.2, y - r * 0.2, r * 0.05, x, y, r);
    g.addColorStop(0, '#fff'); g.addColorStop(0.25, rn.depth === 0 ? '#4dc9f0' : cl);
    g.addColorStop(1, (rn.depth === 0 ? '#4dc9f0' : cl) + '44');
    c!.beginPath(); c!.arc(x, y, r, 0, Math.PI * 2); c!.fillStyle = g; c!.fill();
    c!.globalAlpha = 1;'''
c = c.replace(old_gradient, new_gradient)

# 6. Also dim the halo/glow
old_halo = '''    // 光晕
    const h = c!.createRadialGradient(x, y, r * 0.2, x, y, r * 2.2);
    h.addColorStop(0, cl + '55'); h.addColorStop(0.4, cl + '0a'); h.addColorStop(1, 'transparent');
    c!.beginPath(); c!.arc(x, y, r * 2.2, 0, Math.PI * 2); c!.fillStyle = h; c!.fill();'''
new_halo = '''    // 光晕(选中态也受alpha影响)
    c!.globalAlpha = nodeAlpha;
    const h = c!.createRadialGradient(x, y, r * 0.2, x, y, r * 2.2);
    h.addColorStop(0, cl + '55'); h.addColorStop(0.4, cl + '0a'); h.addColorStop(1, 'transparent');
    c!.beginPath(); c!.arc(x, y, r * 2.2, 0, Math.PI * 2); c!.fillStyle = h; c!.fill();
    c!.globalAlpha = 1;'''
c = c.replace(old_halo, new_halo)

# 7. Add selected ring effect
old_root_ring = '''    // 根节点光环
    if (rn.depth === 0) {'''
new_root_ring = '''    // 选中环
    if (selId && rn.id === selId) {
      c!.beginPath(); c!.arc(x, y, r * 1.6, 0, Math.PI * 2);
      c!.strokeStyle = cl + 'aa'; c!.lineWidth = 3; c!.stroke();
      c!.beginPath(); c!.arc(x, y, r * 2.0, 0, Math.PI * 2);
      c!.strokeStyle = cl + '44'; c!.lineWidth = 1.5; c!.setLineDash([6, 6]); c!.stroke(); c!.setLineDash([]);
    }
    // 根节点光环
    if (rn.depth === 0) {'''
c = c.replace(old_root_ring, new_root_ring)

# 8. Also dim labels
old_label = '''    // 标签
    if (r > (rn.depth === 0 ? 10 : rn.depth <= 1 ? 8 : 5)) {'''
new_label = '''    // 标签(选中态也受alpha影响)
    c!.globalAlpha = nodeAlpha;
    if (r > (rn.depth === 0 ? 10 : rn.depth <= 1 ? 8 : 5)) {'''
c = c.replace(old_label, new_label)

# Reset alpha after label
old_label_end = '''      c!.fillText(rn.label, x, y + r + (rn.depth === 0 ? 12 : 6));
    }'''
new_label_end = '''      c!.fillText(rn.label, x, y + r + (rn.depth === 0 ? 12 : 6));
    }
    c!.globalAlpha = 1;'''
c = c.replace(old_label_end, new_label_end)

# 9. Also dim connection lines
old_conn = '''  // 连线
  conns.forEach(([ai, bi]) => {'''
new_conn = '''  // 连线(选中星球外连线变淡)
  conns.forEach(([ai, bi]) => {'''
c = c.replace(old_conn, new_conn)

# Add dimming to connection lines (simplified)
old_conn_draw = '''    c!.globalAlpha = alpha + 0.06 * Math.sin(t * 3 + ai + bi);
    c!.stroke(); c!.globalAlpha = 1;'''
new_conn_draw = '''    let connAlpha = alpha + 0.06 * Math.sin(t * 3 + ai + bi);
    if (selId && a.id !== selId && b.id !== selId) connAlpha *= 0.2;
    c!.globalAlpha = connAlpha;
    c!.stroke(); c!.globalAlpha = 1;'''
c = c.replace(old_conn_draw, new_conn_draw)

with open(path,'w',encoding='utf-8') as f: f.write(c)
print('OK')
