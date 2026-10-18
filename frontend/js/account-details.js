/**
 * SecureBank Account Details Controller
 * Loads detailed account profile, domain specifications, and supports applying yearly interest
 */
document.addEventListener('DOMContentLoaded', async () => {
  Navigation.init('account-details');

  const accountSelect = document.getElementById('account-select');
  const detailsContainer = document.getElementById('details-container');
  const statusMessage = document.getElementById('status-message');

  const urlParams = new URLSearchParams(window.location.search);
  const preselectedId = urlParams.get('id');

  let userAccounts = [];

  try {
    userAccounts = await ApiService.getAccounts();
    if (!userAccounts || userAccounts.length === 0) {
      accountSelect.innerHTML = '<option value="">No accounts found</option>';
      detailsContainer.innerHTML = `
        <div class="card" style="text-align: center; padding: 40px;">
          <p class="text-muted">No accounts registered under your profile.</p>
          <a href="accounts.html" class="btn btn-primary" style="margin-top: 10px;">Open New Account</a>
        </div>
      `;
      return;
    }

    accountSelect.innerHTML = userAccounts.map(acc => `
      <option value="${acc.id}" ${preselectedId == acc.id ? 'selected' : ''}>
        ${acc.accountNumber} (${acc.accountType})
      </option>
    `).join('');

    await loadAccountDetails(accountSelect.value);
    accountSelect.addEventListener('change', () => loadAccountDetails(accountSelect.value));

  } catch (err) {
    detailsContainer.innerHTML = `
      <div class="alert alert-danger">
        ${err.message || 'Error loading account details.'}
      </div>
    `;
  }

  async function loadAccountDetails(accountId) {
    if (!accountId) return;

    detailsContainer.innerHTML = `
      <div style="text-align: center; padding: 30px;" class="text-muted">
        Loading details for account #${accountId}...
      </div>
    `;

    try {
      const account = await ApiService.getAccountById(accountId);

      const isSavings = account.accountType === 'SAVINGS';
      const createdDate = new Date(account.createdAt).toLocaleDateString('en-IN', {
        year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
      });

      let ruleSpecificHtml = '';
      if (isSavings) {
        ruleSpecificHtml = `
          <div class="grid-2" style="margin-top: 16px;">
            <div style="background-color: #f0fdf4; padding: 16px; border-radius: var(--radius-sm); border: 1px solid #bbf7d0;">
              <div class="font-bold text-success" style="margin-bottom: 6px;">Minimum Balance Rule</div>
              <p style="margin-bottom: 0; font-size: 13px;">Must maintain at least <strong>₹${parseFloat(account.minimumBalance || 1000).toFixed(2)}</strong> at all times. Withdrawals breaching this threshold will be denied by the server.</p>
            </div>
            <div style="background-color: #eff6ff; padding: 16px; border-radius: var(--radius-sm); border: 1px solid #bfdbfe;">
              <div class="font-bold text-info" style="margin-bottom: 6px;">Interest Rate</div>
              <p style="margin-bottom: 8px; font-size: 13px;">Annual yield: <strong>4.00%</strong> calculated via <code>SavingsAccount.calculateYearlyInterest()</code>.</p>
              <button id="btn-apply-interest" class="btn btn-accent" style="padding: 6px 12px; font-size: 12px;">Credit 4% Yearly Interest</button>
            </div>
          </div>
        `;
      } else {
        ruleSpecificHtml = `
          <div style="background-color: #fefce8; padding: 16px; border-radius: var(--radius-sm); border: 1px solid #fef08a; margin-top: 16px;">
            <div class="font-bold text-warning" style="margin-bottom: 6px;">Current Account Overdraft Protection</div>
            <p style="margin-bottom: 0; font-size: 13px;">Equipped with an authorized overdraft cushion of up to <strong>₹${parseFloat(account.overdraftLimit || 5000).toFixed(2)}</strong>. Balance is permitted to reach -₹${parseFloat(account.overdraftLimit || 5000).toFixed(2)} before rejection.</p>
          </div>
        `;
      }

      detailsContainer.innerHTML = `
        <div class="card">
          <div class="card-header">
            <h3>Account Summary: ${account.accountNumber}</h3>
            <span class="badge ${isSavings ? 'badge-savings' : 'badge-current'}">${account.accountType}</span>
          </div>

          <table class="table" style="margin-bottom: 20px;">
            <tbody>
              <tr>
                <td style="width: 30%; color: var(--text-muted);">Account Number</td>
                <td><strong>${account.accountNumber}</strong></td>
              </tr>
              <tr>
                <td style="color: var(--text-muted);">Account Holder Name</td>
                <td>${account.accountHolderName}</td>
              </tr>
              <tr>
                <td style="color: var(--text-muted);">Account Type</td>
                <td>${account.accountType} Account</td>
              </tr>
              <tr>
                <td style="color: var(--text-muted);">Authoritative Balance</td>
                <td class="font-bold" style="font-size: 1.2rem; color: var(--color-primary);">
                  ${CONFIG.CURRENCY_SYMBOL}${parseFloat(account.balance).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </td>
              </tr>
              <tr>
                <td style="color: var(--text-muted);">OOP String Summary</td>
                <td><code>${account.details}</code></td>
              </tr>
              <tr>
                <td style="color: var(--text-muted);">Opening Date</td>
                <td>${createdDate}</td>
              </tr>
            </tbody>
          </table>

          <h4 style="margin-top: 24px;">Domain Rules & Facilities</h4>
          ${ruleSpecificHtml}

          <div style="margin-top: 24px; display: flex; gap: 10px;">
            <a href="deposit.html?id=${account.id}" class="btn btn-primary">Deposit to this Account</a>
            <a href="withdraw.html?id=${account.id}" class="btn btn-outline">Withdraw Funds</a>
            <a href="transactions.html" class="btn btn-outline">View Statement</a>
          </div>
        </div>
      `;

      // Setup interest trigger button if available
      const btnInterest = document.getElementById('btn-apply-interest');
      if (btnInterest) {
        btnInterest.addEventListener('click', async () => {
          btnInterest.disabled = true;
          btnInterest.textContent = 'Applying...';
          try {
            const tx = await ApiService.applyInterest(accountId);
            showAlert(`Yearly interest of ₹${parseFloat(tx.amount).toFixed(2)} credited successfully! Updated balance: ₹${parseFloat(tx.balanceAfter).toFixed(2)}`, 'success');
            await loadAccountDetails(accountId);
          } catch (intErr) {
            showAlert(intErr.message || 'Failed to apply interest.', 'danger');
            btnInterest.disabled = false;
            btnInterest.textContent = 'Credit 4% Yearly Interest';
          }
        });
      }

    } catch (err) {
      detailsContainer.innerHTML = `
        <div class="alert alert-danger">
          ${err.message || 'Error fetching account information.'}
        </div>
      `;
    }
  }

  function showAlert(msg, type = 'danger') {
    statusMessage.className = `alert alert-${type}`;
    statusMessage.textContent = msg;
    statusMessage.style.display = 'block';
  }
});
