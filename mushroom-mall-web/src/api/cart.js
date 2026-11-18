import request from '@/utils/request'

// 查询购物车列表
export function getCartList() {
  return request({ url: '/cart/list', method: 'get' })
}

// 添加商品到购物车
export function addCart(data) {
  return request({ url: '/cart', method: 'post', data })
}

// 更新购物车商品数量
export function updateCart(data) {
  return request({ url: '/cart', method: 'put', data })
}

// 删除购物车商品
export function deleteCart(id) {
  return request({ url: `/cart/${id}`, method: 'delete' })
}
