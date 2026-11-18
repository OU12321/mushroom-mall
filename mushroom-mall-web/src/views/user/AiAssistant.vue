<template>
  <div class="ai-assistant">
    <el-card class="chat-card">
      <div slot="header" class="chat-header">
        <span><i class="el-icon-magic-stick"></i> AI助手</span>
        <el-tag size="mini" type="success">大模型驱动</el-tag>
      </div>

      <div class="chat-body" ref="chatBody">
        <div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
          <div class="avatar" :class="msg.role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">
            <div v-if="msg.image" class="msg-image">
              <img :src="msg.image" alt="上传图片" />
            </div>
            <p v-html="safeHtml(msg.content)"></p>
          </div>
        </div>
        <div v-if="loading" class="msg-row assistant">
          <div class="avatar assistant">AI</div>
          <div class="bubble"><p>正在识别中...</p></div>
        </div>
      </div>

      <div class="upload-area" v-if="showUpload">
        <el-upload
          :action="$uploadUrl"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleUploadSuccess"
          accept="image/*">
          <el-button icon="el-icon-camera" type="primary" plain>上传菌菇照片识别</el-button>
        </el-upload>
        <p class="upload-tip">上传一张菌菇照片，AI将判断是否可食用并给出建议</p>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入问题，如：这种菌能吃吗？怎么烹饪松茸？"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button type="primary" icon="el-icon-s-promotion" :loading="loading" @click="sendMessage">发送</el-button>
        <el-button icon="el-icon-picture-outline" @click="showUpload = !showUpload">识图</el-button>
      </div>
    </el-card>

    <el-card class="tip-card">
      <div slot="header">使用提示</div>
      <ul class="tips">
        <li>上传菌菇照片，AI可判断可食/有毒/慎食</li>
        <li>可询问烹饪方法、保存技巧、辨识要点</li>
        <li>AI结果仅供参考，不认识的菌菇请勿食用</li>
        <li>野生菌中毒请立即就医并拨打120</li>
      </ul>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'
import { sanitizeHtml } from '@/utils/sanitize'

