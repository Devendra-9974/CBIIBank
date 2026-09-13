/* ==========================================================================
   admin.js — CBII Bank | Executive Operations & Admin Command Center
   ========================================================================== */

'use strict';

/* ── Auth Guard ─────────────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.requireAuth(['ADMIN', 'BANK_EMPLOYEE'])) return;

    // Personalise the navbar avatar from the live session
    const user = Auth.getUser();
    if (user) {
        const initials = user.name
            ? user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
            : 'AD';
        const avatarEl = document.querySelector('.user-display-avatar');
        const nameEl   = document.querySelector('.user-display-name');
        const roleEl   = document.querySelector('.user-display-role');
        if (avatarEl) avatarEl.textContent = initials;
        if (nameEl)   nameEl.textContent   = user.name || 'Administrator';
        if (roleEl)   roleEl.textContent   = user.role || 'ADMIN';
    }

    loadAdminDashboardData();
});

/* ── Master Loader (called on init & by "Synchronize System" button) ─────── */
async function loadAdminDashboardData() {
    loadStats();
    loadAdminAccounts();
    loadAdminLoans();
    loadAdminCustomers();
    loadAdminTransactions();
}

/* ── Helper: skeleton loading row ────────────────────────────────────────── */
function skeletonRow(cols, message) {
    return `<tr><td colspan="${cols}" class="text-center py-5 text-muted">
        <div class="d-flex flex-column align-items-center gap-2">
            <div class="spinner-border spinner-border-sm text-secondary" role="status"></div>
            <span class="small">${message}</span>
        </div>
    </td></tr>`;
}

function emptyRow(cols, message) {
    return `<tr><td colspan="${cols}" class="text-center py-5 text-muted">
        <i class="bi bi-inbox fs-3 d-block mb-2 opacity-50"></i>
        ${message}
    </td></tr>`;
}

function errorRow(cols, message) {
    return `<tr><td colspan="${cols}" class="text-center py-4 text-danger">
        <i class="bi bi-exclamation-triangle me-1"></i> ${message}
    </td></tr>`;
}

/* ══════════════════════════════════════════════════════════════════════════
   STATS
   ══════════════════════════════════════════════════════════════════════════ */
