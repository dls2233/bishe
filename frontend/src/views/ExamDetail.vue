<template>
  <div class="exam-detail-container" v-loading="loading" element-loading-text="拼命加载中...">
    <el-page-header @back="router.push('/exam')" :content="exam ? exam.title : '试卷详情'" class="header" />
    
    <div v-if="exam" class="exam-content">
      <el-card shadow="never" class="exam-info" style="position: relative;">
        <div style="padding-right: 120px;">
          <p>
            <el-tag v-if="exam.isMandatory" type="danger" effect="dark" size="small" style="margin-right: 8px;">必修</el-tag>
            <strong>说明：</strong>{{ exam.description }}
          </p>
          <p>
            <strong>满分：</strong>{{ exam.totalScore }} 分 &nbsp;&nbsp;&nbsp;&nbsp; 
            <strong>及格线：</strong>{{ exam.passScore }} 分 &nbsp;&nbsp;&nbsp;&nbsp;
            <strong>限时：</strong>{{ exam.timeLimit || 30 }} 分钟
            <span v-if="exam.deadline" style="color: #F56C6C; font-weight: bold; margin-left: 20px;">
              <el-icon><Warning /></el-icon> 截止时间：{{ exam.deadline }}
            </span>
          </p>
        </div>
        <div class="countdown-box" v-if="timeLeft > 0 && !exam.isReadOnly">
          <div class="time-label">剩余时间</div>
          <div class="time-value" :class="{ 'time-warning': timeLeft < 300 }">{{ formattedTime }}</div>
        </div>
      </el-card>

      <el-form :model="form" class="question-list">
        <el-card v-for="(question, index) in questions" :key="question.id" class="question-card">
          <div class="question-title">
            <span class="q-num">{{ index + 1 }}.</span>
            <el-tag size="small" class="q-type" :type="getTypeColor(question.type)">
              {{ getTypeName(question.type) }}
            </el-tag>
            <span class="q-content">{{ question.content }} ({{ question.score }}分)</span>
          </div>

          <!-- 单选题 & 判断题 -->
          <div v-if="question.type === 'SINGLE_CHOICE' || question.type === 'JUDGE'" class="options">
            <el-radio-group v-model="form.answers[question.id]" :disabled="exam.isReadOnly">
              <el-radio 
                v-for="(opt, oIndex) in parseOptions(question.options)" 
                :key="oIndex" 
                :value="getOptionLetter(opt)">
                {{ opt }}
              </el-radio>
            </el-radio-group>
          </div>

          <!-- 多选题 -->
          <div v-if="question.type === 'MULTIPLE_CHOICE'" class="options">
            <el-checkbox-group v-model="form.answers[question.id]" :disabled="exam.isReadOnly">
              <el-checkbox 
                v-for="(opt, oIndex) in parseOptions(question.options)" 
                :key="oIndex" 
                :value="getOptionLetter(opt)">
                {{ opt }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
          
          <!-- 显示正确答案与解析 (仅在只读模式下展示) -->
          <div v-if="exam.isReadOnly" class="answer-analysis">
            <div class="correct-answer">
              <el-icon style="margin-right: 4px; vertical-align: middle;"><CircleCheckFilled /></el-icon>
              <strong>正确答案：</strong> 
              <span class="answer-text">{{ getCorrectAnswerDisplay(question) }}</span>
            </div>
          </div>
        </el-card>
      </el-form>

      <div class="submit-action" v-if="!exam.isReadOnly">
        <el-button type="primary" size="large" @click="submitExam(false)" :loading="submitting">
          <el-icon style="margin-right: 6px;"><Upload /></el-icon> 交 卷
        </el-button>
      </div>
    </div>

    <!-- 结果弹窗 -->
    <el-dialog v-model="resultDialogVisible" title="测评结果" width="400px" center :close-on-click-modal="false" :show-close="false">
      <div class="result-content">
        <el-icon :size="60" :color="result.isPass ? '#67C23A' : '#F56C6C'">
          <SuccessFilled v-if="result.isPass" />
          <WarningFilled v-else />
        </el-icon>
        <h2 :class="result.isPass ? 'text-success' : 'text-danger'">
          {{ result.isPass ? '恭喜，及格了！' : '很遗憾，未及格' }}
        </h2>
        <p class="score">您的得分：<strong>{{ result.score }}</strong> 分</p>
        <p v-if="result.rewardPoints > 0" class="reward">
          🎁 获得奖励积分：+{{ result.rewardPoints }}
        </p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="$router.push('/exam')">返回试卷列表</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, SuccessFilled, WarningFilled, Warning, CircleCheckFilled } from '@element-plus/icons-vue'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const exam = ref(null)
const questions = ref([])
const resultDialogVisible = ref(false)
const result = ref({})

const form = reactive({
  answers: {}
})

// 倒计时相关
const timeLeft = ref(0)
let timer = null

