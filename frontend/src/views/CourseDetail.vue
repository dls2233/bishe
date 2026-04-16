<template>
  <div class="course-detail-container" v-loading="loading">
    <el-page-header @back="$router.back()" :content="course?.title || '课程详情'" class="header" />

    <div v-if="course" class="content-wrapper">
      <el-steps :active="activeStep" finish-status="success" align-center style="margin-bottom: 30px;">
        <el-step title="课程学习" description="阅读文章或观看视频" />
        <el-step title="课后测验" description="完成题目获取积分" />
      </el-steps>

      <!-- 第一阶段：学习区 -->
      <div v-show="activeStep === 0">
        <!-- 标题和元信息区 (移到最上方) -->
        <el-card shadow="never" class="article-section">
          <div class="article-header">
            <h1 class="title">{{ course.title }}</h1>
            <div class="meta">
              <el-tag size="small" type="success">{{ course.category }}</el-tag>
              <span class="time"><el-icon><Clock /></el-icon> {{ new Date(course.createTime).toLocaleDateString() }}</span>
              <span class="points"><el-icon><Coin /></el-icon> 奖励: {{ course.rewardPoints }} 积分</span>
            </div>
          </div>
          <el-divider />
          
          <!-- 视频学习区 (移到标题下方) -->
          <div v-if="course.videoUrl" class="video-section">
            <video 
              :src="course.videoUrl" 
              controls 
              class="course-video"
              @ended="handleVideoEnded"
            ></video>
          </div>
          <el-alert
            v-if="course.videoUrl"
            title="观看完视频后，点击下方按钮进入课后测验"
            type="info"
            show-icon
            :closable="false"
            style="margin-bottom: 20px;"
          />

          <!-- 图文学习区 (紧跟在视频下方) -->
          <div class="article-content" v-html="formatContent(course.content)"></div>
        </el-card>

        <!-- 进入测验按钮 -->
        <div class="action-section">
          <el-alert
            v-if="isFinished"
            :title="course.progressStatus === 'COMPLETED' ? '您已通过测验并获得积分，无需重新答题' : '该课程已标记为学习结束，但您未通过测验（无积分）。您可以继续复习。'"
            :type="course.progressStatus === 'COMPLETED' ? 'success' : 'warning'"
            show-icon
            :closable="false"
            style="margin-bottom: 20px; display: inline-flex;"
          />
          <br v-if="isFinished" />
          <el-button 
            type="primary" 
            size="large" 
            @click="goToQuiz" 
            v-if="!isFinished">
            <el-icon style="margin-right: 6px;"><CircleCheckFilled /></el-icon>
            我已学完本课程，开始测验
          </el-button>
          <el-button 
            v-else
            type="info" 
            size="large" 
            @click="activeStep = 2">
            <el-icon style="margin-right: 6px;"><Select /></el-icon>
            查看历史测验记录
          </el-button>
        </div>
      </div>

        <!-- 第二阶段：测验区 -->
      <div v-show="activeStep === 1 || activeStep === 2">
        <el-card shadow="never" class="quiz-section">
          <template #header>
            <div class="quiz-header">
              <h2>{{ isFinished ? '课后测验复习' : '课后知识测验' }}</h2>
              <el-tag v-if="!isFinished" type="danger" effect="plain">剩余答题机会：{{ attemptsLeft }} 次</el-tag>
              <el-tag v-else :type="course.progressStatus === 'COMPLETED' ? 'success' : 'danger'" effect="dark">
                {{ course.progressStatus === 'COMPLETED' ? '测验通过' : '测验未通过' }}
              </el-tag>
            </div>
          </template>

          <div v-if="quizList.length > 0">
            <div v-for="(q, index) in quizList" :key="index" class="question-item">
              <div class="question-title">
                <span class="q-num">{{ index + 1 }}.</span> {{ q.question }}
                <el-tag v-if="isFinished" size="small" :type="userAnswers[index] === q.answer ? 'success' : 'danger'" style="margin-left: 10px;">
                  {{ userAnswers[index] === q.answer ? '回答正确' : '回答错误' }}
                </el-tag>
              </div>
              <el-radio-group v-model="userAnswers[index]" class="options-group" :disabled="isFinished">
                <el-radio 
                  v-for="(opt, optIndex) in q.options" 
                  :key="optIndex" 
                  :label="optIndex"
                  class="option-item"
                  :class="{ 
                    'is-correct-answer': isFinished && optIndex === q.answer,
                    'is-wrong-answer': isFinished && userAnswers[index] === optIndex && optIndex !== q.answer 
                  }"
                >
                  {{ String.fromCharCode(65 + optIndex) }}. {{ opt }}
                  <el-icon v-if="isFinished && optIndex === q.answer" color="#67C23A" style="margin-left: 8px;"><Select /></el-icon>
                  <el-icon v-if="isFinished && userAnswers[index] === optIndex && optIndex !== q.answer" color="#F56C6C" style="margin-left: 8px;"><CloseBold /></el-icon>
                </el-radio>
              </el-radio-group>
            </div>
          </div>
          <el-empty v-else description="本课程暂无测验题目" />

          <div class="action-section">
            <el-button @click="activeStep = 0">返回阅读内容</el-button>
            <el-button 
              v-if="!isFinished"
              type="success" 
              size="large" 
              @click="submitQuiz" 
              :loading="submitting">
              <el-icon style="margin-right: 6px;"><CircleCheckFilled /></el-icon>
              提交测验
            </el-button>
          </div>
        </el-card>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { Clock, Coin, CircleCheckFilled, Select, CloseBold } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const store = useStore()

