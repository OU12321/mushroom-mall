<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 48 48" width="56" height="56" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="rgba(255,255,255,0.15)"/>
            <rect x="21" y="28" width="6" height="12" rx="3" fill="#E8D5B5"/>
            <path d="M9 30 Q24 8 39 30 Z" fill="#D85A30"/>
            <circle cx="16" cy="22" r="2.6" fill="#F4E9D8"/>
            <circle cx="27" cy="25" r="2.2" fill="#F4E9D8"/>
            <circle cx="24" cy="18" r="2" fill="#F4E9D8"/>
          </svg>
          <span>云岭山珍</span>
        </div>
        <h1 class="brand-title">高山野生菌<br />线上销售商城</h1>
        <p class="brand-slogan">来自海拔3500米原始森林的自然馈赠</p>
        <div class="brand-features">
          <div class="brand-feature"><i class="el-icon-mountain"></i><span>高山采摘</span></div>
          <div class="brand-feature"><i class="el-icon-truck"></i><span>冷链直达</span></div>
          <div class="brand-feature"><i class="el-icon-medal"></i><span>品质溯源</span></div>
          <div class="brand-feature"><i class="el-icon-magic-stick"></i><span>AI识菌</span></div>
        </div>
      </div>
      <svg class="brand-mountain" viewBox="0 0 960 300" preserveAspectRatio="xMidYMax slice" xmlns="http://www.w3.org/2000/svg">
        <path d="M0 300 L0 210 Q120 140 240 190 Q360 110 480 180 Q600 100 720 170 Q840 130 960 190 L960 300 Z" fill="rgba(255,255,255,0.07)"/>
        <path d="M0 300 L0 240 Q160 170 320 220 Q480 150 640 210 Q800 170 960 230 L960 300 Z" fill="rgba(255,255,255,0.09)"/>
        <path d="M0 300 L0 270 Q200 210 420 255 Q620 200 960 265 L960 300 Z" fill="rgba(255,255,255,0.08)"/>
      </svg>
    </div>

    <!-- 右侧登录区 -->
    <div class="form-side">
      <div class="login-card">
        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>野生菌 · 溯源 · 安全 · 智能识菌</p>
        </div>
        <el-form :model="form" :rules="rules" ref="loginForm" class="login-form">
          <el-form-item prop="username">
            <el-input v-model="form.username" prefix-icon="el-icon-user" placeholder="请输入用户名" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" prefix-icon="el-icon-lock" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter.native="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" style="width:100%;height:44px;font-size:15px;border-radius:8px" @click="handleLogin">登 录</el-button>
          </el-form-item>
          <div class="login-tips">
            <span @click="goRegister">没有账号？<b>立即注册</b></span>
          </div>
        </el-form>
        <div class="test-accounts">
          <p>测试账号（密码均为 123456）</p>
          <el-tag size="small" effect="plain" @click="fill('admin')">管理员 admin</el-tag>
          <el-tag size="small" effect="plain" type="success" @click="fill('shop1')">商家 shop1</el-tag>
          <el-tag size="small" effect="plain" type="warning" @click="fill('user1')">用户 user1</el-tag>
        </div>
      </div>
      <p class="copyright">© 2026 云岭山珍 · 高山野生菌线上销售商城</p>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog title="用户注册" :visible.sync="registerVisible" width="420px" :close-on-click-modal="false" @closed="resetRegisterForm">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名，用于登录" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码，6-20位" maxlength="20" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" maxlength="20" @keyup.enter.native="handleRegister" />
        </el-form-item>
        <el-form-item label="昵称" prop="name">
          <el-input v-model="registerForm.name" placeholder="请输入昵称/真实姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号（选填）" maxlength="11" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册并登录</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { login, register } from '@/api/user'

