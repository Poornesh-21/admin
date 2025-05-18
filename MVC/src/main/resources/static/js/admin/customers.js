document.addEventListener('DOMContentLoaded', function() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const sidebar = document.getElementById('sidebar');

    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', function() {
            sidebar.classList.toggle('active');
        });
    }

    function showSpinner() {
        const spinner = document.getElementById('spinnerOverlay');
        spinner.classList.add('show');
    }

    function hideSpinner() {
        const spinner = document.getElementById('spinnerOverlay');
        setTimeout(() => {
            spinner.classList.remove('show');
        }, 300);
    }

    function showConfirmation(title, message) {
        document.getElementById('confirmationTitle').textContent = title;
        document.getElementById('confirmationMessage').textContent = message;
        const successModal = new bootstrap.Modal(document.getElementById('successModal'));
        successModal.show();
    }

    function getToken() {
        return localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");
    }

    const token = getToken();
    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
    }

    document.querySelectorAll('.sidebar-menu-link').forEach(link => {
        const href = link.getAttribute('href');

        if (href && !href.startsWith('/admin/') && !href.startsWith('#')) {
            link.setAttribute('href', '/admin' + href);
        }

        if (href && href.includes('token=')) {
            const url = new URL(href, window.location.origin);
            url.searchParams.delete('token');
            link.setAttribute('href', url.pathname + url.search);
        }

        if (window.location.pathname.includes(href)) {
            link.classList.add('active');
        }
    });

    const filterPills = document.querySelectorAll('.filter-pill');
    filterPills.forEach(pill => {
        pill.addEventListener('click', function() {
            filterPills.forEach(p => p.classList.remove('active'));
            this.classList.add('active');

            const filter = this.textContent.trim();
            const rows = document.querySelectorAll('.customer-row');

            rows.forEach(row => {
                if (filter === 'All Customers') {
                    row.style.display = '';
                } else if (filter === 'Premium Members') {
                    const membershipBadge = row.querySelector('.membership-badge');
                    if (membershipBadge && membershipBadge.classList.contains('premium')) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                } else if (filter === 'Standard Members') {
                    const membershipBadge = row.querySelector('.membership-badge');
                    if (membershipBadge && membershipBadge.classList.contains('standard')) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                }
            });
        });
    });

    const searchInput = document.getElementById('customerSearch');
    if (searchInput) {
        searchInput.addEventListener('keyup', function() {
            const searchTerm = this.value.toLowerCase();
            const rows = document.querySelectorAll('.customer-row');

            rows.forEach(row => {
                const customerName = row.querySelector('.customer-name').textContent.toLowerCase();
                const email = row.querySelector('td:nth-child(2)').textContent.toLowerCase();
                const phone = row.querySelector('.phone-number').textContent.toLowerCase();

                if (customerName.includes(searchTerm) || email.includes(searchTerm) || phone.includes(searchTerm)) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        });
    }

    const profileTabs = document.querySelectorAll('.profile-tab');
    profileTabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const tabId = this.getAttribute('data-tab');

            profileTabs.forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

            this.classList.add('active');
            document.getElementById(`${tabId}-tab`).classList.add('active');
        });
    });

    const customerRows = document.querySelectorAll('.customer-row');
    customerRows.forEach(row => {
        row.addEventListener('click', function() {
            const customerId = this.getAttribute('data-customer-id');

            showSpinner();

            fetch(`/admin/customers/api/${customerId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + getToken()
                }
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Server responded with status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(customer => {
                    populateCustomerDetails(customer);
                    hideSpinner();
                    const customerDetailsModal = new bootstrap.Modal(document.getElementById('customerDetailsModal'));
                    customerDetailsModal.show();
                })
                .catch(error => {
                    hideSpinner();
                    showToast('Error', 'Failed to load customer details. Please try again.', 'error');
                });
        });
    });

    function populateCustomerDetails(customer) {
        document.getElementById('viewCustomerInitials').textContent = getInitials(customer.firstName, customer.lastName);
        document.getElementById('viewCustomerName').textContent = `${customer.firstName} ${customer.lastName}`;
        document.getElementById('viewCustomerEmail').textContent = customer.email;
        document.getElementById('viewCustomerPhone').textContent = customer.phoneNumber || 'Not provided';

        const address = [customer.street, customer.city, customer.state, customer.postalCode]
            .filter(part => part && part.trim() !== '')
            .join(', ');
        document.getElementById('viewCustomerAddress').textContent = address || 'Not provided';

        document.getElementById('viewCustomerMembership').textContent = customer.membershipStatus || 'Standard';
        document.getElementById('viewCustomerServices').textContent = customer.totalServices || '0';
        document.getElementById('viewCustomerLastService').textContent = customer.lastServiceDate
            ? new Date(customer.lastServiceDate).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
            : 'No service yet';

        document.getElementById('editCustomerFromDetailsBtn').setAttribute('data-customer-id', customer.customerId);
    }

    function getInitials(firstName, lastName) {
        const firstInitial = firstName && firstName.length > 0 ? firstName.charAt(0).toUpperCase() : '';
        const lastInitial = lastName && lastName.length > 0 ? lastName.charAt(0).toUpperCase() : '';
        return firstInitial + lastInitial;
    }

    function validateCustomerForm(formId, singleFieldId = null) {
        try {
            let ids = {};
            let errorIds = {};

            if (formId === 'editCustomerForm') {
                ids = {
                    firstName: 'editFirstName',
                    lastName: 'editLastName',
                    email: 'editEmail',
                    phone: 'editPhone',
                    street: 'editStreet',
                    city: 'editCity',
                    state: 'editState',
                    postalCode: 'editPostalCode',
                    membershipStatus: 'editMembershipStatus'
                };
                errorIds = {
                    firstName: 'editFirstName-error',
                    lastName: 'editLastName-error',
                    email: 'editEmail-error',
                    phone: 'editPhone-error',
                    street: 'editStreet-error',
                    city: 'editCity-error',
                    state: 'editState-error',
                    postalCode: 'editPostalCode-error',
                    membershipStatus: 'editMembershipStatus-error'
                };
            } else {
                ids = {
                    firstName: 'firstName',
                    lastName: 'lastName',
                    email: 'email',
                    phone: 'phone',
                    street: 'street',
                    city: 'city',
                    state: 'state',
                    postalCode: 'postalCode',
                    membershipStatus: 'membershipStatus'
                };
                errorIds = {
                    firstName: 'firstName-error',
                    lastName: 'lastName-error',
                    email: 'email-error',
                    phone: 'phone-error',
                    street: 'street-error',
                    city: 'city-error',
                    state: 'state-error',
                    postalCode: 'postalCode-error',
                    membershipStatus: 'membershipStatus-error'
                };
            }

            if (singleFieldId) {
                const fieldKey = Object.keys(ids).find(key => ids[key] === singleFieldId);

                if (fieldKey) {
                    const errorElement = document.getElementById(errorIds[fieldKey]);
                    if (errorElement) {
                        errorElement.textContent = '';
                        errorElement.style.display = 'none';
                    }

                    const inputElement = document.getElementById(singleFieldId);
                    if (inputElement) {
                        inputElement.classList.remove('is-invalid');
                    }
                }
            } else {
                Object.values(errorIds).forEach(id => {
                    const errorElement = document.getElementById(id);
                    if (errorElement) {
                        errorElement.textContent = '';
                        errorElement.style.display = 'none';
                    }
                });

                Object.values(ids).forEach(id => {
                    const inputElement = document.getElementById(id);
                    if (inputElement) {
                        inputElement.classList.remove('is-invalid');
                    }
                });
            }

            let isValid = true;

            if (singleFieldId) {
                const fieldKey = Object.keys(ids).find(key => ids[key] === singleFieldId);

                if (fieldKey) {
                    const value = document.getElementById(singleFieldId).value.trim();

                    switch (fieldKey) {
                        case 'firstName':
                            if (!value) {
                                displayFieldError(ids.firstName, errorIds.firstName, "First name is required");
                                isValid = false;
                            } else if (value.length < 1 || value.length > 50) {
                                displayFieldError(ids.firstName, errorIds.firstName, "First name must be between 2 and 50 characters");
                                isValid = false;
                            } else if (!/^[A-Za-z]+$/.test(value)) {
                                displayFieldError(ids.firstName, errorIds.firstName, "First name must contain only alphabetic characters");
                                isValid = false;
                            }
                            break;

                        case 'lastName':
                            if (!value) {
                                displayFieldError(ids.lastName, errorIds.lastName, "Last name is required");
                                isValid = false;
                            } else if (value.length < 1 || value.length > 50) {
                                displayFieldError(ids.lastName, errorIds.lastName, "Last name must be between 2 and 50 characters");
                                isValid = false;
                            } else if (!/^[A-Za-z]+$/.test(value)) {
                                displayFieldError(ids.lastName, errorIds.lastName, "Last name must contain only alphabetic characters");
                                isValid = false;
                            }
                            break;

                        case 'email':
                            if (!value) {
                                displayFieldError(ids.email, errorIds.email, "Email is required");
                                isValid = false;
                            } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                                displayFieldError(ids.email, errorIds.email, "Please enter a valid email address");
                                isValid = false;
                            } else if (value.length > 100) {
                                displayFieldError(ids.email, errorIds.email, "Email must be less than 100 characters");
                                isValid = false;
                            }
                            break;

                        case 'phone':
                            if (!value) {
                                displayFieldError(ids.phone, errorIds.phone, "Phone number is required");
                                isValid = false;
                            } else if (!/^[6-9]\d{9}$/.test(value)) {
                                displayFieldError(ids.phone, errorIds.phone, "Phone number must be a valid 10-digit Indian mobile number starting with 6-9");
                                isValid = false;
                            }
                            break;

                        case 'street':
                            if (value.length > 200) {
                                displayFieldError(ids.street, errorIds.street, "Street address must be less than 200 characters");
                                isValid = false;
                            }
                            break;

                        case 'city':
                            if (value && value.length > 100) {
                                displayFieldError(ids.city, errorIds.city, "City must be less than 100 characters");
                                isValid = false;
                            } else if (value && !/^[A-Za-z\s]*$/.test(value)) {
                                displayFieldError(ids.city, errorIds.city, "City must contain only alphabetic characters and spaces");
                                isValid = false;
                            }
                            break;

                        case 'state':
                            if (value && value.length > 100) {
                                displayFieldError(ids.state, errorIds.state, "State must be less than 100 characters");
                                isValid = false;
                            } else if (value && !/^[A-Za-z\s]*$/.test(value)) {
                                displayFieldError(ids.state, errorIds.state, "State must contain only alphabetic characters and spaces");
                                isValid = false;
                            }
                            break;

                        case 'postalCode':
                            if (value && !/^\d{6}$/.test(value)) {
                                displayFieldError(ids.postalCode, errorIds.postalCode, "Postal code must be a 6-digit number");
                                isValid = false;
                            }
                            break;

                        case 'membershipStatus':
                            if (!value) {
                                displayFieldError(ids.membershipStatus, errorIds.membershipStatus, "Membership status is required");
                                isValid = false;
                            }
                            break;
                    }
                }
            } else {
                const firstName = document.getElementById(ids.firstName).value.trim();
                const lastName = document.getElementById(ids.lastName).value.trim();
                const email = document.getElementById(ids.email).value.trim();
                const phone = document.getElementById(ids.phone).value.trim();
                const street = document.getElementById(ids.street).value.trim();
                const city = document.getElementById(ids.city).value.trim();
                const state = document.getElementById(ids.state).value.trim();
                const postalCode = document.getElementById(ids.postalCode).value.trim();
                const membershipStatus = document.getElementById(ids.membershipStatus).value;

                if (!firstName) {
                    displayFieldError(ids.firstName, errorIds.firstName, "First name is required");
                    isValid = false;
                } else if (firstName.length < 1 || firstName.length > 50) {
                    displayFieldError(ids.firstName, errorIds.firstName, "First name must be between 2 and 50 characters");
                    isValid = false;
                } else if (!/^[A-Za-z]+$/.test(firstName)) {
                    displayFieldError(ids.firstName, errorIds.firstName, "First name must contain only alphabetic characters");
                    isValid = false;
                }

                if (!lastName) {
                    displayFieldError(ids.lastName, errorIds.lastName, "Last name is required");
                    isValid = false;
                } else if (lastName.length < 1 || lastName.length > 50) {
                    displayFieldError(ids.lastName, errorIds.lastName, "Last name must be between 2 and 50 characters");
                    isValid = false;
                } else if (!/^[A-Za-z]+$/.test(lastName)) {
                    displayFieldError(ids.lastName, errorIds.lastName, "Last name must contain only alphabetic characters");
                    isValid = false;
                }

                if (!email) {
                    displayFieldError(ids.email, errorIds.email, "Email is required");
                    isValid = false;
                } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                    displayFieldError(ids.email, errorIds.email, "Please enter a valid email address");
                    isValid = false;
                } else if (email.length > 100) {
                    displayFieldError(ids.email, errorIds.email, "Email must be less than 100 characters");
                    isValid = false;
                }

                if (!phone) {
                    displayFieldError(ids.phone, errorIds.phone, "Phone number is required");
                    isValid = false;
                } else if (!/^[6-9]\d{9}$/.test(phone)) {
                    displayFieldError(ids.phone, errorIds.phone, "Phone number must be a valid 10-digit Indian mobile number starting with 6-9");
                    isValid = false;
                }

                if (street.length > 200) {
                    displayFieldError(ids.street, errorIds.street, "Street address must be less than 200 characters");
                    isValid = false;
                }

                if (city && city.length > 100) {
                    displayFieldError(ids.city, errorIds.city, "City must be less than 100 characters");
                    isValid = false;
                } else if (city && !/^[A-Za-z\s]*$/.test(city)) {
                    displayFieldError(ids.city, errorIds.city, "City must contain only alphabetic characters and spaces");
                    isValid = false;
                }

                if (state && state.length > 100) {
                    displayFieldError(ids.state, errorIds.state, "State must be less than 100 characters");
                    isValid = false;
                } else if (state && !/^[A-Za-z\s]*$/.test(state)) {
                    displayFieldError(ids.state, errorIds.state, "State must contain only alphabetic characters and spaces");
                    isValid = false;
                }

                if (postalCode && !/^\d{6}$/.test(postalCode)) {
                    displayFieldError(ids.postalCode, errorIds.postalCode, "Postal code must be a 6-digit number");
                    isValid = false;
                }

                if (!membershipStatus) {
                    displayFieldError(ids.membershipStatus, errorIds.membershipStatus, "Membership status is required");
                    isValid = false;
                }
            }

            return isValid;
        } catch (error) {
            showToast("Error", "There was an error validating the form. Please check all fields and try again.", "error");
            return false;
        }
    }

    function displayFieldError(fieldId, errorId, message) {
        const field = document.getElementById(fieldId);
        const errorElement = document.getElementById(errorId);

        if (field && errorElement) {
            field.classList.add('is-invalid');
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }
    }

    function showToast(title, message, type = 'info') {
        let toastContainer = document.getElementById('toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '1050';
            document.body.appendChild(toastContainer);
        }

        const toastId = 'toast-' + Date.now();
        const toast = document.createElement('div');
        toast.id = toastId;
        toast.className = `toast align-items-center ${type === 'error' ? 'bg-danger' : type === 'success' ? 'bg-success' : 'bg-info'} text-white border-0`;
        toast.setAttribute('role', 'alert');
        toast.setAttribute('aria-live', 'assertive');
        toast.setAttribute('aria-atomic', 'true');

        toast.innerHTML = `
        <div class="d-flex">
          <div class="toast-body">
            <strong>${title}</strong>: ${message}
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      `;

        toastContainer.appendChild(toast);

        const bsToast = new bootstrap.Toast(toast, {
            autohide: true,
            delay: 5000
        });
        bsToast.show();

        toast.addEventListener('hidden.bs.toast', function() {
            toast.remove();
        });
    }

    function validateField(fieldId, formId) {
        const tempForm = document.createElement('form');
        tempForm.id = formId;

        const field = document.getElementById(fieldId);
        const fieldClone = field.cloneNode(true);
        tempForm.appendChild(fieldClone);

        validateCustomerForm(formId, fieldId);
    }

    function setupFieldValidation(formId) {
        const prefix = formId === 'editCustomerForm' ? 'edit' : '';
        const fieldIds = [
            prefix + (prefix ? 'F' : 'f') + 'irstName',
            prefix + (prefix ? 'L' : 'l') + 'astName',
            prefix + (prefix ? 'E' : 'e') + 'mail',
            prefix + (prefix ? 'P' : 'p') + 'hone',
            prefix + (prefix ? 'S' : 's') + 'treet',
            prefix + (prefix ? 'C' : 'c') + 'ity',
            prefix + (prefix ? 'S' : 's') + 'tate',
            prefix + (prefix ? 'P' : 'p') + 'ostalCode',
            prefix + (prefix ? 'M' : 'm') + 'embershipStatus'
        ];

        fieldIds.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) {
                field.addEventListener('blur', function() {
                    validateField(fieldId, formId);
                });
            }
        });
    }

    const addCustomerBtn = document.getElementById('addCustomerBtn');
    if (addCustomerBtn) {
        addCustomerBtn.addEventListener('click', function() {
            this.classList.add('processing');

            document.getElementById('addCustomerForm').reset();

            const errorElements = document.querySelectorAll('#addCustomerForm .invalid-feedback');
            errorElements.forEach(el => {
                el.textContent = '';
                el.style.display = 'none';
            });

            const inputElements = document.querySelectorAll('#addCustomerForm .form-control, #addCustomerForm .form-select');
            inputElements.forEach(el => {
                el.classList.remove('is-invalid');
            });

            const addCustomerModal = new bootstrap.Modal(document.getElementById('addCustomerModal'));
            addCustomerModal.show();

            setTimeout(() => {
                this.classList.remove('processing');
            }, 300);

            setupFieldValidation('addCustomerForm');
        });
    }

    const saveCustomerBtn = document.getElementById('saveCustomerBtn');
    if (saveCustomerBtn) {
        saveCustomerBtn.addEventListener('click', function() {
            this.classList.add('processing');
            this.disabled = true;

            const form = document.getElementById('addCustomerForm');

            if (!validateCustomerForm('addCustomerForm')) {
                this.classList.remove('processing');
                this.disabled = false;
                return;
            }

            showSpinner();

            const formData = {
                firstName: document.getElementById('firstName').value.trim(),
                lastName: document.getElementById('lastName').value.trim(),
                email: document.getElementById('email').value.trim(),
                phoneNumber: document.getElementById('phone').value.trim(),
                street: document.getElementById('street').value.trim(),
                city: document.getElementById('city').value.trim(),
                state: document.getElementById('state').value.trim(),
                postalCode: document.getElementById('postalCode').value.trim(),
                membershipStatus: document.getElementById('membershipStatus').value,
                isActive: true
            };

            fetch('/admin/api/customers', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + getToken()
                },
                body: JSON.stringify(formData)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.json();
                })
                .then(data => {
                    hideSpinner();

                    const modal = bootstrap.Modal.getInstance(document.getElementById('addCustomerModal'));
                    modal.hide();

                    showConfirmation('Customer Added', 'The customer has been successfully added to the system.');

                    form.reset();

                    setTimeout(() => {
                        window.location.reload();
                    }, 800);
                })
                .catch(error => {
                    hideSpinner();
                    this.classList.remove('processing');
                    this.disabled = false;
                    showToast('Error', 'Failed to add customer. Please try again.', 'error');
                });
        });
    }

    const editCustomerFromDetailsBtn = document.getElementById('editCustomerFromDetailsBtn');
    if (editCustomerFromDetailsBtn) {
        editCustomerFromDetailsBtn.addEventListener('click', function() {
            const customerId = this.getAttribute('data-customer-id');

            showSpinner();

            fetch(`/admin/customers/api/${customerId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + getToken()
                }
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Server responded with status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(customer => {
                    hideSpinner();

                    const detailsModal = bootstrap.Modal.getInstance(document.getElementById('customerDetailsModal'));
                    detailsModal.hide();

                    const errorElements = document.querySelectorAll('#editCustomerForm .invalid-feedback');
                    errorElements.forEach(el => {
                        el.textContent = '';
                        el.style.display = 'none';
                    });

                    const inputElements = document.querySelectorAll('#editCustomerForm .form-control, #editCustomerForm .form-select');
                    inputElements.forEach(el => {
                        el.classList.remove('is-invalid');
                    });

                    document.getElementById('editCustomerId').value = customer.customerId;
                    document.getElementById('editUserId').value = customer.userId;
                    document.getElementById('editFirstName').value = customer.firstName;
                    document.getElementById('editLastName').value = customer.lastName;
                    document.getElementById('editEmail').value = customer.email;
                    document.getElementById('editPhone').value = customer.phoneNumber;
                    document.getElementById('editStreet').value = customer.street;
                    document.getElementById('editCity').value = customer.city;
                    document.getElementById('editState').value = customer.state;
                    document.getElementById('editPostalCode').value = customer.postalCode;
                    document.getElementById('editMembershipStatus').value = customer.membershipStatus;

                    const editModal = new bootstrap.Modal(document.getElementById('editCustomerModal'));
                    editModal.show();

                    setupFieldValidation('editCustomerForm');
                })
                .catch(error => {
                    hideSpinner();
                    showToast('Error', 'Failed to load customer details for editing. Please try again.', 'error');
                });
        });
    }

    const updateCustomerBtn = document.getElementById('updateCustomerBtn');
    if (updateCustomerBtn) {
        updateCustomerBtn.addEventListener('click', function() {
            if (!validateCustomerForm('editCustomerForm')) {
                return;
            }

            const customerId = document.getElementById('editCustomerId').value;

            showSpinner();

            const formData = {
                customerId: customerId,
                userId: document.getElementById('editUserId').value,
                firstName: document.getElementById('editFirstName').value.trim(),
                lastName: document.getElementById('editLastName').value.trim(),
                email: document.getElementById('editEmail').value.trim(),
                phoneNumber: document.getElementById('editPhone').value.trim(),
                street: document.getElementById('editStreet').value.trim(),
                city: document.getElementById('editCity').value.trim(),
                state: document.getElementById('editState').value.trim(),
                postalCode: document.getElementById('editPostalCode').value.trim(),
                membershipStatus: document.getElementById('editMembershipStatus').value,
                isActive: true
            };

            fetch(`/admin/customers/api/${customerId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + getToken()
                },
                body: JSON.stringify(formData)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Server responded with status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    hideSpinner();

                    const modal = bootstrap.Modal.getInstance(document.getElementById('editCustomerModal'));
                    modal.hide();

                    showConfirmation('Customer Updated', 'The customer information has been successfully updated.');

                    setTimeout(() => {
                        window.location.reload();
                    }, 1500);
                })
                .catch(error => {
                    hideSpinner();
                    showToast('Error', 'Failed to update customer. Please try again.', 'error');
                });
        });
    }

    document.querySelector('.logout-btn').addEventListener('click', function(e) {
        e.preventDefault();

        localStorage.removeItem("jwt-token");
        sessionStorage.removeItem("jwt-token");

        window.location.href = '/admin/logout';
    });

    function loadCustomers() {
        showSpinner();

        fetch('/admin/customers/api', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + getToken()
            }
        })
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401) {
                        window.location.href = '/admin/login?error=session_expired';
                        throw new Error('Session expired');
                    }
                    throw new Error(`Server responded with status: ${response.status}`);
                }
                return response.json();
            })
            .then(customers => {
                hideSpinner();

                if (customers && customers.length > 0) {
                    populateCustomersTable(customers);
                }
            })
            .catch(error => {
                hideSpinner();

                if (error.message !== 'Session expired') {
                    showToast('Error', 'Failed to load customers. Please refresh and try again.', 'error');
                }
            });
    }

    function populateCustomersTable(customers) {
        const tableBody = document.querySelector('.customers-table tbody');

        const existingRows = tableBody.querySelectorAll('tr:not(.empty-row)');
        existingRows.forEach(row => row.remove());

        const emptyRow = tableBody.querySelector('.empty-row');
        if (emptyRow) {
            emptyRow.style.display = customers.length > 0 ? 'none' : '';
        }

        customers.forEach(customer => {
            const row = document.createElement('tr');
            row.className = 'customer-row';
            row.setAttribute('data-customer-id', customer.customerId);

            const initials = getInitials(customer.firstName, customer.lastName);
            const membershipClass = customer.membershipStatus === 'Premium' ? 'premium' : 'standard';
            const membershipIcon = customer.membershipStatus === 'Premium' ? 'fas fa-crown' : 'fas fa-user';

            row.innerHTML = `
      <td>
        <div class="customer-cell">
          <div class="customer-avatar">${initials}</div>
          <div class="customer-info">
            <div class="customer-name">${customer.firstName} ${customer.lastName}</div>
          </div>
        </div>
      </td>
      <td>${customer.email}</td>
      <td>
        <span class="phone-number">
          <i class="fas fa-phone-alt"></i>
          <span>${customer.phoneNumber || 'Not provided'}</span>
        </span>
      </td>
      <td>
        <span class="membership-badge ${membershipClass}">
          <i class="${membershipIcon}"></i>
          <span>${customer.membershipStatus || 'Standard'}</span>
        </span>
      </td>
      <td>${customer.totalServices || 0}</td>
      <td>
        <span class="last-service">
          <i class="fas fa-calendar-day"></i>
          <span>${customer.lastServiceDate
                ? new Date(customer.lastServiceDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
                : 'No service yet'
            }</span>
        </span>
      </td>
    `;

            tableBody.appendChild(row);

            row.addEventListener('click', function() {
                const customerId = this.getAttribute('data-customer-id');

                showSpinner();

                fetch(`/admin/customers/api/${customerId}`, {
                    headers: {
                        'Authorization': 'Bearer ' + getToken(),
                        'Content-Type': 'application/json'
                    }
                })
                    .then(response => response.json())
                    .then(customer => {
                        populateCustomerDetails(customer);
                        hideSpinner();
                        const modal = new bootstrap.Modal(document.getElementById('customerDetailsModal'));
                        modal.show();
                    })
                    .catch(error => {
                        hideSpinner();
                        showToast('Error', 'Failed to load customer details. Please try again.', 'error');
                    });
            });
        });
    }

    if (window.location.pathname.includes('/admin/customers')) {
        const existingCustomers = document.querySelectorAll('.customer-row');
        if (existingCustomers.length === 0) {
            loadCustomers();
        }
    }
});