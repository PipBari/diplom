import { createRouter, createWebHistory } from 'vue-router'

import ApplicationsView from '../views/application/ApplicationsView.vue'
import ApplicationEditorView from '../views/application/ApplicationEditorView.vue'

import SettingsView from '../views/settings/SettingsView.vue'
import GitSettingsView from '../views/git/GitSettingsView.vue'
import ProjectSettingsView from '../views/settings/ProjectSettingsView.vue'
import ServersSettingsView from '../views/settings/ServersSettingsView.vue'

const routes = [
    { path: '/', redirect: '/applications' },
    { path: '/applications', component: ApplicationsView },
    { path: '/applications/:name', component: ApplicationEditorView },
    { path: '/settings', component: SettingsView },
    { path: '/settings/git', component: GitSettingsView },
    { path: '/settings/projects', component: ProjectSettingsView },
    { path: '/settings/servers', component: ServersSettingsView }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
