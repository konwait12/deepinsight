<template>
  <section class="cloud-portal" :class="[`cloud-portal--${variant}`, { expanded: portalOpen }]">
    <header class="cloud-portal-top">
      <div class="cloud-title">
        <span>{{ compact ? (isZh ? '素材' : 'Materials') : copy.eyebrow }}</span>
        <strong>{{ title }}</strong>
        <em>{{ subtitle }}</em>
      </div>
      <div class="cloud-actions">
        <button type="button" :disabled="loading" @click="loadCloudItems">
          {{ loading ? copy.syncing : copy.sync }}
        </button>
        <button type="button" class="primary" @click="portalOpen = !portalOpen">
          {{ portalOpen ? copy.close : copy.open }}
        </button>
      </div>
    </header>

    <Transition name="cloud-drop">
      <div v-if="portalOpen" class="cloud-workspace">
        <div class="cloud-quick-stats">
          <article v-for="item in quickStats" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.hint }}</em>
          </article>
        </div>

        <nav class="cloud-nav" :aria-label="copy.navLabel">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <span>{{ tab.label }}</span>
            <b>{{ countByTab(tab.key) }}</b>
          </button>
        </nav>

        <div class="cloud-toolbar">
          <div class="cloud-path">
            <button type="button" @click="selectFolder(null)">{{ copy.root }}</button>
            <template v-for="node in activeFolderTrail" :key="node.id">
              <i>/</i>
              <button type="button" @click="selectFolder(node.id)">{{ node.name }}</button>
            </template>
          </div>
          <label class="cloud-search">
            <span>{{ copy.search }}</span>
            <input v-model="query" type="search" :placeholder="copy.searchPlaceholder" />
          </label>
        </div>

        <div class="cloud-body">
          <aside class="cloud-folder-pane">
            <div class="pane-title">
              <span>{{ copy.folders }}</span>
              <b>{{ folders.length }}</b>
            </div>

            <button
              type="button"
              class="folder-row"
              :class="{ active: activeFolderId === null, dragover: dragOverFolderId === null }"
              @click="selectFolder(null)"
              @dragover.prevent="dragOverFolderId = null"
              @dragleave="dragOverFolderId = undefined"
              @drop.prevent="dropOnFolder(null)"
            >
              <span class="folder-mini"><i></i></span>
              <strong>{{ copy.root }}</strong>
              <b>{{ rootItemCount }}</b>
            </button>

            <div class="folder-tree">
              <button
                v-for="folder in visibleFolders"
                :key="folder.id"
                type="button"
                class="folder-row"
                :class="{ active: activeFolderId === folder.id, dragover: dragOverFolderId === folder.id }"
                :style="{ '--level': folder.level }"
                draggable="true"
                @click="selectFolder(folder.id)"
                @dragstart="startFolderDrag(folder)"
                @dragend="clearDrag"
                @dragover.prevent="dragOverFolderId = folder.id"
                @dragleave="dragOverFolderId = undefined"
                @drop.prevent="dropOnFolder(folder.id)"
              >
                <span class="folder-mini"><i></i></span>
                <strong>{{ folder.name }}</strong>
                <b>{{ countItemsInFolder(folder.id) }}</b>
              </button>
            </div>

            <div class="folder-tools">
              <input v-model="newFolderName" type="text" :placeholder="copy.newFolderPlaceholder" @keydown.enter="createFolder" />
              <button type="button" :disabled="folderBusy" @click="createFolder">{{ copy.createFolder }}</button>
            </div>

            <div v-if="activeFolder" class="folder-manage">
              <span>{{ copy.currentFolder }}</span>
              <strong>{{ activeFolder.name }}</strong>
              <div>
                <button type="button" @click="renameActiveFolder">{{ copy.rename }}</button>
                <button type="button" class="danger" @click="deleteActiveFolder">{{ copy.delete }}</button>
              </div>
            </div>
          </aside>

          <main class="cloud-showcase">
            <div
              class="upload-zone"
              :class="{ over: uploadDragOver }"
              @dragover.prevent="uploadDragOver = true"
              @dragleave="uploadDragOver = false"
              @drop.prevent="handleDropFiles"
            >
              <div>
                <span>{{ copy.uploadTitle }}</span>
                <strong>{{ activeFolder?.name || copy.root }}</strong>
                <em>{{ copy.uploadHint }}</em>
              </div>
              <input ref="fileInputRef" type="file" multiple @change="handleFileInput" />
              <button type="button" :disabled="uploading" @click="fileInputRef?.click()">
                {{ uploading ? copy.uploading : copy.upload }}
              </button>
            </div>

            <div class="cloud-table">
              <button
                v-for="(item, index) in filteredItems"
                :key="item.key"
                type="button"
                :draggable="canMoveItem(item)"
                class="table-row"
                :class="{ selected: activeItem?.key === item.key, readonly: isReadOnlyItem(item) }"
                @click="selectItem(item, index)"
                @dragstart="startItemDrag(item)"
                @dragend="clearDrag"
              >
                <span>{{ typeIcon(item) }}</span>
                <strong>{{ item.title }}</strong>
                <em>{{ item.typeLabel }}</em>
                <i>{{ item.format || item.status || copy.record }}</i>
                <small>{{ isReadOnlyItem(item) ? copy.readOnlyDetail : item.summary || item.meta || copy.savedItem }}</small>
              </button>
              <div v-if="!filteredItems.length" class="cloud-empty">{{ copy.empty }}</div>
            </div>
          </main>

          <aside class="cloud-detail">
            <div v-if="activeItem" class="detail-card">
              <span>{{ activeItem.typeLabel }}</span>
              <h3>{{ activeItem.title }}</h3>
              <p>{{ activeItem.summary || activeItem.meta || activeItem.content || copy.detailFallback }}</p>
              <div class="detail-meta">
                <b>{{ activeItem.format || activeItem.status || 'cloud' }}</b>
                <b>{{ folderName(activeItem.folderId) }}</b>
                <b v-if="isReadOnlyItem(activeItem)">{{ copy.readOnly }}</b>
              </div>
              <div class="detail-actions">
                <button v-if="selectable" type="button" class="primary" @click="emitSelect(activeItem)">
                  {{ selectLabel }}
                </button>
                <button v-if="activeItem.isWorkspaceItem" type="button" @click="downloadItem(activeItem)">{{ copy.download }}</button>
                <button v-if="activeItem.isWorkspaceItem && canMoveItem(activeItem)" type="button" @click="renameItem(activeItem)">{{ copy.rename }}</button>
                <button type="button" @click="copyItem(activeItem)">{{ copy.copySummary }}</button>
                <button v-if="activeItem.isWorkspaceItem && canMoveItem(activeItem)" type="button" class="danger" @click="deleteItem(activeItem)">{{ copy.delete }}</button>
              </div>
            </div>
            <div v-else class="detail-empty">{{ copy.detailEmpty }}</div>
          </aside>
        </div>
      </div>
    </Transition>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiApi } from '@/api'
