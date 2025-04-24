import { createRouter, createWebHistory } from 'vue-router'

import ApplicationsView from '../views/application/ApplicationsView.vue'
import SettingsView from '../views/settings/SettingsView.vue'

import GitSettingsView from '../views/git/GitSettingsView.vue'
import ProjectSettingsView from '../views/settings/ProjectSettingsView.vue'

const routes = [
    { path: '/', redirect: '/applications' },
    { path: '/applications', component: ApplicationsView },
    { path: '/settings', component: SettingsView },
    { path: '/settings/git', component: GitSettingsView },
    { path: '/settings/projects', component: ProjectSettingsView },
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
