import request from '@/utils/request'

// 获取管理员大屏统计数据
export function getDashboardData() {
  return request({ url: '/statistics/dashboard', method: 'get' })
}

// 获取商家销售统计数据
export function getMerchantData() {
  return request({ url: '/statistics/merchant', method: 'get' })
}

// 获取首页顶部统计卡片数据
export function getHomeData() {
  return request({ url: '/statistics/home', method: 'get' })
}