const formattedTime = computed(() => {
  const m = Math.floor(timeLeft.value / 60)
  const s = timeLeft.value % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
})

// 获取唯一的本地存储 key
const getStorageKey = () => `exam_cache_${route.params.id}`

// 保存答题进度到本地
const saveProgressToLocal = () => {
  if (exam.value) {
    const cacheData = {
      answers: form.answers,
      timeLeft: timeLeft.value,
      timestamp: Date.now() // 记录保存时间，防止过期数据
    }
    localStorage.setItem(getStorageKey(), JSON.stringify(cacheData))
  }
}

// 监听答案变化，实时保存
watch(() => form.answers, () => {
  saveProgressToLocal()
}, { deep: true })

const startCountdown = () => {
  if (exam.value && exam.value.timeLimit) {
    // 优先从本地缓存恢复时间
    const cache = localStorage.getItem(getStorageKey())
    if (cache) {
      try {
        const cacheData = JSON.parse(cache)
        // 计算从上次退出到现在流逝的真实时间（毫秒）
        const timePassed = Date.now() - cacheData.timestamp
        const secondsPassed = Math.floor(timePassed / 1000)
        
        // 算出扣除流逝时间后的剩余时间
        const newTimeLeft = cacheData.timeLeft - secondsPassed

        if (newTimeLeft > 0) {
          // 如果时间还没耗尽，继续倒计时
          timeLeft.value = newTimeLeft
        } else {
          // 如果流逝的时间已经超过了剩余时间，说明在后台已经超时了
          timeLeft.value = 0
          ElMessageBox.alert('您在离开期间考试时间已耗尽，系统将自动为您交卷并扣除一次答题次数！', '时间已到', {
            confirmButtonText: '确定',
            type: 'error',
            showClose: false,
            closeOnClickModal: false,
            callback: () => {
              submitExam(true) // 强制交卷
            }
          })
          return
        }
      } catch (e) {
        timeLeft.value = exam.value.timeLimit * 60
      }
    } else {
      timeLeft.value = exam.value.timeLimit * 60
    }

    timer = setInterval(() => {
      if (timeLeft.value > 0) {
        timeLeft.value--
        // 每10秒保存一次时间进度，防止意外刷新
        if (timeLeft.value % 10 === 0) {
          saveProgressToLocal()
        }
      } else {
        clearInterval(timer)
        ElMessageBox.alert('考试时间已到，系统将自动为您交卷！', '时间到', {
          confirmButtonText: '确定',
          type: 'warning',
          showClose: false,
          closeOnClickModal: false,
          callback: () => {
            submitExam(true) // 强制交卷
          }
        })
      }
    }, 1000)
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
  // 当用户离开页面（无论以何种方式）时，检查倒计时是否还在继续
  if (timeLeft.value > 0) {
    // 若在答题过程中退出，将其剩余时间保存，以便下次回自动恢复
    saveProgressToLocal()
  }
})

const fetchExamDetail = async () => {
  try {
    const attemptsRes = await request.get(`/api/exam/attempts/${route.params.id}`)
    const attempts = attemptsRes.data
    
    const res = await request.get(`/api/exam/${route.params.id}`)
    exam.value = res.data.exam
    questions.value = res.data.questions
    
    // 如果已经完成或者次数超过 3 次，就进入“只读/查看成绩”模式，不再允许答题
      if (exam.value.isCompleted || attempts >= 3) {
        exam.value.isReadOnly = true
        
        // 获取成绩与答题状态记录
        let highestScore = '暂无';
        try {
          // 这里可以额外请求一个接口获取最高分，如果后端 res.data.exam 里没有直接带过来
          if (res.data.exam && res.data.exam.highestScore !== undefined) {
             highestScore = res.data.exam.highestScore;
          }
        } catch (e) {}

        ElMessageBox.alert(
          `您的历史最高分为: ${highestScore} 分`, 
          '测评结果', 
          {
            confirmButtonText: '查看解析',
            type: 'info',
            showClose: false,
            closeOnClickModal: true
          }
        )
        loading.value = false
        return
      }

    // 尝试从本地缓存恢复答案
    const cacheStr = localStorage.getItem(getStorageKey())
    let cachedAnswers = null
    if (cacheStr) {
      try {
        const cacheData = JSON.parse(cacheStr)
        if (Date.now() - cacheData.timestamp < 2 * 60 * 60 * 1000) {
          cachedAnswers = cacheData.answers
        }
      } catch (e) {}
    }

    // 初始化答案表单
    questions.value.forEach(q => {
      if (cachedAnswers && cachedAnswers[q.id] !== undefined) {
        form.answers[q.id] = cachedAnswers[q.id]
      } else {
        if (q.type === 'MULTIPLE_CHOICE') {
          form.answers[q.id] = [] // 多选为数组
        } else {
          form.answers[q.id] = '' // 单选/判断为字符串
        }
      }
    })
    
    // 启动倒计时
    startCountdown()
  } catch (error) {
    console.error('获取试卷详情失败:', error)
  } finally {
    loading.value = false
  }
}

