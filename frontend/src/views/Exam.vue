<template>
  <div class="exam-container">
    <div class="page-header">
      <h2 class="page-title">在线测评</h2>
    </div>

    <el-tabs v-model="activeTab" class="exam-tabs">
      <el-tab-pane label="未完成" name="uncompleted"></el-tab-pane>
      <el-tab-pane label="已完成" name="completed"></el-tab-pane>
    </el-tabs>

    <!-- 加载骨架屏 -->
    <el-skeleton style="width: 100%" :loading="loading" animated>
      <template #template>
        <el-row :gutter="20">
          <el-col :span="24" v-for="i in 3" :key="i" style="margin-bottom: 20px;">
            <el-card>
              <div style="padding: 14px;">
                <el-skeleton-item variant="h3" style="width: 40%" />
                <el-skeleton-item variant="text" style="width: 80%; margin-top: 10px;" />
                <div style="display: flex; justify-content: space-between; margin-top: 16px;">
                  <el-skeleton-item variant="text" style="width: 20%" />
                  <el-skeleton-item variant="button" style="width: 15%" />
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- 实际内容 -->
      <template #default>
        <el-empty v-if="filteredExams.length === 0" description="暂无试卷" />

        <el-row :gutter="20" v-else>
          <el-col :span="24" v-for="exam in filteredExams" :key="exam.id" style="margin-bottom: 20px;">
            <el-card class="exam-card" shadow="hover">
              <div class="exam-header">
                <div class="title-area">
                  <el-tag v-if="exam.isMandatory" type="danger" effect="dark" size="small" style="margin-right: 8px;">必修</el-tag>
                  <h3 class="exam-title" style="display: inline-block;">{{ exam.title }}</h3>
                </div>
                <div>
                  <el-tag v-if="exam.isCompleted" type="success" effect="dark" style="margin-right: 10px;">
                    最高分: {{ exam.highestScore || 0 }}
                  </el-tag>
                  <el-tag type="warning" effect="light">通过：{{ exam.passScore }} / 满分：{{ exam.totalScore }}</el-tag>
                </div>
              </div>
              <p class="exam-desc">{{ exam.description }}</p>
              <div class="exam-bottom">
                <span class="exam-info">
                  <el-icon><Clock /></el-icon> 限时: {{ exam.timeLimit || 30 }} 分钟
                  <el-divider direction="vertical" />
                  <span :style="{ color: exam.attempts >= 3 ? '#F56C6C' : '#E6A23C' }">
                    剩余次数: {{ Math.max(0, 3 - (exam.attempts || 0)) }}
                  </span>
                  <span v-if="exam.deadline" class="deadline-text">
                    <el-divider direction="vertical" />
                    <el-icon><Warning /></el-icon> DDL: {{ exam.deadline }}
                  </span>
                </span>
                <el-button 
                  :type="exam.isCompleted ? 'success' : 'primary'" 
                  @click="startExam(exam.id)" 
                  round 
                  plain
                  :disabled="!exam.isCompleted && exam.attempts >= 3"
                >
                  <template v-if="exam.isCompleted">
                    查看成绩
                  </template>
                  <template v-else-if="exam.attempts >= 3">
                    次数用尽
                  </template>
                  <template v-else>
                    进入答题
                  </template>
                </el-button>
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
import { useRouter } from 'vue-router'
import { Clock, Warning } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const examList = ref([])
const loading = ref(true)
const activeTab = ref('uncompleted')

const filteredExams = computed(() => {
  return examList.value.filter(exam => 
    activeTab.value === 'completed' ? exam.isCompleted : !exam.isCompleted
  )
})

const fetchExams = async () => {
  try {
    const res = await request.get('/api/exam/list')
    examList.value = res.data
  } catch (error) {
    console.error('获取试卷列表失败:', error)
  } finally {
    loading.value = false
  }
}

const startExam = (examId) => {
  router.push(`/exam/${examId}`)
}

onMounted(() => {
  setTimeout(() => {
    fetchExams()
  }, 300)
})
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  border-left: 4px solid #67c23a;
  padding-left: 12px;
}

.exam-card {
  border-radius: 12px;
  transition: all 0.3s ease;
}

.exam-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.08) !important;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.exam-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.exam-desc {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
  line-height: 1.5;
}

.exam-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.exam-info {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 5px;
}

.deadline-text {
  color: #F56C6C;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>