import { hasStoredAuthToken } from '@/utils/authState'

type CloudItem = Record<string, any> & {
  key: string
  title: string
  type: string
  typeLabel: string
  group: string
  folderId: number | null
}

type CloudFolder = Record<string, any> & {
  id: number
  parentId: number | null
  name: string
  sortOrder?: number
  level?: number
}

const props = withDefaults(defineProps<{
  title?: string
  subtitle?: string
  compact?: boolean
  defaultOpen?: boolean
  selectable?: boolean
  selectLabel?: string
  variant?: 'workspace' | 'knowledge' | 'ai' | 'page'
}>(), {
  title: '统一管理保存的分析材料',
  subtitle: '模型、数据、矩阵视图和上传文件会在这里形成可继续调用的素材库。',
  compact: false,
  defaultOpen: false,
  selectable: false,
  selectLabel: '加入 AI 上下文',
  variant: 'workspace',
})

const emit = defineEmits<{
  select: [item: CloudItem]
}>()

const { locale } = useI18n()
const isZh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => {
  if (!isZh.value) {
    return {
      eyebrow: 'Material Library',
      sync: 'Refresh materials',
      syncing: 'Syncing',
      open: 'Open library',
      close: 'Collapse',
      navLabel: 'Material library navigation',
      root: 'Library root',
      search: 'Search',
      searchPlaceholder: 'Search name, type, summary...',
      list: 'List',
      folders: 'Folders',
      currentFolder: 'Current folder',
      newFolderPlaceholder: 'New folder name',
      createFolder: 'Create folder',
      rename: 'Rename',
      delete: 'Delete',
      uploadTitle: 'Upload to',
      uploadHint: 'Files are stored in the server database and isolated by account.',
      upload: 'Upload files',
      uploading: 'Uploading',
      select: 'Use',
      preview: 'Preview',
      savedItem: 'Saved material',
      record: 'Record',
      empty: 'No material in this view',
      detailFallback: 'This material can be used as AI context or analysis input.',
      detailEmpty: 'Select a material to view details',
      download: 'Download',
      copySummary: 'Copy summary',
      noLogin: 'Please sign in before using the material library',
      readOnly: 'Built-in read-only',
      readOnlyDetail: 'Built-in assets can be read by AI, but can only be managed in Admin.',
      statFolders: 'Folders',
      statFoldersHint: 'Personal organization',
      statFiles: 'Files',
      statFilesHint: 'Uploaded or synced',
      statAnalysis: 'Analysis',
      statAnalysisHint: 'Reusable results',
      statReadonly: 'Read-only',
      statReadonlyHint: 'Built-in resources',
    }
  }
  return {
    eyebrow: '素材库',
    sync: '刷新素材',
    syncing: '同步中',
    open: '打开素材库',
    close: '收起入口',
    navLabel: '素材库导航',
    root: '素材库根目录',
    search: '搜索',
    searchPlaceholder: '搜索名称、类型、摘要...',
    list: '列表',
    folders: '文件夹',
    currentFolder: '当前文件夹',
    newFolderPlaceholder: '新文件夹名称',
    createFolder: '新建文件夹',
    rename: '重命名',
    delete: '删除',
    uploadTitle: '上传到',
    uploadHint: '文件进入服务器数据库，并按当前账号隔离。',
    upload: '上传文件',
    uploading: '上传中',
    select: '使用',
    preview: '预览',
    savedItem: '素材库保存项',
    record: '记录',
    empty: '当前视图暂无素材',
    detailFallback: '这条记录可以继续作为 AI 上下文或分析材料使用。',
    detailEmpty: '选择一个素材查看详情',
    download: '下载',
    copySummary: '复制摘要',
    noLogin: '请先登录后再使用素材库',
    readOnly: '内置只读',
    readOnlyDetail: '内置资产可被 AI 读取，但只能在管理后台维护。',
    statFolders: '文件夹',
    statFoldersHint: '个人整理结构',
    statFiles: '文件素材',
    statFilesHint: '上传或同步入库',
    statAnalysis: '分析结果',
    statAnalysisHint: '可复用输出',
    statReadonly: '只读资源',
    statReadonlyHint: '内置模型/资产',
  }
})

