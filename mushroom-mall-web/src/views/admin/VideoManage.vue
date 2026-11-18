<template>
  <div class="video-manage">
    <!-- 顶部操作卡片 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <i class="el-icon-video-camera toolbar-icon"></i>
          <div>
            <h3>宣传视频管理</h3>
            <p>上传高山野生菌主题宣传视频，用户端商城首页将自动展示启用的视频</p>
          </div>
        </div>
        <el-button type="primary" icon="el-icon-plus" @click="openDialog(null)">新增宣传视频</el-button>
      </div>
    </el-card>

    <!-- 视频列表卡片 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="封面" width="140">
          <template slot-scope="{row}">
            <div class="cover-cell">
              <img v-if="row.cover" :src="row.cover" class="cover-img" alt="" />
              <div v-else class="cover-img cover-placeholder"><i class="el-icon-video-camera"></i></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column label="视频预览" min-width="220">
          <template slot-scope="{row}">
            <video v-if="row.video" :src="row.video" class="video-preview" preload="metadata" controls />
            <span v-else class="text-muted">未上传</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template slot-scope="{row}">
            <span :class="row.description ? '' : 'text-muted'">{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="状态" width="90">
          <template slot-scope="{row}">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '启用中' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template slot-scope="{row}">
            <el-button size="mini" type="primary" plain @click="openDialog(row)">编辑</el-button>
            <el-button size="mini" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="mini" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top:15px;text-align:right"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="queryForm.page"
        :page-sizes="[10, 20, 50]"
        :page-size="queryForm.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total">
      </el-pagination>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="620px" @close="resetForm" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="视频标题" prop="title">
          <el-input v-model="form.title" placeholder="例如：香格里拉野生菌采摘纪实" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="视频文件" prop="video">
          <el-upload
            :action="$uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleVideoSuccess"
            :before-upload="beforeVideoUpload"
            accept="video/*">
            <div class="upload-box">
              <i class="el-icon-upload"></i>
              <span>{{ form.video ? '重新上传视频' : '点击上传视频' }}</span>
              <em v-if="form.video" class="video-name">{{ form.video.split('/').pop() }}</em>
            </div>
          </el-upload>
          <p class="upload-tip">支持 mp4 / webm / mov 格式，建议不超过 100MB</p>
          <video v-if="form.video" :src="form.video" class="dialog-video-preview" controls preload="metadata" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload :action="$uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleCoverSuccess" accept="image/*">
            <img v-if="form.cover" :src="form.cover" class="cover-upload" alt="" />
            <div v-else class="cover-upload cover-upload-empty">
              <i class="el-icon-plus"></i>
              <span>上传封面</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="视频描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="简要介绍视频内容（选填）" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="sort-tip">数字越小越靠前展示</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用（用户端展示）</el-radio>
            <el-radio :label="0">禁用（暂不展示）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getVideoPage, addVideo, updateVideo, updateVideoStatus, deleteVideo } from '@/api/video'

