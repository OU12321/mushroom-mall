<template>
  <div class="home">
    <el-card class="welcome-card">
      <div class="welcome">
        <h2>{{ greeting }}，{{ userInfo && userInfo.name }}</h2>
        <p>欢迎使用高山野生菌线上销售商城</p>
        <span class="role-badge" :class="'role-' + role">{{ roleName }}端</span>
      </div>
    </el-card>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="item in statList" :key="item.label">
        <el-card class="stat-card" @click.native="item.path && $router.push(item.path)" :class="{ clickable: !!item.path }">
          <div class="stat-icon" :style="{ background: item.color }">
            <i :class="item.icon"></i>
          </div>
          <div class="stat-info">
            <p class="stat-value">{{ item.value }}</p>
            <p class="stat-label">{{ item.label }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <div slot="header">功能导航</div>
          <div class="quick-nav">
            <div class="nav-item" v-for="nav in quickNavs" :key="nav.path" @click="$router.push(nav.path)">
              <i :class="nav.icon"></i>
              <span>{{ nav.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div slot="header">系统公告</div>
          <div class="notice">
            <p>· 野生菌上市季节，请注意食用安全</p>
            <p>· AI助手已上线，拍照即可辨别毒菌</p>
            <p>· 商家上架商品需经管理员审核</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getHomeData } from '@/api/statistics'

export default {
  name: 'Home',
  data() {
    return {
      statList: []
    }
  },
  computed: {
    userInfo() { return this.$store.state.userInfo },
    role() { return this.userInfo ? this.userInfo.role : null },
    roleName() {
      return { 0: '管理员', 1: '商家', 2: '普通用户' }[this.role] || ''
    },
    greeting() {
      const h = new Date().getHours()
      if (h < 6) return '凌晨好'
      if (h < 12) return '早上好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    },
    quickNavs() {
      const map = {
        0: [
          { path: '/admin/dashboard', title: '数据大屏', icon: 'el-icon-data-line' },
          { path: '/admin/user', title: '用户管理', icon: 'el-icon-user' },
          { path: '/admin/product-audit', title: '商品审核', icon: 'el-icon-finished' }
        ],
        1: [
          { path: '/merchant/product', title: '商品管理', icon: 'el-icon-goods' },
          { path: '/merchant/order', title: '订单处理', icon: 'el-icon-tickets' },
          { path: '/merchant/statistics', title: '销售统计', icon: 'el-icon-data-line' }
        ],
        2: [
          { path: '/user/shop', title: '商城浏览', icon: 'el-icon-goods' },
          { path: '/user/ai-assistant', title: 'AI助手', icon: 'el-icon-magic-stick' }
        ]
      }
      return map[this.role] || []
    }
  },
  created() {
    this.loadStats()
  },
  methods: {
    async loadStats() {
      try {
        const res = await getHomeData()
        const d = res.data || {}
        if (this.role === 0) {
          this.statList = [
            { label: '总用户数', value: this.formatNumber(d.userCount || 0), icon: 'el-icon-user', color: '#534AB7', path: '/admin/user' },
            { label: '商品总数', value: this.formatNumber(d.productCount || 0), icon: 'el-icon-goods', color: '#0F6E56', path: '/admin/product-audit' },
            { label: '今日订单', value: this.formatNumber(d.todayOrderCount || 0), icon: 'el-icon-tickets', color: '#185FA5', path: '/admin/order' },
            { label: '销售额(元)', value: this.formatNumber(d.todaySalesAmount || 0), icon: 'el-icon-coin', color: '#BA7517', path: '/admin/dashboard' }
          ]
        } else if (this.role === 1) {
          this.statList = [
            { label: '我的商品', value: this.formatNumber(d.productCount || 0), icon: 'el-icon-goods', color: '#0F6E56', path: '/merchant/product' },
            { label: '待发货', value: this.formatNumber(d.pendingShipCount || 0), icon: 'el-icon-tickets', color: '#185FA5', path: '/merchant/order' },
            { label: '本月销售额', value: this.formatNumber(d.monthSalesAmount || 0), icon: 'el-icon-coin', color: '#BA7517', path: '/merchant/statistics' },
            { label: '待回复评价', value: this.formatNumber(d.pendingReplyCount || 0), icon: 'el-icon-star-off', color: '#D4537E', path: '/merchant/review' }
          ]
        } else {
          this.statList = [
            { label: '我的订单', value: this.formatNumber(d.orderCount || 0), icon: 'el-icon-tickets', color: '#185FA5', path: '/user/order' },
            { label: '购物车', value: this.formatNumber(d.cartCount || 0), icon: 'el-icon-shopping-cart-2', color: '#0F6E56', path: '/user/cart' },
            { label: '收货地址', value: this.formatNumber(d.addressCount || 0), icon: 'el-icon-location-information', color: '#BA7517', path: '/user/address' },
            { label: '收藏商品', value: this.formatNumber(d.favoriteCount || 0), icon: 'el-icon-star-off', color: '#D4537E', path: '/user/shop' }
          ]
        }
      } catch (e) {
        // 请求失败时保持默认空数据，不阻断页面
        this.statList = []
      }
    },
    formatNumber(num) {
      if (num === undefined || num === null) return '0'
      const n = Number(num)
      if (isNaN(n)) return num.toString()
      return n.toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.welcome-card { margin-bottom: 20px; border: none; }
.welcome-card /deep/ .el-card__body {
  background: linear-gradient(120deg, #0B3D2E 0%, #0F6E56 60%, #16A085 100%);
  border-radius: 12px;
  padding: 28px 32px;
}
.welcome { display: flex; flex-direction: column; gap: 6px; }
.welcome h2 { font-size: 22px; color: #fff; margin: 0; }
.welcome p { color: rgba(255,255,255,0.8); font-size: 13px; }
.role-badge { align-self: flex-start; padding: 3px 14px; border-radius: 12px; font-size: 12px; background: rgba(255,255,255,0.2); color: #fff; }
.stat-row { margin-bottom: 20px; }
.stat-card { display: flex; align-items: center; transition: transform 0.2s, box-shadow 0.2s; }
.stat-card.clickable { cursor: pointer; }
.stat-card.clickable:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(11, 61, 46, 0.12) !important; }
.stat-card /deep/ .el-card__body { display: flex; align-items: center; gap: 14px; width: 100%; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; box-shadow: 0 3px 10px rgba(0,0,0,0.12); }
.stat-value { font-size: 22px; font-weight: 600; color: #333; }
.stat-label { font-size: 12px; color: #888; }
.quick-nav { display: flex; flex-wrap: wrap; gap: 20px; }
.nav-item { display: flex; flex-direction: column; align-items: center; gap: 6px; cursor: pointer; width: 80px; padding: 12px 0; border-radius: 8px; transition: background 0.2s, transform 0.2s; }
.nav-item:hover { background: #f0faf6; transform: translateY(-2px); }
.nav-item i { font-size: 28px; color: #0F6E56; }
.nav-item span { font-size: 12px; color: #666; }
.notice p { font-size: 13px; color: #666; line-height: 2.2; }
</style>
