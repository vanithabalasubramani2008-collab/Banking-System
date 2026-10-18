/**
 * SecureBank Dashboard Controller
 * Loads actual user accounts from /api/accounts and recent transactions
 */
document.addEventListener('DOMContentLoaded', async () => {
  Navigation.init('dashboard');

  const accountsContainer = document.getElementById('accounts-container');
  const recentTxContainer = document.getElementById('recent-tx-container');
  const totalBalanceEl = document.getElementById('total-balance');
  const primaryAccNoEl = document.getElementById('primary-account-no');
  const primaryAccTypeEl = document.getElementById('primary-account-type');
  const accountsCountEl = document.getElementById('accounts-count');
  const statusMessage = document.getElementById('status-message');

  try {
    // 1. Fetch user accounts from backend
    const accounts = await ApiService.getAccounts();

    accountsCountEl.textContent = accounts.length;

    if (!accounts || accounts.length === 0) {
      accountsContainer.innerHTML = `
        <div style="text-align: center; padding: 30px;">
          <p class="text-muted">You do not have any active bank accounts yet.</p>
          <a href="accounts.html" class="btn btn-primary" style="margin-top: 10px;">Open Your First Account</a>
        </div>
      `;
      recentTxContainer.innerHTML = `
        <div style="text-align: center; padding: 20px;" class="text-muted">
          No transactions found because you do not have any active accounts.
        </div>
      `;
      return;
    }

    // 2. Compute metrics for summary cards
    let totalBal = 0;
    accounts.forEach(acc => {
      totalBal += parseFloat(acc.balance || 0);
    });
    totalBalanceEl.textContent = `${CONFIG.CURRENCY_SYMBOL}${totalBal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

    const firstAccount = accounts[0];
    primaryAccNoEl.textContent = firstAccount.accountNumber;
    primaryAccTypeEl.textContent = `${firstAccount.accountType} (${CONFIG.CURRENCY_SYMBOL}${parseFloat(firstAccount.balance).toFixed(2)})`;

    // 3. Render accounts table
    let accountsHtml = `
      <table class="table">
        <thead>
          <tr>
            <th>Account Number</th>
            <th>Type</th>
            <th>Holder</th>
            <th class="text-right">Balance</th>
            <th>Rules / Features</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
    `;

    accounts.forEach(acc => {
      const isSavings = acc.accountType === 'SAVINGS';
      const badgeClass = isSavings ? 'badge-savings' : 'badge-current';
      const ruleNote = isSavings
        ? `Min Balance: ${CONFIG.CURRENCY_SYMBOL}1,000 (4% Interest)`
        : `Overdraft Limit: ${CONFIG.CURRENCY_SYMBOL}5,000`;

      accountsHtml += `
        <tr>
          <td><strong>${acc.accountNumber}</strong></td>
          <td><span class="badge ${badgeClass}">${acc.accountType}</span></td>
          <td>${acc.accountHolderName}</td>
          <td class="text-right font-bold">${CONFIG.CURRENCY_SYMBOL}${parseFloat(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
          <td class="text-muted" style="font-size: 13px;">${ruleNote}</td>
          <td>
            <a href="deposit.html?id=${acc.id}" class="btn btn-outline" style="padding: 4px 8px; font-size: 12px;">Deposit</a>
            <a href="withdraw.html?id=${acc.id}" class="btn btn-outline" style="padding: 4px 8px; font-size: 12px;">Withdraw</a>
            <a href="account-details.html?id=${acc.id}" class="btn btn-outline" style="padding: 4px 8px; font-size: 12px;">Details</a>
          </td>
        </tr>
      `;
    });

    accountsHtml += '</tbody></table>';
    accountsContainer.innerHTML = accountsHtml;

    // 4. Load recent transactions for primary account
    loadRecentTransactions(firstAccount.id, firstAccount.accountNumber);

  } catch (err) {
    accountsContainer.innerHTML = `
      <div class="alert alert-danger" style="margin: 20px;">
        ${err.message || 'Error loading accounts from backend server.'}
      </div>
    `;
    recentTxContainer.innerHTML = `
      <div class="text-muted" style="text-align: center; padding: 20px;">
        Unable to load recent transactions.
      </div>
    `;
  }

  async function loadRecentTransactions(accountId, accountNumber) {
    try {
      const txs = await ApiService.getRecentTransactions(accountId, 5);

      if (!txs || txs.length === 0) {
        recentTxContainer.innerHTML = `
          <div style="text-align: center; padding: 20px;" class="text-muted">
            No transactions recorded yet for account <strong>${accountNumber}</strong>.
          </div>
        `;
        return;
      }

      let txHtml = `
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Date & Time</th>
              <th>Type</th>
              <th>Description / Ref</th>
              <th class="text-right">Amount</th>
              <th class="text-right">Balance After</th>
            </tr>
          </thead>
          <tbody>
      `;

      txs.forEach(tx => {
        const isCredit = tx.transactionType.includes('CREDIT') || tx.transactionType === 'DEPOSIT';
        const colorClass = isCredit ? 'text-success' : 'text-danger';
        const sign = isCredit ? '+' : '-';
        const formattedDate = new Date(tx.createdAt).toLocaleString('en-IN', {
          year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit'
        });

        txHtml += `
          <tr>
            <td>#${tx.id}</td>
            <td class="text-muted" style="font-size: 13px;">${formattedDate}</td>
            <td><strong>${tx.transactionType}</strong></td>
            <td>${tx.description || '—'} ${tx.transferReference ? `(${tx.transferReference})` : ''}</td>
            <td class="text-right font-bold ${colorClass}">${sign}${CONFIG.CURRENCY_SYMBOL}${parseFloat(tx.amount).toFixed(2)}</td>
            <td class="text-right">${CONFIG.CURRENCY_SYMBOL}${parseFloat(tx.balanceAfter).toFixed(2)}</td>
          </tr>
        `;
      });

      txHtml += '</tbody></table>';
      recentTxContainer.innerHTML = txHtml;
    } catch (txErr) {
      recentTxContainer.innerHTML = `
        <div class="alert alert-danger" style="margin: 20px;">
          ${txErr.message || 'Failed to load transaction history.'}
        </div>
      `;
    }
  }
});
