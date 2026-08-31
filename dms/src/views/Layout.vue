<template>
  <div class="flex h-screen overflow-hidden bg-slate-950 text-slate-100 font-sans">
    <!-- Sidebar -->
    <aside class="w-64 bg-slate-900/40 border-r border-indigo-500/10 backdrop-blur-xl flex flex-col justify-between shrink-0 relative z-20">
      <div>
        <!-- Brand header -->
        <div class="h-16 flex items-center px-6 gap-3 border-b border-indigo-500/10 bg-slate-950/20">
          <div class="w-8 h-8 bg-gradient-to-tr from-cyan-400 to-indigo-500 rounded-lg flex items-center justify-center shadow-md shadow-indigo-500/10">
            <span class="text-white text-sm font-bold">AS</span>
          </div>
          <span class="text-sm font-bold uppercase tracking-wider text-slate-200">安顺户外广告数据系统</span>
        </div>

        <!-- Navigation Menu -->
        <nav class="p-4 space-y-1">
          <router-link 
            v-for="item in availableMenuItems" 
            :key="item.path" 
            :to="item.path"
            class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group relative"
            :class="[
              isActive(item.path) 
                ? 'bg-gradient-to-r from-indigo-500/15 to-cyan-500/5 text-cyan-400 font-medium border-l-2 border-cyan-400' 
                : 'text-slate-400 hover:bg-slate-800/40 hover:text-slate-200'
            ]"
          >
            <component :is="item.icon" class="w-5 h-5 transition-transform group-hover:scale-110" />
            <span class="text-sm">{{ item.name }}</span>
            <div v-if="isActive(item.path)" class="absolute right-4 w-1.5 h-1.5 rounded-full bg-cyan-400 shadow-lg shadow-cyan-400/50"></div>
          </router-link>
        </nav>
      </div>

      <!-- User footer profile -->
      <div class="p-4 border-t border-indigo-500/10 bg-slate-950/20">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-full bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-300 font-semibold text-sm">
              {{ nickname.substring(0, 1).toUpperCase() }}
            </div>
            <div class="truncate max-w-[120px]">
              <div class="text-xs font-semibold text-slate-200 truncate">{{ nickname }}</div>
              <div class="text-[10px] text-slate-400 capitalize">{{ userStore.user?.role || 'User' }}</div>
            </div>
          </div>
          <el-dropdown trigger="click" placement="top-end">
            <button class="text-slate-400 hover:text-slate-200 p-1 cursor-pointer">
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu class="custom-dropdown-menu">
                <el-dropdown-item @click="handleLogout" class="text-red-400 hover:text-red-300">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </aside>

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden relative">
      <!-- Top header bar -->
      <header class="h-16 border-b border-indigo-500/10 flex items-center justify-between px-8 bg-slate-900/10 backdrop-blur-md relative z-10">
        <div class="flex items-center gap-2">
          <span class="text-sm font-semibold text-slate-300">{{ currentRouteName }}</span>
        </div>
        <div class="flex items-center gap-4 text-xs text-slate-400">
          <span>当前时间：{{ formattedTime }}</span>
        </div>
      </header>

      <!-- Page Content Router-view -->
      <main class="flex-1 overflow-y-auto p-8 relative bg-slate-950">
        <!-- Glow accents inside main view -->
        <div class="absolute w-[300px] h-[300px] rounded-full bg-indigo-500/5 blur-[100px] top-[10%] right-[10%] pointer-events-none"></div>
        <router-view />
      </main>
    </div>

    <!-- AI assistant: global read-only helper, available after login on every business page. -->
    <button
      ref="aiFloatButtonElement"
      class="ai-float-button"
      :class="{ 'is-dragging': aiFloatDragging }"
      :style="aiFloatStyle"
      type="button"
      aria-label="打开 AI 助手"
      @pointerdown="startAiFloatDrag"
      @click="handleAiFloatClick"
    >
      <span class="text-lg">✦</span><span>AI 助手</span>
    </button>

    <section v-if="aiOpen" class="ai-panel" :class="{ 'is-dragging': aiDragging }" :style="aiPanelStyle" aria-label="AI 助手对话窗口">
      <div class="ai-panel-header" title="按住标题栏可拖动窗口" @pointerdown="startAiDrag">
        <div>
          <div class="font-semibold text-slate-100">安顺 AI 助手</div>
          <div class="text-[11px] text-cyan-300/80 mt-0.5">DeepSeek V4 · 查询与待确认操作</div>
        </div>
        <div class="flex items-center gap-2">
          <button class="ai-header-button" type="button" title="管理长期记忆" @pointerdown.stop @click="toggleAiMemory">记忆</button>
          <button class="ai-header-button" type="button" title="查看历史对话" @pointerdown.stop @click="toggleAiHistory">历史</button>
          <button class="ai-header-button" type="button" title="新建对话" @pointerdown.stop @click="startNewAiConversation">新对话</button>
          <button class="ai-header-button text-lg" type="button" aria-label="关闭 AI 助手" @pointerdown.stop @click="aiOpen = false">×</button>
        </div>
      </div>
      <div v-if="aiMemoryOpen" class="ai-memory">
        <div class="ai-history-heading"><span>我的长期记忆</span><span class="text-slate-500 text-[11px]">仅你可见</span></div>
        <p class="ai-memory-tip">只有你手动添加，或明确说“请记住：……”时才会保存。请勿保存密码、密钥或证件信息。</p>
        <div class="ai-memory-editor">
          <select v-model="aiMemoryType" class="ai-memory-select" aria-label="记忆类型">
            <option value="PROFILE">个人资料</option><option value="PREFERENCE">回答偏好</option>
            <option value="WORK_CONTEXT">工作背景</option><option value="OTHER">其他</option>
          </select>
          <textarea v-model="aiMemoryInput" class="ai-memory-input" rows="3" maxlength="300" placeholder="例如：我负责财务数据核对，希望回答先给结论。" />
          <div class="flex justify-end mt-2"><button class="ai-send-button" type="button" :disabled="aiMemorySaving || !aiMemoryInput.trim()" @click="saveAiMemory">{{ aiMemorySaving ? '保存中…' : '保存记忆' }}</button></div>
        </div>
        <div v-if="aiMemoryLoading" class="ai-history-empty">正在加载…</div>
        <div v-else-if="aiMemories.length === 0" class="ai-history-empty">还没有长期记忆。</div>
        <div v-else class="ai-memory-list">
          <div v-for="memory in aiMemories" :key="memory.memoryId" class="ai-memory-item">
            <div class="min-w-0 flex-1"><span class="ai-memory-type">{{ memoryTypeName(memory.memoryType) }}</span><p>{{ memory.content }}</p></div>
            <button class="ai-history-delete" type="button" title="删除这条记忆" @click="deleteAiMemory(memory.memoryId)">×</button>
          </div>
        </div>
      </div>
      <div v-else-if="aiHistoryOpen" class="ai-history">
        <div class="ai-history-heading">
          <span>历史对话</span><span class="text-slate-500 text-[11px]">仅你可见</span>
        </div>
        <div v-if="aiHistoryLoading" class="ai-history-empty">正在加载…</div>
        <div v-else-if="aiConversations.length === 0" class="ai-history-empty">还没有已保存的对话。</div>
        <div v-else class="ai-history-list">
          <div v-for="conversation in aiConversations" :key="conversation.conversationId" class="ai-history-item" :class="{ active: currentConversationId === conversation.conversationId }">
            <button type="button" class="ai-history-open" @click="openAiConversation(conversation.conversationId)">
              <span class="ai-history-title">{{ conversation.title }}</span>
              <span class="ai-history-preview">{{ conversation.preview }}</span>
            </button>
            <button type="button" class="ai-history-delete" title="删除该对话" @click="deleteAiConversation(conversation.conversationId)">×</button>
          </div>
        </div>
      </div>
      <div v-else ref="aiMessagesElement" class="ai-messages">
        <div v-if="aiMessages.length === 0" class="ai-welcome">
          <div class="text-cyan-300 font-medium mb-1">你好，我是系统 AI 助手。</div>
          <p>可以问我页面功能、角色权限、合同和点位的管理流程。</p>
        </div>
        <div v-for="(item, index) in aiMessages" :key="index" class="ai-message" :class="item.role">
          <div class="ai-message-role">{{ item.role === 'user' ? '你' : 'AI' }}</div>
          <div class="min-w-0">
            <div class="ai-message-content" :class="{ 'ai-streaming-message': item.streaming && !item.content }">
              {{ item.content || (item.streaming ? (aiStreamStatus || '正在思考…') : '') }}
            </div>
            <div v-if="item.role === 'assistant' && item.toolCalls?.length" class="ai-tool-calls">
              <div v-for="(tool, toolIndex) in item.toolCalls" :key="toolIndex" class="ai-tool-call" :class="{ failed: !tool.success }">
                <span>{{ toolLabel(tool.toolName) }}</span>
                <span>{{ tool.summary }} · {{ tool.durationMs }}ms</span>
              </div>
            </div>
            <div v-if="item.role === 'assistant' && item.agentRunId" class="ai-feedback">
              <span v-if="item.feedbackRating" class="ai-feedback-thanks">已{{ item.feedbackRating === 'UP' ? '点赞' : '反馈' }}</span>
              <template v-else>
                <span>这条回答有帮助吗？</span>
                <button type="button" title="有帮助" :disabled="aiFeedbackLoading === item.agentRunId" @click="submitAiFeedback(item, 'UP')">👍</button>
                <button type="button" title="需要改进" :disabled="aiFeedbackLoading === item.agentRunId" @click="submitAiFeedback(item, 'DOWN')">👎</button>
              </template>
            </div>
          </div>
        </div>
        <div v-if="aiPendingActions.length" class="ai-pending-actions">
          <div class="ai-pending-heading">待确认操作 <span>不会自动写入数据</span></div>
          <div v-for="action in aiPendingActions" :key="action.actionId" class="ai-pending-action">
            <div class="ai-pending-title">{{ action.title }}</div>
            <p>{{ action.summary }}</p>
            <div class="ai-pending-fields">
              <div v-for="(value, key) in action.fields" :key="key"><span>{{ actionFieldLabel(key) }}</span><strong>{{ actionFieldValue(value) }}</strong></div>
            </div>
            <div class="ai-pending-expiry">请在 {{ formatActionExpiry(action.expiresAt) }} 前确认</div>
            <div class="ai-pending-buttons">
              <button type="button" class="ai-pending-cancel" :disabled="aiActionLoading === action.actionId" @click="cancelAiPendingAction(action)">取消草稿</button>
              <button type="button" class="ai-send-button" :disabled="aiActionLoading === action.actionId" @click="confirmAiPendingAction(action)">
                {{ aiActionLoading === action.actionId ? '执行中…' : '核对并执行' }}
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="ai-composer">
        <textarea
          v-model="aiInput"
          class="ai-input"
          rows="3"
          maxlength="2000"
          placeholder="例如：只读访客为什么不能编辑合同？"
          :disabled="aiLoading"
          @keydown.enter.exact.prevent="sendAiMessage"
        />
        <div class="flex items-center justify-between mt-2">
          <span class="text-[10px] text-slate-500">Enter 发送，Shift + Enter 换行</span>
          <button class="ai-send-button" type="button" :disabled="aiLoading || !aiInput.trim()" @click="sendAiMessage">
            {{ aiLoading ? '发送中…' : '发送' }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store'
import { ElMessageBox, ElMessage } from 'element-plus'
import axios from '../utils/axios'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menuItems = [
  { name: '数据概览', path: '/dashboard', icon: 'Odometer', permission: 'stats:view' },
  { name: '广告点位', path: '/positions', icon: 'MapLocation', permission: 'position:view' },
  { name: '租赁合同', path: '/leases', icon: 'DocumentText', permission: 'lease:view' },
  { name: '运营统计', path: '/analytics', icon: 'PieChart', permission: 'stats:view' },
  { name: '用户与角色', path: '/system/users', icon: 'UserFilled', permission: 'system:user:manage' },
  { name: 'AI 知识库', path: '/knowledge', icon: 'Collection', permission: 'ai:knowledge:manage' },
  { name: 'AI 评测中心', path: '/ai/evaluation', icon: 'DataAnalysis', permission: 'ai:evaluation:view' },
  { name: '操作审计', path: '/system/audit-logs', icon: 'DocumentChecked', permission: 'system:audit:view' }
]

const nickname = computed(() => userStore.user?.nickname || userStore.user?.username || '用户')
const permissions = computed<string[]>(() => userStore.user?.permissions || [])
const availableMenuItems = computed(() => menuItems.filter(item => permissions.value.includes(item.permission)))

const isActive = (path: string) => {
  return route.path.startsWith(path)
}

const currentRouteName = computed(() => {
  const matched = availableMenuItems.value.find(item => route.path.startsWith(item.path))
  return matched ? matched.name : '控制台'
})

const timeStr = ref(new Date().toLocaleString())
let timeInterval: any

const formattedTime = computed(() => timeStr.value)

type AiToolCall = { toolName: string, summary: string, success: boolean, durationMs: number }
type AiMessage = { role: 'user' | 'assistant', content: string, toolCalls?: AiToolCall[], agentRunId?: number, feedbackRating?: 'UP' | 'DOWN', streaming?: boolean }
type AiPendingAction = { actionId: string, actionType: string, title: string, summary: string, fields: Record<string, unknown>, requiredPermission: string, status: string, expiresAt: string }
type AiStreamDone = { answer: string, model: string, conversationId: number, memorySaved?: boolean, memoryMessage?: string, agentRunId?: number, toolCalls?: AiToolCall[], pendingActions?: AiPendingAction[] }
type AiConversation = { conversationId: number, title: string, preview: string, lastMessageAt: string }
type AiMemory = { memoryId: number, memoryType: string, content: string, source: string, createTime: string }
const aiOpen = ref(false)
const aiInput = ref('')
const aiLoading = ref(false)
const aiStreamStatus = ref('')
const aiMessages = ref<AiMessage[]>([])
const aiMessagesElement = ref<HTMLElement | null>(null)
const currentConversationId = ref<number | null>(null)
const aiPendingActions = ref<AiPendingAction[]>([])
const aiActionLoading = ref<string | null>(null)
const aiFeedbackLoading = ref<number | null>(null)
const aiHistoryOpen = ref(false)
const aiHistoryLoading = ref(false)
const aiConversations = ref<AiConversation[]>([])
const aiMemoryOpen = ref(false)
const aiMemoryLoading = ref(false)
const aiMemorySaving = ref(false)
const aiMemories = ref<AiMemory[]>([])
const aiMemoryInput = ref('')
const aiMemoryType = ref('OTHER')
type AiPanelPosition = { left: number, top: number }
const AI_PANEL_POSITION_KEY = 'anshun-ai-panel-position-v1'
const aiPanelPosition = ref<AiPanelPosition>({ left: 28, top: 100 })
const aiDragging = ref(false)
const aiPanelStyle = computed(() => ({ left: `${aiPanelPosition.value.left}px`, top: `${aiPanelPosition.value.top}px` }))
let aiPanelPositionReady = false
let aiDragOffset = { left: 0, top: 0 }
const AI_FLOAT_POSITION_KEY = 'anshun-ai-float-position-v1'
const aiFloatButtonElement = ref<HTMLElement | null>(null)
const aiFloatPosition = ref<AiPanelPosition>({ left: 28, top: 28 })
const aiFloatDragging = ref(false)
const aiFloatStyle = computed(() => ({ left: `${aiFloatPosition.value.left}px`, top: `${aiFloatPosition.value.top}px` }))
let aiFloatPositionReady = false
let aiFloatDragOffset = { left: 0, top: 0 }
let aiFloatDragStart = { x: 0, y: 0 }
let aiFloatClickBlockUntil = 0

onMounted(() => {
  timeInterval = setInterval(() => {
    timeStr.value = new Date().toLocaleString()
  }, 1000)
  initializeAiPanelPosition()
  initializeAiFloatPosition()
  window.addEventListener('resize', constrainAiPanelPosition)
  window.addEventListener('resize', constrainAiFloatPosition)
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  stopAiDrag()
  stopAiFloatDrag()
  window.removeEventListener('resize', constrainAiPanelPosition)
  window.removeEventListener('resize', constrainAiFloatPosition)
})

const aiPanelSize = () => ({
  width: Math.min(390, Math.max(1, window.innerWidth - 32)),
  height: Math.min(570, Math.max(1, window.innerHeight - 120))
})
const clampAiPanelPosition = (left: number, top: number): AiPanelPosition => {
  const { width, height } = aiPanelSize()
  return {
    left: Math.min(Math.max(8, left), Math.max(8, window.innerWidth - width - 8)),
    top: Math.min(Math.max(8, top), Math.max(8, window.innerHeight - height - 8))
  }
}
const persistAiPanelPosition = () => {
  try { localStorage.setItem(AI_PANEL_POSITION_KEY, JSON.stringify(aiPanelPosition.value)) } catch { /* browser storage may be disabled */ }
}
const initializeAiPanelPosition = () => {
  if (aiPanelPositionReady) return
  let saved: AiPanelPosition | null = null
  try {
    const parsed = JSON.parse(localStorage.getItem(AI_PANEL_POSITION_KEY) || 'null')
    if (Number.isFinite(parsed?.left) && Number.isFinite(parsed?.top)) saved = parsed
  } catch { /* ignore malformed local storage */ }
  const { width, height } = aiPanelSize()
  aiPanelPosition.value = clampAiPanelPosition(saved?.left ?? window.innerWidth - width - 28, saved?.top ?? window.innerHeight - height - 84)
  aiPanelPositionReady = true
}
const constrainAiPanelPosition = () => {
  if (!aiPanelPositionReady) return
  aiPanelPosition.value = clampAiPanelPosition(aiPanelPosition.value.left, aiPanelPosition.value.top)
  persistAiPanelPosition()
}
const startAiDrag = (event: PointerEvent) => {
  if (event.button !== 0) return
  initializeAiPanelPosition()
  event.preventDefault()
  aiDragging.value = true
  aiDragOffset = { left: event.clientX - aiPanelPosition.value.left, top: event.clientY - aiPanelPosition.value.top }
  window.addEventListener('pointermove', moveAiPanel)
  window.addEventListener('pointerup', stopAiDrag)
  window.addEventListener('pointercancel', stopAiDrag)
}
const moveAiPanel = (event: PointerEvent) => {
  if (!aiDragging.value) return
  aiPanelPosition.value = clampAiPanelPosition(event.clientX - aiDragOffset.left, event.clientY - aiDragOffset.top)
}
const stopAiDrag = () => {
  if (aiDragging.value) persistAiPanelPosition()
  aiDragging.value = false
  window.removeEventListener('pointermove', moveAiPanel)
  window.removeEventListener('pointerup', stopAiDrag)
  window.removeEventListener('pointercancel', stopAiDrag)
}

const aiFloatSize = () => {
  const rect = aiFloatButtonElement.value?.getBoundingClientRect()
  return { width: rect?.width || 132, height: rect?.height || 52 }
}
const clampAiFloatPosition = (left: number, top: number): AiPanelPosition => {
  const { width, height } = aiFloatSize()
  return {
    left: Math.min(Math.max(8, left), Math.max(8, window.innerWidth - width - 8)),
    top: Math.min(Math.max(8, top), Math.max(8, window.innerHeight - height - 8))
  }
}
const persistAiFloatPosition = () => {
  try { localStorage.setItem(AI_FLOAT_POSITION_KEY, JSON.stringify(aiFloatPosition.value)) } catch { /* browser storage may be disabled */ }
}
const initializeAiFloatPosition = () => {
  if (aiFloatPositionReady) return
  let saved: AiPanelPosition | null = null
  try {
    const parsed = JSON.parse(localStorage.getItem(AI_FLOAT_POSITION_KEY) || 'null')
    if (Number.isFinite(parsed?.left) && Number.isFinite(parsed?.top)) saved = parsed
  } catch { /* ignore malformed local storage */ }
  const { width, height } = aiFloatSize()
  aiFloatPosition.value = clampAiFloatPosition(saved?.left ?? window.innerWidth - width - 28, saved?.top ?? window.innerHeight - height - 26)
  aiFloatPositionReady = true
}
const constrainAiFloatPosition = () => {
  if (!aiFloatPositionReady) return
  aiFloatPosition.value = clampAiFloatPosition(aiFloatPosition.value.left, aiFloatPosition.value.top)
  persistAiFloatPosition()
}
const startAiFloatDrag = (event: PointerEvent) => {
  if (event.pointerType === 'mouse' && event.button !== 0) return
  initializeAiFloatPosition()
  aiFloatDragStart = { x: event.clientX, y: event.clientY }
  aiFloatDragOffset = { left: event.clientX - aiFloatPosition.value.left, top: event.clientY - aiFloatPosition.value.top }
  window.addEventListener('pointermove', moveAiFloat)
  window.addEventListener('pointerup', stopAiFloatDrag)
  window.addEventListener('pointercancel', stopAiFloatDrag)
}
const moveAiFloat = (event: PointerEvent) => {
  const movedFarEnough = Math.abs(event.clientX - aiFloatDragStart.x) > 4 || Math.abs(event.clientY - aiFloatDragStart.y) > 4
  if (!movedFarEnough && !aiFloatDragging.value) return
  aiFloatDragging.value = true
  aiFloatPosition.value = clampAiFloatPosition(event.clientX - aiFloatDragOffset.left, event.clientY - aiFloatDragOffset.top)
}
const stopAiFloatDrag = () => {
  if (aiFloatDragging.value) {
    persistAiFloatPosition()
    aiFloatClickBlockUntil = Date.now() + 250
  }
  aiFloatDragging.value = false
  window.removeEventListener('pointermove', moveAiFloat)
  window.removeEventListener('pointerup', stopAiFloatDrag)
  window.removeEventListener('pointercancel', stopAiFloatDrag)
}

const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'custom-confirm-box'
  }).then(async () => {
    try {
      await axios.post('/auth/logout')
      ElMessage.success('已安全退出')
    } catch {
      ElMessage.warning('服务端退出确认失败，本机登录信息已清除')
    } finally {
      userStore.clearLoginInfo()
      router.push('/login')
    }
  }).catch(() => {})
}

