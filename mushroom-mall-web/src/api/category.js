import request from '@/utils/request'

// 分页查询分类 (管理端)
export function getCategoryPage(params) {
  return request({
    url: '/category/page',
    method: 'get',
    params
  })
}

// 查询所有分类 (下拉选择用)
export function getCategoryList() {
  return request({
    url: '/category/list',
    method: 'get'
  })
}

// 新增分类
export function addCategory(data) {
  return request({
    url: '/category',
    method: 'post',
    data
  })
}

// 修改分类
export function updateCategory(data) {
  return request({
    url: '/category',
    method: 'put',
    data
  })
}

// 删除分类
export function deleteCategory(id) {
  return request({
    url: `/category/${id}`,
    method: 'delete'
  })
}
