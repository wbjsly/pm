import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'Layout',
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '主页', closable: false }
      },
      {
        path: '/pm/charter',
        name: 'CharterList',
        component: () => import('@/views/pm/charter/index.vue'),
        meta: { title: '项目立项', group: '项目管理', perm: 'ROLE_PM' }
      },
      {
        path: '/pm/charter/detail/:id',
        name: 'CharterDetail',
        component: () => import('@/views/pm/charter/detail.vue'),
        meta: { title: '项目详情', hidden: true }
      },
      {
        path: '/pm/charter/form',
        name: 'CharterForm',
        component: () => import('@/views/pm/charter/form.vue'),
        meta: { title: '新增项目', hidden: true }
      },
      {
        path: '/pm/charter/form/:id',
        name: 'CharterEdit',
        component: () => import('@/views/pm/charter/form.vue'),
        meta: { title: '编辑项目', hidden: true }
      },
      // WBS Management routes
      {
        path: '/pm/wbs',
        name: 'WbsList',
        component: () => import('@/views/pm/wbs/index.vue'),
        meta: { title: '项目任务', group: '项目管理', perm: 'ROLE_PM' }
      },
      {
        path: '/pm/wbs/detail/:id',
        name: 'WbsDetail',
        component: () => import('@/views/pm/wbs/detail.vue'),
        meta: { title: '任务详情', hidden: true }
      },
      {
        path: '/pm/wbs/form',
        name: 'WbsForm',
        component: () => import('@/views/pm/wbs/form.vue'),
        meta: { title: '新增任务', hidden: true }
      },
      {
        path: '/pm/wbs/form/:id',
        name: 'WbsEdit',
        component: () => import('@/views/pm/wbs/form.vue'),
        meta: { title: '编辑任务', hidden: true }
      },
      {
        path: '/pm/wbs/history/:id',
        name: 'WbsHistory',
        component: () => import('@/views/pm/wbs/history.vue'),
        meta: { title: '版本历史', hidden: true }
      },
      {
        path: '/pm/product',
        name: 'ProductList',
        component: () => import('@/views/pm/product/index.vue'),
        meta: { title: '产品清单', group: '系统管理', perm: 'ROLE_PM' }
      },
      // Deliverable Management routes
      {
        path: '/pm/deliverable',
        name: 'DeliverableList',
        component: () => import('@/views/pm/deliverable/index.vue'),
        meta: { title: '成果管理', group: '项目管理', perm: 'ROLE_PM,ROLE_SPONSOR' }
      },
      {
        path: '/pm/deliverable/detail/:id',
        name: 'DeliverableDetail',
        component: () => import('@/views/pm/deliverable/detail.vue'),
        meta: { title: '成果物详情', hidden: true }
      },
      {
        path: '/pm/deliverable/form',
        name: 'DeliverableForm',
        component: () => import('@/views/pm/deliverable/form.vue'),
        meta: { title: '新增成果物', hidden: true }
      },
      {
        path: '/pm/deliverable/form/:id',
        name: 'DeliverableEdit',
        component: () => import('@/views/pm/deliverable/form.vue'),
        meta: { title: '编辑成果物', hidden: true }
      },
      // Budget Management routes
      {
        path: '/pm/budget',
        name: 'BudgetList',
        component: () => import('@/views/pm/budget/index.vue'),
        meta: { title: '预算管理', group: '项目管理', perm: 'ROLE_PM' }
      },
      {
        path: '/pm/budget/form',
        name: 'BudgetForm',
        component: () => import('@/views/pm/budget/form.vue'),
        meta: { title: '新增预算', hidden: true }
      },
      {
        path: '/pm/budget/form/:id',
        name: 'BudgetEdit',
        component: () => import('@/views/pm/budget/form.vue'),
        meta: { title: '编辑预算', hidden: true }
      },
      {
        path: '/pm/budget/detail/:id',
        name: 'BudgetDetail',
        component: () => import('@/views/pm/budget/detail.vue'),
        meta: { title: '查看预算', hidden: true }
      },
      {
        path: '/pm/budget/upgrade/:id',
        name: 'BudgetUpgrade',
        component: () => import('@/views/pm/budget/upgrade.vue'),
        meta: { title: '升级预算', hidden: true }
      },
      {
        path: '/pm/budget/comparison/:projectId',
        name: 'BudgetComparison',
        component: () => import('@/views/pm/budget/comparison.vue'),
        meta: { title: '预实对比', hidden: true }
      },
      {
        path: '/pm/work-hours',
        name: 'WorkHours',
        component: () => import('@/views/pm/work-hours/index.vue'),
        meta: { title: '工时管理', group: '项目管理', perm: 'ROLE_PM' }
      },
      {
        path: '/pm/work-hours/approval',
        name: 'WorkHoursApproval',
        component: () => import('@/views/pm/work-hours/approval.vue'),
        meta: { title: '工时审批', hidden: true }
      },
      {
        path: '/system/calendar',
        name: 'WorkCalendar',
        component: () => import('@/views/system/calendar/index.vue'),
        meta: { title: '工作日历', group: '系统管理', perm: 'ROLE_ADMIN' }
      },
      {
        path: '/system/cost-quota',
        name: 'CostQuota',
        component: () => import('@/views/system/cost-quota/index.vue'),
        meta: { title: '成本定额', group: '系统管理', perm: 'ROLE_ADMIN,ROLE_PM' }
      },
      {
        path: '/system/dict',
        name: 'DictList',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', group: '系统管理', perm: 'ROLE_ADMIN' }
      },
      {
        path: '/system/menu',
        name: 'MenuManagement',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', group: '系统管理', perm: 'ROLE_ADMIN' }
      },
      {
        path: '/system/user',
        name: 'UserList',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', group: '系统管理', perm: 'ROLE_ADMIN' }
      },
      {
        path: '/system/user/detail/:id',
        name: 'UserDetail',
        component: () => import('@/views/system/user/detail.vue'),
        meta: { title: '用户详情', hidden: true }
      },
      {
        path: '/system/user/edit/:id',
        name: 'UserEdit',
        component: () => import('@/views/system/user/edit.vue'),
        meta: { title: '编辑用户', hidden: true }
      },
      {
        path: '/system/user/profile',
        name: 'UserProfile',
        component: () => import('@/views/system/user/profile.vue'),
        meta: { title: '个人资料', hidden: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title || 'WH管理系统'

  if (to.path === '/login') {
    next()
    return
  }

  const userStore = (await import('@/store/user')).useUserStore()
  if (!userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (!userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch {
      next({ path: '/login' })
      return
    }
  }

  // Load dictionary data on first navigation after login
  const dictStore = (await import('@/store/dict')).useDictStore()
  if (!dictStore.loaded) {
    dictStore.loadAll().catch(() => {})
  }

  // Load menu data after user info is available
  const menuStore = (await import('@/store/menu')).useMenuStore()
  if (!menuStore.loaded && !menuStore.loading) {
    menuStore.fetchMenus().catch(() => {})
  }

  // Add tab
  const tabStore = (await import('@/store/tab')).useTabStore()
  tabStore.addTab(to)

  next()
})

export default router
