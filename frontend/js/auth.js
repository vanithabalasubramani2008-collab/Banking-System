/**
 * SecureBank Authentication Controller (Login & Client-side Validation)
 */
document.addEventListener('DOMContentLoaded', () => {
  // If already logged in, redirect straight to dashboard
  if (ApiService.isAuthenticated()) {
    window.location.href = 'dashboard.html';
    return;
  }

  const loginForm = document.getElementById('login-form');
  const usernameInput = document.getElementById('username');
  const passwordInput = document.getElementById('password');
  const alertBox = document.getElementById('alert-box');
  const btnSubmit = document.getElementById('btn-submit');
  const btnSpinner = document.getElementById('btn-spinner');
  const btnText = document.getElementById('btn-text');
  const btnTogglePwd = document.getElementById('btn-toggle-pwd');

  // Check query params for logout or session expiration messages
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('msg') === 'logged_out') {
    showAlert('You have been successfully logged out.', 'info');
  } else if (urlParams.get('error') === 'session_expired') {
    showAlert('Your session has expired. Please log in again.', 'danger');
  } else if (urlParams.get('error') === 'unauthorized') {
    showAlert('Please log in to access your banking account.', 'danger');
  }

  // Toggle password visibility
  if (btnTogglePwd) {
    btnTogglePwd.addEventListener('click', () => {
      const isPwd = passwordInput.type === 'password';
      passwordInput.type = isPwd ? 'text' : 'password';
      btnTogglePwd.textContent = isPwd ? '🔒' : '👁️';
    });
  }

  // Handle form submission
  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert();

    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    // Client-side Validation
    if (!username) {
      showAlert('Please enter your username.', 'danger');
      usernameInput.focus();
      return;
    }

    if (!password) {
      showAlert('Please enter your password.', 'danger');
      passwordInput.focus();
      return;
    }

    // Processing state
    setLoading(true);

    try {
      const response = await ApiService.login({
        username,
        password
      });

      // Save token and credentials
      ApiService.saveAuth(response);

      // Redirect to dashboard
      window.location.href = 'dashboard.html';
    } catch (err) {
      setLoading(false);
      let errorMessage = err.message || 'Login failed. Please check your credentials.';

      // Check if account is locked
      if (err.status === 423) {
        errorMessage = 'Your account has been LOCKED due to 3 consecutive failed login attempts. Please contact bank administration.';
      }

      showAlert(errorMessage, 'danger');
    }
  });

  function setLoading(isLoading) {
    btnSubmit.disabled = isLoading;
    if (isLoading) {
      btnSpinner.style.display = 'inline-block';
      btnText.textContent = 'Authenticating...';
    } else {
      btnSpinner.style.display = 'none';
      btnText.textContent = 'Log In Securely';
    }
  }

  function showAlert(message, type = 'danger') {
    alertBox.className = `alert alert-${type}`;
    alertBox.textContent = message;
    alertBox.style.display = 'block';
  }

  function hideAlert() {
    alertBox.style.display = 'none';
    alertBox.textContent = '';
  }
});
