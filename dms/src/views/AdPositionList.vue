<template>
  <div class="space-y-6">
    <!-- Filters Header Card -->
    <div class="p-6 rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl flex flex-wrap gap-4 items-center justify-between">
      <div class="flex flex-wrap gap-3 items-center">
        <!-- Search bar -->
        <el-input 
          v-model="searchQuery" 
          placeholder="搜索编号、设立位置、路段..." 
          prefix-icon="Search"
          class="w-64 custom-input"
          clearable
          @input="handleFilter"
        />

        <!-- District Filter -->
        <el-select v-model="filterDistrict" placeholder="所属区县" clearable class="w-40 custom-select" @change="handleFilter">
          <el-option label="西秀区" value="西秀区" />
          <el-option label="开发区" value="开发区" />
          <el-option label="平坝区" value="平坝区" />
          <el-option label="镇宁县" value="镇宁县" />
        </el-select>

        <!-- Status Filter -->
        <el-select v-model="filterStatus" placeholder="当前状态" clearable class="w-40 custom-select" @change="handleFilter">
          <el-option label="空置" value="vacant" />
          <el-option label="已租赁" value="leased" />
        </el-select>
      </div>

      <div class="flex items-center gap-4">
        <button 
          @click="openCreateDialog" 
          class="px-4 py-2 bg-gradient-to-r from-cyan-500 to-indigo-500 text-white rounded-xl font-medium text-xs hover:from-cyan-400 hover:to-indigo-400 active:scale-95 transition-all shadow-md shadow-indigo-500/10 flex items-center gap-1.5 cursor-pointer border-0"
        >
          <el-icon><Plus /></el-icon> 新增广告点位
        </button>
        <div class="text-xs text-slate-400">
          共找到 <span class="text-cyan-400 font-bold font-mono">{{ total }}</span> 个符合条件的广告点位
        </div>
      </div>
    </div>

    <!-- Data Table Card -->
    <div class="rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl overflow-hidden p-1">
      <el-table :data="tableData" v-loading="loading" class="w-full" @row-click="showDetailDialog">
        <el-table-column prop="ad_position_code" label="点位编码" width="140">
          <template #default="scope">
            <span class="font-bold font-mono text-cyan-400">{{ scope.row.ad_position_code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ad_location" label="具体设立位置" min-width="200" show-overflow-tooltip />
        <el-table-column prop="district" label="所属区县" width="100" />
        <el-table-column prop="road_name" label="路段名称" width="120" show-overflow-tooltip />
        <el-table-column prop="ad_specification" label="规格" width="100" />
        <el-table-column prop="total_ad_area" label="面积(㎡)" width="100" align="center">
          <template #default="scope">
            <span class="font-mono">{{ scope.row.total_ad_area }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="租赁状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'leased' ? 'success' : 'info'" effect="dark" size="small" class="rounded-md border-0">
              {{ scope.row.status === 'leased' ? '已租赁' : '空置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="scope">
            <button class="text-xs font-semibold text-cyan-400 hover:text-cyan-300 transition-colors cursor-pointer border-0 bg-transparent" @click.stop="showDetailDialog(scope.row)">
              查看明细
            </button>
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

    <!-- Create/Edit Position Dialog -->
    <el-dialog 
      v-model="dialogVisible" 
      width="850px"
      align-center
      custom-class="custom-dialog"
    >
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white shadow-lg shadow-indigo-500/20">
            <el-icon size="18"><Platform v-if="!isEdit" /><Edit v-else /></el-icon>
          </div>
          <div>
            <h4 class="text-base font-bold text-slate-100">{{ isEdit ? '编辑广告点位档案' : '录入新广告点位' }}</h4>
            <p class="text-xs text-slate-400 mt-0.5">{{ isEdit ? '更新现有广告点位的物理参数、空间规格与地理坐标' : '在系统中登记全新的户外广告点位，以供租赁业务管理' }}</p>
          </div>
        </div>
      </template>

      <el-form :model="form" :rules="formRules" ref="formRef" label-position="top">
        <div class="grid grid-cols-12 gap-5">
          <!-- Left Column: Base Info (col-span-5) -->
          <div class="col-span-12 lg:col-span-5 space-y-4">
            <div class="p-4 rounded-xl bg-slate-950/30 border border-indigo-500/10 space-y-4 shadow-inner">
              <h5 class="text-xs font-bold text-indigo-400 uppercase tracking-wider pb-2 border-b border-indigo-500/10 flex items-center gap-1.5">
                <el-icon><InfoFilled /></el-icon> 基础属性配置
              </h5>

              <el-form-item label="点位编码" prop="ad_position_code">
                <el-input 
                  v-model="form.ad_position_code" 
                  placeholder="如 HGS-COL-122" 
                  :disabled="isEdit" 
                />
              </el-form-item>

              <el-form-item label="所属区县" prop="district">
                <el-select v-model="form.district" placeholder="请选择区县" class="w-full">
                  <el-option label="西秀区" value="西秀区" />
                  <el-option label="开发区" value="开发区" />
                  <el-option label="平坝区" value="平坝区" />
                  <el-option label="镇宁县" value="镇宁县" />
                </el-select>
              </el-form-item>

              <el-form-item label="路段道路名称" prop="road_name">
                <el-input v-model="form.road_name" placeholder="如 黄果树大街" />
              </el-form-item>

              <el-form-item label="点位状态" prop="status">
                <el-select v-model="form.status" class="w-full">
                  <el-option label="空置中" value="vacant" />
                  <el-option label="已租赁" value="leased" />
                  <el-option label="维护中" value="maintenance" />
                </el-select>
              </el-form-item>
            </div>
          </div>

          <!-- Right Column: Specs & Geo (col-span-7) -->
          <div class="col-span-12 lg:col-span-7 space-y-4">
            <div class="p-4 rounded-xl bg-slate-950/30 border border-indigo-500/10 space-y-4 shadow-inner">
              <h5 class="text-xs font-bold text-cyan-400 uppercase tracking-wider pb-2 border-b border-indigo-500/10 flex items-center gap-1.5">
                <el-icon><Location /></el-icon> 空间规格与地理坐标
              </h5>

              <el-form-item label="设立具体物理位置" prop="ad_location">
                <el-input v-model="form.ad_location" placeholder="请输入设立位置" />
              </el-form-item>

              <div class="grid grid-cols-2 gap-4">
                <el-form-item label="规格类型" prop="ad_specification">
                  <el-input v-model="form.ad_specification" placeholder="如 单立柱" />
                </el-form-item>
                <el-form-item label="单面面积说明" prop="single_side_area">
                  <el-input v-model="form.single_side_area" placeholder="如 18*6=108㎡" />
                </el-form-item>
              </div>

              <div class="grid grid-cols-3 gap-4">
                <el-form-item label="总广告面积 (㎡)" prop="total_ad_area">
                  <el-input v-model.number="form.total_ad_area" placeholder="如 216" />
                </el-form-item>
                <el-form-item label="地理经度" prop="longitude">
                  <el-input v-model.number="form.longitude" placeholder="如 105.912" />
                </el-form-item>
                <el-form-item label="地理纬度" prop="latitude">
                  <el-input v-model.number="form.latitude" placeholder="如 26.223" />
                </el-form-item>
              </div>
            </div>
          </div>

          <!-- Bottom: Remark (col-span-12) -->
          <div class="col-span-12">
            <div class="p-4 rounded-xl bg-slate-950/30 border border-indigo-500/10 shadow-inner">
              <el-form-item label="备注说明" prop="remark" class="!mb-0">
                <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入关于点位的其他补充特征或备注..." />
              </el-form-item>
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>


    <!-- Centered Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      title="广告点位全息档案"
      width="1000px"
      align-center
      custom-class="custom-dialog"
    >
      <div v-if="detailLoading" class="flex flex-col items-center justify-center py-12 space-y-3">
        <span class="animate-spin rounded-full h-8 w-8 border-2 border-indigo-500/30 border-t-indigo-500"></span>
        <span class="text-xs text-slate-400">正在调档，请稍后...</span>
      </div>
      
      <div v-else-if="selectedPosition" class="grid grid-cols-1 lg:grid-cols-12 gap-6 text-sm text-slate-300 lg:h-[500px]">
        <!-- Left Column: Specs & Valuation (7 cols) -->
        <div class="lg:col-span-7 lg:h-full lg:overflow-y-auto pr-2 space-y-5">
          <!-- Main Header Card -->
          <div class="p-4 rounded-xl bg-slate-950/40 border border-indigo-500/10 space-y-3 shadow-md">
            <div class="flex items-center justify-between">
              <span class="text-lg font-bold font-mono text-cyan-400">{{ selectedPosition.ad_position_code }}</span>
              <div class="flex items-center gap-2">
                <el-tag :type="selectedPosition.status === 'leased' ? 'success' : 'info'" effect="dark">
                  {{ selectedPosition.status === 'leased' ? '正在履约中' : '空置中' }}
                </el-tag>
              </div>
            </div>
            <div class="text-slate-200 font-semibold text-xs leading-relaxed">{{ selectedPosition.ad_location }}</div>
            
            <!-- Edit/Delete Action Toolbar inside Dialog -->
            <div class="pt-2 flex gap-2 border-t border-indigo-500/5">
              <el-button type="primary" size="small" icon="Edit" @click="openEditDialog">编辑点位</el-button>
              <el-button type="danger" size="small" icon="Delete" @click="handleDelete">删除点位</el-button>
            </div>
          </div>

          <!-- Specifications Tab -->
          <div>
            <h5 class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2.5 border-b border-indigo-500/10 pb-1.5 flex items-center gap-1">
              <el-icon><InfoFilled /></el-icon> 物理与空间规格
            </h5>
            <div class="grid grid-cols-2 gap-y-2.5 gap-x-4 bg-slate-900/20 p-3 rounded-lg">
              <div><span class="text-slate-400">单面规格：</span>{{ selectedPosition.single_side_area }}</div>
              <div><span class="text-slate-400">规格类型：</span>{{ selectedPosition.ad_specification }}</div>
              <div><span class="text-slate-400">总广告面积：</span>{{ selectedPosition.total_ad_area }} ㎡</div>
              <div><span class="text-slate-400">路段区县：</span>{{ selectedPosition.road_name }} / {{ selectedPosition.district }}</div>
              <div><span class="text-slate-400">经度坐标：</span><span class="font-mono text-xs">{{ selectedPosition.longitude }}</span></div>
              <div><span class="text-slate-400">纬度坐标：</span><span class="font-mono text-xs">{{ selectedPosition.latitude }}</span></div>
              <div class="col-span-2"><span class="text-slate-400">特征备注：</span>{{ selectedPosition.remark || '暂无备注' }}</div>
            </div>
          </div>

          <!-- Valuation Tab -->
          <div v-if="selectedPosition.valuation">
            <h5 class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2.5 border-b border-indigo-500/10 pb-1.5 flex items-center gap-1">
              <el-icon><TrendCharts /></el-icon> 资产估值与核算
            </h5>
            <div class="grid grid-cols-2 gap-y-2.5 gap-x-4 bg-slate-900/20 p-3 rounded-lg border border-cyan-500/5">
              <div><span class="text-slate-400">评估总值：</span><span class="text-amber-400 font-bold font-mono">{{ selectedPosition.valuation.total_assessed_value }}</span> 万元</div>
              <div><span class="text-slate-400">折现公允年租：</span><span class="text-emerald-400 font-bold font-mono">{{ selectedPosition.valuation.discounted_rent }}</span> 万元/年</div>
              <div><span class="text-slate-400">评估基准日：</span>{{ formatDate(selectedPosition.valuation.valuation_date) }}</div>
              <div><span class="text-slate-400">评估方法：</span>{{ selectedPosition.valuation.valuation_method }}</div>
              <div class="col-span-2"><span class="text-slate-400">评估机构：</span>{{ selectedPosition.valuation.lessee_company }}</div>
            </div>
          </div>
        </div>

        <!-- Right Column: Timeline (5 cols) -->
        <div class="lg:col-span-5 border-l border-indigo-500/10 lg:pl-6 flex flex-col lg:h-full lg:overflow-hidden space-y-4">
          <h5 class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2 border-b border-indigo-500/10 pb-1.5 flex items-center gap-1">
            <el-icon><Calendar /></el-icon> 历史租赁履约记录
          </h5>
          
          <div v-if="!selectedPosition.leaseHistory || selectedPosition.leaseHistory.length === 0" class="text-slate-500 text-xs py-12 text-center flex-1">
            暂无历史租赁及交易合同记录
          </div>
          
          <div v-else class="flex-1 overflow-y-auto pr-1">
            <el-timeline class="pl-2">
              <el-timeline-item
                v-for="lease in selectedPosition.leaseHistory"
                :key="lease.ad_lease_id"
                :type="isCurrentLease(lease.lease_start_date, lease.lease_end_date) ? 'primary' : 'info'"
                :hollow="!isCurrentLease(lease.lease_start_date, lease.lease_end_date)"
                size="normal"
              >
                <div class="space-y-1 bg-slate-900/20 p-3 rounded-lg relative">
                  <div v-if="isCurrentLease(lease.lease_start_date, lease.lease_end_date)" class="absolute top-2 right-2 text-[10px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-1.5 py-0.5 rounded">
                    当前在租
                  </div>
                  
                  <div class="flex items-center justify-between">
                    <span class="font-bold text-slate-200 text-xs truncate max-w-[130px]">{{ lease.lessee_company }}</span>
                    <span class="font-mono text-cyan-400 text-xs">{{ lease.contract_code }}</span>
                  </div>
                  <div class="text-[11px] text-slate-400">
                    {{ formatDate(lease.lease_start_date) }} 至 {{ formatDate(lease.lease_end_date) }}
                  </div>
                  <div class="text-[11px] text-slate-400 flex gap-3">
                    <span>租金: <strong class="text-emerald-400 font-mono">{{ lease.lease_rent }}</strong> 万</span>
                    <span>期限: <strong class="text-slate-200 font-mono">{{ lease.lease_term }}</strong> 天</span>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end pt-2">
          <el-button @click="detailDialogVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from '../utils/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'

const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const searchQuery = ref('')
const filterDistrict = ref('')
const filterStatus = ref('')

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const selectedPosition = ref<any>(null)

// Dialog variables
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  ad_position_code: '',
  ad_location: '',
  single_side_area: '',
  total_ad_area: 0,
  ad_specification: '',
  longitude: 0,
  latitude: 0,
  district: '',
  road_name: '',
  status: 'vacant',
  remark: '',
  version: 0
})

const formRules = {
  ad_position_code: [
    { required: true, message: '请输入点位编码', trigger: 'blur' },
    { pattern: /^[A-Z]{2,4}-COL-\d{3}$/, message: '必须符合特定编码规范，例如: HGS-COL-001', trigger: 'blur' }
  ],
  ad_location: [{ required: true, message: '请输入点设立具体位置', trigger: 'blur' }],
  single_side_area: [{ required: true, message: '请输入单面面积描述', trigger: 'blur' }],
  ad_specification: [{ required: true, message: '请输入规格分类', trigger: 'blur' }],
  total_ad_area: [
    { required: true, message: '请输入总广告面积', trigger: 'blur' },
    { type: 'number', message: '总面积必须为数值', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await axios.get('/positions', {
      params: {
        page: currentPage.value,
        limit: pageSize.value,
        search: searchQuery.value,
        district: filterDistrict.value,
        status: filterStatus.value
      }
    })
    tableData.value = res.data.data
    total.value = res.data.total
  } catch (err: any) {
    ElMessage.error(err.error || '获取点位数据失败')
  } finally {
    loading.value = false
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

const showDetailDialog = async (row: any) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    const res = await axios.get(`/positions/${row.ad_position_code}`)
    selectedPosition.value = res.data
  } catch (err: any) {
    ElMessage.error(err.error || '获取点位详情失败')
    detailDialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  // Reset fields
  Object.assign(form, {
    ad_position_code: '',
    ad_location: '',
    single_side_area: '',
    total_ad_area: 216,
    ad_specification: '单立柱',
    longitude: 105.9,
    latitude: 26.2,
    district: '西秀区',
    road_name: '',
    status: 'vacant',
    remark: '',
    version: 0
  })
  if (formRef.value) formRef.value.clearValidate()
}

const openEditDialog = () => {
  if (!selectedPosition.value) return
  isEdit.value = true
  dialogVisible.value = true
  const pos = selectedPosition.value
  Object.assign(form, {
    ad_position_code: pos.ad_position_code,
    ad_location: pos.ad_location,
    single_side_area: pos.single_side_area,
    total_ad_area: pos.total_ad_area,
    ad_specification: pos.ad_specification,
    longitude: pos.longitude ? Number(pos.longitude) : 0,
    latitude: pos.latitude ? Number(pos.latitude) : 0,
    district: pos.district,
    road_name: pos.road_name,
    status: pos.status,
    remark: pos.remark || '',
    version: pos.version
  })
  if (formRef.value) formRef.value.clearValidate()
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await axios.put(`/positions/${form.ad_position_code}`, form)
        ElMessage.success('更新点位数据成功')
      } else {
        await axios.post('/positions', form)
        ElMessage.success('录入点位数据成功')
      }
      dialogVisible.value = false
      fetchData()
      if (detailDialogVisible.value && selectedPosition.value?.ad_position_code === form.ad_position_code) {
        // Refresh detail
        const detailRes = await axios.get(`/positions/${form.ad_position_code}`)
        selectedPosition.value = detailRes.data
      }
    } catch (err: any) {
      ElMessage.error(err.error || '保存点位失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = () => {
  if (!selectedPosition.value) return
  const code = selectedPosition.value.ad_position_code
  const version = selectedPosition.value.version
  
  ElMessageBox.confirm(
    `确定归档广告点位 [${code}] 吗？\n该操作为逻辑删除，不会物理删除历史合同。`, 
    '警示与二次确认', 
    {
      confirmButtonText: '确认归档',
      cancelButtonText: '取消',
      type: 'danger'
    }
  ).then(async () => {
    try {
      await axios.delete(`/positions/${code}`, { params: { version } })
      ElMessage.success('点位已归档')
      detailDialogVisible.value = false
      fetchData()
    } catch (err: any) {
      ElMessage.error(err.error || '删除失败')
    }
  }).catch(() => {})
}

const formatDate = (dateStr: string | Date) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const isCurrentLease = (startStr: string, endStr: string) => {
  const today = new Date()
  const start = new Date(startStr)
  const end = new Date(endStr)
  return today >= start && today <= end
}

onMounted(() => {
  fetchData()
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
:deep(.custom-dialog .el-textarea__inner) {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border: 1px solid rgba(99, 102, 241, 0.2) !important;
  color: #e2e8f0 !important;
  box-shadow: none !important;
}
</style>