const portalOpen = ref(props.defaultOpen)
const loading = ref(false)
const uploading = ref(false)
const folderBusy = ref(false)
const query = ref('')
const activeTab = ref('all')
const activeFolderId = ref<number | null>(null)
const folders = ref<CloudFolder[]>([])
const cloudItems = ref<CloudItem[]>([])
const activeItem = ref<CloudItem | null>(null)
const selectedIndex = ref(0)
const newFolderName = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const uploadDragOver = ref(false)
const dragOverFolderId = ref<number | null | undefined>(undefined)
const draggingItem = ref<CloudItem | null>(null)
const draggingFolder = ref<CloudFolder | null>(null)

const tabs = computed(() => [
  { key: 'all', label: isZh.value ? '全部' : 'All' },
  { key: 'file', label: isZh.value ? '文件' : 'Files' },
  { key: 'analysis', label: isZh.value ? '分析' : 'Analysis' },
  { key: 'model', label: isZh.value ? '模型' : 'Models' },
  { key: 'dataset', label: isZh.value ? '数据' : 'Data' },
])

const quickStats = computed(() => [
  { label: copy.value.statFolders, value: folders.value.length, hint: copy.value.statFoldersHint },
  { label: copy.value.statFiles, value: cloudItems.value.filter((item) => ['file', 'image'].includes(item.group)).length, hint: copy.value.statFilesHint },
  { label: copy.value.statAnalysis, value: cloudItems.value.filter((item) => item.group === 'analysis').length, hint: copy.value.statAnalysisHint },
  { label: copy.value.statReadonly, value: cloudItems.value.filter(isReadOnlyItem).length, hint: copy.value.statReadonlyHint },
])

const activeFolder = computed(() => folders.value.find((folder) => folder.id === activeFolderId.value) || null)
const rootItemCount = computed(() => cloudItems.value.filter((item) => item.folderId === null).length)
const visibleFolders = computed(() => flattenFolders(null, 0))
const activeFolderTrail = computed(() => {
  const trail: CloudFolder[] = []
  let current = activeFolder.value
  const guard = new Set<number>()
  while (current && !guard.has(current.id)) {
    trail.unshift(current)
    guard.add(current.id)
    current = current.parentId == null ? null : folders.value.find((folder) => folder.id === current?.parentId) || null
  }
  return trail
})

const filteredItems = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return cloudItems.value.filter((item) => {
    const tabMatched = activeTab.value === 'all' || item.group === activeTab.value
    const folderMatched = activeFolderId.value === null ? item.folderId === null : item.folderId === activeFolderId.value
    const text = `${item.title} ${item.typeLabel} ${item.summary || ''} ${item.meta || ''}`.toLowerCase()
    return tabMatched && folderMatched && (!keyword || text.includes(keyword))
  })
})

watch(filteredItems, (items) => {
  selectedIndex.value = Math.min(selectedIndex.value, Math.max(items.length - 1, 0))
  activeItem.value = items[selectedIndex.value] || null
})

onMounted(() => {
  if (props.defaultOpen) void loadCloudItems()
})

