<template>
  <div class="chat-container">
    <div class="page-header mb-20">
      <h2 class="page-title">智能安全咨询</h2>
      <el-tag type="primary" size="small" effect="light" round>✨ Powered by DeepSeek R1</el-tag>
    </div>
    
    <el-card class="chat-card" shadow="never">
      <div class="chat-window" ref="chatWindow">
        <!-- 欢迎消息 -->
        <div class="message ai">
          <div class="avatar-container">
            <el-avatar size="default" src="https://img.icons8.com/color/48/bot.png" class="avatar"></el-avatar>
            <span class="bot-name">小游</span>
          </div>
          <div class="bubble">
            您好！我是重游安全教育平台的智能咨询助手“小游”。您可以向我咨询任何关于校园安全、网络安全、防诈骗、消防安全等方面的问题，也可以和我聊聊天哦。
          </div>
        </div>

        <!-- 聊天记录 -->
        <div 
          v-for="(msg, index) in chatHistory" 
          :key="index" 
          :class="['message', msg.role]"
        >
          <template v-if="msg.role === 'system'">
            <div class="system-msg">
              <span v-html="formatMessage(msg.content)"></span>
            </div>
          </template>

          <template v-else>
            <div class="avatar-container" v-if="msg.role === 'ai'">
              <el-avatar 
                size="default" 
                src="https://img.icons8.com/color/48/bot.png" 
                class="avatar"
              ></el-avatar>
              <span class="bot-name">小游</span>
            </div>
            
            <div class="bubble" v-html="formatMessage(msg.content)"></div>
            
            <el-avatar 
              v-if="msg.role === 'user'" 
              size="default" 
              :src="userAvatar" 
              icon="el-icon-user-solid" 
              class="avatar"
            ></el-avatar>
          </template>
        </div>
        
        <!-- Loading 提示 -->
        <div class="message ai" v-if="loading">
          <div class="avatar-container">
            <el-avatar size="default" src="https://img.icons8.com/color/48/bot.png" class="avatar"></el-avatar>
            <span class="bot-name">小游</span>
          </div>
          <div class="bubble loading-bubble">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          :placeholder="isLimitReached ? '已达到最大对话上限，请清空历史记录' : '请输入您的问题，按 Enter 发送 (Shift + Enter 换行)...'"
          @keydown.enter.prevent="handleEnter"
          resize="none"
          :disabled="isLimitReached"
        ></el-input>
        <div class="actions">
          <span class="tips">由于使用的是免费模型 (DeepSeek R1)，回复可能需要几秒钟时间，请耐心等待。</span>
          <div class="action-btns">
            <el-button type="danger" plain @click="clearHistory" v-if="chatHistory.length > 0">开启新会话</el-button>
            <el-button type="primary" :loading="loading" @click="sendMessage" :disabled="!inputText.trim() || isLimitReached">发送咨询</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const store = useStore()
const chatWindow = ref(null)
const inputText = ref('')
const loading = ref(false)

const chatHistory = ref([])
const MAX_CONVERSATIONS = 100

// 监听是否达到上限
const isLimitReached = computed(() => {
  return chatHistory.value.filter(msg => msg.role === 'user').length >= MAX_CONVERSATIONS
})

// 组件加载时读取本地历史记录
onMounted(() => {
  const saved = localStorage.getItem('chat_history')
  if (saved) {
    try {
      chatHistory.value = JSON.parse(saved)
      scrollToBottom()
    } catch (e) {
      console.error('解析历史记录失败:', e)
    }
  }
})

// 监听聊天记录变化，并持久化到 localStorage
watch(chatHistory, (newVal) => {
  localStorage.setItem('chat_history', JSON.stringify(newVal))
}, { deep: true })

const userAvatar = computed(() => {
  return store.state.user.avatarUrl 
    ? `http://localhost:8080${store.state.user.avatarUrl}` 
    : 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
})

// 处理 Enter 发送
const handleEnter = (e) => {
  if (e.shiftKey) {
    inputText.value += '\n'
    return
  }
  sendMessage()
}

// 格式化消息内容（换行）
const formatMessage = (content) => {
  if (!content) return ''
  // 隐藏自动触发的 action 代码块，不给用户展示
  let displayContent = content.replace(/```action[\s\S]*?```/g, '')
  return displayContent.replace(/\n/g, '<br>')
}

// 滚动到最底部
const scrollToBottom = async () => {
  await nextTick()
  if (chatWindow.value) {
    chatWindow.value.scrollTop = chatWindow.value.scrollHeight
  }
}

// 清空对话
const clearHistory = () => {
  chatHistory.value = []
  localStorage.removeItem('chat_history')
  inputText.value = ''
  ElMessage.success('已开启新会话，可开始提问。')
}

