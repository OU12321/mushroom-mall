<template>
  <div class="product-detail" v-loading="loading">
    <div class="detail-header">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回</el-button>
    </div>

    <el-row :gutter="20" v-if="product">
      <el-col :span="10">
        <div class="product-image">
          <img :src="product.image || defaultImg" alt="" />
        </div>
      </el-col>
      <el-col :span="14">
        <div class="product-info">
          <h2 class="product-name">{{ product.name }}</h2>
          <p class="product-price">¥{{ (product.price || 0).toFixed(2) }}/kg</p>

          <el-descriptions :column="1" border size="small" style="margin-top:15px">
            <el-descriptions-item label="产地">{{ product.origin || '-' }}</el-descriptions-item>
            <el-descriptions-item label="采摘时间">{{ product.harvestTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="食用方法">{{ product.cookingMethod || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销量">{{ product.saleCount }}kg</el-descriptions-item>
            <el-descriptions-item label="库存">
              <span v-if="product.stock > 0" style="color:#67C23A;font-weight:600">有货 ({{ product.stock }}kg)</span>
              <span v-else style="color:#F56C6C;font-weight:600">已售罄</span>
            </el-descriptions-item>
          </el-descriptions>

          <p class="product-desc" v-if="product.description">{{ product.description }}</p>

          <el-alert v-if="product.stock <= 0" title="该商品已售罄，暂时无法购买" type="error" :closable="false" style="margin-top:15px" />

          <!-- 操作按钮 -->
          <div class="action-bar">
            <el-input-number v-model="buyNumber" :min="1" :max="product.stock || 1" size="medium" :disabled="product.stock <= 0" />
            <el-button type="success" size="medium" @click="addToCart" :disabled="product.stock <= 0">加入购物车</el-button>
            <el-button type="primary" size="medium" @click="buyNow" :disabled="product.stock <= 0">立即购买</el-button>
            <el-button type="warning" size="medium" plain @click="contactMerchant">联系商家</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 产地故事 -->
    <div class="story-banner" v-if="product">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="story-card" style="background:linear-gradient(135deg,#E8F5E9,#C8E6C9)">
            <i class="el-icon-mountain" style="color:#0F6E56;font-size:32px"></i>
            <h4>高山产地</h4>
            <p>{{ product.origin || '云南高山原始森林' }}</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="story-card" style="background:linear-gradient(135deg,#FFF3E0,#FFE0B2)">
            <i class="el-icon-time" style="color:#F57C00;font-size:32px"></i>
            <h4>应季采摘</h4>
            <p>{{ product.harvestTime || '当季新鲜采摘' }}</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="story-card" style="background:linear-gradient(135deg,#E3F2FD,#BBDEFB)">
            <i class="el-icon-food" style="color:#185FA5;font-size:32px"></i>
            <h4>食用方法</h4>
            <p>{{ product.cookingMethod || '多种烹饪方式' }}</p>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 安全承诺 -->
    <div class="safety-banner">
      <div class="safety-item"><i class="el-icon-circle-check"></i><span>品质溯源</span></div>
      <div class="safety-item"><i class="el-icon-circle-check"></i><span>冷链保鲜</span></div>
      <div class="safety-item"><i class="el-icon-circle-check"></i><span>安全检测</span></div>
      <div class="safety-item"><i class="el-icon-circle-check"></i><span>假一赔十</span></div>
    </div>

    <!-- 评价列表 -->
    <el-card style="margin-top:20px">
      <div slot="header">商品评价</div>
      <div v-if="reviews.length === 0" style="text-align:center;color:#aaa;padding:20px">暂无评价</div>
      <div class="review-item" v-for="r in reviews" :key="r.id">
        <div class="review-header">
          <span class="review-user">{{ r.userName || '匿名用户' }}</span>
          <el-rate :value="r.score" :disabled="true" style="display:inline-block" />
          <span class="review-time">{{ r.createTime || '' }}</span>
        </div>
        <p class="review-content">{{ r.content }}</p>
        <div v-if="r.images" class="review-images">
          <el-image v-for="(img, idx) in parseImages(r.images)" :key="idx" :src="img" style="width:60px;height:60px;margin-right:5px" fit="cover" :preview-src-list="parseImages(r.images)" />
        </div>
        <div v-if="r.reply" class="review-reply">
          <el-tag size="mini" type="success">商家回复</el-tag> {{ r.reply }}
        </div>
      </div>
      <el-pagination style="margin-top:10px;text-align:right"
        @current-change="handleReviewPageChange"
        :current-page="reviewQuery.page"
        :page-size="reviewQuery.pageSize"
        layout="total, prev, pager, next"
        :total="reviewTotal" />
    </el-card>
  </div>
</template>

<script>
import { getProductById } from '@/api/product'
import { addCart } from '@/api/cart'
import { getReviewPage } from '@/api/review'

export default {
  name: 'UserProductDetail',
  data() {
    return {
      loading: false,
      product: null,
      buyNumber: 1,
      reviews: [],
      reviewTotal: 0,
      reviewQuery: { productId: null, page: 1, pageSize: 10 },
      defaultImg: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="200" height="150"><rect fill="#E1F5EE" width="200" height="150"/><text x="100" y="80" text-anchor="middle" fill="#0F6E56" font-size="14">野生菌</text></svg>')
    }
  },
  created() {
    const id = this.$route.params.id
    this.reviewQuery.productId = id
    this.loadProduct(id)
    this.loadReviews()
  },
  methods: {
    parseImages(images) {
      if (!images) return []
      if (typeof images === 'string') {
        try { return JSON.parse(images) } catch (e) { return images.split(',') }
      }
      return images
    },
    async loadProduct(id) {
      this.loading = true
      try {
        const res = await getProductById(id)
        this.product = res.data
      } finally { this.loading = false }
    },
    async loadReviews() {
      try {
        const res = await getReviewPage(this.reviewQuery)
        this.reviews = res.data.records || []
        this.reviewTotal = res.data.total || 0
      } catch (e) { /* ignore */ }
    },
    handleReviewPageChange(val) { this.reviewQuery.page = val; this.loadReviews() },
    async addToCart() {
      if (!this.product) return
      await addCart({
        productId: this.product.id,
        number: this.buyNumber,
        merchantId: this.product.merchantId,
        productName: this.product.name,
        productImage: this.product.image,
        price: this.product.price
      })
      this.$message.success('已加入购物车')
    },
    buyNow() {
      // 跳转到购物车结算
      this.addToCart().then(() => {
        this.$router.push('/user/cart')
      })
    },
    goBack() {
      this.$router.back()
    },
    contactMerchant() {
      if (!this.product || !this.product.merchantId) return
      this.$router.push({ path: '/user/chat', query: { merchantId: this.product.merchantId, name: this.product.merchantName || '商家' } })
    }
  }
}
</script>

<style scoped>
.product-detail { padding: 0; }
.detail-header { margin-bottom: 10px; }
.product-image { text-align: center; background: #f9f9f9; border-radius: 8px; padding: 10px; }
.product-image img { max-width: 100%; max-height: 350px; }
.product-name { font-size: 22px; color: #333; margin-bottom: 10px; }
.product-price { font-size: 26px; font-weight: 600; color: #D85A30; }
.product-desc { font-size: 14px; color: #666; line-height: 1.6; margin-top: 12px; }
.action-bar { margin-top: 20px; display: flex; align-items: center; gap: 10px; }
.review-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.review-header { display: flex; align-items: center; gap: 8px; }
.review-user { font-size: 13px; font-weight: 500; color: #333; }
.review-time { font-size: 11px; color: #aaa; }
.review-content { font-size: 14px; color: #555; margin: 6px 0; }
.review-images { margin: 6px 0; }
.review-reply { background: #f5f5f5; padding: 8px; margin-top: 6px; font-size: 13px; color: #666; }

/* 产地故事区 */
.story-banner { margin-top: 25px; padding: 20px; background: #fafafa; border-radius: 10px; }
.story-card { text-align: center; padding: 25px 15px; border-radius: 10px; min-height: 130px; }
.story-card h4 { margin: 10px 0 6px; font-size: 16px; color: #333; }
.story-card p { font-size: 13px; color: #666; line-height: 1.5; margin: 0; }

/* 安全承诺横幅 */
.safety-banner {
  margin-top: 20px;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 18px 30px;
  background: linear-gradient(135deg, #2d8659 0%, #1a5c3a 100%);
  border-radius: 10px;
}
.safety-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
}
.safety-item i { font-size: 20px; color: #b9f6ca; }
</style>
