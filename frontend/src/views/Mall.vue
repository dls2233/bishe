<template>
  <div class="mall-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">积分商城</h2>
        <el-input
          v-model="searchQuery"
          placeholder="搜索商品名称..."
          prefix-icon="Search"
          clearable
          style="width: 250px; margin-left: 20px;"
        />
      </div>
      <div class="my-points">
        <el-icon class="point-icon"><Trophy /></el-icon>
        <span class="label">我的可用积分：</span>
        <span class="value">{{ myPoints }}</span>
      </div>
    </div>

    <!-- 加载骨架屏 -->
    <el-skeleton style="width: 100%" :loading="loading" animated>
      <template #template>
        <el-row :gutter="20">
          <el-col :span="6" v-for="i in 4" :key="i" style="margin-bottom: 20px;">
            <el-card>
              <el-skeleton-item variant="image" style="width: 100%; height: 180px" />
              <div style="padding: 14px;">
                <el-skeleton-item variant="h3" style="width: 70%" />
                <el-skeleton-item variant="text" style="width: 40%; margin-top: 10px;" />
                <div style="display: flex; justify-content: space-between; margin-top: 16px;">
                  <el-skeleton-item variant="text" style="width: 30%" />
                  <el-skeleton-item variant="button" style="width: 40%" />
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- 实际内容 -->
      <template #default>
        <el-empty v-if="filteredGoods.length === 0" description="商城暂无相关商品" />

        <el-row :gutter="20" v-else>
          <el-col :span="6" v-for="goods in filteredGoods" :key="goods.id" style="margin-bottom: 20px;">
            <el-card :body-style="{ padding: '0px' }" class="goods-card" shadow="hover">
              <div class="image-wrapper">
                <img :src="goods.imageUrl || 'https://via.placeholder.com/300x300?text=Gift'" class="image" />
                <div class="stock-badge" :class="{ 'low-stock': goods.stock < 10 }">
                  剩余 {{ goods.stock }} 件
                </div>
              </div>
              
              <div style="padding: 16px;">
                <h3 class="goods-name" :title="goods.name">{{ goods.name }}</h3>
                <p class="goods-desc">{{ goods.description }}</p>
                
                <div class="bottom-action">
                  <div class="price">
                    <span class="num">{{ goods.pointsRequired }}</span>
                    <span class="unit">积分</span>
                  </div>
                  <el-button 
                    type="primary" 
                    :disabled="myPoints < goods.pointsRequired"
                    @click="handleExchange(goods)"
                    :loading="exchangingId === goods.id"
                    round>
                    {{ myPoints >= goods.pointsRequired ? '立即兑换' : '积分不足' }}
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Trophy, Search } from '@element-plus/icons-vue'
import request from '../utils/request'

const store = useStore()
const goodsList = ref([])
const loading = ref(true)
const exchangingId = ref(null)
const searchQuery = ref('')

const myPoints = computed(() => store.state.user.points || 0)

const filteredGoods = computed(() => {
  if (!searchQuery.value) return goodsList.value
  const lowerQuery = searchQuery.value.toLowerCase()
  return goodsList.value.filter(goods => 
    goods.name.toLowerCase().includes(lowerQuery) || 
    (goods.description && goods.description.toLowerCase().includes(lowerQuery))
  )
})

const fetchGoods = async () => {
  try {
    const res = await request.get('/api/mall/goods')
    goodsList.value = res.data
  } catch (error) {
    console.error('获取商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleExchange = (goods) => {
  ElMessageBox.confirm(
    `确定要消耗 ${goods.pointsRequired} 积分兑换【${goods.name}】吗？`,
    '兑换确认',
    {
      confirmButtonText: '确定兑换',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    exchangingId.value = goods.id
    try {
      await request.post('/api/mall/exchange', { goodsId: goods.id })
      ElMessage.success('兑换成功！')
      await store.dispatch('fetchUserInfo') // 兑换成功后更新用户信息（积分）
      fetchGoods() // 刷新库存
    } catch (error) {
      console.error('兑换失败:', error)
    } finally {
      exchangingId.value = null
    }
  }).catch(() => {})
}

onMounted(() => {
  setTimeout(() => {
    fetchGoods()
  }, 400)
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  border-left: 4px solid #f56c6c;
  padding-left: 12px;
}

.my-points {
  display: flex;
  align-items: center;
  background-color: #fdf6ec;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #faecd8;
}

.point-icon {
  color: #e6a23c;
  font-size: 18px;
  margin-right: 6px;
}

.my-points .label {
  color: #606266;
  font-size: 14px;
}

.my-points .value {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
  margin-left: 4px;
}

.goods-card {
  border-radius: 12px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.goods-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 32px rgba(0,0,0,0.1) !important;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.stock-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.stock-badge.low-stock {
  background-color: #f56c6c;
}

.goods-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods-desc {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #909399;
  height: 38px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bottom-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #f56c6c;
}

.price .num {
  font-size: 22px;
  font-weight: bold;
}

.price .unit {
  font-size: 12px;
  margin-left: 2px;
}
</style>