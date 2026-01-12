<template>
  <div class="search-overlay" v-if="isOpen" @click.self="close">
    <div class="search-modal">
      <div class="search-header">
        <div class="mode-toggles">
          <button 
            :class="{ active: mode === 'search' }" 
            @click="mode = 'search'"
          >🔍 SEARCH</button>
          <button 
            :class="{ active: mode === 'chat' }" 
            @click="mode = 'chat'"
          >🤖 AI CHAT</button>
        </div>
        <button class="close-btn" @click="close">×</button>
      </div>

      <!-- SEARCH MODE -->
      <div v-if="mode === 'search'" class="search-mode">
        <input 
          ref="searchInput"
          v-model="query" 
          @keyup.enter="performSearch"
          placeholder="Search your knowledge base..." 
          class="cyber-input"
          autofocus
        />
        
        <div class="results-area">
          <div v-if="loading" class="loading">SEARCHING...</div>
          <div v-else-if="results.length" class="results-list">
            <div 
              v-for="item in results" 
              :key="item.id" 
              class="result-item"
              @click="openUrl(item.url)"
            >
              <div class="result-title">{{ item.title }}</div>
              <div class="result-summary">{{ item.contentSummary }}</div>
              <div class="result-meta">{{ new Date(item.visitTime).toLocaleDateString() }}</div>
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

const props = defineProps(['modelValue'])
const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const mode = ref('search') // 'search' | 'chat'
const query = ref('')
const chatQuery = ref('')
const results = ref([])
const messages = ref([
  { role: 'assistant', content: 'Hello. I act as your second brain. Ask me anything about what you have browsed.' }
])
const loading = ref(false)
const chatLoading = ref(false)
const searched = ref(false)
const searchInput = ref(null)
const chatHistory = ref(null)

const API_BASE = 'http://localhost:8080/api'

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
  background: rgba(5, 10, 18, 0.9);
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
  border: 1px solid var(--cyber-cyan);
  box-shadow: 0 0 30px rgba(0,255,255,0.15);
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.search-header {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  background: rgba(0,255,255,0.05);
  border-bottom: 1px solid var(--cyber-border);
}

.mode-toggles button {
  background: none;
  border: none;
  color: var(--text-secondary);
  padding: 8px 16px;
  cursor: pointer;
  font-family: 'Orbitron', monospace;
  transition: all 0.2s;
}

.mode-toggles button.active {
  color: var(--cyber-cyan);
  text-shadow: 0 0 10px var(--cyber-cyan);
  border-bottom: 2px solid var(--cyber-cyan);
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 20px;
  cursor: pointer;
}

.cyber-input {
  width: 100%;
  padding: 20px;
  background: none;
  border: none;
  border-bottom: 1px solid var(--cyber-border);
  color: var(--text-primary);
  font-size: 18px;
  font-family: 'Rajdhani', sans-serif;
  outline: none;
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
}

.result-item {
  padding: 15px 20px;
  border-bottom: 1px solid rgba(0,255,255,0.05);
  cursor: pointer;
  transition: background 0.2s;
}

.result-item:hover {
  background: rgba(0,255,255,0.05);
}

.result-title {
  color: var(--cyber-cyan);
  font-weight: bold;
  font-size: 16px;
}

.result-summary {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 5px;
  line-height: 1.4;
}

.result-meta {
  color: var(--text-secondary);
  font-size: 11px;
  margin-top: 5px;
  opacity: 0.7;
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
  max-width: 80%;
  padding: 10px 15px;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
}

.message.user {
  align-self: flex-end;
  background: rgba(0,255,255,0.1);
  border: 1px solid var(--cyber-cyan);
  color: var(--text-primary);
}

.message.assistant {
  align-self: flex-start;
  background: rgba(191,95,255,0.1);
  border: 1px solid var(--cyber-purple);
  color: var(--text-primary);
}

.chat-input-area {
  display: flex;
  border-top: 1px solid var(--cyber-border);
}

.send-btn {
  background: rgba(0,255,255,0.1);
  border: none;
  border-left: 1px solid var(--cyber-border);
  color: var(--cyber-cyan);
  padding: 0 20px;
  cursor: pointer;
  font-family: 'Orbitron', monospace;
}

.send-btn:hover {
  background: rgba(0,255,255,0.2);
}

.loading, .empty {
  padding: 20px;
  text-align: center;
  color: var(--text-secondary);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
