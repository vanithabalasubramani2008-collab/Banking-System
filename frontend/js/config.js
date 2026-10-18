/**
 * SecureBank Global Configuration
 *
 * Controls:
 * - API_BASE_URL: Backend Spring Boot server URL
 * - DEMO_MODE: Explicit demo mode toggle. MUST be false by default.
 */
const CONFIG = {
  API_BASE_URL: 'http://localhost:8080',
  DEMO_MODE: false, // Default is strictly false (Normal Mode connecting to real backend)
  APP_NAME: 'SecureBank',
  CURRENCY_SYMBOL: '₹'
};

// Freeze configuration to prevent tampering
Object.freeze(CONFIG);
