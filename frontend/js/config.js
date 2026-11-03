/**
 * SecureBank Global Configuration
 *
 * Controls:
 * - API_BASE_URL: Backend Spring Boot server URL
 * - DEMO_MODE: Explicit demo mode toggle. MUST be false by default.
 */
const isLocalDevelopment = ['localhost', '127.0.0.1'].includes(window.location.hostname);
const CONFIG = {
  API_BASE_URL: (window.SECUREBANK_API_BASE_URL ||
    (isLocalDevelopment ? 'http://localhost:8080' : window.location.origin)).replace(/\/+$/, ''),
  DEMO_MODE: false, // Default is strictly false (Normal Mode connecting to real backend)
  APP_NAME: 'SecureBank',
  CURRENCY_SYMBOL: '₹'
};

// Freeze configuration to prevent tampering
Object.freeze(CONFIG);
