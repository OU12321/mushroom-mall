import request from '@/utils/request'

// 分页查询评价
export function getReviewPage(params) {
  return request({ url: '/review/page', method: 'get', params })
}

// 新增评价
export function addReview(data) {
  return request({ url: '/review', method: 'post', data })
}

// 商家回复评价
export function replyReview(id, reply) {
  return request({ url: `/review/reply/${id}`, method: 'put', data: { reply } })
}

// 删除评价
export function deleteReview(id) {
  return request({ url: `/review/${id}`, method: 'delete' })
}