const parseOptions = (optionsStr) => {
  try {
    return JSON.parse(optionsStr)
  } catch (e) {
    return []
  }
}

// 获取选项首字母，例如 "A. 苹果" 返回 "A"
const getOptionLetter = (opt) => {
  return opt.split('.')[0]
}

const getTypeName = (type) => {
  const map = {
    'SINGLE_CHOICE': '单选',
    'MULTIPLE_CHOICE': '多选',
    'JUDGE': '判断'
  }
  return map[type] || '未知'
}

const getTypeColor = (type) => {
  const map = {
    'SINGLE_CHOICE': '',
    'MULTIPLE_CHOICE': 'warning',
    'JUDGE': 'success'
  }
  return map[type] || 'info'
}

// 获取正确答案用于展示
const getCorrectAnswerDisplay = (question) => {
  if (!question || !question.answer) return '未提供'
  
  try {
    // 尝试解析是否为 JSON 数组（多选题）
    const parsed = JSON.parse(question.answer)
    if (Array.isArray(parsed)) {
      return parsed.map(idx => getOptionLetter('', parseInt(idx))).join(', ')
    }
  } catch (e) {
    // 如果不是 JSON，直接转换单个答案
  }
  
  // 处理单选题（通常是单个索引数字的字符串形式如 "0"）
  if (!isNaN(question.answer)) {
    return getOptionLetter('', parseInt(question.answer))
  }
  
  return question.answer
}

const submitExam = (isForce = false) => {
  if (isForce === true) {
    executeSubmit()
  } else {
    ElMessageBox.confirm('确定要交卷吗？', '提示', {
      confirmButtonText: '确定交卷',
      cancelButtonText: '继续答题',
      type: 'warning'
    }).then(() => {
      executeSubmit()
    }).catch(() => {})
  }
}

const executeSubmit = async () => {
  if (timer) clearInterval(timer)
  submitting.value = true
  
  // 转换答案格式：多选数组转为以逗号分隔的字符串
  const processedAnswers = {}
  for (const key in form.answers) {
    if (Array.isArray(form.answers[key])) {
      // 多选题答案需要排序，比如选了 C 和 A，转换后应为 "A,C"
      processedAnswers[key] = form.answers[key].sort().join(',')
    } else {
      processedAnswers[key] = form.answers[key]
    }
  }

  try {
    const res = await request.post('/api/exam/submit', {
      examId: exam.value.id,
      answers: processedAnswers
    })
    result.value = res.data
    resultDialogVisible.value = true
    
    // 交卷成功后，清除本地缓存
    localStorage.removeItem(getStorageKey())
  } catch (error) {
    console.error('交卷失败:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchExamDetail()
})
</script>

<style scoped>
.header {
  margin-bottom: 20px;
}

.exam-info {
  background-color: #f8f9fa;
  margin-bottom: 24px;
}

.question-card {
  margin-bottom: 20px;
}

.question-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 16px;
  line-height: 1.6;
}

.q-num {
  font-weight: bold;
  margin-right: 8px;
}

.q-type {
  margin-right: 8px;
}

.options {
  padding-left: 20px;
}

.el-radio, .el-checkbox {
  display: block;
  margin-bottom: 12px;
  white-space: normal;
  word-break: break-all;
}

.submit-action {
  text-align: center;
  margin: 40px 0;
}

.answer-analysis {
  margin-top: 16px;
  padding: 12px 16px;
  background-color: #f0f9eb;
  border-radius: 8px;
  border-left: 4px solid #67c23a;
}

.correct-answer {
  color: #67c23a;
  font-size: 15px;
}

.answer-text {
  font-weight: bold;
  font-size: 16px;
  margin-left: 8px;
}

.countdown-box {
  position: absolute;
  top: 50%;
  right: 20px;
  transform: translateY(-50%);
  background: #fdf6ec;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  padding: 10px 20px;
  text-align: center;
}

.countdown-box .time-label {
  font-size: 12px;
  color: #67C23A;
  margin-bottom: 5px;
}

.countdown-box .time-value {
  font-size: 24px;
  font-weight: bold;
  color: #67C23A;
  font-family: monospace;
}

.countdown-box .time-warning {
  color: #F56C6C !important;
}

.result-content {
  text-align: center;
  padding: 20px 0;
}

.text-success { color: #67C23A; }
.text-danger { color: #F56C6C; }

.score {
  font-size: 18px;
  color: #606266;
}

.score strong {
  font-size: 24px;
  color: #303133;
}

.reward {
  margin-top: 15px;
  color: #E6A23C;
  font-weight: bold;
  background-color: #fdf6ec;
  padding: 8px;
  border-radius: 4px;
}
</style>