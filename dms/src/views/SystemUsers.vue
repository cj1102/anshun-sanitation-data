<template>
  <div class="space-y-6 relative z-10">
    <section class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold text-white">用户与角色</h1>
        <p class="mt-1 text-sm text-slate-400">为系统用户分配角色；变更后旧登录状态会立即失效。</p>
      </div>
      <el-input v-model="search" class="w-64" clearable placeholder="搜索用户名或昵称" @keyup.enter="loadUsers">
        <template #append><el-button :icon="Search" @click="loadUsers" /></template>
      </el-input>
    </section>

    <el-card class="dark-card" shadow="never">
      <el-table :data="users" v-loading="loading" class="dark-table">
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="130" />
        <el-table-column prop="roles" label="当前角色" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="role in roleList(row.roles)" :key="role" class="mr-2 mb-1" effect="dark">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 'active' ? 'success' : 'danger'">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }"><el-button type="primary" link @click="openRoles(row)">分配角色</el-button></template>
        </el-table-column>
      </el-table>
      <div class="mt-5 flex justify-end"><el-pagination v-model:current-page="page" background layout="total, prev, pager, next" :page-size="20" :total="total" @current-change="loadUsers" /></div>
    </el-card>

    <section>
      <h2 class="mb-3 text-base font-semibold text-slate-200">角色说明</h2>
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        <el-card v-for="role in roles" :key="role.role_code" class="dark-card" shadow="never">
          <div class="flex items-center justify-between gap-3"><span class="font-semibold text-cyan-300">{{ role.role_name }}</span><el-tag effect="plain">{{ role.role_code }}</el-tag></div>
          <p class="mt-3 text-sm text-slate-400">{{ role.description }}</p>
        </el-card>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="分配角色" width="460px" :close-on-click-modal="false">
      <p class="mb-4 text-sm text-slate-500">用户：{{ selectedUser?.username }}。至少选择一个角色。</p>
      <el-checkbox-group v-model="selectedRoles" class="grid gap-3">
        <el-checkbox v-for="role in roles" :key="role.role_code" :label="role.role_code" border>{{ role.role_name }}（{{ role.role_code }}）</el-checkbox>
      </el-checkbox-group>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios'

type User = { user_id: number; username: string; nickname?: string; roles: string; status: string }
type Role = { role_code: string; role_name: string; description?: string }

const loading = ref(false), saving = ref(false), users = ref<User[]>([]), roles = ref<Role[]>([])
const total = ref(0), page = ref(1), search = ref(''), dialogVisible = ref(false)
const selectedUser = ref<User | null>(null), selectedRoles = ref<string[]>([])

const roleList = (value?: string) => value ? value.split(',').filter(Boolean) : []
const loadUsers = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/system/users', { params: { page: page.value, limit: 20, search: search.value || undefined } })
    users.value = data.data; total.value = data.total
  } catch (error: any) { ElMessage.error(error.error || '用户列表加载失败') } finally { loading.value = false }
}
const openRoles = (user: User) => { selectedUser.value = user; selectedRoles.value = roleList(user.roles); dialogVisible.value = true }
const saveRoles = async () => {
  if (!selectedUser.value || selectedRoles.value.length === 0) return ElMessage.warning('至少选择一个角色')
  saving.value = true
  try { await axios.put(`/system/users/${selectedUser.value.user_id}/roles`, { roleCodes: selectedRoles.value }); ElMessage.success('角色已更新，该用户需要重新登录'); dialogVisible.value = false; loadUsers() }
  catch (error: any) { ElMessage.error(error.error || '角色更新失败') } finally { saving.value = false }
}
onMounted(async () => { try { roles.value = (await axios.get('/system/roles')).data; await loadUsers() } catch (error: any) { ElMessage.error(error.error || '角色信息加载失败') } })
</script>

<style scoped>
:deep(.dark-card) { background: rgba(15, 23, 42, .72); border-color: rgba(99, 102, 241, .2); color: #e2e8f0; }
:deep(.dark-table), :deep(.dark-table tr), :deep(.dark-table th.el-table__cell), :deep(.dark-table td.el-table__cell) { background: transparent; color: #cbd5e1; border-color: rgba(99, 102, 241, .14); }
:deep(.dark-table .el-table__inner-wrapper::before) { background-color: rgba(99, 102, 241, .14); }
</style>
