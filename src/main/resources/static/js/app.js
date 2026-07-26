// app.js — Main application utilities, theme manager & live table search

// ──────────────────────────────────────────────────────────────────────────
// 1. THEME MANAGER (LIGHT / DARK MODE)
// ──────────────────────────────────────────────────────────────────────────
function initTheme() {
    const savedTheme = localStorage.getItem('portx_theme') || 'light';
    setTheme(savedTheme);
}

function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem('portx_theme', theme);
    
    // Update theme toggle icons
    const icons = document.querySelectorAll('.theme-toggle-icon');
    icons.forEach(icon => {
        if (theme === 'dark') {
            icon.className = 'bi bi-sun-fill text-warning theme-toggle-icon';
        } else {
            icon.className = 'bi bi-moon-stars-fill text-dark theme-toggle-icon';
        }
    });
}

function toggleTheme() {
    const currentTheme = localStorage.getItem('portx_theme') || 'light';
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
}

// ──────────────────────────────────────────────────────────────────────────
// 2. SIDEBAR TOGGLE MANAGER (WORKS AT ALL SCREEN WIDTHS & HALF SCREEN)
// ──────────────────────────────────────────────────────────────────────────
function initSidebarToggle() {
    // Restore saved sidebar collapsed state
    if (localStorage.getItem('portx_sidebar_collapsed') === 'true') {
        document.body.classList.add('sidebar-collapsed');
    }

    document.addEventListener('click', function(e) {
        if (e.target.closest('#sidebar-toggle')) {
            document.body.classList.toggle('sidebar-collapsed');
            localStorage.setItem('portx_sidebar_collapsed', document.body.classList.contains('sidebar-collapsed'));
        }
    });
}

// ──────────────────────────────────────────────────────────────────────────
// 3. TOAST & NOTIFICATION SYSTEM
// ──────────────────────────────────────────────────────────────────────────
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }

    const toastId = 'toast-' + Date.now();
    const bgClass = type === 'success' ? 'bg-success' : (type === 'error' ? 'bg-danger' : 'bg-primary');
    
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0 shadow-lg" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body fw-medium">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;
    
    container.insertAdjacentHTML('beforeend', toastHtml);
    const toastEl = document.getElementById(toastId);
    if (window.bootstrap) {
        const bsToast = new bootstrap.Toast(toastEl, { delay: 4000 });
        bsToast.show();
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 4. LIVE TABLE SEARCH SYSTEM (REAL-TIME FILTERING & ENTER KEY SUPPORT)
// ──────────────────────────────────────────────────────────────────────────
function initTableSearch() {
    document.addEventListener('input', function(e) {
        if (e.target && e.target.classList.contains('table-search-input')) {
            filterTable(e.target);
        }
    });

    document.addEventListener('keydown', function(e) {
        if (e.target && e.target.classList.contains('table-search-input') && e.key === 'Enter') {
            e.preventDefault();
            filterTable(e.target);
        }
    });
}

function filterTable(searchInput) {
    const query = searchInput.value.toLowerCase().trim();
    const card = searchInput.closest('.card');
    if (!card) return;

    const table = card.querySelector('table');
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr');
    let visibleCount = 0;

    rows.forEach(row => {
        // Skip empty state or no-result message rows
        if (row.classList.contains('no-search-results') || row.classList.contains('empty-table-row')) {
            return;
        }

        const text = row.textContent.toLowerCase();
        if (text.includes(query)) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    // Handle "No matching records found" dynamic feedback row
    let noResultRow = table.querySelector('.no-search-results');
    if (visibleCount === 0 && query !== '') {
        if (!noResultRow) {
            const colSpan = table.querySelectorAll('thead th').length || 7;
            noResultRow = document.createElement('tr');
            noResultRow.className = 'no-search-results';
            noResultRow.innerHTML = `
                <td colspan="${colSpan}" class="text-center py-5 text-muted">
                    <i class="bi bi-search fs-1 d-block mb-3 text-secondary"></i>
                    <h6 class="fw-bold">No matching records found</h6>
                    <p class="small mb-0">No records match "<span class="fw-bold text-primary">${escapeHtml(query)}</span>"</p>
                </td>
            `;
            table.querySelector('tbody').appendChild(noResultRow);
        } else {
            noResultRow.querySelector('span').textContent = query;
            noResultRow.style.display = '';
        }
    } else if (noResultRow) {
        noResultRow.style.display = 'none';
    }
}

function escapeHtml(text) {
    return text.replace(/[&<>"']/g, function(m) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
    });
}

// ──────────────────────────────────────────────────────────────────────────
// INITIALIZATION
// ──────────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function() {
    initTheme();
    initSidebarToggle();
    initTableSearch();

    // Event delegation for theme toggle buttons
    document.addEventListener('click', function(e) {
        if (e.target.closest('.theme-toggle-btn')) {
            toggleTheme();
        }
    });

    // Enable Bootstrap tooltips if available
    if (window.bootstrap && bootstrap.Tooltip) {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
});