watch(portalOpen, (open) => {
  if (open && !cloudItems.value.length && !loading.value) void loadCloudItems()
})

async function loadCloudItems() {
  if (!hasStoredAuthToken()) {
    cloudItems.value = []
    folders.value = []
    return
  }
  loading.value = true
  try {
    const [treeResponse, resourcesResponse] = await Promise.all([
      aiApi.getWorkspaceTree(),
      aiApi.listWorkspaceResources(),
    ])
    folders.value = normalizeFolders(treeResponse.data.data?.folders || [])
    const saved = normalizeRecords(treeResponse.data.data?.items || [])
    const resources = normalizeRecords(resourcesResponse.data.data || [])
    const merged = new Map<string, CloudItem>()
    ;[...saved, ...resources].forEach((item) => merged.set(item.key, item))
    cloudItems.value = [...merged.values()]
    activeItem.value = filteredItems.value[0] || cloudItems.value[0] || null
  } catch (error: any) {
    cloudItems.value = []
    folders.value = []
    ElMessage.error(error?.response?.data?.message || (isZh.value ? '素材库读取失败' : 'Failed to load materials'))
  } finally {
    loading.value = false
  }
}

function normalizeFolders(records: any[]): CloudFolder[] {
  return records.map((raw) => ({
    ...raw,
    id: Number(raw.id),
    parentId: raw.parentId == null ? null : Number(raw.parentId),
    name: String(raw.name || (isZh.value ? '未命名文件夹' : 'Untitled folder')),
    sortOrder: Number(raw.sortOrder || 0),
  }))
}

function normalizeRecords(records: any[]): CloudItem[] {
  return records.map((raw, index) => {
    const isWorkspaceItem = raw.itemType != null
    const type = String(raw.itemType || raw.type || raw.sourceType || 'resource')
    const title = String(raw.title || raw.fileName || raw.name || `${isZh.value ? '素材' : 'Material'} ${index + 1}`)
    const format = String(raw.format || raw.mimeType || raw.status || '')
    const folderId = raw.folderId == null ? null : Number(raw.folderId)
    const official = Boolean(raw.official || raw.isOfficial || raw.payload?.official)
    const readOnly = official || Boolean(raw.readOnly || raw.payload?.readOnly)
    return {
      ...raw,
      key: `${type}:${raw.id || raw.fileUrl || title}`,
      id: raw.id == null ? raw.id : Number(raw.id),
      folderId,
      isWorkspaceItem,
      title,
      type,
      typeLabel: typeLabel(type),
      group: groupOf(type),
      format,
      official,
      readOnly,
      canManage: raw.canManage !== false && !readOnly,
      canSync: raw.canSync !== false && !readOnly,
    }
  })
}

function flattenFolders(parentId: number | null, level: number): CloudFolder[] {
  return folders.value
    .filter((folder) => folder.parentId === parentId)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0) || a.name.localeCompare(b.name))
    .flatMap((folder) => [{ ...folder, level }, ...flattenFolders(folder.id, level + 1)])
}

function selectFolder(folderId: number | null) {
  activeFolderId.value = folderId
  activeItem.value = filteredItems.value[0] || null
}

async function createFolder() {
  const name = newFolderName.value.trim()
  if (!name) return
  folderBusy.value = true
  try {
    await aiApi.createWorkspaceFolder({ name, parentId: activeFolderId.value })
    newFolderName.value = ''
    await loadCloudItems()
    ElMessage.success(isZh.value ? '文件夹已创建' : 'Folder created')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || (isZh.value ? '创建文件夹失败' : 'Failed to create folder'))
  } finally {
    folderBusy.value = false
  }
}

async function renameActiveFolder() {
  if (!activeFolder.value) return
  const result = await ElMessageBox.prompt(isZh.value ? '输入新的文件夹名称' : 'Enter a new folder name', copy.value.rename, {
    inputValue: activeFolder.value.name,
    confirmButtonText: isZh.value ? '保存' : 'Save',
    cancelButtonText: isZh.value ? '取消' : 'Cancel',
  }).catch(() => null)
  if (!result) return
  await aiApi.renameWorkspaceFolder(activeFolder.value.id, result.value)
  await loadCloudItems()
  ElMessage.success(isZh.value ? '文件夹已重命名' : 'Folder renamed')
}

async function deleteActiveFolder() {
  if (!activeFolder.value) return
  await ElMessageBox.confirm(
    isZh.value ? '删除文件夹后，其中素材会回到全部文件。确认继续？' : 'Items in this folder will move back to all files. Continue?',
    copy.value.delete,
    { type: 'warning', confirmButtonText: copy.value.delete, cancelButtonText: isZh.value ? '取消' : 'Cancel' },
  )
  await aiApi.deleteWorkspaceFolder(activeFolder.value.id)
  activeFolderId.value = null
  await loadCloudItems()
  ElMessage.success(isZh.value ? '文件夹已删除' : 'Folder deleted')
}

