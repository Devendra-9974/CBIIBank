/**
 * CBII BANK - TRANSACTIONS LEDGER CONTROLLER
 */

let currentLedgerPage = 0;
let fetchedLedgerTransactions = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.requireAuth()) return;

    loadTransactionsLedger(0);

    document.getElementById('txSearchInput').addEventListener('input', debounce(() => {
        applyLedgerFilters();
    }, 250));

    document.getElementById('txTypeFilter').addEventListener('change', () => {
        applyLedgerFilters();
    });

    document.getElementById('txPageSizeSelect').addEventListener('change', () => {
        loadTransactionsLedger(0);
    });
});

async function loadTransactionsLedger(page = 0) {
    currentLedgerPage = page;
    const pageSize = document.getElementById('txPageSizeSelect').value;
    const tbody = document.getElementById('ledgerTableBody');

    tbody.innerHTML = `<tr><td colspan="9" class="text-center py-5 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div> Loading entries...</td></tr>`;

    try {
        const response = await apiRequest(`/api/transactions?page=${currentLedgerPage}&size=${pageSize}&paged=true`);
        const pagedData = response.data;

        fetchedLedgerTransactions = pagedData.content || [];
        renderLedgerTable(fetchedLedgerTransactions);
        renderLedgerPagination(pagedData);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-danger">Failed to load transactions: ${err.message}</td></tr>`;
    }
}

function applyLedgerFilters() {
    const search = document.getElementById('txSearchInput').value.toLowerCase().trim();
    const type = document.getElementById('txTypeFilter').value;

    const filtered = fetchedLedgerTransactions.filter(tx => {
        const matchType = type === 'ALL' || tx.type === type;
        const matchSearch = !search ||
            tx.transactionReference.toLowerCase().includes(search) ||
            (tx.description && tx.description.toLowerCase().includes(search)) ||
            (tx.sourceAccountNumber && tx.sourceAccountNumber.toLowerCase().includes(search)) ||
            (tx.targetAccountNumber && tx.targetAccountNumber.toLowerCase().includes(search));

        return matchType && matchSearch;
    });

    renderLedgerTable(filtered);
}

function renderLedgerTable(transactions) {
    const tbody = document.getElementById('ledgerTableBody');

    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-5 text-muted small">No transactions matching your criteria</td></tr>`;
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

        return `
            <tr>
                <td><code class="fw-bold text-dark">${tx.transactionReference}</code></td>
                <td class="small text-muted">${formatDate(tx.timestamp)}</td>
                <td>${typeBadge}</td>
                <td><span class="font-monospace small">${tx.sourceAccountNumber !== 'N/A' ? tx.sourceAccountNumber : 'Cash/Counter'}</span></td>
                <td><span class="font-monospace small">${tx.targetAccountNumber !== 'N/A' ? tx.targetAccountNumber : 'Cash/Counter'}</span></td>
                <td><span class="small text-muted text-truncate d-inline-block" style="max-width: 160px;">${tx.description || 'N/A'}</span></td>
                <td class="${amountClass}">${sign}${formatCurrency(tx.amount)}</td>
                <td><span class="badge bg-light text-dark small">${tx.status}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-secondary py-0 px-2" onclick="openLedgerReceipt(${tx.id})" title="View Voucher">
                        <i class="bi bi-receipt"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderLedgerPagination(pagedData) {
    const pageInfo = document.getElementById('ledgerPageInfo');
    const nav = document.getElementById('ledgerPaginationNav');

    const total = pagedData.totalElements || 0;
    const start = total === 0 ? 0 : (pagedData.pageNumber * pagedData.pageSize) + 1;
    const end = Math.min((pagedData.pageNumber + 1) * pagedData.pageSize, total);

    pageInfo.textContent = `Showing ${start} to ${end} of ${total} entries`;

    if (pagedData.totalPages <= 1) {
        nav.innerHTML = '';
        return;
    }

    let html = '';
    html += `
        <li class="page-item ${pagedData.pageNumber === 0 ? 'disabled' : ''}">
            <button class="page-link" onclick="loadTransactionsLedger(${pagedData.pageNumber - 1})">Prev</button>
        </li>
    `;

    for (let i = 0; i < pagedData.totalPages; i++) {
        html += `
            <li class="page-item ${pagedData.pageNumber === i ? 'active' : ''}">
                <button class="page-link" onclick="loadTransactionsLedger(${i})">${i + 1}</button>
            </li>
        `;
    }

    html += `
        <li class="page-item ${pagedData.isLast ? 'disabled' : ''}">
            <button class="page-link" onclick="loadTransactionsLedger(${pagedData.pageNumber + 1})">Next</button>
        </li>
    `;

    nav.innerHTML = html;
}

function exportStatementCsv() {
    if (fetchedLedgerTransactions.length === 0) {
        showAlert('No transaction records available to export.', 'warning');
        return;
    }

    const headers = ['Reference ID', 'Date Time', 'Type', 'Source Account', 'Target Account', 'Amount', 'Status', 'Memo'];
    const rows = fetchedLedgerTransactions.map(tx => [
        tx.transactionReference,
        formatDate(tx.timestamp),
        tx.type,
        tx.sourceAccountNumber,
        tx.targetAccountNumber,
        tx.amount,
        tx.status,
        tx.description || ''
    ]);

    exportToCsv('Apex_Bank_Statement', headers, rows);
}

function openLedgerReceipt(txId) {
    const tx = fetchedLedgerTransactions.find(t => t.id === txId);
    if (!tx) return;

    document.getElementById('rcptAmount').textContent = formatCurrency(tx.amount);
    document.getElementById('rcptStatus').textContent = tx.status;
    document.getElementById('rcptRef').textContent = tx.transactionReference;
    document.getElementById('rcptType').textContent = tx.type;
    document.getElementById('rcptSource').textContent = tx.sourceAccountNumber !== 'N/A' ? `${tx.sourceAccountNumber} (${tx.sourceUserName || 'Sender'})` : 'Cash/Counter Deposit';
    document.getElementById('rcptTarget').textContent = tx.targetAccountNumber !== 'N/A' ? `${tx.targetAccountNumber} (${tx.targetUserName || 'Receiver'})` : 'Cash Withdrawal';
    document.getElementById('rcptDesc').textContent = tx.description || 'None';
    document.getElementById('rcptDate').textContent = formatDate(tx.timestamp);

    new bootstrap.Modal(document.getElementById('receiptModal')).show();
}

function debounce(func, wait) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}
