// dashboard.js — Admin Dashboard Charts
function initDeliveryChart(data) {
    const ctx = document.getElementById('deliveryChart');
    if (!ctx) return;
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Pending', 'Assigned', 'Out For Delivery', 'Delivered'],
            datasets: [{
                data: [data.PENDING || 0, data.ASSIGNED || 0, data.OUT_FOR_DELIVERY || 0, data.DELIVERED || 0],
                backgroundColor: ['#f59e0b', '#0ea5e9', '#6366f1', '#10b981'],
                borderWidth: 0,
                hoverOffset: 4
            }]
        },
        options: { 
            responsive: true, 
            cutout: '70%', 
            plugins: { 
                legend: { position: 'bottom' } 
            } 
        }
    });
}

function initInvoiceChart(data) {
    const ctx = document.getElementById('invoiceChart');
    if (!ctx) return;
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Pending', 'Paid'],
            datasets: [{
                label: 'Invoices',
                data: [data.PENDING || 0, data.PAID || 0],
                backgroundColor: ['#f59e0b', '#10b981'],
                borderRadius: 8,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: { 
                y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' } }, 
                x: { grid: { display: false } } 
            }
        }
    });
}