async function handleFileInput(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files || [])
  await uploadFiles(files)
  if (fileInputRef.value) fileInputRef.value.value = ''
}

async function handleDropFiles(event: DragEvent) {
  uploadDragOver.value = false
  const files = Array.from(event.dataTransfer?.files || [])
  await uploadFiles(files)
}

async function uploadFiles(files: File[]) {
  if (!files.length) return
  if (!hasStoredAuthToken()) {
    ElMessage.warning(copy.value.noLogin)
    return
  }
  uploading.value = true
  try {
    await aiApi.uploadWorkspaceFiles(files, activeFolderId.value)
    await loadCloudItems()
    ElMessage.success(isZh.value ? `已上传 ${files.length} 个文件` : `${files.length} file(s) uploaded`)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || (isZh.value ? '上传失败' : 'Upload failed'))
  } finally {
    uploading.value = false
  }
}

function startItemDrag(item: CloudItem) {
  if (!canMoveItem(item)) {
    draggingItem.value = null
    draggingFolder.value = null
    ElMessage.info(copy.value.readOnlyDetail)
    return
  }
  draggingItem.value = item
  draggingFolder.value = null
}

function startFolderDrag(folder: CloudFolder) {
  if (!folder?.id) return
  draggingFolder.value = folder
  draggingItem.value = null
}

async function dropOnFolder(folderId: number | null) {
  try {
    if (draggingItem.value?.id) {
      if (!canMoveItem(draggingItem.value)) {
        ElMessage.info(copy.value.readOnlyDetail)
        return
      }
      await aiApi.moveWorkspaceItem(draggingItem.value.id, { folderId })
      ElMessage.success(isZh.value ? '素材已移动' : 'Item moved')
    } else if (draggingFolder.value?.id) {
      if (draggingFolder.value.id === folderId) {
        clearDrag()
        return
      }
      await aiApi.moveWorkspaceFolder(draggingFolder.value.id, { parentId: folderId })
      ElMessage.success(isZh.value ? '文件夹已移动' : 'Folder moved')
    }
    await loadCloudItems()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || (isZh.value ? '移动失败' : 'Move failed'))
  } finally {
    clearDrag()
  }
}

function clearDrag() {
  draggingItem.value = null
  draggingFolder.value = null
  dragOverFolderId.value = undefined
}

function isReadOnlyItem(item: CloudItem | null | undefined) {
  return Boolean(item?.official || item?.readOnly || item?.canManage === false)
}

function canMoveItem(item: CloudItem | null | undefined) {
  return Boolean(item?.isWorkspaceItem && item?.id && !isReadOnlyItem(item))
}

function selectItem(item: CloudItem, index: number) {
  activeItem.value = item
  selectedIndex.value = index
}

function emitSelect(item: CloudItem) {
  if (!props.selectable) {
    activeItem.value = item
    return
  }
  emit('select', item)
  ElMessage.success(isZh.value ? '已加入 AI 分析上下文' : 'Added to AI context')
}

async function renameItem(item: CloudItem) {
  if (!canMoveItem(item)) {
    ElMessage.info(copy.value.readOnlyDetail)
    return
  }
  if (!item.id) return
  const result = await ElMessageBox.prompt(isZh.value ? '输入新的素材名称' : 'Enter a new item name', copy.value.rename, {
    inputValue: item.title,
    confirmButtonText: isZh.value ? '保存' : 'Save',
    cancelButtonText: isZh.value ? '取消' : 'Cancel',
  }).catch(() => null)
  if (!result) return
  await aiApi.renameWorkspaceItem(item.id, result.value)
  await loadCloudItems()
  ElMessage.success(isZh.value ? '素材已重命名' : 'Item renamed')
}

async function deleteItem(item: CloudItem) {
  if (!canMoveItem(item)) {
    ElMessage.info(copy.value.readOnlyDetail)
    return
  }
  if (!item.id) return
  await ElMessageBox.confirm(
    isZh.value ? '确认删除这条素材？' : 'Delete this material?',
    copy.value.delete,
    { type: 'warning', confirmButtonText: copy.value.delete, cancelButtonText: isZh.value ? '取消' : 'Cancel' },
  )
  await aiApi.deleteWorkspaceItem(item.id)
  await loadCloudItems()
  ElMessage.success(isZh.value ? '素材已删除' : 'Item deleted')
}

async function downloadItem(item: CloudItem) {
  if (!item.id) return
  try {
    const response = await aiApi.downloadWorkspaceItem(item.id)
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = item.fileName || item.title || 'material-item'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || (isZh.value ? '下载失败' : 'Download failed'))
  }
}

