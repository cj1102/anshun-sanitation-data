<template>
  <div class="space-y-6">
    <!-- Charts Row -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Hot Area Performance Chart -->
      <div class="p-6 rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl flex flex-col h-[450px]">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-cyan-400"></span>
            <h4 class="text-base font-bold text-slate-200">热门路段区域承租率与均价对比</h4>
          </div>
        </div>
        <div ref="hotAreaChartRef" class="flex-1 min-h-0"></div>
      </div>

      <!-- Top Corporate Ad Spend Budget -->
      <div class="p-6 rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl flex flex-col h-[450px]">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-violet-400"></span>
            <h4 class="text-base font-bold text-slate-200">2025年度广告投入总额 TOP 10 企业</h4>
          </div>
        </div>
        <div ref="topEnterpriseChartRef" class="flex-1 min-h-0"></div>
      </div>
    </div>

    <!-- Audience Analysis Cards Grid -->
    <div class="space-y-4">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-indigo-500 animate-pulse"></span>
        <h4 class="text-base font-bold text-slate-200">热门区域受众特征与运行指标</h4>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <div 
          v-for="area in hotAreas" 
          :key="area.area_name"
          class="p-4 rounded-xl border border-indigo-500/10 bg-slate-900/20 backdrop-blur-sm space-y-3 shadow-md transition-all duration-300 hover:border-indigo-500/25"
        >
          <div class="font-semibold text-slate-200 text-xs truncate">{{ area.area_name }}</div>
          <div class="space-y-1">
            <div class="text-[11px] text-slate-400 flex justify-between">
              <span>平均承租率:</span>
              <span class="font-bold text-cyan-400 font-mono">{{ area.rent_rate }}%</span>
            </div>
            <div class="text-[11px] text-slate-400 flex justify-between">
              <span>周转率:</span>
              <span class="font-bold text-violet-400 font-mono">{{ area.turnover_rate }}%</span>
            </div>
            <div class="text-[11px] text-slate-400 flex justify-between">
              <span>均租金:</span>
              <span class="font-bold text-emerald-400 font-mono">{{ area.avg_rent }}元</span>
            </div>
          </div>
          <div class="pt-2 border-t border-indigo-500/5">
            <div class="text-[10px] text-slate-400 font-bold uppercase mb-1">主要受众：</div>
            <div class="text-[10px] text-slate-300 leading-normal">{{ getAudienceType(area.area_name) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import axios from '../utils/axios'
import { ElMessage } from 'element-plus'

const hotAreaChartRef = ref<HTMLDivElement>()
const topEnterpriseChartRef = ref<HTMLDivElement>()

let hotAreaChart: echarts.ECharts | null = null
let topEnterpriseChart: echarts.ECharts | null = null

const hotAreas = ref<any[]>([])

const getAudienceType = (name: string) => {
  if (name.includes('黄果树')) return '城市通勤人群、商业消费人群'
  if (name.includes('二环')) return '周边居民、物流运输司机'
  if (name.includes('迎辉')) return '进出城人群、通勤客流'
  if (name.includes('两六路')) return '产业园区员工、货运司机'
  if (name.includes('龙宫')) return '旅游景区游客、本地居民'
  if (name.includes('武当山')) return '行政办公人员、市民客流'
  if (name.includes('贵安')) return '往返贵阳安顺人群、城际客流'
  if (name.includes('沪昆')) return '高速长途旅客、跨省物流司机'
  if (name.includes('安普')) return '城际短途旅客、自驾游人群'
  if (name.includes('安紫')) return '往来安顺紫云人群、旅游客流'
  return '城市多元化受众、通用客流'
}

const fetchData = async () => {
  try {
    const [hotRes, topRes] = await Promise.all([
      axios.get('/stats/hot-areas'),
      axios.get('/stats/top-enterprises')
    ])

    hotAreas.value = hotRes.data
    
    initHotAreaChart(hotRes.data)
    initTopEnterpriseChart(topRes.data)
  } catch (err: any) {
    ElMessage.error(err.error || '获取分析统计数据失败')
  }
}

const initHotAreaChart = (data: any[]) => {
  if (!hotAreaChartRef.value) return

  const areaNames = data.map((item: any) => item.area_name.replace('西秀区', '').replace('沿线', ''))
  const rentRates = data.map((item: any) => item.rent_rate)
  const avgRents = data.map((item: any) => item.avg_rent)

  hotAreaChart = echarts.init(hotAreaChartRef.value)

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      borderColor: 'rgba(99, 102, 241, 0.4)',
      textStyle: { color: '#f8fafc', fontSize: 12 }
    },
    legend: {
      data: ['平均承租率', '单价均价'],
      textStyle: { color: '#94a3b8', fontSize: 11 },
      top: '0%'
    },
    grid: { left: '4%', right: '4%', top: '15%', bottom: '15%' },
    xAxis: [
      {
        type: 'category',
        data: areaNames,
        axisPointer: { type: 'shadow' },
        axisLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.2)' } },
        axisLabel: { 
          color: '#94a3b8',
          fontSize: 9,
          rotate: 30
        }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '承租率 (%)',
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}%', color: '#94a3b8' },
        splitLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.03)' } }
      },
      {
        type: 'value',
        name: '年租金 (元/㎡)',
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        axisLabel: { formatter: '{value}元', color: '#94a3b8' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '平均承租率',
        type: 'bar',
        data: rentRates,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#06b6d4' },
            { offset: 1, color: 'rgba(6, 182, 212, 0.2)' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '单价均价',
        type: 'line',
        yAxisIndex: 1,
        data: avgRents,
        smooth: true,
        symbolSize: 6,
        itemStyle: { color: '#fbbf24' },
        lineStyle: { width: 2.5, color: '#fbbf24' }
      }
    ]
  }

  hotAreaChart.setOption(option)
}

const initTopEnterpriseChart = (data: any[]) => {
  if (!topEnterpriseChartRef.value) return

  // Sort ascending for bottom-up horizontal bar charting
  const sorted = [...data].reverse()
  const names = sorted.map((item: any) => item.lessee_company)
  const spend = sorted.map((item: any) => Number(item.total_rent))

  topEnterpriseChart = echarts.init(topEnterpriseChartRef.value)

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      borderColor: 'rgba(99, 102, 241, 0.4)',
      textStyle: { color: '#f8fafc', fontSize: 12 },
      formatter: '{b}: {c} 万元'
    },
    grid: { left: '3%', right: '8%', top: '5%', bottom: '8%', containLabel: true },
    xAxis: {
      type: 'value',
      name: '投入总额 (万元)',
      nameTextStyle: { color: '#64748b', fontSize: 10 },
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.05)' } }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { 
        color: '#94a3b8',
        fontSize: 10,
        formatter: (val: string) => val.length > 8 ? `${val.substring(0, 8)}...` : val
      },
      axisLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.2)' } }
    },
    series: [
      {
        name: '广告投入',
        type: 'bar',
        data: spend,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(139, 92, 246, 0.2)' },
            { offset: 1, color: '#8b5cf6' }
          ]),
          borderRadius: [0, 4, 4, 0]
        },
        label: {
          show: true,
          position: 'right',
          color: '#e2e8f0',
          fontSize: 10,
          fontFamily: 'monospace'
        }
      }
    ]
  }

  topEnterpriseChart.setOption(option)
}

const handleResize = () => {
  hotAreaChart?.resize()
  topEnterpriseChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  hotAreaChart?.dispose()
  topEnterpriseChart?.dispose()
})
</script>

<style scoped>
</style>
