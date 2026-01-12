import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080/api/admin',
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

export default api