const loading = ref(true)
const submitting = ref(false)
const course = ref(null)
const isFinished = ref(false)

const activeStep = ref(0)
const quizList = ref([])
const userAnswers = ref({})
const attemptsLeft = ref(3)

const fetchCourseDetail = async () => {
  try {
    const res = await request.get(`/api/course/${route.params.id}`)
    course.value = res.data
    
    if (course.value.isLearned) {
      isFinished.value = true
      activeStep.value = 0 // 默认展示文章，用户点击按钮再看记录
    }
    
    // 解析历史答案
    if (course.value.lastAnswers) {
      try {
        userAnswers.value = typeof course.value.lastAnswers === 'string' ? JSON.parse(course.value.lastAnswers) : course.value.lastAnswers
      } catch (e) {
        console.error('解析历史答案失败', e)
      }
    }
    if (course.value.quiz) {
      try {
        quizList.value = typeof course.value.quiz === 'string' ? JSON.parse(course.value.quiz) : course.value.quiz
      } catch (e) {
        console.error('解析测验题目失败', e)
        quizList.value = []
      }
    }
  } catch (error) {
    console.error('获取课程详情失败:', error)
  } finally {
    loading.value = false
  }
}

const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br/>')
}

const handleVideoEnded = () => {
  ElMessage.success('视频播放完毕！您可以进入课后测验了。')
}

const goToQuiz = () => {
  activeStep.value = 1
  userAnswers.value = {} // 清空之前的答案
}

const submitQuiz = async () => {
  // 检查是否全部作答
  if (quizList.value.length > 0) {
    const answeredCount = Object.keys(userAnswers.value).length
    if (answeredCount < quizList.value.length) {
      ElMessage.warning('请先回答完所有题目再提交！')
      return
    }

    // 校验答案
    let wrongQuestions = []
    quizList.value.forEach((q, index) => {
      if (userAnswers.value[index] !== q.answer) {
        wrongQuestions.push(index + 1)
      }
    })

    if (wrongQuestions.length > 0) {
      attemptsLeft.value--
      if (attemptsLeft.value > 0) {
        ElMessageBox.alert(
          `第 ${wrongQuestions.join('、')} 题回答错误。<br/>您还有 <strong>${attemptsLeft.value}</strong> 次重新答题的机会。`,
          '测验未通过',
          {
            dangerouslyUseHTMLString: true,
            type: 'error',
            confirmButtonText: '重新答题'
          }
        )
      } else {
        ElMessageBox.alert(
          '很遗憾，3次答题机会已用完。该课程将被标记为已学习，但无法获取本次学习积分。',
          '测验失败',
          {
            type: 'warning',
            confirmButtonText: '确认',
            callback: () => {
              finishCourse(false)
            }
          }
        )
      }
      return
    }
  }

  // 答案全对或没有题目，调用完成接口
  finishCourse(true)
}

const finishCourse = async (passed) => {
  submitting.value = true
  try {
    const payload = {
      passed: passed,
      lastAnswers: JSON.stringify(userAnswers.value)
    }
    const res = await request.post(`/api/course/${course.value.id}/finish`, payload)
    
    // 提交后更新本地状态，以便横幅显示正确
    if (!course.value.progressStatus || course.value.progressStatus === 'LEARNING') {
      course.value.progressStatus = passed ? 'COMPLETED' : 'FAILED'
    }

    ElMessage({
      message: res.data || (passed ? '恭喜！测验全对，学习完成，已获得积分！' : '该课程已标记为已学习。'),
      type: passed ? 'success' : 'warning',
      duration: 3000
    })
    isFinished.value = true
    activeStep.value = 0 // 提交后自动回到文章页
    
    // 更新全局积分状态
    await store.dispatch('fetchUserInfo')
    
    setTimeout(() => {
      router.push('/course')
    }, 2000)
  } catch (error) {
    console.error('提交学习记录失败:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchCourseDetail()
})
</script>

<style scoped>
.header {
  margin-bottom: 24px;
}

.content-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.video-section {
  margin-bottom: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  display: flex;
  justify-content: center;
  align-items: center;
}

.course-video {
  width: 100%;
  max-height: 500px;
  outline: none;
  background: transparent;
  display: block;
}

.article-section, .quiz-section {
  margin-bottom: 24px;
}

.article-header {
  text-align: center;
  margin-bottom: 20px;
}

.article-header .title {
  font-size: 24px;
  color: #303133;
  margin-bottom: 16px;
}

.meta {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 14px;
}

.meta .points {
  color: #E6A23C;
  font-weight: bold;
}

.article-content {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  text-align: justify;
  padding: 0 20px;
  margin-top: 20px;
}

.action-section {
  text-align: center;
  margin: 40px 0;
}

.quiz-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.quiz-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.question-item {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.question-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}
.q-num {
  color: #409EFF;
  margin-right: 8px;
}

.options-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.option-item {
  margin-right: 0;
  padding: 10px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.3s;
  height: auto;
  white-space: normal;
  display: flex;
  align-items: center;
}
.option-item.is-checked {
  border-color: #409EFF;
  background: #ecf5ff;
}
.option-item.is-correct-answer {
  border-color: #67C23A;
  background: #f0f9eb;
}
.option-item.is-wrong-answer {
  border-color: #F56C6C;
  background: #fef0f0;
}
</style>