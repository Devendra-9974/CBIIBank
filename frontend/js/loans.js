/**
 * CBII BANK - LOANS CONTROLLER
 */

document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.requireAuth()) return;

    loadLoansList();

    // Simulator inputs
    const simAmount = document.getElementById('simAmountRange');
    const simTenure = document.getElementById('simTenureRange');
    if (simAmount && simTenure) {
        simAmount.addEventListener('input', updateSimulator);
        simTenure.addEventListener('input', updateSimulator);
        updateSimulator();
    }

    // Modal inputs
    const modalAmount = document.getElementById('modalLoanAmount');
    const modalTenure = document.getElementById('modalLoanTenure');
    if (modalAmount && modalTenure) {
        modalAmount.addEventListener('input', updateModalEmi);
        modalTenure.addEventListener('input', updateModalEmi);
        updateModalEmi();
    }

    document.getElementById('applyLoanForm').addEventListener('submit', handleLoanApplicationSubmit);
});

async function loadLoansList() {
    const tbody = document.getElementById('loansTableBody');

    try {
        const res = await apiRequest('/api/loans/my-loans');
        const loans = res.data || [];

        if (loans.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5 text-muted">
                        <i class="bi bi-cash-coin fs-2 text-muted"></i>
                        <h6 class="mt-3 fw-bold text-dark">No Active Loan Applications</h6>
                        <p class="small text-muted mb-2">You have not submitted any retail loan applications.</p>
                        <button class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#applyLoanModal">Apply for Financing</button>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = loans.map(loan => {
            let statusBadge = '';
            if (loan.status === 'APPROVED') {
                statusBadge = '<span class="badge bg-success-subtle text-success badge-fintech">APPROVED</span>';
            } else if (loan.status === 'REJECTED') {
                statusBadge = '<span class="badge bg-danger-subtle text-danger badge-fintech">REJECTED</span>';
            } else {
                statusBadge = '<span class="badge bg-warning-subtle text-warning badge-fintech">PENDING REVIEW</span>';
            }

            const emi = calculateEmiValue(loan.amount, loan.interestRate, loan.tenureMonths);

            return `
                <tr>
                    <td><strong>#${loan.id}</strong></td>
                    <td class="fw-bold text-primary">${formatCurrency(loan.amount)}</td>
                    <td><span class="badge bg-light text-dark">${loan.interestRate}% APR</span></td>
                    <td>${loan.tenureMonths} Months</td>
                    <td class="fw-semibold text-dark">${formatCurrency(emi)} / mo</td>
                    <td>${statusBadge}</td>
                    <td class="small text-muted">${formatDate(loan.createdAt)}</td>
                    <td class="small text-muted">${loan.remarks || 'Underwriting in progress'}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-danger">Failed to load loans: ${err.message}</td></tr>`;
    }
}

function calculateEmiValue(principal, annualRate, tenureMonths) {
    if (!principal || !annualRate || !tenureMonths || tenureMonths <= 0) return 0;
    const monthlyRate = (annualRate / 12) / 100;
    const emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)) / (Math.pow(1 + monthlyRate, tenureMonths) - 1);
    return isFinite(emi) ? emi : 0;
}

function updateSimulator() {
    const amount = parseFloat(document.getElementById('simAmountRange').value) || 0;
    const tenure = parseInt(document.getElementById('simTenureRange').value) || 0;

    let rate = 8.50;
    if (amount > 50000 && amount <= 500000) rate = 9.25;
    else if (amount > 500000) rate = 10.50;

    document.getElementById('simAmountLabel').textContent = formatCurrency(amount);
    document.getElementById('simTenureLabel').textContent = `${tenure} Months`;
    document.getElementById('simRateResult').textContent = `${rate.toFixed(2)}% APR`;

    const emi = calculateEmiValue(amount, rate, tenure);
    const totalCost = emi * tenure;

    document.getElementById('simEmiResult').textContent = formatCurrency(emi);
    document.getElementById('simTotalCostResult').textContent = formatCurrency(totalCost);
}

function updateModalEmi() {
    const amount = parseFloat(document.getElementById('modalLoanAmount').value) || 0;
    const tenure = parseInt(document.getElementById('modalLoanTenure').value) || 0;

    let rate = 8.50;
    if (amount > 50000 && amount <= 500000) rate = 9.25;
    else if (amount > 500000) rate = 10.50;

    const emi = calculateEmiValue(amount, rate, tenure);
    document.getElementById('modalCalculatedEmi').textContent = `${formatCurrency(emi)} / mo (${rate}% APR)`;
}

async function handleLoanApplicationSubmit(e) {
    e.preventDefault();

    const amount = parseFloat(document.getElementById('modalLoanAmount').value);
    const tenureMonths = parseInt(document.getElementById('modalLoanTenure').value);
    const remarks = document.getElementById('modalLoanRemarks').value.trim();

    if (isNaN(amount) || amount < 1000) {
        showAlert('Minimum loan amount is ₹1,000', 'error');
        return;
    }

    if (isNaN(tenureMonths) || tenureMonths < 3) {
        showAlert('Minimum tenure is 3 months', 'error');
        return;
    }

    const btn = document.getElementById('modalApplyBtn');
    btn.disabled = true;

    try {
        await apiRequest('/api/loans/apply', {
            method: 'POST',
            body: JSON.stringify({
                amount,
                tenureMonths,
                remarks
            })
        });

        showAlert('Loan application successfully submitted! A credit officer will review it.', 'success');
        bootstrap.Modal.getInstance(document.getElementById('applyLoanModal')).hide();
        document.getElementById('applyLoanForm').reset();
        loadLoansList();
    } catch (err) {
        showAlert(err.message || 'Failed to submit loan application', 'error');
    } finally {
        btn.disabled = false;
    }
}
