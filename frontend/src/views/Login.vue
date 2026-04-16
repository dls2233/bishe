<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="logo-title">
        <h2>重游校园安全教育平台</h2>
      </div>
      <el-tabs v-model="activeName" class="auth-tabs">
        <!-- 登录 -->
        <el-tab-pane label="登 录" name="login">
          <el-form :model="loginForm" :rules="rules" ref="loginRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" clearable></el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password @keyup.enter="handleLogin"></el-input>
            </el-form-item>
            <el-button type="primary" style="width: 100%; margin-top: 10px;" @click="handleLogin" :loading="loading">登 录</el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注 册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入注册用户名" :prefix-icon="User" clearable></el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password></el-input>
            </el-form-item>
            <el-form-item prop="college">
              <el-select v-model="registerForm.college" placeholder="请选择您的学院" style="width: 100%">
                <el-option label="计算机学院" value="计算机学院"></el-option>
                <el-option label="通信与信息工程学院" value="通信与信息工程学院"></el-option>
                <el-option label="自动化学院" value="自动化学院"></el-option>
                <el-option label="光电工程学院" value="光电工程学院"></el-option>
                <el-option label="软件工程学院" value="软件工程学院"></el-option>
                <el-option label="经济管理学院" value="经济管理学院"></el-option>
                <el-option label="其他学院" value="其他学院"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="请输入常用邮箱" :prefix-icon="Message" clearable></el-input>
            </el-form-item>
            <el-form-item prop="code">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input v-model="registerForm.code" placeholder="请输入验证码" clearable></el-input>
                <el-button @click="sendCode" :disabled="countdown > 0" type="primary" plain>
                  {{ countdown > 0 ? `${countdown}s 后重试` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-button type="success" style="width: 100%; margin-top: 10px;" @click="handleRegister" :loading="loading">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import request from '../utils/request'

const store = useStore()
const router = useRouter()

const activeName = ref('login')
const loginRef = ref(null)
const registerRef = ref(null)
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', email: '', college: '', code: '' })
const countdown = ref(0)
let timer = null

const rules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱不能为空', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  college: [{ required: true, message: '请选择您的学院', trigger: 'change' }],
  code: [{ required: true, message: '验证码不能为空', trigger: 'blur' }]
}

const sendCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  // 简单验证邮箱格式
  const reg = /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/
  if (!reg.test(registerForm.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  try {
    await request.post('/api/user/sendCode', { email: registerForm.email })
    ElMessage.success('验证码发送成功，请注意查收')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败', error)
  }
}

const handleLogin = () => {
  loginRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await store.dispatch('login', loginForm)
        ElMessage.success('登录成功，欢迎回来！')
        router.replace('/') // 强制替换当前路由到首页
      } catch (error) {
        console.error('登录失败', error)
      } finally {
        loading.value = false
      }
    }
  })
}

const handleRegister = () => {
  registerRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await store.dispatch('register', registerForm)
        ElMessage.success('注册成功，请使用新账号登录')
        // 注册成功后清空表单并切换回登录Tab
        registerRef.value.resetFields()
        activeName.value = 'login'
      } catch (error) {
        console.error('注册失败', error)
      } finally {
        loading.value = false
      }
    }
  })
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: url('https://images.unsplash.com/photo-1541339907198-e08756dedf3f?auto=format&fit=crop&w=1920&q=80') center center / cover no-repeat;
  position: relative;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.8) 0%, rgba(54, 207, 201, 0.8) 100%);
  backdrop-filter: blur(4px);
}

.login-card {
  width: 440px;
  padding: 30px 20px;
  border-radius: 16px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
}

.logo-title {
  text-align: center;
  margin-bottom: 24px;
}

.logo-title h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(120deg, #409EFF, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.auth-tabs {
  margin-top: 10px;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

:deep(.el-button) {
  height: 44px;
  font-size: 16px;
}
</style>