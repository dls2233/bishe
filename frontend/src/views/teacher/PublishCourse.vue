<template>
  <div class="publish-course">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>发布新课程</span>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="课程标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入课程标题"></el-input>
        </el-form-item>
        
        <el-form-item label="课程分类" prop="category">
          <el-input v-model="form.category" placeholder="例如：消防安全、实验室安全"></el-input>
        </el-form-item>

        <el-form-item label="封面图片上传" prop="coverUrl">
          <el-upload
            class="upload-demo"
            action="/api/upload"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :before-upload="beforeImageUpload"
          >
            <el-button type="primary">点击上传封面图片</el-button>
            <template #tip>
              <div class="el-upload__tip">只能上传 jpg/png 文件，且不超过 5MB。如果不上传也可以直接在下方填写URL。</div>
            </template>
          </el-upload>
          <el-input v-model="form.coverUrl" placeholder="或直接输入图片URL" style="margin-top: 10px;"></el-input>
          <div v-if="form.coverUrl" style="margin-top: 10px;">
            <el-image :src="form.coverUrl" style="width: 200px; height: 112px; object-fit: cover; border-radius: 8px;"></el-image>
          </div>
        </el-form-item>

        <el-form-item label="视频文件上传" prop="videoUrl">
          <el-upload
            class="upload-demo"
            action="/api/upload"
            :show-file-list="false"
            :on-success="handleVideoSuccess"
            :before-upload="beforeVideoUpload"
            :on-progress="handleVideoProgress"
          >
            <el-button type="primary" :loading="videoUploading">点击上传视频</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 mp4 格式，视频较大时上传需要一定时间，请耐心等待。如果不上传也可以直接在下方填写视频URL。</div>
            </template>
          </el-upload>
          <el-progress v-if="videoUploading" :percentage="videoUploadPercent" style="margin-top: 10px;"></el-progress>
          <el-input v-model="form.videoUrl" placeholder="或直接输入视频URL (选填)" style="margin-top: 10px;"></el-input>
          <div v-if="form.videoUrl && !videoUploading" style="margin-top: 10px;">
            <video :src="form.videoUrl" controls style="width: 300px; border-radius: 8px;"></video>
          </div>
        </el-form-item>

        <el-form-item label="学习内容" prop="content">
          <el-input type="textarea" v-model="form.content" :rows="8" placeholder="请输入课程学习文章内容"></el-input>
        </el-form-item>

        <el-form-item label="奖励积分" prop="rewardPoints">
          <el-input-number v-model="form.rewardPoints" :min="0" :max="1000"></el-input-number>
        </el-form-item>

        <el-divider>随堂测验题目设置</el-divider>

        <div v-for="(q, index) in quizList" :key="index" class="quiz-item">
          <el-card shadow="hover" style="margin-bottom: 15px;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
              <b>题目 {{ index + 1 }}</b>
              <el-button type="danger" size="small" @click="removeQuiz(index)">删除</el-button>
            </div>
            <el-form-item label="题目" :prop="'quiz.' + index + '.question'" :rules="{ required: true, message: '题目不能为空', trigger: 'blur' }">
              <el-input v-model="q.question" placeholder="请输入题目"></el-input>
            </el-form-item>
            
            <el-form-item label="选项A" :prop="'quiz.' + index + '.options.0'" :rules="{ required: true, message: '选项不能为空', trigger: 'blur' }">
              <el-input v-model="q.options[0]" placeholder="请输入选项A"></el-input>
            </el-form-item>
            <el-form-item label="选项B" :prop="'quiz.' + index + '.options.1'" :rules="{ required: true, message: '选项不能为空', trigger: 'blur' }">
              <el-input v-model="q.options[1]" placeholder="请输入选项B"></el-input>
            </el-form-item>
            <el-form-item label="选项C" :prop="'quiz.' + index + '.options.2'" :rules="{ required: true, message: '选项不能为空', trigger: 'blur' }">
              <el-input v-model="q.options[2]" placeholder="请输入选项C"></el-input>
            </el-form-item>
            <el-form-item label="选项D" :prop="'quiz.' + index + '.options.3'" :rules="{ required: true, message: '选项不能为空', trigger: 'blur' }">
              <el-input v-model="q.options[3]" placeholder="请输入选项D"></el-input>
            </el-form-item>

            <el-form-item label="正确答案" :prop="'quiz.' + index + '.answer'" :rules="{ required: true, message: '请选择正确答案', trigger: 'change' }">
              <el-select v-model="q.answer" placeholder="请选择正确选项">
                <el-option label="A" :value="0"></el-option>
                <el-option label="B" :value="1"></el-option>
                <el-option label="C" :value="2"></el-option>
                <el-option label="D" :value="3"></el-option>
              </el-select>
            </el-form-item>
          </el-card>
        </div>

        <el-button type="dashed" style="width: 100%; margin-bottom: 20px;" @click="addQuiz">+ 添加一道题目</el-button>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="loading">发 布 课 程</el-button>
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
const videoUploading = ref(false)
const videoUploadPercent = ref(0)