const scrollAiMessages = async () => {
  await nextTick()
  if (aiMessagesElement.value) aiMessagesElement.value.scrollTop = aiMessagesElement.value.scrollHeight
}

const toolLabel = (toolName: string) => ({
  get_dashboard_overview: '查询数据概览',
  search_ad_positions: '搜索广告点位',
  get_ad_position_detail: '查询点位详情',
  search_lease_contracts: '搜索租赁合同',
  get_lease_contract_detail: '查询合同详情',
  prepare_create_ad_position: '生成新增点位草稿'
}[toolName] || '调用业务工具')

const actionFieldLabel = (key: string) => ({
  adPositionCode: '点位编码', adLocation: '设立位置', singleSideArea: '单面面积', totalAdArea: '总面积',
  adSpecification: '点位规格', district: '区县', roadName: '道路', longitude: '经度', latitude: '纬度',
  status: '初始状态', remark: '备注'
}[key] || key)
const actionFieldValue = (value: unknown) => value === 'vacant' ? '空置' : String(value ?? '')
const formatActionExpiry = (value: string) => new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })

const startNewAiConversation = () => {
  currentConversationId.value = null
  aiMessages.value = []
  aiInput.value = ''
  aiHistoryOpen.value = false
  aiMemoryOpen.value = false
}

