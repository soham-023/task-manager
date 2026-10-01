/**
 * Task Manager — Frontend Application Logic
 * Handles CRUD operations, filtering, search, sorting, pagination, and UI interactions
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
const sortBySelect = document.getElementById('sort-by');
const toastEl = document.getElementById('toast');

// Pagination elements
const paginationBar = document.getElementById('pagination-bar');
const btnPrevPage = document.getElementById('btn-prev-page');
const btnNextPage = document.getElementById('btn-next-page');
const pageNumbersEl = document.getElementById('page-numbers');
const selectPageSize = document.getElementById('select-page-size');

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
let currentPage = 0;
let pageSize = 6;
let currentSort = 'createdAt';
let currentSortDir = 'desc';
let totalPages = 0;

// ==========================================
// API Functions
// ==========================================

async function fetchTasks(params = {}) {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', params.page);
    if (params.size !== undefined) query.set('size', params.size);
    if (params.sortBy) query.set('sortBy', params.sortBy);
    if (params.sortDir) query.set('sortDir', params.sortDir);
    if (params.status) query.set('status', params.status);
    if (params.priority) query.set('priority', params.priority);
    if (params.category) query.set('category', params.category);
    if (params.search) query.set('search', params.search);

    const url = query.toString() ? `${API_URL}?${query}` : API_URL;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to fetch tasks');
    return res.json();
}

async function fetchStats() {
    const res = await fetch(`${API_URL}/stats`);
    if (!res.ok) throw new Error('Failed to fetch stats');
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

    if (!tasks || tasks.length === 0) {
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

function renderPagination(pageData) {
    totalPages = pageData.totalPages;
    currentPage = pageData.number;

    if (!pageData.totalElements || pageData.totalElements === 0) {
        paginationBar.style.display = 'none';
        return;
    }

    paginationBar.style.display = 'flex';
    btnPrevPage.disabled = pageData.first;
    btnNextPage.disabled = pageData.last;

    // Render page buttons
    pageNumbersEl.innerHTML = '';
    for (let i = 0; i < totalPages; i++) {
        const btn = document.createElement('button');
        btn.className = `page-num-btn ${i === currentPage ? 'active' : ''}`;
        btn.textContent = i + 1;
        btn.title = `Go to page ${i + 1}`;
        btn.addEventListener('click', () => {
            if (i !== currentPage) {
                currentPage = i;
                loadTasks();
            }
        });
        pageNumbersEl.appendChild(btn);
    }
}

function updateStats(stats) {
    statTotal.textContent = stats.total || 0;
    statTodo.textContent = stats.todo || 0;
    statInProgress.textContent = stats.inProgress || 0;
    statDone.textContent = stats.done || 0;
    statOverdue.textContent = stats.overdue || 0;
}

// ==========================================
// Load Tasks & Stats
// ==========================================

async function loadStats() {
    try {
        const stats = await fetchStats();
        updateStats(stats);
    } catch (err) {
        console.error('Failed to load stats:', err);
    }
}

async function loadTasks() {
    try {
        const params = {
            page: currentPage,
            size: pageSize,
            sortBy: currentSort,
            sortDir: currentSortDir,
        };

        const search = searchInput.value.trim();
        const status = filterStatus.value;
        const priority = filterPriority.value;
        const category = filterCategory.value;

        if (search) params.search = search;
        if (status) params.status = status;
        if (priority) params.priority = priority;
        if (category) params.category = category;

        const data = await fetchTasks(params);

        // Edge case: if current page has no tasks after a deletion, go to previous page
        if (data.content.length === 0 && currentPage > 0) {
            currentPage = Math.max(0, data.totalPages - 1);
            return loadTasks();
        }

        renderTasks(data.content);
        renderPagination(data);
        loadStats();
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
    searchTimeout = setTimeout(() => {
        currentPage = 0;
        loadTasks();
    }, 300);
});

// Filters
filterStatus.addEventListener('change', () => {
    currentPage = 0;
    loadTasks();
});
filterPriority.addEventListener('change', () => {
    currentPage = 0;
    loadTasks();
});
filterCategory.addEventListener('change', () => {
    currentPage = 0;
    loadTasks();
});

// Sort
sortBySelect.addEventListener('change', (e) => {
    const [field, dir] = e.target.value.split(',');
    currentSort = field;
    currentSortDir = dir;
    currentPage = 0;
    loadTasks();
});

// Page size selector
selectPageSize.addEventListener('change', (e) => {
    pageSize = parseInt(e.target.value, 10);
    currentPage = 0;
    loadTasks();
});

// Pagination buttons
btnPrevPage.addEventListener('click', () => {
    if (currentPage > 0) {
        currentPage--;
        loadTasks();
    }
});

btnNextPage.addEventListener('click', () => {
    if (currentPage < totalPages - 1) {
        currentPage++;
        loadTasks();
    }
});

// ==========================================
// Initial Load
// ==========================================
loadTasks();
