// background.js - MindMe Service Worker with Interest Scoring

// ============== Browser Agent ==============
import { browserAgent } from './browserAgent.js';

// ============== Configuration ==============
const DEFAULT_SERVER_URL = "http://localhost:8091/api/ingest/activity";
const SYNC_INTERVAL_MINUTES = 1;

// Default settings
const DEFAULT_SETTINGS = {
    serverUrl: DEFAULT_SERVER_URL,
    blacklist: ["youtube.com", "netflix.com"],
    minDurationSeconds: 10,      // Minimum active time to report
    minInterestScore: 30,        // Minimum score to report (0-100)
    activeOnlyMode: false,       // Only report typed/generated/bookmark
    trackSearchQueries: true,    // Track search engine queries
    agentEnabled: true           // Browser agent enabled by default
};

// ============== State ==============
let activityQueue = [];
let notesQueue = [];
let todayCount = 0;

// Active tab tracking
let activeTabId = null;
let activeTabStartTime = null;
let tabData = new Map(); // tabId -> { url, title, startTime, activeTime, transitionType, scrollDepth, interactions, searchQuery, referrer }
let tabReferrers = new Map(); // tabId -> referrerUrl (temporary storage for new tabs)
let tabSelections = new Map(); // tabId -> lastSelectedText

// ============== Interest Scoring ==============
const INTENT_SCORES = {
    typed: 40,           // User manually typed URL
    generated: 35,       // Search engine result click
    auto_bookmark: 30,   // Opened from bookmark
    bookmark_created: 50, // User created a bookmark
    keyword: 30,         // Address bar keyword search
    link: 15,            // Clicked a link
    reload: 10,          // Page reload
    auto_subframe: 5,    // Auto redirect/subframe
    form_submit: 20,     // Form submission
    default: 10          // Unknown
};

