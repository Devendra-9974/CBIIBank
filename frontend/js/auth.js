/**
 * CBII BANK ONLINE NETBANKING - AUTH & SESSION MANAGER
 */

const Auth = {
    getToken() {
        return localStorage.getItem('token');
    },

    getUser() {
        const userStr = localStorage.getItem('user');
        try {
            return userStr ? JSON.parse(userStr) : null;
        } catch (e) {
            return null;
        }
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    setUserSession(authResponse) {
        localStorage.setItem('token', authResponse.token);
        localStorage.setItem('user', JSON.stringify({
            id: authResponse.id,
            name: authResponse.name,
            email: authResponse.email,
            role: authResponse.role
        }));
    },

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = 'login.html';
    },

    requireAuth(allowedRoles = []) {
        if (!this.isAuthenticated()) {
            window.location.href = 'login.html';
            return false;
        }

        const user = this.getUser();
        if (allowedRoles.length > 0 && user && !allowedRoles.includes(user.role)) {
            showAlert('Access Restricted: You do not have permissions for this module', 'error');
            setTimeout(() => {
                if (user.role === 'ADMIN' || user.role === 'BANK_EMPLOYEE') {
                    window.location.href = 'admin.html';
                } else {
                    window.location.href = 'dashboard.html';
                }
            }, 1200);
            return false;
        }

        this.renderUserNav();
        return true;
    },

    redirectIfAuthenticated() {
        if (this.isAuthenticated()) {
            const user = this.getUser();
            if (user && (user.role === 'ADMIN' || user.role === 'BANK_EMPLOYEE')) {
                window.location.href = 'admin.html';
            } else {
                window.location.href = 'dashboard.html';
            }
        }
    },

    renderUserNav() {
        const user = this.getUser();
        if (!user) return;

        // Header elements
        const nameElements = document.querySelectorAll('.user-display-name');
        const emailElements = document.querySelectorAll('.user-display-email');
        const roleElements = document.querySelectorAll('.user-display-role');
        const avatarElements = document.querySelectorAll('.user-display-avatar');
        const adminLinks = document.querySelectorAll('.nav-admin-only');

        nameElements.forEach(el => el.textContent = user.name);
        emailElements.forEach(el => el.textContent = user.email);
        roleElements.forEach(el => {
            el.textContent = user.role.replace('_', ' ');
            if (user.role === 'ADMIN') el.className = 'badge bg-danger user-display-role';
            else if (user.role === 'BANK_EMPLOYEE') el.className = 'badge bg-info text-dark user-display-role';
            else el.className = 'badge bg-success user-display-role';
        });

        const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
        avatarElements.forEach(el => el.textContent = initials);

        adminLinks.forEach(el => {
            if (user.role === 'ADMIN' || user.role === 'BANK_EMPLOYEE') {
                el.classList.remove('d-none');
            } else {
                el.classList.add('d-none');
            }
        });
    }
};
