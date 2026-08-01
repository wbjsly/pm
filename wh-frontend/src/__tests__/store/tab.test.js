import { describe, it, expect, beforeEach } from 'vitest'
import { setupPinia } from '../helpers'
import { useTabStore } from '@/store/tab'

describe('useTabStore', () => {
  beforeEach(() => {
    setupPinia()
  })

  it('初始状态包含主页标签', () => {
    const store = useTabStore()
    expect(store.tabs).toHaveLength(1)
    expect(store.tabs[0]).toMatchObject({
      name: 'Dashboard',
      path: '/dashboard',
      title: '主页',
      closable: false,
    })
    expect(store.activeTab).toBe('/dashboard')
  })

  it('addTab 添加新标签', () => {
    const store = useTabStore()
    store.addTab({ name: 'Charter', path: '/pm/charter', meta: { title: '项目立项' } })

    expect(store.tabs).toHaveLength(2)
    expect(store.tabs[1]).toMatchObject({ name: 'Charter', path: '/pm/charter', title: '项目立项' })
    expect(store.activeTab).toBe('/pm/charter')
  })

  it('addTab 更新已有标签的标题', () => {
    const store = useTabStore()
    store.addTab({ name: 'Charter', path: '/pm/charter', meta: { title: '项目立项' } })
    store.addTab({ name: 'Charter', path: '/pm/charter', meta: { title: '立项管理' } })

    expect(store.tabs).toHaveLength(2)
    expect(store.tabs[1].title).toBe('立项管理')
  })

  it('addTab 对隐藏路由仅更新 activeTab，不添加新标签', () => {
    const store = useTabStore()
    store.addTab({ name: 'Detail', path: '/pm/charter/detail/1', meta: { hidden: true, title: '详情' } })

    expect(store.tabs).toHaveLength(1)
    expect(store.activeTab).toBe('/pm/charter/detail/1')
  })

  it('addTab dashboard 始终在首位', () => {
    const store = useTabStore()
    store.addTab({ name: 'Charter', path: '/pm/charter', meta: { title: '项目立项' } })
    store.addTab({ name: 'Budget', path: '/pm/budget', meta: { title: '预算管理' } })

    expect(store.tabs[0].path).toBe('/dashboard')
    expect(store.tabs[1].path).toBe('/pm/charter')
    expect(store.tabs[2].path).toBe('/pm/budget')

    store.addTab({ name: 'Dashboard', path: '/dashboard', meta: { title: '主页', closable: false } })
    expect(store.tabs[0].path).toBe('/dashboard')
  })

  it('removeTab 移除指定标签', () => {
    const store = useTabStore()
    store.addTab({ name: 'Charter', path: '/pm/charter', meta: { title: '项目立项' } })
    expect(store.tabs).toHaveLength(2)

    store.removeTab('/pm/charter')
    expect(store.tabs).toHaveLength(1)
  })

  it('removeTab 不能移除不可关闭的标签（Dashboard）', () => {
    const store = useTabStore()
    store.removeTab('/dashboard')
    expect(store.tabs).toHaveLength(1)
  })

  it('removeTab 当 activeTab 被移除时激活下一个标签', () => {
    const store = useTabStore()
    store.addTab({ name: 'Charter', path: '/pm/charter', meta: { title: '项目立项' } })
    store.addTab({ name: 'Budget', path: '/pm/budget', meta: { title: '预算管理' } })
    store.activeTab = '/pm/charter'

    store.removeTab('/pm/charter')
    expect(store.activeTab).toBe('/pm/budget')
  })

  it('closeLeft 关闭当前标签左侧的可关闭标签', () => {
    const store = useTabStore()
    store.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    store.addTab({ name: 'B', path: '/b', meta: { title: 'B' } })
    store.addTab({ name: 'C', path: '/c', meta: { title: 'C' } })

    store.closeLeft('/b')
    expect(store.tabs.map(t => t.path)).toEqual(['/dashboard', '/b', '/c'])
  })

  it('closeRight 关闭当前标签右侧的可关闭标签', () => {
    const store = useTabStore()
    store.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    store.addTab({ name: 'B', path: '/b', meta: { title: 'B' } })
    store.addTab({ name: 'C', path: '/c', meta: { title: 'C' } })

    store.closeRight('/a')
    expect(store.tabs.map(t => t.path)).toEqual(['/dashboard', '/a'])
  })

  it('closeOther 仅保留指定标签和不可关闭标签', () => {
    const store = useTabStore()
    store.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    store.addTab({ name: 'B', path: '/b', meta: { title: 'B' } })
    store.addTab({ name: 'C', path: '/c', meta: { title: 'C' } })

    store.closeOther('/b')
    expect(store.tabs.map(t => t.path)).toEqual(['/dashboard', '/b'])
    expect(store.activeTab).toBe('/b')
  })

  it('closeAll 仅保留不可关闭标签', () => {
    const store = useTabStore()
    store.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    store.addTab({ name: 'B', path: '/b', meta: { title: 'B' } })

    store.closeAll()
    expect(store.tabs).toHaveLength(1)
    expect(store.tabs[0].path).toBe('/dashboard')
    expect(store.activeTab).toBe('/dashboard')
  })

  it('closeAll 无标签时 activeTab 为空', () => {
    const store = useTabStore()
    store.tabs = []

    store.closeAll()
    expect(store.tabs).toHaveLength(0)
    expect(store.activeTab).toBe('')
  })
})

describe('useTabStore 边界', () => {
  beforeEach(() => setupPinia())

  it('addTab 无 meta 时 title 回退到 name', () => {
    const store = useTabStore()
    store.addTab({ name: 'NoMeta', path: '/no-meta', meta: {} })
    const tab = store.tabs.find(t => t.path === '/no-meta')
    expect(tab.title).toBe('NoMeta')
    expect(tab.closable).toBe(true)
  })

  it('addTab closable 为 false 时保持不可关闭', () => {
    const store = useTabStore()
    store.addTab({ name: 'Fixed', path: '/fixed', meta: { title: '固定', closable: false } })
    const tab = store.tabs.find(t => t.path === '/fixed')
    expect(tab.closable).toBe(false)
  })

  it('removeTab 不存在的路径无副作用', () => {
    const store = useTabStore()
    const before = store.tabs.length
    store.removeTab('/not-exist')
    expect(store.tabs.length).toBe(before)
  })

  it('removeTab 移除最后一个激活标签时激活前一个', () => {
    const store = useTabStore()
    store.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    store.addTab({ name: 'B', path: '/b', meta: { title: 'B' } })
    store.activeTab = '/b'
    store.removeTab('/b')
    expect(store.activeTab).toBe('/a')
  })

  it('closeLeft 路径不存在时无副作用', () => {
    const store = useTabStore()
    store.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    const before = store.tabs.length
    store.closeLeft('/not-exist')
    expect(store.tabs.length).toBe(before)
  })

  it('closeRight 路径不存在时无副作用', () => {
    const store = useTabStore()
    store.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    const before = store.tabs.length
    store.closeRight('/not-exist')
    expect(store.tabs.length).toBe(before)
  })

  it('closeOther 路径不存在时无副作用', () => {
    const store = useTabStore()
    store.addTab({ name: 'A', path: '/a', meta: { title: 'A' } })
    const before = store.tabs.length
    store.closeOther('/not-exist')
    expect(store.tabs.length).toBe(before)
  })
})