// Search engine patterns
const SEARCH_ENGINES = [
    { pattern: /google\.[a-z]+\/search/, queryParam: 'q' },
    { pattern: /bing\.com\/search/, queryParam: 'q' },
    { pattern: /baidu\.com\/s/, queryParam: 'wd' },
    { pattern: /duckduckgo\.com\//, queryParam: 'q' },
    { pattern: /yahoo\.com\/search/, queryParam: 'p' },
    { pattern: /sogou\.com\/web/, queryParam: 'query' },
    { pattern: /zhihu\.com\/search/, queryParam: 'q' }
];

function calculateInterestScore(data) {
    let score = 0;

    // Intent score (0-40)
    const intentScore = INTENT_SCORES[data.transitionType] || INTENT_SCORES.default;
    score += intentScore;

    // Engagement score (0-40)
    // Active time: 0-20 points (2 points per 10 seconds, max 20)
    const activeSeconds = data.activeTime || 0;
    const timeScore = Math.min(20, Math.floor(activeSeconds / 10) * 2);
    score += timeScore;

    // Scroll depth: 0-10 points
    const scrollDepth = data.scrollDepth || 0;
    const scrollScore = scrollDepth >= 80 ? 10 : scrollDepth >= 50 ? 5 : scrollDepth >= 20 ? 2 : 0;
    score += scrollScore;

    // Interactions: 0-10 points
    const interactions = data.interactions || 0;
    const interactionScore = interactions > 0 ? 10 : 0;
    score += interactionScore;

    // Bonus score (0-20)
    // Search query bonus
    if (data.searchQuery) {
        score += 10;
    }

    // High intent bonus for typed/generated
    if (data.transitionType === 'typed' || data.transitionType === 'generated') {
        score += 5;
    }

    return Math.min(100, score);
}

function extractSearchQuery(url) {
    try {
        const urlObj = new URL(url);
        for (const engine of SEARCH_ENGINES) {
            if (engine.pattern.test(url)) {
                return urlObj.searchParams.get(engine.queryParam) || null;
            }
        }
    } catch (e) {
        // Invalid URL
    }
    return null;
}

function isSearchResultClick(data) {
    // Check if this page was reached from a search engine
    return data.transitionType === 'generated';
}

// ============== Initialization ==============
// ============== Initialization ==============
chrome.runtime.onInstalled.addListener(async () => {
    // Only set defaults for missing keys to avoid overwriting user settings
    const stored = await chrome.storage.sync.get(null);
    const toSet = {};
    for (const [key, value] of Object.entries(DEFAULT_SETTINGS)) {
        if (stored[key] === undefined) {
            toSet[key] = value;
        }
    }
    if (Object.keys(toSet).length > 0) {
        await chrome.storage.sync.set(toSet);
        console.log("Initialized missing settings with defaults");
    }

    console.log("MindMe installed/updated.");
    chrome.alarms.create("syncData", { periodInMinutes: SYNC_INTERVAL_MINUTES });
    chrome.alarms.create("agentProcess", { periodInMinutes: 5 }); // Agent check every 5 mins
    createContextMenus();

    // Start browser agent if enabled
    if (stored.agentEnabled !== false) {
        browserAgent.start();
    }
});

chrome.runtime.onStartup.addListener(async () => {
    createContextMenus();

    // Start browser agent if enabled
    const { agentEnabled } = await chrome.storage.sync.get(['agentEnabled']);
    if (agentEnabled !== false) {
        browserAgent.start();
    }
});

// ============== Web Navigation Tracking ==============
chrome.webNavigation.onCreatedNavigationTarget.addListener((details) => {
    // Check if we have data for the source tab
    const sourceData = tabData.get(details.sourceTabId);
    if (sourceData && sourceData.url) {
        tabReferrers.set(details.tabId, sourceData.url);
        console.log(`[Nav] Tab ${details.tabId} opened from ${details.sourceTabId} (${sourceData.url})`);
    }
});

chrome.webNavigation.onCommitted.addListener((details) => {
    if (details.frameId !== 0) return; // Only main frame

    const tabId = details.tabId;
    const transitionType = details.transitionType;
    const url = details.url;

    // Skip chrome:// and extension pages
    if (url.startsWith('chrome://') || url.startsWith('chrome-extension://')) {
        return;
    }

    // Check for search query
    const searchQuery = extractSearchQuery(url);

    // Check if this search matches a recent selection from the referrer
    let relatedRecordUrl = null;
    let relationshipType = null;

    if (searchQuery && tabReferrers.has(tabId)) {
        // Find the source tab ID for this referrer URL
        // Simple heuristic: check all tabs to find who has this URL. 
        // Better: store sourceTabId in tabReferrers (currently it stores URL). 
        // For now, let's rely on recent selections across all tabs provided they match the query
        // Or if we stored sourceTabId in `onCreatedNavigationTarget`, we could look it up.
        // Let's iterate tabSelections to find a match.
        for (const [sourceTabId, selection] of tabSelections.entries()) {
            if (selection && (selection.includes(searchQuery) || searchQuery.includes(selection))) {
                const sourceTab = tabData.get(sourceTabId);
                if (sourceTab) {
                    relatedRecordUrl = sourceTab.url;
                    relationshipType = "SEARCH_FROM_SELECTION";
                    console.log(`[Link] Search "${searchQuery}" linked to source tab ${sourceTabId}`);

                    // Notify user of the link
                    chrome.scripting.executeScript({
                        target: { tabId: tabId },
                        func: showNotification,
                        args: [`Context Linked: Search from "${sourceTab.title.substring(0, 15)}..."`]
                    });
                    break;
                }
            }
        }
    }

    // Initialize or update tab data
    tabData.set(tabId, {
        url: url,
        title: '',
        startTime: Date.now(),
        activeTime: 0,
        transitionType: transitionType,
        scrollDepth: 0,
        interactions: 0,
        activeReadingSeconds: 0,
        activeReadingSeconds: 0,
        searchQuery: searchQuery,
        searchQuery: searchQuery,
        referrer: tabReferrers.get(tabId) || null,
        relatedRecordUrl: relatedRecordUrl,
        relationshipType: relationshipType
    });

    // Clean up temporary referrer provided it was used
    if (tabReferrers.has(tabId)) {
        setTimeout(() => tabReferrers.delete(tabId), 5000); // clear after 5s just in case
    }

    console.log(`[Nav] Tab ${tabId}: ${transitionType} -> ${url}${searchQuery ? ` (search: "${searchQuery}")` : ''}${tabData.get(tabId).referrer ? ` [Ref: ${tabData.get(tabId).referrer}]` : ''}`);
});

// Get page title when completed
chrome.webNavigation.onCompleted.addListener((details) => {
    if (details.frameId !== 0) return;

    chrome.tabs.get(details.tabId, (tab) => {
        if (chrome.runtime.lastError) return;

        const data = tabData.get(details.tabId);
        if (data) {
            data.title = tab.title || '';
        }
    });
});

// ============== Active Tab Time Tracking ==============
chrome.tabs.onActivated.addListener((activeInfo) => {
    const now = Date.now();

    // Save time for previous active tab
    if (activeTabId !== null && tabData.has(activeTabId)) {
        const prevData = tabData.get(activeTabId);
        if (activeTabStartTime) {
            prevData.activeTime += (now - activeTabStartTime) / 1000;
        }
    }

    // Start tracking new active tab
    activeTabId = activeInfo.tabId;
    activeTabStartTime = now;
});

// Handle tab close - report activity
chrome.tabs.onRemoved.addListener(async (tabId) => {
    const now = Date.now();

    // Update active time if this was the active tab
    if (tabId === activeTabId && tabData.has(tabId)) {
        const data = tabData.get(tabId);
        if (activeTabStartTime) {
            data.activeTime += (now - activeTabStartTime) / 1000;
        }
    }

    // Check if we should report this tab
    const data = tabData.get(tabId);
    if (data) {
        await evaluateAndReport(tabId, data);
        tabData.delete(tabId);
    }

    if (tabId === activeTabId) {
        activeTabId = null;
        activeTabStartTime = null;
    }
});

// ============== Bookmarks Tracking ==============
chrome.bookmarks.onCreated.addListener(async (id, bookmark) => {
    if (!bookmark.url) return; // Folder

    console.log(`[Bookmark] New bookmark created: ${bookmark.title} (${bookmark.url})`);

    // Check if this URL is currently open in any tab
    let found = false;
    for (const [tid, data] of tabData.entries()) {
        if (data.url === bookmark.url) {
            data.interestScore = (data.interestScore || 0) + 30; // Boost score
            data.interactions += 1;
            console.log(`[Bookmark] Boosted active tab ${tid} score to ${data.interestScore}`);

            // Optional: Notify user
            chrome.scripting.executeScript({
                target: { tabId: tid },
                func: showNotification,
                args: ["Interest Boosted: Bookmark Saved!"]
            });
            found = true;
            break;
        }
    }

    if (!found) {
        // If not in active tabs (e.g. bookmarked from other device sync, or quick save), create record
        const record = {
            url: bookmark.url,
            title: bookmark.title,
            visitTime: new Date().toISOString(),
            durationSeconds: 0,
            contentSummary: "Bookmarked page",
            pageContent: "",
            // High interest
            activeSeconds: 0,
            scrollDepth: 0,
            interactionCount: 1,
            interestScore: 80, // High default for bookmarks
            transitionType: 'bookmark_created'
        };
        activityQueue.push(record);
        todayCount++;
        syncData(); // Sync immediately
    }
});

// ============== Evaluate and Report ==============
async function evaluateAndReport(tabId, data) {
    // Get settings with proper defaults handling
    const stored = await chrome.storage.sync.get([
        'serverUrl', 'blacklist', 'minDurationSeconds',
        'minInterestScore', 'activeOnlyMode', 'trackSearchQueries'
    ]);
    const settings = {
        serverUrl: stored.serverUrl ?? DEFAULT_SETTINGS.serverUrl,
        blacklist: stored.blacklist ?? DEFAULT_SETTINGS.blacklist,
        minDurationSeconds: stored.minDurationSeconds ?? DEFAULT_SETTINGS.minDurationSeconds,
        minInterestScore: stored.minInterestScore ?? DEFAULT_SETTINGS.minInterestScore,
        activeOnlyMode: stored.activeOnlyMode ?? DEFAULT_SETTINGS.activeOnlyMode,
        trackSearchQueries: stored.trackSearchQueries ?? DEFAULT_SETTINGS.trackSearchQueries
    };

    // Skip blacklisted domains
    try {
        const urlObj = new URL(data.url);
        const hostname = urlObj.hostname;

        const blacklist = Array.isArray(settings.blacklist) ? settings.blacklist : [];
        for (const pattern of blacklist) {
            if (matchWildcard(hostname, pattern)) {
                console.log(`[Skip] Blacklisted: ${hostname} (matched ${pattern})`);
                return;
            }
        }
    } catch (e) {
        return;
    }

    // Calculate interest score
    const interestScore = calculateInterestScore(data);

    // Check minimum duration
    if (data.activeTime < settings.minDurationSeconds) {
        console.log(`[Skip] Duration too short: ${data.activeTime.toFixed(1)}s < ${settings.minDurationSeconds}s`);
        return;
    }

    // Check minimum interest score
    if (interestScore < settings.minInterestScore) {
        console.log(`[Skip] Score too low: ${interestScore} < ${settings.minInterestScore}`);
        return;
    }

    // Check active-only mode
    if (settings.activeOnlyMode) {
        const activeTypes = ['typed', 'generated', 'auto_bookmark', 'keyword'];
        if (!activeTypes.includes(data.transitionType)) {
            console.log(`[Skip] Active-only mode: ${data.transitionType} not in active types`);
            return;
        }
    }

    // Create activity record
    const record = {
        url: data.url,
        title: data.title,
        visitTime: new Date(data.startTime).toISOString(),
        durationSeconds: Math.floor(data.activeTime),
        contentSummary: data.searchQuery ? `Search: "${data.searchQuery}"` : '',
        pageContent: '',
        // Extended fields
        transitionType: data.transitionType,
        scrollDepth: data.scrollDepth,
        interactionCount: data.interactions,
        interestScore: interestScore,
        interestScore: interestScore,
        searchQuery: data.searchQuery,
        searchQuery: data.searchQuery,
        referrer: data.referrer,
        relatedRecordUrl: data.relatedRecordUrl,
        relationshipType: data.relationshipType
    };

    console.log(`[Report] Score ${interestScore}: ${data.title} (${data.transitionType}, ${data.activeTime.toFixed(1)}s)`);

    activityQueue.push(record);
    todayCount++;

    if (activityQueue.length > 5) {
        syncData();
    }
}

// ============== AI Conversation Handling ==============
async function handleAIConversation(data, tab) {
    if (!data || !data.fullConversation) {
        console.log('[AI] Empty conversation data, skipping');
        return;
    }

    // Generate external ID using platform + native conversation ID
    const externalId = data.conversationId
        ? `${data.platform}_${data.conversationId}`
        : null;

    console.log(`[AI] Captured ${data.messageCount} messages from ${data.platform}${externalId ? ` (ExternalID: ${externalId})` : ''}`);

    // Create activity record for AI conversation
    const record = {
        // Use external ID for deduplication (same conversation updates instead of creates new)
        externalId: externalId,
        url: data.url || tab?.url || `https://${data.platform}.com`,
        title: `AI对话 (${data.platform}) - ${data.summary.substring(0, 50)}`,
        visitTime: data.timestamp || new Date().toISOString(),
        durationSeconds: 0,
        // Store full conversation in contentSummary
        contentSummary: data.fullConversation,
        pageContent: '', // Could store last response here
        activeSeconds: 0,
        scrollDepth: 100,
        interactionCount: data.messageCount || 1,
        interestScore: 85, // AI conversations are high-value learning activities
        transitionType: 'ai_conversation',
        tags: `AI,${data.platform}`,
        // Store conversation metadata
        searchQuery: data.lastUserQuery || '',
        referrer: null,
        relatedRecordUrl: null,
        relationshipType: null
    };

    activityQueue.push(record);
    todayCount++;

    // Sync immediately for AI conversations (they're valuable)
    if (activityQueue.length >= 1) {
        syncData();
    }
}

// ============== Message Handling ==============
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    switch (request.type) {
        case "ENGAGEMENT_UPDATE":
            // Update from content script
            if (sender.tab && tabData.has(sender.tab.id)) {
                const data = tabData.get(sender.tab.id);
                if (request.scrollDepth !== undefined) {
                    data.scrollDepth = Math.max(data.scrollDepth, request.scrollDepth);
                }
                if (request.interaction) {
                    data.interactions++;
                }
                if (request.activeSeconds !== undefined) {
                    data.activeReadingSeconds = request.activeSeconds;
                }
            }
            break;

        case "SELECTION_UPDATE":
            if (sender.tab) {
                tabSelections.set(sender.tab.id, request.text);
                // console.log(`[Selection] Tab ${sender.tab.id}: ${request.text.substring(0, 20)}...`);
            }
            break;

        case "ACTIVITY_RECORD":
            // Legacy support
            console.log("Received activity:", request.data);
            activityQueue.push(request.data);
            todayCount++;
            if (activityQueue.length > 5) {
                syncData();
            }
            break;

        case "SAVE_NOTE":
            console.log("Received note:", request.data);
            notesQueue.push(request.data);
            if (notesQueue.length > 3) {
                syncData();
            }
            sendResponse({ success: true });
            break;

        case "FORCE_SYNC":
            syncData();
            sendResponse({ success: true });
            break;

        case "GET_STATS":
            sendResponse({
                queueCount: activityQueue.length + notesQueue.length,
                todayCount: todayCount
            });
            break;

        case "AI_CONVERSATION":
            // Handle AI chat conversation capture
            handleAIConversation(request.data, sender.tab);
            sendResponse({ success: true });
            break;

        case "WATCH_PAGE":
            // Add page to watch list via agent
            {
                const url = request.url || sender.tab?.url;
                const title = request.title || sender.tab?.title;
                if (url) {
                    const taskId = browserAgent.addTask(url, 'watch_check', 5, {
                        title,
                        isInitialWatch: true
                    });
                    console.log(`[Agent] Added watch task: ${url}`);
                    sendResponse({ success: true, taskId });
                } else {
                    sendResponse({ success: false, error: 'No URL provided' });
                }
            }
            break;

        case "AGENT_CRAWL":
            // Add URL to agent crawl queue
            {
                const taskId = browserAgent.addTask(
                    request.url,
                    request.type || 'manual',
                    request.priority || 0,
                    request.metadata || {}
                );
                sendResponse({ success: true, taskId });
            }
            break;

        case "AGENT_STATUS":
            // Get agent status
            sendResponse(browserAgent.getStatus());
            break;

        case "AGENT_CONTROL":
            // Start/stop agent
            if (request.action === 'start') {
                browserAgent.start();
                sendResponse({ success: true, status: 'started' });
            } else if (request.action === 'stop') {
                browserAgent.stop();
                sendResponse({ success: true, status: 'stopped' });
            }
            break;
    }
    return true;
});

