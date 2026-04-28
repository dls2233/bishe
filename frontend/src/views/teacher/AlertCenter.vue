<template>
  <div class="alert-center">
    <el-card class="header-card">
      <div class="header-content">
        <h2>🔔 智能预警中心</h2>
        <p>校园安全自动预警系统：AI WebSearch驱动、实时热点、自动推送</p>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：预警功能概览 -->
      <el-col :span="8">
        <el-card class="stat-card">
          <template #header>
            <div class="card-header">
              <span>预警机制</span>
            </div>
          </template>
          
          <div class="feature-list">
            <div class="feature-item" style="background: linear-gradient(135deg, #ff6b6b15, #ffd93d15);">
              <el-icon color="#ff6b6b"><TrendCharts /></el-icon>
              <div class="feature-info">
                <div class="feature-title">实时热点预警</div>
                <div class="feature-desc">AI WebSearch驱动，每4小时推送最新安全热点</div>
              </div>
              <el-tag type="danger" effect="dark">NEW</el-tag>
            </div>
            
            <div class="feature-item">
              <el-icon color="#409EFF"><Document /></el-icon>
              <div class="feature-info">
                <div class="feature-title">安全资讯预警</div>
                <div class="feature-desc">每30分钟扫描新闻关键词</div>
              </div>
              <el-tag type="success">运行中</el-tag>
            </div>
            
            <div class="feature-item">
              <el-icon color="#E6A23C"><User /></el-icon>
              <div class="feature-info">
                <div class="feature-title">学习行为预警</div>
                <div class="feature-desc">连续不通过自动提醒</div>
              </div>
              <el-tag type="success">运行中</el-tag>
            </div>
            
            <div class="feature-item">
              <el-icon color="#67C23A"><Calendar /></el-icon>
              <div class="feature-info">
                <div class="feature-title">季节性预警</div>
                <div class="feature-desc">每日8点定时推送</div>
              </div>
              <el-tag type="success">运行中</el-tag>
            </div>
          </div>
        </el-card>

        <!-- 快捷操作 -->
        <el-card class="action-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          
          <div class="action-buttons">
            <el-button type="danger" @click="showHotspotDialog" style="width: 100%; margin-bottom: 10px;">
              🔴 触发热点预警
            </el-button>
            <el-button type="primary" @click="testSeasonalAlert" style="width: 100%; margin-bottom: 10px;">
              📅 测试季节性预警
            </el-button>
            <el-button type="warning" @click="showKeywordDialog" style="width: 100%; margin-bottom: 10px;">
              🚨 测试关键词预警
            </el-button>
            <el-button type="info" @click="refreshAlerts" style="width: 100%;">
              🔄 刷新列表
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：热点事件库 + 预警历史 -->
      <el-col :span="16">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="真实案例库" name="hotspots">
            <el-row :gutter="15">
              <el-col :span="24">
                <el-card v-for="(hotspot, index) in hotspotList" :key="index" style="margin-bottom: 15px;">
                  <div style="display: flex; justify-content: space-between; align-items: start;">
                    <div style="flex: 1;">
                      <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                        <el-tag :type="getLevelType(hotspot.level)" effect="dark">
                          {{ getLevelText(hotspot.level) }}
                        </el-tag>
                        <span style="font-weight: bold; font-size: 16px;">{{ hotspot.title }}</span>
                        <span style="color: #909399; font-size: 12px;">{{ hotspot.date }}</span>
                      </div>
                      <p style="color: #606266; margin-bottom: 10px;">{{ hotspot.description }}</p>
                      <div style="margin-bottom: 10px;">
                        <span style="font-size: 12px; color: #909399;">标签：</span>
                        <el-tag v-for="tag in hotspot.categories" :key="tag" size="small" style="margin-right: 5px;">
                          {{ tag }}
                        </el-tag>
                      </div>
                      <div>
                        <span style="font-size: 12px; color: #909399;">建议：</span>
                        <ul style="margin: 5px 0 0 20px; color: #606266; font-size: 13px;">
                          <li v-for="(suggestion, i) in hotspot.suggestions" :key="i">{{ suggestion }}</li>
                        </ul>
                      </div>
                    </div>
                    <el-button type="danger" size="small" @click="triggerHotspot(index)">
                      发布预警
                    </el-button>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>
          
          <el-tab-pane label="预警历史" name="history">
            <el-table :data="alertList" style="width: 100%" v-loading="loading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="title" label="标题" width="300" />
              <el-table-column prop="content" label="内容" show-overflow-tooltip />
              <el-table-column prop="level" label="级别" width="120">
                <template #default="{ row }">
                  <el-tag :type="getLevelType(row.level)">
                    {{ getLevelText(row.level) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="时间" width="180" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>

    <!-- 关键词预警测试弹窗 -->
    <el-dialog v-model="keywordDialogVisible" title="测试关键词预警" width="500px">
      <el-form label-width="100px">
        <el-form-item label="关键词">
          <el-select v-model="testKeyword" placeholder="选择关键词">
            <el-option label="诈骗" value="诈骗" />
            <el-option label="火灾" value="火灾" />
            <el-option label="地震" value="地震" />
            <el-option label="台风" value="台风" />
            <el-option label="暴雨" value="暴雨" />
            <el-option label="校园贷" value="校园贷" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="testLevel" placeholder="选择级别">
            <el-option label="信息" value="INFO" />
            <el-option label="警告" value="WARNING" />
            <el-option label="危险" value="DANGER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="keywordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="testKeywordAlert">发送预警</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Document, User, Calendar, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const alertList = ref([])
const hotspotList = ref([])
const loading = ref(false)
const keywordDialogVisible = ref(false)
const hotspotDialogVisible = ref(false)
const activeTab = ref('hotspots')
const testKeyword = ref('诈骗')
const testLevel = ref('WARNING')

const refreshAlerts = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/alert/list')
    if (res.code === 200) {
      alertList.value = res.data
    }
  } catch (e) {
    ElMessage.error('获取预警列表失败')
  } finally {
    loading.value = false
  }
}