const loadAiConversations = async () => {
  aiHistoryLoading.value = true
  try {
    const res = await axios.get('/ai/conversations', { params: { page: 1, pageSize: 20 } })
    aiConversations.value = res.data?.data || []
  } catch {
    ElMessage.error('历史对话加载失败，请稍后重试')
  } finally {
    aiHistoryLoading.value = false
  }
}

const loadAiPendingActions = async () => {
  try {
    const res = await axios.get('/ai/actions/pending')
    aiPendingActions.value = res.data || []
  } catch (err: any) {
    ElMessage.error(err.error || '待确认操作加载失败，请稍后重试')
  }
}

const toggleAiAssistant = async () => {
  initializeAiPanelPosition()
  aiOpen.value = !aiOpen.value
  if (aiOpen.value) await Promise.all([loadAiConversations(), loadAiPendingActions()])
}

const handleAiFloatClick = async () => {
  if (Date.now() < aiFloatClickBlockUntil) return
  await toggleAiAssistant()
}

const toggleAiHistory = async () => {
  aiHistoryOpen.value = !aiHistoryOpen.value
  if (aiHistoryOpen.value) aiMemoryOpen.value = false
  if (aiHistoryOpen.value) await loadAiConversations()
}

const loadAiMemories = async () => {
  aiMemoryLoading.value = true
  try {
    const res = await axios.get('/ai/memories')
    aiMemories.value = res.data || []
  } catch (err: any) {
    ElMessage.error(err.error || '长期记忆加载失败，请稍后重试')
  } finally {
    aiMemoryLoading.value = false
  }
}

