<template>
  <div>
    <!-- 搜索栏 -->
    <el-card style="margin-bottom: 15px">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="姓名">
          <el-input v-model="queryForm.name" placeholder="用户名/姓名" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="queryForm.role" placeholder="全部" clearable style="width:120px">
            <el-option label="管理员" :value="0" />
            <el-option label="商家" :value="1" />
            <el-option label="普通用户" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openDialog(null)">新增用户</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户表格 -->
    <el-card>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="90">
          <template slot-scope="{row}">
            <el-tag :type="roleTagType(row.role)">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template slot-scope="{row}">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="商家审核" width="120" v-if="queryForm.role === 1 || !queryForm.role">
          <template slot-scope="{row}">
            <el-tag v-if="row.role === 1" :type="auditTagType(row.auditStatus)" size="mini">
              {{ auditText(row.auditStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template slot-scope="{row}">
            <el-button size="mini" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button size="mini" :type="row.status === 1 ? 'warning' : 'success'"
                       @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="row.role === 1 && row.auditStatus === 0" size="mini" type="success"
                       @click="handleAudit(row, 1)">通过</el-button>
            <el-button v-if="row.role === 1 && row.auditStatus === 0" size="mini" type="danger"
                       @click="handleAudit(row, 2)">驳回</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" :placeholder="isEdit ? '留空则不修改' : '默认123456'" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio :label="0">管理员</el-radio>
            <el-radio :label="1">商家</el-radio>
            <el-radio :label="2">普通用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.sex">
            <el-radio :label="0">男</el-radio>
            <el-radio :label="1">女</el-radio>
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
import { getUserPage, addUser, updateUser, updateUserStatus, auditMerchant, deleteUser } from '@/api/user'

export default {
  name: 'UserManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryForm: { page: 1, pageSize: 10, name: '', role: null },
      dialogVisible: false,
      isEdit: false,
      form: { username: '', password: '', name: '', phone: '', role: 2, sex: 0 },
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        role: [{ required: true, message: '请选择角色', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() { return this.isEdit ? '编辑用户' : '新增用户' }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getUserPage(this.queryForm)
        this.tableData = res.data.records
        this.total = res.data.total
      } finally { this.loading = false }
    },
    handleSearch() { this.queryForm.page = 1; this.loadData() },
    resetSearch() { this.queryForm = { page: 1, pageSize: 10, name: '', role: null }; this.loadData() },
    handleSizeChange(val) { this.queryForm.pageSize = val; this.loadData() },
    handleCurrentChange(val) { this.queryForm.page = val; this.loadData() },
    roleText(r) { return ['管理员', '商家', '普通用户'][r] || '未知' },
    roleTagType(r) { return ['', 'warning', ''][r] || '' },
    auditText(a) { return ['待审核', '已通过', '已驳回'][a] || '-' },
    auditTagType(a) { return ['info', 'success', 'danger'][a] || 'info' },
    openDialog(row) {
      this.resetForm()
      if (row) {
        this.isEdit = true
        this.form = { ...row, password: '' }
      } else {
        this.isEdit = false
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.form = { username: '', password: '', name: '', phone: '', role: 2, sex: 0 }
      this.$refs.formRef && this.$refs.formRef.clearValidate()
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        try {
          if (this.isEdit) {
            await updateUser(this.form)
            this.$message.success('修改成功')
          } else {
            await addUser(this.form)
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
        await updateUserStatus(row.id, newStatus)
        this.$message.success(newStatus === 1 ? '已启用' : '已禁用')
        this.loadData()
      } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
    },
    async handleAudit(row, auditStatus) {
      try {
        await auditMerchant(row.id, auditStatus)
        this.$message.success(auditStatus === 1 ? '审核通过' : '已驳回')
        this.loadData()
      } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
    },
    handleDelete(row) {
      this.$confirm(`确认删除用户 [${row.username}] 吗？`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteUser(row.id)
          this.$message.success('删除成功')
          this.loadData()
        }).catch(() => {})
    }
  }
}
</script>
