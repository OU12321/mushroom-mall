<template>
  <div>
    <el-card style="margin-bottom: 15px">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="订单号">
          <el-input v-model="queryForm.orderNo" placeholder="订单号" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width:140px">
            <el-option label="待付款" :value="1" />
            <el-option label="待发货" :value="2" />
            <el-option label="已发货" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已取消" :value="5" />
            <el-option label="退款中" :value="6" />
            <el-option label="已退款" :value="7" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
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
        <el-table-column prop="consignee" label="收货人" width="100" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{row}">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="160">
          <template slot-scope="{row}">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="{row}">
            <el-button v-if="row.status === 6" size="mini" type="warning" @click="handleAgreeRefund(row)">同意退款</el-button>
            <el-button size="mini" type="primary" @click="viewDetail(row)">查看</el-button>
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
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog title="订单详情" :visible.sync="detailVisible" width="700px">
      <el-descriptions :column="2" border v-if="detailData">
        <el-descriptions-item label="订单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ (detailData.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detailData.consignee }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.phone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ detailData.address }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payMethodText(detailData.payMethod) }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="statusTagType(detailData.status)">{{ statusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="detailData && detailData.orderDetails || []" border style="margin-top:15px">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="商品图片" width="80">
          <template slot-scope="{row}">
            <el-image v-if="row.productImage" :src="row.productImage" style="width:50px;height:50px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column label="单价" width="100">
          <template slot-scope="{row}">¥{{ (row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="number" label="数量" width="80" />
        <el-table-column label="小计" width="100">
          <template slot-scope="{row}">¥{{ (row.price * row.number || 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { getOrderPage, getOrderById, agreeRefundOrder } from '@/api/order'

export default {
  name: 'OrderManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryForm: { page: 1, pageSize: 10, orderNo: '', status: null },
      detailVisible: false,
      detailData: null
    }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getOrderPage(this.queryForm)
        this.tableData = res.data.records
        this.total = res.data.total
      } finally { this.loading = false }
    },
    handleSearch() { this.queryForm.page = 1; this.loadData() },
    resetSearch() { this.queryForm = { page: 1, pageSize: 10, orderNo: '', status: null }; this.loadData() },
    handleSizeChange(val) { this.queryForm.pageSize = val; this.loadData() },
    handleCurrentChange(val) { this.queryForm.page = val; this.loadData() },
    statusText(s) { return { 1: '待付款', 2: '待发货', 3: '已发货', 4: '已完成', 5: '已取消', 6: '退款中', 7: '已退款' }[s] || '未知' },
    statusTagType(s) { return { 1: 'warning', 2: 'primary', 3: '', 4: 'success', 5: 'info', 6: 'danger', 7: 'info' }[s] || '' },
    payMethodText(p) { return ['微信支付', '支付宝'][p] || '未知' },
    formatTime(t) { return t ? t.replace('T', ' ') : '-' },
    handleAgreeRefund(row) {
      this.$confirm(`确认同意订单 [${row.orderNo}] 的退款申请吗？退款金额 ¥${(row.totalAmount || 0).toFixed(2)}`, '退款确认', { type: 'warning' })
        .then(async () => {
          try {
            await agreeRefundOrder(row.id)
            this.$message.success('已同意退款')
            this.loadData()
          } catch (e) { /* request.js已提示 */ }
        }).catch(() => {})
    },
    async viewDetail(row) {
      try {
        const res = await getOrderById(row.id)
        this.detailData = res.data
        this.detailVisible = true
      } catch (e) { /* ignore */ }
    }
  }
}
</script>