const toggleAiMemory = async () => {
  aiMemoryOpen.value = !aiMemoryOpen.value
  if (aiMemoryOpen.value) {
    aiHistoryOpen.value = false
    await loadAiMemories()
  }
}

const saveAiMemory = async () => {
  const content = aiMemoryInput.value.trim()
  if (!content || aiMemorySaving.value) return
  aiMemorySaving.value = true
  try {
    await axios.post('/ai/memories', { content, memoryType: aiMemoryType.value })
    aiMemoryInput.value = ''
    ElMessage.success('已保存为长期记忆')
    await loadAiMemories()
  } catch (err: any) {
    ElMessage.error(err.error || '长期记忆保存失败')
  } finally {
    aiMemorySaving.value = false
  }
}

const deleteAiMemory = async (memoryId: number) => {
  try {
    await axios.delete(`/ai/memories/${memoryId}`)
    ElMessage.success('长期记忆已删除')
    await loadAiMemories()
  } catch (err: any) {
    ElMessage.error(err.error || '长期记忆删除失败')
  }
}

const memoryTypeName = (value: string) => ({ PROFILE: '个人资料', PREFERENCE: '回答偏好', WORK_CONTEXT: '工作背景', OTHER: '其他' }[value] || '其他')

const openAiConversation = async (conversationId: number) => {
  aiLoading.value = true
  try {
    const res = await axios.get(`/ai/conversations/${conversationId}`)
    currentConversationId.value = res.data.conversationId
    aiMessages.value = (res.data.messages || []).map((item: AiMessage) => ({ role: item.role, content: item.content }))
    aiHistoryOpen.value = false
    aiMemoryOpen.value = false
    await scrollAiMessages()
  } catch (err: any) {
    ElMessage.error(err.error || '对话加载失败，请稍后重试')
  } finally {
    aiLoading.value = false
  }
}

