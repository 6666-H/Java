import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', name: 'Root', component: { template: '<div></div>' } },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  {
    path: '/admin',
    component: () => import('../views/admin/Layout.vue'),
    meta: { requiresAuth: true, role: 'SUPER_ADMIN' },
    children: [
      { path: '', redirect: '/admin/tenants' },
      { path: 'dashboard', redirect: '/admin/tenants' },
      { path: 'tenants', name: 'AdminTenants', component: () => import('../views/admin/TenantManage.vue') },
      { path: 'content', name: 'AdminContent', component: () => import('../views/admin/BookManage.vue') },
      { path: 'permissions', name: 'AdminPermissions', component: () => import('../views/admin/BookPermission.vue') },
      { path: 'settings', name: 'AdminSettings', component: () => import('../views/admin/SystemSettings.vue') },
      { path: 'sync', name: 'AdminSync', component: () => import('../views/admin/WordSync.vue') }
    ]
  },
  {
    path: '/org',
    component: () => import('../views/org/Layout.vue'),
    meta: { requiresAuth: true, role: 'ORG_ADMIN' },
    children: [
      { path: '', redirect: '/org/dashboard' },
      { path: 'dashboard', name: 'OrgDashboard', component: () => import('../views/org/Dashboard.vue') },
      { path: 'students', name: 'OrgStudents', component: () => import('../views/org/StudentManage.vue') },
      { path: 'tasks', name: 'OrgTasks', component: () => import('../views/org/Tasks.vue') },
      { path: 'reports', name: 'OrgReports', component: () => import('../views/org/LearningProgress.vue') },
      { path: 'settings', name: 'OrgSettings', component: () => import('../views/org/Profile.vue') },
      { path: 'progress', redirect: '/org/reports' },
      { path: 'stats', redirect: '/org/reports' },
      { path: 'profile', redirect: '/org/settings' }
    ]
  },
  {
    path: '/student',
    component: () => import('../views/student/Layout.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      { path: '', redirect: '/student/home' },
      { path: 'home', name: 'StudentHome', component: () => import('../views/student/Home.vue') },
      { path: 'books', name: 'StudentBookSelect', component: () => import('../views/student/BookSelect.vue') },
      { path: 'units/:bookId', name: 'StudentUnitList', component: () => import('../views/student/UnitList.vue') },
      { path: 'unit/:unitId', name: 'StudentUnitDetail', component: () => import('../views/student/UnitDetail.vue') },
      { path: 'review', name: 'StudentReview', component: () => import('../views/student/Review.vue') },
      { path: 'review/:date', name: 'StudentReviewByDate', component: () => import('../views/student/ReviewByDate.vue') },
      { path: 'review-practice/:date', name: 'StudentReviewPractice', component: () => import('../views/student/Study.vue') },
      { path: 'journey/:unitId', name: 'StudentJourneyStudy', component: () => import('../views/student/Journey.vue') },
      { path: 'result/:unitId/:stage', name: 'StudentResult', component: () => import('../views/student/Result.vue') },
      { path: 'wordbook', name: 'StudentWordbook', component: () => import('../views/student/Wordbook.vue') },
      { path: 'notifications', name: 'StudentNotifications', component: () => import('../views/student/Notifications.vue') },
      { path: 'profile', name: 'StudentProfile', component: () => import('../views/student/Profile.vue') },
      { path: 'study/:unitId', name: 'StudentStudy', component: () => import('../views/student/Study.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: import.meta.env.PROD ? createWebHistory('/') : createWebHashHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()
  if (to.path === '/') {
    if (!auth.isLoggedIn) return next('/login')
    if (auth.hasTokenForRole('SUPER_ADMIN')) return next('/admin')
    if (auth.hasTokenForRole('ORG_ADMIN')) return next('/org')
    if (auth.hasTokenForRole('STUDENT')) return next('/student')
    return next('/login')
  }

  if (to.meta.requiresAuth) {
    const needRole = to.meta.role
    if (!needRole || !auth.hasTokenForRole(needRole)) {
      return next('/login')
    }
  }
  if (to.meta.guest && auth.isLoggedIn) {
    return next('/')
  }

  next()
})

export default router
