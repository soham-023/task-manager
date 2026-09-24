/**
 * Task Manager — Frontend Application Logic
 * Handles CRUD operations, filtering, search, and UI interactions
 * via the REST API at /api/tasks.
 */

const API_URL = '/api/tasks';

// --- DOM Elements ---
const taskListEl = document.getElementById('task-list');
const emptyStateEl = document.getElementById('empty-state');
const modalOverlay = document.getElementById('modal-overlay');
const modalTitle = document.getElementById('modal-title');
const taskForm = document.getElementById('task-form');
const searchInput = document.getElementById('search-input');
const filterStatus = document.getElementById('filter-status');
const filterPriority = document.getElementById('filter-priority');
const filterCategory = document.getElementById('filter-category');
const toastEl = document.getElementById('toast');

// Form fields
const inputTitle = document.getElementById('input-title');
const inputDescription = document.getElementById('input-description');
const inputPriority = document.getElementById('input-priority');
const inputStatus = document.getElementById('input-status');
const inputCategory = document.getElementById('input-category');
const inputDueDate = document.getElementById('input-due-date');
const titleChars = document.getElementById('title-chars');
const descChars = document.getElementById('desc-chars');

// Stats
const statTotal = document.getElementById('stat-total');
const statTodo = document.getElementById('stat-todo');
const statInProgress = document.getElementById('stat-in-progress');
const statDone = document.getElementById('stat-done');
const statOverdue = document.getElementById('stat-overdue');

// State
let editingTaskId = null;
let searchTimeout = null;

// ==========================================
// API Functions
// ==========================================

async function fetchTasks(params = {}) {
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    if (params.priority) query.set('priority', params.priority);
    if (params.category) query.set('category', params.category);
    if (params.search) query.set('search', params.search);

    const url = query.toString() ? `${API_URL}?${query}` : API_URL;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to fetch tasks');
    return res.json();
}

async function createTask(task) {
    const res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(task),
    });
    if (!res.ok) {
        const err = await res.json();
        throw new Error(err.fieldErrors ? Object.values(err.fieldErrors).join(', ') : err.message);
    }
    return res.json();
}

async function updateTask(id, task) {
    const res = await fetch(`${API_URL}/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(task),
    });
    if (!res.ok) {
        const err = await res.json();
        throw new Error(err.fieldErrors ? Object.values(err.fieldErrors).join(', ') : err.message);
    }
    return res.json();
}

async function deleteTask(id) {
    const res = await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Failed to delete task');
}

async function updateTaskStatus(id, status) {
    const res = await fetch(`${API_URL}/${id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
    });
    if (!res.ok) throw new Error('Failed to update status');
    return res.json();
}

// ==========================================
// Rendering
// ==========================================

function getDueDateBadge(dueDateStr, status) {
    if (!dueDateStr) return '';

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const [year, month, day] = dueDateStr.split('-').map(Number);
    const dueDate = new Date(year, month - 1, day);
    dueDate.setHours(0, 0, 0, 0);

    const formatted = dueDate.toLocaleDateString('en-IN', {
        day: 'numeric', month: 'short', year: 'numeric'
    });

    if (status === 'DONE') {
        return `<span class="badge badge-due">📅 Due: ${formatted}</span>`;
    }

    const diffTime = dueDate.getTime() - today.getTime();
    const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 0) {
        const daysAgo = Math.abs(diffDays);
        return `<span class="badge badge-overdue">🚨 Overdue (${daysAgo}d ago)</span>`;
    } else if (diffDays === 0) {
        return `<span class="badge badge-due-today">⏳ Due Today</span>`;
    } else if (diffDays === 1) {
        return `<span class="badge badge-due">⏰ Due Tomorrow</span>`;
    } else {
        return `<span class="badge badge-due">📅 Due: ${formatted}</span>`;
    }
}

function getCategoryBadge(category) {
    const cat = category || 'OTHER';
    const labels = {
        WORK: '💼 Work',
        PERSONAL: '🏠 Personal',
        STUDY: '📚 Study',
        FINANCE: '💰 Finance',
        OTHER: '📌 Other'
    };
    return `<span class="badge badge-category-${cat}">${labels[cat] || cat}</span>`;
}

