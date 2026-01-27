<template>
  <div class="search-overlay" v-if="isOpen" @click.self="close">
    <div class="search-modal">
      <div class="search-header">
        <div class="mode-toggles">
          <button 
            :class="{ active: mode === 'search' }" 
            @click="mode = 'search'"
          >
           <el-icon><Search /></el-icon> SEARCH
          </button>
          <button 
            :class="{ active: mode === 'chat' }" 
            @click="mode = 'chat'"
          >
           <el-icon><Cpu /></el-icon> AI CHAT
          </button>
        </div>
        <button class="close-btn" @click="close">×</button>
      </div>

      <!-- SEARCH MODE -->
      <div v-if="mode === 'search'" class="search-mode">
        <div class="input-wrapper">
             <input 
              ref="searchInput"
              v-model="query" 
              @keyup.enter="performSearch"
              placeholder="Search your knowledge base..." 
              class="cyber-input"
              autofocus
            />
            <div class="input-line"></div>
        </div>
        
        <div class="results-area">
          <div v-if="loading" class="loading">
              <div class="scan-line"></div>
              SEARCHING NEURAL NET...
          </div>
          <div v-else-if="results.length" class="results-list">
            <div 
              v-for="item in results" 
              :key="item.id" 
              class="result-item"
              @click="openUrl(item.url)"
            >
              <div class="result-title">{{ item.title }}</div>
              <div class="result-summary">{{ item.contentSummary }}</div>
              <div class="result-meta">
                  <el-icon><Clock /></el-icon> {{ new Date(item.visitTime).toLocaleDateString() }}
              </div>
            </div>
          </div>
          <div v-else-if="searched" class="empty">NO DATA FOUND</div>
        </div>
      </div>

      <!-- CHAT MODE -->
      <div v-if="mode === 'chat'" class="chat-mode">
        <div class="chat-history" ref="chatHistory">
          <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
            <div class="msg-content">{{ msg.content }}</div>
          </div>
          <div v-if="chatLoading" class="message assistant thinking">
            <div class="msg-content">THINKING...</div>
          </div>
        </div>

        <div class="chat-input-area">
          <input 
            v-model="chatQuery" 
            @keyup.enter="sendMessage"
            placeholder="Ask AI about your history..." 
            class="cyber-input"
          />
          <button @click="sendMessage" class="send-btn">SEND</button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Search, Cpu, Clock } from '@element-plus/icons-vue'

const props = defineProps(['modelValue'])
const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const mode = ref('search') // 'search' | 'chat'
const query = ref('')
const chatQuery = ref('')
const results = ref([])
const messages = ref([
  { role: 'assistant', content: 'Connection established. Ready for query.' }
])
const loading = ref(false)
const chatLoading = ref(false)
const searched = ref(false)
const searchInput = ref(null)
const chatHistory = ref(null)

const API_BASE = 'http://localhost:8091/api'

// Open/Close logic
watch(() => props.modelValue, (val) => {
  isOpen.value = val
  if (val) {
    nextTick(() => searchInput.value?.focus())
  }
})

function close() {
  emit('update:modelValue', false)
}

function openUrl(url) {
  window.open(url, '_blank')
}

// Search Logic
async function performSearch() {
  if (!query.value.trim()) return
  loading.value = true
  searched.value = true
  results.value = []
  
  try {
    const res = await fetch(`${API_BASE}/search?q=${encodeURIComponent(query.value)}`)
    if (res.ok) {
      results.value = await res.json()
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// Chat Logic
async function sendMessage() {
  if (!chatQuery.value.trim() || chatLoading.value) return
  
  const q = chatQuery.value
  messages.value.push({ role: 'user', content: q })
  chatQuery.value = ''
  chatLoading.value = true
  scrollToBottom()

  try {
    const res = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: q })
    })
    
    if (res.ok) {
      const data = await res.json()
      messages.value.push({ role: 'assistant', content: data.answer })
    } else {
      messages.value.push({ role: 'assistant', content: 'Error: Failed to reach neural core.' })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: 'Connection Error.' })
  } finally {
    chatLoading.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatHistory.value) {
      chatHistory.value.scrollTop = chatHistory.value.scrollHeight
    }
  })
}

