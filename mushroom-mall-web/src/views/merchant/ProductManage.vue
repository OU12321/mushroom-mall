<template>
  <div>
    <el-card style="margin-bottom: 15px">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="商品名称">
          <el-input v-model="queryForm.name" placeholder="商品名称" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.categoryId" placeholder="全部" clearable style="width:140px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="queryForm.auditStatus" placeholder="全部" clearable style="width:120px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openDialog(null)">新增商品</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="图片" width="80">
          <template slot-scope="{row}">
            <el-image v-if="row.image" :src="row.image" style="width:50px;height:50px" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column label="分类" width="100">
          <template slot-scope="{row}">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column label="价格" width="100">
          <template slot-scope="{row}">¥{{ (row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="saleCount" label="销量" width="80" />
        <el-table-column label="库存" width="90">
          <template slot-scope="{row}">
            <el-tag :type="row.stock > 0 ? 'success' : 'danger'" size="mini">{{ row.stock > 0 ? row.stock : '售罄' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template slot-scope="{row}">
            <el-tag :type="auditTagType(row.auditStatus)">{{ auditText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上架状态" width="100">
          <template slot-scope="{row}">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template slot-scope="{row}">
            <el-button size="mini" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button size="mini" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="700px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="如：美味牛肝菌(鲜)" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :precision="0" placeholder="库存数量" />
          <span style="margin-left:10px;color:#999;font-size:12px">设为0则显示售罄</span>
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload :action="$uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleUploadSuccess">
            <img v-if="form.image" :src="form.image" style="width:100px;height:100px" />
            <el-button v-else size="small">上传图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="产地">
          <el-input v-model="form.origin" placeholder="如：云南香格里拉" />
        </el-form-item>
        <el-form-item label="采摘时间">
          <el-input v-model="form.harvestTime" placeholder="如：2024年7月" />
        </el-form-item>
        <el-form-item label="食用方法">
          <el-input v-model="form.cookingMethod" type="textarea" :rows="3" placeholder="烹饪建议" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="商品详细描述" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getProductPage, addProduct, updateProduct, updateProductStatus, deleteProduct } from '@/api/product'
import { getCategoryList } from '@/api/category'

export default {
  name: 'ProductManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryForm: { page: 1, pageSize: 10, name: '', categoryId: null, auditStatus: null },
      categories: [],
      dialogVisible: false,
      isEdit: false,
      form: this.getDefaultForm(),
      rules: {
        name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
        categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
        price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() { return this.isEdit ? '编辑商品' : '新增商品' }
  },
  created() {
    this.loadCategories()
    this.loadData()
  },
  methods: {
    getDefaultForm() {
      return { name: '', categoryId: null, price: 0, stock: 0, image: '', origin: '', harvestTime: '', cookingMethod: '', description: '' }
    },
    auditText(a) { return ['待审核', '已通过', '已驳回'][a] || '未知' },
    auditTagType(a) { return ['info', 'success', 'danger'][a] || 'info' },
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
        if (!res || !res.data) {
          this.tableData = []
          this.total = 0
          return
        }
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        this.tableData = []
        this.total = 0
      } finally { this.loading = false }
    },
    handleSearch() { this.queryForm.page = 1; this.loadData() },
    resetSearch() { this.queryForm = { page: 1, pageSize: 10, name: '', categoryId: null, auditStatus: null }; this.loadData() },
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
      this.form = this.getDefaultForm()
      this.$refs.formRef && this.$refs.formRef.clearValidate()
    },
    handleUploadSuccess(res) { this.form.image = res.data.url },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        try {
          if (this.isEdit) {
            await updateProduct(this.form)
            this.$message.success('修改成功')
          } else {
            await addProduct(this.form)
            this.$message.success('新增成功')
          }
          this.dialogVisible = false
          this.loadData()
        } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
      })
    },
    async toggleStatus(row) {
      const newStatus = row.status === 1 ? 0 : 1
      try {
        await updateProductStatus(row.id, newStatus)
        this.$message.success(newStatus === 1 ? '已上架' : '已下架')
        this.loadData()
      } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
    },
    handleDelete(row) {
      this.$confirm(`确认删除商品 [${row.name}] 吗？`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteProduct(row.id)
          this.$message.success('删除成功')
          this.loadData()
        }).catch(() => {})
    }
  }
}
</script>
