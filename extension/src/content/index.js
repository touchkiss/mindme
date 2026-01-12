// content.js - MindMe Content Script with Engagement Tracking

// ============== Engagement Tracking ==============
let maxScrollDepth = 0;
let interactionCount = 0;
let lastScrollUpdate = 0;
let activeReadingSeconds = 0;
let lastActivityTime = 0;
let isUserActive = false;
let activityTimer = null;

// Mark user as active
function userActivity() {
    lastActivityTime = Date.now();
    if (!isUserActive) {
        isUserActive = true;
        // Start counting if not already
        if (!activityTimer) {
            activityTimer = setInterval(() => {
                if (Date.now() - lastActivityTime < 5000) { // Considered active if activity within last 5s
                    activeReadingSeconds++;
                } else {
                    isUserActive = false;
                    clearInterval(activityTimer);
                    activityTimer = null;
                }
            }, 1000);
        }
    }
}

// Track scroll depth
function updateScrollDepth() {
    userActivity();
    const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
    if (scrollHeight <= 0) {
        maxScrollDepth = 100;
        return;
    }

    const currentScroll = window.scrollY;
    const currentDepth = Math.round((currentScroll / scrollHeight) * 100);
    maxScrollDepth = Math.max(maxScrollDepth, currentDepth);

    // Throttle updates to background
    const now = Date.now();
    if (now - lastScrollUpdate > 2000) { // Every 2 seconds max
        lastScrollUpdate = now;
        chrome.runtime.sendMessage({
            type: "ENGAGEMENT_UPDATE",
            scrollDepth: maxScrollDepth,
            activeSeconds: activeReadingSeconds
        }).catch(() => { }); // Ignore errors if background not ready
    }
}

// Track interactions
function trackInteraction(type) {
    userActivity();
    interactionCount++;
    chrome.runtime.sendMessage({
        type: "ENGAGEMENT_UPDATE",
        interaction: type,
        activeSeconds: activeReadingSeconds
    }).catch(() => { });
}

// Event listeners
document.addEventListener("scroll", updateScrollDepth, { passive: true });
document.addEventListener("click", () => trackInteraction("click"));
document.addEventListener("keydown", () => userActivity());
document.addEventListener("mousemove", () => userActivity());
// Active Reading / Selection Tracking
let lastSelection = "";
let selectionTimeout = null;

document.addEventListener("selectionchange", () => {
    const selection = window.getSelection().toString().trim();
    if (selection && selection.length > 2 && selection !== lastSelection) {
        lastSelection = selection;

        // Debounce sending to background
        if (selectionTimeout) clearTimeout(selectionTimeout);
        selectionTimeout = setTimeout(() => {
            console.log("Sending selection:", selection);
            chrome.runtime.sendMessage({
                type: 'SELECTION_UPDATE',
                text: selection
            }).catch(() => { });
        }, 1000); // Wait 1s after selection stabilizes
    }
});

document.addEventListener("mouseup", (e) => {
    userActivity();
    // Immediate check for text selection (handled by selectionchange mostly)
    const selection = window.getSelection();
    if (selection && selection.toString().trim().length > 0) {
        trackInteraction("selection");
    }
});

// Sync active seconds periodically even if no scroll/interaction
setInterval(() => {
    if (activeReadingSeconds > 0) {
        chrome.runtime.sendMessage({
            type: "ENGAGEMENT_UPDATE",
            activeSeconds: activeReadingSeconds
        }).catch(() => { });
    }
}, 5000);

// Initial scroll check
setTimeout(updateScrollDepth, 1000);

// ============== Message Handling ==============
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.type === "SHOW_QUICK_NOTE") {
        showQuickNoteOverlay();
    }
});