const quizList = ref([
  { question: '', options: ['', '', '', ''], answer: null }
])

const form = reactive({
  title: '',
  category: '',
  coverUrl: '',
  videoUrl: '',
  content: '',
  rewardPoints: 10,
})

const rules = {
  title: [{ required: true, message: '请输入课程标题', trigger: 'blur' }],
  category: [{ required: true, message: '请输入课程分类', trigger: 'blur' }],
  content: [{ required: true, message: '请输入课程内容', trigger: 'blur' }],
  rewardPoints: [{ required: true, message: '请输入奖励积分', trigger: 'blur' }]
}

const addQuiz = () => {
  quizList.value.push({ question: '', options: ['', '', '', ''], answer: null })
}

const removeQuiz = (index) => {
  quizList.value.splice(index, 1)
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      if (quizList.value.length === 0) {
        ElMessage.warning('请至少添加一道测验题目')
        return
      }

      loading.value = true
      try {
        const payload = {
          ...form,
          quiz: JSON.stringify(quizList.value)
        }
        await request.post('/api/course/publish', payload)
        ElMessage.success('课程发布成功！')
        router.push('/course')
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
  quizList.value = [{ question: '', options: ['', '', '', ''], answer: null }]
  form.coverUrl = ''
  form.videoUrl = ''
  videoUploadPercent.value = 0
}

const handleCoverSuccess = (res) => {
  if (res.code === 200) {
    form.coverUrl = res.data
    ElMessage.success('封面图片上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const beforeImageUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif'
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('上传图片只能是 JPG/PNG/GIF 格式!')
  }
  if (!isLt5M) {
    ElMessage.error('上传图片大小不能超过 5MB!')
  }
  return isImage && isLt5M
}

const handleVideoSuccess = (res) => {
  videoUploading.value = false
  if (res.code === 200) {
    form.videoUrl = res.data
    ElMessage.success('视频上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const beforeVideoUpload = (file) => {
  const isVideo = file.type === 'video/mp4' || file.type === 'video/webm' || file.type === 'video/ogg'
  // 考虑到后端的配置修改为500MB
  const isLt500M = file.size / 1024 / 1024 < 500

  if (!isVideo) {
    ElMessage.error('上传视频只能是 MP4/WEBM/OGG 格式!')
  }
  if (!isLt500M) {
    ElMessage.error('上传视频大小不能超过 500MB!')
  }
  if (isVideo && isLt500M) {
    videoUploading.value = true
    videoUploadPercent.value = 0
  }
  return isVideo && isLt500M
}

const handleVideoProgress = (event) => {
  videoUploadPercent.value = parseInt(event.percent, 10)
}
</script>

<style scoped>
.publish-course {
  max-width: 800px;
  margin: 0 auto;
}
.card-header {
  font-size: 18px;
  font-weight: bold;
}
</style>
