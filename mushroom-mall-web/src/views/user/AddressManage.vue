<template>
  <div class="address-page">
    <el-card>
      <div slot="header">
        收货地址管理
        <el-button type="primary" size="mini" style="float:right" @click="handleAdd">新增地址</el-button>
      </div>

      <el-table :data="addressList" border v-loading="loading" empty-text="暂无收货地址，请新增">
        <el-table-column prop="consignee" label="收货人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="detailAddress" label="详细地址" min-width="240" show-overflow-tooltip />
        <el-table-column label="默认地址" width="100" align="center">
          <template slot-scope="{row}">
            <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
            <span v-else style="color:#aaa">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template slot-scope="{row}">
            <el-button size="mini" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="mini" type="success" :disabled="row.isDefault === 1" @click="handleSetDefault(row)">设为默认</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="480px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="收货人" prop="consignee">
          <el-input v-model="form.consignee" placeholder="收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" type="textarea" :rows="2" placeholder="省市区+街道门牌号" maxlength="200" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'

export default {
  name: 'UserAddress',
  data() {
    const checkPhone = (rule, value, cb) => {
      if (!value) return cb(new Error('请输入联系电话'))
      if (!/^1[3-9]\d{9}$/.test(value)) return cb(new Error('请输入正确的手机号'))
      cb()
    }
    return {
      loading: false,
      addressList: [],
      dialogVisible: false,
      isEdit: false,
      form: { id: null, consignee: '', phone: '', detailAddress: '', isDefault: 0 },
      rules: {
        consignee: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
        phone: [{ required: true, validator: checkPhone, trigger: 'blur' }],
        detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() { return this.isEdit ? '编辑地址' : '新增地址' }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getAddressList()
        this.addressList = res.data || []
      } finally { this.loading = false }
    },
    handleAdd() {
      this.isEdit = false
      this.form = { id: null, consignee: '', phone: '', detailAddress: '', isDefault: this.addressList.length === 0 ? 1 : 0 }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true
      this.form = { id: row.id, consignee: row.consignee, phone: row.phone, detailAddress: row.detailAddress, isDefault: row.isDefault }
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        try {
          if (this.isEdit) {
            await updateAddress({ ...this.form })
            this.$message.success('修改成功')
          } else {
            await addAddress({ ...this.form })
            this.$message.success('新增成功')
          }
          this.dialogVisible = false
          this.loadData()
        } catch (e) { /* 错误已由拦截器提示 */ }
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除该收货地址吗？', '提示', { type: 'warning' })
        .then(async () => {
          await deleteAddress(row.id)
          this.$message.success('删除成功')
          this.loadData()
        }).catch(() => {})
    },
    async handleSetDefault(row) {
      try {
        await setDefaultAddress(row.id)
        this.$message.success('已设为默认地址')
        this.loadData()
      } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
    },
    resetForm() {
      this.form = { id: null, consignee: '', phone: '', detailAddress: '', isDefault: 0 }
      this.$nextTick(() => { this.$refs.formRef && this.$refs.formRef.clearValidate() })
    }
  }
}
</script>

<style scoped>
.address-page { padding: 0; }
</style>
