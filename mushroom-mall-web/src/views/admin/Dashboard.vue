<template>
  <div class="dashboard">
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
          <div class="metric-icon" style="background:#EDE7F6"><i class="el-icon-money" style="color:#534AB7"></i></div>
          <div class="metric-body">
            <p class="metric-label">总销售额</p>
            <p class="metric-value" style="color:#534AB7">¥{{ fmt(totalSales) }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background:#E8F5E9"><i class="el-icon-document" style="color:#0F6E56"></i></div>
          <div class="metric-body">
            <p class="metric-label">总订单数</p>
            <p class="metric-value" style="color:#0F6E56">{{ totalOrders }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background:#E3F2FD"><i class="el-icon-user" style="color:#185FA5"></i></div>
          <div class="metric-body">
            <p class="metric-label">注册用户</p>
            <p class="metric-value" style="color:#185FA5">{{ totalUsers }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background:#FCE4EC"><i class="el-icon-goods" style="color:#D4537E"></i></div>
          <div class="metric-body">
            <p class="metric-label">商品总数</p>
            <p class="metric-value" style="color:#D4537E">{{ totalProducts }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 当日/当月/范围 销售额+利润 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :span="8">
        <el-card shadow="hover" class="mini-card">
          <p class="mini-label">当日营业额</p>
          <p class="mini-value">¥{{ fmt(todaySales) }}</p>
          <p class="mini-sub">预估利润 <span style="color:#7B1FA2;font-weight:600">¥{{ fmt(todayProfit) }}</span></p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="mini-card">
          <p class="mini-label">当月营业额</p>
          <p class="mini-value">¥{{ fmt(monthSales) }}</p>
          <p class="mini-sub">预估利润 <span style="color:#7B1FA2;font-weight:600">¥{{ fmt(monthProfit) }}</span></p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="mini-card">
          <p class="mini-label">{{ rangeLabel }}营业额</p>
          <p class="mini-value">¥{{ fmt(rangeSales) }}</p>
          <p class="mini-sub">预估利润 <span style="color:#7B1FA2;font-weight:600">¥{{ fmt(rangeProfit) }}</span></p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表第一行 -->
    <el-row :gutter="16" style="margin-bottom:16px" v-loading="loading">
      <el-col :span="14">
        <el-card shadow="never">
          <div slot="header"><b>销售额趋势</b> <span style="color:#999;font-size:12px">({{ rangeLabel }})</span></div>
          <div ref="lineChart" style="height:320px;"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <div slot="header"><b>热销品类占比</b></div>
          <div ref="pieChart" style="height:320px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表第二行 -->
    <el-row :gutter="16" style="margin-bottom:16px" v-loading="loading">
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header"><b>热销商品 TOP5</b></div>
          <div ref="barChart" style="height:300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header"><b>订单量趋势</b> <span style="color:#999;font-size:12px">({{ rangeLabel }})</span></div>
          <div ref="ringChart" style="height:300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表第三行：订单状态分布饼图 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="24">
        <el-card shadow="never">
          <div slot="header"><b>订单状态分布</b></div>
          <div ref="statusPie" style="height:280px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import request from '@/utils/request'

export default {
  name: 'Dashboard',
  data() {
    return {
      loading: true,
      range: '7days',
      totalSales: 0,
      totalOrders: 0,
      totalUsers: 0,
      totalProducts: 0,
      todaySales: 0,
      todayProfit: 0,
      monthSales: 0,
      monthProfit: 0,
      rangeSales: 0,
      rangeProfit: 0
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
      return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    async loadData() {
      this.loading = true
      try {
        const res = await request.get('/statistics/dashboard', { params: { range: this.range } })
        const d = res.data || {}
        this.totalSales = d.totalSales || 0
        this.totalOrders = d.totalOrders || 0
        this.totalUsers = d.totalUsers || 0
        this.totalProducts = d.totalProducts || 0
        this.todaySales = d.todaySales || 0
        this.todayProfit = d.todayProfit || 0
        this.monthSales = d.monthSales || 0
        this.monthProfit = d.monthProfit || 0
        this.rangeSales = d.rangeSales || 0
        this.rangeProfit = d.rangeProfit || 0

        this.$nextTick(() => {
          this.initLineChart(d.salesTrend || [])
          this.initPieChart(d.categorySales || [])
          this.initBarChart(d.topProducts || [])
          this.initRingChart(d.orderTrend || [])
          this.initStatusPie(d.orderStatusDist || {})
        })
      } catch (e) {
        this.$nextTick(() => {
          this.initLineChart([])
          this.initPieChart([])
          this.initBarChart([])
          this.initRingChart([])
          this.initStatusPie({})
        })
      } finally {
        this.loading = false
      }
    },
    resizeCharts() {
      [this.$refs.lineChart, this.$refs.pieChart, this.$refs.barChart, this.$refs.ringChart, this.$refs.statusPie].forEach(ref => {
        if (ref) { const c = echarts.getInstanceByDom(ref); if (c) c.resize() }
      })
    },
    initLineChart(trend) {
      const chart = echarts.init(this.$refs.lineChart)
      chart.setOption({
        tooltip: { trigger: 'axis', formatter: p => `${p[0].axisValue}<br/>销售额: ¥${Number(p[0].value).toFixed(2)}` },
        xAxis: { type: 'category', data: trend.map(t => t.date), axisLabel: { rotate: trend.length > 10 ? 30 : 0 } },
        yAxis: { type: 'value', name: '元' },
        grid: { left: 50, right: 20, bottom: 40 },
        series: [{
          name: '销售额', type: 'line', smooth: true,
          data: trend.map(t => t.amount || 0),
          itemStyle: { color: '#534AB7' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(83,74,183,0.25)' }, { offset: 1, color: 'rgba(83,74,183,0.02)' }]) }
        }]
      })
    },
    initPieChart(categorySales) {
      const chart = echarts.init(this.$refs.pieChart)
      const pieData = categorySales.map(c => ({ value: c.value || c.sales || 0, name: c.name }))
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}kg ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'],
          data: pieData.length > 0 ? pieData : [{ value: 1, name: '暂无数据' }],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{d}%', fontSize: 11 },
          color: ['#534AB7', '#0F6E56', '#F39C12', '#E74C3C', '#3498DB', '#9B59B6', '#1ABC9C', '#FF9800']
        }]
      })
    },
    initBarChart(topProducts) {
      const chart = echarts.init(this.$refs.barChart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'value', name: '销量' },
        yAxis: { type: 'category', data: topProducts.map(p => p.name).reverse(), axisLabel: { width: 80, overflow: 'truncate' } },
        grid: { left: 100, right: 40, bottom: 30 },
        series: [{
          type: 'bar',
          data: topProducts.map(p => p.saleCount || 0).reverse(),
          itemStyle: { color: '#0F6E56', borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: 'right', formatter: '{c}kg' }
        }]
      })
    },
    initRingChart(orderTrend) {
      const chart = echarts.init(this.$refs.ringChart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: orderTrend.map(t => t.date), axisLabel: { rotate: orderTrend.length > 10 ? 30 : 0 } },
        yAxis: { type: 'value', name: '单' },
        grid: { left: 45, right: 20, bottom: 40 },
        series: [{
          type: 'bar',
          data: orderTrend.map(t => t.count || 0),
          itemStyle: { color: '#185FA5', borderRadius: [4, 4, 0, 0] },
          label: { show: true, position: 'top', formatter: '{c}' }
        }]
      })
    },
    initStatusPie(dist) {
      const chart = echarts.init(this.$refs.statusPie)
      const colors = ['#909399', '#E6A23C', '#409EFF', '#67C23A', '#F56C6C', '#FF9800', '#9C27B0']
      const data = Object.entries(dist).map(([name, value], i) => ({ name, value, itemStyle: { color: colors[i % colors.length] } }))
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}单 ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie', radius: ['35%', '60%'], center: ['50%', '45%'],
          data: data.length > 0 ? data : [{ name: '暂无数据', value: 1 }],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}: {c}单', fontSize: 11 },
          color: colors
        }]
      })
    }
  }
}
</script>

<style scoped>
.range-card { margin-bottom: 16px; }
.metric-row { margin-bottom: 16px; }
.metric-card { display: flex; align-items: center; }
.metric-card .el-card__body { display: flex; align-items: center; width: 100%; padding: 16px; }
.metric-icon { width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 24px; flex-shrink: 0; margin-right: 14px; }
.metric-body { flex: 1; }
.metric-label { font-size: 13px; color: #888; margin: 0 0 4px; }
.metric-value { font-size: 22px; font-weight: 700; margin: 0; }
.mini-card .el-card__body { padding: 14px 16px; }
.mini-label { font-size: 12px; color: #999; margin: 0 0 4px; }
.mini-value { font-size: 20px; font-weight: 700; color: #333; margin: 0 0 2px; }
.mini-sub { font-size: 11px; color: #aaa; margin: 0; }
</style>
