<template>
  <div class="sidebar-menu" :class="{ collapsed: props.collapsed }">
    <div class="sidebar-header">
      <div class="sidebar-logo" :class="{ collapsed: props.collapsed }">
        <span v-show="!props.collapsed">WH管理系统</span>
        <span v-show="props.collapsed">WH</span>
      </div>
      <el-icon class="collapse-btn" @click="toggleCollapse">
        <Fold v-if="!props.collapsed" />
        <Expand v-else />
      </el-icon>
    </div>
    <el-input
      v-show="!props.collapsed"
      v-model="searchText"
      placeholder="搜索菜单"
      clearable
      size="small"
      :prefix-icon="Search"
      class="sidebar-search"
    />
    <el-menu
      :default-active="activeMenu"
      router
      :collapse="props.collapsed"
      :collapse-transition="false"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
    >
      <el-menu-item index="/dashboard">
        <el-icon><component :is="'HomeFilled'" /></el-icon>
        <span>主页</span>
      </el-menu-item>
      <template v-for="group in filteredGroups" :key="group.name">
        <el-sub-menu v-if="group.children.length" :index="group.name">
          <template #title>
            <el-icon><component :is="group.icon || 'Folder'" /></el-icon>
            <span>{{ group.name }}</span>
          </template>
          <el-menu-item
            v-for="item in group.children"
            :key="item.path"
            :index="item.path"
          >
            {{ item.title }}
          </el-menu-item>
        </el-sub-menu>
      </template>
      <div v-if="filteredGroups.length === 0 && !props.collapsed" class="no-result">无匹配菜单</div>
    </el-menu>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Search, Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

const props = defineProps({
  collapsed: { type: Boolean, default: false }
})

const emit = defineEmits(['update:collapsed'])

const searchText = ref('')

function toggleCollapse() {
  emit('update:collapsed', !props.collapsed)
}

const activeMenu = computed(() => {
  const r = route.matched[route.matched.length - 1]
  return r?.path || route.path
})

const menuItems = [
  { path: '/pm/charter', title: '项目立项', group: '项目管理', icon: 'Document', perm: 'ROLE_PM' },
  { path: '/pm/wbs', title: '项目任务', group: '项目管理', icon: 'List', perm: 'ROLE_PM' },
  { path: '/pm/budget', title: '预算管理', group: '项目管理', icon: 'Money', perm: 'ROLE_PM' },
  { path: '/pm/work-hours', title: '工时管理', group: '项目管理', icon: 'Clock', perm: 'ROLE_PM' },
  { path: '/pm/product', title: '产品清单', group: '产品管理', icon: 'Tickets', perm: 'ROLE_PM' },
  { path: '/system/user', title: '用户管理', group: '系统管理', icon: 'User', perm: 'ROLE_ADMIN' },
  { path: '/system/calendar', title: '工作日历', group: '系统管理', icon: 'Calendar', perm: 'ROLE_ADMIN' },
]

const visibleItems = computed(() => {
  const roles = userStore.userInfo?.roles || []
  const isAdmin = roles.includes('ROLE_ADMIN')
  return menuItems.filter(item => {
    if (item.hidden) return false
    if (!isAdmin && item.perm) {
      const permCode = item.perm.toLowerCase().replace('role_', '')
      if (!roles.some(r => r.toLowerCase().includes(permCode))) return false
    }
    return true
  })
})

const menuGroups = computed(() => {
  const groups = {}
  for (const item of visibleItems.value) {
    const g = item.group || '其他'
    if (!groups[g]) groups[g] = { name: g, icon: item.icon, children: [] }
    groups[g].children.push({ path: item.path, title: item.title })
  }
  return Object.values(groups)
})

const filteredGroups = computed(() => {
  if (!searchText.value) return menuGroups.value
  const keyword = searchText.value.toLowerCase()
  return menuGroups.value.map(group => ({
    ...group,
    children: group.children.filter(item => item.title.toLowerCase().includes(keyword))
  })).filter(group => group.children.length > 0)
})
</script>

<style scoped>
.sidebar-menu {
  height: 100%;
  overflow-y: auto;
  transition: width 0.3s;
}
.sidebar-header {
  display: flex;
  align-items: center;
  background: #263445;
  height: 50px;
}
.sidebar-logo {
  flex: 1;
  line-height: 50px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  transition: font-size 0.3s;
}
.sidebar-logo.collapsed {
  font-size: 14px;
}
.collapse-btn {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bfcbd9;
  cursor: pointer;
  font-size: 18px;
  flex-shrink: 0;
}
.collapse-btn:hover {
  color: #409EFF;
}
.sidebar-search {
  margin: 8px 12px;
  max-width: calc(100% - 24px);
}
.sidebar-search :deep(.el-input__wrapper) {
  background-color: #263445;
}
.sidebar-search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409EFF inset;
}
.no-result {
  text-align: center;
  color: #bfcbd9;
  padding: 16px;
  font-size: 13px;
}
:deep(.el-sub-menu .el-menu-item) {
  padding-left: 50px !important;
}
</style>
