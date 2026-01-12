import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
    },
    {
        path: '/activities',
        name: 'Activities',
        component: () => import('../views/Activities.vue')
    },
    {
        path: '/knowledge',
        name: 'Knowledge',
        component: () => import('../views/Knowledge.vue')
    },
    {
        path: '/reports',
        name: 'Reports',
        component: () => import('../views/Reports.vue')
    },
    {
        path: '/sites',
        name: 'Sites',
        component: () => import('../views/Sites.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
