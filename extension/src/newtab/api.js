import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8091/api',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// Activities API
export const activitiesApi = {
    list: (params = {}) => api.get('/activities', { params }),
    get: (id) => api.get(`/activities/${id}`),
    delete: (id) => api.delete(`/activities/${id}`),
    batchDelete: (ids) => api.post('/activities/batch/delete', ids),
    batchAnalyze: (ids) => api.post('/activities/batch/analyze', ids),
    batchBoost: (ids) => api.post('/activities/batch/boost', ids),
    stats: () => api.get('/activities/stats')
}

// Knowledge API
export const knowledgeApi = {
    list: (params = {}) => api.get('/knowledge', { params }),
    categories: () => api.get('/knowledge/categories'),
    get: (id) => api.get(`/knowledge/${id}`),
    update: (id, data) => api.put(`/knowledge/${id}`, data),
    delete: (id) => api.delete(`/knowledge/${id}`),
    stats: () => api.get('/knowledge/stats')
}

// Analysis API
export const analysisApi = {
    trigger: () => api.post('/analysis/trigger'),
    report: (date) => api.get('/analysis/report', { params: { date } })
}

// Sites API
export const sitesApi = {
    list: (sortBy = 'visits') => api.get('/sites', { params: { sortBy } }),
    knowledge: () => api.get('/sites/knowledge')
}

// Cognition API
export const cognitionApi = {
    gaps: () => api.get('/cognition/gaps'),
    // Longer timeout for AI-powered horizon (may take 30-60s with retries)
    horizon: () => api.get('/cognition/horizon', { timeout: 120000 })
}


export default api
