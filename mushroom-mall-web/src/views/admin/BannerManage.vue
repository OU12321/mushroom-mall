<template>
  <div>
    <el-card style="margin-bottom: 15px">
      <el-form :inline="true">
        <el-form-item>
          <el-button type="success" @click="openDialog(null)">新增轮播图</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="图片预览" width="140">
          <template slot-scope="{row}">
            <el-image v-if="row.image" :src="row.image" style="width:120px;height:60px" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="160" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{row}">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{row}">
            <el-button size="mini" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button size="mini" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="轮播图标题" />
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <el-upload :action="$uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleUploadSuccess">
            <img v-if="form.image" :src="form.image" style="width:200px;height:100px" />
            <el-button v-else size="small">上传图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { getBannerPage, addBanner, updateBanner, updateBannerStatus, deleteBanner } from '@/api/banner'

export default {
  name: 'BannerManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryForm: { page: 1, pageSize: 10 },
      dialogVisible: false,
      isEdit: false,
      form: { title: '', image: '', sort: 0, status: 1 },
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        image: [{ required: true, message: '请上传图片', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() { return this.isEdit ? '编辑轮播图' : '新增轮播图' }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getBannerPage(this.queryForm)
        if (res && res.data) {
          this.tableData = res.data.records || []
          this.total = res.data.total || 0
        }
      } catch (e) {
        this.tableData = []
        this.total = 0
      } finally { this.loading = false }
    },
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
      this.form = { title: '', image: '', sort: 0, status: 1 }
      this.$refs.formRef && this.$refs.formRef.clearValidate()
    },
    handleUploadSuccess(res) { this.form.image = res.data.url },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        try {
          if (this.isEdit) {
            await updateBanner(this.form)
            this.$message.success('修改成功')
          } else {
            await addBanner(this.form)
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
        await updateBannerStatus(row.id, newStatus)
        this.$message.success(newStatus === 1 ? '已启用' : '已禁用')
        this.loadData()
      } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
    },
    handleDelete(row) {
      this.$confirm(`确认删除轮播图 [${row.title}] 吗？`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteBanner(row.id)
          this.$message.success('删除成功')
          this.loadData()
        }).catch(() => {})
    }
  }
}
</script>
