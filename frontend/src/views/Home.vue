<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card shadow="hover" class="welcome-card">
          <div class="welcome-content">
            <h2>🎉 欢迎使用重游校园安全教育平台</h2>
            <p>在这里，你可以学习到最新的消防安全、交通安全以及网络安全知识。平台提供课程学习、在线测评以及丰富的积分奖励机制，参与安全教育，让校园生活更加美好安全。</p>
            <el-button type="primary" size="large" @click="$router.push('/course')">开始学习课程</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon bg-blue"><el-icon><VideoCamera /></el-icon></div>
          <div class="stat-info">
            <div class="title">已完成课程</div>
            <div class="value">{{ stats.completedCourses }} <span class="unit">节</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon bg-green"><el-icon><Document /></el-icon></div>
          <div class="stat-info">
            <div class="title">测评通过次数</div>
            <div class="value">{{ stats.passedExams }} <span class="unit">次</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon bg-orange"><el-icon><Trophy /></el-icon></div>
          <div class="stat-info">
            <div class="title">当前积分</div>
            <div class="value">{{ stats.points }} <span class="unit">分</span></div>
          </div>
        </el-card>
      </el-col>
      <!-- 修改为实时在线人数卡片 -->
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon bg-purple"><el-icon><UserFilled /></el-icon></div>
          <div class="stat-info">
            <div class="title">当前在线人数</div>
            <div class="value">{{ onlineCount }} <span class="unit">人</span></div>
            <div class="sub-desc" style="font-size: 12px; color: #909399; margin-top: 4px;">基于 SSE 实时统计</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

      <!-- 快捷功能区 -->
    <el-row :gutter="20" class="action-row">
      <el-col :span="12">
        <el-card shadow="hover" class="action-card" @click="$router.push('/chat')">
          <div class="action-content">
            <div class="action-icon ai-icon">
              <el-icon><ChatLineRound /></el-icon>
            </div>
            <div class="action-text">
              <h3>智能安全咨询</h3>
              <p>遇到安全问题？7x24小时 AI 助手为您提供专业防骗、消防及网络安全解答。</p>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="action-card" @click="$router.push('/news')">
          <div class="action-content">
            <div class="action-icon news-icon">
              <el-icon><DataBoard /></el-icon>
            </div>
            <div class="action-text">
              <h3>校园安全资讯</h3>
              <p>获取最新校园安全动态、防诈骗预警与科技前沿资讯，实时同步更新。</p>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { VideoCamera, Document, Trophy, UserFilled, ChatLineRound, DataBoard, ArrowRight } from '@element-plus/icons-vue'
import request from '../utils/request'

const onlineCount = ref(0)
const stats = ref({
  completedCourses: 0,
  passedExams: 0,
  points: 0
})
let timer = null

const fetchOnlineCount = async () => {
  try {
    const res = await request.get('/api/sse/online-count')
    onlineCount.value = res.data
  } catch (e) {
    console.error('获取在线人数失败', e)
  }
}

const fetchStats = async () => {
  try {
    const res = await request.get('/api/dashboard/stats')
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

onMounted(() => {
  // 初始获取
  fetchOnlineCount()
  fetchStats()
  
  // 每 3 秒轮询一次最新在线人数
  timer = setInterval(() => {
    fetchOnlineCount()
  }, 3000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.home-container {
  animation: fadeIn 0.5s;
}

.welcome-card {
  margin-bottom: 24px;
  background: linear-gradient(135deg, #e6f7ff 0%, #ffffff 100%);
  border: none;
  position: relative;
  overflow: hidden;
}

.welcome-card::after {
  content: '';
  position: absolute;
  top: -50px;
  right: -50px;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(64,158,255,0.1) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
}

.welcome-content {
  padding: 30px;
  position: relative;
  z-index: 1;
}

.welcome-content h2 {
  color: #1f2937;
  margin-top: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.welcome-content p {
  color: #4b5563;
  line-height: 1.8;
  font-size: 16px;
  margin-bottom: 24px;
  max-width: 800px;
}

.stat-row {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
  height: 115px; /* 稍微增加一点高度 */
  overflow: hidden; /* 隐藏溢出内容，消除内部可能出现的滚动条 */
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 28px rgba(0,0,0,0.08) !important;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  width: 100%;
  padding: 24px;
  align-items: center;
  height: 100%; /* 充满卡片 */
  box-sizing: border-box;
  overflow: hidden; /* 防止内部元素撑出滚动条 */
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 28px;
  color: white;
  margin-right: 16px;
  flex-shrink: 0; /* 防止图标被压缩 */
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.bg-blue { background: linear-gradient(135deg, #409EFF, #36cfc9); }
.bg-green { background: linear-gradient(135deg, #67C23A, #95d475); }
.bg-orange { background: linear-gradient(135deg, #E6A23C, #f3d19e); }
.bg-purple { background: linear-gradient(135deg, #8b5cf6, #c4b5fd); }

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0; /* 防止内容撑破容器 */
}

.stat-info .title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-info .value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-info .unit {
  font-size: 13px;
  font-weight: normal;
  color: #909399;
  margin-left: 2px;
}

.action-row {
  margin-top: 20px;
}

.action-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  height: 100px;
}

.action-card :deep(.el-card__body) {
  display: flex;
  width: 100%;
  padding: 20px;
  align-items: center;
  height: 100%;
}

.action-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06) !important;
}

.action-content {
  display: flex;
  align-items: center;
  width: 100%;
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  color: white;
  margin-right: 20px;
}

.ai-icon {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}

.news-icon {
  background: linear-gradient(135deg, #f472b6, #ec4899);
}

.action-text {
  flex: 1;
}

.action-text h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #303133;
}

.action-text p {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.arrow-icon {
  font-size: 20px;
  color: #c0c4cc;
  transition: transform 0.3s;
}

.action-card:hover .arrow-icon {
  transform: translateX(5px);
  color: #409eff;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>