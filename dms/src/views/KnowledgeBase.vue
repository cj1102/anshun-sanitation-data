<template>
  <div class="space-y-6 relative z-10">
    <section>
      <h1 class="text-xl font-bold text-white">AI 知识库</h1>
      <p class="mt-1 text-sm text-slate-400">上传业务制度、操作手册或常见问题。AI 对话时只会检索当前用户有权限查看的资料。</p>
    </section>

    <el-card class="dark-card" shadow="never">
      <template #header><span class="font-semibold text-slate-200">上传知识文档</span></template>
      <el-form label-position="top" class="grid grid-cols-1 gap-x-5 md:grid-cols-2">
        <el-form-item label="文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.txt,.md,application/pdf,text/plain,text/markdown"
            :on-change="onFileChange"
            :on-remove="clearFile"
            :on-exceed="replaceFile"
          >
            <el-button type="primary" plain>选择 PDF / TXT / MD</el-button>
            <template #tip><div class="mt-1 text-xs text-slate-500">最大 20MB；扫描版 PDF 请先 OCR，系统需要可提取的文本。</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档标题（可选）">
          <el-input v-model="title" maxlength="150" show-word-limit placeholder="默认使用文件名" />
        </el-form-item>
        <el-form-item label="资料可见范围" class="md:col-span-2">
          <el-radio-group v-model="visibilityMode">
            <el-radio value="all">全部已登录角色</el-radio>
            <el-radio value="roles">仅指定角色</el-radio>
          </el-radio-group>
          <el-checkbox-group v-if="visibilityMode === 'roles'" v-model="visibleRoles" class="mt-3 flex flex-wrap gap-x-5 gap-y-2">
            <el-checkbox v-for="role in roles" :key="role.code" :value="role.code">{{ role.name }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <div class="flex flex-wrap justify-end gap-3">
        <el-button :loading="reindexing" @click="reindex">重建向量索引</el-button>
        <el-button type="primary" :loading="uploading" @click="upload">上传并建立索引</el-button>
      </div>
    </el-card>

    <el-card class="dark-card" shadow="never">
      <template #header>
        <div class="flex items-center justify-between"><span class="font-semibold text-slate-200">已上传文档</span><span class="text-xs text-slate-500">{{ documents.length }} 份</span></div>
      </template>
      <el-table :data="documents" v-loading="loading" class="dark-table" empty-text="尚未上传知识文档">
        <el-table-column prop="title" label="文档标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="originalFilename" label="原始文件" min-width="180" show-overflow-tooltip />
        <el-table-column label="可见范围" min-width="165">
          <template #default="{ row }"><el-tag effect="plain">{{ displayRoles(row.visibleRoles) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="文本分段" width="105" align="center" />
        <el-table-column label="文件大小" width="110"><template #default="{ row }">{{ formatSize(row.fileSize) }}</template></el-table-column>
        <el-table-column prop="uploaderUsername" label="上传人" width="120" />
        <el-table-column label="上传时间" min-width="170"><template #default="{ row }">{{ formatDate(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="85" fixed="right"><template #default="{ row }"><el-button type="danger" link @click="removeDocument(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { UploadFile, UploadInstance, UploadRawFile } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../utils/axios'

type KnowledgeDocument = {
  documentId: number
  title: string
  originalFilename: string
  visibleRoles: string
  fileSize: number
  uploaderUsername: string
  chunkCount: number
  createTime: string
}

const roles = [
  { code: 'ADMIN', name: '系统管理员' }, { code: 'OPERATOR', name: '运营专员' },
  { code: 'FINANCE', name: '财务人员' }, { code: 'AUDITOR', name: '审核员' }, { code: 'VIEWER', name: '只读访客' }
]
const documents = ref<KnowledgeDocument[]>([])
const loading = ref(false), uploading = ref(false), reindexing = ref(false), title = ref('')
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<UploadRawFile | null>(null)
const visibilityMode = ref<'all' | 'roles'>('all')
const visibleRoles = ref<string[]>([])
const visibleRoleValue = computed(() => visibilityMode.value === 'all' ? 'ALL' : visibleRoles.value.join(','))

const loadDocuments = async () => {
  loading.value = true
  try { documents.value = (await axios.get('/ai/knowledge/documents')).data }
  catch (error: any) { ElMessage.error(error.error || '知识文档加载失败') }
  finally { loading.value = false }
}
const onFileChange = (file: UploadFile) => { selectedFile.value = file.raw || null }
const clearFile = () => { selectedFile.value = null }
const replaceFile = (_files: File[], uploadFiles: UploadFile[]) => {
  uploadFiles.splice(0, uploadFiles.length - 1)
  ElMessage.warning('一次只能上传一份文档，请先移除当前文件')
}
const upload = async () => {
  if (!selectedFile.value) return ElMessage.warning('请选择要上传的 PDF、TXT 或 MD 文件')
  if (visibilityMode.value === 'roles' && visibleRoles.value.length === 0) return ElMessage.warning('请至少选择一个可见角色')
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  if (title.value.trim()) formData.append('title', title.value.trim())
  formData.append('visibleRoles', visibleRoleValue.value)
  uploading.value = true
  try {
    await axios.post('/ai/knowledge/documents', formData)
    ElMessage.success('文档已解析并建立知识索引')
    uploadRef.value?.clearFiles()
    selectedFile.value = null; title.value = ''; visibilityMode.value = 'all'; visibleRoles.value = []
    await loadDocuments()
  } catch (error: any) { ElMessage.error(error.error || '文档上传失败') }
  finally { uploading.value = false }
}
const reindex = async () => {
  try {
    await ElMessageBox.confirm('将从 MySQL 的知识库分段重新构建向量索引；在索引期间，AI 仍可使用关键词检索。', '重建向量索引', { type: 'info' })
    reindexing.value = true
    const result = (await axios.post('/ai/knowledge/documents/reindex')).data
    if (!result.vectorStoreEnabled) ElMessage.warning('当前未启用向量库，仍可继续使用关键词检索')
    else ElMessage.success(`向量索引已重建：${result.documentCount} 份文档，${result.chunkCount} 个分段`)
  } catch (error: any) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.error || '重建向量索引失败') }
  finally { reindexing.value = false }
}
const removeDocument = async (document: KnowledgeDocument) => {
  try {
    await ElMessageBox.confirm(`删除“${document.title}”后，AI 将不再检索该资料。`, '确认删除', { type: 'warning' })
    await axios.delete(`/ai/knowledge/documents/${document.documentId}`)
    ElMessage.success('知识文档已删除')
    await loadDocuments()
  } catch (error: any) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.error || '删除失败') }
}
const displayRoles = (value: string) => value === 'ALL' ? '全部角色' : value.split(',').map(code => roles.find(role => role.code === code)?.name || code).join('、')
const formatSize = (value: number) => value < 1024 * 1024 ? `${Math.max(1, Math.round(value / 1024))} KB` : `${(value / 1024 / 1024).toFixed(2)} MB`
const formatDate = (value: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'

onMounted(loadDocuments)
</script>

<style scoped>
:deep(.dark-card) { background: rgba(15, 23, 42, .72); border-color: rgba(99, 102, 241, .2); color: #e2e8f0; }
:deep(.dark-table), :deep(.dark-table tr), :deep(.dark-table th.el-table__cell), :deep(.dark-table td.el-table__cell) { background: transparent; color: #cbd5e1; border-color: rgba(99, 102, 241, .14); }
:deep(.dark-table .el-table__inner-wrapper::before) { background-color: rgba(99, 102, 241, .14); }
</style>
