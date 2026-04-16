<template>
  <div class="course-container">
    <div class="header-container">
      <h2 class="page-title">安全课程学习</h2>
      <el-input
        v-model="searchQuery"
        placeholder="搜索课程名称或分类..."
        prefix-icon="Search"
        clearable
        style="width: 300px;"
      />
    </div>

    <el-tabs v-model="activeTab" class="course-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="未学习" name="unlearned"></el-tab-pane>
      <el-tab-pane label="已学习" name="learned"></el-tab-pane>
    </el-tabs>
    
    <!-- 加载骨架屏 -->
    <el-skeleton style="width: 100%" :loading="loading" animated>
      <template #template>
        <el-row :gutter="20">
          <el-col :span="8" v-for="i in 3" :key="i" style="margin-bottom: 20px;">
            <el-card>
              <el-skeleton-item variant="image" style="width: 100%; height: 200px" />
              <div style="padding: 14px;">
                <el-skeleton-item variant="h3" style="width: 50%" />
                <div style="display: flex; align-items: center; justify-content: space-between; margin-top: 16px;">
                  <el-skeleton-item variant="text" style="width: 30%" />
                  <el-skeleton-item variant="button" style="width: 30%" />
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- 实际内容 -->
      <template #default>
        <el-empty v-if="filteredCourses.length === 0" description="暂无课程数据" />
        
        <el-row :gutter="20" v-else>
          <el-col :span="8" v-for="course in filteredCourses" :key="course.id" style="margin-bottom: 20px;">
            <el-card :body-style="{ padding: '0px' }" class="course-card">
              <img :src="course.coverUrl || 'https://via.placeholder.com/300x200?text=No+Cover'" class="image" />
              <div style="padding: 16px;">
                <div class="course-header">
                  <span class="title" :title="course.title">{{ course.title }}</span>
                  <el-tag size="small" type="success" effect="dark">{{ course.category }}</el-tag>
                </div>
                
                <div class="desc">{{ course.content }}</div>
                
                <div class="bottom">
                  <span class="points">
                    <el-icon style="vertical-align: middle; margin-right: 4px;"><Coin /></el-icon>
                    奖励积分: {{ course.rewardPoints }}
                  </span>
                  <el-button v-if="!course.isLearned" type="primary" plain size="small" @click="$router.push(`/course/${course.id}`)">开始学习</el-button>
                  <el-button v-else type="success" size="small" @click="$router.push(`/course/${course.id}`)">重新复习</el-button>
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
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { Coin, Search } from '@element-plus/icons-vue'

const courses = ref([])
const loading = ref(true)
const searchQuery = ref('')
const activeTab = ref('unlearned')

const handleTabChange = () => {
  searchQuery.value = ''
}

const filteredCourses = computed(() => {
  // 先按 tab 过滤
  let baseList = courses.value.filter(course => 
    activeTab.value === 'learned' ? course.isLearned : !course.isLearned
  )
  
  // 再按搜索词过滤
  if (!searchQuery.value) return baseList
  const lowerQuery = searchQuery.value.toLowerCase()
  return baseList.filter(course => 
    course.title.toLowerCase().includes(lowerQuery) || 
    (course.category && course.category.toLowerCase().includes(lowerQuery))
  )
})

const fetchCourses = async () => {
  try {
    const res = await request.get('/api/course/list')
    courses.value = res.data
  } catch (error) {
    console.error('获取课程列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 模拟一点延迟，为了展示骨架屏的效果
  setTimeout(() => {
    fetchCourses()
  }, 500)
})
</script>

<style scoped>
.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  border-left: 4px solid #409EFF;
  padding-left: 12px;
}

.course-card {
  transition: all 0.3s ease;
  border-radius: 8px;
  overflow: hidden;
}

.course-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.1);
}

.image {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 65%;
}

.desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  height: 40px;
  margin-bottom: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.points {
  font-size: 14px;
  color: #E6A23C;
  font-weight: bold;
  display: flex;
  align-items: center;
}
</style>