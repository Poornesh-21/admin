
    document.addEventListener('DOMContentLoaded', function() {
    // Toggle mobile menu
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const sidebar = document.getElementById('sidebar');

    if (mobileMenuToggle) {
    mobileMenuToggle.addEventListener('click', function() {
    sidebar.classList.toggle('active');
});
}

    // Show spinner function
    function showSpinner() {
    const spinner = document.getElementById('spinnerOverlay');
    spinner.classList.add('show');
}

    // Hide spinner function
    function hideSpinner() {
    const spinner = document.getElementById('spinnerOverlay');
    setTimeout(() => {
    spinner.classList.remove('show');
}, 300); // Reduced minimum show time for spinner for better responsiveness
}

    // Success confirmation modal function
    function showConfirmation(title, message) {
    document.getElementById('confirmationTitle').textContent = title;
    document.getElementById('confirmationMessage').textContent = message;
    const successModal = new bootstrap.Modal(document.getElementById('successModal'));
    successModal.show();
}

    // Get token from storage
    function getToken() {
    return localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");
}

    // Check if token exists, redirect to login if not
    const token = getToken();
    if (!token) {
    console.warn("No authentication token found");
    window.location.href = '/admin/login?error=session_expired';
}

    // Fix sidebar navigation links
    document.querySelectorAll('.sidebar-menu-link').forEach(link => {
    const href = link.getAttribute('href');

    // Ensure links have correct admin prefix if needed
    if (href && !href.startsWith('/admin/') && !href.startsWith('#')) {
    link.setAttribute('href', '/admin' + href);
}

    // Remove any token parameters if present
    if (href && href.includes('token=')) {
    const url = new URL(href, window.location.origin);
    url.searchParams.delete('token');
    link.setAttribute('href', url.pathname + url.search);
}

    // Set active state for current page
    if (window.location.pathname.includes(href)) {
    link.classList.add('active');
}
});

    // Filter pills functionality
    const filterPills = document.querySelectorAll('.filter-pill');
    filterPills.forEach(pill => {
    pill.addEventListener('click', function() {
    // Remove active class from all pills
    filterPills.forEach(p => p.classList.remove('active'));

    // Add active class to clicked pill
    this.classList.add('active');

    // Filter customers based on selected filter
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

    // Search functionality
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

    // Profile tabs functionality
    const profileTabs = document.querySelectorAll('.profile-tab');
    profileTabs.forEach(tab => {
    tab.addEventListener('click', function() {
    // Get the tab ID
    const tabId = this.getAttribute('data-tab');

    // Remove active class from all tabs and tab contents
    profileTabs.forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

    // Add active class to clicked tab and corresponding content
    this.classList.add('active');
    document.getElementById(`${tabId}-tab`).classList.add('active');
});
});

    // Make table rows clickable to open customer details
    const customerRows = document.querySelectorAll('.customer-row');
    customerRows.forEach(row => {
    row.addEventListener('click', function() {
    const customerId = this.getAttribute('data-customer-id');

    // Show spinner before loading customer details
    showSpinner();

    // FIXED URL: Changed from /admin/api/customers/ to /admin/customers/api/
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
    // Populate customer details modal
    populateCustomerDetails(customer);

    // Hide spinner
    hideSpinner();

    // Open customer details modal
    const customerDetailsModal = new bootstrap.Modal(document.getElementById('customerDetailsModal'));
    customerDetailsModal.show();
})
    .catch(error => {
    console.error('Error fetching customer details:', error);
    hideSpinner();
    showToast('Error', 'Failed to load customer details. Please try again.', 'error');
});
});
});

    function populateCustomerDetails(customer) {
    // Set customer profile details
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

    // Store customer ID for edit functionality
    document.getElementById('editCustomerFromDetailsBtn').setAttribute('data-customer-id', customer.customerId);
}

    function getInitials(firstName, lastName) {
    const firstInitial = firstName && firstName.length > 0 ? firstName.charAt(0).toUpperCase() : '';
    const lastInitial = lastName && lastName.length > 0 ? lastName.charAt(0).toUpperCase() : '';
    return firstInitial + lastInitial;
}

    /**
     * Validates the customer form before submission
     * @param {string} formId - The ID of the form to validate ('addCustomerForm' or 'editCustomerForm')
     * @param {string} [singleFieldId] - Optional ID of a single field to validate
     * @returns {boolean} - True if valid, false otherwise
     */
    function validateCustomerForm(formId, singleFieldId = null) {
    try {
    // Get form IDs based on whether it's the add or edit form
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

    // If validating a single field, only reset that field's error
    if (singleFieldId) {
    // Find the field key (firstName, lastName, etc.) that matches the singleFieldId
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
    // Reset all error messages and remove invalid class
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

    // If validating a single field, only validate that field
    if (singleFieldId) {
    // Find the field key (firstName, lastName, etc.) that matches the singleFieldId
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
    // Get values from form using the correct IDs
    const firstName = document.getElementById(ids.firstName).value.trim();
    const lastName = document.getElementById(ids.lastName).value.trim();
    const email = document.getElementById(ids.email).value.trim();
    const phone = document.getElementById(ids.phone).value.trim();
    const street = document.getElementById(ids.street).value.trim();
    const city = document.getElementById(ids.city).value.trim();
    const state = document.getElementById(ids.state).value.trim();
    const postalCode = document.getElementById(ids.postalCode).value.trim();
    const membershipStatus = document.getElementById(ids.membershipStatus).value;

    // First Name validation - required, 2-50 chars, alphabetic only
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

    // Last Name validation - required, 2-50 chars, alphabetic only
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

    // Email validation - required, valid format, max 100 chars
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

    // Phone validation - required, 10-digit Indian mobile format
    if (!phone) {
    displayFieldError(ids.phone, errorIds.phone, "Phone number is required");
    isValid = false;
} else if (!/^[6-9]\d{9}$/.test(phone)) {
    displayFieldError(ids.phone, errorIds.phone, "Phone number must be a valid 10-digit Indian mobile number starting with 6-9");
    isValid = false;
}

    // Street validation - max 200 chars
    if (street.length > 200) {
    displayFieldError(ids.street, errorIds.street, "Street address must be less than 200 characters");
    isValid = false;
}

    // City validation - max 100 chars, alphabetic + spaces only
    if (city && city.length > 100) {
    displayFieldError(ids.city, errorIds.city, "City must be less than 100 characters");
    isValid = false;
} else if (city && !/^[A-Za-z\s]*$/.test(city)) {
    displayFieldError(ids.city, errorIds.city, "City must contain only alphabetic characters and spaces");
    isValid = false;
}

    // State validation - max 100 chars, alphabetic + spaces only
    if (state && state.length > 100) {
    displayFieldError(ids.state, errorIds.state, "State must be less than 100 characters");
    isValid = false;
} else if (state && !/^[A-Za-z\s]*$/.test(state)) {
    displayFieldError(ids.state, errorIds.state, "State must contain only alphabetic characters and spaces");
    isValid = false;
}

    // Postal code validation - 6-digit number
    if (postalCode && !/^\d{6}$/.test(postalCode)) {
    displayFieldError(ids.postalCode, errorIds.postalCode, "Postal code must be a 6-digit number");
    isValid = false;
}

    // Membership status validation - required
    if (!membershipStatus) {
    displayFieldError(ids.membershipStatus, errorIds.membershipStatus, "Membership status is required");
    isValid = false;
}
}

    return isValid;
} catch (error) {
    console.error("Form validation error:", error);
    showToast("Error", "There was an error validating the form. Please check all fields and try again.", "error");
    return false;
}
}

    // Helper function to display field-specific errors
    function displayFieldError(fieldId, errorId, message) {
    const field = document.getElementById(fieldId);
    const errorElement = document.getElementById(errorId);

    if (field && errorElement) {
    field.classList.add('is-invalid');
    errorElement.textContent = message;
    errorElement.style.display = 'block';
}
}

    // Function to show toast notifications
    function showToast(title, message, type = 'info') {
    // Create toast container if it doesn't exist
    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'toast-container';
    toastContainer.className = 'position-fixed top-0 end-0 p-3';
    toastContainer.style.zIndex = '1050';
    document.body.appendChild(toastContainer);
}

    // Create toast element
    const toastId = 'toast-' + Date.now();
    const toast = document.createElement('div');
    toast.id = toastId;
    toast.className = `toast align-items-center ${type === 'error' ? 'bg-danger' : type === 'success' ? 'bg-success' : 'bg-info'} text-white border-0`;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');
    toast.setAttribute('aria-atomic', 'true');

    // Create toast content
    toast.innerHTML = `
        <div class="d-flex">
          <div class="toast-body">
            <strong>${title}</strong>: ${message}
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      `;

    // Add toast to container
    toastContainer.appendChild(toast);

    // Initialize and show toast
    const bsToast = new bootstrap.Toast(toast, {
    autohide: true,
    delay: 5000
});
    bsToast.show();

    // Remove toast after it's hidden
    toast.addEventListener('hidden.bs.toast', function() {
    toast.remove();
});
}

    // Function to validate a single field
    function validateField(fieldId, formId) {
    // Create a temporary form object with just this field
    const tempForm = document.createElement('form');
    tempForm.id = formId;

    // Clone the field and append to temp form
    const field = document.getElementById(fieldId);
    const fieldClone = field.cloneNode(true);
    tempForm.appendChild(fieldClone);

    // Validate just this field
    validateCustomerForm(formId, fieldId);
}

    // Add real-time validation to form fields
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
    // Add blur event listener to validate when user leaves the field
    field.addEventListener('blur', function() {
    validateField(fieldId, formId);
});
}
});
}

    // Add Customer button handler
    const addCustomerBtn = document.getElementById('addCustomerBtn');
    if (addCustomerBtn) {
    addCustomerBtn.addEventListener('click', function() {
    // Show immediate visual feedback
    this.classList.add('processing');

    // Reset form
    document.getElementById('addCustomerForm').reset();

    // Reset any validation errors
    const errorElements = document.querySelectorAll('#addCustomerForm .invalid-feedback');
    errorElements.forEach(el => {
    el.textContent = '';
    el.style.display = 'none';
});

    const inputElements = document.querySelectorAll('#addCustomerForm .form-control, #addCustomerForm .form-select');
    inputElements.forEach(el => {
    el.classList.remove('is-invalid');
});

    // Show add customer modal
    const addCustomerModal = new bootstrap.Modal(document.getElementById('addCustomerModal'));
    addCustomerModal.show();

    // Remove processing class after modal is shown
    setTimeout(() => {
    this.classList.remove('processing');
}, 300);

    // Setup field validation
    setupFieldValidation('addCustomerForm');
});
}

    // Save Customer button handler
    const saveCustomerBtn = document.getElementById('saveCustomerBtn');
    if (saveCustomerBtn) {
    saveCustomerBtn.addEventListener('click', function() {
    // Show immediate visual feedback
    this.classList.add('processing');
    this.disabled = true;

    const form = document.getElementById('addCustomerForm');

    // Validate form before submission
    if (!validateCustomerForm('addCustomerForm')) {
    // Re-enable button if validation fails
    this.classList.remove('processing');
    this.disabled = false;
    return; // Stop if validation fails
}

    // Show spinner during processing
    showSpinner();

    // Prepare form data - do this before showing spinner to improve perceived performance
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

    // FIXED URL: Now using /admin/api/customers for POST (this is the CustomerController's URL)
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
    // Hide spinner
    hideSpinner();

    // Close modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('addCustomerModal'));
    modal.hide();

    // Show success confirmation
    showConfirmation('Customer Added', 'The customer has been successfully added to the system.');

    // Reset form
    form.reset();

    // Refresh the page to show the new customer - reduced delay for better responsiveness
    setTimeout(() => {
    window.location.reload();
}, 800);
})
    .catch(error => {
    console.error('Error adding customer:', error);
    hideSpinner();
    // Re-enable button on error
    this.classList.remove('processing');
    this.disabled = false;
    showToast('Error', 'Failed to add customer. Please try again.', 'error');
});
});
}

    // Edit Customer button handler from customer details
    const editCustomerFromDetailsBtn = document.getElementById('editCustomerFromDetailsBtn');
    if (editCustomerFromDetailsBtn) {
    editCustomerFromDetailsBtn.addEventListener('click', function() {
    const customerId = this.getAttribute('data-customer-id');

    // Show spinner
    showSpinner();

    // FIXED URL: Changed from /admin/api/customers/ to /admin/customers/api/
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
    // Hide spinner
    hideSpinner();

    // Close customer details modal
    const detailsModal = bootstrap.Modal.getInstance(document.getElementById('customerDetailsModal'));
    detailsModal.hide();

    // Reset any validation errors
    const errorElements = document.querySelectorAll('#editCustomerForm .invalid-feedback');
    errorElements.forEach(el => {
    el.textContent = '';
    el.style.display = 'none';
});

    const inputElements = document.querySelectorAll('#editCustomerForm .form-control, #editCustomerForm .form-select');
    inputElements.forEach(el => {
    el.classList.remove('is-invalid');
});

    // Populate edit form with customer data
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

    // Show edit customer modal
    const editModal = new bootstrap.Modal(document.getElementById('editCustomerModal'));
    editModal.show();

    // Setup field validation for edit form
    setupFieldValidation('editCustomerForm');
})
    .catch(error => {
    console.error('Error fetching customer details for editing:', error);
    hideSpinner();
    showToast('Error', 'Failed to load customer details for editing. Please try again.', 'error');
});
});
}

    // Update Customer button handler
    const updateCustomerBtn = document.getElementById('updateCustomerBtn');
    if (updateCustomerBtn) {
    updateCustomerBtn.addEventListener('click', function() {
    // Validate form before submission
    if (!validateCustomerForm('editCustomerForm')) {
    return; // Stop if validation fails
}

    // Get customer ID
    const customerId = document.getElementById('editCustomerId').value;

    // Show spinner
    showSpinner();

    // Prepare form data
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

    // FIXED URL: Changed from /admin/api/customers/ to /admin/customers/api/
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
    // Hide spinner
    hideSpinner();

    // Close modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('editCustomerModal'));
    modal.hide();

    // Show success confirmation
    showConfirmation('Customer Updated', 'The customer information has been successfully updated.');

    // Refresh the page to show the updated customer
    setTimeout(() => {
    window.location.reload();
}, 1500);
})
    .catch(error => {
    console.error('Error updating customer:', error);
    hideSpinner();
    showToast('Error', 'Failed to update customer. Please try again.', 'error');
});
});
}

    // Logout handler
    document.querySelector('.logout-btn').addEventListener('click', function(e) {
    e.preventDefault();

    // Clear storage
    localStorage.removeItem("jwt-token");
    sessionStorage.removeItem("jwt-token");

    // Redirect to logout
    window.location.href = '/admin/logout';
});

    // Load customers on page load
    function loadCustomers() {
    showSpinner();

    // FIXED URL: Using the correct URL for customer list
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
    // Populate the table with customers
    populateCustomersTable(customers);
}
})
    .catch(error => {
    console.error('Error loading customers:', error);
    hideSpinner();

    if (error.message !== 'Session expired') {
    showToast('Error', 'Failed to load customers. Please refresh and try again.', 'error');
}
});
}

    function populateCustomersTable(customers) {
    const tableBody = document.querySelector('.customers-table tbody');

    // Clear any existing rows except the empty message row
    const existingRows = tableBody.querySelectorAll('tr:not(.empty-row)');
    existingRows.forEach(row => row.remove());

    // Hide empty message if we have customers
    const emptyRow = tableBody.querySelector('.empty-row');
    if (emptyRow) {
    emptyRow.style.display = customers.length > 0 ? 'none' : '';
}

    // Add customer rows
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

    // Add click event to the new row
    row.addEventListener('click', function() {
    const customerId = this.getAttribute('data-customer-id');

    // Show spinner
    showSpinner();

    // FIXED URL: Changed from /admin/api/customers/ to /admin/customers/api/
    fetch(`/admin/customers/api/${customerId}`, {
    headers: {
    'Authorization': 'Bearer ' + getToken(),
    'Content-Type': 'application/json'
}
})
    .then(response => response.json())
    .then(customer => {
    // Populate modal
    populateCustomerDetails(customer);

    // Hide spinner
    hideSpinner();

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('customerDetailsModal'));
    modal.show();
})
    .catch(error => {
    console.error('Error fetching customer details:', error);
    hideSpinner();
    showToast('Error', 'Failed to load customer details. Please try again.', 'error');
});
});
});
}

    // Try to load customers if we're on the customers page
    if (window.location.pathname.includes('/admin/customers')) {
    // Check if we have existing customers in the table
    const existingCustomers = document.querySelectorAll('.customer-row');
    if (existingCustomers.length === 0) {
    // Only load if we don't have customers already rendered from server-side
    loadCustomers();
}
}
});