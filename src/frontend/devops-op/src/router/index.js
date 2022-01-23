import Vue from 'vue'
import Router from 'vue-router'
export const TITLE_HOME = sidebarTitle
export const ROUTER_NAME_SERVICE = 'Service'
export const ROUTER_NAME_INSTANCE = 'Instance'
export const ROUTER_NAME_STORAGE_CREDENTIALS = 'StorageCredentials'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'
import { sidebarTitle } from '@/settings'

/**
 * Note: sub-menu only appear when route children.length >= 1
 * Detail see: https://panjiachen.github.io/vue-element-admin-site/guide/essentials/router-and-nav.html
 *
 * hidden: true                   if set true, item will not show in the sidebar(default is false)
 * alwaysShow: true               if set true, will always show the root menu
 *                                if not set alwaysShow, when item has more than one children route,
 *                                it will becomes nested mode, otherwise not show the root menu
 * redirect: noRedirect           if set noRedirect will no redirect in the breadcrumb
 * name:'router-name'             the name is used by <keep-alive> (must set!!!)
 * meta : {
    roles: ['admin','editor']    control the page roles (you can set multiple roles)
    title: 'title'               the name show in sidebar and breadcrumb (recommend set)
    icon: 'svg-name'/'el-icon-x' the icon show in the sidebar
    breadcrumb: false            if set false, the item will hidden in breadcrumb(default is true)
    activeMenu: '/example/list'  if set path, the sidebar will highlight the path you set
  }
 */

/**
 * constantRoutes
 * a base page that does not have permission requirements
 * all roles can be accessed
 */
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },

  {
    path: '/',
    redirect: '/services',
    meta: { title: TITLE_HOME, icon: 'bk' }
  },

  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  }
]

/**
 * asyncRoutes
 * the routes that need to be dynamically loaded based on user roles
 */
export const asyncRoutes = [
  {
    path: '/services',
    component: Layout,
    children: [
      {
        path: '/',
        name: ROUTER_NAME_SERVICE,
        meta: { title: '服务管理', icon: 'service' },
        component: () => import('@/views/service/index')
      },
      {
        path: ':serviceName/instances',
        name: ROUTER_NAME_INSTANCE,
        hidden: true,
        component: () => import('@/views/service/Instance'),
        props: true,
        meta: { title: '服务实例' }
      }
    ]
  },
  {
    path: '/storage',
    alwaysShow: true,
    redirect: '/storage/credentials',
    component: Layout,
    meta: { title: '存储管理', icon: 'storage' },
    children: [
      {
        path: 'credentials',
        name: ROUTER_NAME_STORAGE_CREDENTIALS,
        component: () => import('@/views/storage/Credential'),
        meta: { title: '凭据', icon: 'credentials' }
      }
    ]
  },
  // 404 page must be placed at the end !!!
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  mode: 'history', // require service support
  base: `/${process.env.VUE_APP_BASE_DIR}`,
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

// Detail see: https://github.com/vuejs/vue-router/issues/1234#issuecomment-357941465
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

export default router