<template>
  <div class="space-y-6">
    <!-- Stat Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div v-for="stat in statsCards" :key="stat.title" class="p-6 rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md flex items-center justify-between shadow-xl transition-all duration-300 hover:border-indigo-500/30 hover:-translate-y-1">
        <div class="space-y-2">
          <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider">{{ stat.title }}</span>
          <h3 class="text-3xl font-extrabold text-white font-mono tracking-tight">{{ stat.value }}</h3>
        </div>
        <div class="w-12 h-12 rounded-xl flex items-center justify-center bg-gradient-to-tr" :class="stat.gradient">
          <el-icon class="text-white text-xl"><component :is="stat.icon" /></el-icon>
        </div>
      </div>
    </div>

    <!-- Main Grid: Map & Industry Pie -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Road Distribution Bar Chart -->
      <div class="lg:col-span-2 p-6 rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl flex flex-col h-[500px]">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-cyan-400 animate-pulse"></span>
            <h4 class="text-base font-bold text-slate-200">主要路段广告点位分布与租赁状态</h4>
          </div>
        </div>
        <div ref="mapChartRef" class="flex-1 min-h-0"></div>
      </div>

      <!-- Industry Pie Chart -->
      <div class="p-6 rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl flex flex-col h-[500px]">
        <div class="flex items-center gap-2 mb-4">
          <span class="w-2.5 h-2.5 rounded-full bg-indigo-400"></span>
          <h4 class="text-base font-bold text-slate-200">承租行业广告租金贡献分布</h4>
        </div>
        <div ref="pieChartRef" class="flex-1 min-h-0"></div>
      </div>
    </div>

    <!-- Revenue Trend Chart -->
    <div class="p-6 rounded-2xl border border-indigo-500/10 bg-slate-900/40 backdrop-blur-md shadow-xl h-[380px] flex flex-col">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <span class="w-2.5 h-2.5 rounded-full bg-violet-400"></span>
          <h4 class="text-base font-bold text-slate-200">历年广告租金收入大盘走势</h4>
        </div>
        <div class="text-xs text-slate-400 font-medium">统计单位：万元</div>
      </div>
      <div ref="trendChartRef" class="flex-1 min-h-0"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import axios from '../utils/axios'
import { ElMessage } from 'element-plus'

const mapChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()

let mapChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const overview = ref({
  totalPositions: 0,
  leasedPositions: 0,
  leasedRate: 0,
  totalRevenue: 0,
  arrearsAmount: 0
})

const statsCards = ref([
  { title: '广告位点位总量', value: '0 个', icon: 'MapLocation', gradient: 'from-blue-500 to-cyan-500' },
  { title: '点位实时租赁率', value: '0.00%', icon: 'Checked', gradient: 'from-emerald-500 to-teal-500' },
  { title: '年度累计收入', value: '0.00 万元', icon: 'Money', gradient: 'from-amber-500 to-orange-500' },
  { title: '欠费与待回收额', value: '0.00 万元', icon: 'Warning', gradient: 'from-red-500 to-rose-500' }
])

const updateCards = () => {
  statsCards.value[0].value = `${overview.value.totalPositions} 个`
  statsCards.value[1].value = `${overview.value.leasedRate.toFixed(2)}%`
  statsCards.value[2].value = `${overview.value.totalRevenue.toLocaleString()} 万元`
  statsCards.value[3].value = `${overview.value.arrearsAmount.toLocaleString()} 万元`
}

// Fetch dashboard statistical details
const fetchData = async () => {
  try {
    const ovRes = await axios.get('/stats/overview')
    overview.value = ovRes.data
    updateCards()

    // Draw charts after fetching
    await Promise.all([
      initMapChart(),
      initPieChart(),
      initTrendChart()
    ])
  } catch (err: any) {
    ElMessage.error(err.error || '获取统计概览失败')
  }
}

