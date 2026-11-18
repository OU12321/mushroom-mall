<template>
  <div class="shop">
    <!-- 轮播图 -->
    <el-carousel v-if="bannerList.length > 0" height="340px" :interval="4000" arrow="hover" class="banner-carousel">
      <el-carousel-item v-for="b in bannerList" :key="b.id">
        <div class="banner-item" :style="{ background: b.bgColor || 'linear-gradient(135deg, #0F6E56, #16A085)' }">
          <div class="banner-content">
            <h2 class="banner-title">{{ b.title }}</h2>
            <p class="banner-subtitle" v-if="b.subtitle">{{ b.subtitle }}</p>
          </div>
          <img v-if="b.image" :src="b.image" class="banner-img" alt="" />
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 品牌特色 -->
    <div class="features-section">
      <el-row :gutter="20">
        <el-col :span="6" v-for="(f, i) in features" :key="i">
          <div class="feature-card" :style="{ background: f.bg }">
            <i :class="f.icon" :style="{ color: f.color }"></i>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 视频介绍区（管理员后台配置） -->
    <div class="video-section" v-if="videoList.length > 0">
      <h2 class="section-title"><i class="el-icon-video-camera"></i> 高山野生菌主题宣传</h2>
      <div class="video-layout">
        <div class="video-main" :class="{ 'with-list': videoList.length > 1 }">
          <video :key="currentVideo.id" ref="mainVideo" controls :poster="currentVideo.cover || videoPoster" class="video-player">
            <source :src="currentVideo.video" />
            您的浏览器不支持视频播放
          </video>
          <div class="video-overlay" v-if="!videoLoaded" @click="playVideo">
            <img v-if="currentVideo.cover" :src="currentVideo.cover" class="video-poster" alt="" />
            <div class="play-button"><i class="el-icon-video-play"></i></div>
            <p class="video-desc">{{ currentVideo.description || '点击播放' }}</p>
          </div>
        </div>
        <!-- 多视频切换列表 -->
        <div class="video-list" v-if="videoList.length > 1">
          <div class="video-list-item" v-for="(v, i) in videoList" :key="v.id"
               :class="{ active: i === currentVideoIndex }" @click="switchVideo(i)">
            <img :src="v.cover || defaultImg" alt="" class="video-list-thumb" />
            <div class="video-list-info">
              <span class="video-list-title">{{ v.title }}</span>
              <span class="video-list-desc">{{ v.description || '' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类导航 -->
    <div class="category-section" v-if="categories.length > 0">
      <h2 class="section-title"><i class="el-icon-menu"></i> 菌菇分类</h2>
      <div class="category-grid">
        <div class="category-item" v-for="c in categories" :key="c.id" @click="searchByCategory(c.id)">
          <div class="cat-icon" :style="{ background: getCatColor(c.id) }">
            {{ c.name ? c.name.charAt(0) : '?' }}
          </div>
          <span>{{ c.name }}</span>
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="商品名称">
          <el-input v-model="query.name" placeholder="搜索野生菌" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width:140px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="loadProducts">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 商品列表 -->
    <div class="product-section">
      <h2 class="section-title"><i class="el-icon-goods"></i> 精选野生菌</h2>
      <el-row :gutter="16" class="product-list">
        <el-col :span="6" v-for="p in productList" :key="p.id">
          <el-card class="product-card" shadow="hover" @click.native="goDetail(p.id)">
            <div class="product-img">
              <img :src="p.image || defaultImg" alt="" />
              <div v-if="p.stock !== undefined && p.stock <= 0" class="sold-out-mask">
                <span>售罄</span>
              </div>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ p.name }}</h3>
              <p class="product-origin"><i class="el-icon-location-outline"></i> {{ p.origin || '云南高山' }}</p>
              <div class="product-bottom">
                <span class="price">¥{{ p.price }}</span>
                <span class="stock-tag" v-if="p.stock > 0">库存{{ p.stock }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-pagination v-if="total > 0" class="pagination" background layout="total, prev, pager, next"
        :total="total" :page-size="query.pageSize" :current-page.sync="query.page" @current-change="loadProducts" />
    </div>

    <!-- 品牌故事 -->
    <div class="story-section">
      <el-row :gutter="30">
        <el-col :span="10">
          <div class="story-img" :style="{ background: 'linear-gradient(135deg, #0F6E56, #16A085)' }">
            <svg viewBox="0 0 300 200" xmlns="http://www.w3.org/2000/svg">
              <rect width="300" height="200" fill="none" />
              <text x="150" y="90" text-anchor="middle" fill="rgba(255,255,255,0.9)" font-size="28" font-weight="bold">云岭山珍</text>
              <text x="150" y="120" text-anchor="middle" fill="rgba(255,255,255,0.7)" font-size="14">高山野生菌 · 自然馈赠</text>
              <path d="M50 150 Q150 130 250 150" stroke="rgba(255,255,255,0.3)" stroke-width="2" fill="none" />
              <circle cx="80" cy="160" r="8" fill="rgba(255,255,255,0.2)" />
              <circle cx="150" cy="155" r="6" fill="rgba(255,255,255,0.15)" />
              <circle cx="220" cy="162" r="7" fill="rgba(255,255,255,0.2)" />
            </svg>
          </div>
        </el-col>
        <el-col :span="14">
          <h2 class="story-title">来自云岭高原的自然馈赠</h2>
          <p class="story-text">我们的野生菌均采自云南香格里拉、楚雄、玉溪等海拔3000米以上的高山原始森林。每年雨季，当地采菌人深入山林，遵循世代相传的经验，手工采摘最优质的野生菌。</p>
          <p class="story-text">从采摘到送达，全程冷链保鲜，确保每一朵野生菌都保持最鲜美的口感和最完整的营养。我们承诺：每一份产品都可溯源，让每一位食客吃得放心、品得安心。</p>
          <div class="story-tags">
            <el-tag size="small" effect="plain">海拔3500m+</el-tag>
            <el-tag size="small" effect="plain" type="success">手工采摘</el-tag>
            <el-tag size="small" effect="plain" type="warning">冷链直达</el-tag>
            <el-tag size="small" effect="plain" type="danger">品质溯源</el-tag>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import { getBannerList } from '@/api/banner'
import { getVideoList } from '@/api/video'

export default {
  name: 'UserShop',
  data() {
    return {
      query: { name: '', categoryId: '', page: 1, pageSize: 12 },
      categories: [],
      productList: [],
      total: 0,
      bannerList: [],
      videoList: [],
      currentVideoIndex: 0,
      videoLoaded: false,
      videoPoster: 'data:image/svg+xml,' + encodeURIComponent(
        '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="400">' +
        '<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">' +
        '<stop offset="0" stop-color="#0F6E56"/><stop offset="1" stop-color="#16A085"/></linearGradient></defs>' +
        '<rect width="800" height="400" fill="url(#g)"/>' +
        '<text x="400" y="180" text-anchor="middle" fill="rgba(255,255,255,0.9)" font-size="32" font-weight="bold">香格里拉原始森林</text>' +
        '<text x="400" y="220" text-anchor="middle" fill="rgba(255,255,255,0.6)" font-size="16">野生菌采摘纪实</text>' +
        '</svg>'),
      features: [
        { icon: 'el-icon-mountain', title: '高山采摘', desc: '海拔3500米原始森林', color: '#0F6E56', bg: 'linear-gradient(135deg, #E8F5E9, #C8E6C9)' },
        { icon: 'el-icon-truck', title: '冷链直达', desc: '采摘后24小时内发货', color: '#185FA5', bg: 'linear-gradient(135deg, #E3F2FD, #BBDEFB)' },
        { icon: 'el-icon-medal', title: '品质保证', desc: '每朵菌可溯源', color: '#F57C00', bg: 'linear-gradient(135deg, #FFF3E0, #FFE0B2)' },
        { icon: 'el-icon-magic-stick', title: 'AI识菌', desc: 'AI助手智能识别', color: '#7B1FA2', bg: 'linear-gradient(135deg, #F3E5F5, #E1BEE7)' }
      ],
      defaultImg: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="200" height="150"><rect fill="#E1F5EE" width="200" height="150"/><text x="100" y="80" text-anchor="middle" fill="#0F6E56" font-size="14">野生菌</text></svg>')
    }
  },
  created() {
    this.loadBanners()
    this.loadVideos()
    this.loadCategories()
    this.loadProducts()
  },
  computed: {
    currentVideo() { return this.videoList[this.currentVideoIndex] || {} }
  },
  methods: {
    async loadBanners() {
      try {
        const res = await getBannerList()
        this.bannerList = res.data || []
      } catch (e) { /* 轮播图加载失败不影响商城 */ }
    },
    async loadVideos() {
      try {
        const res = await getVideoList()
        this.videoList = res.data || []
      } catch (e) { /* 视频加载失败不影响商城 */ }
    },
    switchVideo(i) {
      if (i === this.currentVideoIndex) return
      this.currentVideoIndex = i
      this.videoLoaded = false
    },
    playVideo() {
      this.videoLoaded = true
      this.$nextTick(() => {
        if (this.$refs.mainVideo) this.$refs.mainVideo.play().catch(() => {})
      })
    },
    async loadCategories() {
      try {
        const res = await request.get('/category/list')
        this.categories = res.data || []
      } catch (e) { /* 接口待实现 */ }
    },
    async loadProducts() {
      try {
        const res = await request.get('/product/list', { params: this.query })
        this.productList = res.data && res.data.records || []
        this.total = res.data && res.data.total || 0
      } catch (e) {
        this.productList = []
        this.total = 0
      }
    },
    searchByCategory(catId) {
      this.query.categoryId = catId
      this.query.page = 1
      this.loadProducts()
    },
    getCatColor(id) {
      const colors = ['#0F6E56', '#185FA5', '#F57C00', '#7B1FA2', '#D4537E', '#16A085', '#E74C3C', '#3498DB']
      return colors[(id - 1) % colors.length]
    },
    goDetail(id) {
      this.$router.push('/user/product/' + id)
    }
  }
}
</script>

<style scoped>
.banner-carousel { margin-bottom: 20px; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.banner-item { height: 100%; display: flex; align-items: flex-end; padding: 0 50px 36px; position: relative; overflow: hidden; }
.banner-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; z-index: 1; }
.banner-item::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: linear-gradient(to top left, rgba(0,0,0,0.6) 0%, rgba(0,0,0,0.15) 45%, rgba(0,0,0,0) 80%); z-index: 1; pointer-events: none; }
.banner-content { z-index: 2; max-width: 55%; }
.banner-title { font-size: 32px; color: #fff; margin: 0 0 12px; text-shadow: 0 2px 10px rgba(0,0,0,0.6); }
.banner-subtitle { font-size: 16px; color: rgba(255,255,255,0.95); margin: 0; text-shadow: 0 1px 5px rgba(0,0,0,0.6); }

/* 品牌特色 */
.features-section { margin-bottom: 30px; }
.feature-card { padding: 24px 20px; border-radius: 12px; text-align: center; transition: transform 0.3s, box-shadow 0.3s; }
.feature-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }
.feature-card i { font-size: 36px; margin-bottom: 12px; }
.feature-card h3 { font-size: 16px; color: #333; margin: 0 0 6px; }
.feature-card p { font-size: 13px; color: #666; margin: 0; }

/* 视频介绍 */
.video-section { margin-bottom: 30px; }
.section-title { font-size: 20px; font-weight: 600; color: #333; margin: 0 0 16px; display: flex; align-items: center; gap: 8px; }
.video-layout { display: flex; gap: 16px; align-items: stretch; }
.video-main { position: relative; flex: 1; min-width: 0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.video-player { width: 100%; height: 400px; object-fit: cover; background: #000; display: block; }
.video-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; cursor: pointer; background: linear-gradient(135deg, #0F6E56, #16A085); }
.video-poster { width: 100%; height: 100%; object-fit: cover; }
.play-button { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 70px; height: 70px; background: rgba(255,255,255,0.9); border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 16px rgba(0,0,0,0.2); }
.play-button i { font-size: 40px; color: #0F6E56; }
.video-desc { position: absolute; bottom: 20px; left: 0; width: 100%; text-align: center; color: #fff; font-size: 14px; text-shadow: 0 1px 5px rgba(0,0,0,0.6); padding: 0 20px; box-sizing: border-box; }

/* 视频切换列表 */
.video-list { width: 300px; display: flex; flex-direction: column; gap: 12px; max-height: 400px; overflow-y: auto; }
.video-list-item { display: flex; gap: 10px; padding: 8px; border-radius: 10px; cursor: pointer; background: #fff; border: 2px solid transparent; transition: border-color 0.2s, box-shadow 0.2s; }
.video-list-item:hover { box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
.video-list-item.active { border-color: #0F6E56; background: #f0faf6; }
.video-list-thumb { width: 96px; height: 56px; border-radius: 6px; object-fit: cover; flex-shrink: 0; background: #eee; }
.video-list-info { display: flex; flex-direction: column; justify-content: center; min-width: 0; }
.video-list-title { font-size: 13px; color: #333; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.video-list-desc { font-size: 11px; color: #999; margin-top: 4px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* 分类导航 */
.category-section { margin-bottom: 30px; }
.category-grid { display: flex; flex-wrap: wrap; gap: 16px; }
.category-item { display: flex; flex-direction: column; align-items: center; cursor: pointer; padding: 16px 20px; border-radius: 10px; transition: background 0.2s; min-width: 90px; }
.category-item:hover { background: #f0f7ff; }
.cat-icon { width: 50px; height: 50px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; font-weight: 600; margin-bottom: 8px; }
.category-item span { font-size: 13px; color: #555; }

/* 筛选 */
.filter-card { margin-bottom: 20px; border-radius: 10px; }

/* 商品列表 */
.product-section { margin-bottom: 30px; }
.product-list { min-height: 400px; }
.product-card { cursor: pointer; margin-bottom: 16px; border-radius: 10px; transition: transform 0.2s; }
.product-card:hover { transform: translateY(-3px); }
.product-img { height: 160px; display: flex; align-items: center; justify-content: center; background: #f9f9f9; border-radius: 8px; overflow: hidden; position: relative; }
.product-img img { max-width: 100%; max-height: 100%; }
.sold-out-mask { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.sold-out-mask span { color: #fff; font-size: 18px; font-weight: bold; padding: 4px 18px; border: 2px solid #fff; border-radius: 4px; }
.product-name { font-size: 15px; color: #333; margin: 10px 0 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.product-origin { font-size: 12px; color: #888; }
.product-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.price { font-size: 18px; font-weight: 600; color: #D85A30; }
.stock-tag { font-size: 12px; color: #999; }
.pagination { margin-top: 20px; text-align: right; }

/* 品牌故事 */
.story-section { background: linear-gradient(135deg, #f5fffa, #f0f8ff); border-radius: 12px; padding: 30px; margin-bottom: 20px; }
.story-img { border-radius: 12px; height: 200px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.story-img svg { width: 100%; height: 100%; }
.story-title { font-size: 22px; color: #0F6E56; margin: 0 0 16px; }
.story-text { font-size: 14px; color: #555; line-height: 1.8; margin: 0 0 12px; }
.story-tags { display: flex; gap: 10px; margin-top: 16px; }
</style>