// Global Shortcut Cmd+K
window.addEventListener('keydown', (e) => {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    emit('update:modelValue', !isOpen.value)
  }
  if (e.key === 'Escape' && isOpen.value) {
    close()
  }
})
</script>

<style scoped>
.search-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.9);
  backdrop-filter: blur(5px);
  z-index: 10000;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 100px;
  animation: fadeIn 0.2s;
}

.search-modal {
  width: 700px;
  background: var(--cyber-panel);
  border: 1px solid var(--cyber-primary);
  box-shadow: 0 0 30px rgba(6, 182, 212, 0.15);
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.search-header {
  display: flex;
  justify-content: space-between;
  padding: 0;
  background: rgba(15, 23, 42, 0.5);
  border-bottom: 1px solid var(--cyber-panel-border);
}

.mode-toggles { display: flex; }

.mode-toggles button {
  background: none;
  border: none;
  color: var(--text-secondary);
  padding: 12px 24px;
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 13px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 2px solid transparent;
}

.mode-toggles button.active {
  color: var(--cyber-primary);
  background: rgba(6, 182, 212, 0.1);
  border-bottom-color: var(--cyber-primary);
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 24px;
  cursor: pointer;
  padding: 0 20px;
  transition: color 0.2s;
}
.close-btn:hover { color: var(--cyber-accent); }

.input-wrapper {
    position: relative;
    padding: 2px;
}
.cyber-input {
  width: 100%;
  padding: 20px;
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: 16px;
  font-family: var(--font-mono);
  outline: none;
}
.input-line {
    height: 1px;
    background: var(--cyber-panel-border);
    width: 100%;
    margin-top: -1px;
    transition: 0.3s;
}
.cyber-input:focus + .input-line {
    background: var(--cyber-primary);
    box-shadow: 0 0 10px var(--cyber-primary);
}

.cyber-input::placeholder {
  color: var(--text-secondary);
  opacity: 0.5;
}

/* Results */
.results-area {
  padding: 0;
  overflow-y: auto;
  max-height: 500px;
  min-height: 100px;
}

.result-item {
  padding: 15px 20px;
  border-bottom: 1px solid var(--cyber-panel-border);
  cursor: pointer;
  transition: background 0.2s;
}

.result-item:hover {
  background: rgba(6, 182, 212, 0.05);
  padding-left: 25px; /* Slight shift */
}

.result-title {
  color: var(--cyber-primary);
  font-weight: 600;
  font-size: 15px;
  font-family: var(--font-body);
}

.result-summary {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 5px;
  line-height: 1.4;
}

.result-meta {
  color: var(--text-muted);
  font-size: 11px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Chat */
.chat-mode {
  display: flex;
  flex-direction: column;
  height: 500px;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message {
  max-width: 85%;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;
  position: relative;
  /* clip-path: polygon(0 0, 100% 0, 100% calc(100% - 10px), calc(100% - 10px) 100%, 0 100%); */
}

.message.user {
  align-self: flex-end;
  background: rgba(6, 182, 212, 0.1);
  border: 1px solid var(--cyber-primary);
  color: var(--text-primary);
  border-radius: 8px 8px 0 8px;
}

.message.assistant {
  align-self: flex-start;
  background: rgba(139, 92, 246, 0.1);
  border: 1px solid var(--cyber-secondary);
  color: var(--text-primary);
  border-radius: 8px 8px 8px 0;
}

.chat-input-area {
  display: flex;
  border-top: 1px solid var(--cyber-panel-border);
}

.send-btn {
  background: rgba(6, 182, 212, 0.1);
  border: none;
  border-left: 1px solid var(--cyber-panel-border);
  color: var(--cyber-primary);
  padding: 0 24px;
  cursor: pointer;
  font-family: var(--font-heading);
  letter-spacing: 0.1em;
  font-weight: bold;
  transition: all 0.2s;
}

.send-btn:hover {
  background: rgba(6, 182, 212, 0.2);
  color: #fff;
}

.loading, .empty {
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 15px;
  text-align: center;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.1em;
}

.scan-line {
    width: 60%;
    height: 2px;
    background: var(--cyber-primary);
    box-shadow: 0 0 15px var(--cyber-primary);
    animation: scan 1.5s infinite ease-in-out alternate;
}

@keyframes scan {
    from { width: 10%; opacity: 0.3; }
    to { width: 80%; opacity: 1; }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
