 // Global variables
    let serviceRequests = [];
    let customers = [];
    let serviceAdvisors = [];
    let vehicles = [];
    let currentPage = 1;
    const itemsPerPage = 5;

    // Declare these functions in the global scope so they can be called from HTML
    let loadServiceAdvisors;
    let showAssignAdvisorModal;
    let populateServiceAdvisorsList;
    let showServiceRequestDetails;

    document.addEventListener('DOMContentLoaded', function() {
    // Initialize app
    initializeApp();

    // Set up event listeners
    setupEventListeners();

    // Load service requests
    loadServiceRequests();

    // Add event listener for Add Service Request modal
    const addServiceRequestModal = document.getElementById('addServiceRequestModal');
    if (addServiceRequestModal) {
    addServiceRequestModal.addEventListener('show.bs.modal', function() {
    // Load customers when the modal is shown
    loadCustomersDirectly();
});
}

    // Initialize global functions
    initializeGlobalFunctions();
});

    /**
    * Initialize the application
    */
    function initializeApp() {
    // Setup mobile menu toggle
    setupMobileMenu();

    // Setup logout button
    setupLogout();

    // Set up token-based authentication
    setupAuthentication();

    // Add CSS for membership filters
    addMembershipFilterStyles();

    // Set current date if element exists
    setupDateDisplay();
}

    /**
    * Initialize global functions
    */
    function initializeGlobalFunctions() {
    /**
     * Load service advisors
     */
    loadServiceAdvisors = function() {
        console.log("Loading service advisors...");

        // Get token
        const token = getToken();
        if (!token) {
            console.error("No auth token found");
            return Promise.resolve([]);
        }

        return fetch('/admin/service-advisors/api', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            }
        })
            .then(response => {
                if (response.status === 401) {
                    window.location.href = '/admin/login?error=session_expired';
                    throw new Error('Session expired');
                }
                if (!response.ok) {
                    // Try alternative endpoint
                    return fetch('/admin/service-advisors/api/advisors', {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': 'Bearer ' + token
                        }
                    });
                }
                return response;
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log("Service advisors loaded:", data);
                serviceAdvisors = data || [];
                return serviceAdvisors;
            })
            .catch(error => {
                console.error('Error loading service advisors:', error);
                showToast("Failed to load service advisors: " + error.message, "error");
                return [];
            });
    };

    /**
     * Show service request details
     */
    showServiceRequestDetails = function(requestId) {
    console.log("Showing details for request ID:", requestId);

    // Find the request in our cached data
    const request = serviceRequests.find(req => req.requestId === requestId);

    if (!request) {
    console.error('Request not found:', requestId);

    // Try to fetch the request from the API
    fetchServiceRequest(requestId);
    return;
}

    displayServiceRequestDetails(request);
};

    /**
     * Show the assign advisor modal
     */
    showAssignAdvisorModal = function(requestId) {
    console.log("Showing assign advisor modal for request ID:", requestId);

    const request = serviceRequests.find(req => req.requestId === requestId);

    if (!request) {
    console.error('Request not found:', requestId);
    showToast("Service request not found", "error");
    return;
}

    // Set request details in the modal
    document.getElementById('assignVehicleName').textContent = request.vehicleName || `${request.vehicleBrand} ${request.vehicleModel}`;
    document.getElementById('assignVehicleReg').textContent = request.registrationNumber || 'N/A';
    document.getElementById('assignCustomerName').textContent = request.customerName || 'Customer';
    document.getElementById('assignRequestId').textContent = `REQ-${request.requestId}`;
    document.getElementById('selectedRequestId').value = request.requestId;

    // Show loading state in advisors list
    const advisorsList = document.getElementById('advisorsList');
    if (advisorsList) {
    advisorsList.innerHTML = `
            <div class="text-center py-4">
                <div class="spinner-border text-wine" role="status"></div>
                <p class="mt-2">Loading service advisors...</p>
            </div>
        `;
}

    // Load service advisors
    loadServiceAdvisors()
    .then(advisors => {
    populateServiceAdvisorsList(advisors);

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('assignAdvisorModal'));
    modal.show();
})
    .catch(error => {
    console.error("Error preparing advisor modal:", error);
    showToast("Failed to load service advisors", "error");
});
};

    /**
     * Populate the service advisors list
     */
    populateServiceAdvisorsList = function(advisors) {
    const advisorsList = document.getElementById('advisorsList');
    if (!advisorsList) return;

    if (!advisors || advisors.length === 0) {
    advisorsList.innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-info-circle me-2"></i>
                No service advisors available. Please add service advisors first.
            </div>
        `;
    return;
}

    let html = '';

    advisors.forEach(advisor => {
    // Calculate workload percentage (example)
    const workloadPercentage = advisor.workloadPercentage ||
    (advisor.activeServices ? Math.min(Math.round((advisor.activeServices / 10) * 100), 100) : 0);

    // Determine workload class
    let workloadClass = 'bg-success';
    if (workloadPercentage > 75) {
    workloadClass = 'bg-danger';
} else if (workloadPercentage > 50) {
    workloadClass = 'bg-warning';
}

    const name = advisor.firstName && advisor.lastName ?
    `${advisor.firstName} ${advisor.lastName}` : advisor.name || 'Unnamed Advisor';

    const initials = (advisor.firstName ? advisor.firstName.charAt(0) : '') +
    (advisor.lastName ? advisor.lastName.charAt(0) : '');

    html += `
        <div class="advisor-card" data-advisor-id="${advisor.advisorId || advisor.id}">
            <div class="advisor-avatar">
                ${initials || 'SA'}
            </div>
            <div class="advisor-info">
                <div class="advisor-name">${name}</div>
                <div class="advisor-meta">
                    <div class="advisor-stat">
                        <i class="fas fa-clipboard-list"></i>
                        ${advisor.activeServices || 0} active requests
                    </div>
                    <div class="advisor-stat">
                        <i class="fas fa-id-badge"></i>
                        ${advisor.formattedId || `SA-${String(advisor.advisorId || 0).padStart(3, '0')}`}
                    </div>
                </div>
                <div class="advisor-services">
                    <i class="fas fa-tools"></i>
                    ${advisor.specialization || 'General Service'}
                </div>
                <div class="advisor-workload">
                    <div class="progress">
                        <div class="progress-bar ${workloadClass}" role="progressbar"
                            style="width: ${workloadPercentage}%"
                            aria-valuenow="${workloadPercentage}" aria-valuemin="0" aria-valuemax="100">
                        </div>
                    </div>
                    <div class="workload-text">
                        Workload: ${workloadPercentage}%
                    </div>
                </div>
            </div>
        </div>
    `;
});

    advisorsList.innerHTML = html;

    // Add click event to select advisor
    document.querySelectorAll('.advisor-card').forEach(card => {
    card.addEventListener('click', function() {
    document.querySelectorAll('.advisor-card').forEach(c => c.classList.remove('selected'));
    this.classList.add('selected');
});
});
};
}

    /**
    * Set up mobile menu toggle
    */
    function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    if (mobileMenuToggle) {
    mobileMenuToggle.addEventListener('click', function() {
    document.getElementById('sidebar').classList.toggle('active');
});
}
}

    /**
    * Set up logout button
    */
    function setupLogout() {
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
    logoutBtn.addEventListener('click', function() {
    // Show confirmation dialog
    if (confirm('Are you sure you want to logout?')) {
    // Clear storage
    localStorage.removeItem("jwt-token");
    sessionStorage.removeItem("jwt-token");
    localStorage.removeItem("user-role");
    localStorage.removeItem("user-name");
    sessionStorage.removeItem("user-role");
    sessionStorage.removeItem("user-name");

    // Redirect to logout
    window.location.href = '/admin/logout';
}
});
}
}

    /**
    * Set up date display
    */
    function setupDateDisplay() {
    const dateElement = document.getElementById('current-date');
    if (dateElement) {
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const today = new Date();
    dateElement.textContent = today.toLocaleDateString('en-US', options);
}
}

    /**
    * Set up authentication token for all requests
    */
    function setupAuthentication() {
    // Get token from storage
    const tokenFromStorage = localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");

    if (tokenFromStorage) {
    console.log("Token found in storage");

    // Check if current URL already has a token parameter
    const currentUrl = new URL(window.location.href);
    const urlToken = currentUrl.searchParams.get('token');

    // If URL doesn't have the token, update all navigation links
    if (!urlToken) {
    // Add token to all sidebar navigation links
    document.querySelectorAll('.sidebar-menu-link').forEach(link => {
    const href = link.getAttribute('href');
    if (href && !href.includes('token=')) {
    const separator = href.includes('?') ? '&' : '?';
    link.setAttribute('href', href + separator + 'token=' + encodeURIComponent(tokenFromStorage));
}
});

    // Add token to current URL for refresh protection
    if (window.location.href.indexOf('token=') === -1) {
    const separator = window.location.href.indexOf('?') === -1 ? '?' : '&';
    const newUrl = window.location.href + separator + 'token=' + encodeURIComponent(tokenFromStorage);
    window.history.replaceState({}, document.title, newUrl);
}
}
} else {
    console.warn("No token found in storage");
    // If no token is found, redirect to login
    window.location.href = '/admin/login?error=session_expired';
}
}

    /**
    * Add CSS for membership filters
    */
    function addMembershipFilterStyles() {
    // Check if styles already exist
    if (!document.getElementById('membership-filter-styles')) {
    const style = document.createElement('style');
    style.id = 'membership-filter-styles';
    style.textContent = `
        .membership-filters {
            display: flex;
            align-items: center;
        }

        .membership-filters .btn-group {
            box-shadow: var(--shadow-sm);
            border-radius: var(--radius-lg);
            overflow: hidden;
        }

        .membership-filters .btn {
            padding: 0.4rem 0.75rem;
            font-size: 0.85rem;
            font-weight: 500;
            border-radius: 0;
        }

        .membership-filters .btn.active {
            background-color: var(--wine);
            color: white;
            border-color: var(--wine);
        }
    `;
    document.head.appendChild(style);
}
}

    /**
    * Set up event listeners
    */
    function setupEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
    searchInput.addEventListener('keyup', function() {
    filterServiceRequests(this.value);
});
}

    // Table search
    const tableSearchInput = document.querySelector('.search-input-sm');
    if (tableSearchInput) {
    tableSearchInput.addEventListener('keyup', function() {
    filterTableRows(this.value);
});
}

    // Pagination
    document.querySelectorAll('#pagination .page-link').forEach(link => {
    if (link.id !== 'prevBtn' && link.id !== 'nextBtn') {
    link.addEventListener('click', function(e) {
    e.preventDefault();
    const page = parseInt(this.getAttribute('data-page'));
    changePage(page);
});
}
});

    const prevBtn = document.getElementById('prevBtn');
    if (prevBtn) {
    prevBtn.addEventListener('click', function(e) {
    e.preventDefault();
    if (currentPage > 1) {
    changePage(currentPage - 1);
}
});
}

    const nextBtn = document.getElementById('nextBtn');
    if (nextBtn) {
    nextBtn.addEventListener('click', function(e) {
    e.preventDefault();
    const totalPages = Math.ceil(serviceRequests.length / itemsPerPage);
    if (currentPage < totalPages) {
    changePage(currentPage + 1);
}
});
}

    // Add Service Request Form
    const customerIdSelect = document.getElementById('customerId');
    if (customerIdSelect) {
    customerIdSelect.addEventListener('change', function() {
    const customerId = this.value;
    const addNewVehicleBtn = document.getElementById('addNewVehicleBtn');
    const newVehicleForm = document.getElementById('newVehicleForm');

    if (customerId) {
    loadVehiclesForCustomer(customerId);
    // Enable the "Add Vehicle" button
    if (addNewVehicleBtn) {
    addNewVehicleBtn.disabled = false;
}
} else {
    resetVehicleDropdown();
    // Disable the "Add Vehicle" button
    if (addNewVehicleBtn) {
    addNewVehicleBtn.disabled = true;
}
    // Hide new vehicle form if it's open
    if (newVehicleForm) {
    newVehicleForm.style.display = 'none';
}
}
});
}

    // Add New Vehicle button
    const addNewVehicleBtn = document.getElementById('addNewVehicleBtn');
    if (addNewVehicleBtn) {
    addNewVehicleBtn.addEventListener('click', function() {
    const newVehicleForm = document.getElementById('newVehicleForm');
    if (newVehicleForm) {
    newVehicleForm.style.display = 'block';

    // Disable the vehicle dropdown when adding a new vehicle
    const vehicleDropdown = document.getElementById('vehicleId');
    if (vehicleDropdown) vehicleDropdown.disabled = true;
}
});
}

    // Cancel New Vehicle button
    const cancelNewVehicleBtn = document.getElementById('cancelNewVehicleBtn');
    if (cancelNewVehicleBtn) {
    cancelNewVehicleBtn.addEventListener('click', function() {
    const newVehicleForm = document.getElementById('newVehicleForm');
    if (newVehicleForm) {
    newVehicleForm.style.display = 'none';

    // Enable the vehicle dropdown when not adding a new vehicle
    const vehicleDropdown = document.getElementById('vehicleId');
    if (vehicleDropdown) vehicleDropdown.disabled = false;
}
});
}

    // Save New Vehicle button
    const saveNewVehicleBtn = document.getElementById('saveNewVehicleBtn');
    if (saveNewVehicleBtn) {
    saveNewVehicleBtn.addEventListener('click', function() {
    saveNewVehicle();
});
}

    // Save service request
    const saveServiceRequestBtn = document.getElementById('saveServiceRequestBtn');
    if (saveServiceRequestBtn) {
    saveServiceRequestBtn.addEventListener('click', function() {
    saveServiceRequest();
});
}

    // Assign service advisor
    const confirmAssignBtn = document.getElementById('confirmAssignBtn');
    if (confirmAssignBtn) {
    confirmAssignBtn.addEventListener('click', function() {
    assignServiceAdvisor();
});
}
}

    /**
    * Get the authentication token
    */
    function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

    /**
    * Show the loading spinner
    */
    function showSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
    spinnerOverlay.classList.add('show');
}
}

    /**
    * Hide the loading spinner
    */
    function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
    spinnerOverlay.classList.remove('show');
}
}

    /**
    * Load service requests from API
    */
    /**
    * Load service requests from API and filter for unassigned ones
    */
    function loadServiceRequests() {
    console.log("Loading service requests...");

    // Show loading state
    const tableBody = document.getElementById('serviceRequestsTableBody');
    if (!tableBody) return;

    const loadingRow = document.getElementById('loading-row');
    const emptyRow = document.getElementById('empty-row');

    if (loadingRow) loadingRow.style.display = 'table-row';
    if (emptyRow) emptyRow.style.display = 'none';

    // Get the token
    const token = getToken();
    if (!token) {
    console.error("No auth token found");
    window.location.href = '/admin/login?error=session_expired';
    return;
}

    // Fetch service requests from API
    fetch(`/admin/service-requests/api${token ? `?token=${token}` : ''}`, {
    method: 'GET',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
}
})
    .then(response => {
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}
    if (!response.ok) {
    throw new Error(`HTTP error! Status: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    console.log("All service requests loaded:", data);

    // Hide loading row
    if (loadingRow) loadingRow.style.display = 'none';

    // Process the response
    // Filter to only include requests without a service advisor
    const allRequests = data || [];
    serviceRequests = allRequests.filter(request => !request.serviceAdvisorId);

    console.log("Filtered unassigned requests:", serviceRequests);

    if (serviceRequests.length === 0) {
    if (emptyRow) {
    emptyRow.style.display = 'table-row';
    const message = emptyRow.querySelector('p');
    if (message) {
    message.textContent = 'No unassigned service requests found';
}
}
} else {
    renderServiceRequests();
    setupPagination();
}
})
    .catch(error => {
    console.error("Error loading service requests:", error);
    if (loadingRow) loadingRow.style.display = 'none';

    // Show empty state with error message
    if (emptyRow) {
    emptyRow.style.display = 'table-row';
    const errorMessage = emptyRow.querySelector('p');
    if (errorMessage) {
    errorMessage.textContent = 'Error loading service requests. Please try again.';
}
}
});
}

    /**
    * Load customers for the dropdown with better error handling
    */
    function loadCustomersDirectly() {
    console.log("Loading customers directly for dropdown...");

    const dropdown = document.getElementById('customerId');
    if (!dropdown) {
    console.error("Customer dropdown element not found");
    return;
}

    dropdown.innerHTML = '<option value="">Loading customers...</option>';
    dropdown.disabled = true;

    // Get token
    const token = getToken();
    if (!token) {
    console.error("No auth token found");
    dropdown.innerHTML = '<option value="">Error loading customers - No authentication</option>';
    dropdown.disabled = true;
    return;
}

    // Try using the correct endpoint
    fetch('/admin/api/customers', {
    method: 'GET',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
}
})
    .then(response => {
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}
    if (!response.ok) {
    // If first attempt fails, try alternative endpoint
    console.warn("Primary endpoint failed, trying alternative endpoint");
    return fetch('/admin/customers/api', {
    method: 'GET',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
}
});
}
    return response;
})
    .then(response => {
    if (!response.ok) {
    throw new Error(`HTTP error! Status: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    console.log("Customer data loaded:", data);

    dropdown.innerHTML = '<option value="">Select Customer</option>';

    if (!data || !Array.isArray(data) || data.length === 0) {
    dropdown.innerHTML += '<option value="" disabled>No customers available</option>';
    return;
}

    // Process each customer
    data.forEach(customer => {
    try {
    const customerId = customer.customerId || customer.userId || customer.id;
    const firstName = customer.firstName || '';
    const lastName = customer.lastName || '';
    const membershipStatus = customer.membershipStatus || 'Standard';

    if (!customerId) {
    console.warn("Customer missing ID:", customer);
    return;
}

    const option = document.createElement('option');
    option.value = customerId;
    option.textContent = `${firstName} ${lastName} (${membershipStatus})`;
    dropdown.appendChild(option);
} catch (e) {
    console.error("Error processing customer for dropdown:", e, customer);
}
});

    dropdown.disabled = false;
    console.log(`Added ${dropdown.options.length - 1} customers to dropdown`);
})
    .catch(error => {
    console.error('Error loading customers:', error);
    dropdown.innerHTML = '<option value="">Error loading customers</option>';
    dropdown.disabled = true;
    showToast("Failed to load customers: " + error.message, "error");
});
}

    /**
    * Load vehicles for a specific customer
    */
    function loadVehiclesForCustomer(customerId) {
    const vehicleDropdown = document.getElementById('vehicleId');
    if (!vehicleDropdown) return;

    vehicleDropdown.disabled = true;
    vehicleDropdown.innerHTML = '<option value="">Loading vehicles...</option>';

    // Get token
    const token = getToken();
    if (!token) {
    console.error("No auth token found");
    resetVehicleDropdown('Error loading vehicles - No authentication');
    return;
}

    // Use the correct admin API endpoint
    fetch(`/admin/api/customers/${customerId}/vehicles${token ? `?token=${token}` : ''}`, {
    method: 'GET',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
}
})
    .then(response => {
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}
    if (!response.ok) {
    throw new Error(`HTTP error! Status: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    vehicles = data || [];

    // Reset the dropdown
    vehicleDropdown.innerHTML = '<option value="">Select Vehicle</option>';

    if (vehicles.length === 0) {
    vehicleDropdown.innerHTML += '<option value="" disabled>No vehicles found for this customer</option>';
    vehicleDropdown.disabled = true;
    return;
}

    // Add each vehicle to the dropdown
    vehicles.forEach(vehicle => {
    const option = document.createElement('option');
    option.value = vehicle.vehicleId;
    option.textContent = `${vehicle.brand} ${vehicle.model} (${vehicle.registrationNumber})`;
    vehicleDropdown.appendChild(option);
});

    vehicleDropdown.disabled = false;
})
    .catch(error => {
    console.error('Error loading vehicles:', error);
    resetVehicleDropdown('Error loading vehicles. Please try again.');
});
}

    /**
    * Reset the vehicle dropdown to initial state
    */
    function resetVehicleDropdown(message = 'Select Customer First') {
    const dropdown = document.getElementById('vehicleId');
    if (dropdown) {
    dropdown.innerHTML = `<option value="">${message}</option>`;
    dropdown.disabled = true;
}
}

    /**
    * Save a new vehicle
    */
    function saveNewVehicle(callback) {
    const customerId = document.getElementById('customerId').value;

    if (!customerId) {
    showToast('Please select a customer first', 'error');
    return;
}

    // Get form values
    const brand = document.getElementById('vehicleBrand').value;
    const model = document.getElementById('vehicleModel').value;
    const regNumber = document.getElementById('vehicleRegNumber').value;
    const category = document.getElementById('vehicleCategory').value;
    const year = document.getElementById('vehicleYear').value;

    if (!brand || !model || !regNumber || !category || !year) {
    showToast('Please fill all required vehicle fields', 'error');
    return;
}

    const vehicleData = {
    brand: brand,
    model: model,
    registrationNumber: regNumber,
    category: category,
    year: parseInt(year)
};

    showSpinner();

    // Get the token
    const token = getToken();
    if (!token) {
    hideSpinner();
    showToast('Authentication error - please login again', 'error');
    return;
}

    // Use the correct endpoint URL
    fetch(`/admin/api/customers/${customerId}/vehicles${token ? `?token=${token}` : ''}`, {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
},
    body: JSON.stringify(vehicleData)
})
    .then(response => {
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}
    if (!response.ok) {
    return response.json().then(errorData => {
    throw new Error(errorData.error || 'Failed to create vehicle');
});
}
    return response.json();
})
    .then(data => {
    // Hide the form
    document.getElementById('newVehicleForm').style.display = 'none';

    // Add new vehicle to the vehicles array
    vehicles.push(data);

    // Update dropdown
    const dropdown = document.getElementById('vehicleId');
    if (dropdown) {
    const option = document.createElement('option');
    option.value = data.vehicleId;
    option.textContent = `${data.brand} ${data.model} (${data.registrationNumber})`;
    dropdown.appendChild(option);
    dropdown.value = data.vehicleId;
    dropdown.disabled = false;
}

    // Execute callback with the new vehicle ID
    if (callback && typeof callback === 'function') {
    callback(data.vehicleId);
} else {
    hideSpinner();
    showToast('Vehicle added successfully', 'success');
}
})
    .catch(error => {
    hideSpinner();
    console.error('Error saving vehicle:', error);
    showToast('Failed to create vehicle: ' + error.message, 'error');
});
}

    /**
    * Create a service request with a specific vehicle
    */
    function createServiceRequestWithVehicle(vehicleId) {
    const serviceRequest = {
    vehicleId: vehicleId,
    serviceType: document.getElementById('serviceType').value,
    deliveryDate: document.getElementById('deliveryDate').value,
    additionalDescription: document.getElementById('description').value || "",
    status: "Received" // Initialize with Received status
};

    showSpinner();

    // Get the token
    const token = getToken();
    if (!token) {
    hideSpinner();
    showToast('Authentication error - please login again', 'error');
    return;
}

    // Post to the correct API endpoint
    fetch(`/admin/service-requests/api${token ? `?token=${token}` : ''}`, {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
},
    body: JSON.stringify(serviceRequest)
})
    .then(response => {
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}

    if (!response.ok) {
    return response.text().then(text => {
    try {
    const errorData = JSON.parse(text);
    throw new Error(errorData.error || 'Failed to create service request');
} catch (e) {
    throw new Error('Failed to create service request: ' + text);
}
});
}

    return response.json();
})
    .then(data => {
    hideSpinner();

    // Close modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('addServiceRequestModal'));
    if (modal) {
    modal.hide();
}

    // Reset form
    document.getElementById('addServiceRequestForm').reset();
    document.getElementById('newVehicleForm').style.display = 'none';
    resetVehicleDropdown();

    // Show success message
    showSuccessModal('Service Request Created', 'The service request has been created successfully.');

    // Add new request to the list and refresh
    if (data) {
    serviceRequests.unshift(data);
    renderServiceRequests();
    setupPagination();
} else {
    // Reload service requests if data is not returned
    loadServiceRequests();
}
})
    .catch(error => {
    hideSpinner();
    console.error('Error creating service request:', error);
    showToast('Failed to create service request: ' + error.message, 'error');
});
}

    /**
    * Save a service request - handles validation & form logic
    */
    function saveServiceRequest() {
    const form = document.getElementById('addServiceRequestForm');
    if (!form) return;

    // Check if we're adding a new vehicle
    const newVehicleForm = document.getElementById('newVehicleForm');
    const isAddingNewVehicle = newVehicleForm && newVehicleForm.style.display !== 'none';

    // We need to handle validation differently based on what the user is doing
    let formValid = true;

    // Validate main form fields
    const customerId = document.getElementById('customerId').value;
    const serviceType = document.getElementById('serviceType').value;
    const deliveryDate = document.getElementById('deliveryDate').value;

    if (!customerId) {
    showToast('Please select a customer', 'error');
    formValid = false;
    return;
}

    if (!serviceType) {
    showToast('Please select a service type', 'error');
    formValid = false;
    return;
}

    if (!deliveryDate) {
    showToast('Please select a delivery date', 'error');
    formValid = false;
    return;
}

    // Different validation based on whether we're adding a new vehicle or using an existing one
    if (isAddingNewVehicle) {
    // Validate new vehicle fields
    const brand = document.getElementById('vehicleBrand').value;
    const model = document.getElementById('vehicleModel').value;
    const regNumber = document.getElementById('vehicleRegNumber').value;
    const category = document.getElementById('vehicleCategory').value;
    const year = document.getElementById('vehicleYear').value;

    if (!brand || !model || !regNumber || !category || !year) {
    showToast('Please fill all required vehicle information', 'error');
    formValid = false;
    return;
}

    // If vehicle form is valid, first save the vehicle
    if (formValid) {
    saveNewVehicle(function(newVehicleId) {
    // After vehicle is saved, create the service request with the new vehicle ID
    createServiceRequestWithVehicle(newVehicleId);
});
    return; // Return early as we'll create the service request in the callback
}
} else {
    // Validate vehicle selection
    const vehicleId = document.getElementById('vehicleId').value;
    if (!vehicleId) {
    showToast('Please select a vehicle', 'error');
    formValid = false;
    return;
}

    // If form is valid, create the service request with the selected vehicle
    if (formValid) {
    createServiceRequestWithVehicle(vehicleId);
}
}
}

    /**
    * Fetch a service request from API
    */
    function fetchServiceRequest(requestId) {
    // Get token
    const token = getToken();
    if (!token) {
    showToast('Authentication error - please login again', 'error');
    return;
}

    showSpinner();

    fetch(`/admin/service-requests/api/${requestId}${token ? `?token=${token}` : ''}`, {
    method: 'GET',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
}
})
    .then(response => {
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}
    if (!response.ok) {
    throw new Error(`HTTP error! Status: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    hideSpinner();
    displayServiceRequestDetails(data);
})
    .catch(error => {
    hideSpinner();
    console.error('Error fetching service request:', error);
    showToast('Failed to load service request details: ' + error.message, 'error');
});
}

    /**
    * Display service request details in modal
    */
    function displayServiceRequestDetails(request) {
    console.log("Displaying request details:", JSON.stringify(request));

    // Set request details in the modal
    document.getElementById('viewRequestId').textContent = `REQ-${request.requestId}`;

    // Set status with appropriate badge
    const statusElement = document.getElementById('viewStatus');
    const status = request.status || 'Received';
    statusElement.innerHTML = `
        <span class="status-badge status-${status.toLowerCase()}">
            <i class="fas fa-${
    status === 'Received' ? 'clock' :
    status === 'Diagnosis' ? 'stethoscope' :
    status === 'Repair' ? 'wrench' :
    'check-circle'
}"></i>
            ${status}
        </span>
    `;

    document.getElementById('viewCreatedDate').textContent = formatDate(request.createdAt || new Date().toISOString());

    // Handle vehicle name properly
    const vehicleName = request.vehicleName ||
    (request.vehicleBrand && request.vehicleModel ?
    `${request.vehicleBrand} ${request.vehicleModel}` :
    (request.vehicleModel || 'Unknown Vehicle'));
    document.getElementById('viewVehicleName').textContent = vehicleName;

    // Handle registration number - check both field names
    const regNumber = request.registrationNumber || request.vehicleRegistration || 'No Registration';
    document.getElementById('viewRegistrationNumber').textContent = regNumber;

    document.getElementById('viewVehicleCategory').textContent = request.vehicleCategory || request.vehicleType || 'Car';
    document.getElementById('viewCustomerName').textContent = request.customerName || 'N/A';

    // Fix for customer contact - try several possible fields
    const contactInfo = request.customerPhone ||
    request.phoneNumber ||
    request.customerContact ||
    request.customerEmail ||
    'N/A';
    document.getElementById('viewCustomerContact').textContent = contactInfo;

    // Set membership with appropriate badge
    const membershipElement = document.getElementById('viewMembership');
    const membershipStatus = request.membershipStatus || 'Standard';
    membershipElement.innerHTML = `
        <span class="membership-badge membership-${membershipStatus.toLowerCase()}">
            <i class="fas fa-${membershipStatus === 'Premium' ? 'crown' : 'user'}"></i>
            ${membershipStatus}
        </span>
    `;

    document.getElementById('viewServiceType').textContent = request.serviceType || 'N/A';
    document.getElementById('viewDeliveryDate').textContent = formatDate(request.deliveryDate);
    document.getElementById('viewServiceAdvisor').textContent = request.serviceAdvisorName || 'Not Assigned';
    document.getElementById('viewDescription').textContent = request.additionalDescription || 'No additional description provided.';

    // Show materials section if available
    const materialsSection = document.getElementById('materialsSection');
    if (materialsSection) {
    if (request.materials && request.materials.length > 0) {
    materialsSection.style.display = 'block';
    populateMaterialsTable(request.materials);
} else {
    materialsSection.style.display = 'none';
}
}

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('viewServiceRequestModal'));
    modal.show();
}

    /**
    * Populate materials table
    */
    function populateMaterialsTable(materials) {
    const tableBody = document.getElementById('materialsTableBody');
    if (!tableBody) return;

    tableBody.innerHTML = '';

    let totalCost = 0;

    materials.forEach(material => {
    const row = document.createElement('tr');
    const itemTotal = material.quantity * material.unitPrice;
    totalCost += itemTotal;

    row.innerHTML = `
            <td>${material.name}</td>
            <td>${material.quantity}</td>
            <td>₹${material.unitPrice.toFixed(2)}</td>
            <td>₹${itemTotal.toFixed(2)}</td>
        `;

    tableBody.appendChild(row);
});

    // Add total row
    const totalRow = document.createElement('tr');
    totalRow.className = 'fw-bold';
    totalRow.innerHTML = `
        <td colspan="3" class="text-end">Total</td>
        <td>₹${totalCost.toFixed(2)}</td>
    `;

    tableBody.appendChild(totalRow);
}

    /**
    * Assign a service advisor to a request
    */
    function assignServiceAdvisor() {
    const requestId = document.getElementById('selectedRequestId').value;
    const selectedAdvisor = document.querySelector('.advisor-card.selected');

    if (!selectedAdvisor) {
    showToast('Please select a service advisor', 'error');
    return;
}

    const advisorId = selectedAdvisor.getAttribute('data-advisor-id');
    if (!advisorId) {
    showToast('Invalid advisor selection', 'error');
    return;
}

    showSpinner();

    // Get token
    const token = getToken();
    if (!token) {
    hideSpinner();
    showToast('Authentication error - please login again', 'error');
    return;
}

    // Add log to see what we're sending
    console.log(`Assigning advisor ${advisorId} to request ${requestId}`);

    // Use the correct endpoint and method
    fetch(`/admin/service-requests/api/${requestId}/assign${token ? `?token=${token}` : ''}`, {
    method: 'PUT',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
},
    body: JSON.stringify({ advisorId: parseInt(advisorId) })
})
    .then(response => {
    console.log("Response status:", response.status);
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}
    if (!response.ok) {
    return response.text().then(text => {
    console.error("Error response:", text);
    try {
    const errorData = JSON.parse(text);
    throw new Error(errorData.error || 'Failed to assign service advisor');
} catch (e) {
    throw new Error('Failed to assign service advisor: ' + text);
}
});
}
    return response.json();
})
    .then(data => {
    hideSpinner();
    console.log("Assignment successful:", data);

    // Close modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('assignAdvisorModal'));
    if (modal) {
    modal.hide();
}

    // Show success message
    showSuccessModal('Service Advisor Assigned', 'The service advisor has been assigned successfully.');

    // Update request in the list
    const index = serviceRequests.findIndex(req => req.requestId === parseInt(requestId));
    if (index !== -1) {
    serviceRequests[index] = data;
    renderServiceRequests();
} else {
    // Reload service requests if the updated request isn't found
    loadServiceRequests();
}
})
    .catch(error => {
    hideSpinner();
    console.error('Error assigning service advisor:', error);
    showToast('Failed to assign service advisor: ' + error.message, 'error');
});
}

    /**
    * Render the service requests in the table
    */
    function renderServiceRequests() {
    const tableBody = document.getElementById('serviceRequestsTableBody');
    if (!tableBody) return;

    // Clear table body except for special rows
    const rows = tableBody.querySelectorAll('tr:not(#loading-row):not(#empty-row)');
    rows.forEach(row => row.remove());

    if (!serviceRequests || serviceRequests.length === 0) {
    const emptyRow = document.getElementById('empty-row');
    if (emptyRow) {
    emptyRow.style.display = 'table-row';
}
    return;
}

    const emptyRow = document.getElementById('empty-row');
    if (emptyRow) {
    emptyRow.style.display = 'none';
}

    // Get requests for current page
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, serviceRequests.length);
    const displayedRequests = serviceRequests.slice(startIndex, endIndex);

    // Create rows for displayed requests
    displayedRequests.forEach(request => {
    // Log the full request object to debug
    console.log("Request object:", JSON.stringify(request));

    // Ensure we have valid data or defaults
    const status = request.status || "Unknown";
    const membershipStatus = request.membershipStatus || 'Standard';

    // Handle vehicle name/brand/model
    const vehicleName = request.vehicleName ||
    (request.vehicleBrand && request.vehicleModel ?
    `${request.vehicleBrand} ${request.vehicleModel}` :
    (request.vehicleModel || 'Unknown Vehicle'));

    // Handle registration number
    const regNumber = request.registrationNumber ||
    request.vehicleRegistration ||
    'No Registration';

    const row = document.createElement('tr');
    row.className = 'active-page';
    row.innerHTML = `
        <td>REQ-${request.requestId}</td>
        <td>
            <div class="vehicle-info">
                <div class="vehicle-icon">
                    <i class="fas fa-${request.vehicleCategory === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                </div>
                <div class="vehicle-details">
                    <h5>${vehicleName}</h5>
                    <p>${regNumber}</p>
                </div>
            </div>
        </td>
        <td>
            <div class="person-info">
                <div class="person-details">
                    <h5>${request.customerName || 'N/A'}</h5>
                    <p>${request.customerEmail || ''}</p>
                </div>
            </div>
        </td>
        <td>
            <span class="membership-badge membership-${membershipStatus.toLowerCase()}">
                <i class="fas fa-${membershipStatus.toLowerCase() === 'premium' ? 'crown' : 'user'}"></i>
                ${membershipStatus}
            </span>
        </td>
        <td>${request.serviceType || 'N/A'}</td>
        <td>
            <span class="status-badge status-${status.toLowerCase()}">
                <i class="fas fa-${
    status === 'Received' ? 'clock' :
    status === 'Diagnosis' ? 'stethoscope' :
    status === 'Repair' ? 'wrench' :
    status === 'Completed' ? 'check-circle' :
    'question-circle'
}"></i>
                ${status}
            </span>
        </td>
        <td>${formatDate(request.deliveryDate)}</td>
        <td>${request.serviceAdvisorName || 'Not Assigned'}</td>
        <td class="table-actions-cell">
            <button class="btn-table-action view-btn" title="View Details" onclick="showServiceRequestDetails(${request.requestId})">
                <i class="fas fa-eye"></i>
            </button>
            ${!request.serviceAdvisorId ?
    `<button class="btn-assign" title="Assign Advisor" onclick="showAssignAdvisorModal(${request.requestId})">
                    <i class="fas fa-user-plus"></i> Assign
                </button>` :
    `<button class="btn-table-action edit-btn" title="Edit Request">
                    <i class="fas fa-edit"></i>
                </button>`
}
        </td>
    `;

    tableBody.appendChild(row);
});
}

    /**
    * Filter service requests by search term
    */
    function filterServiceRequests(searchTerm) {
    if (!searchTerm) {
    renderServiceRequests();
    return;
}

    searchTerm = searchTerm.toLowerCase();

    const filteredRequests = serviceRequests.filter(request => {
    return (
    `req-${request.requestId}`.toLowerCase().includes(searchTerm) ||
    (request.customerName && request.customerName.toLowerCase().includes(searchTerm)) ||
    (request.registrationNumber && request.registrationNumber.toLowerCase().includes(searchTerm)) ||
    (request.vehicleName && request.vehicleName.toLowerCase().includes(searchTerm)) ||
    (request.vehicleBrand && request.vehicleBrand.toLowerCase().includes(searchTerm)) ||
    (request.vehicleModel && request.vehicleModel.toLowerCase().includes(searchTerm)) ||
    (request.serviceType && request.serviceType.toLowerCase().includes(searchTerm)) ||
    (request.status && request.status.toLowerCase().includes(searchTerm))
    );
});

    currentPage = 1;
    renderServiceRequests(filteredRequests);
    setupPagination();
}

    /**
    * Filter table rows by search term
    */
    function filterTableRows(searchTerm) {
    if (!searchTerm) {
    // Reset all rows
    document.querySelectorAll('#serviceRequestsTable tbody tr').forEach(row => {
    row.style.display = '';
});
    return;
}

    searchTerm = searchTerm.toLowerCase();

    document.querySelectorAll('#serviceRequestsTable tbody tr').forEach(row => {
    const text = row.textContent.toLowerCase();
    if (text.includes(searchTerm)) {
    row.style.display = '';
} else {
    row.style.display = 'none';
}
});
}

    /**
    * Set up pagination for service requests
    */
    function setupPagination() {
    const totalPages = Math.ceil(serviceRequests.length / itemsPerPage);
    const pagination = document.getElementById('pagination');
    if (!pagination) return;

    // Clear existing page links except for prev/next buttons
    const pageLinks = pagination.querySelectorAll('.page-link:not(#prevBtn):not(#nextBtn)');
    pageLinks.forEach(link => link.parentElement.remove());

    // Add page links
    const prevBtn = document.getElementById('prevBtn');
    const prevBtnParent = prevBtn ? prevBtn.parentElement : null;
    const nextBtn = document.getElementById('nextBtn');
    const nextBtnParent = nextBtn ? nextBtn.parentElement : null;

    if (prevBtnParent && nextBtnParent) {
    for (let i = 1; i <= totalPages; i++) {
    const li = document.createElement('li');
    li.className = `page-item ${i === currentPage ? 'active' : ''}`;
    li.innerHTML = `<a class="page-link" href="#" data-page="${i}">${i}</a>`;

    // Add event listener
    li.querySelector('.page-link').addEventListener('click', function(e) {
    e.preventDefault();
    changePage(i);
});

    pagination.insertBefore(li, nextBtnParent);
}

    // Update prev/next buttons
    prevBtnParent.classList.toggle('disabled', currentPage === 1);
    nextBtnParent.classList.toggle('disabled', currentPage === totalPages || totalPages === 0);
}
}

    /**
    * Change the current page
    */
    function changePage(page) {
    currentPage = page;
    renderServiceRequests();
    updatePaginationUI();
}

    /**
    * Update the pagination UI based on current page
    */
    function updatePaginationUI() {
    const totalPages = Math.ceil(serviceRequests.length / itemsPerPage);
    const pagination = document.getElementById('pagination');
    if (!pagination) return;

    // Update active page
    const pageItems = pagination.querySelectorAll('.page-item');
    pageItems.forEach(item => {
    const pageLink = item.querySelector('.page-link');
    if (pageLink && pageLink.id !== 'prevBtn' && pageLink.id !== 'nextBtn') {
    const page = parseInt(pageLink.getAttribute('data-page'));
    item.classList.toggle('active', page === currentPage);
}
});

    // Update prev/next buttons
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    if (prevBtn) {
    prevBtn.parentElement.classList.toggle('disabled', currentPage === 1);
}

    if (nextBtn) {
    nextBtn.parentElement.classList.toggle('disabled', currentPage === totalPages || totalPages === 0);
}
}

    /**
    * Format a date string nicely
    */
    function formatDate(dateString) {
    if (!dateString) return 'N/A';

    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    try {
    return new Date(dateString).toLocaleDateString('en-US', options);
} catch (e) {
    console.error("Error formatting date:", dateString, e);
    return dateString || 'N/A';
}
}

    /**
    * Show toast message
    */
    function showToast(message, type = 'success') {
    // Create toast container if it doesn't exist
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
    document.body.appendChild(toastContainer);
}

    // Create toast element
    const toastId = 'toast-' + Date.now();
    const toastHTML = `
        <div class="toast" role="alert" aria-live="assertive" aria-atomic="true" id="${toastId}">
            <div class="toast-header">
                <strong class="me-auto">${type === 'success' ? 'Success' : 'Error'}</strong>
                <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;

    toastContainer.insertAdjacentHTML('beforeend', toastHTML);

    // Initialize and show toast
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
    autohide: true,
    delay: 5000
});
    toast.show();

    // Remove toast after it's hidden
    toastElement.addEventListener('hidden.bs.toast', function() {
    toastElement.remove();
});
}

    /**
    * Show success modal
    */
    function showSuccessModal(title, message) {
    document.getElementById('successTitle').textContent = title;
    document.getElementById('successMessage').textContent = message;

    const modal = new bootstrap.Modal(document.getElementById('successModal'));
    modal.show();
}