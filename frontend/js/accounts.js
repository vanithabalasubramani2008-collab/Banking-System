/**
 * SecureBank Open Account Controller
 * Submits to POST /api/accounts with CreateAccountRequest { accountType, initialDeposit, pin }
 */
document.addEventListener('DOMContentLoaded', () => {
  Navigation.init('accounts');

  const form = document.getElementById('create-account-form');
  const accountTypeSelect = document.getElementById('accountType');
  const initialDepositInput = document.getElementById('initialDeposit');
  const pinInput = document.getElementById('pin');
  const ruleCallout = document.getElementById('rule-callout');
  const depositHint = document.getElementById('deposit-hint');
  const statusMessage = document.getElementById('status-message');
  const btnSubmit = document.getElementById('btn-submit');
  const btnSpinner = document.getElementById('btn-spinner');
  const btnText = document.getElementById('btn-text');

  // Dynamic Rule change listener
  accountTypeSelect.addEventListener('change', () => {
    if (accountTypeSelect.value === 'SAVINGS') {
      ruleCallout.className = 'alert alert-info';
      ruleCallout.innerHTML = '<strong>Savings Account Rules:</strong> Requires maintaining a minimum balance of ₹1,000 at all times. Accrues 4.00% annual interest credited on demand.';
      depositHint.textContent = 'Savings Account requires at least ₹1,000 initial deposit.';
      initialDepositInput.placeholder = '1000.00';
    } else {
      ruleCallout.className = 'alert alert-info';
      ruleCallout.innerHTML = '<strong>Current Account Rules:</strong> Allows overdraft balance up to ₹5,000 (balance can reach -₹5,000). Designed for high-frequency business transactions.';
      depositHint.textContent = 'Current Account can be opened with ₹0 or any positive deposit.';
      initialDepositInput.placeholder = '0.00';
    }
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert();

    const accountType = accountTypeSelect.value;
    const initialDepositStr = initialDepositInput.value.trim();
    const pin = pinInput.value.trim();

    // Client-side validations
    if (!initialDepositStr || isNaN(parseFloat(initialDepositStr))) {
      showAlert('Please enter a valid initial deposit amount.', 'danger');
      initialDepositInput.focus();
      return;
    }

    const initialDeposit = parseFloat(initialDepositStr);
    if (initialDeposit < 0) {
      showAlert('Initial deposit cannot be negative.', 'danger');
      return;
    }

    if (accountType === 'SAVINGS' && initialDeposit < 1000) {
      showAlert('Savings account requires an initial deposit of at least ₹1,000 to meet minimum balance rules.', 'danger');
      return;
    }

    if (!pin) {
      showAlert('Please enter your transaction authorization PIN.', 'danger');
      pinInput.focus();
      return;
    }

    setLoading(true);

    try {
      const response = await ApiService.createAccount({
        accountType,
        initialDeposit,
        pin
      });

      showAlert(`Account created successfully! Your new account number is ${response.accountNumber}. Redirecting to dashboard...`, 'success');
      setTimeout(() => {
        window.location.href = 'dashboard.html';
      }, 1800);
    } catch (err) {
      setLoading(false);
      showAlert(err.message || 'Failed to create account.', 'danger');
    }
  });

  function setLoading(isLoading) {
    btnSubmit.disabled = isLoading;
    if (isLoading) {
      btnSpinner.style.display = 'inline-block';
      btnText.textContent = 'Opening Account...';
    } else {
      btnSpinner.style.display = 'none';
      btnText.textContent = 'Confirm & Create Account';
    }
  }

  function showAlert(msg, type = 'danger') {
    statusMessage.className = `alert alert-${type}`;
    statusMessage.textContent = msg;
    statusMessage.style.display = 'block';
  }

  function hideAlert() {
    statusMessage.style.display = 'none';
    statusMessage.textContent = '';
  }
});
