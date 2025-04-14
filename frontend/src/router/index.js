import { createRouter, createWebHistory } from 'vue-router'
import ApplicationsView from '../views/application/ApplicationsView.vue'
import GitSettingsView from '../views/git/GitSettingsView.vue'

const routes = [
    { path: '/', redirect: '/applications' },
    { path: '/applications', component: ApplicationsView },
    { path: '/settings', component: GitSettingsView }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
