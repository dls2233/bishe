import { createStore } from 'vuex'
import request from '../utils/request'

export default createStore({
  state: {
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || '{}')
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    SET_USER(state, user) {
      state.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    LOGOUT(state) {
      state.token = ''
      state.user = {}
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  },
  actions: {
    async login({ commit, dispatch }, userInfo) {
      const res = await request.post('/api/user/login', userInfo)
      // 注意：后端的 Result 包装体里，真正的 token 是在 res.data 里面
      commit('SET_TOKEN', res.data)
      
      // 获取并设置完整的用户信息（包含积分）
      await dispatch('fetchUserInfo')
      return res
    },
    async register(_, userInfo) {
      return await request.post('/api/user/register', userInfo)
    },
    async fetchUserInfo({ commit }) {
      try {
        const res = await request.get('/api/user/info')
        commit('SET_USER', res.data)
        return res.data
      } catch (error) {
        console.error('获取用户信息失败', error)
      }
    }
  },
  modules: {
  }
})
