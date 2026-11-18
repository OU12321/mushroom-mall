import request from '@/utils/request'

/** 获取与某人的聊天历史 */
export function getChatHistory(otherId, otherRole) {
  return request.get('/chat/history', { params: { otherId, otherRole } })
}

/** 获取未读消息数 */
export function getUnreadCount() {
  return request.get('/chat/unread')
}

/** 获取联系人列表 */
export function getContacts() {
  return request.get('/chat/contacts')
}
