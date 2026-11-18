<template>
  <div class="chat-page">
    <el-container class="chat-container">
      <!-- 左侧联系人列表 -->
      <el-aside width="260px" class="contact-aside">
        <div class="contact-header">
          <span>消息列表</span>
          <el-badge :value="totalUnread" :hidden="totalUnread === 0" class="total-badge" />
        </div>
        <div class="contact-list">
          <div v-if="contacts.length === 0" class="no-contact">
            <i class="el-icon-chat-dot-square" style="font-size:40px;color:#ddd"></i>
            <p>暂无消息</p>
            <p style="font-size:12px;color:#bbb">在商品详情页点击"联系商家"开始对话</p>
          </div>
          <div
            v-for="c in contacts"
            :key="c.otherId + '_' + c.otherRole"
            class="contact-item"
            :class="{ active: activeContact && activeContact.otherId === c.otherId }"
            @click="selectContact(c)"
          >
            <el-avatar :size="40" :src="c.avatar || ''" style="flex-shrink:0">
              <i class="el-icon-user-solid"></i>
            </el-avatar>
            <div class="contact-info">
              <div class="contact-top">
                <span class="contact-name">{{ c.name }}</span>
                <span class="contact-time">{{ formatTime(c.lastTime) }}</span>
              </div>
              <div class="contact-bottom">
                <span class="contact-msg">{{ c.lastMessage }}</span>
                <el-badge :value="c.unreadCount" :hidden="!c.unreadCount" />
              </div>
            </div>
          </div>
        </div>
      </el-aside>

      <!-- 右侧聊天窗口 -->
      <el-main class="chat-main">
        <div v-if="!activeContact" class="no-chat">
          <i class="el-icon-chat-line-square" style="font-size:60px;color:#ddd"></i>
          <p style="color:#999;margin-top:15px">选择一位联系人开始聊天</p>
        </div>
        <template v-else>
          <div class="chat-header">
            <span class="chat-title">{{ activeContact.name }}</span>
            <el-tag size="mini" :type="activeContact.otherRole === 1 ? 'warning' : 'primary'">
              {{ activeContact.otherRole === 1 ? '商家' : '用户' }}
            </el-tag>
          </div>
          <div class="chat-body" ref="chatBody">
            <div v-for="msg in messageList" :key="msg.id" class="msg-item" :class="{ 'msg-mine': msg.senderId === userId }">
              <el-avatar :size="34" :src="msg.senderId === userId ? myAvatar : activeContact.avatar || ''">
                <i class="el-icon-user-solid"></i>
              </el-avatar>
              <div class="msg-content">
                <p class="msg-time">{{ msg.createTime }}</p>
                <div class="msg-bubble">{{ msg.content }}</div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="输入消息，Ctrl+Enter发送"
              @keydown.native.ctrl.enter="sendMessage"
              resize="none"
            />
            <el-button type="primary" @click="sendMessage" :disabled="!inputText.trim()">发送</el-button>
          </div>
        </template>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { getChatHistory, getContacts } from '@/api/chat'