export default {
  name: 'UserAiAssistant',
  data() {
    return {
      messages: [
        { role: 'assistant', content: '你好！我是AI助手。你可以上传菌菇照片让我识别，也可以问我任何关于野生菌的问题。' }
      ],
      inputText: '',
      loading: false,
      showUpload: false
    }
  },
  methods: {
    // 渲染前的净化：AI 会把用户评价原文带进回复，直接 v-html 会构成存储型 XSS
    safeHtml(html) {
      return sanitizeHtml(html)
    },
    handleUploadSuccess(res) {
      const imageUrl = res.data.url
      this.messages.push({ role: 'user', content: '请帮我识别这种菌菇', image: imageUrl })
      this.callAi('请识别这张菌菇图片，判断是否可食用，给出名称、安全等级和烹饪建议。', imageUrl)
    },
    async sendMessage() {
      if (!this.inputText.trim()) return
      const text = this.inputText
      this.messages.push({ role: 'user', content: text })
      this.inputText = ''
      this.callAi(text)
    },
    async callAi(text, image) {
      this.loading = true
      this.scrollToBottom()
      try {
        // AI 大模型响应较慢，单独给 AI 接口设 60 秒超时（避免全局 10 秒超时误报"无法连接服务器"）
        const res = await request.post('/ai/chat', { message: text, image: image || '' }, { timeout: 60000 })
        // 兼容 request.js 在 401/网络错误时返回的 {code:-1, data:null} 兜底
        if (res && res.code === 1 && res.data) {
          this.messages.push({ role: 'assistant', content: res.data })
        } else {
          // 接口未实现或后端未启动：走前端降级提示
          this.messages.push(this.buildFallbackReply(text, image))
        }
      } catch (e) {
        // 兜底：AI 不可用时不踢用户，给出友好提示
        this.messages.push(this.buildFallbackReply(text, image))
      } finally {
        this.loading = false
        this.scrollToBottom()
      }
    },
    buildFallbackReply(text, image) {
      if (image) {
        return {
          role: 'assistant',
          content: '⚠️ 图片识别服务暂不可用（请确认后端已启动）。<br><br>通用安全提示：<br>· 不确定的野生菌请勿食用<br>· 野生菌中毒可危及生命，如有不适立即就医并拨打120<br><br>你可以用文字描述菌菇特征或直接提问菌菇知识，我会尽力回答 🍄'
        }
      }
      const msg = (text || '').toLowerCase()
      // 简单的本地降级（不依赖后端）
      if (msg.includes('商品') || msg.includes('有什么') || msg.includes('商城') || msg.includes('卖什么')) {
        return { role: 'assistant', content: '⚠️ AI 服务暂不可用，无法查询实时商品数据。<br><br>请到左侧菜单 <b>商城浏览</b> 查看所有在售商品 🛒' }
      }
      if (msg.includes('松茸')) {
        return { role: 'assistant', content: '🍄 <b>松茸</b>（Tricholoma matsutake）<br>· 安全等级：可食用（名贵食用菌）<br>· 产地：云南香格里拉、四川、西藏等高原<br>· 烹饪：黄油煎松茸、松茸炖鸡、松茸刺身<br>· 采摘季节：7-10月<br><br><span style="color:#999;font-size:12px">（AI 大模型服务不可用，以上为本地预设回答）</span>' }
      }
      if (msg.includes('牛肝菌')) {
        return { role: 'assistant', content: '🍄 <b>牛肝菌</b>（Boletus）<br>· 产地：云南、四川、贵州<br>· 特点：菌肉肥厚，味道鲜美<br>· 注意：见手青必须充分炒熟，否则会引起幻觉中毒<br>· 采摘季节：6-9月<br><br><span style="color:#999;font-size:12px">（AI 大模型服务不可用，以上为本地预设回答）</span>' }
      }
      return {
        role: 'assistant',
        content: '⚠️ AI 大模型服务暂时不可用（请确认后端已启动后再试）。<br><br>你仍然可以尝试：<br>· 问"商城里有什么商品"<br>· 问"松茸怎么吃"<br>· 上传菌菇照片识别<br><br>本地知识库已内置部分常见菌菇的问答 🍄'
      }
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const body = this.$refs.chatBody
        if (body) body.scrollTop = body.scrollHeight
      })
    }
  }
}
</script>

<style scoped>
.ai-assistant { max-width: 900px; }
.chat-card { margin-bottom: 16px; }
.chat-header { display: flex; justify-content: space-between; align-items: center; }
.chat-body { height: 420px; overflow-y: auto; padding: 10px; background: #f9f9f9; border-radius: 8px; }
.msg-row { display: flex; gap: 10px; margin-bottom: 16px; }
.msg-row.user { flex-direction: row-reverse; }
.avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 500; flex-shrink: 0; }
.avatar.user { background: #185FA5; color: #fff; }
.avatar.assistant { background: #534AB7; color: #fff; }
.bubble { max-width: 70%; padding: 10px 14px; border-radius: 10px; font-size: 14px; line-height: 1.6; }
.msg-row.user .bubble { background: #185FA5; color: #fff; }
.msg-row.assistant .bubble { background: #fff; color: #333; border: 1px solid #eee; }
.msg-image img { max-width: 200px; border-radius: 6px; margin-bottom: 6px; }
.upload-area { text-align: center; padding: 12px 0; }
.upload-tip { font-size: 12px; color: #aaa; margin-top: 6px; }
.input-area { display: flex; gap: 10px; margin-top: 12px; align-items: flex-end; }
.input-area .el-button { height: 60px; }
.tips { padding-left: 18px; line-height: 2; color: #666; font-size: 13px; }
</style>
