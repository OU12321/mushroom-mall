<template>
  <div>
    <el-card style="margin-bottom: 15px">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="商品名称">
          <el-input v-model="queryForm.name" placeholder="商品名称" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="queryForm.auditStatus" placeholder="全部" clearable style="width:120px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.categoryId" placeholder="全部" clearable style="width:140px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
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
        <el-table-column label="商品图片" width="100">
          <template slot-scope="{row}">
            <el-image v-if="row.image" :src="row.image" style="width:60px;height:60px" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" width="160" />
        <el-table-column label="价格" width="100">
          <template slot-scope="{row}">¥{{ (row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="origin" label="产地" width="120" />
        <el-table-column prop="merchantName" label="商家" width="120">
          <template slot-scope="{row}">{{ row.merchantName || '-' }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template slot-scope="{row}">
            <el-tag :type="auditTagType(row.auditStatus)">{{ auditText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{row}">
            <el-button v-if="row.auditStatus === 0" size="mini" type="success" @click="handleAudit(row, 1)">通过</el-button>
            <el-button v-if="row.auditStatus === 0" size="mini" type="danger" @click="handleAudit(row, 2)">驳回</el-button>
            <el-button size="mini" type="primary" @click="viewDetail(row)">查看详情</el-button>
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

    <!-- 商品详情弹窗 -->
    <el-dialog title="商品详情" :visible.sync="detailVisible" width="600px">
      <el-descriptions :column="2" border v-if="detailData">
        <el-descriptions-item label="商品名称">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="价格">¥{{ (detailData.price || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="产地">{{ detailData.origin || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ detailData.merchantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditTagType(detailData.auditStatus)">{{ auditText(detailData.auditStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="采摘时间">{{ detailData.harvestTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="食用方法">{{ detailData.cookingMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品描述" :span="2">{{ detailData.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detailData && detailData.image" style="margin-top:10px;text-align:center">
        <img :src="detailData.image" style="max-width:300px;max-height:200px" />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getProductPage, getProductById, auditProduct } from '@/api/product'
import { getCategoryList } from '@/api/category'

export default {
  name: 'ProductAudit',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryForm: { page: 1, pageSize: 10, name: '', auditStatus: null, categoryId: null },
      categories: [],
      detailVisible: false,
      detailData: null
    }
  },
  created() {
    this.loadCategories()
    this.loadData()
  },
  methods: {
    async loadCategories() {
      try {
        const res = await getCategoryList()
        this.categories = res.data || []
      } catch (e) { /* ignore */ }
    },
    async loadData() {
      this.loading = true
      try {
        const res = await getProductPage(this.queryForm)
        this.tableData = res.data.records
        this.total = res.data.total
      } finally { this.loading = false }
    },
    handleSearch() { this.queryForm.page = 1; this.loadData() },
    resetSearch() { this.queryForm = { page: 1, pageSize: 10, name: '', auditStatus: null, categoryId: null }; this.loadData() },
    handleSizeChange(val) { this.queryForm.pageSize = val; this.loadData() },
    handleCurrentChange(val) { this.queryForm.page = val; this.loadData() },
    auditText(a) { return ['待审核', '已通过', '已驳回'][a] || '未知' },
    auditTagType(a) { return ['info', 'success', 'danger'][a] || 'info' },
    async handleAudit(row, auditStatus) {
      await auditProduct(row.id, auditStatus)
      this.$message.success(auditStatus === 1 ? '审核通过' : '已驳回')
      this.loadData()
    },
    async viewDetail(row) {
      try {
        const res = await getProductById(row.id)
        this.detailData = res.data
        this.detailVisible = true
      } catch (e) { /* ignore */ }
    }
  }
}
</script>
