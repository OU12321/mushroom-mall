<template>
  <div>
    <el-card>
      <div slot="header">个人中心</div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width:500px">
        <el-form-item label="头像">
          <el-upload :action="$uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleUploadSuccess">
            <img v-if="form.avatar" :src="form.avatar" style="width:80px;height:80px;border-radius:50%" />
            <el-button v-else size="small">上传头像</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.sex">
            <el-radio :label="0">男</el-radio>
            <el-radio :label="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitForm">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { updateUser, getUserInfo } from '@/api/user'

export default {
  name: 'Profile',
  data() {
    const userInfo = this.$store.state.userInfo || {}
    return {
      form: {
        id: userInfo.userId,
        username: userInfo.username || '',
        name: userInfo.name || '',
        phone: '',
        sex: 0,
        avatar: userInfo.avatar || ''
      },
      rules: {
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadUserInfo()
  },
  methods: {
    async loadUserInfo() {
      try {
        const res = await getUserInfo()
        const data = res.data
        if (data) {
          this.form.name = data.name || ''
          this.form.phone = data.phone || ''
          this.form.sex = data.sex || 0
          this.form.avatar = data.avatar || ''
        }
      } catch (e) { /* ignore */ }
    },
    handleUploadSuccess(res) {
      this.form.avatar = res.data.url
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        try {
          await updateUser(this.form)
          // 更新Vuex中的用户信息
          const userInfo = { ...this.$store.state.userInfo, ...this.form }
          this.$store.dispatch('login', userInfo)
          this.$message.success('个人信息已更新')
        } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
      })
    }
  }
}
</script>
