<template>
  <div class="space-y-6">
    <!-- Filters Header Card -->
    <div class="p-6 rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl flex flex-wrap gap-4 items-center justify-between">
      <div class="flex flex-wrap gap-3 items-center">
        <!-- Search bar -->
        <el-input 
          v-model="searchQuery" 
          placeholder="搜索合同号、点位编号、设立位置..." 
          prefix-icon="Search"
          class="w-72 custom-input"
          clearable
          @input="handleFilter"
        />

        <!-- Lessee Company Search -->
        <el-input 
          v-model="filterLessee" 
          placeholder="搜索承租单位..." 
          prefix-icon="Avatar"
          class="w-56 custom-input"
          clearable
          @input="handleFilter"
        />

        <el-select v-model="filterApprovalStatus" placeholder="全部审批状态" clearable class="w-36" @change="handleFilter">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </div>

      <div class="flex items-center gap-3">
        <button v-if="canCreate"
          @click="openCreateDialog" 
          class="px-4 py-2 bg-gradient-to-r from-emerald-500 to-teal-500 text-white rounded-xl font-medium text-xs hover:from-emerald-400 hover:to-teal-400 active:scale-95 transition-all shadow-md shadow-teal-500/10 flex items-center gap-1.5 cursor-pointer border-0"
        >
          <el-icon><Plus /></el-icon> 录入新合同
        </button>
        <button 
          @click="exportCSV" 
          class="px-4 py-2 bg-gradient-to-r from-cyan-500 to-indigo-500 text-white rounded-xl font-medium text-xs hover:from-cyan-400 hover:to-indigo-400 active:scale-95 transition-all shadow-md shadow-indigo-500/10 flex items-center gap-1.5 cursor-pointer border-0"
        >
          <el-icon><Download /></el-icon> 导出当前表格 (CSV)
        </button>
        <div class="text-xs text-slate-400">
          共计 <span class="text-cyan-400 font-bold font-mono">{{ total }}</span> 份承租合同
        </div>
      </div>
    </div>

    <!-- Data Table Card -->
    <div class="rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl overflow-hidden p-1">
      <el-table :data="tableData" v-loading="loading" class="w-full">
        <el-table-column prop="contract_code" label="合同编码" width="140">
          <template #default="scope">
            <span class="font-bold font-mono text-cyan-400">{{ scope.row.contract_code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ad_position_code" label="点位编码" width="120">
          <template #default="scope">
            <span class="font-bold font-mono text-slate-400">{{ scope.row.ad_position_code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lessee_company" label="承租人 / 承租单位" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ad_location" label="广告点位具体设立位置" min-width="200" show-overflow-tooltip />
        <el-table-column prop="lease_rent" label="分配租金" width="110" align="right">
          <template #default="scope">
            <span class="font-bold font-mono text-emerald-400">{{ scope.row.lease_rent }}</span> 万元
          </template>
        </el-table-column>
        <el-table-column prop="lease_term" label="周期(天)" width="90" align="center">
          <template #default="scope">
            <span class="font-mono text-slate-300">{{ scope.row.lease_term }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lease_start_date" label="租期开始时间" width="115" align="center">
          <template #default="scope">
            <span class="font-mono text-xs">{{ formatDate(scope.row.lease_start_date) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lease_end_date" label="租期结束时间" width="115" align="center">
          <template #default="scope">
            <span class="font-mono text-xs">{{ formatDate(scope.row.lease_end_date) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="118" align="center">
          <template #default="scope">
            <el-tooltip v-if="scope.row.approval_comment" :content="`审核意见：${scope.row.approval_comment}`" placement="top">
              <el-tag :type="approvalTagType(scope.row.approval_status)" effect="dark" size="small" class="cursor-help border-0">
                {{ approvalStatusLabel(scope.row.approval_status) }}
              </el-tag>
            </el-tooltip>
            <el-tag v-else :type="approvalTagType(scope.row.approval_status)" effect="dark" size="small" class="border-0">
              {{ approvalStatusLabel(scope.row.approval_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="112" align="center">
          <template #default="scope">
            <el-dropdown trigger="click">
              <button class="px-2.5 py-1 text-xs font-semibold text-cyan-400 hover:text-cyan-300 rounded-lg border border-cyan-500/20 hover:border-cyan-400/50 bg-cyan-500/5 transition-colors cursor-pointer">
                更多操作
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openAttachments(scope.row)">
                    附件{{ scope.row.attachment_count ? ` (${scope.row.attachment_count})` : '' }}
                  </el-dropdown-item>
                  <el-dropdown-item v-if="canEdit(scope.row)" @click="openEditDialog(scope.row)">编辑草稿</el-dropdown-item>
                  <el-dropdown-item v-if="canSubmit(scope.row)" @click="submitForApproval(scope.row)">提交审核</el-dropdown-item>
                  <el-dropdown-item v-if="canApprove(scope.row)" divided @click="reviewLease(scope.row, true)">审核通过</el-dropdown-item>
                  <el-dropdown-item v-if="canApprove(scope.row)" @click="reviewLease(scope.row, false)">驳回并退回</el-dropdown-item>
                  <el-dropdown-item v-if="canArchive(scope.row)" divided @click="handleDelete(scope.row)">归档合同</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="p-4 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- Record/Edit Lease Dialog -->
    <el-dialog
      v-model="dialogVisible"
      width="850px"
      align-center
      custom-class="custom-dialog"
    >
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-teal-500 to-emerald-600 flex items-center justify-center text-white shadow-lg shadow-teal-500/20">
            <el-icon size="18"><Document v-if="!isEdit" /><Edit v-else /></el-icon>
          </div>
          <div>
            <h4 class="text-base font-bold text-slate-100">{{ isEdit ? '编辑合同草稿' : '录入新合同草稿' }}</h4>
            <p class="text-xs text-slate-400 mt-0.5">{{ isEdit ? '可修改草稿或被驳回合同；保存后需重新提交审核' : '新合同会先保存为草稿，审核通过后才会计入出租状态与统计' }}</p>
          </div>
        </div>
      </template>

      <el-form :model="form" :rules="formRules" ref="formRef" label-position="top">
        <div class="grid grid-cols-12 gap-5">
          <!-- Left Column: Contract Meta & Associate (col-span-6) -->
          <div class="col-span-12 lg:col-span-6 space-y-4">
            <div class="p-4 rounded-xl bg-slate-950/30 border border-indigo-500/10 space-y-4 shadow-inner h-full">
              <h5 class="text-xs font-bold text-indigo-400 uppercase tracking-wider pb-2 border-b border-indigo-500/10 flex items-center gap-1.5">
                <el-icon><InfoFilled /></el-icon> 契约基础关联
              </h5>

              <el-form-item label="合同编码" prop="contract_code">
                <el-input v-model="form.contract_code" placeholder="如 CON-2026-0001" />
              </el-form-item>

              <el-form-item label="广告点位编码" prop="ad_position_code">
                <el-select 
                  v-model="form.ad_position_code" 
                  placeholder="请选择或搜索点位" 
                  filterable
                  class="w-full"
                >
                  <el-option 
                    v-for="code in positionCodes" 
                    :key="code" 
                    :label="code" 
                    :value="code" 
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="承租单位编码" prop="lessee_code">
                <el-input v-model="form.lessee_code" placeholder="如 AS-ENT-116" />
              </el-form-item>

              <el-form-item label="承租单位名称" prop="lessee_company">
                <el-input v-model="form.lessee_company" placeholder="请输入承租单位" />
              </el-form-item>
            </div>
          </div>

          <!-- Right Column: Financial & Term (col-span-6) -->
          <div class="col-span-12 lg:col-span-6 space-y-4">
            <div class="p-4 rounded-xl bg-slate-950/30 border border-indigo-500/10 space-y-4 shadow-inner h-full">
              <h5 class="text-xs font-bold text-emerald-400 uppercase tracking-wider pb-2 border-b border-indigo-500/10 flex items-center gap-1.5">
                <el-icon><Calendar /></el-icon> 财务核算与周期
              </h5>

              <el-form-item label="合同分配年租金 (万元)" prop="lease_rent">
                <el-input v-model.number="form.lease_rent" placeholder="如 12.50" />
              </el-form-item>

              <el-form-item label="合同签订时间" prop="contract_sign_date">
                <el-date-picker 
                  v-model="form.contract_sign_date" 
                  type="date" 
                  placeholder="选择签订日期" 
                  value-format="YYYY-MM-DD"
                  class="w-full"
                />
              </el-form-item>

              <div class="grid grid-cols-2 gap-3">
                <el-form-item label="租赁开始时间" prop="lease_start_date">
                  <el-date-picker 
                    v-model="form.lease_start_date" 
                    type="date" 
                    placeholder="选择开始日期" 
                    value-format="YYYY-MM-DD"
                    class="w-full"
                  />
                </el-form-item>
                <el-form-item label="租赁结束时间" prop="lease_end_date">
                  <el-date-picker 
                    v-model="form.lease_end_date" 
                    type="date" 
                    placeholder="选择结束日期" 
                    value-format="YYYY-MM-DD"
                    class="w-full"
                  />
                </el-form-item>
              </div>

              <!-- Term Self-Check Banner -->
              <div v-if="calculatedTerm > 0" class="mt-4 p-3 bg-indigo-500/5 rounded-xl border border-indigo-500/20 flex justify-between items-center shadow-sm">
                <div class="flex items-center gap-2">
                  <el-icon class="text-indigo-400 animate-pulse"><Calendar /></el-icon>
                  <span class="text-[11px] text-slate-400 font-medium">租赁天数自检自算</span>
                </div>
                <span class="text-cyan-400 font-bold font-mono text-sm bg-indigo-500/10 px-2.5 py-0.5 rounded-md border border-indigo-500/10">{{ calculatedTerm }} 天</span>
              </div>
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">保存草稿</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="attachmentDialogVisible" width="680px" align-center custom-class="custom-dialog">
      <template #header>
        <div>
          <h4 class="text-base font-bold text-slate-100">合同附件</h4>
          <p class="text-xs text-slate-400 mt-1">{{ activeLease?.contract_code }} · 仅支持 PDF、PNG、JPG，单个文件不超过 20MB</p>
        </div>
      </template>
      <div class="space-y-4">
        <div v-if="canEditAttachment" class="flex items-center gap-3 p-3 rounded-xl bg-slate-950/30 border border-indigo-500/10">
          <el-upload ref="attachmentUploadRef" :auto-upload="false" :show-file-list="true" :limit="1" accept=".pdf,.png,.jpg,.jpeg" @change="selectAttachment" @exceed="handleAttachmentExceed">
            <el-button>选择附件</el-button>
          </el-upload>
          <el-button type="primary" :loading="attachmentUploading" :disabled="!selectedAttachment" @click="uploadAttachment">上传</el-button>
        </div>
        <el-table :data="attachments" v-loading="attachmentsLoading" size="small" empty-text="暂未上传附件">
          <el-table-column prop="original_filename" label="文件名" min-width="220" show-overflow-tooltip />
          <el-table-column prop="file_size" label="大小" width="100">
            <template #default="scope">{{ formatFileSize(scope.row.file_size) }}</template>
          </el-table-column>
          <el-table-column prop="uploader_username" label="上传人" width="100" />
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <button class="text-xs font-semibold text-cyan-400 hover:text-cyan-300 transition-colors cursor-pointer border-0 bg-transparent mr-2" @click="downloadAttachment(scope.row)">下载</button>
              <button v-if="canEditAttachment" class="text-xs font-semibold text-red-400 hover:text-red-300 transition-colors cursor-pointer border-0 bg-transparent" @click="deleteAttachment(scope.row)">删除</button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import axios from '../utils/axios'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import type { FormInstance, UploadFile, UploadInstance } from 'element-plus'
import { useUserStore } from '../store'

const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const searchQuery = ref('')
const filterLessee = ref('')
const filterApprovalStatus = ref('')
const userStore = useUserStore()
const permissions = computed<string[]>(() => userStore.user?.permissions || [])
const canCreate = computed(() => permissions.value.includes('lease:create'))
const canEditAttachment = computed(() => permissions.value.includes('lease:update'))

// Dialog variables
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const positionCodes = ref<string[]>([])
const attachmentDialogVisible = ref(false)
const activeLease = ref<any>(null)
const attachments = ref<any[]>([])
const attachmentsLoading = ref(false)
const attachmentUploading = ref(false)
const selectedAttachment = ref<File | null>(null)
const attachmentUploadRef = ref<UploadInstance>()

const form = reactive({
  contract_code: '',
  ad_position_code: '',
  lessee_code: '',
  lessee_company: '',
  lease_rent: 0,
  lease_start_date: '',
  lease_end_date: '',
  contract_sign_date: '',
  version: 0
})

const formRules = {
  contract_code: [{ required: true, message: '请输入合同编码', trigger: 'blur' }],
  ad_position_code: [{ required: true, message: '请选择广告点位编码', trigger: 'change' }],
  lessee_code: [{ required: true, message: '请输入承租单位编码', trigger: 'blur' }],
  lessee_company: [{ required: true, message: '请输入承租单位名称', trigger: 'blur' }],
  lease_rent: [
    { required: true, message: '请输入分配租金', trigger: 'blur' },
    { type: 'number', message: '租金必须为数值', trigger: 'blur' }
  ],
  lease_start_date: [{ required: true, message: '请选择租赁开始日期', trigger: 'change' }],
  lease_end_date: [{ required: true, message: '请选择租赁结束日期', trigger: 'change' }],
  contract_sign_date: [{ required: true, message: '请选择合同签订日期', trigger: 'change' }]
}

// Compute lease term dynamically
const calculatedTerm = computed(() => {
  if (!form.lease_start_date || !form.lease_end_date) return 0
  const start = new Date(form.lease_start_date)
  const end = new Date(form.lease_end_date)
  const diff = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1
  return diff > 0 ? diff : 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await axios.get('/leases', {
      params: {
        page: currentPage.value,
        limit: pageSize.value,
        search: searchQuery.value,
        lessee_company: filterLessee.value,
        approval_status: filterApprovalStatus.value
      }
    })
    tableData.value = res.data.data
    total.value = res.data.total
  } catch (err: any) {
    ElMessage.error(err.error || '获取合同数据失败')
  } finally {
    loading.value = false
  }
}

const fetchPositionCodes = async () => {
  try {
    const res = await axios.get('/stats/map-positions')
    positionCodes.value = res.data.map((p: any) => p.ad_position_code)
  } catch (err) {
    console.error('Failed to load position codes:', err)
  }
}

const handleFilter = () => {
  currentPage.value = 1
  fetchData()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchData()
}

const formatDate = (dateStr: string | Date) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const approvalStatusLabel = (status: string) => ({
  DRAFT: '草稿', PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回'
}[status] || '未知')

const approvalTagType = (status: string) => ({
  DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger'
}[status] || 'info') as 'success' | 'warning' | 'danger' | 'info'

const canEdit = (row: any) => permissions.value.includes('lease:update') && ['DRAFT', 'REJECTED'].includes(row.approval_status)
const canSubmit = (row: any) => permissions.value.includes('lease:submit') && ['DRAFT', 'REJECTED'].includes(row.approval_status)
const canApprove = (row: any) => permissions.value.includes('lease:approve') && row.approval_status === 'PENDING'
const canArchive = (row: any) => permissions.value.includes('lease:delete') && ['DRAFT', 'REJECTED'].includes(row.approval_status)

const openCreateDialog = () => {
  isEdit.value = false
  editId.value = null
  dialogVisible.value = true
  Object.assign(form, {
    contract_code: `CON-${new Date().getFullYear()}-${String(Math.floor(Math.random() * 900) + 100)}`,
    ad_position_code: '',
    lessee_code: 'AS-ENT-',
    lessee_company: '',
    lease_rent: 10.0,
    lease_start_date: '',
    lease_end_date: '',
    contract_sign_date: formatDate(new Date()),
    version: 0
  })
  if (formRef.value) formRef.value.clearValidate()
}

const openEditDialog = (row: any) => {
  isEdit.value = true
  editId.value = row.ad_lease_id
  dialogVisible.value = true
  Object.assign(form, {
    contract_code: row.contract_code,
    ad_position_code: row.ad_position_code,
    lessee_code: row.lessee_code,
    lessee_company: row.lessee_company,
    lease_rent: Number(row.lease_rent),
    lease_start_date: formatDate(row.lease_start_date),
    lease_end_date: formatDate(row.lease_end_date),
    contract_sign_date: formatDate(row.contract_sign_date),
    version: row.version
  })
  if (formRef.value) formRef.value.clearValidate()
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value && editId.value !== null) {
        await axios.put(`/leases/${editId.value}`, form)
        ElMessage.success('合同草稿已更新，请重新提交审核')
      } else {
        await axios.post('/leases', form)
        ElMessage.success('合同草稿已保存，请上传附件后提交审核')
      }
      dialogVisible.value = false
      fetchData()
    } catch (err: any) {
      ElMessage.error(err.error || '保存合同失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const submitForApproval = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `提交合同「${row.contract_code}」后将无法继续编辑，等待审核员处理。是否继续？`,
      '提交合同审核',
      { confirmButtonText: '提交审核', cancelButtonText: '暂不提交', type: 'warning' }
    )
    await axios.post(`/leases/${row.ad_lease_id}/submit`)
    ElMessage.success('合同已提交审核')
    await fetchData()
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err.error || '提交审核失败')
  }
}

const reviewLease = async (row: any, approved: boolean) => {
  const action = approved ? '审核通过' : '驳回合同'
  try {
    const { value } = await ElMessageBox.prompt(
      approved ? '可填写审核备注；通过后合同将正式计入出租状态与统计。' : '请填写驳回原因，运营人员修改后可再次提交。',
      `${action} · ${row.contract_code}`,
      {
        confirmButtonText: approved ? '确认通过' : '确认驳回',
        cancelButtonText: '取消',
        inputPlaceholder: approved ? '例如：合同资料齐全，审核通过（可选）' : '请说明需要修改的内容',
        inputValidator: (value) => !approved && !String(value || '').trim() ? '驳回时必须填写审核意见' : true
      }
    )
    await axios.post(`/leases/${row.ad_lease_id}/${approved ? 'approve' : 'reject'}`, { comment: value || '' })
    ElMessage.success(approved ? '合同已审核通过并生效' : '合同已驳回，等待修改')
    await fetchData()
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err.error || `${action}失败`)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定归档合同 [${row.contract_code}] (${row.lessee_company}) 吗？\n该操作为逻辑删除，历史数据不会被物理移除。`,
    '警示与二次确认',
    {
      confirmButtonText: '归档',
      cancelButtonText: '取消',
      type: 'danger'
    }
  ).then(async () => {
    try {
      await axios.delete(`/leases/${row.ad_lease_id}`, { data: { version: row.version } })
      ElMessage.success('合同已归档')
      fetchData()
    } catch (err: any) {
      ElMessage.error(err.error || '归档合同失败')
    }
  }).catch(() => {})
}

const openAttachments = async (row: any) => {
  activeLease.value = row
  selectedAttachment.value = null
  attachmentUploadRef.value?.clearFiles()
  attachmentDialogVisible.value = true
  await fetchAttachments()
}

const fetchAttachments = async () => {
  if (!activeLease.value) return
  attachmentsLoading.value = true
  try {
    const res = await axios.get(`/leases/${activeLease.value.ad_lease_id}/attachments`)
    attachments.value = res.data
  } catch (err: any) {
    ElMessage.error(err.error || '获取合同附件失败')
  } finally {
    attachmentsLoading.value = false
  }
}

const selectAttachment = (file: UploadFile) => {
  selectedAttachment.value = file.raw || null
}

const handleAttachmentExceed = () => {
  ElMessage.warning('一次只能上传一个附件')
}

const uploadAttachment = async () => {
  if (!activeLease.value || !selectedAttachment.value) return
  if (selectedAttachment.value.size > 20 * 1024 * 1024) {
    ElMessage.warning('附件大小不能超过 20MB')
    return
  }
  const formData = new FormData()
  formData.append('file', selectedAttachment.value)
  attachmentUploading.value = true
  try {
    await axios.post(`/leases/${activeLease.value.ad_lease_id}/attachments`, formData)
    ElMessage.success('附件上传成功')
    selectedAttachment.value = null
    attachmentUploadRef.value?.clearFiles()
    await fetchAttachments()
    await fetchData()
  } catch (err: any) {
    ElMessage.error(err.error || '附件上传失败')
  } finally {
    attachmentUploading.value = false
  }
}

const downloadAttachment = async (attachment: any) => {
  if (!activeLease.value) return
  try {
    const res = await axios.get(`/leases/${activeLease.value.ad_lease_id}/attachments/${attachment.attachment_id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data], { type: attachment.content_type }))
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.original_filename
    link.click()
    URL.revokeObjectURL(url)
  } catch (err: any) {
    ElMessage.error(err.error || '附件下载失败')
  }
}

const deleteAttachment = async (attachment: any) => {
  if (!activeLease.value) return
  try {
    await ElMessageBox.confirm(`确定删除附件「${attachment.original_filename}」吗？`, '删除附件', { type: 'warning' })
    await axios.delete(`/leases/${activeLease.value.ad_lease_id}/attachments/${attachment.attachment_id}`)
    ElMessage.success('附件删除成功')
    await fetchAttachments()
    await fetchData()
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err.error || '附件删除失败')
  }
}

const formatFileSize = (size: number) => size < 1024 * 1024 ? `${Math.ceil(size / 1024)} KB` : `${(size / 1024 / 1024).toFixed(2)} MB`

const exportCSV = async () => {
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '正在打包并导出全部符合条件的合同数据...',
    background: 'rgba(15, 23, 42, 0.7)'
  })

  try {
    const res = await axios.get('/leases', {
      params: {
        page: 1,
        limit: 1000,
        search: searchQuery.value,
        lessee_company: filterLessee.value,
        approval_status: filterApprovalStatus.value
      }
    })

    const records = res.data.data
    const headers = ['合同编码', '点位编码', '承租单位', '设立位置', '分配租金(万元)', '租赁期限(天)', '租赁开始时间', '租赁结束时间', '合同状态']
    const csvRows = [headers.join(',')]

    for (const r of records) {
      const status = approvalStatusLabel(r.approval_status)
      const row = [
        r.contract_code,
        r.ad_position_code,
        `"${r.lessee_company.replace(/"/g, '""')}"`,
        `"${r.ad_location.replace(/"/g, '""')}"`,
        r.lease_rent,
        r.lease_term,
        formatDate(r.lease_start_date),
        formatDate(r.lease_end_date),
        status
      ]
      csvRows.push(row.join(','))
    }

    const csvContent = '\ufeff' + csvRows.join('\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.setAttribute('href', url)
    link.setAttribute('download', `安顺广告合同明细表_${new Date().toISOString().split('T')[0]}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('合同数据 CSV 导出成功！')
  } catch (err: any) {
    ElMessage.error('导出失败，请重试')
  } finally {
    loadingInstance.close()
  }
}

onMounted(() => {
  fetchData()
  fetchPositionCodes()
})
</script>

<style scoped>
:deep(.custom-dialog) {
  background: radial-gradient(circle at top left, hsl(222, 47%, 16%), hsl(222, 47%, 10%)) !important;
  border: 1px solid rgba(99, 102, 241, 0.25) !important;
  border-radius: 16px !important;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7) !important;
  overflow: hidden;
}
:deep(.custom-dialog .el-dialog__header) {
  margin-right: 0 !important;
  padding: 20px 24px 16px !important;
  background-color: rgba(15, 23, 42, 0.45);
  border-bottom: 1px solid rgba(99, 102, 241, 0.15);
}
:deep(.custom-dialog .el-dialog__body) {
  padding: 24px !important;
  max-height: 65vh;
  overflow-y: auto;
}
:deep(.custom-dialog .el-dialog__footer) {
  padding: 16px 24px !important;
  background-color: rgba(15, 23, 42, 0.45);
  border-top: 1px solid rgba(99, 102, 241, 0.1);
}
:deep(.custom-dialog .el-dialog__title) {
  color: #f8fafc !important;
}
:deep(.custom-dialog .el-form-item__label) {
  color: #94a3b8 !important;
  font-weight: 500;
  padding-bottom: 4px;
}
:deep(.custom-dialog .el-input__inner) {
  color: #e2e8f0 !important;
}
</style>
