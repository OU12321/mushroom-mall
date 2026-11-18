import request from '@/utils/request'

// 分页查询订单
export function getOrderPage(params) {
  return request({ url: '/orders/page', method: 'get', params })
}

// 查询订单详情
export function getOrderById(id) {
  return request({ url: `/orders/${id}`, method: 'get' })
}

// 创建订单
export function createOrder(data) {
  return request({ url: '/orders', method: 'post', data })
}

// 支付订单
export function payOrder(id) {
  return request({ url: `/orders/pay/${id}`, method: 'put' })
}

// 商家发货
export function shipOrder(id) {
  return request({ url: `/orders/ship/${id}`, method: 'put' })
}

// 用户确认收货
export function receiveOrder(id) {
  return request({ url: `/orders/receive/${id}`, method: 'put' })
}

// 取消订单
export function cancelOrder(id) {
  return request({ url: `/orders/cancel/${id}`, method: 'put' })
}

// 申请退款
export function refundOrder(id) {
  return request({ url: `/orders/refund/${id}`, method: 'put' })
}

// 同意退款
export function agreeRefundOrder(id) {
  return request({ url: `/orders/agreeRefund/${id}`, method: 'put' })
}