// ============== Context Menus ==============
function createContextMenus() {
    chrome.contextMenus.removeAll(() => {
        chrome.contextMenus.create({
            id: "save-to-queue",
            title: "Save to Reading Queue",
            contexts: ["page", "link"]
        });
        chrome.contextMenus.create({
            id: "save-selection",
            title: "Save selection to MindMe",
            contexts: ["selection"]
        });
        chrome.contextMenus.create({
            id: "mark-important",
            title: "Mark page as important",
            contexts: ["page"]
        });
        chrome.contextMenus.create({
            id: "quick-note",
            title: "Add quick note for this page",
            contexts: ["page"]
        });
        chrome.contextMenus.create({
            id: "watch-page",
            title: "👁️ Watch this page for updates",
            contexts: ["page"]
        });
    });
}

chrome.contextMenus.onClicked.addListener((info, tab) => {
    switch (info.menuItemId) {
        case "save-to-queue":
            saveToQueue(info, tab);
            break;
        case "save-selection":
            saveSelection(info.selectionText, tab);
            break;
        case "mark-important":
            markPageImportant(tab);
            break;
        case "quick-note":
            chrome.tabs.sendMessage(tab.id, { type: "SHOW_QUICK_NOTE" });
            break;
        case "watch-page":
            // Add page to watch list
            browserAgent.addTask(tab.url, 'watch_check', 5, { 
                title: tab.title, 
                isInitialWatch: true 
            });
            // Show notification
            chrome.scripting.executeScript({
                target: { tabId: tab.id },
                func: showNotification,
                args: ["👁️ Page added to watch list!"]
            });
            break;
    }
});

