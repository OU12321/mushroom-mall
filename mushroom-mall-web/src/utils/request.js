import axios from 'axios'
import { Message } from 'element-ui'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器: 携带 JWT token
request.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器: 统一处理结果
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 1) {
      return res
    }
    // 业务级 401: 拦截器返回了 {code:401} 但 HTTP 状态为 200（兼容处理）
    if (res.code === 401) {
      Message.error(res.msg || '登录已过期，请重新登录')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('userInfo')
      if (router.currentRoute.path !== '/login') {
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg || '未登录'))
    }
    // 业务错误
    // 标记 handled：提示已在下面弹出，main.js 的 unhandledrejection 会静默该错误，
    // 避免页面漏写 try/catch 时弹出 dev 环境的红色报错覆盖层
    const bizError = new Error(res.msg || '请求失败')
    bizError.handled = true
    Message.error(res.msg || '请求失败')
    return Promise.reject(bizError)
  },
  error => {
    const status = error.response && error.response.status
    if (status === 401) {
      // 未登录或 token 过期（已统一提示和跳转，不抛出避免触发 dev 覆盖层）
      Message.error('登录已过期，请重新登录')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('userInfo')
      if (router.currentRoute.path !== '/login') {
        router.push('/login')
      }
    } else if (!error.response) {
      // 网络连接失败（后端未启动等），不踢用户、不清 token
      // 原因：后端重启后 token 仍然有效，清掉会导致用户必须重新登录
      Message.error('无法连接服务器，请检查后端是否启动')
    } else {
      // 500 等服务端异常：尝试从后端响应体提取 msg，避免再次抛出触发红色覆盖层
      const serverMsg = error.response.data && error.response.data.msg
      Message.error(serverMsg || '网络异常，请稍后重试')
    }
    // 401 / 网络错误 / 5xx 时返回空数据，让 UI 优雅处理，不触发红色覆盖层
    if (status === 401 || !error.response || status >= 500) {
      return Promise.resolve({ code: status || -1, msg: '服务异常', data: null })
    }
    return Promise.reject(error)
  }
)

export default request
