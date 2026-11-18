<template>
  <div>
    <!-- 搜索栏 -->
    <el-card style="margin-bottom: 15px">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="分类名称">
          <el-input v-model="queryForm.name" placeholder="分类名称" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openDialog(null)">新增分类</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 分类表格 -->
    <el-card>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="分类名称" width="180" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template slot-scope="{row}">
            <el-button size="mini" type="warning" @click="viewProducts(row)">查看商品</el-button>
            <el-button size="mini" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="450px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：牛肝菌、松茸、鸡枞" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span style="margin-left:10px;color:#999">数值越小越靠前</span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <!-- 查看分类下商品弹窗 -->
    <el-dialog :title="productDialogTitle" :visible.sync="productDialogVisible" width="850px" top="5vh">
      <el-table :data="productList" border stripe v-loading="productLoading" size="small" max-height="500">
        <el-table-column type="index" label="#" width="45" />
        <el-table-column label="图片" width="70">
          <template slot-scope="{row}">
            <el-image v-if="row.image" :src="row.image" style="width:40px;height:40px" fit="cover" :preview-src-list="[row.image]" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="price" label="价格(元)" width="90">
          <template slot-scope="{row}">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="saleCount" label="销量" width="70" />
        <el-table-column label="审核" width="80">
          <template slot-scope="{row}">
            <el-tag size="mini" :type="auditTagType(row.auditStatus)">{{ auditText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上架" width="70">
          <template slot-scope="{row}">
            <el-tag size="mini" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="所属商家" width="110" show-overflow-tooltip />
      </el-table>

      <el-pagination v-if="productTotal > 0" style="margin-top:12px;text-align:right"
        @current-change="handleProductPageChange"
        :current-page="productQuery.page"
        :page-size="productQuery.pageSize"
        layout="total, prev, pager, next"
        :total="productTotal">
      </el-pagination>

      <div v-if="!productLoading && productList.length === 0" style="text-align:center;color:#999;padding:30px 0">
        该分类下暂无商品
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getCategoryPage, addCategory, updateCategory, deleteCategory } from '@/api/category'
import { getProductPage } from '@/api/product'

export default {
  name: 'CategoryManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryForm: { page: 1, pageSize: 10, name: '' },
      dialogVisible: false,
      isEdit: false,
      form: { name: '', sort: 0 },
      rules: {
        name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
      },
      // 查看商品弹窗
      productDialogVisible: false,
      productLoading: false,
      productList: [],
      productTotal: 0,
      currentCategory: null,
      productQuery: { page: 1, pageSize: 10 }
    }
  },
  computed: {
    dialogTitle() { return this.isEdit ? '编辑分类' : '新增分类' },
    productDialogTitle() {
      return this.currentCategory ? `分类「${this.currentCategory.name}」下的商品` : '分类商品'
    }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getCategoryPage(this.queryForm)
        this.tableData = res.data.records
        this.total = res.data.total
      } finally { this.loading = false }
    },
    handleSearch() { this.queryForm.page = 1; this.loadData() },
    resetSearch() { this.queryForm = { page: 1, pageSize: 10, name: '' }; this.loadData() },
    handleSizeChange(val) { this.queryForm.pageSize = val; this.loadData() },
    handleCurrentChange(val) { this.queryForm.page = val; this.loadData() },
    openDialog(row) {
      this.resetForm()
      if (row) {
        this.isEdit = true
        this.form = { ...row }
      } else {
        this.isEdit = false
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.form = { name: '', sort: 0 }
      this.$refs.formRef && this.$refs.formRef.clearValidate()
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        if (this.isEdit) {
          await updateCategory(this.form)
          this.$message.success('修改成功')
        } else {
          await addCategory(this.form)
          this.$message.success('新增成功')
        }
        this.dialogVisible = false
        this.loadData()
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除分类 [${row.name}] 吗？如果该分类下有商品将无法删除。`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteCategory(row.id)
          this.$message.success('删除成功')
          this.loadData()
        }).catch(() => {})
    },
    // 查看分类下商品
    viewProducts(row) {
      this.currentCategory = row
      this.productQuery.page = 1
      this.productDialogVisible = true
      this.loadProducts()
    },
    async loadProducts() {
      this.productLoading = true
      try {
        const res = await getProductPage({
          ...this.productQuery,
          categoryId: this.currentCategory.id
        })
        this.productList = res.data.records
        this.productTotal = res.data.total
      } finally { this.productLoading = false }
    },
    handleProductPageChange(val) {
      this.productQuery.page = val
      this.loadProducts()
    },
    auditText(status) {
      return { 0: '待审核', 1: '已通过', 2: '已驳回' }[status] || '未知'
    },
    auditTagType(status) {
      return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
    }
  }
}
</script>
