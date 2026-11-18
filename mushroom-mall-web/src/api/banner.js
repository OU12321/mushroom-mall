import request from '@/utils/request'

// 分页查询轮播图
export function getBannerPage(params) {
  return request({ url: '/banner/page', method: 'get', params })
}

// 查询所有轮播图列表
export function getBannerList() {
  return request({ url: '/banner/list', method: 'get' })
}

// 新增轮播图
export function addBanner(data) {
  return request({ url: '/banner', method: 'post', data })
}

// 修改轮播图
export function updateBanner(data) {
  return request({ url: '/banner', method: 'put', data })
}

// 修改轮播图状态
export function updateBannerStatus(id, status) {
  return request({ url: `/banner/status/${id}`, method: 'put', params: { status } })
}

// 删除轮播图
export function deleteBanner(id) {
  return request({ url: `/banner/${id}`, method: 'delete' })
}
