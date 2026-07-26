// auth.js — Login and JWT handling
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const btn = document.getElementById('login-btn');
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Signing in...';
            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });
                if (response.ok) {
                    const data = await response.json();
                    saveToken(data.token);
                    window.location.href = '/dashboard';
                } else {
                    const err = await response.json();
                    showError(err.message || 'Invalid credentials');
                }
            } catch (e) {
                showError('Connection error. Please try again.');
            } finally {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-box-arrow-in-right me-2"></i>Sign In';
            }
        });
    }
    
    function showError(msg) {
        const el = document.getElementById('error-msg');
        if (el) { 
            el.textContent = msg; 
            el.classList.remove('d-none'); 
        }
    }
});
