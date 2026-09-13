/**
 * CBII BANK - DASHBOARD CONTROLLER
 */

let activeAccountsList = [];
let quickDeskMode = 'DEPOSIT';
let recentTransactionsList = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.requireAuth()) return;

    loadDashboard();

    document.getElementById('quickDeskForm').addEventListener('submit', handleDeskSubmit);
});

async function loadDashboard() {
    try {
        // 1. Fetch user accounts
        const accRes = await apiRequest('/api/customers/accounts');
        activeAccountsList = accRes.data || [];
        renderDashboardAccounts(activeAccountsList);
        populateDeskAccountsSelect(activeAccountsList);

        // 2. Fetch recent transactions
        const txRes = await apiRequest('/api/transactions?page=0&size=6&paged=true');
        recentTransactionsList = txRes.data && txRes.data.content ? txRes.data.content : [];
        const totalTxCount = txRes.data ? txRes.data.totalElements : recentTransactionsList.length;

        document.getElementById('totalTxCountBadge').textContent = totalTxCount;
        renderRecentTransactions(recentTransactionsList);
    } catch (err) {
        showAlert('Failed to synchronize dashboard: ' + err.message, 'error');
    }
}

function renderDashboardAccounts(accounts) {
    const container = document.getElementById('virtualCardsContainer');
    const totalBalanceEl = document.getElementById('totalNetBalance');
    const activeAccountsBadge = document.getElementById('activeAccountsBadge');

    if (!accounts || accounts.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="bi bi-wallet2 fs-1 text-muted"></i>
                <h6 class="mt-3 text-dark fw-bold">No Active Accounts</h6>
                <p class="text-muted small">Open your first checking or savings account in seconds.</p>
                <a href="accounts.html" class="btn btn-primary btn-sm mt-2">+ Open Bank Account</a>
            </div>
        `;
        totalBalanceEl.textContent = '$0.00';
        activeAccountsBadge.textContent = '0';
        return;
    }

    let netWorth = 0;
    let activeCount = 0;

    const cardsHtml = accounts.map(acc => {
        netWorth += parseFloat(acc.balance);
        if (acc.status === 'ACTIVE') activeCount++;

        const isSavings = acc.accountType === 'SAVINGS';
        const cardClass = isSavings ? 'card-savings' : 'card-checking';

        return `
            <div class="col-md-6">
                <div class="virtual-debit-card ${cardClass}">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <span class="badge bg-white bg-opacity-25 text-white fw-bold mb-1">${acc.accountType} ACCOUNT</span>
                            <div class="fs-4 fw-bold">${formatCurrency(acc.balance)}</div>
                        </div>
                        <span class="badge ${acc.status === 'ACTIVE' ? 'bg-success' : 'bg-danger'}">${acc.status}</span>
                    </div>

                    <div class="d-flex align-items-center justify-content-between my-2">
                        <div class="card-emv-chip"></div>
                        <i class="bi bi-wifi fs-4 text-white-50"></i>
                    </div>

                    <div>
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="card-number-display" style="font-size: 1.05rem;">
                                •••• •••• ${acc.accountNumber.slice(-4)}
                            </span>
                            <button class="btn btn-sm btn-link text-white p-0" onclick="copyToClipboard('${acc.accountNumber}', 'Account number')" title="Copy Account Number">
                                <i class="bi bi-clipboard"></i>
                            </button>
                        </div>
                        <div class="d-flex justify-content-between align-items-end text-white-50" style="font-size: 0.75rem;">
                            <span>${acc.userName ? acc.userName.toUpperCase() : 'VALUED CUSTOMER'}</span>
                            <span>VALID THRU 12/29</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    container.innerHTML = cardsHtml;
    totalBalanceEl.textContent = formatCurrency(netWorth);
    activeAccountsBadge.textContent = activeCount;
}

function populateDeskAccountsSelect(accounts) {
    const select = document.getElementById('deskAccountSelect');
    select.innerHTML = '<option value="">-- Choose Target Account --</option>';

    accounts.filter(a => a.status === 'ACTIVE').forEach(acc => {
        const opt = document.createElement('option');
        opt.value = acc.accountNumber;
        opt.textContent = `${acc.accountType} (•••• ${acc.accountNumber.slice(-4)}) - Balance: ${formatCurrency(acc.balance)}`;
        select.appendChild(opt);
    });
}

function setQuickDeskMode(mode) {
    quickDeskMode = mode;
    const tabDeposit = document.getElementById('tabDepositBtn');
    const tabWithdraw = document.getElementById('tabWithdrawBtn');
    const submitBtn = document.getElementById('deskSubmitBtn');

    if (mode === 'DEPOSIT') {
        tabDeposit.classList.add('active');
        tabWithdraw.classList.remove('active');
        submitBtn.className = 'btn btn-primary w-100 py-2 fw-semibold';
        submitBtn.innerHTML = '<span id="deskSpinner" class="spinner-border spinner-border-sm d-none me-2"></span> Confirm Deposit';
    } else {
        tabWithdraw.classList.add('active');
        tabDeposit.classList.remove('active');
        submitBtn.className = 'btn btn-warning w-100 py-2 fw-bold text-dark';
        submitBtn.innerHTML = '<span id="deskSpinner" class="spinner-border spinner-border-sm d-none me-2"></span> Confirm Withdrawal';
    }
}

async function handleDeskSubmit(e) {
    e.preventDefault();

    const accountNumber = document.getElementById('deskAccountSelect').value;
    const amount = parseFloat(document.getElementById('deskAmount').value);
    const description = document.getElementById('deskDescription').value.trim();

    if (!accountNumber || isNaN(amount) || amount <= 0) {
        showAlert('Please choose an account and specify a valid amount greater than $0.00', 'error');
        return;
    }

    const btn = document.getElementById('deskSubmitBtn');
    const spinner = document.getElementById('deskSpinner');
    btn.disabled = true;
    if (spinner) spinner.classList.remove('d-none');

    try {
        const endpoint = quickDeskMode === 'DEPOSIT' 
            ? '/api/transactions/deposit' 
            : '/api/transactions/withdraw';

        const payload = {
            accountNumber,
            amount,
            description: description || (quickDeskMode === 'DEPOSIT' ? 'Fast Desk Cash Deposit' : 'Fast Desk Cash Withdrawal')
        };

        const res = await apiRequest(endpoint, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        showAlert(`${quickDeskMode === 'DEPOSIT' ? 'Deposit' : 'Withdrawal'} of ${formatCurrency(amount)} executed successfully!`, 'success');
        document.getElementById('deskAmount').value = '';
        document.getElementById('deskDescription').value = '';
        loadDashboard();
    } catch (err) {
        showAlert(err.message || 'Transaction failed.', 'error');
    } finally {
        btn.disabled = false;
        if (spinner) spinner.classList.add('d-none');
    }
}

function renderRecentTransactions(transactions) {
    const tbody = document.getElementById('recentTransactionsTableBody');

    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted small">No recent transaction entries found</td></tr>`;
        return;
    }

    tbody.innerHTML = transactions.map(tx => {
        let typeBadge = '';
        let amountClass = 'text-dark';
        let sign = '';

        if (tx.type === 'DEPOSIT') {
            typeBadge = '<span class="badge bg-success-subtle text-success badge-fintech">DEPOSIT</span>';
            amountClass = 'text-success fw-bold';
            sign = '+';
        } else if (tx.type === 'WITHDRAWAL') {
            typeBadge = '<span class="badge bg-danger-subtle text-danger badge-fintech">WITHDRAWAL</span>';
            amountClass = 'text-danger fw-bold';
            sign = '-';
        } else {
            typeBadge = '<span class="badge bg-primary-subtle text-primary badge-fintech">TRANSFER</span>';
            amountClass = 'text-primary fw-bold';
        }

        const counterparty = tx.type === 'TRANSFER'
            ? `<span>${tx.sourceAccountNumber} &rarr; ${tx.targetAccountNumber}</span>`
            : `<span>${tx.description || 'General Transaction'}</span>`;

        return `
            <tr>
                <td><code class="fw-bold text-dark">${tx.transactionReference}</code></td>
                <td>${typeBadge}</td>
                <td class="small">${counterparty}</td>
                <td class="${amountClass}">${sign}${formatCurrency(tx.amount)}</td>
                <td><span class="badge bg-light text-dark small">${tx.status}</span></td>
                <td class="small text-muted">${formatDate(tx.timestamp)}</td>
                <td>
                    <button class="btn btn-sm btn-outline-secondary py-0 px-2" onclick="openReceipt(${tx.id})" title="Print Receipt">
                        <i class="bi bi-receipt"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

async function openReceipt(txId) {
    const tx = recentTransactionsList.find(t => t.id === txId);
    if (!tx) {
        try {
            const res = await apiRequest(`/api/transactions/${txId}`);
            populateReceiptModal(res.data);
        } catch (e) {
            showAlert('Could not load receipt details', 'error');
        }
        return;
    }
    populateReceiptModal(tx);
}

function populateReceiptModal(tx) {
    document.getElementById('rcptAmount').textContent = formatCurrency(tx.amount);
    document.getElementById('rcptStatus').textContent = tx.status;
    document.getElementById('rcptRef').textContent = tx.transactionReference;
    document.getElementById('rcptType').textContent = tx.type;
    document.getElementById('rcptSource').textContent = tx.sourceAccountNumber !== 'N/A' ? `${tx.sourceAccountNumber} (${tx.sourceUserName || 'Account Holder'})` : 'Cash/Counter Deposit';
    document.getElementById('rcptTarget').textContent = tx.targetAccountNumber !== 'N/A' ? `${tx.targetAccountNumber} (${tx.targetUserName || 'Recipient'})` : 'Cash Withdrawal';
    document.getElementById('rcptDesc').textContent = tx.description || 'None';
    document.getElementById('rcptDate').textContent = formatDate(tx.timestamp);

    new bootstrap.Modal(document.getElementById('receiptModal')).show();
}
