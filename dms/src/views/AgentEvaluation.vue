<template>
  <div class="space-y-6 relative z-10">
    <section class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-bold text-white">AI Agent 评测中心</h1>
        <p class="mt-1 text-sm text-slate-400">查看线上运行质量，并用可重复的评测用例验证工具调用和回答关键词。</p>
      </div>
      <el-radio-group v-model="days" @change="loadOverview">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
      </el-radio-group>
    </section>

    <div v-loading="loading" class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div v-for="card in metricCards" :key="card.label" class="metric-card">
        <div class="metric-label">{{ card.label }}</div>
        <div class="metric-value" :class="card.color">{{ card.value }}</div>
        <div class="metric-tip">{{ card.tip }}</div>
      </div>
    </div>

    <el-card class="dark-card" shadow="never">
      <template #header><span class="font-semibold text-slate-200">新增离线评测用例</span></template>
      <el-form label-position="top" class="grid grid-cols-1 gap-x-5 md:grid-cols-2">
        <el-form-item label="用例名称" required><el-input v-model="form.caseName" maxlength="120" placeholder="例如：数据概览工具回归" /></el-form-item>
        <el-form-item label="页面上下文"><el-input v-model="form.pageContext" maxlength="100" placeholder="/dashboard" /></el-form-item>
        <el-form-item label="评测问题" required class="md:col-span-2"><el-input v-model="form.question" type="textarea" :rows="2" maxlength="2000" placeholder="发送给真实 Agent 的问题" /></el-form-item>
        <el-form-item label="期望工具名"><el-input v-model="form.expectedToolName" maxlength="64" placeholder="例如：get_dashboard_overview" /></el-form-item>
        <el-form-item label="期望关键词"><el-input v-model="form.expectedKeywords" maxlength="500" placeholder="以逗号分隔，例如：广告,点位" /></el-form-item>
      </el-form>
      <div class="flex items-center justify-between gap-4">
        <p class="text-xs leading-5 text-slate-500">至少填写“期望工具名”或“期望关键词”之一。运行时会移除 AI 的写权限，不会创建业务草稿或真实数据。</p>
        <el-button type="primary" :loading="saving" @click="createCase">保存用例</el-button>
      </div>
    </el-card>

    <el-card class="dark-card" shadow="never">
      <template #header><div class="flex items-center justify-between"><span class="font-semibold text-slate-200">评测用例</span><span class="text-xs text-slate-500">每次运行会真实调用模型并消耗额度</span></div></template>
      <el-table :data="cases" v-loading="loading" class="dark-table" empty-text="还没有评测用例">
        <el-table-column prop="caseName" label="用例" min-width="150" show-overflow-tooltip />
        <el-table-column prop="question" label="问题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="expectedToolName" label="期望工具" min-width="170"><template #default="{ row }">{{ row.expectedToolName || '-' }}</template></el-table-column>
        <el-table-column prop="expectedKeywords" label="期望关键词" min-width="145"><template #default="{ row }">{{ row.expectedKeywords || '-' }}</template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button type="primary" link :loading="runningCaseId === row.caseId" @click="runCase(row)">运行</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-card class="dark-card" shadow="never">
      <template #header><div class="flex items-center justify-between"><span class="font-semibold text-slate-200">最近评测结果</span><el-button link type="primary" @click="loadResults">刷新</el-button></div></template>
      <el-table :data="results" v-loading="resultsLoading" class="dark-table" empty-text="尚未运行评测">
        <el-table-column prop="caseName" label="用例" min-width="140" show-overflow-tooltip />
        <el-table-column label="结果" width="90"><template #default="{ row }"><el-tag :type="row.passed ? 'success' : 'danger'" effect="dark">{{ row.passed ? '通过' : '失败' }}</el-tag></template></el-table-column>
        <el-table-column prop="actualTools" label="实际工具" min-width="170"><template #default="{ row }">{{ row.actualTools || '未调用工具' }}</template></el-table-column>
        <el-table-column label="耗时" width="95"><template #default="{ row }">{{ row.durationMs }} ms</template></el-table-column>
        <el-table-column prop="detail" label="评分说明" min-width="280" show-overflow-tooltip />
        <el-table-column label="运行时间" min-width="165"><template #default="{ row }">{{ formatDate(row.createTime) }}</template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios'

type Overview = { totalRuns: number, succeededRuns: number, failedRuns: number, averageDurationMs: number, totalToolCalls: number, failedToolCalls: number, feedbackTotal: number, positiveFeedback: number, negativeFeedback: number, successRate: number, toolSuccessRate: number, positiveFeedbackRate: number }
type EvaluationCase = { caseId: number, caseName: string, question: string, pageContext: string, expectedToolName?: string, expectedKeywords?: string }
type EvaluationResult = { resultId: number, caseName: string, passed: boolean, actualTools?: string, durationMs: number, detail: string, createTime?: string }