function copyItem(item: CloudItem) {
  navigator.clipboard?.writeText(`${item.title}\n${item.summary || item.meta || item.content || ''}`)
  ElMessage.success(isZh.value ? '已复制摘要' : 'Summary copied')
}

function groupOf(type: string) {
  if (['image'].includes(type)) return 'file'
  if (['file', 'text', 'context_bundle', 'data_asset', 'cloud_item'].includes(type)) return 'file'
  if (['analysis_result', 'saved_view', 'visual_view'].includes(type)) return 'analysis'
  if (['model'].includes(type)) return 'model'
  if (['dataset', 'training', 'run'].includes(type)) return 'dataset'
  return 'analysis'
}

function typeLabel(type: string) {
  const zh: Record<string, string> = {
    image: '图片',
    file: '文件',
    text: '文本',
    context_bundle: '素材包',
    data_asset: '数据资产',
    analysis_result: '矩阵记录',
    saved_view: '保存视图',
    visual_view: '可视化视图',
    model: '模型',
    dataset: '数据集',
    training: '训练任务',
    run: '运行日志',
  }
  const en: Record<string, string> = {
    image: 'Image',
    file: 'File',
    text: 'Text',
    context_bundle: 'Bundle',
    data_asset: 'Data asset',
    analysis_result: 'Matrix record',
    saved_view: 'Saved view',
    visual_view: 'Visual view',
    model: 'Model',
    dataset: 'Dataset',
    training: 'Training',
    run: 'Run',
  }
  return (isZh.value ? zh : en)[type] || (isZh.value ? '资源' : 'Resource')
}

function typeIcon(item: CloudItem) {
  const zhIcons: Record<string, string> = {
    image: '图',
    file: '文',
    text: '文',
    context_bundle: '包',
    data_asset: '数据',
    analysis_result: '矩阵',
    saved_view: '视图',
    visual_view: '视图',
    model: '模型',
    dataset: '数据',
    training: '训练',
    run: '日志',
  }
  const enIcons: Record<string, string> = {
    image: 'IMG',
    file: 'DOC',
    text: 'TXT',
    context_bundle: 'PKG',
    data_asset: 'DATA',
    analysis_result: 'MAT',
    saved_view: 'VIEW',
    visual_view: 'VIEW',
    model: 'MOD',
    dataset: 'DATA',
    training: 'RUN',
    run: 'LOG',
  }
  return (isZh.value ? zhIcons : enIcons)[item.type] || (isZh.value ? '云' : 'CLD')
}

function countByTab(key: string) {
  if (key === 'all') return cloudItems.value.length
  return cloudItems.value.filter((item) => item.group === key).length
}

function countItemsInFolder(folderId: number) {
  return cloudItems.value.filter((item) => item.folderId === folderId).length
}

function folderName(folderId: number | null | undefined) {
  if (folderId == null) return copy.value.root
  return folders.value.find((folder) => folder.id === folderId)?.name || copy.value.root
}

</script>

<style scoped>
.cloud-portal {
  position: relative;
  z-index: 8;
  margin: 0 0 18px;
  --cloud-glass-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018)),
    color-mix(in srgb, rgba(var(--glass-bg-rgb), var(--glass-opacity)) 78%, transparent);
  --cloud-card-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), transparent),
    color-mix(in srgb, var(--panel-bg) 84%, transparent);
  border: 1px solid color-mix(in srgb, var(--primary-color) 20%, var(--border-color));
  border-radius: 8px;
  background: var(--cloud-glass-bg);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  overflow: hidden;
}

.cloud-portal--knowledge {
  width: min(1180px, calc(100% - 32px));
  margin: -54px auto 34px;
}

.cloud-portal--ai {
  margin: 0 0 12px;
}

.cloud-portal--page {
  margin: 0;
  border-radius: 8px;
  background: var(--cloud-glass-bg);
  overflow: visible;
}

.cloud-portal-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
}

.cloud-portal--page .cloud-portal-top {
  padding: 18px 22px 16px;
}

.cloud-title {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.cloud-title span {
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.cloud-title strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-size: clamp(18px, 2vw, 26px);
  font-weight: var(--font-weight-title);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cloud-portal--page .cloud-title strong {
  font-size: clamp(24px, 3vw, 44px);
  letter-spacing: -0.055em;
}

.cloud-title em {
  display: -webkit-box;
  max-width: 780px;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  line-height: 1.6;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.cloud-actions,
.detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.cloud-actions button,
.detail-actions button,
.folder-tools button,
.folder-manage button,
.upload-zone button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  min-height: 34px;
  min-width: 0;
  padding: 0 13px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--workbench-control-bg);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.cloud-actions button:hover,
.detail-actions button:hover,
.folder-tools button:hover,
.folder-manage button:hover,
.upload-zone button:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--primary-color) 42%, var(--border-color));
  background: rgba(var(--primary-rgb), 0.09);
}

