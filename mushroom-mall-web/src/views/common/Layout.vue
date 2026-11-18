<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo-area">
        <svg v-if="!isCollapse" viewBox="0 0 48 48" width="30" height="30" xmlns="http://www.w3.org/2000/svg" class="logo-icon">
          <rect x="21" y="28" width="6" height="12" rx="3" fill="#E8D5B5"/>
          <path d="M9 30 Q24 8 39 30 Z" fill="#D85A30"/>
          <circle cx="16" cy="22" r="2.6" fill="#F4E9D8"/>
          <circle cx="27" cy="25" r="2.2" fill="#F4E9D8"/>
          <circle cx="24" cy="18" r="2" fill="#F4E9D8"/>
        </svg>
        <svg v-else viewBox="0 0 48 48" width="26" height="26" xmlns="http://www.w3.org/2000/svg">
          <rect x="21" y="28" width="6" height="12" rx="3" fill="#E8D5B5"/>
          <path d="M9 30 Q24 8 39 30 Z" fill="#D85A30"/>
          <circle cx="16" cy="22" r="2.6" fill="#F4E9D8"/>
          <circle cx="27" cy="25" r="2.2" fill="#F4E9D8"/>
        </svg>
        <span v-if="!isCollapse" class="logo-text">云岭山珍商城</span>
      </div>
      <el-menu :default-active="$route.path" router :collapse="isCollapse"
               background-color="transparent" text-color="rgba(255,255,255,0.75)" active-text-color="#fff">
        <el-menu-item index="/home">
          <i class="el-icon-house"></i>
          <span slot="title">首页</span>
        </el-menu-item>

        <!-- ========== 管理员菜单 (role=0) ========== -->
        <template v-if="role === 0">
          <el-menu-item index="/admin/dashboard">
            <i class="el-icon-data-line"></i><span slot="title">数据大屏</span>
          </el-menu-item>
          <el-menu-item index="/admin/user">
            <i class="el-icon-user"></i><span slot="title">用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/category">
            <i class="el-icon-menu"></i><span slot="title">分类管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/product-audit">
            <i class="el-icon-finished"></i><span slot="title">商品审核</span>
          </el-menu-item>
          <el-menu-item index="/admin/order">
            <i class="el-icon-tickets"></i><span slot="title">订单管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/banner">
            <i class="el-icon-picture"></i><span slot="title">轮播图管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/video">
            <i class="el-icon-video-camera"></i><span slot="title">宣传视频管理</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <i class="el-icon-s-custom"></i><span slot="title">个人中心</span>
          </el-menu-item>
        </template>

        <!-- ========== 商家菜单 (role=1) ========== -->
        <template v-if="role === 1">
          <el-menu-item index="/merchant/product">
            <i class="el-icon-goods"></i><span slot="title">商品管理</span>
          </el-menu-item>
          <el-menu-item index="/merchant/order">
            <i class="el-icon-tickets"></i><span slot="title">订单处理</span>
          </el-menu-item>
          <el-menu-item index="/merchant/statistics">
            <i class="el-icon-data-line"></i><span slot="title">销售统计</span>
          </el-menu-item>
          <el-menu-item index="/merchant/review">
            <i class="el-icon-star-off"></i><span slot="title">评价管理</span>
          </el-menu-item>
          <el-menu-item index="/merchant/chat">
            <i class="el-icon-chat-dot-round"></i><span slot="title">客服沟通</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <i class="el-icon-s-custom"></i><span slot="title">个人中心</span>
          </el-menu-item>
        </template>

        <!-- ========== 普通用户菜单 (role=2) ========== -->
        <template v-if="role === 2">
          <el-menu-item index="/user/shop">
            <i class="el-icon-goods"></i><span slot="title">商城浏览</span>
          </el-menu-item>
          <el-menu-item index="/user/cart">
            <i class="el-icon-shopping-cart-2"></i><span slot="title">购物车</span>
          </el-menu-item>
          <el-menu-item index="/user/order">
            <i class="el-icon-tickets"></i><span slot="title">我的订单</span>
          </el-menu-item>
          <el-menu-item index="/user/address">
            <i class="el-icon-location-information"></i><span slot="title">收货地址</span>
          </el-menu-item>
          <el-menu-item index="/user/ai-assistant">
            <i class="el-icon-magic-stick"></i><span slot="title">AI助手</span>
          </el-menu-item>
          <el-menu-item index="/user/chat">
            <i class="el-icon-headset"></i><span slot="title">在线客服</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <i class="el-icon-s-custom"></i><span slot="title">个人中心</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧主区域 -->
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
             @click="isCollapse = !isCollapse" class="collapse-btn"></i>
          <span class="page-title">{{ $route.meta.title || '首页' }}</span>
        </div>
        <div class="header-right">
          <span class="role-tag" :class="'role-' + role">{{ roleName }}</span>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <img v-if="userInfo && userInfo.avatar" :src="userInfo.avatar" class="header-avatar" />
              <span v-else class="header-avatar header-avatar-default">
                <i class="el-icon-user-solid"></i>
              </span>
              <span>{{ userInfo && userInfo.name }}</span>
              <i class="el-icon-arrow-down"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'Layout',
  data() {
    return { isCollapse: false }
  },
  computed: {
    userInfo() { return this.$store.state.userInfo },
    role() { return this.userInfo ? this.userInfo.role : null },
    roleName() {
      const map = { 0: '管理员', 1: '商家', 2: '普通用户' }
      return map[this.role] || ''
    }
  },
  created() {
    // 校验当前 token 与本地用户信息是否一致（防止多账号同时登录导致 token 被覆盖）
    this.checkTokenMatch()
  },
  methods: {
    async checkTokenMatch() {
      const token = sessionStorage.getItem('token')
      if (!token || !this.userInfo) return
      try {
        const res = await request.get('/user/info')
        const serverUser = res.data
        if (!serverUser) return
        if (serverUser.role !== this.userInfo.role || serverUser.username !== this.userInfo.username) {
          this.$message.warning('检测到账号信息已变更，请重新登录')
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('userInfo')
          this.$store.dispatch('logout')
          // 延迟跳转，避免在导航过程中触发 NavigationFailure 红色覆盖层
          this.$nextTick(() => {
            if (this.$route.path !== '/login') {
              this.$router.push('/login').catch(() => {})
            }
          })
        }
      } catch (e) { /* ignore */ }
    },
    handleCommand(cmd) {
      if (cmd === 'logout') {
        this.$confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(() => {
          sessionStorage.removeItem('token')
          this.$store.dispatch('logout')
          this.$router.push('/login')
        }).catch(() => {})
      } else if (cmd === 'profile') {
        this.$router.push('/profile')
      }
    }
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.sidebar {
  background: linear-gradient(180deg, #0B3D2E 0%, #0F6E56 60%, #12835f 100%);
  transition: width 0.3s;
  overflow-x: hidden;
}
.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.12);
}
.logo-icon { flex-shrink: 0; }
.logo-text { font-size: 17px; font-weight: 600; letter-spacing: 1px; white-space: nowrap; }
.el-menu { border-right: none; }
.el-menu-item { border-radius: 8px; margin: 4px 8px; }
.el-menu-item.is-active { background: rgba(255,255,255,0.18) !important; }
.el-menu-item:hover { background: rgba(255,255,255,0.1) !important; }
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.collapse-btn { font-size: 20px; cursor: pointer; color: #0F6E56; }
.page-title { font-size: 16px; font-weight: 600; color: #333; }
.header-right { display: flex; align-items: center; gap: 16px; }
.role-tag {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
}
.role-0 { background: #EEEDFE; color: #534AB7; }
.role-1 { background: #E1F5EE; color: #0F6E56; }
.role-2 { background: #E6F1FB; color: #185FA5; }
.user-info { cursor: pointer; display: flex; align-items: center; gap: 8px; color: #333; }
.header-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
  flex-shrink: 0;
}
.header-avatar-default {
  background: linear-gradient(135deg, #0F6E56, #16A085);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.main-content {
  background: linear-gradient(135deg, #f6faf8 0%, #f0f5f2 100%);
  padding: 20px;
}
</style>