const days = ref(7), loading = ref(false), saving = ref(false), resultsLoading = ref(false), runningCaseId = ref<number | null>(null)
const overview = ref<Overview>({ totalRuns: 0, succeededRuns: 0, failedRuns: 0, averageDurationMs: 0, totalToolCalls: 0, failedToolCalls: 0, feedbackTotal: 0, positiveFeedback: 0, negativeFeedback: 0, successRate: 0, toolSuccessRate: 0, positiveFeedbackRate: 0 })
const cases = ref<EvaluationCase[]>([]), results = ref<EvaluationResult[]>([])
const form = reactive({ caseName: '', question: '', pageContext: '/dashboard', expectedToolName: '', expectedKeywords: '' })

const metricCards = computed(() => [
  { label: 'Agent 成功率', value: `${overview.value.successRate}%`, tip: `${overview.value.succeededRuns} 成功 / ${overview.value.failedRuns} 失败`, color: 'text-emerald-400' },
  { label: '平均响应耗时', value: `${overview.value.averageDurationMs} ms`, tip: `近 ${days.value} 天共 ${overview.value.totalRuns} 次运行`, color: 'text-cyan-400' },
  { label: '工具调用成功率', value: `${overview.value.toolSuccessRate}%`, tip: `${overview.value.totalToolCalls - overview.value.failedToolCalls} 成功 / ${overview.value.failedToolCalls} 失败`, color: 'text-violet-400' },
  { label: '正向反馈率', value: `${overview.value.positiveFeedbackRate}%`, tip: `${overview.value.positiveFeedback} 赞 / ${overview.value.negativeFeedback} 踩`, color: 'text-amber-300' }
])

const loadOverview = async () => { try { overview.value = (await axios.get('/ai/evaluation/overview', { params: { days: days.value } })).data } catch (error: any) { ElMessage.error(error.error || '评测概览加载失败') } }
const loadCases = async () => { try { cases.value = (await axios.get('/ai/evaluation/cases')).data } catch (error: any) { ElMessage.error(error.error || '评测用例加载失败') } }
const loadResults = async () => { resultsLoading.value = true; try { results.value = (await axios.get('/ai/evaluation/results', { params: { limit: 20 } })).data } catch (error: any) { ElMessage.error(error.error || '评测结果加载失败') } finally { resultsLoading.value = false } }
const loadAll = async () => { loading.value = true; try { await Promise.all([loadOverview(), loadCases()]); await loadResults() } finally { loading.value = false } }
const createCase = async () => {
  if (!form.caseName.trim() || !form.question.trim()) return ElMessage.warning('请填写用例名称和评测问题')
  if (!form.expectedToolName.trim() && !form.expectedKeywords.trim()) return ElMessage.warning('至少填写期望工具名或期望关键词')
  saving.value = true
  try {
    await axios.post('/ai/evaluation/cases', { ...form })
    Object.assign(form, { caseName: '', question: '', pageContext: '/dashboard', expectedToolName: '', expectedKeywords: '' })
    ElMessage.success('评测用例已保存')
    await loadCases()
  } catch (error: any) { ElMessage.error(error.error || '评测用例保存失败') } finally { saving.value = false }
}
const runCase = async (item: EvaluationCase) => {
  runningCaseId.value = item.caseId
  try {
    const result = (await axios.post(`/ai/evaluation/cases/${item.caseId}/run`)).data as EvaluationResult
    ElMessage[result.passed ? 'success' : 'warning'](result.passed ? '评测通过' : '评测未通过，请查看评分说明')
    await Promise.all([loadOverview(), loadResults()])
  } catch (error: any) { ElMessage.error(error.error || '评测运行失败') } finally { runningCaseId.value = null }
}
const formatDate = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'
onMounted(loadAll)
</script>

<style scoped>
.metric-card { min-height: 132px; padding: 18px; border: 1px solid rgba(99, 102, 241, .2); border-radius: 14px; background: rgba(15, 23, 42, .72); }
.metric-label { color: #94a3b8; font-size: 12px; }
.metric-value { margin-top: 12px; font-size: 26px; font-weight: 700; font-variant-numeric: tabular-nums; }
.metric-tip { margin-top: 8px; color: #64748b; font-size: 11px; }
:deep(.dark-card) { background: rgba(15, 23, 42, .72); border-color: rgba(99, 102, 241, .2); color: #e2e8f0; }
:deep(.dark-table), :deep(.dark-table tr), :deep(.dark-table th.el-table__cell), :deep(.dark-table td.el-table__cell) { background: transparent; color: #cbd5e1; border-color: rgba(99, 102, 241, .14); }
:deep(.dark-table .el-table__inner-wrapper::before) { background-color: rgba(99, 102, 241, .14); }
</style>