const deleteAiConversation = async (conversationId: number) => {
  try {
    await axios.delete(`/ai/conversations/${conversationId}`)
    if (currentConversationId.value === conversationId) startNewAiConversation()
    await loadAiConversations()
  } catch (err: any) {
    ElMessage.error(err.error || '删除对话失败，请稍后重试')
  }
}

const confirmAiPendingAction = async (action: AiPendingAction) => {
  if (aiActionLoading.value) return
  try {
    await ElMessageBox.confirm(`将执行：${action.summary}。字段已在下方预览，执行后会新增真实业务数据。`, '确认执行 AI 操作', {
      confirmButtonText: '确认执行', cancelButtonText: '返回核对', type: 'warning', customClass: 'custom-confirm-box'
    })
  } catch {
    return
  }
  aiActionLoading.value = action.actionId
  try {
    const res = await axios.post(`/ai/actions/${action.actionId}/confirm`)
    aiPendingActions.value = aiPendingActions.value.filter(item => item.actionId !== action.actionId)
    ElMessage.success(res.data.message || '操作已执行')
  } catch (err: any) {
    ElMessage.error(err.error || '操作执行失败，请重新生成草稿')
    await loadAiPendingActions()
  } finally {
    aiActionLoading.value = null
  }
}

const cancelAiPendingAction = async (action: AiPendingAction) => {
  if (aiActionLoading.value) return
  aiActionLoading.value = action.actionId
  try {
    await axios.delete(`/ai/actions/${action.actionId}`)
    aiPendingActions.value = aiPendingActions.value.filter(item => item.actionId !== action.actionId)
    ElMessage.info('已取消待确认操作')
  } catch (err: any) {
    ElMessage.error(err.error || '取消操作失败')
    await loadAiPendingActions()
  } finally {
    aiActionLoading.value = null
  }
}