export default {
  name: 'UserChat',
  data() {
    return {
      userId: null,
      userRole: 2,
      myAvatar: '',
      ws: null,
      refreshTimer: null,
      contacts: [],
      activeContact: null,
      messageList: [],
      inputText: '',
      totalUnread: 0
    }
  },
  mounted() {
    const userInfo = this.$store.state.userInfo || {}
    this.userId = userInfo.userId
    this.myAvatar = userInfo.avatar || ''
    this.connectWebSocket()
    this.loadContacts().then(() => {
      // 从商品详情页跳转过来时，自动选中商家
      const merchantId = this.$route.query.merchantId
      if (merchantId) {
        let contact = this.contacts.find(c => String(c.otherId) === String(merchantId))
        if (!contact) {
          contact = { otherId: parseInt(merchantId), otherRole: 1, name: this.$route.query.name || '商家', avatar: '', unreadCount: 0 }
          this.contacts.unshift(contact)
        }
        this.selectContact(contact)
      } else if (this.contacts.length > 0 && !this.activeContact) {
        // 切回页面时自动选中最近联系人
        this.selectContact(this.contacts[0])
      }
    })
    // 定期刷新联系人列表
    this.refreshTimer = setInterval(() => {
      this.loadContacts()
    }, 10000)
  },
  beforeDestroy() {
    if (this.ws) {
      this.ws.close()
    }
    if (this.refreshTimer) clearInterval(this.refreshTimer)
  },
  methods: {
    connectWebSocket() {
      // 携带 JWT 连接，服务端在握手时校验身份（原来传 userId/role 可被随意伪造）
      const token = sessionStorage.getItem('token')
      const wsUrl = `ws://${window.location.hostname}:8080/api/ws/chat?token=${token}`
      this.ws = new WebSocket(wsUrl)
      this.ws.onopen = () => { console.log('[User Chat] WebSocket连接成功') }
      this.ws.onmessage = (event) => {
        const msg = JSON.parse(event.data)
        console.log('[User Chat] 收到消息:', msg)
        // 如果是当前聊天的对象，直接显示
        if (this.activeContact && msg.senderId === this.activeContact.otherId) {
          this.messageList.push(msg)
          this.$nextTick(() => this.scrollToBottom())
        }
        // 刷新联系人列表和未读数
        this.loadContacts()
      }
      this.ws.onclose = () => {
        console.log('[User Chat] WebSocket断开,3秒后重连')
        setTimeout(() => this.connectWebSocket(), 3000)
      }
      this.ws.onerror = (e) => { console.error('[User Chat] WebSocket错误', e) }
    },
    async loadContacts() {
      try {
        const res = await getContacts()
        this.contacts = res.data || []
        this.totalUnread = this.contacts.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
      } catch (e) { /* ignore */ }
    },
    async selectContact(contact) {
      this.activeContact = contact
      try {
        const res = await getChatHistory(contact.otherId, contact.otherRole)
        this.messageList = res.data || []
        this.$nextTick(() => this.scrollToBottom())
        this.loadContacts() // 刷新未读数
      } catch (e) { /* ignore */ }
    },
    sendMessage() {
      const text = this.inputText.trim()
      if (!text || !this.activeContact || !this.ws) return
      const msg = {
        senderId: this.userId,
        senderRole: this.userRole,
        receiverId: this.activeContact.otherId,
        receiverRole: this.activeContact.otherRole,
        content: text
      }
      this.ws.send(JSON.stringify(msg))
      this.inputText = ''
      // 乐观更新：立即显示自己的消息
      this.messageList.push({
        id: Date.now(),
        senderId: this.userId,
        senderRole: this.userRole,
        receiverId: this.activeContact.otherId,
        receiverRole: this.activeContact.otherRole,
        content: text,
        createTime: this.formatNow(),
        isRead: 0
      })
      this.$nextTick(() => this.scrollToBottom())
      this.loadContacts()
    },
    scrollToBottom() {
      const el = this.$refs.chatBody
      if (el) el.scrollTop = el.scrollHeight
    },
    formatTime(time) {
      if (!time) return ''
      return time.substring(5, 16)
    },
    formatNow() {
      const d = new Date()
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }
  }
}
</script>

<style scoped>
.chat-page { height: calc(100vh - 100px); }
.chat-container { height: 100%; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; }
.contact-aside { border-right: 1px solid #e4e7ed; background: #fafafa; }
.contact-header { padding: 15px; font-weight: 600; font-size: 15px; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; justify-content: space-between; }
.contact-list { overflow-y: auto; max-height: calc(100% - 50px); }
.no-contact { text-align: center; padding: 40px 20px; color: #ccc; }
.no-contact p { margin: 8px 0 0; font-size: 13px; }
.contact-item { display: flex; align-items: center; padding: 12px 15px; cursor: pointer; border-bottom: 1px solid #f0f0f0; transition: background 0.2s; }
.contact-item:hover { background: #f0f7ff; }
.contact-item.active { background: #e6f7ff; }
.contact-info { flex: 1; margin-left: 10px; overflow: hidden; }
.contact-top { display: flex; justify-content: space-between; align-items: center; }
.contact-name { font-size: 14px; font-weight: 500; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.contact-time { font-size: 11px; color: #bbb; flex-shrink: 0; margin-left: 8px; }
.contact-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.contact-msg { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 160px; }
.chat-main { display: flex; flex-direction: column; padding: 0; background: #fff; }
.no-chat { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.chat-header { padding: 15px 20px; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; gap: 10px; }
.chat-title { font-size: 16px; font-weight: 600; }
.chat-body { flex: 1; overflow-y: auto; padding: 20px; background: #f5f5f5; }
.msg-item { display: flex; margin-bottom: 18px; }
.msg-item.msg-mine { flex-direction: row-reverse; }
.msg-content { margin: 0 10px; max-width: 60%; }
.msg-mine .msg-content { text-align: right; }
.msg-time { font-size: 11px; color: #bbb; margin: 0 0 4px; }
.msg-bubble { display: inline-block; padding: 10px 14px; border-radius: 10px; font-size: 14px; line-height: 1.5; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,0.08); text-align: left; word-break: break-all; }
.msg-mine .msg-bubble { background: #0F6E56; color: #fff; }
.chat-input { padding: 12px 15px; border-top: 1px solid #e4e7ed; display: flex; gap: 10px; align-items: flex-end; }
.chat-input .el-textarea { flex: 1; }
</style>