// 发送消息
const sendMessage = async () => {
  if (isLimitReached.value) {
    ElMessage.warning('已达到最大对话上限，请清空历史记录。')
    return
  }

  const content = inputText.value.trim()
  if (!content) return
  
  // 1. 追加用户消息
  chatHistory.value.push({ role: 'user', content })
  inputText.value = ''
  scrollToBottom()
  
  // 2. 设置 loading 状态
  loading.value = true
  let aiMessageIndex = -1
  scrollToBottom()
  
  // 构建带上下文的消息列表
  const messagesToSend = chatHistory.value
    .filter(msg => msg.role !== 'system' && msg.content)
    .map(msg => ({
      role: msg.role === 'ai' ? 'assistant' : msg.role,
      content: msg.content
    }))

  try {
    const token = localStorage.getItem('token') || ''
    const response = await fetch('/api/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ 
        message: content, // 兼容旧接口
        messages: messagesToSend // 新增上下文参数
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    loading.value = false // 收到第一个字节就关闭 loading 动画
    chatHistory.value.push({ role: 'ai', content: '' })
    aiMessageIndex = chatHistory.value.length - 1
    
    // 处理流式响应 (SSE)
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      buffer += decoder.decode(value, { stream: true })
      
      // 按行处理 SSE 数据格式: data: {...}
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 最后一行可能不完整，留到下一次处理
      
      for (const line of lines) {
        const trimmedLine = line.trim()
        if (trimmedLine.startsWith('data: ')) {
          const dataStr = trimmedLine.slice(6)
          if (dataStr === '[DONE]') {
            continue
          }
          try {
            const dataObj = JSON.parse(dataStr)
            if (dataObj.choices && dataObj.choices[0].delta && dataObj.choices[0].delta.content) {
              // 将增量的文字拼接到 AI 消息的内容中
              chatHistory.value[aiMessageIndex].content += dataObj.choices[0].delta.content
              scrollToBottom()
            }
          } catch (e) {
            console.error('JSON 解析失败:', e, dataStr)
          }
        }
      }
    }
  } catch (error) {
    loading.value = false
    console.error('AI Consultation Error:', error)
    if (aiMessageIndex === -1) {
      chatHistory.value.push({ role: 'ai', content: '很抱歉，我现在遇到了一点问题，请稍后再试。' })
    } else {
      chatHistory.value[aiMessageIndex].content = '很抱歉，我现在遇到了一点问题，请稍后再试。'
    }
  } finally {
    loading.value = false
    scrollToBottom()

    // 检查并执行 AI 的自动化 Action 指令
    const finalContent = chatHistory.value[aiMessageIndex].content;
    const actionMatch = finalContent.match(/```action\s*(\{[\s\S]*?\})\s*```/);
    if (actionMatch) {
      try {
        let jsonStr = actionMatch[1];
        // 尝试修复由于大模型生成长文本导致的常见 JSON 截断或未转义错误
        jsonStr = jsonStr.replace(/\\n/g, "\\n")
                         .replace(/\\'/g, "\\'")
                         .replace(/\\"/g, '\\"')
                         .replace(/\\&/g, "\\&")
                         .replace(/\\r/g, "\\r")
                         .replace(/\\t/g, "\\t")
                         .replace(/\\b/g, "\\b")
                         .replace(/\\f/g, "\\f");
        
        const actionData = JSON.parse(jsonStr);
        if (actionData.action === 'publish_course') {
          // 不再显示右上角的弹窗提示
          
          // 将 quizList 转换为后端需要的 JSON 字符串格式
          if (actionData.payload.quizList) {
            actionData.payload.quiz = JSON.stringify(actionData.payload.quizList);
            delete actionData.payload.quizList;
          }
          const needCategory = !actionData.payload.category || actionData.payload.category.trim() === '';
          const needTitle = !actionData.payload.title || actionData.payload.title.trim() === '';
          if (needCategory || needTitle) {
            const missingTips = [];
            if (needTitle) missingTips.push('课程标题');
            if (needCategory) missingTips.push('课程主题/分类');
            chatHistory.value[aiMessageIndex].content += `\n\n⚠️ 发布课程需要${missingTips.join('、')}，请补充后我再继续创建。`;
            return;
          }
          if (!actionData.payload.coverUrl || actionData.payload.coverUrl.trim() === '' || actionData.payload.coverUrl.includes('source.unsplash.com')) {
            // 如果 AI 没有返回图片，或者返回了不可用的图床，使用稳定图床兜底
            const defaultImages = [
              'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80', // 食品安全
              'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=800&q=80', // 网络安全
              'https://images.unsplash.com/photo-1473655584838-8924b45d2903?auto=format&fit=crop&w=800&q=80', // 消防安全
              'https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=800&q=80'  // 实验室安全
            ];
            actionData.payload.coverUrl = defaultImages[Math.floor(Math.random() * defaultImages.length)];
          }
          
          const res = await request.post('/api/course/publish', actionData.payload);
          if (res.code === 200) {
            // 从生成的内容中提取前100个字符作为摘要，如果不足100则取全部
            let summary = actionData.payload.content || '';
            summary = summary.replace(/<[^>]+>/g, ''); // 简单去除HTML标签（如果模型生成了富文本）
            summary = summary.substring(0, 150) + (summary.length > 150 ? '...' : '');

            const successFlag = `课程《${actionData.payload.title}》发布成功`;
            if (!chatHistory.value[aiMessageIndex].content.includes(successFlag)) {
              chatHistory.value[aiMessageIndex].content += `\n\n✅ **课程《${actionData.payload.title}》发布成功！**\n\n` +
                         `**【分类】** ${actionData.payload.category}\n` +
                         `**【内容摘要】**\n${summary}\n\n` +
                         `**【内容统计】**\n已生成详尽课程内容，并配套了 ${actionData.payload.quiz ? JSON.parse(actionData.payload.quiz).length : 0} 道随堂测验。\n\n` +
                         `**【奖励积分】** ${actionData.payload.rewardPoints} 积分\n\n` +
                         `👉 教师可前往“安全课程”列表查看详细内容。`;
            }

          } else {
            chatHistory.value[aiMessageIndex].content += `\n\n❌ 课程发布失败：${res.message}`;
          }
        } else if (actionData.action === 'publish_exam') {
          // 不再显示右上角的弹窗提示
          
          // 将 optionsList 转换为后端需要的 JSON 字符串格式
          if (actionData.payload.questions) {
            actionData.payload.questions.forEach(q => {
              if (q.optionsList) {
                q.options = JSON.stringify(q.optionsList);
                delete q.optionsList;
              }
            });
          }
          
          const res = await request.post('/api/exam/publish', actionData.payload);
          if (res.code === 200) {
            chatHistory.value[aiMessageIndex].content += `\n\n✅ **测评《${actionData.payload.exam.title}》发布成功！**\n\n` +
                       `**【测评说明】**\n${actionData.payload.exam.description}\n\n` +
                       `**【题目数量】**\n已自动生成并导入 ${actionData.payload.questions ? actionData.payload.questions.length : 0} 道测试题。\n\n` +
                       `**【时间与分数】**\n答题限时 ${actionData.payload.exam.timeLimit} 分钟，及格线为 ${actionData.payload.exam.passScore} 分（总分 ${actionData.payload.exam.totalScore} 分）。\n\n` +
                       `现在学生可以在“在线测评”列表中看到并参与该测试了！`
            
          } else {
            chatHistory.value[aiMessageIndex].content += `\n\n❌ 测评发布失败：${res.message}`;
          }
        }
        scrollToBottom();
      } catch (e) {
        console.error("Action 解析执行失败:", e);
      }
    }

    // 达到上限时，确保在对话末尾添加一次系统提示
    if (isLimitReached.value) {
      const lastMsg = chatHistory.value[chatHistory.value.length - 1]
      if (lastMsg && lastMsg.role !== 'system') {
        chatHistory.value.push({ 
          role: 'system', 
          content: `<span style="color: #e6a23c; font-weight: bold;">⚠️ 提示</span><br>您已达到本次对话上限（${MAX_CONVERSATIONS}条），为了保证优质体验，请清空对话历史以重新开始。` 
        })
        scrollToBottom();
      }
    }
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mb-20 {
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  border-left: 4px solid #409eff;
  padding-left: 12px;
}

.chat-card {
  border-radius: 12px;
  height: calc(100vh - 85px); /* 进一步拉长聊天框 */
  display: flex;
  flex-direction: column;
}

:deep(.el-card__body) {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-window {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f7f9fc;
}

.message {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-start;
}

.message.user {
  justify-content: flex-end;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 0 12px;
  flex-shrink: 0;
  width: 50px;
}

.bot-name {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  font-weight: 500;
}

.avatar {
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border: 2px solid #fff;
}

.message.user .avatar {
  margin: 0 12px;
}

.bubble {
  max-width: 70%;
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  position: relative;
}

.message.ai .bubble {
  background-color: #ffffff;
  color: #333333;
  border-top-left-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-top: 6px;
}

.message.user .bubble {
  background-color: #409EFF;
  color: #ffffff;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.input-area {
  padding: 20px;
  background-color: #ffffff;
  border-top: 1px solid #ebeef5;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
}

.action-btns {
  display: flex;
  gap: 10px;
}

.message.system {
  justify-content: center;
  margin-bottom: 24px;
}

.system-msg {
  background-color: #f4f4f5;
  color: #606266;
  padding: 16px 20px;
  border-radius: 12px;
  font-size: 14px;
  text-align: left;
  line-height: 1.6;
  max-width: 80%;
  border-left: 4px solid #67c23a;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.tips {
  font-size: 12px;
  color: #909399;
}

/* Loading 动画 */
.loading-bubble {
  display: flex;
  align-items: center;
  height: 46px;
}

.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin: 0 3px;
  background-color: #909399;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