.cloud-actions .primary,
.detail-actions .primary {
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.detail-actions .danger,
.folder-manage .danger {
  color: #fb7185;
  border-color: rgba(251, 113, 133, 0.28);
}

.cloud-workspace {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
  padding: 0 16px 16px;
  overflow: hidden;
  overscroll-behavior: contain;
}

.cloud-portal--page .cloud-workspace {
  height: auto;
  min-height: 0;
  padding: 0 22px 22px;
  overflow: visible;
  overscroll-behavior: auto;
}

.cloud-quick-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.cloud-quick-stats article {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: color-mix(in srgb, var(--surface-2) 68%, transparent);
}

.cloud-quick-stats span,
.cloud-quick-stats strong,
.cloud-quick-stats em {
  display: block;
}

.cloud-quick-stats span {
  color: var(--primary-color);
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.cloud-quick-stats strong {
  margin: 6px 0 4px;
  color: var(--text-primary);
  font-size: 24px;
}

.cloud-quick-stats em {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cloud-nav,
.cloud-toolbar,
.cloud-body {
  border: 1px solid var(--border-color);
  background: var(--cloud-card-bg);
}

.cloud-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
}

.cloud-nav button {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 32px;
  padding: 0 11px;
  border-radius: 999px;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.cloud-nav button span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cloud-nav button.active {
  background: rgba(var(--primary-rgb), 0.14);
  color: var(--primary-color);
  box-shadow: inset 0 0 0 1px rgba(var(--primary-rgb), 0.22);
}

.cloud-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 420px);
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
}

.cloud-path {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-title);
  max-height: 58px;
  overflow-x: hidden;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.cloud-path button {
  flex: 0 0 auto;
  max-width: 180px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: inherit;
  font-weight: inherit;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cloud-search {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 8px;
  height: 36px;
  border: 1px solid rgba(var(--primary-rgb), 0.16);
  border-radius: 999px;
  background: var(--workbench-control-bg);
  color: var(--text-muted);
  padding: 0 12px;
}

.cloud-search span {
  font-size: 10px;
  font-weight: var(--font-weight-title);
  white-space: nowrap;
}

.cloud-search input {
  min-width: 0;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  outline: none;
}

.cloud-body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 280px;
  gap: 12px;
  min-height: 0;
  height: min(760px, calc(100dvh - 220px));
  min-height: 520px;
  padding: 12px;
  border-radius: 8px;
  overflow: hidden;
}

.cloud-portal--page .cloud-body {
  height: auto;
  min-height: 0;
  grid-template-columns: 300px minmax(0, 1fr) 320px;
  align-items: start;
  overflow: visible;
  overscroll-behavior: auto;
}

.cloud-folder-pane,
.cloud-showcase,
.cloud-detail {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--cloud-card-bg);
}

.cloud-folder-pane {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto auto;
  overflow: hidden;
  min-height: 0;
  padding: 10px;
}

.pane-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 2px 10px;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.pane-title b {
  color: var(--primary-color);
}

.folder-tree {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scroll-behavior: smooth;
  padding: 6px 0;
}

.folder-row {
  width: 100%;
  display: grid;
  grid-template-columns: 30px 1fr auto;
  gap: 8px;
  align-items: center;
  min-height: 40px;
  margin-bottom: 6px;
  padding: 7px 8px 7px calc(8px + var(--level, 0) * 16px);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--cloud-card-bg);
  color: var(--text-secondary);
  text-align: left;
  transition:
    transform var(--motion-hover) var(--ease-liquid),
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.folder-row:hover,
.folder-row.active,
.folder-row.dragover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.38);
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
}

.folder-row strong {
  overflow: hidden;
  color: inherit;
  font-size: 12px;
  font-weight: var(--font-weight-title);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-row b {
  color: inherit;
  font-size: 10px;
}

.folder-mini {
  position: relative;
  width: 28px;
  height: 22px;
  border-radius: 3px 6px 6px 6px;
  background: color-mix(in srgb, var(--primary-color) 82%, #0f172a);
}

.folder-mini::before {
  content: "";
  position: absolute;
  left: 0;
  bottom: 94%;
  width: 11px;
  height: 5px;
  border-radius: 4px 4px 0 0;
  background: inherit;
}

.folder-mini i {
  position: absolute;
  left: 7px;
  right: 5px;
  bottom: 5px;
  height: 12px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.78);
}

.folder-tools {
  display: grid;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
}

.folder-tools input {
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  background: var(--workbench-control-bg);
  color: var(--text-primary);
  padding: 0 11px;
  outline: none;
}

.folder-manage {
  display: grid;
  gap: 6px;
  margin-top: 10px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.11);
  border-radius: 15px;
  background: var(--workbench-soft-bg);
}