function renderTasks(tasks) {
    taskListEl.innerHTML = '';

    if (tasks.length === 0) {
        taskListEl.style.display = 'none';
        emptyStateEl.style.display = 'block';
        return;
    }

    taskListEl.style.display = 'flex';
    emptyStateEl.style.display = 'none';

    tasks.forEach(task => {
        const card = document.createElement('div');
        card.className = `task-card priority-${task.priority} status-${task.status}`;
        card.dataset.id = task.id;

        const isChecked = task.status === 'DONE' ? 'checked' : '';
        const createdDate = new Date(task.createdAt).toLocaleDateString('en-IN', {
            day: 'numeric', month: 'short', year: 'numeric'
        });

        card.innerHTML = `
            <div class="task-checkbox">
                <input type="checkbox" ${isChecked} title="Toggle done" />
            </div>
            <div class="task-body">
                <div class="task-title">${escapeHtml(task.title)}</div>
                ${task.description ? `<div class="task-description">${escapeHtml(task.description)}</div>` : ''}
                <div class="task-meta">
                    <span class="badge badge-priority-${task.priority}">${task.priority}</span>
                    <span class="badge badge-status-${task.status}">${formatStatus(task.status)}</span>
                    ${getCategoryBadge(task.category)}
                    ${getDueDateBadge(task.dueDate, task.status)}
                    <span class="task-date">Created: ${createdDate}</span>
                </div>
            </div>
            <div class="task-actions">
                <button class="btn-icon" title="Edit" data-action="edit">✏️</button>
                <button class="btn-icon" title="Delete" data-action="delete">🗑️</button>
            </div>
        `;

        // Checkbox toggle
        card.querySelector('input[type="checkbox"]').addEventListener('change', async (e) => {
            const newStatus = e.target.checked ? 'DONE' : 'TODO';
            try {
                await updateTaskStatus(task.id, newStatus);
                showToast(e.target.checked ? 'Task completed! 🎉' : 'Task reopened', 'success');
                loadTasks();
            } catch (err) {
                showToast(err.message, 'error');
                e.target.checked = !e.target.checked;
            }
        });

        // Edit button
        card.querySelector('[data-action="edit"]').addEventListener('click', () => openEditModal(task));

        // Delete button
        card.querySelector('[data-action="delete"]').addEventListener('click', async () => {
            if (!confirm(`Delete "${task.title}"?`)) return;
            try {
                await deleteTask(task.id);
                showToast('Task deleted', 'success');
                loadTasks();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });

        taskListEl.appendChild(card);
    });
}

function updateStats(tasks) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const overdueCount = tasks.filter(t => {
        if (t.status === 'DONE' || !t.dueDate) return false;
        const [y, m, d] = t.dueDate.split('-').map(Number);
        const due = new Date(y, m - 1, d);
        due.setHours(0, 0, 0, 0);
        return due < today;
    }).length;

    statTotal.textContent = tasks.length;
    statTodo.textContent = tasks.filter(t => t.status === 'TODO').length;
    statInProgress.textContent = tasks.filter(t => t.status === 'IN_PROGRESS').length;
    statDone.textContent = tasks.filter(t => t.status === 'DONE').length;
    statOverdue.textContent = overdueCount;
}

// ==========================================
// Load Tasks (with filters)
// ==========================================

async function loadTasks() {
    try {
        const params = {};
        const search = searchInput.value.trim();
        const status = filterStatus.value;
        const priority = filterPriority.value;
        const category = filterCategory.value;

        if (search) params.search = search;
        if (status) params.status = status;
        if (priority) params.priority = priority;
        if (category) params.category = category;

        const tasks = await fetchTasks(params);
        renderTasks(tasks);

        // Always fetch all tasks for accurate stats
        const allTasks = (search || status || priority || category) ? await fetchTasks() : tasks;
        updateStats(allTasks);
    } catch (err) {
        showToast('Failed to load tasks', 'error');
    }
}

// ==========================================
// Modal
// ==========================================

function openNewModal() {
    editingTaskId = null;
    modalTitle.textContent = 'New Task';
    taskForm.reset();
    inputPriority.value = 'MEDIUM';
    inputStatus.value = 'TODO';
    inputCategory.value = 'OTHER';
    inputDueDate.value = '';
    titleChars.textContent = '0';
    descChars.textContent = '0';
    document.getElementById('btn-save').textContent = 'Save Task';
    modalOverlay.classList.add('active');
    inputTitle.focus();
}

function openEditModal(task) {
    editingTaskId = task.id;
    modalTitle.textContent = 'Edit Task';
    inputTitle.value = task.title;
    inputDescription.value = task.description || '';
    inputPriority.value = task.priority;
    inputStatus.value = task.status;
    inputCategory.value = task.category || 'OTHER';
    inputDueDate.value = task.dueDate || '';
    titleChars.textContent = task.title.length;
    descChars.textContent = (task.description || '').length;
    document.getElementById('btn-save').textContent = 'Update Task';
    modalOverlay.classList.add('active');
    inputTitle.focus();
}

function closeModal() {
    modalOverlay.classList.remove('active');
    editingTaskId = null;
}

// ==========================================
// Toast Notification
// ==========================================

function showToast(message, type = 'success') {
    toastEl.textContent = message;
    toastEl.className = `toast ${type} show`;
    setTimeout(() => {
        toastEl.classList.remove('show');
    }, 2500);
}

// ==========================================
// Utility
// ==========================================

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatStatus(status) {
    return status.replace('_', ' ').replace(/\b\w/g, c => c.toUpperCase());
}

// ==========================================
// Event Listeners
// ==========================================

// New task button
document.getElementById('btn-new-task').addEventListener('click', openNewModal);

// Close modal
document.getElementById('btn-close-modal').addEventListener('click', closeModal);
document.getElementById('btn-cancel').addEventListener('click', closeModal);
modalOverlay.addEventListener('click', (e) => {
    if (e.target === modalOverlay) closeModal();
});

// Escape key closes modal
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeModal();
});

// Character counters
inputTitle.addEventListener('input', () => {
    titleChars.textContent = inputTitle.value.length;
});
inputDescription.addEventListener('input', () => {
    descChars.textContent = inputDescription.value.length;
});

// Form submit — Create or Update
taskForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const task = {
        title: inputTitle.value.trim(),
        description: inputDescription.value.trim() || null,
        priority: inputPriority.value,
        status: inputStatus.value,
        category: inputCategory.value || 'OTHER',
        dueDate: inputDueDate.value || null,
    };

    if (!task.title) {
        showToast('Title is required', 'error');
        return;
    }

    try {
        if (editingTaskId) {
            await updateTask(editingTaskId, task);
            showToast('Task updated ✅', 'success');
        } else {
            await createTask(task);
            showToast('Task created ✅', 'success');
        }
        closeModal();
        loadTasks();
    } catch (err) {
        showToast(err.message, 'error');
    }
});

// Search (debounced)
searchInput.addEventListener('input', () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(loadTasks, 300);
});

// Filters
filterStatus.addEventListener('change', loadTasks);
filterPriority.addEventListener('change', loadTasks);
filterCategory.addEventListener('change', loadTasks);

// ==========================================
// Initial Load
// ==========================================
loadTasks();
