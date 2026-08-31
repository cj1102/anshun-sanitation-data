<template>
  <div class="space-y-6 relative z-10">
    <section class="flex items-center justify-between"><div><h1 class="text-xl font-bold text-white">操作审计</h1><p class="mt-1 text-sm text-slate-400">记录关键业务操作，不记录密码等敏感请求内容。</p></div><el-button :icon="Refresh" @click="load">刷新</el-button></section>
    <el-card class="dark-card" shadow="never">
      <div class="mb-4 flex gap-3"><el-input v-model="username" clearable placeholder="操作用户名" class="w-52" @keyup.enter="load" /><el-input v-model="module" clearable placeholder="业务模块" class="w-52" @keyup.enter="load" /><el-button type="primary" @click="load">查询</el-button></div>
      <el-table :data="logs" v-loading="loading" class="dark-table">
        <el-table-column prop="create_time" label="时间" width="170" /><el-table-column prop="operator_username" label="操作人" width="110" />
        <el-table-column prop="module_name" label="模块" width="110" /><el-table-column prop="action_name" label="动作" width="130" />
        <el-table-column prop="target_id" label="对象" min-width="140" /><el-table-column prop="duration_ms" label="耗时(ms)" width="100" />
        <el-table-column label="结果" width="100"><template #default="{ row }"><el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag></template></el-table-column>
        <el-table-column prop="request_id" label="请求 ID" min-width="220" show-overflow-tooltip />
      </el-table>
      <div class="mt-5 flex justify-end"><el-pagination v-model:current-page="page" background layout="total, prev, pager, next" :page-size="20" :total="total" @current-change="load" /></div>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios'
const logs = ref<any[]>([]), loading = ref(false), total = ref(0), page = ref(1), username = ref(''), module = ref('')
const load = async () => { loading.value = true; try { const { data } = await axios.get('/system/audit-logs', { params: { page: page.value, limit: 20, username: username.value || undefined, module: module.value || undefined } }); logs.value = data.data; total.value = data.total } catch (error: any) { ElMessage.error(error.error || '审计日志加载失败') } finally { loading.value = false } }
onMounted(load)
</script>
<style scoped>
:deep(.dark-card) { background: rgba(15, 23, 42, .72); border-color: rgba(99, 102, 241, .2); color: #e2e8f0; }
:deep(.dark-table), :deep(.dark-table tr), :deep(.dark-table th.el-table__cell), :deep(.dark-table td.el-table__cell) { background: transparent; color: #cbd5e1; border-color: rgba(99, 102, 241, .14); }
</style>
