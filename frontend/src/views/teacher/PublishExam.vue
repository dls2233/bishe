<template>
  <div class="publish-exam">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>发布新测评</span>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="测评标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入测评标题"></el-input>
        </el-form-item>
        
        <el-form-item label="测评描述" prop="description">
          <el-input type="textarea" v-model="form.description" placeholder="请输入测评描述"></el-input>
        </el-form-item>

        <el-form-item label="是否必修" prop="isMandatory">
          <el-switch v-model="form.isMandatory" active-text="是" inactive-text="否"></el-switch>
        </el-form-item>

        <el-form-item label="时间限制(分钟)" prop="timeLimit">
          <el-input-number v-model="form.timeLimit" :min="1" :max="300"></el-input-number>
        </el-form-item>

        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="datetime" placeholder="选择截止日期时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss"></el-date-picker>
        </el-form-item>

        <el-form-item label="及格分数" prop="passScore">
          <el-input-number v-model="form.passScore" :min="1" :max="1000"></el-input-number>
        </el-form-item>

        <el-divider>题目设置</el-divider>

        <div v-for="(q, index) in questionList" :key="index" class="quiz-item">
          <el-card shadow="hover" style="margin-bottom: 15px;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
              <b>题目 {{ index + 1 }}</b>
              <el-button type="danger" size="small" @click="removeQuestion(index)">删除</el-button>
            </div>
            
            <el-form-item label="题目类型" :prop="'questions.' + index + '.type'" :rules="{ required: true, message: '类型不能为空', trigger: 'change' }">
              <el-select v-model="q.type" placeholder="请选择题目类型">
                <el-option label="单选题" value="SINGLE_CHOICE"></el-option>
                <el-option label="判断题" value="TRUE_FALSE"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="题目内容" :prop="'questions.' + index + '.content'" :rules="{ required: true, message: '题目不能为空', trigger: 'blur' }">
              <el-input v-model="q.content" placeholder="请输入题目"></el-input>
            </el-form-item>
            
            <div v-if="q.type === 'SINGLE_CHOICE'">
              <el-form-item label="选项A" :prop="'questions.' + index + '.options.0'" :rules="{ required: true, message: '选项不能为空', trigger: 'blur' }">
                <el-input v-model="q.optionsArray[0]" placeholder="请输入选项A"></el-input>
              </el-form-item>
              <el-form-item label="选项B" :prop="'questions.' + index + '.options.1'" :rules="{ required: true, message: '选项不能为空', trigger: 'blur' }">
                <el-input v-model="q.optionsArray[1]" placeholder="请输入选项B"></el-input>
              </el-form-item>
              <el-form-item label="选项C" :prop="'questions.' + index + '.options.2'" :rules="{ required: true, message: '选项不能为空', trigger: 'blur' }">
                <el-input v-model="q.optionsArray[2]" placeholder="请输入选项C"></el-input>
              </el-form-item>
              <el-form-item label="选项D" :prop="'questions.' + index + '.options.3'" :rules="{ required: true, message: '选项不能为空', trigger: 'blur' }">
                <el-input v-model="q.optionsArray[3]" placeholder="请输入选项D"></el-input>
              </el-form-item>

              <el-form-item label="正确答案" :prop="'questions.' + index + '.answer'" :rules="{ required: true, message: '请选择正确答案', trigger: 'change' }">
                <el-select v-model="q.answer" placeholder="请选择正确选项">
                  <el-option label="A" value="A"></el-option>
                  <el-option label="B" value="B"></el-option>
                  <el-option label="C" value="C"></el-option>
                  <el-option label="D" value="D"></el-option>
                </el-select>
              </el-form-item>
            </div>
            
            <div v-else-if="q.type === 'TRUE_FALSE'">
              <el-form-item label="正确答案" :prop="'questions.' + index + '.answer'" :rules="{ required: true, message: '请选择正确答案', trigger: 'change' }">
                <el-select v-model="q.answer" placeholder="请选择正确选项">
                  <el-option label="正确" value="T"></el-option>
                  <el-option label="错误" value="F"></el-option>
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="题目分值" :prop="'questions.' + index + '.score'" :rules="{ required: true, message: '请输入分值', trigger: 'blur' }">
              <el-input-number v-model="q.score" :min="1" :max="100"></el-input-number>
            </el-form-item>

          </el-card>
        </div>

        <el-button type="dashed" style="width: 100%; margin-bottom: 20px;" @click="addQuestion">+ 添加一道题目</el-button>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="loading">发 布 测 评</el-button>
          <el-button @click="resetForm">重 置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useRouter } from 'vue-router'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const questionList = ref([
  { type: 'SINGLE_CHOICE', content: '', optionsArray: ['', '', '', ''], answer: '', score: 5 }
])

const form = reactive({
  title: '',
  description: '',
  isMandatory: false,
  timeLimit: 30,
  deadline: '',
  passScore: 60,
})

const rules = {
  title: [{ required: true, message: '请输入测评标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入测评描述', trigger: 'blur' }],
  timeLimit: [{ required: true, message: '请输入时间限制', trigger: 'blur' }],
  passScore: [{ required: true, message: '请输入及格分数', trigger: 'blur' }]
}

const addQuestion = () => {
  questionList.value.push({ type: 'SINGLE_CHOICE', content: '', optionsArray: ['', '', '', ''], answer: '', score: 5 })
}

const removeQuestion = (index) => {
  questionList.value.splice(index, 1)
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      if (questionList.value.length === 0) {
        ElMessage.warning('请至少添加一道测验题目')
        return
      }

      let totalScore = 0;
      const formattedQuestions = questionList.value.map(q => {
        totalScore += q.score;
        return {
          type: q.type,
          content: q.content,
          options: q.type === 'SINGLE_CHOICE' ? JSON.stringify(q.optionsArray) : null,
          answer: q.answer,
          score: q.score
        }
      })

      loading.value = true
      try {
        const payload = {
          exam: {
            ...form,
            totalScore: totalScore
          },
          questions: formattedQuestions
        }
        await request.post('/api/exam/publish', payload)
        ElMessage.success('测评发布成功！')
        router.push('/exam')
      } catch (error) {
        console.error(error)
        ElMessage.error('发布失败，请重试')
      } finally {
        loading.value = false
      }
    } else {
      ElMessage.warning('请完善表单信息')
    }
  })
}

const resetForm = () => {
  formRef.value.resetFields()
  questionList.value = [{ type: 'SINGLE_CHOICE', content: '', optionsArray: ['', '', '', ''], answer: '', score: 5 }]
}
</script>

<style scoped>
.publish-exam {
  max-width: 800px;
  margin: 0 auto;
}
.card-header {
  font-size: 18px;
  font-weight: bold;
}
</style>
