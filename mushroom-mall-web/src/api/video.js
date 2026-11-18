import request from '@/utils/request'

// 分页查询宣传视频
export function getVideoPage(params) {
  return request({ url: '/video/page', method: 'get', params })
}

// 查询启用的宣传视频列表（用户端）
export function getVideoList() {
  return request({ url: '/video/list', method: 'get' })
}

// 新增宣传视频
export function addVideo(data) {
  return request({ url: '/video', method: 'post', data })
}

// 修改宣传视频
export function updateVideo(data) {
  return request({ url: '/video', method: 'put', data })
}

// 修改宣传视频状态
export function updateVideoStatus(id, status) {
  return request({ url: `/video/status/${id}`, method: 'put', params: { status } })
}

// 删除宣传视频
export function deleteVideo(id) {
  return request({ url: `/video/${id}`, method: 'delete' })
}
