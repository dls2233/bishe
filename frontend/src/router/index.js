import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue')
      },
      {
        path: 'course',
        name: 'Course',
        component: () => import('../views/Course.vue')
      },
      {
        path: 'course/:id',
        name: 'CourseDetail',
        component: () => import('../views/CourseDetail.vue')
      },
      {
        path: 'mall',
        name: 'Mall',
        component: () => import('../views/Mall.vue')
      },
      {
        path: 'exam',
        name: 'Exam',
        component: () => import('../views/Exam.vue')
      },
      {
        path: 'exam/:id',
        name: 'ExamDetail',
        component: () => import('../views/ExamDetail.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue')
      },
      {
        path: 'news',
        name: 'News',
        component: () => import('../views/News.vue')
      },
      {
        path: 'news/:id',
        name: 'NewsDetail',
        component: () => import('../views/NewsDetail.vue')
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue')
      },
      {
        path: 'teacher/course/publish',
        name: 'PublishCourse',
        component: () => import('../views/teacher/PublishCourse.vue')
      },
      {
        path: 'teacher/exam/publish',
        name: 'PublishExam',
        component: () => import('../views/teacher/PublishExam.vue')
      },
      {
        path: 'teacher/alert',
        name: 'AlertCenter',
        component: () => import('../views/teacher/AlertCenter.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：如果没有 token 则强制跳转到登录页
router.beforeEach((to, from, next) => {
  const token = store.state.token
  const user = store.state.user

  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else if (to.path.startsWith('/teacher')) {
    if (user.role === 'TEACHER' || user.role === 'ADMIN') {
      next()
    } else {
      next('/')
    }
  } else {
    next()
  }
})

export default router
