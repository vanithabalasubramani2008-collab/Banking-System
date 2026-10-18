/**
 * SecureBank User Registration Controller
 * Connects to POST /api/auth/register with RegisterRequest DTO:
 * { username, email, password, pin, fullName, phoneNumber }
 */
document.addEventListener('DOMContentLoaded', () => {
  if (ApiService.isAuthenticated()) {
    window.location.href = 'dashboard.html';
    return;
  }

  const form = document.getElementById('register-form');
  const fullNameInput = document.getElementById('fullName');
  const usernameInput = document.getElementById('username');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const pinInput = document.getElementById('pin');
  const phoneInput = document.getElementById('phoneNumber');
  const alertBox = document.getElementById('alert-box');
  const btnSubmit = document.getElementById('btn-submit');
  const btnSpinner = document.getElementById('btn-spinner');
  const btnText = document.getElementById('btn-text');
  const btnTogglePwd = document.getElementById('btn-toggle-pwd');

  if (btnTogglePwd) {
    btnTogglePwd.addEventListener('click', () => {
      const isPwd = passwordInput.type === 'password';
      passwordInput.type = isPwd ? 'text' : 'password';
      btnTogglePwd.textContent = isPwd ? '🔒' : '👁️';
    });
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert();

    const fullName = fullNameInput.value.trim();
    const username = usernameInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;
    const pin = pinInput.value.trim();
    const phoneNumber = phoneInput.value.trim();

    // Client-side validations
    if (!fullName) {
      showAlert('Please enter your full name.', 'danger');
      fullNameInput.focus();
      return;
    }

    if (!username || username.length < 4) {
      showAlert('Username must be at least 4 characters long.', 'danger');
      usernameInput.focus();
      return;
    }

    if (!email || !email.includes('@')) {
      showAlert('Please enter a valid email address.', 'danger');
      emailInput.focus();
      return;
    }

    if (!password || password.length < 6) {
      showAlert('Password must be at least 6 characters long.', 'danger');
      passwordInput.focus();
      return;
    }

    if (!pin || !/^[0-9]{4,6}$/.test(pin)) {
      showAlert('PIN must be a 4 to 6 digit numerical code.', 'danger');
      pinInput.focus();
      return;
    }

    setLoading(true);

    try {
      const response = await ApiService.register({
        username,
        email,
        password,
        pin,
        fullName,
        phoneNumber: phoneNumber || null
      });

      // Automatically sign in with returned JWT token
      ApiService.saveAuth(response);

      showAlert('User account created successfully! Redirecting to your dashboard...', 'success');
      setTimeout(() => {
        window.location.href = 'dashboard.html';
      }, 1500);

    } catch (err) {
      setLoading(false);
      let errorMsg = err.message || 'Failed to create account. Please try again.';
      if (err.data && err.data.details) {
        const details = err.data.details;
        const firstKey = Object.keys(details)[0];
        if (firstKey) {
          errorMsg = details[firstKey];
        }
      }
      showAlert(errorMsg, 'danger');
    }
  });

  function setLoading(isLoading) {
    btnSubmit.disabled = isLoading;
    if (isLoading) {
      btnSpinner.style.display = 'inline-block';
      btnText.textContent = 'Creating Account...';
    } else {
      btnSpinner.style.display = 'none';
      btnText.textContent = 'Create Account & Sign In';
    }
  }

  function showAlert(msg, type = 'danger') {
    alertBox.className = `alert alert-${type}`;
    alertBox.textContent = msg;
    alertBox.style.display = 'block';
  }

  function hideAlert() {
    alertBox.style.display = 'none';
    alertBox.textContent = '';
  }
});
