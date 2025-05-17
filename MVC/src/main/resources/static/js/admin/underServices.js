 // Vehicle Tracking System JavaScript
    document.addEventListener('DOMContentLoaded', function() {
    // Initialize app
    initializeApp();

    // Set up event listeners
    setupEventListeners();

    // Load vehicles under service from API
    loadVehiclesUnderServiceFromAPI();
});

    // Global variables
    let vehiclesUnderService = [];
    let currentServicePage = 1;
    const itemsPerPage = 5;
    let currentServiceId = null;

    // Get JWT token from storage or URL
    function getJwtToken() {
    // First check URL parameter
    const urlParams = new URLSearchParams(window.location.search);
    const tokenFromUrl = urlParams.get('token');

    if (tokenFromUrl) {
    return tokenFromUrl;
}

    // Then check local storage
    return localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");
}

    // Initialize app
    function initializeApp() {
    // Setup mobile menu toggle
    setupMobileMenu();

    // Setup logout button
    setupLogout();

    // Setup token-based authentication
    setupAuthentication();

    // Set current date if element exists
    setupDateDisplay();
}

    function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    if (mobileMenuToggle) {
    mobileMenuToggle.addEventListener('click', function() {
    document.getElementById('sidebar').classList.toggle('active');
});
}
}

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

    function setupDateDisplay() {
    const dateElement = document.getElementById('current-date');
    if (dateElement) {
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const today = new Date();
    dateElement.textContent = today.toLocaleDateString('en-US', options);
}
}

    function setupAuthentication() {
    // Get token from storage
    const tokenFromStorage = getJwtToken();

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

    function setupEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
    searchInput.addEventListener('keyup', function() {
    filterVehiclesUnderService(this.value);
});
}

    // Table search for Vehicles Under Service
    const serviceTableSearchInput = document.querySelector('.search-input-sm');
    if (serviceTableSearchInput) {
    serviceTableSearchInput.addEventListener('keyup', function() {
    filterTableRows(this.value, 'vehiclesUnderServiceTable');
});
}

    // Table pagination for Vehicles Under Service
    document.querySelectorAll('#paginationService .page-link').forEach(link => {
    link.addEventListener('click', function(e) {
    e.preventDefault();
    if (!this.parentElement.classList.contains('disabled')) {
    const page = this.getAttribute('data-page');
    if (page) {
    currentServicePage = parseInt(page);
    updateServicePagination();
    showVehiclesUnderServicePage(currentServicePage);
}
}
});
});

    // Previous & Next buttons for Vehicles Under Service
    document.getElementById('prevBtnService').addEventListener('click', function(e) {
    e.preventDefault();
    if (currentServicePage > 1) {
    currentServicePage--;
    updateServicePagination();
    showVehiclesUnderServicePage(currentServicePage);
}
});

    document.getElementById('nextBtnService').addEventListener('click', function(e) {
    e.preventDefault();
    const totalPages = Math.ceil(vehiclesUnderService.length / itemsPerPage);
    if (currentServicePage < totalPages) {
    currentServicePage++;
    updateServicePagination();
    showVehiclesUnderServicePage(currentServicePage);
}
});

    // Apply Filter button
    document.getElementById('applyFilterBtn').addEventListener('click', function() {
    applyFilters();
});

    // Update Status button click
    document.getElementById('updateStatusBtn').addEventListener('click', function() {
    const serviceId = document.getElementById('viewServiceId').textContent.replace('REQ-', '');
    updateServiceStatus(serviceId);
});

    // Confirm Update Status button click
    document.getElementById('confirmUpdateStatusBtn').addEventListener('click', function() {
    submitStatusUpdate();
});
}

    // Load vehicles under service from API
    function loadVehiclesUnderServiceFromAPI() {
    // Show loading spinner
    document.getElementById('loading-row-service').style.display = '';
    document.getElementById('empty-row-service').style.display = 'none';

    // Get token
    const token = getJwtToken();
    if (!token) {
    console.error('No token found');
    return;
}

    // API call to fetch vehicles under service
    fetch('/admin/api/vehicle-tracking/under-service?token=' + encodeURIComponent(token))
    .then(response => {
    if (!response.ok) {
    throw new Error('API call failed: ' + response.status);
}
    return response.json();
})
    .then(data => {
    vehiclesUnderService = data;

    // Hide loading spinner
    document.getElementById('loading-row-service').style.display = 'none';

    // Check if there are vehicles to display
    if (vehiclesUnderService.length > 0) {
    // Update pagination
    updateServicePagination();

    // Show first page
    showVehiclesUnderServicePage(currentServicePage);
} else {
    // Show enhanced empty state with better visuals
    document.getElementById('empty-row-service').style.display = '';
    document.getElementById('empty-row-service').innerHTML = `
                <td colspan="10" class="text-center py-5">
                    <div class="my-5">
                        <i class="fas fa-car-alt fa-4x text-muted mb-4" style="opacity: 0.3;"></i>
                        <h4 class="text-wine">No Vehicles Currently Under Service</h4>
                        <p class="text-muted mt-3">
                            There are no vehicles currently undergoing service.
                            <br>Vehicles will appear here when they are received for servicing.
                        </p>
                        <button class="btn-premium primary mt-3" onclick="refreshVehicleData()">
                            <i class="fas fa-sync-alt"></i>
                            Refresh Data
                        </button>
                    </div>
                </td>
                `;
}
})
    .catch(error => {
    console.error('Error fetching vehicles under service:', error);
    // Hide loading spinner and show empty state with error
    document.getElementById('loading-row-service').style.display = 'none';
    document.getElementById('empty-row-service').style.display = '';
    document.getElementById('empty-row-service').innerHTML = `
                <td colspan="10" class="text-center py-5">
                    <div class="my-5">
                        <i class="fas fa-exclamation-triangle fa-4x text-danger mb-4"></i>
                        <h4 class="text-danger">Error Loading Vehicles</h4>
                        <p class="text-muted mt-3">
                            We encountered a problem while loading vehicles under service.
                            <br>Error: ${error.message}
                        </p>
                        <button class="btn-premium primary mt-3" onclick="refreshVehicleData()">
                            <i class="fas fa-sync-alt"></i>
                            Try Again
                        </button>
                    </div>
                </td>
            `;
});
}

    // Add a refresh function to reload the data
    function refreshVehicleData() {
    // Show loading state
    document.getElementById('loading-row-service').style.display = '';
    document.getElementById('empty-row-service').style.display = 'none';

    // Refresh data
    loadVehiclesUnderServiceFromAPI();

    // Show toast notification
    showToastNotification('Refreshing Data', 'Fetching the latest vehicle service information...');
}

    // Filter vehicles under service
    function filterVehiclesUnderService(searchText) {
    if (!searchText || searchText.trim() === '') {
    // Reset to show all vehicles
    showVehiclesUnderServicePage(currentServicePage);
    updateServicePagination();
    return;
}

    searchText = searchText.toLowerCase().trim();

    // Filter vehicles based on search text
    const filteredVehicles = vehiclesUnderService.filter(vehicle => {
    return (
    (vehicle.vehicleName && vehicle.vehicleName.toLowerCase().includes(searchText)) ||
    (vehicle.registrationNumber && vehicle.registrationNumber.toLowerCase().includes(searchText)) ||
    (vehicle.customerName && vehicle.customerName.toLowerCase().includes(searchText)) ||
    (vehicle.serviceType && vehicle.serviceType.toLowerCase().includes(searchText)) ||
    (vehicle.status && vehicle.status.toLowerCase().includes(searchText)) ||
    (vehicle.serviceAdvisorName && vehicle.serviceAdvisorName.toLowerCase().includes(searchText))
    );
});

    // Display filtered vehicles
    displayVehiclesUnderService(filteredVehicles);

    // Update pagination
    updateServicePagination(filteredVehicles.length);
}

    // Filter table rows
    function filterTableRows(searchText, tableId) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr');
    if (!rows.length) return;

    searchText = searchText.toLowerCase().trim();

    rows.forEach(row => {
    if (row.id.includes('loading-row') || row.id.includes('empty-row')) {
    return; // Skip loading and empty rows
}

    const text = row.textContent.toLowerCase();
    if (text.includes(searchText) || !searchText) {
    row.style.display = '';
} else {
    row.style.display = 'none';
}
});
}

    // Show vehicles under service page
    function showVehiclesUnderServicePage(page) {
    const startIndex = (page - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, vehiclesUnderService.length);
    const vehiclesToShow = vehiclesUnderService.slice(startIndex, endIndex);

    displayVehiclesUnderService(vehiclesToShow);
}

    // Display vehicles under service
    function displayVehiclesUnderService(vehicles) {
    const tableBody = document.getElementById('vehiclesUnderServiceTableBody');

    // Clear existing rows except loading and empty state rows
    const loadingRow = document.getElementById('loading-row-service');
    const emptyRow = document.getElementById('empty-row-service');

    tableBody.innerHTML = '';
    tableBody.appendChild(loadingRow);
    tableBody.appendChild(emptyRow);

    loadingRow.style.display = 'none';

    // Filter vehicles that have service advisors assigned
    const assignedVehicles = vehicles.filter(vehicle =>
    vehicle.serviceAdvisorName &&
    vehicle.serviceAdvisorName !== 'Not Assigned');

    if (assignedVehicles.length === 0) {
    emptyRow.style.display = '';
    // Update empty state message for clarity
    emptyRow.innerHTML = `
            <td colspan="10" class="text-center py-4">
                <div class="my-5">
                    <i class="fas fa-user-tie fa-3x text-muted mb-3"></i>
                    <h5>No vehicles with assigned service advisors</h5>
                    <p class="text-muted">There are no vehicles currently assigned to service advisors</p>
                </div>
            </td>
        `;
    return;
}

    emptyRow.style.display = 'none';

    // Add vehicle rows
    assignedVehicles.forEach(vehicle => {
    const row = document.createElement('tr');
    row.setAttribute('data-id', vehicle.requestId);
    row.classList.add('active-page');

    // Format the date strings
    const startDate = new Date(vehicle.startDate).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
});

    const estimatedCompletion = new Date(vehicle.estimatedCompletionDate).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
});

    // Build the row HTML
    row.innerHTML = `
        <td>REQ-${vehicle.requestId}</td>
        <td>
            <div class="vehicle-info">
                <div class="vehicle-icon">
                    <i class="fas fa-${vehicle.category.toLowerCase() === 'bike' ? 'motorcycle' : 'car'}"></i>
                </div>
                <div class="vehicle-details">
                    <h5>${vehicle.vehicleName}</h5>
                    <p>${vehicle.registrationNumber}</p>
                </div>
            </div>
        </td>
        <td>
            <div class="person-info">
                <h5>${vehicle.customerName}</h5>
            </div>
        </td>
        <td>
            ${vehicle.membershipStatus === 'Premium' ?
    '<span class="membership-badge membership-premium"><i class="fas fa-crown"></i> Premium</span>' :
    '<span class="membership-badge membership-standard"><i class="fas fa-user"></i> Standard</span>'}
        </td>
        <td>${vehicle.serviceType}</td>
        <td>${vehicle.serviceAdvisorName}</td>
        <td>${startDate}</td>
        <td>${estimatedCompletion}</td>
        <td>
            <div class="table-actions-cell">
                <button class="btn-table-action" id="viewServiceBtn" title="View Details">
                    <i class="fas fa-eye"></i>
                </button>
            </div>
        </td>
    `;

    tableBody.appendChild(row);
});
}

    // Update service pagination
    function updateServicePagination(filteredCount) {
    const totalItems = filteredCount !== undefined ? filteredCount : vehiclesUnderService.length;
    const totalPages = Math.ceil(totalItems / itemsPerPage);

    // Update page numbers
    const pagination = document.getElementById('paginationService');
    const pageLinks = pagination.querySelectorAll('.page-link[data-page]');

    // Update visibility of page numbers
    pageLinks.forEach((link, index) => {
    const pageNumber = index + 1;
    const pageItem = link.parentElement;

    if (pageNumber <= totalPages) {
    pageItem.style.display = '';
    pageItem.classList.toggle('active', pageNumber === currentServicePage);
} else {
    pageItem.style.display = 'none';
}
});

    // Update prev/next buttons
    const prevBtn = document.getElementById('prevBtnService');
    const nextBtn = document.getElementById('nextBtnService');

    prevBtn.parentElement.classList.toggle('disabled', currentServicePage === 1);
    nextBtn.parentElement.classList.toggle('disabled', currentServicePage === totalPages || totalPages === 0);
}

    // View service details for vehicles under service
    function viewServiceUnderServiceDetails(serviceId) {
    // Show spinner
    document.getElementById('spinnerOverlay').classList.add('show');

    // Store current service ID
    currentServiceId = serviceId;

    // Get token
    const token = getJwtToken();
    if (!token) {
    console.error('No token found');
    return;
}

    // Find the vehicle in local data first for initial population
    const vehicle = vehiclesUnderService.find(v => v.requestId === parseInt(serviceId));

    // Fetch service details from API
    fetch(`/admin/api/vehicle-tracking/service-request/${serviceId}?token=${encodeURIComponent(token)}`)
    .then(response => {
    if (!response.ok) {
    throw new Error('API call failed: ' + response.status);
}
    return response.json();
})
    .then(service => {
    // Merge local data with API response for more complete information
    const mergedService = { ...vehicle, ...service };

    // Hide spinner
    document.getElementById('spinnerOverlay').classList.remove('show');

    // Format dates with fallbacks
    const startDate = new Date(mergedService.startDate || mergedService.createdAt || Date.now()).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
});

    const estimatedCompletion = new Date(mergedService.estimatedCompletionDate || mergedService.deliveryDate || Date.now()).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
});

    // Update modal content
    document.getElementById('viewServiceId').textContent = 'REQ-' + serviceId;

    // Status badge with fallback
    const status = (mergedService.status || 'Received').toLowerCase();
    let statusBadge = '';
    switch (status) {
    case 'received':
    statusBadge = `<span class="status-badge status-received"><i class="fas fa-clock"></i> Received</span>`;
    break;
    case 'diagnosis':
    statusBadge = `<span class="status-badge status-diagnosis"><i class="fas fa-search"></i> Diagnosis</span>`;
    break;
    case 'repair':
    statusBadge = `<span class="status-badge status-repair"><i class="fas fa-wrench"></i> Repair</span>`;
    break;
    default:
    statusBadge = `<span class="status-badge status-received"><i class="fas fa-clock"></i> Received</span>`;
}

    document.getElementById('viewStatus').innerHTML = statusBadge;
    document.getElementById('viewStartDate').textContent = startDate;
    document.getElementById('viewVehicleName').textContent = mergedService.vehicleName || mergedService.vehicleModel || 'Vehicle';
    document.getElementById('viewRegistrationNumber').textContent = mergedService.registrationNumber || 'Not specified';
    document.getElementById('viewVehicleCategory').textContent = mergedService.category || mergedService.vehicleType || 'Car';
    document.getElementById('viewCustomerName').textContent = mergedService.customerName || 'Customer';
    document.getElementById('viewCustomerContact').textContent = mergedService.customerPhone || mergedService.customerEmail || 'Not available';

    // Membership badge
    let membershipBadge = '';
    if ((mergedService.membershipStatus || '') === 'Premium') {
    membershipBadge = '<span class="membership-badge membership-premium"><i class="fas fa-crown"></i> Premium</span>';
} else {
    membershipBadge = '<span class="membership-badge membership-standard"><i class="fas fa-user"></i> Standard</span>';
}

    document.getElementById('viewMembership').innerHTML = membershipBadge;
    document.getElementById('viewServiceType').textContent = mergedService.serviceType || 'Regular Service';
    document.getElementById('viewEstimatedCompletion').textContent = estimatedCompletion;
    document.getElementById('viewServiceAdvisor').textContent = mergedService.serviceAdvisorName || 'Not Assigned';
    document.getElementById('viewServiceDescription').textContent = mergedService.additionalDescription || 'No description provided.';

    // Progress bar
    const progressBar = document.querySelector('.progress-bar');
    const progressPercentage = calculateProgressPercentage(mergedService.status);
    progressBar.style.width = progressPercentage + '%';
    progressBar.textContent = progressPercentage + '%';
    progressBar.setAttribute('aria-valuenow', progressPercentage);

    document.getElementById('viewProgressNotes').textContent = getProgressNotes(mergedService) || 'No progress notes available.';

    // Show the modal with higher z-index
    const modal = document.getElementById('viewServiceDetailsModal');
    $(modal).modal('show');
    // Force higher z-index after modal is shown
    setTimeout(() => {
    modal.style.zIndex = "1060";
    document.querySelector('.modal-backdrop').style.zIndex = "1050";
}, 100);
})
    .catch(error => {
    // Hide spinner
    document.getElementById('spinnerOverlay').classList.remove('show');
    console.error('Error fetching service details:', error);
    alert('Failed to load service details: ' + error.message);
});
}

    // Calculate progress percentage based on status
    function calculateProgressPercentage(status) {
    switch ((status || '').toLowerCase()) {
    case 'received':
    return 10;
    case 'diagnosis':
    return 40;
    case 'repair':
    return 70;
    case 'completed':
    return 100;
    default:
    return 0;
}
}

    // Get progress notes based on service details
    function getProgressNotes(service) {
    switch ((service.status || '').toLowerCase()) {
    case 'received':
    return 'Vehicle received and awaiting diagnosis. Service advisor will be assigned soon.';
    case 'diagnosis':
    return `Diagnosis in progress by ${service.serviceAdvisorName}. Checking for issues related to ${service.serviceType}.`;
    case 'repair':
    return `Repair work in progress. Estimated completion on ${new Date(service.estimatedCompletionDate || service.deliveryDate).toLocaleDateString()}.`;
    case 'completed':
    return 'Service completed. Vehicle ready for pickup.';
    default:
    return 'Status updates will appear here.';
}
}

    // Apply filters
    function applyFilters() {
    // Get filter values
    const vehicleType = document.getElementById('filterByVehicleType').value;
    const serviceType = document.getElementById('filterByServiceType').value;
    const status = document.getElementById('filterByStatus').value;
    const dateFrom = document.getElementById('filterDateFrom').value;
    const dateTo = document.getElementById('filterDateTo').value;
    const serviceAdvisor = document.getElementById('filterByServiceAdvisor').value;

    // Get token
    const token = getJwtToken();
    if (!token) {
    console.error('No token found');
    return;
}

    // Prepare filter criteria
    const filterCriteria = {
    vehicleType: vehicleType || null,
    serviceType: serviceType || null,
    status: status || null,
    dateFrom: dateFrom || null,
    dateTo: dateTo || null,
    serviceAdvisor: serviceAdvisor || null
};

    // Show loading
    document.getElementById('loading-row-service').style.display = '';
    document.getElementById('empty-row-service').style.display = 'none';

    // API call to filter vehicles under service
    fetch(`/admin/api/vehicle-tracking/under-service/filter?token=${encodeURIComponent(token)}`, {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json'
},
    body: JSON.stringify(filterCriteria)
})
    .then(response => {
    if (!response.ok) {
    throw new Error('API call failed: ' + response.status);
}
    return response.json();
})
    .then(data => {
    // Update filtered vehicles
    vehiclesUnderService = data;

    // Hide loading
    document.getElementById('loading-row-service').style.display = 'none';

    // Display filtered vehicles
    if (vehiclesUnderService.length > 0) {
    displayVehiclesUnderService(vehiclesUnderService);
    updateServicePagination();
} else {
    document.getElementById('empty-row-service').style.display = '';
}

    // Hide filter modal
    $('#filterVehiclesModal').modal('hide');
})
    .catch(error => {
    // Hide loading
    document.getElementById('loading-row-service').style.display = 'none';
    document.getElementById('empty-row-service').style.display = '';

    console.error('Error applying filters to vehicles under service:', error);
    showToastNotification('Error', 'Failed to apply filters: ' + error.message);
});
}

    // Update service status
    function updateServiceStatus(serviceId) {
    // Get current status text
    const currentStatusElement = document.getElementById('viewStatus');
    const currentStatusText = currentStatusElement.textContent.trim();
    let currentStatus = '';

    // Get current status value
    if (currentStatusText.includes('Received')) {
    currentStatus = 'Received';
} else if (currentStatusText.includes('Diagnosis')) {
    currentStatus = 'Diagnosis';
} else if (currentStatusText.includes('Repair')) {
    currentStatus = 'Repair';
} else {
    currentStatus = 'Received'; // Default
}

    // Store current service ID
    currentServiceId = serviceId;

    // Determine recommended next status
    let recommendedStatus = '';
    if (currentStatus === 'Received') {
    recommendedStatus = 'Diagnosis';
} else if (currentStatus === 'Diagnosis') {
    recommendedStatus = 'Repair';
} else if (currentStatus === 'Repair') {
    recommendedStatus = 'Completed';
} else {
    recommendedStatus = 'Diagnosis'; // Default
}

    // Update the confirmation modal
    document.getElementById('updateServiceId').textContent = 'REQ-' + serviceId;
    document.getElementById('currentStatusText').textContent = currentStatus;
    document.getElementById('newStatusText').textContent = recommendedStatus;

    // Update the status dropdown
    const statusSelect = document.getElementById('serviceStatusSelect');
    statusSelect.value = recommendedStatus;

    // Enable/disable options based on current status
    // Cannot go backward in status
    for (let i = 0; i < statusSelect.options.length; i++) {
    const option = statusSelect.options[i];

    // Disable completed if service is not at least in repair
    if (option.value === 'Completed' && currentStatus !== 'Repair') {
    option.disabled = true;
}
    // Disable repair if service is not at least in diagnosis
    else if (option.value === 'Repair' && currentStatus === 'Received') {
    option.disabled = true;
} else {
    option.disabled = false;
}
}

    // Update status select change handler
    statusSelect.onchange = function() {
    document.getElementById('newStatusText').textContent = this.value;
};

    // Hide the view modal and show the update status modal
    $('#viewServiceDetailsModal').modal('hide');

    // Remove modal backdrop and reset body classes before showing new modal
    setTimeout(function() {
    $('.modal-backdrop').remove();
    $('body').removeClass('modal-open');
    $('body').css('padding-right', '');
    $('#updateStatusModal').modal('show');
}, 100);
}

    // Submit the status update to the server
    function submitStatusUpdate() {
    // Show spinner
    document.getElementById('spinnerOverlay').classList.add('show');

    // Get service ID
    const serviceId = currentServiceId;

    // Get token
    const token = getJwtToken();
    if (!token) {
    console.error('No token found');
    return;
}

    // Get the new status
    const newStatus = document.getElementById('serviceStatusSelect').value;

    // Hide confirmation modal
    $('#updateStatusModal').modal('hide');

    // Remove modal backdrop and reset body classes
    setTimeout(function() {
    $('.modal-backdrop').remove();
    $('body').removeClass('modal-open');
    $('body').css('padding-right', '');
}, 100);

    // API call to update status
    fetch(`/admin/api/vehicle-tracking/service-request/${serviceId}/status?token=${encodeURIComponent(token)}`, {
    method: 'PUT',
    headers: {
    'Content-Type': 'application/json'
},
    body: JSON.stringify({
    status: newStatus
})
})
    .then(response => {
    if (!response.ok) {
    throw new Error('API call failed: ' + response.status);
}
    return response.json();
})
    .then(data => {
    // Hide spinner
    document.getElementById('spinnerOverlay').classList.remove('show');

    // If status is now Completed, remove from under service
    if (newStatus === 'Completed') {
    // Remove the vehicle from the under service array
    vehiclesUnderService = vehiclesUnderService.filter(v => v.requestId !== parseInt(serviceId));

    // Update counts and pagination
    updateServicePagination();

    // Refresh the display
    showVehiclesUnderServicePage(currentServicePage);

    // Show a success message with information about the completed service
    document.getElementById('successTitle').textContent = 'Service Completed!';
    document.getElementById('successMessage').textContent =
    `Service REQ-${serviceId} has been marked as completed and moved to Completed Services.`;
    $('#successModal').modal('show');
} else {
    // Update the status in the vehiclesUnderService array
    const serviceIndex = vehiclesUnderService.findIndex(v => v.requestId === parseInt(serviceId));
    if (serviceIndex !== -1) {
    vehiclesUnderService[serviceIndex].status = newStatus;
    showVehiclesUnderServicePage(currentServicePage);
}

    // Show a brief success toast notification
    showToastNotification('Status Updated!', `Service REQ-${serviceId} status updated to ${newStatus}.`);
}
})
    .catch(error => {
    // Hide spinner
    document.getElementById('spinnerOverlay').classList.remove('show');
    console.error('Error updating service status:', error);

    // Show error toast
    showToastNotification('Error', 'Failed to update status: ' + error.message);
});
}

    // Toast notification function
    function showToastNotification(title, message) {
    // Create toast container if it doesn't exist
    let toastContainer = document.querySelector('.toast-container');

    // Create toast element
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white bg-wine border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <strong>${title}</strong> ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
        `;

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    // Initialize and show the toast
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
    animation: true,
    autohide: true,
    delay: 3000
});

    toast.show();

    // Remove toast after it's hidden
    toastElement.addEventListener('hidden.bs.toast', function() {
    toastElement.remove();
});
}

    // Add click event listener to all clickable rows and buttons
    document.addEventListener('click', function(e) {
    // For view vehicle details button
    if (e.target && e.target.id === 'viewServiceBtn' || e.target.closest('#viewServiceBtn')) {
    const row = e.target.closest('tr');
    if (row) {
    const serviceId = row.getAttribute('data-id');
    if (serviceId) {
    viewServiceUnderServiceDetails(serviceId);
}
}
}

    // For clicking on a row
    if (e.target && e.target.tagName !== 'BUTTON' && e.target.closest('tr') && !e.target.closest('thead')) {
    const row = e.target.closest('tr');
    if (row.id !== 'loading-row-service' && row.id !== 'empty-row-service') {
    const serviceId = row.getAttribute('data-id');
    if (serviceId) {
    // Skip if clicking on action buttons or their icons
    if (e.target.closest('.table-actions-cell') ||
    e.target.closest('button') ||
    (e.target.closest('i') && e.target.closest('button'))) {
    return;
}

    viewServiceUnderServiceDetails(serviceId);
}
}
}
});
