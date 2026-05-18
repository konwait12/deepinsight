path = r'D:\project\0721\Deep2\src\views\knowledge\KnowledgeGraph3D.vue'
with open(path,'r',encoding='utf-8') as f: c=f.read()

# 1. Update onClick
c = c.replace(
    "popStyle.value = { left: e.clientX + 14 + 'px', top: e.clientY - 14 + 'px' };",
    "panelOpen.value = true; fetchArticles(best.id);"
)
c = c.replace("sel.value = null;", "closePanel();", 1)  # first occurrence only (in onClick else branch)

# 2. Add helper functions before onSpeed
old = "const onSpeed = (e: Event) => { rotSpeed.value = parseFloat((e.target as HTMLInputElement).value); };"
new = """const closePanel = () => { panelOpen.value = false; sel.value = null; articles.value = []; };
const fetchArticles = async (nodeId: string) => {
  articlesLoading.value = true; articles.value = [];
  try {
    const res = await fetch('/api/v1/forum/knowledge/articles');
    const data = await res.json();
    if (data.code === 200) articles.value = (data.data || []).filter((a:any) => a.nodeId === nodeId);
  } catch(e) { console.error(e); }
  articlesLoading.value = false;
};
const openArticle = (a: any) => { readingArticle.value = a; };
const renderMd = (md: string) => {
  if (!md) return '';
  let h = md.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  h = h.replace(/```(\\w*)\\n?([\\s\\S]*?)```/g, '<pre><code>$2</code></pre>');
  h = h.replace(/`([^`]+)`/g, '<code>$1</code>');
  h = h.replace(/\\*\\*(.+?)\\*\\*/g, '<strong>$1</strong>');
  h = h.replace(/\\*(.+?)\\*/g, '<em>$1</em>');
  h = h.replace(/^### (.+)$/gm, '<h4>$1</h4>');
  h = h.replace(/^## (.+)$/gm, '<h3>$1</h3>');
  h = h.replace(/^# (.+)$/gm, '<h2>$1</h2>');
  h = h.replace(/^- (.+)$/gm, '<li>$1</li>');
  h = h.replace(/\\n/g, '<br/>');
  return h;
};

const onSpeed = (e: Event) => { rotSpeed.value = parseFloat((e.target as HTMLInputElement).value); };"""
c = c.replace(old, new)

with open(path,'w',encoding='utf-8') as f: f.write(c)
print('OK')
