// app.js — Main application utilities, theme manager, tab filters & live table search

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
// 4. UNIFIED TAB FILTER & LIVE TABLE SEARCH SYSTEM
// ──────────────────────────────────────────────────────────────────────────
function initTableFiltersAndSearch() {
    // Event listener for tab button clicks
    document.addEventListener('click', function(e) {
        const tabBtn = e.target.closest('.filter-tab');
        if (tabBtn) {
            const group = tabBtn.closest('.filter-tab-group');
            if (group) {
                group.querySelectorAll('.filter-tab').forEach(b => b.classList.remove('active'));
                tabBtn.classList.add('active');
                applyTableFilters(tabBtn);
            }
        }
    });

    // Event listener for live search input
    document.addEventListener('input', function(e) {
        if (e.target && e.target.classList.contains('table-search-input')) {
            applyTableFilters(e.target);
        }
    });

    document.addEventListener('keydown', function(e) {
        if (e.target && e.target.classList.contains('table-search-input') && e.key === 'Enter') {
            e.preventDefault();
            applyTableFilters(e.target);
        }
    });

    // Initial count calculation on page load
    document.querySelectorAll('.filter-tab-group').forEach(group => {
        applyTableFilters(group);
    });
}

function applyTableFilters(triggerElement) {
    // Find closest context wrapper or page content area
    const wrapper = triggerElement.closest('.content-area') || triggerElement.closest('.fade-in') || document;
    const activeTab = wrapper.querySelector('.filter-tab-group .filter-tab.active');
    const searchInput = wrapper.querySelector('.table-search-input');
    const table = wrapper.querySelector('table');
    if (!table) return;

    const filterVal = activeTab ? activeTab.getAttribute('data-filter') : 'all';
    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';

    const rows = table.querySelectorAll('tbody tr');
    let visibleCount = 0;

    rows.forEach(row => {
        // Skip empty state or no-result feedback rows
        if (row.classList.contains('no-search-results') || row.classList.contains('empty-table-row')) {
            return;
        }

        const text = row.textContent.toLowerCase();
        const rowStatus = row.getAttribute('data-status') || '';
        const rowCategory = row.getAttribute('data-category') || '';
        const hasVehicleAttr = row.getAttribute('data-has-vehicle');

        // Tab Filter Match Evaluation
        let matchesTab = false;
        if (filterVal === 'all') {
            matchesTab = true;
        } else if (filterVal === 'WITH_VEHICLE') {
            matchesTab = (hasVehicleAttr === 'true');
        } else if (filterVal === 'WITHOUT_VEHICLE') {
            matchesTab = (hasVehicleAttr === 'false');
        } else {
            matchesTab = (rowStatus.toUpperCase() === filterVal.toUpperCase()) || 
                         (rowCategory.toUpperCase() === filterVal.toUpperCase());
        }

        // Search Input Match Evaluation
        let matchesQuery = (query === '') || text.includes(query);

        if (matchesTab && matchesQuery) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    // Handle "No matching records found" message feedback
    let noResultRow = table.querySelector('.no-search-results');
    if (visibleCount === 0 && (query !== '' || filterVal !== 'all')) {
        if (!noResultRow) {
            const colSpan = table.querySelectorAll('thead th').length || 8;
            noResultRow = document.createElement('tr');
            noResultRow.className = 'no-search-results';
            noResultRow.innerHTML = `
                <td colspan="${colSpan}" class="text-center py-5 text-muted">
                    <i class="bi bi-funnel fs-1 d-block mb-3 text-secondary"></i>
                    <h6 class="fw-bold">No matching records found</h6>
                    <p class="small mb-0">No items match the selected tab filter or search query.</p>
                </td>
            `;
            table.querySelector('tbody').appendChild(noResultRow);
        } else {
            noResultRow.style.display = '';
        }
    } else if (noResultRow) {
        noResultRow.style.display = 'none';
    }

    // Recalculate and update tab badge counts dynamically
    updateTabBadgeCounts(wrapper, rows);
}

function updateTabBadgeCounts(wrapper, rows) {
    const tabs = wrapper.querySelectorAll('.filter-tab-group .filter-tab');
    if (!tabs.length) return;

    tabs.forEach(tab => {
        const filterVal = tab.getAttribute('data-filter');
        let count = 0;

        rows.forEach(row => {
            if (row.classList.contains('no-search-results') || row.classList.contains('empty-table-row')) return;

            const rowStatus = row.getAttribute('data-status') || '';
            const rowCategory = row.getAttribute('data-category') || '';
            const hasVehicleAttr = row.getAttribute('data-has-vehicle');

            if (filterVal === 'all') {
                count++;
            } else if (filterVal === 'WITH_VEHICLE') {
                if (hasVehicleAttr === 'true') count++;
            } else if (filterVal === 'WITHOUT_VEHICLE') {
                if (hasVehicleAttr === 'false') count++;
            } else {
                if (rowStatus.toUpperCase() === filterVal.toUpperCase() || rowCategory.toUpperCase() === filterVal.toUpperCase()) {
                    count++;
                }
            }
        });

        const badge = tab.querySelector('.badge');
        if (badge) {
            badge.textContent = count;
        }
    });
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
    initTableFiltersAndSearch();

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
