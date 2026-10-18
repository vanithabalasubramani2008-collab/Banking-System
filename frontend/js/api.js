/**
 * Centralized API Service for SecureBank
 * Handles Fetch API requests, headers, JWT tokens, and standardized error parsing.
 */
const ApiService = {
  /**
   * Retrieves the stored JWT token
   */
  getToken() {
    return sessionStorage.getItem('securebank_token');
  },

  /**
   * Saves JWT token and user info upon login
   */
  saveAuth(authResponse) {
    if (authResponse && authResponse.token) {
      sessionStorage.setItem('securebank_token', authResponse.token);
      sessionStorage.setItem('securebank_user', authResponse.username || '');
      sessionStorage.setItem('securebank_role', authResponse.role || '');
    }
  },

  /**
   * Clears auth session
   */
  clearAuth() {
    sessionStorage.removeItem('securebank_token');
    sessionStorage.removeItem('securebank_user');
    sessionStorage.removeItem('securebank_role');
  },

  /**
   * Retrieves current authenticated username
   */
  getCurrentUser() {
    return sessionStorage.getItem('securebank_user') || 'John';
  },

  /**
   * Checks if user has a token in session
   */
  isAuthenticated() {
    return !!this.getToken();
  },

  /**
   * Centralized HTTP Request Wrapper
   */
  async request(endpoint, options = {}) {
    const url = `${CONFIG.API_BASE_URL}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    };

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const config = {
      ...options,
      headers
    };

    try {
      const response = await fetch(url, config);

      // Handle 401 Unauthorized (Expired or Invalid token)
      if (response.status === 401 && !endpoint.includes('/api/auth/login')) {
        this.clearAuth();
        window.location.href = 'index.html?error=session_expired';
        throw new Error('Session expired. Please log in again.');
      }

      // Read response body as text first to handle empty responses
      const responseText = await response.text();
      let responseData = null;
      if (responseText) {
        try {
          responseData = JSON.parse(responseText);
        } catch (e) {
          responseData = { message: responseText };
        }
      }

      if (!response.ok) {
        const error = new Error(
          (responseData && responseData.message) ||
          (responseData && responseData.error) ||
          `Request failed with status ${response.status}`
        );
        error.status = response.status;
        error.data = responseData;
        throw error;
      }

      return responseData;
    } catch (err) {
      // Re-throw with network detail if fetch failed completely
      if (err.name === 'TypeError' && err.message.includes('fetch')) {
        const networkError = new Error('Cannot connect to SecureBank backend server. Please check if the Spring Boot application is running at ' + CONFIG.API_BASE_URL);
        networkError.status = 0;
        throw networkError;
      }
      throw err;
    }
  },

  // Auth endpoints
  login(credentials) {
    return this.request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials)
    });
  },

  register(data) {
    return this.request('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  // Account endpoints
  getAccounts() {
    return this.request('/api/accounts', { method: 'GET' });
  },

  getAccountById(id) {
    return this.request(`/api/accounts/${id}`, { method: 'GET' });
  },

  createAccount(accountData) {
    return this.request('/api/accounts', {
      method: 'POST',
      body: JSON.stringify(accountData)
    });
  },

  getRecentTransactions(accountId, limit = 5) {
    return this.request(`/api/accounts/${accountId}/transactions?limit=${limit}`, { method: 'GET' });
  },

  getAccountStatement(accountId) {
    return this.request(`/api/accounts/${accountId}/statement`, { method: 'GET' });
  },

  applyInterest(accountId) {
    return this.request(`/api/accounts/${accountId}/apply-interest`, { method: 'POST' });
  },

  // Transaction endpoints
  deposit(accountId, depositData) {
    return this.request(`/api/accounts/${accountId}/deposits`, {
      method: 'POST',
      body: JSON.stringify(depositData)
    });
  },

  withdraw(accountId, withdrawalData) {
    return this.request(`/api/accounts/${accountId}/withdrawals`, {
      method: 'POST',
      body: JSON.stringify(withdrawalData)
    });
  },

  transfer(transferData) {
    return this.request('/api/transfers', {
      method: 'POST',
      body: JSON.stringify(transferData)
    });
  }
};
