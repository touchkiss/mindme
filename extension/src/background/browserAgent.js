// browserAgent.js - Smart Browser Agent for automated page browsing and content extraction

// ============== Configuration ==============
const AGENT_CONFIG = {
    maxConcurrentTabs: 2,
    minDelayMs: 3000,
    maxDelayMs: 8000,
    pageLoadTimeoutMs: 30000,
    scrollIntervalMs: 500,
    maxScrolls: 10
};

// ============== Task Queue ==============
class TaskQueue {
    constructor() {
        this.queue = [];
        this.processing = false;
    }

    add(task) {
        // task: { url, type, priority, metadata }
        // Types: 'watch_check', 'pre_validate', 'deep_dive', 'manual'
        task.id = `task_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        task.status = 'pending';
        task.addedAt = Date.now();

        // Insert by priority (higher = more urgent)
        const priority = task.priority || 0;
        let inserted = false;
        for (let i = 0; i < this.queue.length; i++) {
            if ((this.queue[i].priority || 0) < priority) {
                this.queue.splice(i, 0, task);
                inserted = true;
                break;
            }
        }
        if (!inserted) {
            this.queue.push(task);
        }

        console.log(`[Agent] Task added: ${task.type} - ${task.url} (Queue size: ${this.queue.length})`);
        return task.id;
    }

    getNext() {
        return this.queue.find(t => t.status === 'pending');
    }

    markProcessing(taskId) {
        const task = this.queue.find(t => t.id === taskId);
        if (task) task.status = 'processing';
    }

    markComplete(taskId, result) {
        const task = this.queue.find(t => t.id === taskId);
        if (task) {
            task.status = 'complete';
            task.result = result;
            task.completedAt = Date.now();
        }
    }

    markFailed(taskId, error) {
        const task = this.queue.find(t => t.id === taskId);
        if (task) {
            task.status = 'failed';
            task.error = error;
            task.completedAt = Date.now();
        }
    }

    getStats() {
        return {
            pending: this.queue.filter(t => t.status === 'pending').length,
            processing: this.queue.filter(t => t.status === 'processing').length,
            complete: this.queue.filter(t => t.status === 'complete').length,
            failed: this.queue.filter(t => t.status === 'failed').length,
            total: this.queue.length
        };
    }

    cleanup(maxAge = 3600000) { // Clean up tasks older than 1 hour
        const cutoff = Date.now() - maxAge;
        this.queue = this.queue.filter(t =>
            t.status === 'pending' ||
            t.status === 'processing' ||
            (t.completedAt && t.completedAt > cutoff)
        );
    }
}

// ============== Tab Manager ==============
class TabManager {
    constructor(maxConcurrent = AGENT_CONFIG.maxConcurrentTabs) {
        this.maxConcurrent = maxConcurrent;
        this.activeTabs = new Map(); // tabId -> taskId
    }

    canOpenNew() {
        return this.activeTabs.size < this.maxConcurrent;
    }

    async openTab(url, taskId) {
        return new Promise((resolve, reject) => {
            chrome.tabs.create({
                url,
                active: false, // Open in background
                pinned: false
            }, (tab) => {
                if (chrome.runtime.lastError) {
                    reject(new Error(chrome.runtime.lastError.message));
                    return;
                }
                this.activeTabs.set(tab.id, taskId);
                console.log(`[Agent] Opened tab ${tab.id} for task ${taskId}`);
                resolve(tab);
            });
        });
    }

    async closeTab(tabId) {
        return new Promise((resolve) => {
            this.activeTabs.delete(tabId);
            chrome.tabs.remove(tabId, () => {
                if (chrome.runtime.lastError) {
                    console.warn(`[Agent] Failed to close tab ${tabId}:`, chrome.runtime.lastError.message);
                }
                console.log(`[Agent] Closed tab ${tabId}`);
                resolve();
            });
        });
    }

    getActiveCount() {
        return this.activeTabs.size;
    }
}

// ============== Rate Limiter ==============
class RateLimiter {
    constructor(minDelay = AGENT_CONFIG.minDelayMs, maxDelay = AGENT_CONFIG.maxDelayMs) {
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.lastRequestTime = 0;
    }

    async wait() {
        const now = Date.now();
        const timeSinceLastRequest = now - this.lastRequestTime;
        const randomDelay = this.minDelay + Math.random() * (this.maxDelay - this.minDelay);
        const waitTime = Math.max(0, randomDelay - timeSinceLastRequest);

        if (waitTime > 0) {
            console.log(`[Agent] Rate limiting: waiting ${Math.round(waitTime)}ms`);
            await new Promise(resolve => setTimeout(resolve, waitTime));
        }

        this.lastRequestTime = Date.now();
    }
}

// ============== Content Extractor ==============
async function extractPageContent(tabId) {
    return new Promise((resolve) => {
        chrome.scripting.executeScript({
            target: { tabId },
            func: () => {
                // Simple readability-like extraction
                const article = document.querySelector('article')
                    || document.querySelector('[role="main"]')
                    || document.querySelector('main')
                    || document.body;

                // Clone and clean
                const clone = article.cloneNode(true);

                // Remove unwanted elements
                const unwanted = clone.querySelectorAll(
                    'script, style, nav, header, footer, aside, ' +
                    '.advertisement, .ad, .sidebar, .comments, ' +
                    '[role="navigation"], [role="banner"], [role="complementary"]'
                );
                unwanted.forEach(el => el.remove());

                // Extract text
                const text = clone.textContent
                    ?.replace(/\s+/g, ' ')
                    ?.trim()
                    ?.substring(0, 50000) || ''; // Limit to 50KB

                // Extract links for deep-dive
                const links = Array.from(document.querySelectorAll('a[href]'))
                    .filter(a => {
                        const href = a.href;
                        const text = a.textContent?.toLowerCase() || '';
                        // Look for "related", "see also", "read more" type links
                        return (text.includes('related') ||
                            text.includes('see also') ||
                            text.includes('read more') ||
                            text.includes('相关') ||
                            text.includes('推荐') ||
                            text.includes('阅读更多')) &&
                            href.startsWith('http') &&
                            !href.includes('javascript:');
                    })
                    .slice(0, 5)
                    .map(a => ({ url: a.href, text: a.textContent?.trim() }));

                return {
                    url: window.location.href,
                    title: document.title,
                    content: text,
                    contentLength: text.length,
                    relatedLinks: links,
                    extractedAt: new Date().toISOString()
                };
            }
        }, (results) => {
            if (chrome.runtime.lastError || !results || !results[0]) {
                resolve({ error: chrome.runtime.lastError?.message || 'Extraction failed' });
            } else {
                resolve(results[0].result);
            }
        });
    });
}

// ============== Page Quality Validator ==============
function validatePageQuality(content) {
    const issues = [];
    let score = 100;

    // Check for search result pages
    const searchPatterns = [
        /google\.\w+\/search/i,
        /bing\.com\/search/i,
        /baidu\.com\/s/i,
        /duckduckgo\.com/i
    ];
    if (searchPatterns.some(p => p.test(content.url))) {
        issues.push('Search result page');
        score -= 80;
    }

    // Check content length
    if (content.contentLength < 500) {
        issues.push('Very short content');
        score -= 30;
    } else if (content.contentLength < 1000) {
        issues.push('Short content');
        score -= 15;
    }

    // Check for error pages
    const errorPatterns = ['404', 'not found', 'page not found', '找不到页面', '页面不存在'];
    const lowerTitle = content.title?.toLowerCase() || '';
    if (errorPatterns.some(p => lowerTitle.includes(p))) {
        issues.push('Error page (404)');
        score -= 70;
    }

    // Check for paywall indicators
    const paywallPatterns = ['subscribe', 'paywall', 'premium', 'login required', '订阅', '付费'];
    const lowerContent = content.content?.toLowerCase() || '';
    if (paywallPatterns.some(p => lowerContent.includes(p)) && content.contentLength < 2000) {
        issues.push('Possible paywall');
        score -= 20;
    }

    return {
        score: Math.max(0, score),
        issues,
        isValid: score >= 50
    };
}

// ============== Browser Agent ==============
class BrowserAgent {
    constructor() {
        this.taskQueue = new TaskQueue();
        this.tabManager = new TabManager();
        this.rateLimiter = new RateLimiter();
        this.isRunning = false;
        this.processInterval = null;
    }

    start() {
        if (this.isRunning) return;
        this.isRunning = true;
        console.log('[Agent] Starting browser agent');

        // Process queue every 5 seconds
        this.processInterval = setInterval(() => this.processNextTask(), 5000);

        // Initial process
        this.processNextTask();
    }

    stop() {
        this.isRunning = false;
        if (this.processInterval) {
            clearInterval(this.processInterval);
            this.processInterval = null;
        }
        console.log('[Agent] Stopped browser agent');
    }

    async processNextTask() {
        if (!this.isRunning) return;
        if (!this.tabManager.canOpenNew()) {
            console.log('[Agent] Max concurrent tabs reached, waiting...');
            return;
        }

        const task = this.taskQueue.getNext();
        if (!task) return;

        this.taskQueue.markProcessing(task.id);

        try {
            await this.rateLimiter.wait();

            // Open tab
            const tab = await this.tabManager.openTab(task.url, task.id);

            // Wait for page load with timeout
            await this.waitForPageLoad(tab.id);

            // Simulate human-like scrolling
            await this.simulateScrolling(tab.id);

            // Extract content
            const content = await extractPageContent(tab.id);

            if (content.error) {
                throw new Error(content.error);
            }

            // Validate quality
            const quality = validatePageQuality(content);
            content.quality = quality;

            // Close tab
            await this.tabManager.closeTab(tab.id);

            // Mark complete and handle result
            this.taskQueue.markComplete(task.id, content);

            // Report to backend
            await this.reportResult(task, content);

            console.log(`[Agent] Task complete: ${task.url} (Quality: ${quality.score})`);

        } catch (error) {
            console.error(`[Agent] Task failed: ${task.url}`, error);
            this.taskQueue.markFailed(task.id, error.message);
        }
    }

    async waitForPageLoad(tabId, timeout = AGENT_CONFIG.pageLoadTimeoutMs) {
        return new Promise((resolve, reject) => {
            const startTime = Date.now();

            const checkStatus = () => {
                chrome.tabs.get(tabId, (tab) => {
                    if (chrome.runtime.lastError) {
                        reject(new Error('Tab closed unexpectedly'));
                        return;
                    }

                    if (tab.status === 'complete') {
                        resolve();
                    } else if (Date.now() - startTime > timeout) {
                        reject(new Error('Page load timeout'));
                    } else {
                        setTimeout(checkStatus, 500);
                    }
                });
            };

            checkStatus();
        });
    }

    async simulateScrolling(tabId) {
        for (let i = 0; i < AGENT_CONFIG.maxScrolls; i++) {
            await new Promise(resolve => setTimeout(resolve, AGENT_CONFIG.scrollIntervalMs));

            await chrome.scripting.executeScript({
                target: { tabId },
                func: () => {
                    window.scrollBy(0, window.innerHeight * 0.8);
                }
            }).catch(() => { }); // Ignore scroll errors
        }

        // Scroll back to top
        await chrome.scripting.executeScript({
            target: { tabId },
            func: () => {
                window.scrollTo(0, 0);
            }
        }).catch(() => { });
    }

    async reportResult(task, content) {
        try {
            const payload = {
                taskId: task.id,
                taskType: task.type,
                url: content.url,
                title: content.title,
                contentSummary: content.content?.substring(0, 5000), // First 5KB
                contentLength: content.contentLength,
                qualityScore: content.quality?.score || 0,
                qualityIssues: content.quality?.issues || [],
                relatedLinks: content.relatedLinks || [],
                metadata: task.metadata || {}
            };

            // Send to backend
            const serverUrl = await getServerUrl();
            const response = await fetch(`${serverUrl.replace('/activity', '/agent-result')}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                console.warn('[Agent] Failed to report result to backend');
            }
        } catch (error) {
            console.warn('[Agent] Error reporting result:', error);
        }
    }

    // Public API
    addTask(url, type = 'manual', priority = 0, metadata = {}) {
        return this.taskQueue.add({ url, type, priority, metadata });
    }

    getQueueStats() {
        return this.taskQueue.getStats();
    }

    getStatus() {
        return {
            isRunning: this.isRunning,
            activeTabs: this.tabManager.getActiveCount(),
            queue: this.taskQueue.getStats()
        };
    }
}

// ============== Helper ==============
async function getServerUrl() {
    const result = await chrome.storage.sync.get(['serverUrl']);
    return result.serverUrl || 'http://localhost:8091/api/ingest/activity';
}

// ============== Singleton Instance ==============
const browserAgent = new BrowserAgent();

// Export for use in background script
export { browserAgent, BrowserAgent, TaskQueue, TabManager, RateLimiter };
