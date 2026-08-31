import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// Route-level code splitting keeps charts and administration pages out of the login bundle.
const Login = () => import('./views/Login.vue')
const Layout = () => import('./views/Layout.vue')
const Dashboard = () => import('./views/Dashboard.vue')
const AdPositionList = () => import('./views/AdPositionList.vue')
const LeaseDetailList = () => import('./views/LeaseDetailList.vue')
const Analytics = () => import('./views/Analytics.vue')
const SystemUsers = () => import('./views/SystemUsers.vue')
const AuditLogs = () => import('./views/AuditLogs.vue')
const KnowledgeBase = () => import('./views/KnowledgeBase.vue')
const AgentEvaluation = () => import('./views/AgentEvaluation.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard
      },
      {
        path: 'positions',
        name: 'Positions',
        component: AdPositionList
      },
      {
        path: 'leases',
        name: 'Leases',
        component: LeaseDetailList
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: Analytics
      },
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: SystemUsers
      },
      {
        path: 'system/audit-logs',
        name: 'AuditLogs',
        component: AuditLogs
      },
      {
        path: 'knowledge',
        name: 'KnowledgeBase',
        component: KnowledgeBase
      },
      {
        path: 'ai/evaluation',
        name: 'AgentEvaluation',
        component: AgentEvaluation
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