const submitAiFeedback = async (message: AiMessage, rating: 'UP' | 'DOWN') => {
  if (!message.agentRunId || aiFeedbackLoading.value) return
  aiFeedbackLoading.value = message.agentRunId
  try {
    await axios.post(`/ai/agent-runs/${message.agentRunId}/feedback`, { rating })
    message.feedbackRating = rating
    ElMessage.success(rating === 'UP' ? '感谢你的认可' : '反馈已记录，我们会持续改进')
  } catch (err: any) {
    ElMessage.error(err.error || '反馈保存失败，请稍后重试')
  } finally {
    aiFeedbackLoading.value = null
  }
}

const sendAiMessage = async () => {
  const message = aiInput.value.trim()
  if (!message || aiLoading.value) return
  aiMessages.value.push({ role: 'user', content: message })
  aiInput.value = ''
  aiLoading.value = true
  aiStreamStatus.value = '正在连接 AI 服务…'
  const assistantMessage: AiMessage = { role: 'assistant', content: '', toolCalls: [], streaming: true }
  aiMessages.value.push(assistantMessage)
  await scrollAiMessages()
  let completed = false
  try {
    const token = userStore.token || localStorage.getItem('token')
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ message, conversationId: currentConversationId.value, page: route.path })
    })
    if (!response.ok || !response.body) {
      const errorBody = await response.json().catch(() => null)
      if (response.status === 401) {
        userStore.clearLoginInfo()
        router.push('/login')
      }
      throw new Error(errorBody?.message || `AI 服务请求失败（HTTP ${response.status}）`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    const consumeEvent = async (rawEvent: string) => {
      const lines = rawEvent.replace(/\r/g, '').split('\n')
      const eventName = lines.find(line => line.startsWith('event:'))?.slice(6).trim() || 'message'
      const dataText = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trimStart()).join('\n')
      if (!dataText) return
      let data: any
      try { data = JSON.parse(dataText) } catch { return }
      if (eventName === 'status') {
        aiStreamStatus.value = data.stage || '正在思考…'
      } else if (eventName === 'delta') {
        assistantMessage.content += data.content || ''
        aiStreamStatus.value = ''
      } else if (eventName === 'reset') {
        assistantMessage.content = ''
      } else if (eventName === 'tool') {
        assistantMessage.toolCalls = [...(assistantMessage.toolCalls || []), data as AiToolCall]
      } else if (eventName === 'done') {
        const done = data as AiStreamDone
        completed = true
        assistantMessage.content = done.answer || assistantMessage.content
        assistantMessage.toolCalls = done.toolCalls || assistantMessage.toolCalls
        assistantMessage.agentRunId = done.agentRunId || undefined
        currentConversationId.value = done.conversationId
        if (done.pendingActions?.length) {
          const existing = new Set(aiPendingActions.value.map(item => item.actionId))
          aiPendingActions.value = [...done.pendingActions.filter(item => !existing.has(item.actionId)), ...aiPendingActions.value]
        }
        if (done.memoryMessage) {
          const notify = done.memorySaved ? ElMessage.success : ElMessage.info
          notify(done.memoryMessage)
        }
      } else if (eventName === 'error') {
        throw new Error(data.message || 'AI 流式响应异常，请稍后重试')
      }
      await scrollAiMessages()
    }
    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      let separatorIndex: number
      let separator: RegExpMatchArray | null
      while ((separatorIndex = buffer.search(/\r?\n\r?\n/)) >= 0 && (separator = buffer.match(/\r?\n\r?\n/))) {
        const rawEvent = buffer.slice(0, separatorIndex)
        buffer = buffer.slice(separatorIndex + separator[0].length)
        await consumeEvent(rawEvent)
      }
      if (done) break
    }
    if (buffer.trim()) await consumeEvent(buffer)
    if (!completed) throw new Error('AI 响应未正常结束，请稍后重试')
    await loadAiConversations()
  } catch (err: any) {
    assistantMessage.content = err.error || err.message || 'AI 助手暂时不可用，请稍后重试。'
  } finally {
    assistantMessage.streaming = false
    aiLoading.value = false
    aiStreamStatus.value = ''
    await scrollAiMessages()
  }
}
</script>

