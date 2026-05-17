import { createPinia, setActivePinia } from 'pinia'

export function setupPinia() {
  setActivePinia(createPinia())
  localStorage.clear()
}

export const routerStubs = {
  provide: {
    router: {
      push: () => {},
      replace: () => {},
      go: () => {},
    },
    route: {
      path: '/',
      params: {},
      query: {},
    },
  },
}

export const elementStubs = {
  'router-link': true,
  'el-card': { template: '<div class="el-card"><slot name="header"/><slot/></div>' },
  'el-row': { template: '<div><slot/></div>' },
  'el-col': { template: '<div><slot/></div>' },
  'el-form': { template: '<div><slot/></div>' },
  'el-form-item': { template: '<div><slot/></div>' },
  'el-input': { template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"/>', props: ['modelValue'] },
  'el-button': { template: '<button @click="$emit(\'click\')"><slot/></button>' },
  'el-table': true,
  'el-table-column': true,
  'el-tag': true,
  'el-dialog': true,
  'el-select': true,
  'el-option': true,
  'el-date-picker': true,
  'el-pagination': true,
  'el-icon': { template: '<i/>' },
  'el-popconfirm': true,
  'el-descriptions': true,
  'el-descriptions-item': true,
  'el-upload': true,
  'el-tree': true,
  'el-cascader': true,
  'el-tabs': true,
  'el-tab-pane': true,
  'el-dropdown': true,
  'el-dropdown-menu': true,
  'el-dropdown-item': true,
  'el-statistic': true,
  'el-switch': true,
  'el-checkbox': true,
  'el-radio': true,
  'el-radio-group': true,
  'el-input-number': true,
  'el-alert': true,
  'el-breadcrumb': true,
  'el-breadcrumb-item': true,
  'el-divider': true,
  'el-link': true,
  'el-image': true,
  'el-popover': true,
  'el-tooltip': true,
  'el-empty': true,
  'el-result': true,
  'el-skeleton': true,
  'el-scrollbar': true,
  Plus: true,
  Edit: true,
  Delete: true,
  View: true,
  Promotion: true,
  Document: true,
  Select: true,
  CloseBold: true,
}