async function saveToQueue(info, tab) {
    const url = info.linkUrl || tab.url;
    const title = info.linkUrl ? url : tab.title;

    try {
        const { serverUrl } = await chrome.storage.sync.get("serverUrl");
        const apiBase = serverUrl && serverUrl.includes('/ingest')
            ? serverUrl.substring(0, serverUrl.indexOf('/ingest'))
            : "http://localhost:8091/api";

        await fetch(`${apiBase}/reading-queue`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url, title })
        });

        chrome.scripting.executeScript({
            target: { tabId: tab.id },
            func: showNotification,
            args: ["Saved to Reading Queue!"]
        });
    } catch (e) {
        console.error("Failed to save to queue", e);
    }
}

function saveSelection(text, tab) {
    const record = {
        type: "SELECTION",
        content: text,
        url: tab.url,
        title: tab.title,
        timestamp: new Date().toISOString(),
        tags: ["selection"]
    };
    notesQueue.push(record);
    chrome.scripting.executeScript({
        target: { tabId: tab.id },
        func: showNotification,
        args: ["Selection saved to MindMe!"]
    });
}

function markPageImportant(tab) {
    // Boost interest score for this tab
    if (tabData.has(tab.id)) {
        const data = tabData.get(tab.id);
        data.interactions += 5; // Add significant interaction bonus
    }

    const record = {
        type: "PAGE_MARK",
        url: tab.url,
        title: tab.title,
        timestamp: new Date().toISOString(),
        tags: ["important"]
    };
    notesQueue.push(record);
    chrome.scripting.executeScript({
        target: { tabId: tab.id },
        func: showNotification,
        args: ["Page marked as important!"]
    });
}

