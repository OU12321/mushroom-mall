<template>
  <div class="cart-page">
    <el-card>
      <div slot="header">
        我的购物车
        <span style="float:right;font-size:13px;color:#888">共 {{ cartList.length }} 件商品</span>
      </div>

      <div v-if="cartList.length === 0" class="empty-cart">
        <i class="el-icon-shopping-cart-2" style="font-size:48px;color:#ddd"></i>
        <p>购物车空空如也，快去选购吧</p>
        <el-button type="primary" @click="$router.push('/user/shop')">去商城</el-button>
      </div>

      <el-table v-else ref="cartTable" :data="cartList" border v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="商品图片" width="80">
          <template slot-scope="{row}">
            <el-image v-if="row.productImage" :src="row.productImage" style="width:50px;height:50px" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" width="200" />
        <el-table-column label="单价" width="100">
          <template slot-scope="{row}">¥{{ (row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template slot-scope="{row}">
            <el-input-number v-model="row.number" :min="1" :max="99" size="mini" @change="updateCartItem(row)" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100">
          <template slot-scope="{row}">¥{{ (row.price * row.number || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template slot-scope="{row}">
            <el-button size="mini" type="danger" @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="cartList.length > 0" class="cart-footer">
        <el-checkbox v-model="selectAll" @change="toggleSelectAll">全选</el-checkbox>
        <span class="total-info">已选 {{ selectedItems.length }} 件，合计：<span class="total-price">¥{{ totalAmount.toFixed(2) }}</span></span>
        <el-button type="danger" size="medium" @click="checkout" :disabled="selectedItems.length === 0">结算</el-button>
      </div>
    </el-card>

    <!-- 结算弹窗 -->
    <el-dialog title="确认订单" :visible.sync="orderDialogVisible" width="500px">
      <el-form :model="orderForm" :rules="orderRules" ref="orderFormRef" label-width="80px">
        <el-form-item label="选择地址">
          <el-select v-model="selectedAddressId" placeholder="选择已保存的收货地址" clearable filterable
                     @change="handleAddressChange" style="width:100%">
            <el-option v-for="a in addressList" :key="a.id"
                       :label="a.consignee + ' ' + a.phone + ' ' + a.detailAddress" :value="a.id" />
          </el-select>
          <el-button type="text" @click="$router.push('/user/address')">管理地址</el-button>
        </el-form-item>
        <el-form-item label="收货人" prop="consignee">
          <el-input v-model="orderForm.consignee" placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="orderForm.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="收货地址" prop="address">
          <el-input v-model="orderForm.address" placeholder="收货地址" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="orderForm.payMethod">
            <el-option label="微信支付" :value="0" />
            <el-option label="支付宝" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="2" placeholder="订单备注（选填）" />
        </el-form-item>
      </el-form>
      <div class="order-summary">
        <p>共 {{ selectedItems.length }} 件商品，合计：<span class="total-price">¥{{ totalAmount.toFixed(2) }}</span></p>
      </div>
      <div slot="footer">
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitOrder">提交订单</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getCartList, updateCart, deleteCart } from '@/api/cart'
import { createOrder } from '@/api/order'
import { getAddressList } from '@/api/address'

export default {
  name: 'UserCart',
  data() {
    return {
      loading: false,
      cartList: [],
      selectedItems: [],
      selectAll: false,
      orderDialogVisible: false,
      orderForm: { consignee: '', phone: '', address: '', payMethod: 0, remark: '' },
      addressList: [],
      selectedAddressId: null,
      orderRules: {
        consignee: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
        address: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
      }
    }
  },
  computed: {
    totalAmount() {
      return this.selectedItems.reduce((sum, item) => sum + item.price * item.number, 0)
    }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getCartList()
        this.cartList = res.data || []
        // 默认全选购物车商品
        this.$nextTick(() => {
          this.cartList.forEach(row => {
            this.$refs.cartTable.toggleRowSelection(row, true)
          })
        })
      } finally { this.loading = false }
    },
    handleSelectionChange(val) {
      this.selectedItems = val
      this.selectAll = val.length === this.cartList.length && this.cartList.length > 0
    },
    toggleSelectAll(val) {
      if (val) {
        this.cartList.forEach(row => {
          this.$refs.cartTable.toggleRowSelection(row, true)
        })
      } else {
        this.$refs.cartTable.clearSelection()
      }
    },
    async updateCartItem(row) {
      try {
        await updateCart({ id: row.id, number: row.number })
      } catch (e) { /* ignore */ }
    },
    removeItem(row) {
      this.$confirm('确认删除该商品吗？', '提示', { type: 'warning' })
        .then(async () => {
          await deleteCart(row.id)
          this.$message.success('已删除')
          this.loadData()
        }).catch(() => {})
    },
    async checkout() {
      if (this.selectedItems.length === 0) {
        this.$message.warning('请选择要结算的商品')
        return
      }
      const userInfo = this.$store.state.userInfo || {}
      this.orderForm.consignee = userInfo.name || ''
      this.orderForm.phone = userInfo.phone || ''
      this.orderForm.address = ''
      this.selectedAddressId = null
      // 加载已保存地址，若有默认地址则自动填充
      try {
        const res = await getAddressList()
        this.addressList = res.data || []
        const def = this.addressList.find(a => a.isDefault === 1)
        if (def) {
          this.selectedAddressId = def.id
          this.fillAddress(def)
        }
      } catch (e) { /* ignore */ }
      this.orderDialogVisible = true
    },
    handleAddressChange(val) {
      if (!val) return
      const addr = this.addressList.find(a => a.id === val)
      this.fillAddress(addr)
    },
    fillAddress(addr) {
      if (!addr) return
      this.orderForm.consignee = addr.consignee
      this.orderForm.phone = addr.phone
      this.orderForm.address = addr.detailAddress
    },
    submitOrder() {
      this.$refs.orderFormRef.validate(async valid => {
        if (!valid) return
        try {
          const items = this.selectedItems.map(item => ({
            productId: item.productId,
            number: item.number
          }))
          const res = await createOrder({
            ...this.orderForm,
            items
          })
          const orderId = res.data
          this.$message.success('下单成功')
          this.orderDialogVisible = false
          // 清空已选商品的购物车
          for (const item of this.selectedItems) {
            await deleteCart(item.id)
          }
          // 直接跳转到付款页
          this.$router.push({ path: '/user/pay', query: { orderId: orderId } })
        } catch (e) {
          // request.js 已弹出后端错误提示，这里不再继续抛出，避免全屏红屏
          console.error('下单失败', e)
        }
      })
    }
  }
}
</script>

<style scoped>
.empty-cart { text-align: center; padding: 60px 0; }
.empty-cart p { color: #aaa; margin: 15px 0; }
.cart-footer { display: flex; align-items: center; gap: 20px; margin-top: 15px; padding: 10px 0; }
.total-info { font-size: 14px; color: #333; }
.total-price { font-size: 18px; font-weight: 600; color: #D85A30; }
.order-summary { margin-top: 10px; padding: 10px; background: #f5f5f5; border-radius: 4px; }
</style>