.folder-manage span {
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.folder-manage strong {
  color: var(--text-primary);
  font-size: 13px;
}

.folder-manage div {
  display: flex;
  gap: 6px;
}

.cloud-showcase {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  padding: 8px;
}

.upload-zone {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding: 12px;
  border: 1px dashed rgba(255, 255, 255, 0.16);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.018) 42%, rgba(0, 0, 0, 0.08)),
    var(--cloud-card-bg);
}

.upload-zone.over {
  border-color: rgba(66, 230, 164, 0.7);
  background: rgba(66, 230, 164, 0.08);
}

.upload-zone span {
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.upload-zone strong {
  display: block;
  margin: 2px 0;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: var(--font-weight-title);
}

.upload-zone em {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
}

.upload-zone input {
  display: none;
}

.cloud-table {
  display: grid;
  gap: 8px;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scroll-behavior: smooth;
  padding-right: 4px;
}

.table-row {
  display: grid;
  grid-template-columns: 48px minmax(160px, 1fr) 82px 80px minmax(160px, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 15px;
  background: var(--cloud-card-bg);
  color: var(--text-secondary);
  text-align: left;
  transition:
    transform var(--motion-hover) var(--ease-liquid),
    border-color var(--motion-hover) var(--ease-liquid),
    background var(--motion-medium) var(--ease-smooth),
    box-shadow var(--motion-medium) var(--ease-smooth);
}

.table-row.selected,
.table-row:hover {
  border-color: rgba(var(--primary-rgb), 0.36);
  background: rgba(var(--primary-rgb), 0.1);
}

.table-row.readonly {
  border-color: rgba(251, 191, 36, 0.24);
  cursor: default;
}

.table-row span {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: rgba(var(--primary-rgb), 0.12);
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.table-row strong,
.table-row small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-row em,
.table-row i {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-row strong {
  color: var(--text-primary);
}

.table-row em,
.table-row i,
.table-row small {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
}

.cloud-detail {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scroll-behavior: smooth;
  padding: 10px;
}

.detail-card {
  display: grid;
  gap: 10px;
}

.detail-card > span {
  color: var(--primary-color);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.detail-card h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: var(--font-weight-title);
  line-height: 1.25;
}

.detail-card p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.65;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-meta b {
  padding: 5px 8px;
  border-radius: 999px;
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-size: 10px;
}

.detail-empty,
.cloud-empty {
  min-height: 180px;
  display: grid;
  place-items: center;
  padding: 16px;
  color: var(--text-muted);
  font-size: 12px;
  text-align: center;
}

.cloud-drop-enter-active,
.cloud-drop-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}

.cloud-drop-enter-from,
.cloud-drop-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@media (hover: hover) and (pointer: fine) {
  .cloud-portal:hover,
  .cloud-folder-pane:hover,
  .cloud-showcase:hover,
  .cloud-detail:hover {
    border-color: color-mix(in srgb, var(--primary-color) 32%, var(--border-color));
  }

  .folder-row:hover,
  .table-row:hover {
    box-shadow: var(--shadow-ring);
  }
}

@media (prefers-reduced-motion: reduce) {
  .cloud-actions button,
  .detail-actions button,
  .folder-tools button,
  .folder-manage button,
  .upload-zone button,
  .folder-row,
  .table-row,
  .cloud-drop-enter-active,
  .cloud-drop-leave-active {
    transition: none;
    transform: none !important;
  }
}

@media (max-width: 1180px) {
  .cloud-quick-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .cloud-body,
  .cloud-portal--page .cloud-body {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .cloud-detail {
    grid-column: 1 / -1;
    max-height: 320px;
  }
}

@media (max-width: 860px) {
  .cloud-quick-stats {
    grid-template-columns: 1fr;
  }

  .cloud-portal-top,
  .cloud-toolbar,
  .upload-zone {
    grid-template-columns: 1fr;
    display: grid;
  }

  .cloud-body {
    grid-template-columns: 1fr;
    height: auto;
    overflow: visible;
  }

  .table-row {
    grid-template-columns: 40px 1fr;
  }

  .table-row em,
  .table-row i,
  .table-row small {
    grid-column: 2;
  }
}

.cloud-portal--page .cloud-folder-pane,
.cloud-portal--page .cloud-showcase,
.cloud-portal--page .cloud-detail {
  overflow: visible;
}

.cloud-portal--page .cloud-folder-pane {
  grid-template-rows: auto auto auto auto auto;
}

.cloud-portal--page .cloud-showcase {
  grid-template-rows: auto auto;
}

.cloud-portal--page .folder-tree,
.cloud-portal--page .cloud-table,
.cloud-portal--page .cloud-detail {
  max-height: none;
  overflow: visible;
  overscroll-behavior: auto;
}

</style>
