<template>
  <div class="container">
    <!-- Main View -->
    <div v-if="!showSettings">
      <!-- Header -->
      <header class="header">
        <div class="logo">
          <div class="logo-icon">🧠</div>
          <span class="logo-text">MindMe</span>
        </div>
        <div class="status-badge">
          <span class="status-dot"></span>
          <span>{{ isTracking ? 'Tracking' : 'Paused' }}</span>
        </div>
      </header>

      <!-- Stats -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">Queue</div>
          <div class="stat-value">{{ queueCount }}<span class="stat-unit">items</span></div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Today</div>
          <div class="stat-value">{{ todayCount }}<span class="stat-unit">pages</span></div>
        </div>
      </div>

      <!-- Quick Note -->
      <div class="quick-note">
        <div class="section-title">
          <span>📝</span> Quick Note
        </div>
        <textarea
          v-model="noteText"
          class="note-input"
          placeholder="Jot down a quick thought..."
          @keydown.ctrl.enter="saveNote"
          @keydown.meta.enter="saveNote"
        ></textarea>
        <div class="note-actions">
          <div class="tags">
            <span
              v-for="tag in availableTags"
              :key="tag"
              class="tag"
              :class="{ active: selectedTags.includes(tag) }"
              @click="toggleTag(tag)"
            >{{ tag }}</span>
          </div>
          <button class="btn btn-primary" @click="saveNote" :disabled="!noteText.trim()">
            Save
          </button>
        </div>
      </div>

      <!-- Actions -->
      <div class="actions">
        <button class="action-btn" @click="syncNow">
          <span class="icon">🔄</span> Sync Now
        </button>
        <button class="action-btn" @click="showSettings = true">
          <span class="icon">⚙️</span> Settings
        </button>
      </div>

      <!-- Footer -->
      <div class="footer">
        <span class="shortcut-hint">
          <kbd>⌘</kbd>+<kbd>⇧</kbd>+<kbd>M</kbd> Quick Note
        </span>
      </div>
    </div>

    <!-- Settings Panel -->
    <div v-else class="settings-panel active">
      <div class="settings-header">
        <button class="back-btn" @click="showSettings = false">←</button>
        <span class="settings-title">Settings</span>
      </div>

      <div class="setting-group">
        <label class="setting-label">Server URL</label>
        <input
          v-model="settings.serverUrl"
          type="text"
          class="setting-input"
          placeholder="http://localhost:8080/api/ingest/activity"
        />
      </div>

      <div class="setting-group">
        <label class="setting-label">Blacklist (domains to ignore)</label>
        <input
          v-model="newBlacklistItem"
          type="text"
          class="setting-input"
          placeholder="Enter domain and press Enter"
          @keydown.enter="addBlacklistItem"
        />
        <div class="blacklist-items">
          <div v-for="item in settings.blacklist" :key="item" class="blacklist-item">
            {{ item }}
            <button class="blacklist-remove" @click="removeBlacklistItem(item)">×</button>
          </div>
        </div>
      </div>

      <div class="setting-group">
        <label class="setting-label">Min Duration: {{ settings.minDurationSeconds }}s</label>
        <input
          v-model.number="settings.minDurationSeconds"
          type="range"
          min="5"
          max="120"
          step="5"
          class="setting-slider"
        />
        <div class="slider-labels">
          <span>5s</span>
          <span>120s</span>
        </div>
      </div>

      <div class="setting-group">
        <label class="setting-label">Min Interest Score: {{ settings.minInterestScore }}</label>
        <input
          v-model.number="settings.minInterestScore"
          type="range"
          min="10"
          max="80"
          step="5"
          class="setting-slider"
        />
        <div class="slider-labels">
          <span>10</span>
          <span>80</span>
        </div>
      </div>

      <div class="setting-group">
        <label class="setting-checkbox">
          <input type="checkbox" v-model="settings.activeOnlyMode" />
          <span>Only track active behaviors (typed URL, search, bookmarks)</span>
        </label>
      </div>

      <div class="setting-group">
        <label class="setting-checkbox">
          <input type="checkbox" v-model="settings.trackSearchQueries" />
          <span>Track search engine queries</span>
        </label>
      </div>

      <button class="btn btn-primary primary-action" @click="saveSettings">
        Save Settings
      </button>

      <button class="btn btn-secondary action-btn-full" @click="importBookmarks" :disabled="importingBookmarks">
        <span class="icon">{{ importingBookmarks ? '⏳' : '📚' }}</span> 
        {{ importingBookmarks ? `Importing ${importProgress}...` : 'Import Chrome Bookmarks' }}
      </button>
    </div>

    <!-- Toast -->
    <div class="toast" :class="{ show: showToast }">{{ toastMessage }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// State
const showSettings = ref(false)
const isTracking = ref(true)
const queueCount = ref(0)
const todayCount = ref(0)
const noteText = ref('')
const selectedTags = ref([])
const availableTags = ['idea', 'todo', 'important', 'research']
const showToast = ref(false)
const toastMessage = ref('')
const newBlacklistItem = ref('')
const importingBookmarks = ref(false)
const importProgress = ref('')

const settings = ref({
  serverUrl: 'http://localhost:8080/api/ingest/activity',
  blacklist: ['youtube.com', 'netflix.com'],
  minDurationSeconds: 10,
  minInterestScore: 30,
  activeOnlyMode: false,
  trackSearchQueries: true
})

// Methods
function toggleTag(tag) {
  const index = selectedTags.value.indexOf(tag)
  if (index === -1) {
    selectedTags.value.push(tag)
  } else {
    selectedTags.value.splice(index, 1)
  }
}

function toast(msg) {
  toastMessage.value = msg
  showToast.value = true
  setTimeout(() => {
    showToast.value = false
  }, 2000)
}

async function saveNote() {
  if (!noteText.value.trim()) return

  const note = {
    type: 'QUICK_NOTE',
    content: noteText.value.trim(),
    tags: [...selectedTags.value],
    timestamp: new Date().toISOString(),
    url: null
  }

  try {
    await chrome.runtime.sendMessage({ type: 'SAVE_NOTE', data: note })
    noteText.value = ''
    selectedTags.value = []
    toast('Note saved!')
    loadStats()
  } catch (err) {
    console.error('Failed to save note:', err)
    toast('Failed to save note')
  }
}

async function syncNow() {
  try {
    await chrome.runtime.sendMessage({ type: 'FORCE_SYNC' })
    toast('Sync triggered!')
    setTimeout(loadStats, 1000)
  } catch (err) {
    console.error('Sync failed:', err)
    toast('Sync failed')
  }
}

function addBlacklistItem() {
  const item = newBlacklistItem.value.trim()
  if (item && !settings.value.blacklist.includes(item)) {
    settings.value.blacklist.push(item)
    newBlacklistItem.value = ''
  }
}

function removeBlacklistItem(item) {
  settings.value.blacklist = settings.value.blacklist.filter(i => i !== item)
}

async function saveSettings() {
  try {
    // Fix: Deep copy to remove Vue Proxies before saving
    const settingsPlain = JSON.parse(JSON.stringify(settings.value))
    await chrome.storage.sync.set(settingsPlain)

    // Push to backend for persistence
    const apiBase = settings.value.serverUrl.includes('/ingest')
        ? settings.value.serverUrl.substring(0, settings.value.serverUrl.indexOf('/ingest'))
        : "http://localhost:8080/api";

    await fetch(`${apiBase}/config/extension_settings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ value: JSON.stringify(settings.value) })
    });

    toast('Settings saved & synced!')
    showSettings.value = false
  } catch (err) {
    console.error('Failed to save settings:', err)
    // Don't block UI if sync fails, but warn
    toast('Local saved, Sync failed')
    showSettings.value = false
  }
}

async function importBookmarks() {
  if (!chrome.bookmarks) {
    toast('Error: No bookmarks permission')
    return
  }
  
  try {
    importingBookmarks.value = true
    importProgress.value = 'scanning'
    
    // 1. Get entire tree
    const tree = await chrome.bookmarks.getTree()
    
    // 2. Flatten
    const bookmarks = []
    const traverse = (nodes) => {
      for (const node of nodes) {
        if (node.url) {
          bookmarks.push(node)
        }
        if (node.children) {
          traverse(node.children)
        }
      }
    }
    traverse(tree)
    
    if (bookmarks.length === 0) {
      toast('No bookmarks found')
      importingBookmarks.value = false
      return
    }

    importProgress.value = `0/${bookmarks.length}`
    
    // 3. Batch upload
    const apiBase = settings.value.serverUrl.includes('/ingest')
        ? settings.value.serverUrl.substring(0, settings.value.serverUrl.indexOf('/ingest'))
        : "http://localhost:8080/api";
    const INGEST_URL = `${apiBase}/ingest/activity`

    const BATCH_SIZE = 50
    let uploaded = 0

    for (let i = 0; i < bookmarks.length; i += BATCH_SIZE) {
      const batch = bookmarks.slice(i, i + BATCH_SIZE).map(b => ({
        url: b.url,
        title: b.title,
        visitTime: new Date(b.dateAdded || Date.now()).toISOString(),
        durationSeconds: 0,
        contentSummary: "Imported from Chrome Bookmarks",
        pageContent: "",
        transitionType: 'imported_bookmark',
        interestScore: 70, // High interest for bookmarks
        interactionCount: 1
      }))

      await fetch(INGEST_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(batch)
      })

      uploaded += batch.length
      importProgress.value = `${uploaded}/${bookmarks.length}`
    }

    toast(`Successfully imported ${uploaded} bookmarks!`)
    loadStats() // Refresh counts

  } catch (e) {
    console.error('Import failed', e)
    toast('Import failed: ' + e.message)
  } finally {
    importingBookmarks.value = false
  }
}

async function loadSettings() {
  try {
    const stored = await chrome.storage.sync.get([
      'serverUrl', 'blacklist', 'minDurationSeconds',
      'minInterestScore', 'activeOnlyMode', 'trackSearchQueries'
    ])
    if (stored.serverUrl) settings.value.serverUrl = stored.serverUrl
    if (stored.blacklist) settings.value.blacklist = stored.blacklist
    if (stored.minDurationSeconds !== undefined) settings.value.minDurationSeconds = stored.minDurationSeconds
    if (stored.minInterestScore !== undefined) settings.value.minInterestScore = stored.minInterestScore
    if (stored.activeOnlyMode !== undefined) settings.value.activeOnlyMode = stored.activeOnlyMode
    if (stored.trackSearchQueries !== undefined) settings.value.trackSearchQueries = stored.trackSearchQueries
  } catch (err) {
    console.error('Failed to load settings:', err)
  }
}

async function loadStats() {
  try {
    const response = await chrome.runtime.sendMessage({ type: 'GET_STATS' })
    if (response) {
      queueCount.value = response.queueCount || 0
      todayCount.value = response.todayCount || 0
    }
  } catch (err) {
    console.error('Failed to load stats:', err)
  }
}

onMounted(() => {
  loadSettings()
  loadStats()
})
</script>

<style scoped>
.container {
  width: 320px;
  background: #0f172a;
  color: #f1f5f9;
  font-size: 14px;
  padding-bottom: 20px;
}

.primary-action {
  width: 100%;
  margin-top: 20px;
  margin-bottom: 10px;
}

.action-btn-full {
  width: 100%;
  padding: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #a0aec0;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.action-btn-full:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.action-btn-full:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
