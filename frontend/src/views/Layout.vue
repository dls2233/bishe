<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="logo">
        <el-icon class="logo-icon"><Lock /></el-icon>
        <span>重游安全教育平台</span>
      </div>
      <div class="user-actions" style="display: flex; align-items: center;">
        <el-tag :type="isConnected ? 'success' : 'danger'" size="small" effect="dark" style="margin-right: 15px;">
          SSE状态: {{ isConnected ? '已连接' : '已断开' }}
        </el-tag>
        <el-button v-if="isTeacher" type="warning" size="small" @click="testSingleAlert" style="margin-right: 15px;">
          <el-icon style="margin-right: 4px;"><Message /></el-icon>发送测试预警
        </el-button>
        <div class="user-info" style="display: flex; align-items: center;">
          <el-avatar v-if="user.avatarUrl" size="small" :src="`http://localhost:8080${user.avatarUrl}`" style="margin-right: 10px;" />
          <el-avatar v-else size="small" icon="el-icon-user-solid" style="margin-right: 10px;" />
          <span class="username">欢迎, {{ username || '测试用户' }}</span>
          <el-button type="danger" size="small" plain @click="logout" style="margin-left: 15px;">退出登录</el-button>
        </div>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px" class="aside">
        <el-menu 
          router 
          :default-active="$route.path"
          class="el-menu-vertical"
          background-color="#ffffff"
          text-color="#333333"
          active-text-color="#409EFF">
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>首页概览</span>
          </el-menu-item>
          <el-menu-item index="/course">
            <el-icon><VideoCamera /></el-icon>
            <span>安全课程</span>
          </el-menu-item>
          <el-menu-item index="/exam">
            <el-icon><Document /></el-icon>
            <span>在线测评</span>
          </el-menu-item>
          <el-menu-item index="/mall">
            <el-icon><Goods /></el-icon>
            <span>积分商城</span>
          </el-menu-item>
          <el-menu-item index="/news">
            <el-icon><DataBoard /></el-icon>
            <span>新闻资讯</span>
          </el-menu-item>
          <el-menu-item index="/chat">
            <el-icon><ChatLineRound /></el-icon>
            <span>智能咨询</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>

          <!-- 教师专属菜单 -->
          <el-sub-menu index="/teacher" v-if="isTeacher">
            <template #title>
              <el-icon><Management /></el-icon>
              <span>教师管理台</span>
            </template>
            <el-menu-item index="/teacher/course/publish">
              <el-icon><VideoPlay /></el-icon>
              <span>发布课程</span>
            </el-menu-item>
            <el-menu-item index="/teacher/exam/publish">
              <el-icon><Edit /></el-icon>
              <span>发布测评</span>
            </el-menu-item>
            <el-menu-item index="/teacher/alert">
              <el-icon><Bell /></el-icon>
              <span>智能预警中心</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { HomeFilled, VideoCamera, Document, Goods, DataBoard, User, Lock, Message, Management, ChatLineRound, Bell, VideoPlay, Edit } from '@element-plus/icons-vue'
import { ElNotification, ElMessage } from 'element-plus'
import request from '../utils/request'

const store = useStore()
const router = useRouter()

const username = computed(() => store.state.user.username)
const user = computed(() => store.state.user)
const isTeacher = computed(() => store.state.user.role === 'TEACHER' || store.state.user.role === 'ADMIN')

const logout = () => {
  store.commit('LOGOUT')
  router.push('/login')
}

let eventSource = null;
let reconnectTimer = null;
const isConnected = ref(false)

const initSSE = () => {
  const token = store.state.token;
  if (!token) return;

  // 每次重连前确保先关闭旧连接
  if (eventSource) {
    eventSource.close();
  }

  // 使用原生 EventSource 发起 SSE 连接，携带 token 参数过拦截器
  eventSource = new EventSource(`http://localhost:8080/api/sse/connect?token=${token}`);

  eventSource.onopen = () => {
    isConnected.value = true;
    console.log('SSE 连接成功建立');
  };

  // 监听后端发送的事件
  eventSource.onmessage = (event) => {
    // 忽略刚建立连接时发送的确认消息
    if (event.data === 'CONNECTED') {
      return;
    }

    try {
      const alertData = JSON.parse(event.data);
      // 根据危险等级映射到 ElementPlus Notification 的类型
      let notifType = 'info';
      if (alertData.level === 'WARNING') notifType = 'warning';
      if (alertData.level === 'DANGER') notifType = 'error';

      ElNotification({
        title: `🚨 安全预警: ${alertData.title}`,
        message: alertData.content,
        type: notifType,
        duration: 0, // 0表示不会自动关闭，必须用户手动点击
        position: 'top-right',
      });
    } catch (e) {
      console.error('解析 SSE 消息失败', e);
    }
  };

  eventSource.onerror = (error) => {
    console.error('SSE 连接发生异常或已断开，准备重连...', error);
    isConnected.value = false;
    eventSource.close(); // 关闭出错的连接
    
    // 自定义重连逻辑：每隔 5 秒尝试重新连接一次
    if (reconnectTimer) clearTimeout(reconnectTimer);
    reconnectTimer = setTimeout(() => {
      console.log('正在尝试重新连接 SSE...');
      initSSE();
    }, 5000);
  };
};

// 触发测试预警的方法
const testSingleAlert = async () => {
  try {
    await request.post('/api/alert/test-single')
    ElMessage.success('测试推送指令已发送，请注意右上角弹窗！')
  } catch (error) {
    console.error('发送测试推送失败', error)
  }
}

onMounted(() => {
  initSSE();
})

onUnmounted(() => {
  if (eventSource) {
    eventSource.close();
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.header {
  background-color: #ffffff;
  color: #303133;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  z-index: 10;
  height: 64px;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
  background: linear-gradient(120deg, #409EFF, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.logo-icon {
  font-size: 26px;
  margin-right: 10px;
  color: #409EFF;
  -webkit-text-fill-color: initial;
}

.user-info {
  display: flex;
  align-items: center;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.aside {
  background-color: #fff;
  border-right: none;
  box-shadow: 4px 0 16px rgba(0, 0, 0, 0.02);
  z-index: 5;
  padding-top: 10px;
}

.el-menu-vertical {
  border-right: none;
}

.el-menu-item, .el-sub-menu__title {
  margin: 4px 12px;
  border-radius: 8px;
  height: 48px;
  line-height: 48px;
}

.el-menu-item.is-active {
  background-color: #ecf5ff !important;
  color: #409EFF;
  font-weight: 600;
}

.main {
  background-color: #f5f7fa;
  padding: 24px;
  overflow-y: auto;
}

/* 路由切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(15px);
}
</style>