/**
 * SecureBank Operations Controller (Deposit, Withdrawal, Transfer)
 */
document.addEventListener('DOMContentLoaded', async () => {
  const currentPath = window.location.pathname;
  let activePage = 'deposit';
  if (currentPath.includes('withdraw')) activePage = 'withdraw';
  if (currentPath.includes('transfer')) activePage = 'transfer';

  Navigation.init(activePage);

  const accountSelect = document.getElementById('account-select');
  const amountInput = document.getElementById('amount');
  const pinInput = document.getElementById('pin');
  const descInput = document.getElementById('description');
  const destAccInput = document.getElementById('destinationAccountNumber');
  const previewEl = document.getElementById('account-balance-preview');
  const statusMessage = document.getElementById('status-message');
  const btnSubmit = document.getElementById('btn-submit');
  const btnSpinner = document.getElementById('btn-spinner');
  const btnText = document.getElementById('btn-text');

  let userAccounts = [];

  // Parse pre-selected account ID from URL
  const urlParams = new URLSearchParams(window.location.search);
  const preselectedId = urlParams.get('id');

  // Load User Accounts into dropdown
  try {
    userAccounts = await ApiService.getAccounts();
    if (!userAccounts || userAccounts.length === 0) {
      accountSelect.innerHTML = '<option value="">No active accounts found</option>';
      showAlert('You do not have any active accounts. Please open an account first.', 'danger');
      if (btnSubmit) btnSubmit.disabled = true;
      return;
    }

    accountSelect.innerHTML = userAccounts.map(acc => `
      <option value="${acc.id}" data-accnum="${acc.accountNumber}" ${preselectedId == acc.id ? 'selected' : ''}>
        ${acc.accountNumber} (${acc.accountType} — ₹${parseFloat(acc.balance).toFixed(2)})
      </option>
    `).join('');

    updateBalancePreview();
    accountSelect.addEventListener('change', updateBalancePreview);

  } catch (err) {
    showAlert(err.message || 'Error loading accounts.', 'danger');
    return;
  }

  function updateBalancePreview() {
    const selectedAcc = getSelectedAccount();
    if (!selectedAcc) return;

    if (selectedAcc.accountType === 'SAVINGS') {
      previewEl.innerHTML = `Available Balance: <strong>₹${parseFloat(selectedAcc.balance).toFixed(2)}</strong> (Min Balance: ₹1,000)`;
    } else {
      previewEl.innerHTML = `Available Balance: <strong>₹${parseFloat(selectedAcc.balance).toFixed(2)}</strong> (Overdraft Limit: ₹5,000)`;
    }
  }

  function getSelectedAccount() {
    const selectedId = accountSelect.value;
    return userAccounts.find(a => a.id == selectedId);
  }

  // 1. DEPOSIT WORKFLOW
  const depositForm = document.getElementById('deposit-form');
  if (depositForm) {
    depositForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      hideAlert();

      const accountId = accountSelect.value;
      const amountStr = amountInput.value.trim();
      const amount = parseFloat(amountStr);
      const description = descInput ? descInput.value.trim() : '';

      if (!amountStr || isNaN(amount) || amount <= 0) {
        showAlert('Please enter a positive deposit amount greater than ₹0.00.', 'danger');
        return;
      }

      setLoading(true, 'Processing Deposit...');

      try {
        const response = await ApiService.deposit(accountId, { amount, description });
        showAlert(`Deposit of ₹${parseFloat(response.amount).toFixed(2)} successful! New balance: ₹${parseFloat(response.balanceAfter).toFixed(2)}`, 'success');

        // Refresh accounts and UI state
        await refreshAccounts(accountId);
        amountInput.value = '';
        if (descInput) descInput.value = '';
      } catch (err) {
        showAlert(err.message || 'Deposit failed.', 'danger');
      } finally {
        setLoading(false, 'Confirm Deposit');
      }
    });
  }

  // 2. WITHDRAWAL WORKFLOW
  const withdrawForm = document.getElementById('withdraw-form');
  if (withdrawForm) {
    withdrawForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      hideAlert();

      const accountId = accountSelect.value;
      const amountStr = amountInput.value.trim();
      const amount = parseFloat(amountStr);
      const pin = pinInput.value.trim();
      const description = descInput ? descInput.value.trim() : '';

      if (!amountStr || isNaN(amount) || amount <= 0) {
        showAlert('Please enter a valid withdrawal amount greater than ₹0.00.', 'danger');
        return;
      }

      if (!pin) {
        showAlert('Please enter your transaction PIN to authorize this withdrawal.', 'danger');
        return;
      }

      setLoading(true, 'Processing Withdrawal...');

      try {
        const response = await ApiService.withdraw(accountId, { amount, pin, description });
        showAlert(`Withdrawal of ₹${parseFloat(response.amount).toFixed(2)} successful! New balance: ₹${parseFloat(response.balanceAfter).toFixed(2)}`, 'success');

        await refreshAccounts(accountId);
        amountInput.value = '';
        pinInput.value = '';
        if (descInput) descInput.value = '';
      } catch (err) {
        let msg = err.message || 'Withdrawal rejected.';
        if (err.data && err.data.details && err.data.details.shortfall) {
          msg += ` (Shortfall amount: ₹${err.data.details.shortfall})`;
        }
        showAlert(msg, 'danger');
      } finally {
        setLoading(false, 'Confirm Withdrawal');
      }
    });
  }

  // 3. TRANSFER WORKFLOW (With Confirmation Modal)
  const transferForm = document.getElementById('transfer-form');
  const confirmModal = document.getElementById('confirm-modal');
  const btnModalCancel = document.getElementById('btn-modal-cancel');
  const btnModalConfirm = document.getElementById('btn-modal-confirm');

  if (transferForm && confirmModal) {
    let pendingTransferData = null;

    transferForm.addEventListener('submit', (e) => {
      e.preventDefault();
      hideAlert();

      const selectedAccount = getSelectedAccount();
      const sourceAccountNumber = selectedAccount ? selectedAccount.accountNumber : '';
      const destinationAccountNumber = destAccInput.value.trim().toUpperCase();
      const amountStr = amountInput.value.trim();
      const amount = parseFloat(amountStr);
      const pin = pinInput.value.trim();
      const description = descInput ? descInput.value.trim() : '';

      if (!sourceAccountNumber) {
        showAlert('Please select a valid source account.', 'danger');
        return;
      }

      if (!destinationAccountNumber) {
        showAlert('Please specify a destination account number.', 'danger');
        return;
      }

      if (sourceAccountNumber.toUpperCase() === destinationAccountNumber) {
        showAlert('Source and destination accounts cannot be identical.', 'danger');
        return;
      }

      if (!amountStr || isNaN(amount) || amount <= 0) {
        showAlert('Please enter a positive transfer amount.', 'danger');
        return;
      }

      if (!pin) {
        showAlert('Please enter your transaction authorization PIN.', 'danger');
        return;
      }

      pendingTransferData = {
        sourceAccountNumber,
        destinationAccountNumber,
        amount,
        pin,
        description
      };

      // Populate confirmation modal
      document.getElementById('modal-from').textContent = sourceAccountNumber;
      document.getElementById('modal-to').textContent = destinationAccountNumber;
      document.getElementById('modal-amount').textContent = `₹${amount.toFixed(2)}`;
      document.getElementById('modal-notes').textContent = description || 'None';

      confirmModal.style.display = 'flex';
    });

    btnModalCancel.addEventListener('click', () => {
      confirmModal.style.display = 'none';
      pendingTransferData = null;
    });

    btnModalConfirm.addEventListener('click', async () => {
      if (!pendingTransferData) return;
      confirmModal.style.display = 'none';

      setLoading(true, 'Executing Transfer...');

      try {
        const response = await ApiService.transfer(pendingTransferData);
        showAlert(`Transfer successful! Reference ID: ${response.transferReference}. Debited: ₹${parseFloat(response.amount).toFixed(2)}. Remaining balance: ₹${parseFloat(response.balanceAfter).toFixed(2)}`, 'success');

        await refreshAccounts(accountSelect.value);
        destAccInput.value = '';
        amountInput.value = '';
        pinInput.value = '';
        if (descInput) descInput.value = '';
      } catch (err) {
        let msg = err.message || 'Transfer failed.';
        if (err.data && err.data.details && err.data.details.shortfall) {
          msg += ` (Shortfall amount: ₹${err.data.details.shortfall})`;
        }
        showAlert(msg, 'danger');
      } finally {
        setLoading(false, 'Review & Transfer');
        pendingTransferData = null;
      }
    });
  }

  async function refreshAccounts(selectedId) {
    userAccounts = await ApiService.getAccounts();
    accountSelect.innerHTML = userAccounts.map(acc => `
      <option value="${acc.id}" data-accnum="${acc.accountNumber}" ${selectedId == acc.id ? 'selected' : ''}>
        ${acc.accountNumber} (${acc.accountType} — ₹${parseFloat(acc.balance).toFixed(2)})
      </option>
    `).join('');
    updateBalancePreview();
  }

  function setLoading(isLoading, label) {
    if (btnSubmit) {
      btnSubmit.disabled = isLoading;
      if (btnSpinner) btnSpinner.style.display = isLoading ? 'inline-block' : 'none';
      if (btnText) btnText.textContent = label;
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
