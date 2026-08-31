<template>
  <div class="login-container flex items-center justify-center min-h-screen bg-slate-950 relative overflow-hidden font-sans">
    <!-- Glow effects -->
    <div class="absolute w-[500px] h-[500px] rounded-full bg-indigo-600/15 blur-[120px] top-[-100px] left-[-100px] pointer-events-none"></div>
    <div class="absolute w-[600px] h-[600px] rounded-full bg-cyan-600/10 blur-[150px] bottom-[-200px] right-[-200px] pointer-events-none"></div>
    
    <!-- Transparent Glass Login Card -->
    <div class="w-full max-w-md p-8 rounded-2xl border border-indigo-500/20 bg-slate-900/60 backdrop-blur-xl shadow-2xl shadow-indigo-950/50 relative z-10">
      <div class="flex flex-col items-center mb-6">
        <h2 class="text-2xl font-bold tracking-wide text-white">安顺户外广告管理系统</h2>
        <p class="text-slate-400 text-sm mt-1">
          {{ isRegister ? '注册系统用户账号' : '输入您的账号以访问系统' }}
        </p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名" prop="username" class="custom-form-item">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password" class="custom-form-item">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password size="large" />
        </el-form-item>

        <el-form-item v-if="isRegister" label="昵称" prop="nickname" class="custom-form-item">
          <el-input v-model="form.nickname" placeholder="请输入昵称" prefix-icon="Avatar" size="large" />
        </el-form-item>

        <div class="flex items-center justify-between text-xs text-slate-400 mt-2 mb-6">
          <el-checkbox v-if="!isRegister" v-model="rememberMe" label="保持登录" class="custom-checkbox" />
          <div v-else></div>
          <button type="button" @click="toggleMode" class="text-cyan-400 hover:text-cyan-300 transition-colors font-medium">
            {{ isRegister ? '已有账号? 立即登录' : '没有账号? 立即注册' }}
          </button>
        </div>

        <button type="submit" :disabled="loading" class="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-500 text-white font-semibold hover:from-cyan-400 hover:to-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 shadow-lg shadow-indigo-500/20 active:scale-[0.98] transition-all disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer">
          <span v-if="loading" class="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white"></span>
          <span>{{ isRegister ? '注 册' : '登 录' }}</span>
        </button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store'
import axios from '../utils/axios'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const isRegister = ref(false)
const loading = ref(false)
const rememberMe = ref(true)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 50, message: '用户名不能超过 50 个字符', trigger: 'blur' },
    { validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
      if (isRegister.value && !/^[A-Za-z0-9_]{4,30}$/.test(value)) callback(new Error('注册用户名须为 4-30 位字母、数字或下划线'))
      else callback()
    }, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 72, message: '密码不能超过 72 个字符', trigger: 'blur' },
    { validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
      if (isRegister.value && (value.length < 8 || !/[A-Za-z]/.test(value) || !/\d/.test(value))) {
        callback(new Error('注册密码至少 8 位，并同时包含字母和数字'))
      } else callback()
    }, trigger: 'blur' }
  ]
}

const toggleMode = () => {
  isRegister.value = !isRegister.value
  form.username = ''
  form.password = ''
  form.nickname = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      if (isRegister.value) {
        // Register Mode
        await axios.post('/auth/register', {
          username: form.username,
          password: form.password,
          nickname: form.nickname
        })
        ElMessage.success('注册成功，请使用新账户登录')
        toggleMode()
      } else {
        // Login Mode
        const res = await axios.post('/auth/login', {
          username: form.username,
          password: form.password
        })
        userStore.setLoginInfo(res.data.token, res.data.user)
        ElMessage.success('登录成功，欢迎使用系统')
        router.push('/')
      }
    } catch (err: any) {
      ElMessage.error(err.error || '操作失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  background: radial-gradient(circle at center, hsl(222, 47%, 14%) 0%, hsl(222, 47%, 8%) 100%);
}

:deep(.custom-form-item) {
  margin-bottom: 20px;
}
:deep(.custom-form-item .el-form-item__label) {
  color: #94a3b8 !important;
  font-weight: 500;
  padding-bottom: 4px;
  font-size: 0.875rem;
}
:deep(.el-input__wrapper) {
  border-radius: 10px;
  background-color: rgba(15, 23, 42, 0.5) !important;
  border: 1px solid rgba(99, 102, 241, 0.2);
  box-shadow: none !important;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: #06b6d4 !important;
  background-color: rgba(15, 23, 42, 0.7) !important;
}
:deep(.custom-checkbox .el-checkbox__label) {
  color: #94a3b8 !important;
}
:deep(.custom-checkbox .el-checkbox__inner) {
  background-color: rgba(15, 23, 42, 0.6);
  border-color: rgba(99, 102, 241, 0.3);
}
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #06b6d4;
  border-color: #06b6d4;
}
</style>
