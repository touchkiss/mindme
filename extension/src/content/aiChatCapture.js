// aiChatCapture.js - Captures AI conversations from ChatGPT, DeepSeek, Gemini

const AI_PLATFORMS = {
    chatgpt: {
        hostPatterns: ['chat.openai.com', 'chatgpt.com'],
        selectors: {
            // ChatGPT uses data attributes for message roles
            userMessage: '[data-message-author-role="user"]',
            assistantMessage: '[data-message-author-role="assistant"]',
            // Fallback selectors
            fallbackUser: '.text-base:has(.whitespace-pre-wrap)',
            fallbackAssistant: '.markdown.prose'
        },
        conversationContainer: 'main',
        // ChatGPT URL: https://chat.openai.com/c/{conversation-id}
        // or https://chatgpt.com/c/{conversation-id}
        idExtractor: (url) => {
            const match = url.match(/\/c\/([a-f0-9-]+)/i);
            return match ? match[1] : null;
        }
    },
    deepseek: {
        hostPatterns: ['chat.deepseek.com'],
        selectors: {
            userMessage: '.fbb737a4', // User message container class
            assistantMessage: '.ds-markdown', // Assistant response
            fallbackUser: '[class*="user"]',
            fallbackAssistant: '[class*="assistant"]'
        },
        conversationContainer: '.chat-container, main',
        // DeepSeek URL: https://chat.deepseek.com/a/chat/s/{session-id}
        // or variations
        idExtractor: (url) => {
            const match = url.match(/\/chat\/[a-z]\/([a-zA-Z0-9_-]+)/i)
                || url.match(/\/chat\/([a-zA-Z0-9_-]+)/i);
            return match ? match[1] : null;
        }
    },
    gemini: {
        hostPatterns: ['gemini.google.com'],
        selectors: {
            userMessage: 'user-query, .user-query-container, [data-query]',
            assistantMessage: 'model-response, .model-response-container, [data-response]',
            fallbackUser: '.query-content',
            fallbackAssistant: '.response-content'
        },
        conversationContainer: 'main, .chat-window',
        // Gemini URL: https://gemini.google.com/app/{conversation-id}
        idExtractor: (url) => {
            const match = url.match(/\/app\/([a-zA-Z0-9_-]+)/i);
            return match ? match[1] : null;
        }
    }
};

let currentPlatform = null;
let lastCapturedContent = '';
let captureDebounceTimer = null;
let observer = null;

// Extract conversation ID from URL
function extractConversationId(platform) {
    if (!platform || !platform.config.idExtractor) {
        return null;
    }
    const url = window.location.href;
    const id = platform.config.idExtractor(url);
    if (id) {
        console.log(`[MindMe] Extracted conversation ID: ${id}`);
    }
    return id;
}

// Detect which AI platform we're on
function detectPlatform() {
    const hostname = window.location.hostname;
    for (const [name, config] of Object.entries(AI_PLATFORMS)) {
        if (config.hostPatterns.some(pattern => hostname.includes(pattern))) {
            console.log(`[MindMe] Detected AI platform: ${name}`);
            return { name, config };
        }
    }
    return null;
}

// Extract text content safely
function extractText(element) {
    if (!element) return '';
    // Clone to avoid modifying original
    const clone = element.cloneNode(true);
    // Remove hidden elements, buttons, etc.
    clone.querySelectorAll('button, .sr-only, [aria-hidden="true"], script, style').forEach(el => el.remove());
    return clone.textContent?.trim() || '';
}

// Extract all messages from the conversation
function extractConversation(config) {
    const messages = [];

    // Try primary selectors first
    const userMessages = document.querySelectorAll(config.selectors.userMessage);
    const assistantMessages = document.querySelectorAll(config.selectors.assistantMessage);

    // If primary selectors fail, try fallback
    const userMsgs = userMessages.length > 0 ? userMessages : document.querySelectorAll(config.selectors.fallbackUser);
    const assistantMsgs = assistantMessages.length > 0 ? assistantMessages : document.querySelectorAll(config.selectors.fallbackAssistant);

    // Interleave messages based on DOM order
    const allMessages = [];

    userMsgs.forEach(el => {
        const text = extractText(el);
        if (text.length > 5) { // Filter out empty or trivial content
            allMessages.push({
                role: 'user',
                content: text,
                element: el,
                position: el.getBoundingClientRect().top
            });
        }
    });

    assistantMsgs.forEach(el => {
        const text = extractText(el);
        if (text.length > 10) {
            allMessages.push({
                role: 'assistant',
                content: text,
                element: el,
                position: el.getBoundingClientRect().top
            });
        }
    });

    // Sort by vertical position to maintain conversation order
    allMessages.sort((a, b) => a.position - b.position);

    return allMessages.map(({ role, content }) => ({ role, content }));
}

