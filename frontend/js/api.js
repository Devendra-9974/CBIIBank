/**
 * CBII BANK - API & UTILITY ENGINE
 */

const API_BASE_URL = window.location.origin.includes(':8080') 
    ? window.location.origin 
    :"https://cbiibank.onrender.com" ;

/**
 * Universal API Fetch Client
 */
async function apiRequest(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
    const token = localStorage.getItem('token');

    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers
        });

        // 401 Unauthorized Handling
        if (response.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            if (!window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('index.html')) {
                window.location.href = 'login.html?error=session_expired';
            }
            throw new Error('Your session has expired. Please log in again.');
        }

        const data = await response.json();

        if (!response.ok) {
            let errorMsg = data.message || 'Operation failed';
            if (data.data && typeof data.data === 'object') {
                const validationList = Object.values(data.data).join(', ');
                errorMsg = `${errorMsg}: ${validationList}`;
            }
            throw new Error(errorMsg);
        }

        return data;
    } catch (error) {
        console.error(`[API Error] ${endpoint}:`, error);
        throw error;
    }
}

/**
 * Toast Notification System
 */
function showAlert(message, type = 'success', duration = 4000) {
    let container = document.getElementById('alert-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'alert-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `fintech-toast toast-${type === 'error' ? 'error' : type === 'warning' ? 'warning' : 'success'}`;
    
    const icon = type === 'success' ? 'bi-check-circle-fill text-success' : 
                 type === 'error' ? 'bi-x-circle-fill text-danger' : 
                 type === 'warning' ? 'bi-exclamation-triangle-fill text-warning' : 'bi-info-circle-fill text-primary';

    toast.innerHTML = `
        <i class="bi ${icon} fs-4"></i>
        <div class="flex-grow-1">
            <div class="fw-semibold text-dark small">${type.toUpperCase()}</div>
            <div class="small text-secondary">${message}</div>
        </div>
        <button type="button" class="btn-close btn-close-sm ms-2" onclick="this.parentElement.remove()"></button>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        if (toast.parentElement) {
            toast.style.transition = 'all 0.3s ease';
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            setTimeout(() => toast.remove(), 300);
        }
    }, duration);
}

/**
 * Currency Formatter (INR)
 */
function formatCurrency(amount) {
    if (amount === null || amount === undefined || isNaN(amount)) return '₹0.00';
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    }).format(amount);
}

/**
 * ISO Date Formatter
 */
function formatDate(isoString) {
    if (!isoString) return 'N/A';
    const date = new Date(isoString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * Mask Account Number (e.g. •••• •••• 0300)
 */
function maskAccountNumber(accNumber) {
    if (!accNumber || accNumber.length < 4) return accNumber || 'N/A';
    return '•••• ' + accNumber.slice(-4);
}

/**
 * Copy Text to Clipboard with Feedback
 */
function copyToClipboard(text, label = 'Account number') {
    navigator.clipboard.writeText(text).then(() => {
        showAlert(`${label} copied to clipboard!`, 'success', 2500);
    }).catch(err => {
        console.error('Failed to copy text: ', err);
    });
}

/**
 * Export Rows to CSV File
 */
function exportToCsv(filename, headers, rows) {
    const csvRows = [];
    csvRows.push(headers.join(','));

    rows.forEach(row => {
        const values = row.map(val => {
            const escaped = ('' + (val || '')).replace(/"/g, '""');
            return `"${escaped}"`;
        });
        csvRows.push(values.join(','));
    });

    const csvContent = 'data:text/csv;charset=utf-8,' + csvRows.join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `${filename}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showAlert(`Exported ${filename}.csv successfully!`, 'success');
}
