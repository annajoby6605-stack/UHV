/**
 * Fill the Pockets - Common API & Auth Client
 */

const API = {
  // Use relative path so it automatically works on whatever port or host the backend is running
  BASE_URL: '/api',

  getToken() {
    return localStorage.getItem('ftp_token');
  },

  setToken(token) {
    localStorage.setItem('ftp_token', token);
  },

  getUser() {
    const u = localStorage.getItem('ftp_user');
    return u ? JSON.parse(u) : null;
  },

  setUser(user) {
    localStorage.setItem('ftp_user', JSON.stringify(user));
  },

  clearAuth() {
    localStorage.removeItem('ftp_token');
    localStorage.removeItem('ftp_user');
  },

  logout() {
    const user = this.getUser();
    this.clearAuth();
    if (user && user.role === 'PANCHAYAT_EMPLOYEE') {
      window.location.href = '/employee-login.html';
    } else {
      window.location.href = '/villager-login.html';
    }
  },

  async request(endpoint, options = {}) {
    const headers = options.headers || {};

    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    options.headers = headers;

    try {
      const response = await fetch(`${this.BASE_URL}${endpoint}`, options);
      const data = await response.json();

      if (!response.ok) {
        if (response.status === 401 && !endpoint.includes('/auth/')) {
          this.clearAuth();
          window.location.href = '/villager-login.html';
        }
        throw new Error(data.message || `Request failed with status ${response.status}`);
      }

      return data;
    } catch (err) {
      console.error('API Error:', err);
      throw err;
    }
  },

  // Auth Endpoints
  registerVillager(data) {
    return this.request('/auth/register-villager', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  loginVillager(email, password) {
    return this.request('/auth/login-villager', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
  },

  initiateEmployeeLogin(email, password) {
    return this.request('/auth/employee/initiate-login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
  },

  verifyEmployeeOtp(email, code) {
    return this.request('/auth/employee/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ email, code })
    });
  },

  requestEmployeeAccess(data) {
    return this.request('/auth/employee/request-access', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  // Public Stats
  getPublicStats() {
    return this.request('/stats/public', { method: 'GET' });
  },

  // Villager Endpoints
  getVillagerDashboard() {
    return this.request('/villager/dashboard', { method: 'GET' });
  },

  getWallet() {
    return this.request('/villager/wallet', { method: 'GET' });
  },

  submitRoadReport(formData) {
    return this.request('/reports/submit', {
      method: 'POST',
      body: formData
    });
  },

  getMyReports() {
    return this.request('/reports/my-reports', { method: 'GET' });
  },

  // Employee Endpoints
  getEmployeeStats() {
    return this.request('/employee/dashboard-stats', { method: 'GET' });
  },

  getEmployeeReports(params = {}) {
    const query = new URLSearchParams(params).toString();
    return this.request(`/employee/reports${query ? '?' + query : ''}`, { method: 'GET' });
  },

  updateRepairStatus(reportId, newStatus, notes) {
    return this.request(`/employee/reports/${reportId}/update-repair-status`, {
      method: 'POST',
      body: JSON.stringify({ newStatus, notes })
    });
  },

  getRepairHistory(reportId) {
    return this.request(`/employee/reports/${reportId}/repair-history`, { method: 'GET' });
  },

  getVillagerSummary(villagerId) {
    return this.request(`/employee/villagers/${villagerId}/summary`, { method: 'GET' });
  },

  // Reward Endpoints
  getCoupons() {
    return this.request('/rewards/coupons', { method: 'GET' });
  },

  redeemCoupon(couponId) {
    return this.request('/rewards/redeem', {
      method: 'POST',
      body: JSON.stringify({ couponId })
    });
  },

  getMyRedemptions() {
    return this.request('/rewards/my-redemptions', { method: 'GET' });
  }
};

// UI Toast Notification Helper
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <span style="font-size: 1.2rem;">${type === 'success' ? '✓' : type === 'error' ? '⚠' : 'ℹ'}</span>
    <div>${message}</div>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Format date helper
function formatDate(dateStr) {
  if (!dateStr) return 'N/A';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}
