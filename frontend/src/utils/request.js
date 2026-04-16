import axios from 'axios';
import { ElMessage } from 'element-plus';

// 创建 axios 实例
const service = axios.create({
  baseURL: 'http://localhost:8080', // TODO: 可根据环境变量配置
  timeout: 5000,
});

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token 并添加到 header
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data;
    // 根据后端返回的自定义 code 判断请求是否成功
    if (res.code !== 200) {
      ElMessage({
        message: res.message || 'Error',
        type: 'error',
        duration: 5 * 1000
      });
      // 401 错误，清除本地数据并跳转登录
      if (res.code === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('vuex');
        window.location.href = '/login';
      }
      return Promise.reject(new Error(res.message || 'Error'));
    } else {
      return res;
    }
  },
  error => {
    ElMessage({
      message: error.message,
      type: 'error',
      duration: 5 * 1000
    });
    return Promise.reject(error);
  }
);

export default service;