function showNotification(message) {
    const toast = document.createElement("div");
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed; bottom: 20px; right: 20px;
        background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
        color: white; padding: 12px 24px; border-radius: 8px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        font-size: 14px; font-weight: 500; z-index: 999999;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
}

// ============== Keyboard Shortcuts ==============
chrome.commands.onCommand.addListener((command) => {
    if (command === "quick-note") {
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
            if (tabs[0]) {
                chrome.tabs.sendMessage(tabs[0].id, { type: "SHOW_QUICK_NOTE" });
            }
        });
    }
});

// ============== Periodic Sync ==============
chrome.alarms.onAlarm.addListener((alarm) => {
    if (alarm.name === "syncData") {
        syncData();
    }
});

async function syncData() {
    const hasActivities = activityQueue.length > 0;
    const hasNotes = notesQueue.length > 0;

    if (!hasActivities && !hasNotes) return;

    // Convert all data to ActivityRecord format
    const allRecords = [];

    for (const activity of activityQueue) {
        allRecords.push({
            url: activity.url,
            title: activity.title,
            visitTime: activity.visitTime,
            durationSeconds: activity.durationSeconds,
            contentSummary: activity.contentSummary || "",
            pageContent: activity.pageContent || "",
            // Enhanced metrics
            activeSeconds: activity.activeReadingSeconds || 0,
            scrollDepth: activity.scrollDepth || 0,
            interactionCount: activity.interactionCount || 0,
            interestScore: activity.interestScore || 0,
            referrer: activity.referrer || null
        });
    }

    for (const note of notesQueue) {
        allRecords.push({
            url: note.url || "",
            title: note.title || `[${note.type}] ${note.tags?.join(", ") || ""}`,
            visitTime: note.timestamp,
            durationSeconds: 0,
            contentSummary: note.content || "",
            pageContent: ""
        });
    }

    const backupActivities = [...activityQueue];
    const backupNotes = [...notesQueue];
    activityQueue = [];
    notesQueue = [];

    try {
        const { serverUrl } = await chrome.storage.sync.get("serverUrl");
        if (!serverUrl) return;

        const response = await fetch(serverUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(allRecords)
        });

        if (!response.ok) {
            throw new Error(`Server error: ${response.status}`);
        }
        console.log("Successfully synced", allRecords.length, "records.");
    } catch (err) {
        console.error("Sync failed, restoring queue:", err);
        activityQueue = [...backupActivities, ...activityQueue];
        notesQueue = [...backupNotes, ...notesQueue];
    }
}

// ============== Helper Functions ==============
function matchWildcard(str, rule) {
    if (!rule.includes('*')) {
        return str.includes(rule);
    }
    const escape = (s) => s.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
    return new RegExp("^" + rule.split("*").map(escape).join(".*") + "$").test(str);
}

// ============== Remote Config Sync ==============
async function syncConfig() {
    try {
        const { serverUrl } = await chrome.storage.sync.get("serverUrl");
        const apiBase = serverUrl.includes('/ingest')
            ? serverUrl.substring(0, serverUrl.indexOf('/ingest'))
            : "http://localhost:8091/api";

        const res = await fetch(`${apiBase}/config/extension_settings`);
        if (res.ok) {
            const data = await res.json();
            if (data.value) {
                const remoteSettings = JSON.parse(data.value);
                // Merge with local settings
                await chrome.storage.sync.set(remoteSettings);
                console.log("Config synced from server");
            }
        } else if (res.status === 404) {
            console.log("No remote config found (404), keeping local settings.");
        } else {
            console.log(`Config sync failed with status: ${res.status}`);
        }
    } catch (e) {
        console.log("Config sync failed (server might be down)", e);
    }
}

// Initial sync on startup
syncConfig();
