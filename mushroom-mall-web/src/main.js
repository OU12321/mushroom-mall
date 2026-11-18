import Vue from 'vue'
import VueRouter from 'vue-router'
import App from './App.vue'
import router from './router'
import store from '@/store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './assets/main.css'

Vue.use(ElementUI)
Vue.config.productionTip = false

// =========================================================================
// 关键修复 1：Vue.config.errorHandler 必须在 new Vue() 之前注册
// =========================================================================
// 否则组件首次渲染时同步抛出的错误会走 Vue 默认处理（dev 模式显示红色覆盖层）
// 这里过滤 vue-router 3.x 在导航守卫中同步抛出的 NavigationFailure
Vue.config.errorHandler = (err, vm, info) => {
  if (err && typeof err === 'object' && err.name) {
    if (err.name === 'NavigationDuplicated' ||
        err.name === 'NavigationRedirected' ||
        err.name === 'NavigationAborted' ||
        err.name === 'NavigationCancelled' ||
        err.name === 'NavigationFailure') {
      return // 静默
    }
  }
  if (typeof err?.message === 'string' && err.message.includes('Redirected') && err.message.includes('navigation')) {
    return // 静默（兜底字符串匹配）
  }
  // 其他错误才打印控制台
  console.error('[Vue Error]', err, '\nInfo:', info)
}

// =========================================================================
// 关键修复 2：VueRouter.prototype.push/replace 静默 NavigationFailure
// =========================================================================
// vue-cli 3.x 默认会把"重复导航"的 rejection 包装成 unhandled rejection
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => {
    if (err && (err.name === 'NavigationDuplicated' ||
                err.name === 'NavigationRedirected' ||
                err.name === 'NavigationAborted' ||
                err.name === 'NavigationCancelled' ||
                err.name === 'NavigationFailure')) {
      return err
    }
    return Promise.reject(err)
  })
}
const originalReplace = VueRouter.prototype.replace
VueRouter.prototype.replace = function replace(location) {
  return originalReplace.call(this, location).catch(err => {
    if (err && (err.name === 'NavigationDuplicated' ||
                err.name === 'NavigationRedirected' ||
                err.name === 'NavigationAborted' ||
                err.name === 'NavigationCancelled' ||
                err.name === 'NavigationFailure')) {
      return err
    }
    return Promise.reject(err)
  })
}

// =========================================================================
// 全局兜底：unhandledrejection 静默
// =========================================================================
window.addEventListener('unhandledrejection', event => {
  const err = event.reason
  if (err && (err.name === 'NavigationDuplicated' ||
              err.name === 'NavigationRedirected' ||
              err.name === 'NavigationAborted' ||
              err.name === 'NavigationCancelled' ||
              err.name === 'NavigationFailure')) {
    event.preventDefault()
    return
  }
  // 业务错误：request.js 已弹出 Message 提示并给错误打上 handled 标记
  // 这里一并静默，避免个别页面漏写 try/catch 时弹出红色报错覆盖层
  if (err && err.handled) {
    event.preventDefault()
  }
})

// =========================================================================
// 全局上传配置
// =========================================================================
// 原来 7 处 el-upload 都硬编码 action="/api/common/upload"，与 vue.config.js 的
// 代理前缀强耦合，换部署路径就会全部失效。这里统一成全局变量，页面用 :action="$uploadUrl"
Vue.prototype.$uploadUrl = '/api/common/upload'

// el-upload 是独立于 axios 的请求，不会自动带上 JWT，需要显式传 headers
Vue.mixin({
  computed: {
    uploadHeaders() {
      const token = sessionStorage.getItem('token')
      return token ? { Authorization: 'Bearer ' + token } : {}
    }
  }
})

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')

// 在 Vue 挂载后再注册 router.onError（兜底）
router.onError(err => {
  if (err && (err.name === 'NavigationDuplicated' ||
              err.name === 'NavigationRedirected' ||
              err.name === 'NavigationAborted' ||
              err.name === 'NavigationCancelled' ||
              err.name === 'NavigationFailure')) {
    return
  }
  console.error('[Router Error]', err)
})
