import Vue from 'vue'
import VueRouter from 'vue-router'
import { Message } from 'element-ui'
import store from '@/store'
import Login from '@/views/common/Login.vue'
import Layout from '@/views/common/Layout.vue'

Vue.use(VueRouter)

const routes = [
  { path: '/login', component: Login, meta: { public: true } },
  {
    path: '/',
    component: Layout,
    redirect: '/home',
    children: [
      // ============ 通用路由 (所有角色) ============
      { path: 'home', name: 'Home', component: () => import('@/views/common/Home.vue'), meta: { title: '首页' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/common/Profile.vue'), meta: { title: '个人中心' } },

      // ============ 管理员路由 (role=0) ============
      { path: 'admin/user', name: 'AdminUser', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理', role: 0 } },
      { path: 'admin/category', name: 'AdminCategory', component: () => import('@/views/admin/CategoryManage.vue'), meta: { title: '分类管理', role: 0 } },
      { path: 'admin/product-audit', name: 'AdminProductAudit', component: () => import('@/views/admin/ProductAudit.vue'), meta: { title: '商品审核', role: 0 } },
      { path: 'admin/order', name: 'AdminOrder', component: () => import('@/views/admin/OrderManage.vue'), meta: { title: '订单管理', role: 0 } },
      { path: 'admin/banner', name: 'AdminBanner', component: () => import('@/views/admin/BannerManage.vue'), meta: { title: '轮播图管理', role: 0 } },
      { path: 'admin/video', name: 'AdminVideo', component: () => import('@/views/admin/VideoManage.vue'), meta: { title: '宣传视频管理', role: 0 } },
      { path: 'admin/dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据大屏', role: 0 } },

      // ============ 商家路由 (role=1) ============
      { path: 'merchant/product', name: 'MerchantProduct', component: () => import('@/views/merchant/ProductManage.vue'), meta: { title: '商品管理', role: 1 } },
      { path: 'merchant/order', name: 'MerchantOrder', component: () => import('@/views/merchant/OrderManage.vue'), meta: { title: '订单处理', role: 1 } },
      { path: 'merchant/statistics', name: 'MerchantStatistics', component: () => import('@/views/merchant/Statistics.vue'), meta: { title: '销售统计', role: 1 } },
      { path: 'merchant/review', name: 'MerchantReview', component: () => import('@/views/merchant/ReviewManage.vue'), meta: { title: '评价管理', role: 1 } },
      { path: 'merchant/chat', name: 'MerchantChat', component: () => import('@/views/merchant/Chat.vue'), meta: { title: '客服沟通', role: 1 } },

      // ============ 普通用户路由 (role=2) ============
      { path: 'user/shop', name: 'UserShop', component: () => import('@/views/user/Shop.vue'), meta: { title: '商城浏览', role: 2 } },
      { path: 'user/product/:id', name: 'UserProductDetail', component: () => import('@/views/user/ProductDetail.vue'), meta: { title: '商品详情', role: 2 } },
      { path: 'user/cart', name: 'UserCart', component: () => import('@/views/user/Cart.vue'), meta: { title: '购物车', role: 2 } },
      { path: 'user/order', name: 'UserOrder', component: () => import('@/views/user/OrderList.vue'), meta: { title: '我的订单', role: 2 } },
      { path: 'user/pay', name: 'UserPay', component: () => import('@/views/user/Pay.vue'), meta: { title: '订单支付', role: 2 } },
      { path: 'user/address', name: 'UserAddress', component: () => import('@/views/user/AddressManage.vue'), meta: { title: '收货地址', role: 2 } },
      { path: 'user/ai-assistant', name: 'UserAiAssistant', component: () => import('@/views/user/AiAssistant.vue'), meta: { title: 'AI助手', role: 2 } },
      { path: 'user/chat', name: 'UserChat', component: () => import('@/views/user/Chat.vue'), meta: { title: '在线客服', role: 2 } }
    ]
  },
  // 404 兜底：访问不存在的路径时跳回首页，避免白屏且没有任何提示
  { path: '*', redirect: '/home' }
]

const router = new VueRouter({
  routes
})

// 全局前置守卫: 登录校验 + 角色校验
router.beforeEach((to, from, next) => {
  // 始终从 sessionStorage 读取最新 userInfo（保证 store 和 sessionStorage 一致）
  let userInfo = null
  try {
    const stored = sessionStorage.getItem('userInfo')
    if (stored) userInfo = JSON.parse(stored)
  } catch (e) { /* ignore parse error */ }
  const token = sessionStorage.getItem('token')

  // 同步到 store（如果 store 缺失但 sessionStorage 有，补充回去）
  if (userInfo && !store.state.userInfo) {
    store.commit('SET_USER_INFO', userInfo)
  }

  // 公开页面直接放行
  if (to.meta.public) {
    if (userInfo && to.path === '/login') {
      // 已登录却访问登录页，直接回首页
      // （原实现在此内部又判断 to.path === '/home'，永远不成立，已移除该死分支）
      return next('/home')
    }
    return next()
  }

  // 未登录跳转登录页
  // 注意：token 与 userInfo 必须同时存在。只清掉 token 时若仍放行，
  // 页面会加载但所有接口返回 401，用户体验等同于"页面打不开"。
  if (!userInfo || !token) {
    // 避免在登录页重复跳转（防止触发 NavigationFailure）
    if (to.path === '/login') return next()
    return next('/login')
  }

  // 角色权限校验
  if (to.meta.role !== undefined && to.meta.role !== userInfo.role) {
    // 关键修复：守卫重定向到自身或跳过的目标一致时，会触发 vue-router 同步抛 NavigationRedirectedError → 红色覆盖层
    // 解决：
    //   1. 若 from 已经在 /home（被踢回首页但再次跳转）：
    //        - return next(false) 中止导航，不会触发新导航
    //        - 但给个友好提示（仅第一次）
    //   2. 若要重定向到 /home 但当前就是 /home，直接 next(false)
    //   3. 其他情况才 next('/home')
    // 说明：前端角色校验只是 UI 层便利，真正的权限由后端 @RequireRole 拦截
    if (to.path !== from.path) {
      Message.warning('无权访问该页面')
    }
    if (to.path === '/home' || from.path === '/home') {
      // 已经在 /home 或正要跳到 /home，不需要二次跳转
      return next(false)
    }
    return next('/home')
  }

  next()
})

export default router