async function loadStats() {
    try {
        const res = await apiRequest('/api/admin/stats');
        const s = res.data || {};

        setText('admCustomersCount',    s.totalCustomers     ?? 0);
        setText('admAccountsCount',     s.totalAccounts      ?? 0);
        setText('admActiveAccCount',    s.activeAccounts     ?? 0);
        setText('admBlockedAccCount',   s.blockedAccounts    ?? 0);
        setText('admVolumeAmount',      formatCurrency(s.totalDepositVolume ?? 0));
        setText('admPendingLoansCount', s.pendingLoans       ?? 0);
        setText('admApprovedLoansCount',s.approvedLoans      ?? 0);
    } catch (err) {
        console.error('Stats load error:', err);
    }
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

/* ══════════════════════════════════════════════════════════════════════════
   ACCOUNTS TAB
   ══════════════════════════════════════════════════════════════════════════ */
async function loadAdminAccounts() {
    const tbody = document.getElementById('adminAccountsTbody');
    if (!tbody) return;
    tbody.innerHTML = skeletonRow(8, 'Fetching bank accounts…');

    try {
        const res = await apiRequest('/api/admin/accounts');
        const accounts = res.data || [];

        if (accounts.length === 0) {
            tbody.innerHTML = emptyRow(8, 'No bank accounts found in the system.');
            return;
        }

        tbody.innerHTML = accounts.map(acc => {
            const isBlocked = acc.status === 'BLOCKED';
            const isClosed  = acc.status === 'CLOSED';

            const statusBadge = (() => {
                if (acc.status === 'ACTIVE')  return '<span class="badge bg-success-subtle text-success badge-fintech">ACTIVE</span>';
                if (isBlocked)                 return '<span class="badge bg-danger-subtle text-danger badge-fintech">BLOCKED</span>';
                return '<span class="badge bg-secondary-subtle text-secondary badge-fintech">CLOSED</span>';
            })();

            const typeBadge = `<span class="badge bg-light text-dark badge-fintech">${acc.accountType || 'N/A'}</span>`;

            const actionBtn = (() => {
                if (isClosed)  return '<span class="text-muted small fst-italic">Account closed</span>';
                if (isBlocked) return `
                    <button class="btn btn-sm btn-outline-success" onclick="unblockAccount(${acc.id}, '${esc(acc.accountNumber)}')">
                        <i class="bi bi-unlock me-1"></i>Unblock
                    </button>`;
                return `
                    <button class="btn btn-sm btn-outline-danger" onclick="blockAccount(${acc.id}, '${esc(acc.accountNumber)}')">
                        <i class="bi bi-shield-x me-1"></i>Block
                    </button>`;
            })();

            return `
            <tr>
                <td><code class="fw-bold text-primary">${acc.accountNumber}</code></td>
                <td><strong>${esc(acc.userName)}</strong></td>
                <td class="small text-muted">${esc(acc.userEmail)}</td>
                <td>${typeBadge}</td>
                <td class="fw-bold text-success">${formatCurrency(acc.balance)}</td>
                <td>${statusBadge}</td>
                <td class="small text-muted">${formatDate(acc.createdAt)}</td>
                <td>${actionBtn}</td>
            </tr>`;
        }).join('');
    } catch (err) {
        tbody.innerHTML = errorRow(8, `Failed to load accounts: ${err.message}`);
    }
}

async function blockAccount(id, num) {
    if (!confirm(`⚠️  Block account ${num}?\n\nAll transactions on this account will be frozen immediately.`)) return;
    try {
        await apiRequest(`/api/admin/accounts/${id}/block`, { method: 'PUT' });
        showAlert(`Account ${num} has been blocked.`, 'success');
        loadAdminAccounts();
        loadStats();
    } catch (err) {
        showAlert(err.message || 'Failed to block account.', 'error');
    }
}

async function unblockAccount(id, num) {
    if (!confirm(`Unblock account ${num}?\n\nNormal transaction access will be restored.`)) return;
    try {
        await apiRequest(`/api/admin/accounts/${id}/unblock`, { method: 'PUT' });
        showAlert(`Account ${num} unblocked successfully.`, 'success');
        loadAdminAccounts();
        loadStats();
    } catch (err) {
        showAlert(err.message || 'Failed to unblock account.', 'error');
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   LOANS TAB
   ══════════════════════════════════════════════════════════════════════════ */
async function loadAdminLoans() {
    const tbody = document.getElementById('adminLoansTbody');
    if (!tbody) return;
    tbody.innerHTML = skeletonRow(9, 'Fetching loan applications…');

    try {
        const res = await apiRequest('/api/admin/loans');
        const loans = res.data || [];

        if (loans.length === 0) {
            tbody.innerHTML = emptyRow(9, 'No loan applications on record.');
            return;
        }

        tbody.innerHTML = loans.map(loan => {
            const isPending = loan.status === 'PENDING';

            const statusBadge = (() => {
                if (loan.status === 'APPROVED') return '<span class="badge bg-success-subtle text-success badge-fintech">APPROVED</span>';
                if (loan.status === 'REJECTED') return '<span class="badge bg-danger-subtle text-danger badge-fintech">REJECTED</span>';
                return '<span class="badge bg-warning-subtle text-warning badge-fintech">PENDING</span>';
            })();

            const actionHtml = isPending ? `
                <div class="d-flex gap-1">
                    <button class="btn btn-sm btn-success" onclick="approveLoan(${loan.id}, '${esc(loan.userName)}')">
                        <i class="bi bi-check-lg me-1"></i>Approve
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="rejectLoan(${loan.id}, '${esc(loan.userName)}')">
                        <i class="bi bi-x-lg me-1"></i>Reject
                    </button>
                </div>` : `<span class="small text-muted fst-italic">${esc(loan.remarks) || '—'}</span>`;

            // Monthly EMI preview
            const emi = calcEMI(loan.amount, loan.interestRate, loan.tenureMonths);

            return `
            <tr>
                <td><strong class="text-primary">#${loan.id}</strong></td>
                <td><strong>${esc(loan.userName)}</strong></td>
                <td class="small text-muted">${esc(loan.userEmail)}</td>
                <td class="fw-bold">${formatCurrency(loan.amount)}</td>
                <td>${loan.interestRate}%</td>
                <td>${loan.tenureMonths} Mo.
                    <br><span class="small text-muted">EMI ${formatCurrency(emi)}</span>
                </td>
                <td>${statusBadge}</td>
                <td class="small text-muted">${formatDate(loan.createdAt)}</td>
                <td>${actionHtml}</td>
            </tr>`;
        }).join('');
    } catch (err) {
        tbody.innerHTML = errorRow(9, `Failed to load loan applications: ${err.message}`);
    }
}

async function approveLoan(id, name) {
    const remarks = prompt(
        `Approve loan #${id} for ${name}?\n\nEnter approval remarks (or click Cancel to abort):`,
        'Approved by Credit Committee — Meets all underwriting criteria.'
    );
    if (remarks === null) return; // user cancelled

    try {
        await apiRequest(`/api/admin/loans/${id}/approve?remarks=${encodeURIComponent(remarks)}`, { method: 'PUT' });
        showAlert(`Loan #${id} approved successfully!`, 'success');
        loadAdminLoans();
        loadStats();
    } catch (err) {
        showAlert(err.message || 'Failed to approve loan.', 'error');
    }
}

async function rejectLoan(id, name) {
    const remarks = prompt(
        `Reject loan #${id} for ${name}?\n\nEnter rejection remarks (or click Cancel to abort):`,
        'Does not meet debt-to-income ratio requirements.'
    );
    if (remarks === null) return; // user cancelled

    try {
        await apiRequest(`/api/admin/loans/${id}/reject?remarks=${encodeURIComponent(remarks)}`, { method: 'PUT' });
        showAlert(`Loan #${id} has been rejected.`, 'warning');
        loadAdminLoans();
        loadStats();
    } catch (err) {
        showAlert(err.message || 'Failed to reject loan.', 'error');
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   CUSTOMERS TAB
   ══════════════════════════════════════════════════════════════════════════ */
async function loadAdminCustomers() {
    const tbody = document.getElementById('adminCustomersTbody');
    if (!tbody) return;
    tbody.innerHTML = skeletonRow(6, 'Fetching customer records…');

    try {
        const res = await apiRequest('/api/admin/customers');
        const customers = res.data || [];

        if (customers.length === 0) {
            tbody.innerHTML = emptyRow(6, 'No customers registered yet.');
            return;
        }

        tbody.innerHTML = customers.map(u => {
            // Avatar initials
            const initials = u.name
                ? u.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
                : '??';

            const roleClass = (() => {
                if (u.role === 'ADMIN')         return 'bg-danger-subtle text-danger';
                if (u.role === 'BANK_EMPLOYEE') return 'bg-info-subtle text-info';
                return 'bg-success-subtle text-success';
            })();

            return `
            <tr>
                <td class="text-muted small">#${u.id}</td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold text-white"
                             style="width:34px;height:34px;font-size:0.75rem;background:var(--apex-primary,#1a56db);flex-shrink:0;">
                            ${initials}
                        </div>
                        <strong>${esc(u.name)}</strong>
                    </div>
                </td>
                <td class="small">${esc(u.email)}</td>
                <td class="small text-muted">${esc(u.phone) || '<span class="fst-italic">N/A</span>'}</td>
                <td><span class="badge badge-fintech ${roleClass}">${u.role}</span></td>
                <td class="small text-muted">${formatDate(u.createdAt)}</td>
            </tr>`;
        }).join('');
    } catch (err) {
        tbody.innerHTML = errorRow(6, `Failed to load customers: ${err.message}`);
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   AUDIT / TRANSACTIONS TAB
   ══════════════════════════════════════════════════════════════════════════ */
async function loadAdminTransactions() {
    const tbody = document.getElementById('adminAuditTbody');
    if (!tbody) return;
    tbody.innerHTML = skeletonRow(8, 'Fetching system-wide transaction ledger…');

    try {
        const res = await apiRequest('/api/admin/transactions?page=0&size=25');
        const txs = res.data && res.data.content ? res.data.content : [];

        if (txs.length === 0) {
            tbody.innerHTML = emptyRow(8, 'No transactions recorded yet.');
            return;
        }

        tbody.innerHTML = txs.map(tx => {
            const typeBadge = (() => {
                if (tx.type === 'DEPOSIT')    return '<span class="badge bg-success-subtle text-success badge-fintech">DEPOSIT</span>';
                if (tx.type === 'WITHDRAWAL') return '<span class="badge bg-danger-subtle text-danger badge-fintech">WITHDRAWAL</span>';
                return '<span class="badge bg-primary-subtle text-primary badge-fintech">TRANSFER</span>';
            })();

            const statusBadge = tx.status === 'COMPLETED'
                ? '<span class="badge bg-success-subtle text-success badge-fintech">COMPLETED</span>'
                : '<span class="badge bg-warning-subtle text-warning badge-fintech">PENDING</span>';

            return `
            <tr>
                <td><code class="fw-bold small text-primary">${esc(tx.transactionReference)}</code></td>
                <td class="small text-muted text-nowrap">${formatDate(tx.timestamp)}</td>
                <td>${typeBadge}</td>
                <td class="small font-monospace">${esc(tx.sourceAccountNumber) || '—'}</td>
                <td class="small font-monospace">${esc(tx.targetAccountNumber) || '—'}</td>
                <td class="fw-bold">${formatCurrency(tx.amount)}</td>
                <td>${statusBadge}</td>
                <td class="small text-muted">${esc(tx.description) || '—'}</td>
            </tr>`;
        }).join('');
    } catch (err) {
        tbody.innerHTML = errorRow(8, `Failed to load audit trail: ${err.message}`);
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   UTILITY HELPERS
   ══════════════════════════════════════════════════════════════════════════ */

/** Safe HTML escape — prevents XSS from server data rendered in table cells */
function esc(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#x27;');
}

/** EMI Calculator:  EMI = P × r(1+r)^n / ((1+r)^n − 1) */
function calcEMI(principal, annualRate, months) {
    if (!principal || !annualRate || !months) return 0;
    const r = (annualRate / 100) / 12;
    if (r === 0) return principal / months;
    const factor = Math.pow(1 + r, months);
    return (principal * r * factor) / (factor - 1);
}
