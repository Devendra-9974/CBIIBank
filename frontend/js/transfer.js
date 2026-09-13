/**
 * CBII BANK - TRANSFER CONTROLLER
 */

let userTransferAccounts = [];
let userTransferBeneficiaries = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.requireAuth()) return;

    loadTransferFormData();

    document.querySelectorAll('input[name="destMode"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            const isBeneficiary = e.target.value === 'BENEFICIARY';
            document.getElementById('beneficiarySelectGroup').classList.toggle('d-none', !isBeneficiary);
            document.getElementById('manualAccountGroup').classList.toggle('d-none', isBeneficiary);
        });
    });

    document.getElementById('sourceAccount').addEventListener('change', updateSourceBalanceBadge);
    document.getElementById('transferForm').addEventListener('submit', handleTransferFormSubmit);
});

async function loadTransferFormData() {
    try {
        // Load active accounts
        const accRes = await apiRequest('/api/customers/accounts');
        userTransferAccounts = (accRes.data || []).filter(a => a.status === 'ACTIVE');

        const sourceSelect = document.getElementById('sourceAccount');
        sourceSelect.innerHTML = '<option value="">Select source account...</option>';
        userTransferAccounts.forEach(acc => {
            const opt = document.createElement('option');
            opt.value = acc.accountNumber;
            opt.textContent = `${acc.accountType} (${maskAccountNumber(acc.accountNumber)}) - Balance: ${formatCurrency(acc.balance)}`;
            sourceSelect.appendChild(opt);
        });

        // Load beneficiaries
        const benRes = await apiRequest('/api/beneficiaries');
        userTransferBeneficiaries = benRes.data || [];

        const benSelect = document.getElementById('beneficiarySelect');
        if (userTransferBeneficiaries.length === 0) {
            benSelect.innerHTML = '<option value="">No saved payees found. Use Manual Mode or Add Payee.</option>';
        } else {
            benSelect.innerHTML = '<option value="">Select a saved payee...</option>';
            userTransferBeneficiaries.forEach(ben => {
                const opt = document.createElement('option');
                opt.value = ben.accountNumber;
                opt.textContent = `${ben.beneficiaryName} - ${ben.bankName} (${ben.accountNumber})`;
                benSelect.appendChild(opt);
            });
        }
    } catch (err) {
        showAlert('Could not load accounts or payees: ' + err.message, 'error');
    }
}

function updateSourceBalanceBadge() {
    const selectedAcc = document.getElementById('sourceAccount').value;
    const infoBox = document.getElementById('sourceBalanceInfo');
    const amountEl = document.getElementById('sourceBalanceAmount');

    const account = userTransferAccounts.find(a => a.accountNumber === selectedAcc);
    if (account) {
        infoBox.classList.remove('d-none');
        amountEl.textContent = formatCurrency(account.balance);
    } else {
        infoBox.classList.add('d-none');
    }
}

async function handleTransferFormSubmit(e) {
    e.preventDefault();

    const sourceAccountNumber = document.getElementById('sourceAccount').value;
    const destMode = document.querySelector('input[name="destMode"]:checked').value;
    let targetAccountNumber = '';

    if (destMode === 'BENEFICIARY') {
        targetAccountNumber = document.getElementById('beneficiarySelect').value;
    } else {
        targetAccountNumber = document.getElementById('targetAccount').value.trim();
    }

    const amount = parseFloat(document.getElementById('transferAmount').value);
    const description = document.getElementById('transferDescription').value.trim();

    if (!sourceAccountNumber) {
        showAlert('Please select an active source account.', 'error');
        return;
    }

    if (!targetAccountNumber) {
        showAlert('Please specify or select a recipient destination account.', 'error');
        return;
    }

    if (sourceAccountNumber === targetAccountNumber) {
        showAlert('Source and destination accounts cannot be the same account.', 'error');
        return;
    }

    if (isNaN(amount) || amount <= 0) {
        showAlert('Transfer amount must be greater than ₹0.00.', 'error');
        return;
    }

    const btn = document.getElementById('transferSubmitBtn');
    const spinner = document.getElementById('transferSpinner');
    btn.disabled = true;
    spinner.classList.remove('d-none');

    try {
        const response = await apiRequest('/api/transactions/transfer', {
            method: 'POST',
            body: JSON.stringify({
                sourceAccountNumber,
                targetAccountNumber,
                amount,
                description: description || 'Instant Online Fund Transfer'
            })
        });

        const tx = response.data;
        showTransferReceipt(tx);
        document.getElementById('transferForm').reset();
        document.getElementById('sourceBalanceInfo').classList.add('d-none');
        loadTransferFormData();
    } catch (err) {
        showAlert(err.message || 'Transfer failed.', 'error');
    } finally {
        btn.disabled = false;
        spinner.classList.add('d-none');
    }
}

function showTransferReceipt(tx) {
    document.getElementById('rcptAmount').textContent = formatCurrency(tx.amount);
    document.getElementById('rcptRef').textContent = tx.transactionReference;
    document.getElementById('rcptSource').textContent = `${tx.sourceAccountNumber} (${tx.sourceUserName || 'Account Holder'})`;
    document.getElementById('rcptTarget').textContent = `${tx.targetAccountNumber} (${tx.targetUserName || 'Recipient'})`;
    document.getElementById('rcptDesc').textContent = tx.description || 'Instant Transfer';
    document.getElementById('rcptDate').textContent = formatDate(tx.timestamp);

    new bootstrap.Modal(document.getElementById('receiptModal')).show();
}
