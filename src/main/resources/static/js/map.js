// map.js — Clean, Fast Distance Calculation & Leaflet Map Display

let map;
let routeLayer;
let startMarker;
let endMarker;

function getAuthHeaders() {
    const token = localStorage.getItem('jwt_token');
    return token ? { 'Authorization': 'Bearer ' + token } : {};
}

function initMap(containerId, lat = 20.5937, lng = 78.9629, zoom = 5) {
    if (map) return map;
    map = L.map(containerId).setView([lat, lng], zoom);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(map);
    return map;
}

function generateRoute() {
    const startLocation = document.getElementById('startLocation').value.trim();
    const endLocation = document.getElementById('endLocation').value.trim();
    if (!startLocation || !endLocation) {
        showToast('Please enter both start and end locations', 'error');
        return;
    }
    
    const btn = document.getElementById('generate-btn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Calculating Distance...';
    
    const loadingEl = document.getElementById('route-loading');
    const resultEl = document.getElementById('route-result');
    
    if (loadingEl) loadingEl.classList.remove('d-none');
    if (resultEl) resultEl.classList.add('d-none');

    fetch('/api/routes/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify({ startLocation, endLocation })
    })
    .then(r => r.json())
    .then(data => {
        if (data.routeId || data.id) {
            displayRoute(data);
            if (resultEl) resultEl.classList.remove('d-none');
            
            const distVal = data.distance || 0;
            document.getElementById('result-distance').textContent = distVal.toFixed(2) + ' km';
            document.getElementById('result-time').textContent = data.estimatedTime || '--';
            
            window.currentDistanceKm = distVal;
            if (typeof updateFarePrice === 'function') {
                updateFarePrice();
            }
            
            showToast('Distance calculated successfully!', 'success');
        } else {
            showToast(data.message || 'Failed to calculate distance', 'error');
        }
    })
    .catch(err => {
        showToast('Calculation error: ' + err.message, 'error');
    })
    .finally(() => {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-speedometer2 me-2"></i>Calculate Distance Instantly';
        if (loadingEl) loadingEl.classList.add('d-none');
    });
}

function displayRoute(routeData) {
    if (!map) initMap('map');
    
    // Clear existing markers & polyline
    if (routeLayer) map.removeLayer(routeLayer);
    if (startMarker) map.removeLayer(startMarker);
    if (endMarker) map.removeLayer(endMarker);

    const startLat = routeData.startLat || 19.0760;
    const startLng = routeData.startLng || 72.8777;
    const endLat = routeData.endLat || 28.7041;
    const endLng = routeData.endLng || 77.1025;

    // Custom Icon Pins
    const pickupIcon = L.divIcon({
        html: '<div style="background:#4f46e5;color:#ffffff;width:30px;height:30px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:bold;box-shadow:0 4px 10px rgba(0,0,0,0.3);border:2px solid #fff;">A</div>',
        className: 'custom-map-pin',
        iconSize: [30, 30],
        iconAnchor: [15, 15]
    });

    const dropoffIcon = L.divIcon({
        html: '<div style="background:#ef4444;color:#ffffff;width:30px;height:30px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:bold;box-shadow:0 4px 10px rgba(0,0,0,0.3);border:2px solid #fff;">B</div>',
        className: 'custom-map-pin',
        iconSize: [30, 30],
        iconAnchor: [15, 15]
    });

    startMarker = L.marker([startLat, startLng], { icon: pickupIcon })
        .addTo(map)
        .bindPopup('<b>Pickup (A):</b> ' + routeData.startLocation);

    endMarker = L.marker([endLat, endLng], { icon: dropoffIcon })
        .addTo(map)
        .bindPopup('<b>Destination (B):</b> ' + routeData.endLocation);

    routeLayer = L.polyline(
        [[startLat, startLng], [endLat, endLng]],
        { color: '#4f46e5', weight: 4, opacity: 0.85, dashArray: '6, 6' }
    ).addTo(map);

    const bounds = L.latLngBounds([startLat, startLng], [endLat, endLng]);
    map.fitBounds(bounds, { padding: [50, 50] });
}

document.addEventListener('DOMContentLoaded', function() {
    if (document.getElementById('map')) {
        initMap('map');
    }

    const genBtn = document.getElementById('generate-btn');
    if (genBtn) {
        genBtn.addEventListener('click', generateRoute);
    }
});