// ============== Quick Note Overlay ==============
function showQuickNoteOverlay() {
    if (document.getElementById("mindme-quick-note-overlay")) {
        document.getElementById("mindme-quick-note-overlay").remove();
    }

    const overlay = document.createElement("div");
    overlay.id = "mindme-quick-note-overlay";
    overlay.innerHTML = `
        <div class="mindme-overlay-backdrop"></div>
        <div class="mindme-note-modal">
            <div class="mindme-modal-header">
                <span class="mindme-modal-logo">🧠</span>
                <span class="mindme-modal-title">Quick Note</span>
                <button class="mindme-close-btn">&times;</button>
            </div>
            <textarea class="mindme-note-textarea" placeholder="What's on your mind?" autofocus></textarea>
            <div class="mindme-modal-footer">
                <div class="mindme-tags">
                    <span class="mindme-tag" data-tag="idea">idea</span>
                    <span class="mindme-tag" data-tag="todo">todo</span>
                    <span class="mindme-tag" data-tag="important">important</span>
                    <span class="mindme-tag" data-tag="research">research</span>
                </div>
                <button class="mindme-save-btn">Save</button>
            </div>
        </div>
    `;

    const style = document.createElement("style");
    style.id = "mindme-quick-note-styles";
    style.textContent = `
        #mindme-quick-note-overlay {
            position: fixed; top: 0; left: 0; right: 0; bottom: 0;
            z-index: 999999;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        .mindme-overlay-backdrop {
            position: absolute; top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0, 0, 0, 0.5); backdrop-filter: blur(4px);
        }
        .mindme-note-modal {
            position: absolute; top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            width: 400px; background: #0f172a;
            border-radius: 16px; border: 1px solid #475569;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            animation: modalSlideIn 0.2s ease;
        }
        @keyframes modalSlideIn {
            from { opacity: 0; transform: translate(-50%, -45%); }
            to { opacity: 1; transform: translate(-50%, -50%); }
        }
        .mindme-modal-header {
            display: flex; align-items: center;
            padding: 16px 20px; border-bottom: 1px solid #475569;
        }
        .mindme-modal-logo { font-size: 20px; margin-right: 10px; }
        .mindme-modal-title { flex: 1; font-size: 16px; font-weight: 600; color: #f1f5f9; }
        .mindme-close-btn {
            background: none; border: none; color: #94a3b8;
            font-size: 24px; cursor: pointer; padding: 0; line-height: 1;
        }
        .mindme-close-btn:hover { color: #f1f5f9; }
        .mindme-note-textarea {
            width: 100%; min-height: 120px; padding: 16px 20px;
            background: transparent; border: none; color: #f1f5f9;
            font-size: 14px; resize: none; outline: none;
        }
        .mindme-note-textarea::placeholder { color: #64748b; }
        .mindme-modal-footer {
            display: flex; justify-content: space-between; align-items: center;
            padding: 12px 20px; border-top: 1px solid #475569;
        }
        .mindme-tags { display: flex; gap: 6px; }
        .mindme-tag {
            padding: 4px 8px; background: #334155; border-radius: 4px;
            font-size: 11px; color: #94a3b8; cursor: pointer; transition: all 0.2s;
        }
        .mindme-tag:hover, .mindme-tag.active { background: #6366f1; color: white; }
        .mindme-save-btn {
            padding: 8px 20px;
            background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
            border: none; border-radius: 8px; color: white;
            font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s;
        }
        .mindme-save-btn:hover {
            transform: scale(1.02);
            box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
        }
    `;

    document.head.appendChild(style);
    document.body.appendChild(overlay);

    const closeBtn = overlay.querySelector(".mindme-close-btn");
    const backdrop = overlay.querySelector(".mindme-overlay-backdrop");
    const saveBtn = overlay.querySelector(".mindme-save-btn");
    const textarea = overlay.querySelector(".mindme-note-textarea");
    const tags = overlay.querySelectorAll(".mindme-tag");

    let selectedTags = [];

    function close() {
        overlay.remove();
        document.getElementById("mindme-quick-note-styles")?.remove();
    }

    closeBtn.addEventListener("click", close);
    backdrop.addEventListener("click", close);

    tags.forEach(tag => {
        tag.addEventListener("click", () => {
            const tagValue = tag.dataset.tag;
            if (selectedTags.includes(tagValue)) {
                selectedTags = selectedTags.filter(t => t !== tagValue);
                tag.classList.remove("active");
            } else {
                selectedTags.push(tagValue);
                tag.classList.add("active");
            }
        });
    });

    saveBtn.addEventListener("click", () => {
        const content = textarea.value.trim();
        if (!content) return;

        const note = {
            type: "QUICK_NOTE",
            content: content,
            tags: selectedTags,
            timestamp: new Date().toISOString(),
            url: window.location.href,
            title: document.title
        };

        chrome.runtime.sendMessage({ type: "SAVE_NOTE", data: note }, () => {
            close();
            showToast("Note saved!");
        });
    });

    textarea.addEventListener("keydown", (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
            saveBtn.click();
        }
        if (e.key === "Escape") {
            close();
        }
    });

    setTimeout(() => textarea.focus(), 100);
}

function showToast(message) {
    const toast = document.createElement("div");
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed; bottom: 20px; right: 20px;
        background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
        color: white; padding: 12px 24px; border-radius: 8px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        font-size: 14px; font-weight: 500; z-index: 999999;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
        animation: slideIn 0.3s ease;
    `;
    const style = document.createElement("style");
    style.textContent = `
        @keyframes slideIn {
            from { transform: translateX(100px); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
    `;
    document.head.appendChild(style);
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
}

// ============== Floating Action Button ==============
function createFloatingButton() {
    if (document.getElementById("mindme-fab")) return;

    const fab = document.createElement("div");
    fab.id = "mindme-fab";
    fab.innerHTML = "🧠";
    fab.style.cssText = `
        position: fixed; bottom: 20px; right: 20px;
        width: 48px; height: 48px;
        background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
        border-radius: 50%; display: flex; align-items: center; justify-content: center;
        font-size: 22px; cursor: pointer; z-index: 999998;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
        transition: all 0.2s; opacity: 0.8;
    `;

    fab.addEventListener("mouseenter", () => {
        fab.style.transform = "scale(1.1)";
        fab.style.opacity = "1";
    });

    fab.addEventListener("mouseleave", () => {
        fab.style.transform = "scale(1)";
        fab.style.opacity = "0.8";
    });

    fab.addEventListener("click", showQuickNoteOverlay);

    document.body.appendChild(fab);
}

if (document.readyState === "complete") {
    createFloatingButton();
} else {
    window.addEventListener("load", createFloatingButton);
}