// Format conversation for storage
function formatConversation(messages) {
    return messages.map(m => `[${m.role.toUpperCase()}]\n${m.content}`).join('\n\n---\n\n');
}

// Send conversation to background script
function reportConversation(messages, platform) {
    if (messages.length === 0) return;

    const formatted = formatConversation(messages);

    // Avoid duplicate reports
    if (formatted === lastCapturedContent) return;
    lastCapturedContent = formatted;

    // Extract native conversation ID from URL
    const conversationId = extractConversationId(currentPlatform);

    console.log(`[MindMe] Capturing ${messages.length} messages from ${platform}${conversationId ? ` (ID: ${conversationId})` : ''}`);

    // Get last user question and assistant answer for summary
    const lastUserMsg = messages.filter(m => m.role === 'user').pop();
    const lastAssistantMsg = messages.filter(m => m.role === 'assistant').pop();

    const summary = lastUserMsg
        ? `Q: ${lastUserMsg.content.substring(0, 100)}${lastUserMsg.content.length > 100 ? '...' : ''}`
        : 'AI Conversation';

    chrome.runtime.sendMessage({
        type: 'AI_CONVERSATION',
        data: {
            conversationId: conversationId, // Native platform conversation ID
            platform: platform,
            messageCount: messages.length,
            summary: summary,
            fullConversation: formatted,
            lastUserQuery: lastUserMsg?.content || '',
            lastAssistantResponse: lastAssistantMsg?.content || '',
            timestamp: new Date().toISOString(),
            url: window.location.href
        }
    }).catch(e => console.warn('[MindMe] Failed to send AI conversation', e));
}

// Debounced capture function
function captureWithDebounce() {
    if (!currentPlatform) return;

    if (captureDebounceTimer) {
        clearTimeout(captureDebounceTimer);
    }

    // Wait 2 seconds after last DOM change before capturing
    // This ensures the AI has finished streaming its response
    captureDebounceTimer = setTimeout(() => {
        const messages = extractConversation(currentPlatform.config);
        reportConversation(messages, currentPlatform.name);
    }, 2000);
}

// Set up MutationObserver to watch for new messages
function setupObserver(config) {
    if (observer) {
        observer.disconnect();
    }

    const containerSelector = config.conversationContainer;
    const container = document.querySelector(containerSelector) || document.body;

    observer = new MutationObserver((mutations) => {
        // Check if any mutation is relevant (new messages, not just attributes)
        const hasRelevantChanges = mutations.some(m =>
            m.type === 'childList' && m.addedNodes.length > 0
        );

        if (hasRelevantChanges) {
            captureWithDebounce();
        }
    });

    observer.observe(container, {
        childList: true,
        subtree: true,
        characterData: true
    });

    console.log('[MindMe] AI chat observer started');
}

// Initialize AI chat capture
export function initAIChatCapture() {
    currentPlatform = detectPlatform();

    if (!currentPlatform) {
        return false; // Not an AI chat platform
    }

    console.log(`[MindMe] Initializing AI chat capture for ${currentPlatform.name}`);

    // Wait for DOM to be ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => setupObserver(currentPlatform.config));
    } else {
        // Small delay to let the app initialize
        setTimeout(() => setupObserver(currentPlatform.config), 1000);
    }

    // Also capture on page unload to get final state
    window.addEventListener('beforeunload', () => {
        if (currentPlatform) {
            const messages = extractConversation(currentPlatform.config);
            reportConversation(messages, currentPlatform.name);
        }
    });

    return true;
}

// Manual trigger for testing
export function captureNow() {
    if (!currentPlatform) {
        console.log('[MindMe] Not on AI platform');
        return;
    }
    const messages = extractConversation(currentPlatform.config);
    console.log('[MindMe] Manual capture:', messages);
    reportConversation(messages, currentPlatform.name);
}

// Expose to window for debugging
if (typeof window !== 'undefined') {
    window.mindmeAICapture = { captureNow, detectPlatform };
}