const initMapChart = async () => {
  if (!mapChartRef.value) return
  
  const mapRes = await axios.get('/stats/map-positions')
  const positions = mapRes.data

  // Group counts by road name
  const roadCountsMap: Record<string, { leased: number; vacant: number; total: number }> = {}
  
  positions.forEach((p: any) => {
    const road = p.road_name || '其他路段'
    if (!roadCountsMap[road]) {
      roadCountsMap[road] = { leased: 0, vacant: 0, total: 0 }
    }
    if (p.status === 'leased') {
      roadCountsMap[road].leased++
    } else {
      roadCountsMap[road].vacant++
    }
    roadCountsMap[road].total++
  })

  // Sort roads from lowest to highest total count so highest displays at the top of the horizontal bar chart
  const sortedRoads = Object.keys(roadCountsMap).sort((a, b) => roadCountsMap[a].total - roadCountsMap[b].total)

  const leasedCounts = sortedRoads.map(r => roadCountsMap[r].leased)
  const vacantCounts = sortedRoads.map(r => roadCountsMap[r].vacant)

  mapChart = echarts.init(mapChartRef.value)

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      borderColor: 'rgba(99, 102, 241, 0.4)',
      textStyle: { color: '#f8fafc', fontSize: 12 },
      formatter: (params: any) => {
        let total = 0
        let res = `<div class="font-bold text-slate-200 mb-1.5">${params[0].name}</div>`
        params.forEach((item: any) => {
          total += item.value
          const colorDot = `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:${item.color.colorStops ? item.color.colorStops[0].color : item.color};"></span>`
          res += `<div class="flex items-center justify-between gap-4 text-xs mb-1">
            <span>${colorDot}${item.seriesName}</span>
            <span class="font-mono font-bold">${item.value} 个</span>
          </div>`
        })
        res += `<div class="border-t border-slate-700/50 mt-1.5 pt-1.5 flex items-center justify-between gap-4 text-xs font-bold text-cyan-400">
          <span>总计点位</span>
          <span class="font-mono">${total} 个</span>
        </div>`
        return res
      }
    },
    legend: {
      right: '5%',
      top: '0%',
      textStyle: { color: '#94a3b8', fontSize: 11 },
      itemWidth: 12,
      itemHeight: 12
    },
    grid: { left: '3%', right: '5%', top: '10%', bottom: '5%', containLabel: true },
    xAxis: {
      type: 'value',
      name: '点位数量 (个)',
      nameTextStyle: { color: '#64748b', fontSize: 10, align: 'right', padding: [0, 0, 0, 8] },
      axisLine: { show: false },
      axisLabel: { color: '#64748b', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.05)' } }
    },
    yAxis: {
      type: 'category',
      data: sortedRoads,
      axisLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.2)' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 },
      axisTick: { show: false }
    },
    series: [
      {
        name: '已租赁',
        type: 'bar',
        stack: 'total',
        barWidth: '45%',
        data: leasedCounts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#059669' },
            { offset: 1, color: '#10b981' }
          ])
        }
      },
      {
        name: '空置',
        type: 'bar',
        stack: 'total',
        barWidth: '45%',
        data: vacantCounts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(71, 85, 105, 0.2)' },
            { offset: 1, color: 'rgba(148, 163, 184, 0.45)' }
          ]),
          borderRadius: [0, 4, 4, 0]
        }
      }
    ]
  }

  mapChart.setOption(option)
}

const initPieChart = async () => {
  if (!pieChartRef.value) return

  const indRes = await axios.get('/stats/industry-distribution')
  const chartData = indRes.data.map((item: any) => ({
    name: item.industry_name,
    value: Number(item.total_rent)
  }))

  pieChart = echarts.init(pieChartRef.value)

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      borderColor: 'rgba(99, 102, 241, 0.4)',
      textStyle: { color: '#f8fafc', fontSize: 12 },
      formatter: '{b} : {c} 万元 ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: '0%',
      textStyle: { color: '#94a3b8', fontSize: 10 },
      itemWidth: 10,
      itemHeight: 10
    },
    series: [
      {
        name: '租金贡献',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: 'hsl(222, 47%, 11%)',
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold',
            color: '#f8fafc'
          }
        },
        data: chartData
      }
    ]
  }

  pieChart.setOption(option)
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return

  const trendRes = await axios.get('/stats/revenue-trend')
  const trendData = trendRes.data

  const years = trendData.map((item: any) => `${item.year}年`)
  const revenues = trendData.map((item: any) => item.revenue)

  trendChart = echarts.init(trendChartRef.value)

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      borderColor: 'rgba(99, 102, 241, 0.4)',
      textStyle: { color: '#f8fafc', fontSize: 12 }
    },
    grid: { left: '4%', right: '4%', top: '10%', bottom: '12%' },
    xAxis: {
      type: 'category',
      data: years,
      axisLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.2)' } },
      axisLabel: { color: '#94a3b8' },
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.05)' } }
    },
    series: [
      {
        name: '年度收入',
        type: 'line',
        data: revenues,
        smooth: true,
        showSymbol: true,
        symbolSize: 8,
        itemStyle: { color: '#8b5cf6' },
        lineStyle: { width: 3, color: '#8b5cf6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139, 92, 246, 0.4)' },
            { offset: 1, color: 'rgba(139, 92, 246, 0.0)' }
          ])
        }
      }
    ]
  }

  trendChart.setOption(option)
}

const handleResize = () => {
  mapChart?.resize()
  pieChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  mapChart?.dispose()
  pieChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
</style>
