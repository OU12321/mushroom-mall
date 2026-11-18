<template>
  <div class="pay-page">
    <!-- 顶部返回 -->
    <div class="pay-header">
      <el-button icon="el-icon-arrow-left" size="small" @click="goBack">返回</el-button>
      <span class="pay-title">订单支付</span>
    </div>

    <div v-loading="loading">
      <template v-if="order">
        <!-- 订单信息 -->
        <el-card class="info-card" shadow="hover">
          <div slot="header">订单信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <p><span class="label">订单号：</span>{{ order.orderNo }}</p>
              <p><span class="label">下单时间：</span>{{ formatTime(order.createTime) }}</p>
            </el-col>
            <el-col :span="12">
              <p><span class="label">订单状态：</span>
                <el-tag :type="statusTagType(order.status)">{{ statusText(order.status) }}</el-tag>
              </p>
              <p><span class="label">支付方式：</span>{{ payMethodText(order.payMethod) }}</p>
            </el-col>
          </el-row>
        </el-card>

        <!-- 商品明细 -->
        <el-card class="info-card" shadow="hover">
          <div slot="header">商品明细</div>
          <el-table :data="order.orderDetails" border>
            <el-table-column label="商品图片" width="80">
              <template slot-scope="{row}">
                <el-image v-if="row.productImage" :src="row.productImage" style="width:50px;height:50px" fit="cover" />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称" />
            <el-table-column label="单价" width="100">
              <template slot-scope="{row}">¥{{ (row.price || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="number" label="数量" width="80" />
            <el-table-column label="小计" width="120">
              <template slot-scope="{row}">¥{{ (row.amount || 0).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 收货信息 -->
        <el-card class="info-card" shadow="hover">
          <div slot="header">收货信息</div>
          <p><span class="label">收货人：</span>{{ order.consignee }}</p>
          <p><span class="label">联系电话：</span>{{ order.phone }}</p>
          <p><span class="label">收货地址：</span>{{ order.address }}</p>
          <p v-if="order.remark"><span class="label">备注：</span>{{ order.remark }}</p>
        </el-card>

        <!-- 支付区域 -->
        <el-card class="pay-card" shadow="hover">
          <div class="pay-amount">
            <span>应付金额：</span>
            <span class="amount">¥{{ (order.totalAmount || 0).toFixed(2) }}</span>
          </div>
          <div class="pay-actions" v-if="order.status === 1">
            <el-button type="primary" size="medium" icon="el-icon-wallet" @click="openSandbox">去支付</el-button>
            <el-button size="medium" @click="handleCancel">取消订单</el-button>
          </div>
          <div class="pay-actions" v-else>
            <el-tag :type="statusTagType(order.status)">{{ statusText(order.status) }}</el-tag>
            <el-button type="primary" size="medium" @click="$router.push('/user/order')">查看我的订单</el-button>
          </div>
        </el-card>
      </template>
    </div>

    <!-- 模拟沙盒支付弹窗 -->
    <el-dialog title="沙盒支付 · 模拟收银台" :visible.sync="sandboxVisible" width="560px"
               :close-on-click-modal="false" @open="onSandboxOpen" @closed="onSandboxClosed">
      <div class="sandbox">
        <!-- 支付方式选择 -->
        <div class="pay-methods">
          <div class="method-item" :class="{ active: sandboxMethod === 0 }" @click="sandboxMethod = 0">
            <div class="method-icon wechat">
              <svg viewBox="0 0 1024 1024" width="28" height="28"><path d="M690.1 377.4c5.9 0 11.8.2 17.6.5-24.4-128.7-158.3-227.1-322.3-227.1C204.7 150.8 64 272.3 64 429.8c0 91.7 49.4 166.4 131.3 224.4l-32.7 98.6 114.8-58.2c41.1 8.1 74.1 16.5 115.2 16.5 9.7 0 19.4-.5 29-1.4-6.1-20.8-9.6-42.6-9.6-65.3 0-134.6 112.5-267 278.1-267zM485.2 288.5c19.5 0 32.4 12.9 32.4 32.4 0 19.4-12.9 32.4-32.4 32.4-19.4 0-38.9-12.9-38.9-32.4 0-19.5 19.5-32.4 38.9-32.4zM270.8 353.3c-19.4 0-38.9-12.9-38.9-32.4 0-19.5 19.5-32.4 38.9-32.4 19.5 0 32.4 12.9 32.4 32.4 0 19.5-12.9 32.4-32.4 32.4z" fill="#fff"/><path d="M964.8 602.2c0-127.9-129.5-235.3-274.7-235.3-153.8 0-275.2 107.4-275.2 235.3s121.4 235.3 275.2 235.3c32.2 0 64.6-8.1 96.9-16.3l88.4 48.5-24.2-72.7c64.8-48.6 113.6-113.6 113.6-194.8zM616.2 569.8c-12.9 0-25.9-12.9-25.9-25.9 0-12.9 12.9-25.9 25.9-25.9 19.5 0 32.4 12.9 32.4 25.9 0 12.9-12.9 25.9-32.4 25.9zM790.4 569.8c-12.9 0-25.8-12.9-25.8-25.9 0-12.9 12.9-25.9 25.8-25.9 19.5 0 32.4 12.9 32.4 25.9 0 12.9-12.9 25.9-32.4 25.9z" fill="#fff"/></svg>
            </div>
            <span>微信支付</span>
            <i v-if="sandboxMethod === 0" class="el-icon-success checked"></i>
          </div>
          <div class="method-item" :class="{ active: sandboxMethod === 1 }" @click="sandboxMethod = 1">
            <div class="method-icon alipay">
              <svg viewBox="0 0 1024 1024" width="28" height="28"><path d="M789 688c-12 30-27 57-45 81 21 7 40 15 57 23 42 19 68 37 68 37l-34 56s-27-19-69-39c-42-20-93-37-148-45 20-24 37-51 50-81-44-7-91-11-140-11-70 0-135 8-191 22l-24-64s81-24 215-24c53 0 103 4 148 11 5-14 9-29 12-44H327v-50h209v-58H299v-50h237v-64c0-11 1-22 2-32h-63l21-54s53 4 107 32c25 13 33 27 33 44v74h222v50H695v58h203v50H682c-4 16-9 31-15 45 38 9 72 20 101 33z M512 64C264.6 64 64 228.6 64 463.4c0 232.3 196.9 399.4 448 399.4 82.9 0 160-19.5 226-53.7C595.9 724.3 444.8 647 444.8 647s171.8-316.8 342.3-316.8c-6.9-22.7-16.4-44.4-28.5-64.8C701.2 144.3 612.8 64 512 64z" fill="#fff"/></svg>
            </div>
            <span>支付宝</span>
            <i v-if="sandboxMethod === 1" class="el-icon-success checked"></i>
          </div>
        </div>

        <!-- 二维码 + 金额 -->
        <div class="qr-section">
          <div class="qr-box">
            <svg :width="qrSize" :height="qrSize" class="qr-svg">
              <rect v-for="(cell, i) in qrCells" :key="i"
                    :x="cell.x" :y="cell.y" :width="cell.s" :height="cell.s" :style="{ fill: qrColor }" />
            </svg>
            <div class="qr-logo" :style="{ background: qrLogoBg }">
              <span v-if="sandboxMethod === 0" class="logo-text">微信</span>
              <span v-else class="logo-text">支付宝</span>
            </div>
          </div>
          <div class="qr-info">
            <p class="pay-amount-large">¥{{ (order ? order.totalAmount : 0).toFixed(2) }}</p>
            <p class="pay-tip">
              <i :class="sandboxMethod === 0 ? 'el-icon-chat-dot-round' : 'el-icon-wallet'"></i>
              请使用{{ sandboxMethod === 0 ? '微信' : '支付宝' }}扫一扫<br>完成支付
            </p>
            <p class="pay-countdown" v-if="countdownText">支付剩余时间：{{ countdownText }}</p>
          </div>
        </div>

        <!-- 沙盒提示 -->
        <div class="sandbox-notice">
          <i class="el-icon-info"></i>
          本支付为模拟沙盒环境，点击下方按钮即可模拟完成支付，不会产生真实交易。
        </div>
      </div>

      <div slot="footer">
        <el-button @click="sandboxVisible = false">关闭</el-button>
        <el-button type="primary" :loading="paying" @click="confirmPay">
          <i :class="sandboxMethod === 0 ? 'el-icon-chat-dot-round' : 'el-icon-wallet'"></i>
          确认已支付
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getOrderById, payOrder, cancelOrder } from '@/api/order'

export default {
  name: 'UserPay',
  data() {
    return {
      loading: false,
      paying: false,
      order: null,
      // 沙盒支付
      sandboxVisible: false,
      sandboxMethod: 0,
      qrCells: [],
      qrSize: 200,
      countdownSeconds: 900,
      countdownTimer: null
    }
  },
  computed: {
    qrColor() {
      return this.sandboxMethod === 0 ? '#07C160' : '#1677FF'
    },
    qrLogoBg() {
      return this.sandboxMethod === 0 ? '#07C160' : '#1677FF'
    },
    countdownText() {
      if (this.countdownSeconds <= 0) return ''
      const m = Math.floor(this.countdownSeconds / 60)
      const s = this.countdownSeconds % 60
      return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
    }
  },
  created() {
    const orderId = this.$route.query.orderId
    if (!orderId) {
      this.$message.error('缺少订单信息')
      this.$router.push('/user/order')
      return
    }
    this.loadOrder(orderId)
  },
  beforeDestroy() {
    this.clearCountdown()
  },
  methods: {
    statusText(s) { return { 1: '待付款', 2: '待发货', 3: '已发货', 4: '已完成', 5: '已取消', 6: '退款中', 7: '已退款' }[s] || '未知' },
    statusTagType(s) { return { 1: 'warning', 2: 'primary', 3: '', 4: 'success', 5: 'info', 6: 'danger', 7: 'info' }[s] || '' },
    payMethodText(m) { return { 0: '微信支付', 1: '支付宝' }[m] || '未选择' },
    formatTime(t) {
      if (!t) return '-'
      return t.replace('T', ' ')
    },
    async loadOrder(id) {
      this.loading = true
      try {
        const res = await getOrderById(id)
        this.order = res.data
        // 默认支付方式跟随下单时选择
        if (this.order.payMethod === 1) {
          this.sandboxMethod = 1
        } else {
          this.sandboxMethod = 0
        }
      } catch (e) {
        this.$message.error('订单加载失败')
        this.$router.push('/user/order')
      } finally {
        this.loading = false
      }
    },
    goBack() {
      this.$router.back()
    },
    // ---- 沙盒支付 ----
    openSandbox() {
      this.sandboxVisible = true
    },
    onSandboxOpen() {
      this.generateQR()
      this.countdownSeconds = 900
      this.startCountdown()
    },
    onSandboxClosed() {
      this.clearCountdown()
    },
    generateQR() {
      // 生成 25x25 模拟二维码矩阵
      const size = 25
      const unit = this.qrSize / size
      const matrix = []
      for (let i = 0; i < size; i++) {
        matrix.push([])
        for (let j = 0; j < size; j++) {
          matrix[i].push(Math.random() > 0.52)
        }
      }
      // 三个角加定位标记
      const drawFinder = (r, c) => {
        for (let i = 0; i < 7; i++) {
          for (let j = 0; j < 7; j++) {
            if (r + i < size && c + j < size) {
              const border = i === 0 || i === 6 || j === 0 || j === 6
              const center = i >= 2 && i <= 4 && j >= 2 && j <= 4
              matrix[r + i][c + j] = border || center
            }
          }
        }
      }
      drawFinder(0, 0)
      drawFinder(0, size - 7)
      drawFinder(size - 7, 0)
      // 清除定位标记周围的留白
      const clearArea = (r, c) => {
        for (let i = -1; i <= 7; i++) {
          for (let j = -1; j <= 7; j++) {
            const ri = r + i
            const cj = c + j
            if (ri >= 0 && ri < size && cj >= 0 && cj < size) {
              if (i === -1 || i === 7 || j === -1 || j === 7) {
                matrix[ri][cj] = false
              }
            }
          }
        }
      }
      clearArea(0, 0)
      clearArea(0, size - 7)
      clearArea(size - 7, 0)

      // 转成 cell 列表
      const cells = []
      for (let i = 0; i < size; i++) {
        for (let j = 0; j < size; j++) {
          if (matrix[i][j]) {
            cells.push({ x: j * unit, y: i * unit, s: unit })
          }
        }
      }
      this.qrCells = cells
    },
    startCountdown() {
      this.clearCountdown()
      this.countdownTimer = setInterval(() => {
        if (this.countdownSeconds > 0) {
          this.countdownSeconds--
        } else {
          this.clearCountdown()
        }
      }, 1000)
    },
    clearCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
    },
    async confirmPay() {
      this.paying = true
      try {
        await payOrder(this.order.id)
        this.$message.success('支付成功')
        this.sandboxVisible = false
        this.clearCountdown()
        await this.loadOrder(this.order.id)
      } catch (e) {
        // 错误已在 request 拦截器提示
      } finally {
        this.paying = false
      }
    },
    handleCancel() {
      this.$confirm('确认取消该订单吗？取消后可重新选购商品。', '提示', { type: 'warning' })
        .then(async () => {
          try {
            await cancelOrder(this.order.id)
            this.$message.success('订单已取消')
            this.$router.push('/user/order')
          } catch (e) {
            // 错误已在 request 拦截器提示
          }
        }).catch(() => {})
    }
  },
  watch: {
    sandboxMethod() {
      // 切换支付方式时重新生成二维码
      if (this.sandboxVisible) {
        this.generateQR()
      }
    }
  }
}
</script>

<style scoped>
.pay-page { padding: 0; }
.pay-header { display: flex; align-items: center; gap: 15px; margin-bottom: 15px; }
.pay-title { font-size: 18px; font-weight: 600; }
.info-card { margin-bottom: 15px; }
.info-card p { margin: 8px 0; font-size: 14px; }
.info-card .label { color: #888; display: inline-block; width: 80px; }
.pay-card { text-align: center; padding: 20px 0; }
.pay-amount { margin-bottom: 20px; }
.pay-amount .amount { font-size: 28px; font-weight: 700; color: #D85A30; }
.pay-actions { display: flex; justify-content: center; gap: 15px; align-items: center; }

/* 沙盒支付弹窗 */
.sandbox { }
.pay-methods { display: flex; gap: 15px; margin-bottom: 20px; }
.method-item {
  flex: 1; display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; border: 2px solid #e4e7ed; border-radius: 8px;
  cursor: pointer; transition: all .2s; position: relative;
}
.method-item:hover { border-color: #c0c4cc; }
.method-item.active { border-color: #409EFF; background: #ecf5ff; }
.method-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.method-icon.wechat { background: #07C160; }
.method-icon.alipay { background: #1677FF; }
.method-item span { font-size: 15px; font-weight: 500; }
.method-item .checked { color: #409EFF; font-size: 20px; margin-left: auto; }

.qr-section { display: flex; gap: 30px; align-items: center; justify-content: center; padding: 10px 0 20px; }
.qr-box { position: relative; width: 200px; height: 200px; }
.qr-svg { border: 1px solid #eee; border-radius: 8px; background: #fff; }
.qr-logo {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 44px; height: 44px; border-radius: 8px; border: 3px solid #fff;
  display: flex; align-items: center; justify-content: center;
}
.qr-logo .logo-text { color: #fff; font-size: 11px; font-weight: 700; }
.qr-info { text-align: center; }
.pay-amount-large { font-size: 32px; font-weight: 700; color: #D85A30; margin: 0 0 12px; }
.pay-tip { color: #666; font-size: 14px; line-height: 1.8; margin: 0; }
.pay-tip i { margin-right: 4px; }
.pay-countdown { color: #E6A23C; font-size: 13px; margin-top: 10px; }

.sandbox-notice {
  background: #fdf6ec; border: 1px solid #f5dab1; border-radius: 6px;
  padding: 10px 14px; font-size: 12px; color: #e6a23c; line-height: 1.6;
}
.sandbox-notice i { margin-right: 4px; }
</style>
