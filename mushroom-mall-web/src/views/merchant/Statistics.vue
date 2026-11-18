<template>
  <div class="statistics-page">
    <!-- 时间范围切换 -->
    <el-card shadow="never" class="range-card">
      <el-radio-group v-model="range" @change="loadData" size="small">
        <el-radio-button label="today">当日</el-radio-button>
        <el-radio-button label="7days">近7天</el-radio-button>
        <el-radio-button label="30days">近30天</el-radio-button>
        <el-radio-button label="month">当月</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- 核心指标卡 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background:#E8F5E9"><i class="el-icon-money" style="color:#0F6E56"></i></div>
          <div class="metric-body">
            <p class="metric-label">总销售额</p>
            <p class="metric-value">¥{{ fmt(totalSales) }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background:#E3F2FD"><i class="el-icon-document" style="color:#1976D2"></i></div>
          <div class="metric-body">
            <p class="metric-label">总订单数</p>
            <p class="metric-value">{{ totalOrders }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background:#FFF3E0"><i class="el-icon-coin" style="color:#F57C00"></i></div>
          <div class="metric-body">
            <p class="metric-label">{{ rangeLabel }}销售额</p>
            <p class="metric-value">¥{{ fmt(rangeSales) }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background:#F3E5F5"><i class="el-icon-trophy" style="color:#7B1FA2"></i></div>
          <div class="metric-body">
            <p class="metric-label">{{ rangeLabel }}预估利润</p>
            <p class="metric-value" style="color:#7B1FA2">¥{{ fmt(rangeProfit) }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 当日/当月 快捷对比 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :span="6">
        <el-card shadow="hover" class="mini-card">
          <p class="mini-label">当日销售额</p>
          <p class="mini-value">¥{{ fmt(todaySales) }}</p>
          <p class="mini-sub">利润 ¥{{ fmt(todayProfit) }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-card">
          <p class="mini-label">当月销售额</p>
          <p class="mini-value">¥{{ fmt(monthSales) }}</p>
          <p class="mini-sub">利润 ¥{{ fmt(monthProfit) }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-card">
          <p class="mini-label">在售商品数</p>
          <p class="mini-value">{{ productCount }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-card">
          <p class="mini-label">低库存预警</p>
          <p class="mini-value" :style="{color: lowStockCount > 0 ? '#F56C6C' : '#67C23A'}">{{ lowStockCount }} 件</p>
          <p class="mini-sub" v-if="lowStockCount > 0">库存≤10kg的商品</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表第一行 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="14">
        <el-card shadow="never">
          <div slot="header"><b>销售额趋势</b> <span style="color:#999;font-size:12px">({{ rangeLabel }})</span></div>
          <div ref="lineChart" style="height:320px;"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <div slot="header"><b>热销商品 TOP5</b></div>
          <div ref="barChart" style="height:320px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表第二行：饼图 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header"><b>商品分类销量占比</b></div>
          <div ref="pieChart1" style="height:300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header"><b>订单状态分布</b></div>
          <div ref="pieChart2" style="height:300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import request from '@/utils/request'

export default {
  name: 'MerchantStatistics',
  data() {
    return {
      range: '7days',
      totalSales: 0,
      totalOrders: 0,
      rangeSales: 0,
      rangeProfit: 0,
      todaySales: 0,
      todayProfit: 0,
      monthSales: 0,
      monthProfit: 0,
      productCount: 0,
      lowStockCount: 0,
      salesTrend: [],
      hotProducts: [],
      categorySales: [],
      orderStatusDist: {}
    }
  },
  computed: {
    rangeLabel() {
      return { today: '当日', '7days': '近7天', '30days': '近30天', month: '当月' }[this.range] || '近7天'
    }
  },
  mounted() {
    this.loadData()
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
  },
  methods: {
    fmt(v) {
      return Number(v || 0).toFixed(2)
    },
    async loadData() {
      try {
        const res = await request.get('/statistics/merchant', { params: { range: this.range } })
        const d = res.data || {}
        this.totalSales = d.totalSales || 0
        this.totalOrders = d.totalOrders || 0
        this.rangeSales = d.rangeSales || 0
        this.rangeProfit = d.rangeProfit || 0
        this.todaySales = d.todaySales || 0
        this.todayProfit = d.todayProfit || 0
        this.monthSales = d.monthSales || 0
        this.monthProfit = d.monthProfit || 0
        this.productCount = d.productCount || 0
        this.lowStockCount = d.lowStockCount || 0
        this.salesTrend = d.salesTrend || []
        this.hotProducts = d.topProducts || []
        this.categorySales = d.categorySales || []
        this.orderStatusDist = d.orderStatusDist || {}
      } catch (e) {
        console.error('统计数据加载失败', e)
      }
      this.$nextTick(() => {
        this.initLineChart()
        this.initBarChart()
        this.initPie1()
        this.initPie2()
      })
    },
    resizeCharts() {
      [this.$refs.lineChart, this.$refs.barChart, this.$refs.pieChart1, this.$refs.pieChart2].forEach(ref => {
        if (ref) { const c = echarts.getInstanceByDom(ref); if (c) c.resize() }
      })
    },
    initLineChart() {
      const chart = echarts.init(this.$refs.lineChart)
      chart.setOption({
        tooltip: { trigger: 'axis', formatter: p => `${p[0].axisValue}<br/>销售额: ¥${Number(p[0].value).toFixed(2)}` },
        xAxis: { type: 'category', data: this.salesTrend.map(i => i.date), axisLabel: { rotate: this.salesTrend.length > 10 ? 30 : 0 } },
        yAxis: { type: 'value', name: '元' },
        grid: { left: 50, right: 20, bottom: 40 },
        series: [{
          name: '销售额', type: 'line', smooth: true,
          data: this.salesTrend.map(i => i.amount),
          itemStyle: { color: '#0F6E56' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(15,110,86,0.25)' }, { offset: 1, color: 'rgba(15,110,86,0.02)' }]) }
        }]
      })
    },
    initBarChart() {
      const chart = echarts.init(this.$refs.barChart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'value', name: '销量' },
        yAxis: { type: 'category', data: this.hotProducts.map(i => i.name).reverse(), axisLabel: { width: 80, overflow: 'truncate' } },
        grid: { left: 100, right: 30, bottom: 30 },
        series: [{
          type: 'bar',
          data: this.hotProducts.map(i => i.saleCount).reverse(),
          itemStyle: { color: '#534AB7', borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: 'right', formatter: '{c}kg' }
        }]
      })
    },
    initPie1() {
      const chart = echarts.init(this.$refs.pieChart1)
      const data = this.categorySales.map(i => ({ name: i.name, value: i.value }))
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}kg ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'],
          data: data.length > 0 ? data : [{ name: '暂无数据', value: 1 }],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{d}%', fontSize: 11 },
          color: ['#0F6E56', '#16A085', '#F39C12', '#E74C3C', '#3498DB', '#9B59B6', '#1ABC9C']
        }]
      })
    },
    initPie2() {
      const chart = echarts.init(this.$refs.pieChart2)
      const colors = ['#909399', '#E6A23C', '#409EFF', '#67C23A', '#F56C6C', '#FF9800', '#9C27B0']
      const data = Object.entries(this.orderStatusDist).map(([name, value], i) => ({ name, value, itemStyle: { color: colors[i % colors.length] } }))
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}单 ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'],
          data: data.length > 0 ? data : [{ name: '暂无数据', value: 1 }],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{c}单', fontSize: 11 }
        }]
      })
    }
  }
}
</script>

<style scoped>
.statistics-page { padding: 0; }
.range-card { margin-bottom: 16px; }
.range-card .el-radio-group { width: 100%; }
.metric-row { margin-bottom: 16px; }
.metric-card { display: flex; align-items: center; }
.metric-card .el-card__body { display: flex; align-items: center; width: 100%; padding: 16px; }
.metric-icon { width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 24px; flex-shrink: 0; margin-right: 14px; }
.metric-body { flex: 1; }
.metric-label { font-size: 13px; color: #888; margin: 0 0 4px; }
.metric-value { font-size: 22px; font-weight: 700; color: #333; margin: 0; }
.mini-card .el-card__body { padding: 14px 16px; }
.mini-label { font-size: 12px; color: #999; margin: 0 0 4px; }
.mini-value { font-size: 20px; font-weight: 700; color: #0F6E56; margin: 0 0 2px; }
.mini-sub { font-size: 11px; color: #aaa; margin: 0; }
</style>
