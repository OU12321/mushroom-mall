<template>
  <div>
    <el-card>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="productName" label="商品名称" width="160" />
        <el-table-column prop="userName" label="评价用户" width="120" />
        <el-table-column label="评分" width="150">
          <template slot-scope="{row}">
            <el-rate :value="row.score" :disabled="true" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" show-overflow-tooltip />
        <el-table-column label="评价图片" width="120">
          <template slot-scope="{row}">
            <el-image v-if="row.images" :src="parseImages(row.images)[0]" style="width:60px;height:60px" fit="cover" :preview-src-list="parseImages(row.images)" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="reply" label="商家回复" width="180">
          <template slot-scope="{row}">{{ row.reply || '未回复' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="{row}">
            <el-button v-if="!row.reply" size="mini" type="primary" @click="openReply(row)">回复</el-button>
            <el-button v-else size="mini" @click="openReply(row)">修改回复</el-button>
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

    <!-- 回复弹窗 -->
    <el-dialog title="回复评价" :visible.sync="replyVisible" width="500px">
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="评价内容">{{ replyRow.content }}</el-form-item>
        <el-form-item label="评分">
          <el-rate :value="replyRow.score" :disabled="true" />
        </el-form-item>
        <el-form-item label="回复内容" prop="reply">
          <el-input v-model="replyForm.reply" type="textarea" :rows="4" placeholder="输入回复内容" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getReviewPage, replyReview } from '@/api/review'

export default {
  name: 'ReviewManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryForm: { page: 1, pageSize: 10 },
      replyVisible: false,
      replyRow: {},
      replyForm: { reply: '' }
    }
  },
  created() { this.loadData() },
  methods: {
    parseImages(images) {
      if (!images) return []
      if (typeof images === 'string') {
        try { return JSON.parse(images) } catch (e) { return images.split(',') }
      }
      return images
    },
    async loadData() {
      this.loading = true
      try {
        const res = await getReviewPage(this.queryForm)
        this.tableData = res.data.records
        this.total = res.data.total
      } finally { this.loading = false }
    },
    handleSizeChange(val) { this.queryForm.pageSize = val; this.loadData() },
    handleCurrentChange(val) { this.queryForm.page = val; this.loadData() },
    openReply(row) {
      this.replyRow = row
      this.replyForm.reply = row.reply || ''
      this.replyVisible = true
    },
    async submitReply() {
      if (!this.replyForm.reply.trim()) {
        this.$message.warning('请输入回复内容')
        return
      }
      await replyReview(this.replyRow.id, this.replyForm.reply)
      this.$message.success('回复成功')
      this.replyVisible = false
      this.loadData()
    }
  }
}
</script>