export default {
  name: 'Login',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请再次输入密码'))
      } else if (value !== this.registerForm.password) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    const validatePhone = (rule, value, callback) => {
      if (!value || value === '') {
        callback()
      } else if (!/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error('请输入正确的手机号'))
      } else {
        callback()
      }
    }
    return {
      emoji: 'mushroom',
      form: { username: '', password: '' },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false,
      // 注册
      registerVisible: false,
      registerLoading: false,
      registerForm: { username: '', password: '', confirmPassword: '', name: '', phone: '' },
      registerRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度 3-20 位', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ],
        name: [
          { required: true, message: '请输入昵称', trigger: 'blur' },
          { min: 2, max: 20, message: '昵称长度 2-20 位', trigger: 'blur' }
        ],
        phone: [
          { validator: validatePhone, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    fill(username) {
      this.form.username = username
      this.form.password = '123456'
    },
    async handleLogin() {
      this.$refs.loginForm.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          const res = await login(this.form)
          sessionStorage.setItem('token', res.data.token)
          this.$store.dispatch('login', res.data)
          this.$message.success('登录成功')
          this.$router.push('/home').catch(() => {})
        } catch (e) {
          // 错误已在拦截器处理
        } finally {
          this.loading = false
        }
      })
    },
    goRegister() {
      this.registerVisible = true
    },
    resetRegisterForm() {
      this.registerForm = { username: '', password: '', confirmPassword: '', name: '', phone: '' }
      this.$refs.registerFormRef && this.$refs.registerFormRef.resetFields()
    },
    async handleRegister() {
      this.$refs.registerFormRef.validate(async valid => {
        if (!valid) return
        this.registerLoading = true
        try {
          // 注册
          const { username, password, name, phone } = this.registerForm
          await register({ username, password, name, phone, role: 2 })
          this.$message.success('注册成功，正在自动登录...')
          // 自动登录
          const res = await login({ username, password })
          sessionStorage.setItem('token', res.data.token)
          this.$store.dispatch('login', res.data)
          this.registerVisible = false
          this.$message.success('登录成功')
          this.$router.push('/home').catch(() => {})
        } catch (e) {
          // 错误已在拦截器处理
        } finally {
          this.registerLoading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  background: #0B3D2E;
}

/* ============ 左侧品牌区 ============ */
.brand-side {
  flex: 1.2;
  position: relative;
  background: linear-gradient(160deg, #0B3D2E 0%, #0F6E56 55%, #16A085 100%);
  display: flex;
  align-items: center;
  overflow: hidden;
}
.brand-content {
  position: relative;
  z-index: 2;
  padding: 0 60px;
  max-width: 560px;
  color: #fff;
}
.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}
.brand-logo span {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 2px;
}
.brand-title {
  font-size: 42px;
  font-weight: 700;
  line-height: 1.35;
  margin: 0 0 16px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.25);
}
.brand-slogan {
  font-size: 16px;
  color: rgba(255,255,255,0.85);
  margin: 0 0 36px;
  letter-spacing: 1px;
}
.brand-features {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.brand-feature {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255,255,255,0.12);
  border: 1px solid rgba(255,255,255,0.2);
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 13px;
  backdrop-filter: blur(4px);
}
.brand-feature i { font-size: 15px; }
.brand-mountain {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 40%;
  z-index: 1;
}

/* ============ 右侧登录区 ============ */
.form-side {
  flex: 1;
  background: linear-gradient(135deg, #f8fbf9 0%, #eef5f0 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 30px;
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 44px 40px 32px;
  box-shadow: 0 12px 40px rgba(11, 61, 46, 0.12);
}
.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 {
  font-size: 26px;
  color: #26215C;
  margin: 0 0 8px;
}
.login-header p {
  font-size: 13px;
  color: #999;
  margin: 0;
}
.login-form .el-input__inner { border-radius: 8px; }
.login-tips {
  text-align: center;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}
.login-tips b { color: #0F6E56; }
.test-accounts {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px dashed #e8e8e8;
  text-align: center;
}
.test-accounts p {
  font-size: 11px;
  color: #aaa;
  margin-bottom: 10px;
}
.test-accounts .el-tag {
  cursor: pointer;
  margin: 0 3px;
}
.copyright {
  margin-top: 24px;
  font-size: 12px;
  color: #b0b8b2;
}
</style>