export default {
  name: 'VideoManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryForm: { page: 1, pageSize: 10 },
      dialogVisible: false,
      isEdit: false,
      form: { title: '', video: '', cover: '', description: '', sort: 0, status: 1 },
      rules: {
        title: [{ required: true, message: '请输入视频标题', trigger: 'blur' }],
        video: [{ required: true, message: '请上传视频文件', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() { return this.isEdit ? '编辑宣传视频' : '新增宣传视频' }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getVideoPage(this.queryForm)
        if (res && res.data) {
          this.tableData = res.data.records || []
          this.total = res.data.total || 0
        }
      } catch (e) {
        this.tableData = []
        this.total = 0
      } finally { this.loading = false }
    },
    handleSizeChange(val) { this.queryForm.pageSize = val; this.loadData() },
    handleCurrentChange(val) { this.queryForm.page = val; this.loadData() },
    openDialog(row) {
      this.resetForm()
      if (row) {
        this.isEdit = true
        this.form = { ...row }
      } else {
        this.isEdit = false
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.form = { title: '', video: '', cover: '', description: '', sort: 0, status: 1 }
      this.$refs.formRef && this.$refs.formRef.clearValidate()
    },
    beforeVideoUpload(file) {
      const isVideo = /\.(mp4|webm|mov|m4v|ogg)$/i.test(file.name)
      if (!isVideo) {
        this.$message.error('仅支持 mp4 / webm / mov 等视频格式')
        return false
      }
      if (file.size > 100 * 1024 * 1024) {
        this.$message.error('视频大小不能超过 100MB')
        return false
      }
      return true
    },
    handleVideoSuccess(res) {
      // 注意：后端成功码是 code=1（不是200），直接以 data.url 是否存在为准
      if (res && res.data && res.data.url) {
        this.form.video = res.data.url
        this.$message.success('视频上传成功')
        this.$refs.formRef && this.$refs.formRef.clearValidate('video')
      } else {
        this.$message.error((res && res.msg) || '视频上传失败')
      }
    },
    handleCoverSuccess(res) {
      if (res && res.data && res.data.url) {
        this.form.cover = res.data.url
      } else {
        this.$message.error((res && res.msg) || '封面上传失败')
      }
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        try {
          if (this.isEdit) {
            await updateVideo(this.form)
            this.$message.success('修改成功')
          } else {
            await addVideo(this.form)
            this.$message.success('新增成功')
          }
          this.dialogVisible = false
          this.loadData()
        } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
      })
    },
    async toggleStatus(row) {
      const newStatus = row.status === 1 ? 0 : 1
      try {
        await updateVideoStatus(row.id, newStatus)
        this.$message.success(newStatus === 1 ? '已启用' : '已禁用')
        this.loadData()
      } catch (e) { /* 错误提示已由 request.js 统一处理 */ }
    },
    handleDelete(row) {
      this.$confirm(`确认删除宣传视频 [${row.title}] 吗？`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteVideo(row.id)
          this.$message.success('删除成功')
          this.loadData()
        }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.video-manage { padding: 4px; }
.toolbar-card { margin-bottom: 15px; border-radius: 10px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.toolbar-left { display: flex; align-items: center; gap: 14px; }
.toolbar-icon { font-size: 32px; color: #0F6E56; }
.toolbar h3 { margin: 0 0 4px; font-size: 16px; color: #333; }
.toolbar p { margin: 0; font-size: 12px; color: #999; }
.table-card { border-radius: 10px; }
.cover-cell { display: flex; }
.cover-img { width: 110px; height: 62px; border-radius: 6px; object-fit: cover; }
.cover-placeholder {
  background: #f0f0f0;
  color: #bbb;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
}
.video-preview { width: 200px; height: 62px; border-radius: 6px; background: #000; }
.text-muted { color: #bbb; font-size: 12px; }
.upload-box {
  width: 100%; height: 70px;
  border: 1px dashed #c0c4cc; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; color: #666; transition: border-color 0.2s;
}
.upload-box:hover { border-color: #0F6E56; color: #0F6E56; }
.upload-box i { font-size: 26px; margin-bottom: 4px; }
.upload-box span { font-size: 13px; }
.video-name { font-style: normal; font-size: 11px; color: #999; margin-top: 4px; max-width: 420px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.upload-tip { font-size: 11px; color: #bbb; margin: 6px 0 0; }
.dialog-video-preview { width: 100%; max-height: 260px; margin-top: 10px; border-radius: 8px; background: #000; }
.cover-upload {
  width: 200px; height: 112px; border-radius: 8px; object-fit: cover;
  cursor: pointer; border: 1px solid #eee;
}
.cover-upload-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  border: 1px dashed #c0c4cc; color: #666; background: #fafafa;
}
.cover-upload-empty i { font-size: 24px; margin-bottom: 4px; }
.cover-upload-empty span { font-size: 12px; }
.sort-tip { font-size: 11px; color: #bbb; margin-left: 8px; }
</style>
