import request from '@/utils/request'

// 商家/管理员分页查询商品
export function getProductPage(params) {
  return request({ url: '/product/page', method: 'get', params })
}

// 用户端浏览商品列表
export function getProductList(params) {
  return request({ url: '/product/list', method: 'get', params })
}

// 查询商品详情（公开接口）
export function getProductById(id) {
  return request({ url: `/product/detail/${id}`, method: 'get' })
}

// 新增商品
export function addProduct(data) {
  return request({ url: '/product', method: 'post', data })
}

// 修改商品
export function updateProduct(data) {
  return request({ url: '/product', method: 'put', data })
}

// 审核商品
export function auditProduct(id, auditStatus) {
  return request({ url: `/product/audit/${id}`, method: 'put', params: { auditStatus } })
}

// 修改商品上架状态
export function updateProductStatus(id, status) {
  return request({ url: `/product/status/${id}`, method: 'put', params: { status } })
}

// 删除商品
export function deleteProduct(id) {
  return request({ url: `/product/${id}`, method: 'delete' })
}