<style scoped>
:deep(.el-dropdown-menu) {
  background-color: hsl(222, 47%, 14%) !important;
  border: 1px solid rgba(99, 102, 241, 0.25) !important;
}
:deep(.el-dropdown-menu__item) {
  color: #e2e8f0 !important;
}
:deep(.el-dropdown-menu__item:hover) {
  background-color: rgba(99, 102, 241, 0.15) !important;
}

.ai-float-button {
  position: fixed;
  right: auto;
  bottom: auto;
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(103, 232, 249, 0.4);
  border-radius: 999px;
  padding: 12px 17px;
  color: #ecfeff;
  font-size: 13px;
  font-weight: 600;
  cursor: grab;
  user-select: none;
  touch-action: none;
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.95), rgba(79, 70, 229, 0.95));
  box-shadow: 0 12px 30px rgba(8, 145, 178, 0.32);
  transition: transform 160ms ease, box-shadow 160ms ease;
}
.ai-float-button:hover { transform: translateY(-2px); box-shadow: 0 16px 34px rgba(8, 145, 178, 0.45); }
.ai-float-button.is-dragging { cursor: grabbing; transform: none; transition: none; }
.ai-panel {
  position: fixed;
  right: auto;
  bottom: auto;
  z-index: 40;
  display: flex;
  flex-direction: column;
  width: min(390px, calc(100vw - 32px));
  height: min(570px, calc(100vh - 120px));
  overflow: hidden;
  border: 1px solid rgba(99, 102, 241, 0.35);
  border-radius: 18px;
  background: rgba(12, 18, 38, 0.98);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.52);
  backdrop-filter: blur(18px);
}
.ai-panel-header { display: flex; align-items: center; justify-content: space-between; padding: 15px 17px; border-bottom: 1px solid rgba(99, 102, 241, 0.18); cursor: grab; user-select: none; touch-action: none; background: linear-gradient(135deg, rgba(30, 41, 100, 0.75), rgba(8, 47, 73, 0.55)); }
.ai-panel.is-dragging .ai-panel-header { cursor: grabbing; }
.ai-header-button { border: 0; padding: 4px 6px; color: #94a3b8; background: transparent; cursor: pointer; }
.ai-header-button:hover { color: #e2e8f0; }
.ai-messages { flex: 1; overflow-y: auto; padding: 16px; scroll-behavior: smooth; }
.ai-history { flex: 1; overflow-y: auto; padding: 14px; }
.ai-memory { flex: 1; overflow-y: auto; padding: 14px; }
.ai-history-heading { display: flex; justify-content: space-between; align-items: center; margin: 2px 2px 10px; color: #cbd5e1; font-size: 12px; font-weight: 600; }
.ai-history-list { display: flex; flex-direction: column; gap: 7px; }
.ai-history-empty { padding: 22px 8px; color: #64748b; font-size: 12px; text-align: center; }
.ai-history-item { display: flex; align-items: stretch; overflow: hidden; border: 1px solid rgba(99, 102, 241, 0.18); border-radius: 9px; background: rgba(15, 23, 42, 0.48); }
.ai-history-item.active { border-color: rgba(34, 211, 238, 0.46); background: rgba(8, 47, 73, 0.28); }
.ai-history-open { min-width: 0; flex: 1; padding: 9px 10px; border: 0; color: inherit; text-align: left; cursor: pointer; background: transparent; }
.ai-history-open:hover { background: rgba(30, 41, 59, 0.55); }
.ai-history-title, .ai-history-preview { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-history-title { color: #cbd5e1; font-size: 12px; }
.ai-history-preview { margin-top: 3px; color: #64748b; font-size: 10px; }
.ai-history-delete { width: 30px; border: 0; color: #64748b; font-size: 18px; cursor: pointer; background: transparent; }
.ai-history-delete:hover { color: #f87171; background: rgba(127, 29, 29, 0.18); }
.ai-memory-tip { margin: 0 2px 11px; color: #64748b; font-size: 11px; line-height: 1.55; }
.ai-memory-editor { padding: 10px; border: 1px solid rgba(99, 102, 241, .2); border-radius: 10px; background: rgba(15, 23, 42, .48); }
.ai-memory-select { width: 100%; box-sizing: border-box; margin-bottom: 8px; border: 1px solid rgba(99, 102, 241, .25); border-radius: 7px; padding: 7px 8px; color: #cbd5e1; font-size: 12px; outline: none; background: rgba(15, 23, 42, .78); }
.ai-memory-input { width: 100%; box-sizing: border-box; resize: vertical; border: 1px solid rgba(99, 102, 241, .25); border-radius: 7px; outline: none; padding: 8px; color: #e2e8f0; font: inherit; font-size: 12px; line-height: 1.45; background: rgba(15, 23, 42, .78); }
.ai-memory-input:focus, .ai-memory-select:focus { border-color: rgba(34, 211, 238, .6); }
.ai-memory-list { display: flex; flex-direction: column; gap: 7px; margin-top: 12px; }
.ai-memory-item { display: flex; gap: 7px; align-items: flex-start; padding: 9px 5px 9px 10px; border: 1px solid rgba(99, 102, 241, .18); border-radius: 9px; background: rgba(15, 23, 42, .48); }
.ai-memory-item p { margin: 6px 0 0; color: #cbd5e1; font-size: 12px; line-height: 1.55; white-space: pre-wrap; word-break: break-word; }
.ai-memory-type { display: inline-block; border-radius: 999px; padding: 2px 6px; color: #67e8f9; font-size: 10px; background: rgba(8, 145, 178, .18); }
.ai-welcome { padding: 14px; border: 1px solid rgba(34, 211, 238, 0.16); border-radius: 12px; color: #94a3b8; font-size: 12px; line-height: 1.65; background: rgba(8, 47, 73, 0.2); }
.ai-message { display: flex; gap: 8px; margin: 0 0 13px; align-items: flex-start; }
.ai-message.user { flex-direction: row-reverse; }
.ai-message-role { flex: 0 0 24px; width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center; color: #dbeafe; font-size: 10px; font-weight: 700; background: rgba(79, 70, 229, 0.52); }
.ai-message.user .ai-message-role { background: rgba(8, 145, 178, 0.62); }
.ai-message-content { max-width: 285px; padding: 9px 11px; border-radius: 10px; color: #dbeafe; font-size: 12px; line-height: 1.65; white-space: pre-wrap; background: rgba(30, 41, 59, 0.72); }
.ai-streaming-message { color: #94a3b8; }
.ai-message.user .ai-message-content { color: #ecfeff; background: rgba(8, 145, 178, 0.3); }
.ai-tool-calls { display: flex; flex-direction: column; gap: 4px; max-width: 285px; margin-top: 6px; }
.ai-tool-call { display: flex; flex-direction: column; gap: 2px; padding: 6px 8px; border: 1px solid rgba(34, 211, 238, .2); border-radius: 7px; color: #a5f3fc; font-size: 10px; line-height: 1.4; background: rgba(8, 47, 73, .38); }
.ai-tool-call span:last-child { color: #94a3b8; }
.ai-tool-call.failed { border-color: rgba(251, 113, 133, .28); color: #fda4af; background: rgba(76, 5, 25, .25); }
.ai-feedback { display: flex; align-items: center; gap: 6px; margin: 6px 3px 0; color: #64748b; font-size: 10px; }
.ai-feedback button { border: 0; padding: 1px 3px; cursor: pointer; filter: grayscale(.2); background: transparent; }
.ai-feedback button:hover { filter: none; transform: scale(1.12); }
.ai-feedback button:disabled { cursor: wait; opacity: .5; }
.ai-feedback-thanks { color: #67e8f9; }
.ai-pending-actions { display: flex; flex-direction: column; gap: 7px; margin: 0 0 13px; }
.ai-pending-heading { color: #fcd34d; font-size: 11px; font-weight: 700; }
.ai-pending-heading span { margin-left: 5px; color: #64748b; font-weight: 400; }
.ai-pending-action { padding: 10px; border: 1px solid rgba(251, 191, 36, .35); border-radius: 10px; color: #e2e8f0; background: rgba(120, 53, 15, .18); }
.ai-pending-title { color: #fde68a; font-size: 12px; font-weight: 700; }
.ai-pending-action > p { margin: 4px 0 8px; color: #cbd5e1; font-size: 11px; line-height: 1.45; }
.ai-pending-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px 8px; padding: 7px; border-radius: 7px; background: rgba(15, 23, 42, .42); }
.ai-pending-fields div { min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.ai-pending-fields span { color: #94a3b8; font-size: 10px; }
.ai-pending-fields strong { overflow: hidden; color: #e2e8f0; font-size: 10px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.ai-pending-expiry { margin-top: 8px; color: #fbbf24; font-size: 10px; }
.ai-pending-buttons { display: flex; justify-content: flex-end; gap: 7px; margin-top: 8px; }
.ai-pending-cancel { border: 1px solid rgba(148, 163, 184, .3); border-radius: 7px; padding: 6px 9px; color: #cbd5e1; font-size: 11px; cursor: pointer; background: rgba(15, 23, 42, .45); }
.ai-pending-cancel:disabled { cursor: not-allowed; opacity: .5; }
.ai-composer { padding: 12px 14px; border-top: 1px solid rgba(99, 102, 241, 0.18); background: rgba(2, 6, 23, 0.4); }
.ai-input { width: 100%; box-sizing: border-box; resize: none; border: 1px solid rgba(99, 102, 241, 0.25); border-radius: 10px; outline: none; padding: 9px 10px; color: #e2e8f0; font: inherit; font-size: 12px; line-height: 1.45; background: rgba(15, 23, 42, 0.75); }
.ai-input:focus { border-color: rgba(34, 211, 238, 0.6); }
.ai-send-button { border: 0; border-radius: 7px; padding: 6px 13px; color: white; font-size: 12px; cursor: pointer; background: linear-gradient(135deg, #0891b2, #4f46e5); }
.ai-send-button:disabled { cursor: not-allowed; opacity: 0.48; }
</style>
