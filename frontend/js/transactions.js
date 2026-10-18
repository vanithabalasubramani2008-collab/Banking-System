/**
 * SecureBank Transaction History & Statement Controller
 * Loads transactions from GET /api/accounts/{id}/statement and /transactions
 */
document.addEventListener('DOMContentLoaded', async () => {
  Navigation.init('transactions');

  const accountSelect = document.getElementById('account-select');
  const txFilter = document.getElementById('tx-filter');
  const btnRefresh = document.getElementById('btn-refresh');
  const txCountBadge = document.getElementById('tx-count');
  const transactionsContainer = document.getElementById('transactions-container');
  const statusMessage = document.getElementById('status-message');

  let currentTransactions = [];
  let userAccounts = [];

  try {
    userAccounts = await ApiService.getAccounts();
    if (!userAccounts || userAccounts.length === 0) {
      accountSelect.innerHTML = '<option value="">No accounts found</option>';
      transactionsContainer.innerHTML = `
        <div style="text-align: center; padding: 40px;" class="text-muted">
          No bank accounts found for your profile. Open an account to start transacting.
        </div>
      `;
      return;
    }

    accountSelect.innerHTML = userAccounts.map(acc => `
      <option value="${acc.id}">
        ${acc.accountNumber} (${acc.accountType} — ₹${parseFloat(acc.balance).toFixed(2)})
      </option>
    `).join('');

    await loadStatement(accountSelect.value);

    accountSelect.addEventListener('change', () => loadStatement(accountSelect.value));
    txFilter.addEventListener('change', renderTransactions);
    btnRefresh.addEventListener('click', () => loadStatement(accountSelect.value));

  } catch (err) {
    transactionsContainer.innerHTML = `
      <div class="alert alert-danger" style="margin: 20px;">
        ${err.message || 'Failed to initialize transaction statement.'}
      </div>
    `;
  }

  async function loadStatement(accountId) {
    if (!accountId) return;

    transactionsContainer.innerHTML = `
      <div style="text-align: center; padding: 30px;" class="text-muted">
        Fetching statement from backend ledger...
      </div>
    `;

    try {
      const statement = await ApiService.getAccountStatement(accountId);
      currentTransactions = (statement && statement.recentTransactions) ? statement.recentTransactions : [];
      renderTransactions();
    } catch (err) {
      // Fallback to recent transactions endpoint if statement is unavailable
      try {
        currentTransactions = await ApiService.getRecentTransactions(accountId, 20);
        renderTransactions();
      } catch (innerErr) {
        transactionsContainer.innerHTML = `
          <div class="alert alert-danger" style="margin: 20px;">
            ${innerErr.message || 'Error loading transaction records.'}
          </div>
        `;
      }
    }
  }

  function renderTransactions() {
    const filter = txFilter.value;
    let filtered = currentTransactions;

    if (filter === 'DEPOSIT') {
      filtered = currentTransactions.filter(t => t.transactionType === 'DEPOSIT');
    } else if (filter === 'WITHDRAWAL') {
      filtered = currentTransactions.filter(t => t.transactionType === 'WITHDRAWAL');
    } else if (filter === 'TRANSFER') {
      filtered = currentTransactions.filter(t => t.transactionType.includes('TRANSFER'));
    } else if (filter === 'INTEREST') {
      filtered = currentTransactions.filter(t => t.transactionType.includes('INTEREST'));
    }

    txCountBadge.textContent = `${filtered.length} Records`;

    if (!filtered || filtered.length === 0) {
      transactionsContainer.innerHTML = `
        <div style="text-align: center; padding: 40px;" class="text-muted">
          No transactions match the selected filter.
        </div>
      `;
      return;
    }

    let html = `
      <table class="table">
        <thead>
          <tr>
            <th>Tx ID</th>
            <th>Timestamp</th>
            <th>Type</th>
            <th>Description / Party</th>
            <th>Transfer Reference</th>
            <th class="text-right">Amount</th>
            <th class="text-right">Balance After</th>
          </tr>
        </thead>
        <tbody>
    `;

    filtered.forEach(tx => {
      const isCredit = tx.transactionType.includes('CREDIT') || tx.transactionType === 'DEPOSIT';
      const colorClass = isCredit ? 'text-success' : 'text-danger';
      const sign = isCredit ? '+' : '-';
      const formattedDate = new Date(tx.createdAt).toLocaleString('en-IN', {
        year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit'
      });

      html += `
        <tr>
          <td>#${tx.id}</td>
          <td class="text-muted" style="font-size: 13px;">${formattedDate}</td>
          <td><span class="badge ${isCredit ? 'badge-savings' : 'badge-current'}">${tx.transactionType}</span></td>
          <td>
            ${tx.description || '—'}
            ${tx.counterpartyAccountNumber ? `<br><small class="text-muted">Counterparty: ${tx.counterpartyAccountNumber}</small>` : ''}
          </td>
          <td><code>${tx.transferReference || '—'}</code></td>
          <td class="text-right font-bold ${colorClass}">
            ${sign}${CONFIG.CURRENCY_SYMBOL}${parseFloat(tx.amount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </td>
          <td class="text-right font-bold">
            ${CONFIG.CURRENCY_SYMBOL}${parseFloat(tx.balanceAfter).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </td>
        </tr>
      `;
    });

    html += '</tbody></table>';
    transactionsContainer.innerHTML = html;
  }
});
