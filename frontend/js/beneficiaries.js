/**
 * CBII BANK - BENEFICIARIES CONTROLLER
 */

document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.requireAuth()) return;

    loadBeneficiariesDirectory();

    document.getElementById('addBeneficiaryForm').addEventListener('submit', handleAddBeneficiarySubmit);
});

async function loadBeneficiariesDirectory() {
    const tbody = document.getElementById('beneficiariesTableBody');

    try {
        const res = await apiRequest('/api/beneficiaries');
        const list = res.data || [];

        if (list.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-5 text-muted">
                        <i class="bi bi-people fs-2 text-muted"></i>
                        <h6 class="mt-3 fw-bold text-dark">No Saved Payees</h6>
                        <p class="small text-muted mb-2">Save counterparty bank accounts to send funds without typing details each time.</p>
                        <button class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#addBeneficiaryModal">+ Add First Payee</button>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = list.map(b => {
            const initials = b.beneficiaryName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();

            return `
                <tr>
                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <div class="rounded-circle bg-light border text-primary fw-bold d-flex align-items-center justify-content-center" 
                                 style="width: 34px; height: 34px; font-size: 0.8rem;">
                                ${initials}
                            </div>
                            <strong>${b.beneficiaryName}</strong>
                        </div>
                    </td>
                    <td><code class="fw-bold text-dark">${b.accountNumber}</code></td>
                    <td><span class="badge bg-light text-dark fw-semibold"><i class="bi bi-bank me-1"></i>${b.bankName}</span></td>
                    <td><span class="font-monospace text-muted small">${b.ifscCode}</span></td>
                    <td class="small text-muted">${formatDate(b.createdAt)}</td>
                    <td>
                        <div class="d-flex gap-2">
                            <a href="transfer.html" class="btn btn-sm btn-primary py-0 px-2 small">
                                <i class="bi bi-send me-1"></i> Pay
                            </a>
                            <button class="btn btn-sm btn-outline-danger py-0 px-2" onclick="deleteBeneficiaryPayee(${b.id}, '${b.beneficiaryName}')">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger">Failed to load payees: ${err.message}</td></tr>`;
    }
}

async function handleAddBeneficiarySubmit(e) {
    e.preventDefault();

    const beneficiaryName = document.getElementById('benName').value.trim();
    const accountNumber = document.getElementById('benAccount').value.trim();
    const bankName = document.getElementById('benBank').value.trim();
    const ifscCode = document.getElementById('benIfsc').value.trim().toUpperCase();

    if (!beneficiaryName || !accountNumber || !bankName || !ifscCode) {
        showAlert('Please complete all payee fields', 'error');
        return;
    }

    const btn = document.getElementById('addBenBtn');
    btn.disabled = true;

    try {
        await apiRequest('/api/beneficiaries', {
            method: 'POST',
            body: JSON.stringify({
                beneficiaryName,
                accountNumber,
                bankName,
                ifscCode
            })
        });

        showAlert('Beneficiary payee saved successfully!', 'success');
        bootstrap.Modal.getInstance(document.getElementById('addBeneficiaryModal')).hide();
        document.getElementById('addBeneficiaryForm').reset();
        loadBeneficiariesDirectory();
    } catch (err) {
        showAlert(err.message || 'Failed to save beneficiary', 'error');
    } finally {
        btn.disabled = false;
    }
}

async function deleteBeneficiaryPayee(id, name) {
    if (!confirm(`Are you sure you want to remove ${name} from your registered payees?`)) {
        return;
    }

    try {
        await apiRequest(`/api/beneficiaries/${id}`, { method: 'DELETE' });
        showAlert(`Payee ${name} removed successfully`, 'success');
        loadBeneficiariesDirectory();
    } catch (err) {
        showAlert(err.message || 'Failed to delete beneficiary', 'error');
    }
}
