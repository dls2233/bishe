<template>
  <div class="news-detail-container" v-loading="loading">
    <el-button @click="router.back()" class="mb-20">返回上一页</el-button>
    <el-card class="news-card" shadow="hover" v-if="news">
      <h2 class="title">
        <el-tag size="small" type="danger" effect="dark" v-if="isNew(news.createTime)" style="margin-right: 10px; vertical-align: middle;">最新</el-tag>
        <span style="vertical-align: middle;">{{ news.title }}</span>
      </h2>
      <div class="meta">
        <el-tag size="small" type="info" class="mr-10">{{ news.category || '综合' }}</el-tag>
        <span class="meta-item"><el-icon><Calendar /></el-icon> {{ formatDate(news.createTime) }}</span>
        <span class="meta-item"><el-icon><View /></el-icon> {{ news.views }} 阅读</span>
      </div>
      <el-divider />
      <div class="cover" v-if="news.coverUrl">
        <el-image :src="news.coverUrl" fit="contain" style="max-height: 400px; width: 100%; border-radius: 8px;"></el-image>
      </div>
      <div class="content" v-html="formatContent(news.content)"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Calendar, View } from '@element-plus/icons-vue'
import request from '../utils/request'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const news = ref(null)
const loading = ref(true)

onMounted(async () => {
  const id = route.params.id
  try {
    const res = await request.get(`/api/news/${id}`)
    if (res.code === 200) {
      news.value = res.data
    }
  } catch (error) {
    console.error('获取资讯详情失败', error)
  } finally {
    loading.value = false
  }
})

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
}

const isNew = (date) => {
  if (!date) return false
  return dayjs().diff(dayjs(date), 'day') <= 3
}
</script>

<style scoped>
.news-detail-container {
  max-width: 900px;
  margin: 0 auto;
}

.news-card {
  border-radius: 12px;
  padding: 20px;
}

.title {
  font-size: 26px;
  color: #303133;
  margin-top: 0;
  margin-bottom: 15px;
  text-align: center;
}

.meta {
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  margin-right: 20px;
}

.meta-item .el-icon {
  margin-right: 5px;
}

.mr-10 {
  margin-right: 10px;
}

.cover {
  margin-bottom: 30px;
  text-align: center;
}

.content {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  text-align: justify;
}
</style>
