<template>
  <div>
    <!-- 状态Tab -->
    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" name="1" />
      <el-tab-pane label="待发货" name="2" />
      <el-tab-pane label="已发货" name="3" />
      <el-tab-pane label="已完成" name="4" />
    </el-tabs>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column label="商品" width="200">
        <template slot-scope="{row}">
          <span v-if="row.orderDetails && row.orderDetails.length">{{ row.orderDetails[0].productName }} x{{ row.orderDetails[0].number }}</span>
          <span v-if="row.orderDetails && row.orderDetails.length > 1"> 等{{ row.orderDetails.length }}件</span>
        </template>
      </el-table-column>
      <el-table-column label="总金额" width="100">
        <template slot-scope="{row}">¥{{ (row.totalAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template slot-scope="{row}">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="160">
        <template slot-scope="{row}">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template slot-scope="{row}">
          <el-button v-if="row.status === 1" size="mini" type="success" @click="handlePay(row)">付款</el-button>
          <el-button v-if="row.status === 3" size="mini" type="success" @click="handleReceive(row)">确认收货</el-button>
          <el-button v-if="row.status === 4 && !row.reviewed" size="mini" type="primary" @click="openReview(row)">评价</el-button>
          <el-button v-if="row.status === 4 && row.reviewed" size="mini" type="info" disabled>已评价</el-button>
          <el-button v-if="row.status === 1" size="mini" type="warning" @click="handleCancel(row)">取消</el-button>
          <el-button v-if="row.status === 2 || row.status === 3" size="mini" type="danger" @click="handleRefund(row)">退款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:15px;text-align:right"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="queryForm.page"
      :page-sizes="[10, 20, 50]"
      :page-size="queryForm.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total">
    </el-pagination>

    <!-- 评价弹窗 -->
    <el-dialog title="评价订单" :visible.sync="reviewVisible" width="500px">
      <el-form :model="reviewForm" :rules="reviewRules" ref="reviewFormRef" label-width="80px">
        <el-form-item label="评分" prop="score">
          <el-rate v-model="reviewForm.score" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="分享您的体验" />
        </el-form-item>
        <el-form-item label="评价图片">
          <el-upload :action="$uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleReviewUploadSuccess" :on-remove="handleReviewUploadRemove" list-type="picture-card" :file-list="reviewFileList" :limit="3">
            <i class="el-icon-plus"></i>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getOrderPage, receiveOrder, cancelOrder, refundOrder } from '@/api/order'
import { addReview } from '@/api/review'

export default {
  name: 'UserOrder',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      activeTab: 'all',
      queryForm: { page: 1, pageSize: 10, status: null },
      reviewVisible: false,
      reviewRow: null,
      reviewForm: { productId: null, orderId: null, score: 5, content: '', images: '' },
      reviewRules: {
        score: [{ required: true, message: '请评分', trigger: 'change' }],
        content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
      },
      reviewFileList: [],
      reviewImages: []
    }
  },
  created() { this.loadData() },
  methods: {
    statusText(s) { return { 1: '待付款', 2: '待发货', 3: '已发货', 4: '已完成', 5: '已取消', 6: '退款中', 7: '已退款' }[s] || '未知' },
    statusTagType(s) { return { 1: 'warning', 2: 'primary', 3: '', 4: 'success', 5: 'info', 6: 'danger', 7: 'info' }[s] || '' },
    formatTime(t) {
      if (!t) return '-'
      return t.replace('T', ' ')
    },
    async loadData() {
      this.loading = true
      try {
        const res = await getOrderPage(this.queryForm)
        this.tableData = res.data.records
        this.total = res.data.total
      } finally { this.loading = false }
    },
    handleTabChange(tab) {
      this.queryForm.status = tab.name === 'all' ? null : parseInt(tab.name)
      this.queryForm.page = 1
      this.loadData()
    },
    handleSizeChange(val) { this.queryForm.pageSize = val; this.loadData() },
    handleCurrentChange(val) { this.queryForm.page = val; this.loadData() },
    handlePay(row) {
      this.$router.push({ path: '/user/pay', query: { orderId: row.id } })
    },
    async handleReceive(row) {
      this.$confirm('确认已收到商品吗？', '提示', { type: 'warning' })
        .then(async () => {
          await receiveOrder(row.id)
          this.$message.success('已确认收货')
          this.loadData()
        }).catch(() => {})
    },
    async handleCancel(row) {
      this.$confirm('确认取消该订单吗？', '提示', { type: 'warning' })
        .then(async () => {
          await cancelOrder(row.id)
          this.$message.success('订单已取消')
          this.loadData()
        }).catch(() => {})
    },
    async handleRefund(row) {
      this.$confirm('确认申请退款吗？', '提示', { type: 'warning' })
        .then(async () => {
          await refundOrder(row.id)
          this.$message.success('已申请退款')
          this.loadData()
        }).catch(() => {})
    },
    openReview(row) {
      this.reviewRow = row
      this.reviewForm = { productId: row.orderDetails && row.orderDetails[0] ? row.orderDetails[0].productId : null, orderId: row.id, score: 5, content: '', images: '' }
      this.reviewImages = []
      this.reviewFileList = []
      this.reviewVisible = true
    },
    handleReviewUploadSuccess(res) {
      this.reviewImages.push(res.data.url)
      this.reviewForm.images = JSON.stringify(this.reviewImages)
    },
    handleReviewUploadRemove(file) {
      const url = file.response ? file.response.data : file.url
      this.reviewImages = this.reviewImages.filter(img => img !== url)
      this.reviewForm.images = this.reviewImages.length > 0 ? JSON.stringify(this.reviewImages) : ''
    },
    async submitReview() {
      this.$refs.reviewFormRef.validate(async valid => {
        if (!valid) return
        try {
          await addReview(this.reviewForm)
          this.$message.success('评价成功')
          this.reviewVisible = false
          if (this.reviewRow) {
            this.$set(this.reviewRow, 'reviewed', true)
          }
          this.loadData()
        } catch (e) {
          // 错误已在 request 拦截器用 Message.error 提示，这里只阻止继续执行
        }
      })
    }
  }
}
</script>
