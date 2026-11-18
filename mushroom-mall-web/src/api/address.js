import request from '@/utils/request'

// 查询当前用户收货地址列表
export function getAddressList() {
  return request({ url: '/address/list', method: 'get' })
}

// 新增收货地址
export function addAddress(data) {
  return request({ url: '/address', method: 'post', data })
}

// 修改收货地址
export function updateAddress(data) {
  return request({ url: '/address', method: 'put', data })
}

// 删除收货地址
export function deleteAddress(id) {
  return request({ url: `/address/${id}`, method: 'delete' })
}

// 设为默认地址
export function setDefaultAddress(id) {
  return request({ url: `/address/default/${id}`, method: 'put' })
}