const testSeasonalAlert = async () => {
  try {
    await request.post('/api/alert/test-seasonal')
    ElMessage.success('季节性预警已发送！请查看通知')
    refreshAlerts()
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

const showKeywordDialog = () => {
  keywordDialogVisible.value = true
}

const testKeywordAlert = async () => {
  try {
    await request.post('/api/alert/test-keyword', {
      keyword: testKeyword.value,
      level: testLevel.value
    })
    ElMessage.success('关键词预警已发送！请查看通知')
    keywordDialogVisible.value = false
    refreshAlerts()
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

const getLevelType = (level) => {
  switch (level) {
    case 'INFO': return ''
    case 'WARNING': return 'warning'
    case 'DANGER': return 'danger'
    default: return ''
  }
}

const getLevelText = (level) => {
  switch (level) {
    case 'INFO': return '信息'
    case 'WARNING': return '警告'
    case 'DANGER': return '危险'
    default: return level
  }
}

const loadHotspots = async () => {
  try {
    const res = await request.get('/api/alert/hotspots')
    if (res.code === 200) {
      hotspotList.value = res.data
    }
  } catch (e) {
    ElMessage.error('获取热点事件失败')
  }
}

const triggerHotspot = async (index) => {
  try {
    await request.post(`/api/alert/trigger-hotspot/${index}`)
    ElMessage.success('热点预警已发布！请查看通知')
    refreshAlerts()
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

const showHotspotDialog = () => {
  hotspotDialogVisible.value = true
}

onMounted(() => {
  refreshAlerts()
  loadHotspots()
})
</script>

<style scoped>
.alert-center {
  padding: 20px;
}

.header-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.header-content h2 {
  color: white;
  margin: 0 0 10px 0;
  font-size: 28px;
}

.header-content p {
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
  font-size: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.feature-item .el-icon {
  font-size: 28px;
}

.feature-info {
  flex: 1;
}

.feature-title {
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 12px;
  color: #909399;
}

.action-buttons {
  padding: 10px 0;
}
</style>
