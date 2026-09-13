/**
 * CBII BANK - ACCOUNTS CONTROLLER
 */

document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.requireAuth()) return;

    loadAccountsPortfolio();

    document.getElementById('createAccountForm').addEventListener('submit', handleCreateAccountSubmit);
});

async function loadAccountsPortfolio() {
    const grid = document.getElementById('accountsCardsGrid');

    try {
        const response = await apiRequest('/api/customers/accounts');
        const accounts = response.data || [];

        if (accounts.length === 0) {
            grid.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="bi bi-wallet2 fs-1 text-muted"></i>
                    <h5 class="mt-3 fw-bold">No Bank Accounts Found</h5>
                    <p class="text-muted small">You haven't opened any bank accounts yet.</p>
                    <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#newAccountModal">+ Open First Account</button>
                </div>
            `;
            return;
        }

        grid.innerHTML = accounts.map(acc => {
            const isSavings = acc.accountType === 'SAVINGS';
            const cardClass = isSavings ? 'card-savings' : 'card-checking';
            const statusBadge = acc.status === 'ACTIVE'
                ? '<span class="badge bg-success-subtle text-success badge-fintech">ACTIVE</span>'
                : acc.status === 'BLOCKED'
                    ? '<span class="badge bg-danger-subtle text-danger badge-fintech">BLOCKED</span>'
                    : '<span class="badge bg-secondary-subtle text-secondary badge-fintech">CLOSED</span>';

            const canClose = acc.status === 'ACTIVE';

            return `
                <div class="col-lg-6">
                    <div class="card card-custom p-4 h-100">
                        <!-- Digital Card Preview -->
                        <div class="virtual-debit-card ${cardClass} mb-3">
                            <div class="d-flex justify-content-between align-items-start">
                                <div>
                                    <span class="badge bg-white bg-opacity-25 text-white fw-bold mb-1">${acc.accountType}</span>
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
                                    <span class="card-number-display" style="font-size: 1.15rem;">
                                        •••• •••• •••• ${acc.accountNumber.slice(-4)}
                                    </span>
                                    <button class="btn btn-sm btn-link text-white p-0" onclick="copyToClipboard('${acc.accountNumber}', 'Account number')" title="Copy Account Number">
                                        <i class="bi bi-clipboard"></i>
                                    </button>
                                </div>
                                <div class="d-flex justify-content-between align-items-end text-white-50" style="font-size: 0.75rem;">
                                    <span>${acc.userName ? acc.userName.toUpperCase() : 'VALUED CUSTOMER'}</span>
                                    <span>EXP: 12/29</span>
                                </div>
                            </div>
                        </div>

                        <!-- Account Metadata List -->
                        <div class="bg-light p-3 rounded-3 mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <span class="text-muted small">Account Number:</span>
                                <span class="font-monospace fw-bold">${acc.accountNumber}</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <span class="text-muted small">Account Classification:</span>
                                <span class="fw-semibold">${acc.accountType}</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="text-muted small">Opening Date:</span>
                                <span class="small">${formatDate(acc.createdAt)}</span>
                            </div>
                        </div>

                        <!-- Card Action Buttons -->
                        <div class="d-flex gap-2 mt-auto">
                            <button class="btn btn-outline-primary btn-sm flex-grow-1" onclick="viewAccountModal(${acc.id})">
                                <i class="bi bi-info-circle me-1"></i> Full Details
                            </button>
                            <a href="transfer.html" class="btn btn-primary btn-sm flex-grow-1">
                                <i class="bi bi-send me-1"></i> Transfer
                            </a>
                            ${canClose ? `
                                <button class="btn btn-outline-danger btn-sm" onclick="closeAccount(${acc.id}, '${acc.accountNumber}', ${acc.balance})">
                                    <i class="bi bi-x-circle"></i> Close
                                </button>
                            ` : ''}
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        grid.innerHTML = `<div class="col-12 text-center text-danger py-4">Failed to load accounts: ${err.message}</div>`;
    }
}

async function handleCreateAccountSubmit(e) {
    e.preventDefault();

    const accountType = document.getElementById('newAccountType').value;
    const initialDeposit = parseFloat(document.getElementById('newAccountDeposit').value) || 0;

    const btn = document.getElementById('createAccountBtn');
    btn.disabled = true;

    try {
        await apiRequest('/api/accounts', {
            method: 'POST',
            body: JSON.stringify({
                accountType,
                initialDeposit
            })
        });

        showAlert('Account created successfully! Welcome credit applied.', 'success');
        bootstrap.Modal.getInstance(document.getElementById('newAccountModal')).hide();
        document.getElementById('createAccountForm').reset();
        loadAccountsPortfolio();
    } catch (err) {
        showAlert(err.message || 'Failed to open account', 'error');
    } finally {
        btn.disabled = false;
    }
}

async function viewAccountModal(id) {
    try {
        const response = await apiRequest(`/api/accounts/${id}`);
        const acc = response.data;

        const body = document.getElementById('accountStatementBody');
        body.innerHTML = `
            <div class="list-group list-group-flush">
                <div class="list-group-item d-flex justify-content-between px-0">
                    <span class="text-muted">Account ID</span>
                    <span class="fw-bold">#${acc.id}</span>
                </div>
                <div class="list-group-item d-flex justify-content-between px-0">
                    <span class="text-muted">Account Number</span>
                    <span class="font-monospace fw-bold text-primary">${acc.accountNumber}</span>
                </div>
                <div class="list-group-item d-flex justify-content-between px-0">
                    <span class="text-muted">Account Type</span>
                    <span class="fw-semibold">${acc.accountType}</span>
                </div>
                <div class="list-group-item d-flex justify-content-between px-0">
                    <span class="text-muted">Available Balance</span>
                    <span class="fw-bold text-success fs-5">${formatCurrency(acc.balance)}</span>
                </div>
                <div class="list-group-item d-flex justify-content-between px-0">
                    <span class="text-muted">Account Status</span>
                    <span class="badge ${acc.status === 'ACTIVE' ? 'bg-success' : 'bg-danger'}">${acc.status}</span>
                </div>
                <div class="list-group-item d-flex justify-content-between px-0">
                    <span class="text-muted">Primary Holder</span>
                    <span>${acc.userName} (${acc.userEmail})</span>
                </div>
                <div class="list-group-item d-flex justify-content-between px-0">
                    <span class="text-muted">Date Established</span>
                    <span class="small text-muted">${formatDate(acc.createdAt)}</span>
                </div>
            </div>
        `;

        new bootstrap.Modal(document.getElementById('accountStatementModal')).show();
    } catch (err) {
        showAlert(err.message || 'Could not fetch account information', 'error');
    }
}

async function closeAccount(id, accNum, balance) {
    if (balance > 0) {
        showAlert(`Cannot close account ${accNum}. Account balance must be zero. Current balance: ${formatCurrency(balance)}`, 'error');
        return;
    }

    if (!confirm(`Are you sure you want to PERMANENTLY CLOSE account ${accNum}? This operation cannot be undone.`)) {
        return;
    }

    try {
        await apiRequest(`/api/accounts/${id}/close`, { method: 'PUT' });
        showAlert(`Account ${accNum} has been officially closed.`, 'success');
        loadAccountsPortfolio();
    } catch (err) {
        showAlert(err.message || 'Failed to close account', 'error');
    }
}
