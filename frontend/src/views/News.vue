<template>
  <div class="news-container">
    <h2 class="page-title mb-20">新闻资讯</h2>
    <el-row :gutter="20">
      <el-col :span="24" v-for="news in pagedNewsList" :key="news.id">
        <el-card class="news-card mb-20" shadow="hover" @click="goToDetail(news.id)">
          <div class="news-content">
            <div class="news-image" v-if="news.coverUrl">
              <el-image :src="news.coverUrl" fit="cover" style="width: 200px; height: 120px; border-radius: 8px;"></el-image>
            </div>
            <div class="news-info">
              <h3 class="news-title">
                <el-tag size="small" type="danger" effect="dark" v-if="isNew(news.createTime)" style="margin-right: 8px; vertical-align: middle;">最新</el-tag>
                <span style="vertical-align: middle;">{{ news.title }}</span>
              </h3>
              <div class="news-meta">
                <el-tag size="small" type="info" class="mr-10">{{ news.category || '综合' }}</el-tag>
                <span class="meta-item"><el-icon><View /></el-icon> {{ news.views }} 阅读</span>
                <span class="meta-item"><el-icon><Calendar /></el-icon> {{ formatDate(news.createTime) }}</span>
              </div>
              <p class="news-desc">{{ truncateContent(news.content) }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分页组件 -->
    <div class="pagination-container" v-if="newsList.length > 0" style="display: flex; justify-content: center; margin-top: 20px;">
      <el-pagination
        background
        layout="total, prev, pager, next, jumper"
        :total="newsList.length"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { View, Calendar } from '@element-plus/icons-vue'
import request from '../utils/request'
import dayjs from 'dayjs'

const router = useRouter()
const newsList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

const pagedNewsList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return newsList.value.slice(start, end)
})

const handleCurrentChange = (val) => {
  currentPage.value = val
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const isNew = (date) => {
  if (!date) return false
  return dayjs().diff(dayjs(date), 'day') <= 3
}

onMounted(async () => {
  try {
    const res = await request.get('/api/news/list')
    if (res.code === 200) {
      newsList.value = res.data
    }
  } catch (error) {
    console.error('获取资讯列表失败', error)
  }
})

const goToDetail = (id) => {
  router.push(`/news/${id}`)
}

const truncateContent = (content) => {
  if (!content) return ''
  return content.length > 100 ? content.substring(0, 100) + '...' : content
}

const formatDate = (date) => {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.page-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  border-left: 4px solid #409eff;
  padding-left: 12px;
}

.news-card {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.news-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.08) !important;
}

.news-content {
  display: flex;
  gap: 20px;
}

.news-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.news-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #303133;
}

.news-meta {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  margin-right: 15px;
}

.meta-item .el-icon {
  margin-right: 4px;
}

.mr-10 {
  margin-right: 10px;
}

.news-desc {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}
</style>
