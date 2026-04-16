<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="user-card" shadow="hover">
          <div class="avatar-wrapper">
            <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/api/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :on-error="handleAvatarError"
              :before-upload="beforeAvatarUpload"
            >
              <el-avatar v-if="user.avatarUrl" :size="100" :src="`http://localhost:8080${user.avatarUrl}`" />
              <el-avatar v-else :size="100" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <div class="upload-hover-text">点击修改头像</div>
            </el-upload>
          </div>
          <div class="user-info">
            <h2 class="username">{{ user.username }}</h2>
            <el-tag :type="user.role === 'TEACHER' ? 'danger' : 'success'" effect="dark" class="role-tag">
              {{ user.role === 'TEACHER' ? '教 师' : '学 生' }}
            </el-tag>
          </div>
          <el-divider />
          <div class="user-detail-list">
            <div class="detail-item">
              <el-icon><User /></el-icon>
              <span class="label">真实姓名:</span>
              <span class="value">{{ user.realName || '未填写' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><School /></el-icon>
              <span class="label">所属学院:</span>
              <span class="value">{{ user.college || '未填写' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Message /></el-icon>
              <span class="label">联系邮箱:</span>
              <span class="value">{{ user.email || '未填写' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Document /></el-icon>
              <span class="label">个人简介:</span>
              <span class="value">{{ user.bio || '未填写' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Trophy /></el-icon>
              <span class="label">获奖经历:</span>
              <span class="value">{{ user.awards || '未填写' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Star /></el-icon>
              <span class="label">兴趣爱好:</span>
              <span class="value">{{ user.hobbies || '未填写' }}</span>
            </div>
            <div class="detail-item points-item">
              <el-icon><Coin /></el-icon>
              <span class="label">可用积分:</span>
              <span class="value points-value">{{ user.points || 0 }} 分</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="content-card" shadow="hover">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本信息修改" name="basic">
              <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width: 500px; margin-top: 20px;">
                <el-form-item label="用户名">
                  <el-input v-model="user.username" disabled />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="form.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item label="所属学院">
                  <el-input v-model="user.college" disabled placeholder="所属学院不可修改" />
                </el-form-item>
                <el-form-item label="联系邮箱" prop="email">
                  <el-input v-model="form.email" placeholder="请输入联系邮箱" />
                </el-form-item>
                <el-form-item label="个人简介" prop="bio">
                  <el-input type="textarea" :rows="3" v-model="form.bio" placeholder="介绍一下自己吧" />
                </el-form-item>
                <el-form-item label="获奖经历" prop="awards">
                  <el-input type="textarea" :rows="2" v-model="form.awards" placeholder="请填写您的获奖经历" />
                </el-form-item>
                <el-form-item label="兴趣爱好" prop="hobbies">
                  <el-input v-model="form.hobbies" placeholder="例如：篮球, 编程, 音乐" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitUpdate" :loading="updating" :disabled="!isModified">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" style="max-width: 500px; margin-top: 20px;">
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="warning" @click="submitPassword" :loading="pwdUpdating">确认修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { User, School, Message, Coin, Document, Trophy, Star } from '@element-plus/icons-vue'
import request from '../utils/request'

const store = useStore()
const user = computed(() => store.state.user)

const activeTab = ref('basic')
const formRef = ref(null)
const pwdFormRef = ref(null)
const updating = ref(false)
const pwdUpdating = ref(false)

const uploadHeaders = computed(() => {
  return {
    Authorization: `Bearer ${store.state.token}`
  }
})

const form = reactive({
  realName: '',
  email: '',
  bio: '',
  awards: '',
  hobbies: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const isModified = computed(() => {
  return form.realName !== (user.value.realName || '') ||
         form.email !== (user.value.email || '') ||
         form.bio !== (user.value.bio || '') ||
         form.awards !== (user.value.awards || '') ||
         form.hobbies !== (user.value.hobbies || '')
})

// Initialize form data
onMounted(() => {
  form.realName = user.value.realName || ''
  form.email = user.value.email || ''
  form.bio = user.value.bio || ''
  form.awards = user.value.awards || ''
  form.hobbies = user.value.hobbies || ''
})

const rules = {
  realName: [{ required: true, message: '真实姓名不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱不能为空', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }]
}

const submitUpdate = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      updating.value = true
      try {
        await request.post('/api/user/update', form)
        ElMessage.success('个人信息更新成功')
        // 重新获取用户信息刷新 store
        await store.dispatch('fetchUserInfo')
      } catch (error) {
        console.error('更新失败', error)
      } finally {
        updating.value = false
      }
    }
  })
}

const submitPassword = () => {
  pwdFormRef.value.validate(async (valid) => {
    if (valid) {
      pwdUpdating.value = true
      try {
        await request.post('/api/user/updatePassword', pwdForm)
        ElMessage.success('密码修改成功，请下次使用新密码登录')
        pwdFormRef.value.resetFields()
      } catch (error) {
        console.error('修改密码失败', error)
      } finally {
        pwdUpdating.value = false
      }
    }
  })
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('头像只能是 JPG/PNG/GIF 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('头像图片大小不能超过 2MB!')
  }
  return isImage && isLt2M
}

const handleAvatarSuccess = async (res, file) => {
  if (res.code === 200) {
    try {
      // res.data contains the file URL returned by backend
      await request.post('/api/user/updateAvatar', { avatarUrl: res.data })
      ElMessage.success('头像更新成功')
      await store.dispatch('fetchUserInfo')
    } catch (error) {
      console.error('头像保存失败', error)
      ElMessage.error('头像保存失败，请稍后重试')
    }
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

const handleAvatarError = (err, file, fileList) => {
  console.error('上传失败', err)
  ElMessage.error('上传头像失败，请检查网络或图片大小')
}
</script>

<style scoped>
.profile-container {
  padding: 10px;
}

.user-card {
  text-align: center;
  padding-bottom: 20px;
}

.avatar-wrapper {
  margin: 20px 0;
  position: relative;
  display: inline-block;
}

.avatar-uploader {
  cursor: pointer;
  position: relative;
}

.upload-hover-text {
  position: absolute;
  top: 0;
  left: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .upload-hover-text {
  opacity: 1;
}

.username {
  margin: 10px 0 5px;
  font-size: 22px;
  color: #303133;
}

.role-tag {
  margin-bottom: 15px;
}

.user-detail-list {
  text-align: left;
  padding: 0 20px;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-size: 14px;
  color: #606266;
}

.detail-item .el-icon {
  margin-right: 10px;
  font-size: 18px;
  color: #909399;
}

.detail-item .label {
  width: 80px;
  color: #909399;
}

.detail-item .value {
  flex: 1;
  font-weight: 500;
}

.points-item {
  background-color: #fdf6ec;
  padding: 10px;
  border-radius: 8px;
  margin-top: 20px;
}

.points-item .el-icon {
  color: #E6A23C;
}

.points-value {
  color: #E6A23C;
  font-size: 18px;
  font-weight: bold;
}

.content-card {
  min-height: 450px;
}
</style>
