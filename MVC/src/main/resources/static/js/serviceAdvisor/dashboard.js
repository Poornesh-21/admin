 // Complete JavaScript code for Service Advisor Dashboard with improved invoice generation

    document.addEventListener('DOMContentLoaded', function() {
    // Initialize data structures
    window.inventoryPrices = {};
    window.inventoryData = {}; // Store full inventory data
    window.inventoryItems = [];
    window.laborCharges = [];
    window.currentRequestId = null;
    window.currentInvoiceNumber = null;

    // Set up token persistence
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    if (token) {
    sessionStorage.setItem('jwt-token', token);
    console.log('Token stored in session storage');
}

    // Add auto-refresh button to header
    addRefreshButton();

    // Load initial data from server
    fetchAssignedVehicles();

    // Set up refresh interval (every 5 minutes)
    const refreshInterval = setInterval(fetchAssignedVehicles, 300000);

    // Initialize event listeners
    initializeEventListeners();

    // Initialize status update events
    initializeStatusEvents();

    // Show the appropriate buttons based on active tab
    updateModalFooterButtons();

    // Add connection status indicator
    addConnectionIndicator();
});

    // Add refresh button to header
    function addRefreshButton() {
    const headerActions = document.querySelector('.header-actions');
    if (headerActions) {
    const refreshButton = document.createElement('button');
    refreshButton.className = 'btn btn-primary';
    refreshButton.innerHTML = '<i class="fas fa-sync"></i> Refresh';
    refreshButton.style.marginLeft = '10px';
    refreshButton.addEventListener('click', function() {
    fetchAssignedVehicles();
    showNotification('Refreshing vehicle data...', 'info');
});
    headerActions.appendChild(refreshButton);
}
}

    // Add connection status indicator
    function addConnectionIndicator() {
    const header = document.querySelector('.main-header');
    if (header) {
    const statusIndicator = document.createElement('div');
    statusIndicator.id = 'connection-status';
    statusIndicator.style.display = 'flex';
    statusIndicator.style.alignItems = 'center';
    statusIndicator.style.marginLeft = 'auto';
    statusIndicator.style.gap = '5px';
    statusIndicator.innerHTML = `
            <span id="status-icon" class="status-indicator" style="width: 10px; height: 10px; border-radius: 50%; background-color: #ccc;"></span>
            <span id="status-text" style="font-size: 0.8rem; color: #666;">Checking connection...</span>
        `;

    const headerTitle = header.querySelector('.header-title');
    if (headerTitle) {
    header.insertBefore(statusIndicator, headerTitle.nextSibling);
} else {
    header.appendChild(statusIndicator);
}

    // Initial check
    checkApiConnection();

    // Check every 30 seconds
    setInterval(checkApiConnection, 30000);
}
}

    // Check API connection
    function checkApiConnection() {
    const statusIcon = document.getElementById('status-icon');
    const statusText = document.getElementById('status-text');

    if (!statusIcon || !statusText) return;

    const token = getAuthToken();
    const headers = {};
    if (token) {
    headers['Authorization'] = `Bearer ${token}`;
}

    fetch('/serviceAdvisor/api/assigned-vehicles', {
    method: 'HEAD',
    headers: headers
})
    .then(response => {
    if (response.ok) {
    statusIcon.style.backgroundColor = '#38b000'; // Green
    statusText.textContent = 'Connected';
    statusText.style.color = '#38b000';
} else {
    statusIcon.style.backgroundColor = '#ffaa00'; // Yellow
    statusText.textContent = 'Connected (Error: ' + response.status + ')';
    statusText.style.color = '#ffaa00';
}
})
    .catch(error => {
    statusIcon.style.backgroundColor = '#d90429'; // Red
    statusText.textContent = 'Disconnected';
    statusText.style.color = '#d90429';
});
}

    // Initialize all event listeners
    function initializeEventListeners() {
    // Filter dropdown toggle
    const filterButton = document.getElementById('filterButton');
    const filterMenu = document.getElementById('filterMenu');

    if (filterButton && filterMenu) {
    filterButton.addEventListener('click', function() {
    filterMenu.classList.toggle('show');
});

    // Hide filter menu when clicking outside
    document.addEventListener('click', function(event) {
    if (!filterButton.contains(event.target) && !filterMenu.contains(event.target)) {
    filterMenu.classList.remove('show');
}
});

    // Filter options
    const filterOptions = document.querySelectorAll('.filter-option');
    filterOptions.forEach(option => {
    option.addEventListener('click', function() {
    // Update active class
    filterOptions.forEach(opt => opt.classList.remove('active'));
    this.classList.add('active');

    // Apply filter
    filterVehicles(this.getAttribute('data-filter'));

    // Close menu
    filterMenu.classList.remove('show');
});
});
}

    // Search functionality
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
    searchInput.addEventListener('input', function() {
    const searchTerm = this.value.toLowerCase();
    filterVehiclesBySearch(searchTerm);
});
}

    // Modal close buttons
    const closeVehicleDetailsModal = document.getElementById('closeVehicleDetailsModal');
    const closeDetailsBtn = document.getElementById('closeDetailsBtn');
    const vehicleDetailsModal = document.getElementById('vehicleDetailsModal');

    if (closeVehicleDetailsModal && vehicleDetailsModal) {
    closeVehicleDetailsModal.addEventListener('click', function() {
    vehicleDetailsModal.classList.remove('show');
});
}

    if (closeDetailsBtn && vehicleDetailsModal) {
    closeDetailsBtn.addEventListener('click', function() {
    vehicleDetailsModal.classList.remove('show');
});
}

    // Tab switching
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => {
    tab.addEventListener('click', function() {
    // Use the handleTabClick function for consistent behavior
    handleTabClick(this);
});
});

    // Add inventory item button
    const addItemBtn = document.getElementById('addItemBtn');
    if (addItemBtn) {
    addItemBtn.addEventListener('click', function() {
    const inventoryItemSelect = document.getElementById('inventoryItemSelect');
    const itemQuantity = document.getElementById('itemQuantity');

    if (inventoryItemSelect.value) {
    addInventoryItem(inventoryItemSelect.value, parseInt(itemQuantity.value) || 1);
} else {
    showNotification('Please select an inventory item', 'error');
}
});
}

    // Add labor charge button
    const addLaborBtn = document.getElementById('addLaborBtn');
    if (addLaborBtn) {
    addLaborBtn.addEventListener('click', function() {
    const hours = parseFloat(document.getElementById('laborHours').value);
    const rate = parseFloat(document.getElementById('laborRate').value);

    if (!isNaN(hours) && !isNaN(rate) && hours > 0 && rate > 0) {
    // Using a default description since we removed the description field
    addLaborCharge("Labor Charge", hours, rate);

    // Reset form
    document.getElementById('laborHours').value = '1';
    document.getElementById('laborRate').value = '65';
} else {
    showNotification('Please enter valid hours and rate', 'error');
}
});
}


    // Save invoice button
    const saveInvoiceBtn = document.getElementById('saveInvoiceBtn');
    if (saveInvoiceBtn) {
    saveInvoiceBtn.addEventListener('click', function() {
    saveServiceItems();
});
}

    // Preview invoice button
    const previewInvoiceBtn = document.getElementById('previewInvoiceBtn');
    if (previewInvoiceBtn) {
    previewInvoiceBtn.addEventListener('click', function() {
    // Switch to the generate-invoice tab
    const generateInvoiceTab = document.querySelector('.tab[data-tab="generate-invoice"]');
    if (generateInvoiceTab) {
    handleTabClick(generateInvoiceTab);
}
});
}



    // Mark as complete button
    const markCompleteBtn = document.getElementById('markCompleteBtn');
    if (markCompleteBtn) {
    markCompleteBtn.addEventListener('click', function() {
    markServiceComplete();
});
}

    // Close modal when clicking outside
    const modalBackdrops = document.querySelectorAll('.modal-backdrop');
    modalBackdrops.forEach(backdrop => {
    backdrop.addEventListener('click', function(event) {
    if (event.target === this) {
    this.classList.remove('show');
}
});
});

    // Prevent click on modal content from closing the modal
    const modalContents = document.querySelectorAll('.modal-content');
    modalContents.forEach(content => {
    content.addEventListener('click', function(event) {
    event.stopPropagation();
});
});

    // Add ESC key event to close modals
    document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
    modalBackdrops.forEach(backdrop => {
    backdrop.classList.remove('show');
});
}
});
}

    // Handle tab click with proper switching logic
    function handleTabClick(tabElement) {
    // Get all tabs and tab contents
    const tabs = document.querySelectorAll('.tab');
    const tabContents = document.querySelectorAll('.tab-content');

    // Remove active class from all tabs and contents
    tabs.forEach(tab => tab.classList.remove('active'));
    tabContents.forEach(content => content.classList.remove('active'));

    // Add active class to clicked tab
    tabElement.classList.add('active');

    // Get tab name and activate corresponding content
    const tabName = tabElement.getAttribute('data-tab');
    document.getElementById(`${tabName}-tab`)?.classList.add('active');

    // Update footer buttons
    updateModalFooterButtons();

    // If it's the invoice tab, update the preview
    if (tabName === 'generate-invoice') {
    setTimeout(updateBillPreview, 100); // Small delay to ensure DOM is updated
}
}

    // Update modal footer buttons based on active tab
    function updateModalFooterButtons() {
    const markCompleteBtn = document.getElementById('markCompleteBtn');
    const activeTab = document.querySelector('.tab.active');

    if (markCompleteBtn && activeTab) {
    const tabName = activeTab.getAttribute('data-tab');

    if (tabName === 'service-items' || tabName === 'generate-invoice') {
    markCompleteBtn.style.display = 'block';
} else {
    markCompleteBtn.style.display = 'none';
}
}
}

    // Authentication helper methods
    function getAuthToken() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token') || sessionStorage.getItem('jwt-token');
    return token;
}

    function createAuthHeaders() {
    const token = getAuthToken();
    const headers = {
    'Content-Type': 'application/json'
};

    if (token) {
    headers['Authorization'] = `Bearer ${token}`;
}

    return headers;
}

    // Track the number of retries
    let fetchRetries = 0;
    const MAX_RETRIES = 3;

    // Fetch assigned vehicles from the server
    function fetchAssignedVehicles() {
    // Get token and set up headers
    const token = getAuthToken();
    const headers = {};

    if (token) {
    headers['Authorization'] = `Bearer ${token}`;
    sessionStorage.setItem('jwt-token', token);
}

    console.log('Fetching assigned vehicles with token:', token ? token.substring(0, 10) + '...' : 'none');

    // Show loading indicator
    const tableBody = document.getElementById('vehiclesTableBody');
    if (tableBody) {
    tableBody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 20px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; margin-bottom: 10px;"></i>
                    <p>Loading assigned vehicles...</p>
                </td>
            </tr>
        `;
}

    // Use the correct endpoint
    fetch('/serviceAdvisor/api/assigned-vehicles', {
    method: 'GET',
    headers: headers
})
    .then(response => {
    if (!response.ok) {
    throw new Error(`HTTP error! Status: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    console.log('Received vehicles data:', data);
    if (Array.isArray(data)) {
    // Reset retries on success
    fetchRetries = 0;
    updateVehiclesTable(data);
} else {
    throw new Error('Invalid data format: expected an array');
}
})
    .catch(error => {
    console.error('Error fetching assigned vehicles:', error);

    // Try a few times before falling back to dummy data
    if (fetchRetries < MAX_RETRIES) {
    fetchRetries++;
    console.log(`Retrying fetch... attempt ${fetchRetries}/${MAX_RETRIES}`);
    setTimeout(fetchAssignedVehicles, 1000); // Retry after 1 second
    showNotification(`Retrying to load data (${fetchRetries}/${MAX_RETRIES})...`, 'info');
} else {
    showNotification('Error loading assigned vehicles: ' + error.message, 'error');
    loadDummyData(); // Only fall back after MAX_RETRIES attempts
}
});
}

    // Load dummy data when API fails
    function loadDummyData() {
    console.warn("Loading dummy data as a fallback. This should only happen if the API is unavailable.");
    showNotification("Unable to connect to server, showing placeholder data", "warning");

    // Example dummy data for testing when API is unavailable
    const dummyVehicles = [
{
    requestId: 1,
    vehicleName: "Honda Civic (2020)",
    registrationNumber: "ABC-1234",
    customerName: "John Smith",
    customerEmail: "john.smith@example.com",
    serviceType: "General Service",
    startDate: "2025-05-05",
    status: "Diagnosis"
},
{
    requestId: 2,
    vehicleName: "Toyota Camry (2019)",
    registrationNumber: "XYZ-5678",
    customerName: "Sarah Johnson",
    customerEmail: "sarah.j@example.com",
    serviceType: "Oil Change, Wheel Alignment",
    startDate: "2025-05-03",
    status: "Repair"
},
{
    requestId: 3,
    vehicleName: "Ford Mustang (2018)",
    registrationNumber: "DEF-9012",
    customerName: "Michael Brown",
    customerEmail: "michael.b@example.com",
    serviceType: "Engine Check, Brake Service",
    startDate: "2025-05-01",
    status: "Diagnosis"
}
    ];

    updateVehiclesTable(dummyVehicles);
}

    // Update the vehicles table with data from server
    function updateVehiclesTable(vehicles) {
    const tableBody = document.getElementById('vehiclesTableBody');
    if (!tableBody) return;

    // Clear existing rows
    tableBody.innerHTML = '';

    if (!vehicles || vehicles.length === 0) {
    // No vehicles found - show empty state
    const emptyRow = document.createElement('tr');
    emptyRow.innerHTML = `
            <td colspan="6" class="empty-state">
                <i class="fas fa-car-alt"></i>
                <h3>No vehicles assigned</h3>
                <p>You don't have any active service requests assigned to you at the moment.</p>
            </td>
        `;
    tableBody.appendChild(emptyRow);
    return;
}

    // Add rows for each vehicle
    vehicles.forEach((vehicle, index) => {
    const row = document.createElement('tr');
    row.setAttribute('data-id', vehicle.requestId);
    row.onclick = function() {
    openVehicleDetails(vehicle.requestId);
};

    // Format date
    let formattedDate = 'N/A';
    if (vehicle.startDate) {
    try {
    const requestDate = new Date(vehicle.startDate);
    formattedDate = requestDate.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
});
} catch (e) {
    console.warn('Error formatting date:', e);
    formattedDate = vehicle.startDate; // Use raw value as fallback
}
}

    // Status badge class
    let statusClass = 'new';
    let statusText = vehicle.status || 'New';

    // Normalize status for display
    if (statusText.toLowerCase() === 'diagnosis' ||
    statusText.toLowerCase() === 'repair' ||
    statusText.toLowerCase() === 'in progress') {
    statusClass = 'in-progress';
    if (statusText.toLowerCase() === 'diagnosis') {
    statusText = 'Diagnosis';
} else if (statusText.toLowerCase() === 'repair') {
    statusText = 'Repair';
} else {
    statusText = 'In Progress';
}
} else if (statusText.toLowerCase() === 'received' ||
    statusText.toLowerCase() === 'new') {
    statusClass = 'new';
    statusText = 'New';
} else if (statusText.toLowerCase() === 'completed') {
    statusClass = 'completed';
    statusText = 'Completed';
}

    // Safe access to properties with fallbacks
    const vehicleName = vehicle.vehicleName ||
    `${vehicle.vehicleBrand || ''} ${vehicle.vehicleModel || ''}`.trim() ||
    'Unknown Vehicle';
    const registrationNumber = vehicle.registrationNumber || 'No Registration';
    const customerName = vehicle.customerName || 'Unknown Customer';
    const customerEmail = vehicle.customerEmail || 'No Email';
    const serviceType = vehicle.serviceType || 'General Service';

    row.innerHTML = `
            <td>
                <div class="vehicle-details">
                    <div class="vehicle-model">${vehicleName}</div>
                    <div class="vehicle-info">Registration: ${registrationNumber}</div>
                </div>
            </td>
            <td>
                <div class="customer-details">
                    <div class="customer-name">${customerName}</div>
                    <div class="customer-info">${customerEmail}</div>
                </div>
            </td>
            <td>${serviceType}</td>
            <td>${formattedDate}</td>
            <td>
                <span class="status-badge ${statusClass}">
                    <i class="fas fa-circle"></i> ${statusText}
                </span>
            </td>
            <td class="action-cell">
                <button class="action-btn" onclick="openVehicleDetails(${vehicle.requestId}); event.stopPropagation();">
                    <i class="fas fa-eye"></i> View
                </button>
            </td>
        `;

    tableBody.appendChild(row);
});
}

    // Open vehicle details modal
    function openVehicleDetails(requestId) {
    console.log('Opening details for service request ID:', requestId);
    window.currentRequestId = requestId;

    // Show loading indicator in the modal
    const vehicleDetailsModal = document.getElementById('vehicleDetailsModal');
    vehicleDetailsModal.classList.add('show');

    const detailsTab = document.getElementById('details-tab');
    if (detailsTab) {
    detailsTab.innerHTML = `
            <div style="text-align: center; padding: 50px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 32px; margin-bottom: 20px;"></i>
                <p>Loading service details...</p>
            </div>
        `;
}

    // Reset tab to details tab
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => tab.classList.remove('active'));
    document.querySelector('.tab[data-tab="details"]').classList.add('active');

    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(content => content.classList.remove('active'));
    document.getElementById('details-tab').classList.add('active');

    // Update footer buttons
    updateModalFooterButtons();

    // Get authentication token and set up headers
    const token = getAuthToken();
    const headers = {};
    if (token) {
    headers['Authorization'] = `Bearer ${token}`;
}

    console.log('Fetching service details with token:', token ? 'Token present' : 'No token');

    // Fetch vehicle details from server using proper endpoint
    fetch(`/serviceAdvisor/api/service-details/${requestId}`, {
    method: 'GET',
    headers: headers
})
    .then(response => {
    console.log('Service details response status:', response.status);
    if (!response.ok) {
    throw new Error(`Failed to fetch service details: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    console.log('Received service details:', data);

    // Generate a new invoice number for this session
    window.currentInvoiceNumber = 'INV-' + new Date().getFullYear() + '-' +
    String(Math.floor(Math.random() * 10000)).padStart(4, '0');

    // Load vehicle details
    loadVehicleDetails(data);

    // Load current bill data if available
    if (data.currentBill) {
    loadCurrentBill(data.currentBill);
}

    // Load and populate inventory items dropdown
    fetchInventoryItems();
})
    .catch(error => {
    console.error('Error fetching vehicle details:', error);
    showNotification('Error loading service details: ' + error.message, 'error');

    // Show error in the details tab
    if (detailsTab) {
    detailsTab.innerHTML = `
                    <div class="empty-state">
                        <i class="fas fa-exclamation-triangle" style="color: var(--danger);"></i>
                        <h3>Error Loading Details</h3>
                        <p>${error.message}</p>
                        <button class="btn btn-primary" onclick="openVehicleDetails(${requestId})">
                            <i class="fas fa-sync"></i> Retry
                        </button>
                    </div>
                `;
}
});
}

    // Load vehicle details into the UI
    function loadVehicleDetails(data) {
    // Safety check
    if (!data) {
    console.error('No data provided to loadVehicleDetails');
    showNotification('Error: No data received from server', 'error');
    return;
}

    try {
    console.log('Loading vehicle details with data:', data);

    // Create the detail cards if they don't exist yet
    createDetailCardsIfNeeded();

    // Vehicle Information
    const vehicleCard = document.querySelector('.detail-card:nth-of-type(1)');
    if (vehicleCard) {
    console.log('Vehicle card found');
    const makeModel = `${data.vehicleBrand || ''} ${data.vehicleModel || ''}`.trim();
    setDetailValueFixed(vehicleCard, 1, makeModel || 'Not specified');
    setDetailValueFixed(vehicleCard, 2, data.registrationNumber || 'Not specified');
    setDetailValueFixed(vehicleCard, 3, data.vehicleYear || 'Not specified');
    setDetailValueFixed(vehicleCard, 4, data.vehicleType || 'Not specified');
} else {
    console.error('Vehicle card not found in the DOM');
}

    // Customer Information
    const customerCard = document.querySelector('.detail-card:nth-of-type(2)');
    if (customerCard) {
    console.log('Customer card found');
    setDetailValueFixed(customerCard, 1, data.customerName || 'Not specified');
    setDetailValueFixed(customerCard, 2, data.customerEmail || 'Not specified');
    setDetailValueFixed(customerCard, 3, data.customerPhone || 'Not specified');
} else {
    console.error('Customer card not found in the DOM');
}

    // Service Request Details
    const serviceCard = document.querySelector('.detail-card:nth-of-type(3)');
    if (serviceCard) {
    console.log('Service card found');
    setDetailValueFixed(serviceCard, 1, data.serviceType || 'General Service');

    // Format date
    let formattedDate = 'Not specified';
    if (data.requestDate) {
    try {
    const requestDate = new Date(data.requestDate);
    formattedDate = requestDate.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
});
} catch (e) {
    console.error('Error formatting date:', e);
    formattedDate = data.requestDate; // Use raw value as fallback
}
}
    setDetailValueFixed(serviceCard, 2, formattedDate);

    // Status badge
    const statusCell = serviceCard.querySelector('.detail-card-body .detail-row:nth-child(3) .detail-value');
    if (statusCell) {
    const status = data.status || 'New';
    let statusClass = getStatusClass(status);

    statusCell.innerHTML = `
                    <span class="status-badge ${statusClass}">
                        <i class="fas fa-circle"></i> ${status}
                    </span>
                `;
} else {
    console.error('Status cell not found');
}

    // Description
    setDetailValueFixed(serviceCard, 4, data.additionalDescription || 'No additional description provided.');
} else {
    console.error('Service card not found in the DOM');
}

    // Update vehicle summary in other tabs
    const vehicleSummaryElements = document.querySelectorAll('.vehicle-summary .vehicle-info-summary h4');
    const vehicleInfo = `${data.vehicleBrand || ''} ${data.vehicleModel || ''} (${data.registrationNumber || 'Unknown'})`.trim();
    vehicleSummaryElements.forEach(element => {
    element.textContent = vehicleInfo;
});

    // Update customer info in summary
    const customerElements = document.querySelectorAll('.vehicle-summary .vehicle-info-summary p');
    customerElements.forEach(element => {
    element.textContent = `Customer: ${data.customerName || 'Unknown'}`;
});

    // Update status badges in other tabs
    const statusDisplayElements = document.querySelectorAll('.vehicle-summary .status-display');
    const status = data.status || 'New';
    let statusClass = getStatusClass(status);

    statusDisplayElements.forEach(element => {
    element.innerHTML = `
                <span class="status-badge ${statusClass}" id="currentStatusBadge">
                    <i class="fas fa-circle"></i> ${status}
                </span>
            `;
});

    // Update status dropdown
    const statusSelect = document.getElementById('statusSelect');
    if (statusSelect) {
    // Set current status as value
    for (let i = 0; i < statusSelect.options.length; i++) {
    if (statusSelect.options[i].value.toLowerCase() === status.toLowerCase()) {
    statusSelect.selectedIndex = i;
    break;
}
}
}

    console.log('Vehicle details loaded successfully');
} catch (error) {
    console.error('Error loading vehicle details:', error);
    showNotification('Error displaying vehicle details: ' + error.message, 'error');
}
}

    // Improved detail value setter function that correctly navigates the DOM
    function setDetailValueFixed(cardElement, index, value) {
    try {
    // Find specific row by index (1-based)
    const rows = cardElement.querySelectorAll('.detail-card-body .detail-row');
    if (rows.length >= index) {
    const targetRow = rows[index-1];
    const valueElement = targetRow.querySelector('.detail-value');

    if (valueElement) {
    valueElement.textContent = value;
    console.log(`Set detail value at row ${index}: "${value}"`);
} else {
    console.warn(`Value element not found in row ${index}`);
}
} else {
    console.warn(`Row ${index} not found (only ${rows.length} rows available)`);
}
} catch (error) {
    console.error(`Error setting detail value at index ${index}:`, error);
}
}

    // Create detail cards if they don't exist
    function createDetailCardsIfNeeded() {
    const detailsTab = document.getElementById('details-tab');
    if (!detailsTab) return;

    // Check if detail cards already exist
    if (detailsTab.querySelector('.detail-card')) return;

    // Create row for cards
    const row = document.createElement('div');
    row.className = 'row';

    // Vehicle Information Card
    const vehicleCard = document.createElement('div');
    vehicleCard.className = 'detail-card';
    vehicleCard.innerHTML = `
        <div class="detail-card-header">
            Vehicle Information
        </div>
        <div class="detail-card-body">
            <div class="detail-row">
                <div class="detail-label">Make/Model:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Registration:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Year:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Type:</div>
                <div class="detail-value">Loading...</div>
            </div>
        </div>
    `;

    // Customer Information Card
    const customerCard = document.createElement('div');
    customerCard.className = 'detail-card';
    customerCard.innerHTML = `
        <div class="detail-card-header">
            Customer Information
        </div>
        <div class="detail-card-body">
            <div class="detail-row">
                <div class="detail-label">Name:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Email:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Phone:</div>
                <div class="detail-value">Loading...</div>
            </div>
        </div>
    `;

    // Service Request Card
    const serviceCard = document.createElement('div');
    serviceCard.className = 'detail-card';
    serviceCard.innerHTML = `
        <div class="detail-card-header">
            Service Request Details
        </div>
        <div class="detail-card-body">
            <div class="detail-row">
                <div class="detail-label">Service Type:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Request Date:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Status:</div>
                <div class="detail-value">
                    <span class="status-badge new">
                        <i class="fas fa-circle"></i> Loading...
                    </span>
                </div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Description:</div>
                <div class="detail-value">Loading...</div>
            </div>
        </div>
    `;

    // Add cards to row
    row.appendChild(vehicleCard);
    row.appendChild(customerCard);
    row.appendChild(serviceCard);

    // Add row to tab
    detailsTab.innerHTML = '';
    detailsTab.appendChild(row);
}

    // Helper function to get status class based on status string
    function getStatusClass(status) {
    const statusLower = status.toLowerCase();
    if (statusLower === 'completed') {
    return 'completed';
} else if (statusLower === 'diagnosis' || statusLower === 'repair') {
    return 'in-progress';
} else {
    return 'new';
}
}

    // Load current bill data
    function loadCurrentBill(billData) {
    if (!billData) {
    console.warn('No bill data provided');
    return;
}

    try {
    // Clear existing data
    window.inventoryItems = [];
    window.laborCharges = [];

    // Load materials with proper error handling
    if (billData.materials && Array.isArray(billData.materials)) {
    billData.materials.forEach(item => {
    if (item && item.itemId) {
    window.inventoryItems.push({
    key: item.itemId,
    name: item.name || `Item ${item.itemId}`,
    price: parseFloat(item.unitPrice) || 0,
    quantity: parseInt(item.quantity) || 1
});
}
});
}

    // Load labor charges with proper error handling
    if (billData.laborCharges && Array.isArray(billData.laborCharges)) {
    billData.laborCharges.forEach(charge => {
    if (charge) {
    window.laborCharges.push({
    description: charge.description || 'Labor charge',
    hours: parseFloat(charge.hours) || 0,
    rate: parseFloat(charge.ratePerHour) || 0
});
}
});
}

    // Render items and charges
    renderInventoryItems();
    renderLaborCharges();

    // Update bill summary with safe parsing
    const formatCurrency = (value) => {
    const num = parseFloat(value) || 0;
    return `₹${num.toFixed(2)}`;
};

    document.getElementById('partsSubtotal').textContent = formatCurrency(billData.partsSubtotal);
    document.getElementById('laborSubtotal').textContent = formatCurrency(billData.laborSubtotal);
    document.getElementById('subtotalAmount').textContent = formatCurrency(billData.subtotal);
    document.getElementById('taxAmount').textContent = formatCurrency(billData.tax);
    document.getElementById('totalAmount').textContent = formatCurrency(billData.total);

    // Update service notes
    const serviceNotesTextarea = document.getElementById('serviceNotes');
    if (serviceNotesTextarea && billData.notes) {
    serviceNotesTextarea.value = billData.notes;
}
} catch (error) {
    console.error('Error loading bill data:', error);
    showNotification('Error loading bill information', 'error');
}
}

    // Fetch inventory items for dropdown with improved error handling
    function fetchInventoryItems() {
    // Get token from URL parameter or session storage
    const token = getAuthToken();

    // Set up headers with token
    const headers = createAuthHeaders();

    // Show loading state in dropdown
    const inventorySelect = document.getElementById('inventoryItemSelect');
    if (inventorySelect) {
    // Clear existing options
    while (inventorySelect.options.length > 1) {
    inventorySelect.remove(1);
}

    // Add loading option
    const loadingOption = document.createElement('option');
    loadingOption.disabled = true;
    loadingOption.textContent = 'Loading inventory items...';
    inventorySelect.appendChild(loadingOption);
    inventorySelect.selectedIndex = 1;
}

    console.log('Fetching inventory items from server...');

    // Use the correct endpoint that matches the backend
    return fetch('/serviceAdvisor/api/inventory-items', {
    method: 'GET',
    headers: headers
})
    .then(response => {
    if (!response.ok) {
    throw new Error(`HTTP error! Status: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    console.log('Successfully fetched inventory items:', data);
    populateInventoryDropdown(data);
    return data;
})
    .catch(error => {
    console.error('Error fetching inventory items:', error);
    showNotification('Error loading inventory items. Please try again.', 'error');

    // Clear loading state from dropdown
    if (inventorySelect) {
    // Remove loading option
    while (inventorySelect.options.length > 1) {
    inventorySelect.remove(1);
}

    // Add error option
    const errorOption = document.createElement('option');
    errorOption.disabled = true;
    errorOption.textContent = 'Error loading items. Please try again.';
    inventorySelect.appendChild(errorOption);
}

    // Attempt to retry once
    setTimeout(() => {
    retryFetchInventoryItems();
}, 3000);

    throw error;
});
}

    // Retry function for inventory items
    function retryFetchInventoryItems() {
    console.log('Retrying inventory item fetch...');
    const token = getAuthToken();
    const headers = createAuthHeaders();

    fetch('/serviceAdvisor/api/inventory-items', {
    method: 'GET',
    headers: headers
})
    .then(response => {
    if (!response.ok) {
    throw new Error(`HTTP error! Status: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    console.log('Successfully fetched inventory items on retry:', data);
    populateInventoryDropdown(data);
    showNotification('Inventory items loaded successfully', 'info');
})
    .catch(error => {
    console.error('Error retrying inventory items fetch:', error);
});
}

    // Populate inventory dropdown with improved display
    function populateInventoryDropdown(items) {
    const inventorySelect = document.getElementById('inventoryItemSelect');
    if (!inventorySelect) return;

    // Clear existing options except first one
    while (inventorySelect.options.length > 1) {
    inventorySelect.remove(1);
}

    // Reset inventory prices map
    window.inventoryPrices = {};
    window.inventoryData = {}; // Store full inventory data for validation later

    // Check if we have items
    if (!items || !Array.isArray(items) || items.length === 0) {
    const noItemsOption = document.createElement('option');
    noItemsOption.disabled = true;
    noItemsOption.textContent = 'No inventory items available';
    inventorySelect.appendChild(noItemsOption);
    return;
}

    // Add options
    items.forEach(item => {
    // Skip items with no stock
    if (!item.currentStock || parseFloat(item.currentStock) <= 0) {
    return;
}

    const option = document.createElement('option');
    option.value = item.itemId;

    // Format price with proper currency symbol and 2 decimal places
    const formattedPrice = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2
}).format(item.unitPrice);

    option.textContent = `${item.name} - ${formattedPrice} (${item.currentStock} in stock)`;
    inventorySelect.appendChild(option);

    // Store price in inventory prices map
    window.inventoryPrices[item.itemId] = parseFloat(item.unitPrice);

    // Store full item data for validation
    window.inventoryData[item.itemId] = {
    name: item.name,
    price: parseFloat(item.unitPrice),
    stock: parseFloat(item.currentStock),
    category: item.category || 'General'
};
});

    // Reset the select to the default option
    inventorySelect.selectedIndex = 0;

    console.log('Populated inventory dropdown with', items.length, 'items');
    console.log('Inventory data:', window.inventoryData);
}

    // Add inventory item with stock validation
    function addInventoryItem(itemId, quantity = 1) {
    // Check if item ID exists
    if (!itemId) {
    showNotification('Please select an inventory item', 'error');
    return;
}

    // Parse itemId to ensure it's treated as the correct type (number)
    const parsedItemId = Number(itemId);

    // Validate quantity
    if (!quantity || quantity <= 0) {
    showNotification('Quantity must be greater than zero', 'error');
    return;
}

    // Check if we have the item data
    if (!window.inventoryData || !window.inventoryData[parsedItemId]) {
    showNotification('Item data not found. Please refresh the page.', 'error');
    return;
}

    const itemData = window.inventoryData[parsedItemId];
    const itemName = itemData.name;
    const itemPrice = itemData.price;
    const itemStock = itemData.stock;

    // Check if item already exists in the inventory items array
    const existingItemIndex = window.inventoryItems.findIndex(item => Number(item.key) === parsedItemId);

    // Calculate the new total quantity including existing items
    let newTotalQuantity = quantity;
    if (existingItemIndex >= 0) {
    newTotalQuantity += window.inventoryItems[existingItemIndex].quantity;
}

    // Check if we have enough stock
    if (newTotalQuantity > itemStock) {
    showNotification(`Not enough stock. Only ${itemStock} available for ${itemName}`, 'error');
    return;
}

    if (existingItemIndex >= 0) {
    // Update quantity
    window.inventoryItems[existingItemIndex].quantity += quantity;
    showNotification(`Updated quantity for ${itemName}`, 'info');
} else {
    // Add new item
    window.inventoryItems.push({
    key: parsedItemId,
    name: itemName,
    price: itemPrice,
    quantity: quantity
});
    showNotification(`Added ${itemName} to service items`, 'info');
}

    // Reset form
    const inventorySelect = document.getElementById('inventoryItemSelect');
    const quantityInput = document.getElementById('itemQuantity');
    if (inventorySelect) inventorySelect.selectedIndex = 0;
    if (quantityInput) quantityInput.value = 1;

    renderInventoryItems();
    updateBillSummary();
}

    // Render inventory items with improved formatting
    function renderInventoryItems() {
    const inventoryItemsList = document.getElementById('inventoryItemsList');
    if (!inventoryItemsList) return;

    inventoryItemsList.innerHTML = '';

    if (window.inventoryItems.length === 0) {
    const emptyRow = document.createElement('tr');
    emptyRow.innerHTML = `
            <td colspan="5" style="text-align: center; padding: 20px;">
                No inventory items added yet.
            </td>
        `;
    inventoryItemsList.appendChild(emptyRow);
    return;
}

    // Number formatter for currency
    const formatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2
});

    window.inventoryItems.forEach((item, index) => {
    const row = document.createElement('tr');
    const total = item.price * item.quantity;

    row.innerHTML = `
            <td>${item.name}</td>
            <td>${formatter.format(item.price)}</td>
            <td>
                <div class="quantity-control">
                    <button class="quantity-btn" onclick="decrementInventoryQuantity(${index})">-</button>
                    <input type="number" class="quantity-input" value="${item.quantity}" min="1" data-index="${index}" onchange="updateInventoryQuantity(this)">
                    <button class="quantity-btn" onclick="incrementInventoryQuantity(${index})">+</button>
                </div>
            </td>
            <td>${formatter.format(total)}</td>
            <td style="text-align: center;">
                <button class="btn-remove" onclick="removeInventoryItem(${index})">
                    <i class="fas fa-times"></i>
                </button>
            </td>
        `;

    inventoryItemsList.appendChild(row);
});
}

    // Validate inventory quantities against current stock
    function validateInventoryQuantities() {
    let isValid = true;
    const errorMessages = [];

    // Ensure we have the inventory data
    if (!window.inventoryData) {
    console.warn('Inventory data not available for validation');
    return { isValid: false, message: 'Inventory data not available. Please refresh the page.' };
}

    // Check each inventory item
    window.inventoryItems.forEach(item => {
    const itemId = Number(item.key);
    const quantity = Number(item.quantity);

    // Skip if we don't have data for this item
    if (!window.inventoryData[itemId]) {
    console.warn(`No inventory data for item ${itemId} - ${item.name}`);
    return;
}

    const availableStock = window.inventoryData[itemId].stock;

    if (quantity > availableStock) {
    isValid = false;
    errorMessages.push(`Not enough stock for ${item.name}. Available: ${availableStock}, Requested: ${quantity}`);
}
});

    return {
    isValid,
    message: errorMessages.length > 0 ? errorMessages.join('\n') : ''
};
}

    // Increment inventory quantity with stock validation
    function incrementInventoryQuantity(index) {
    if (!window.inventoryItems[index]) return;

    const item = window.inventoryItems[index];
    const itemId = Number(item.key);

    // Validate against available stock
    if (window.inventoryData && window.inventoryData[itemId]) {
    const availableStock = window.inventoryData[itemId].stock;

    if (item.quantity >= availableStock) {
    showNotification(`Cannot add more. Only ${availableStock} available for ${item.name}`, 'error');
    return;
}
}

    window.inventoryItems[index].quantity++;
    renderInventoryItems();
    updateBillSummary();
}

    // Decrement inventory quantity
    function decrementInventoryQuantity(index) {
    if (window.inventoryItems[index].quantity > 1) {
    window.inventoryItems[index].quantity--;
    renderInventoryItems();
    updateBillSummary();
}
}

    // Update inventory quantity with validation
    function updateInventoryQuantity(input) {
    const index = parseInt(input.getAttribute('data-index'));
    const quantity = parseInt(input.value) || 1;

    if (!window.inventoryItems[index]) return;

    const item = window.inventoryItems[index];
    const itemId = Number(item.key);

    // Validate against available stock
    if (window.inventoryData && window.inventoryData[itemId]) {
    const availableStock = window.inventoryData[itemId].stock;

    if (quantity > availableStock) {
    showNotification(`Cannot set quantity to ${quantity}. Only ${availableStock} available for ${item.name}`, 'error');
    input.value = item.quantity; // Reset to previous value
    return;
}
}

    if (quantity > 0) {
    window.inventoryItems[index].quantity = quantity;
    renderInventoryItems();
    updateBillSummary();
}
}

    // Remove inventory item
    function removeInventoryItem(index) {
    window.inventoryItems.splice(index, 1);
    renderInventoryItems();
    updateBillSummary();
}

    // Add labor charge
    // Improved version of addLaborCharge function
    function addLaborCharge(description, hours, rate) {
    // Convert inputs to proper numbers to avoid issues
    hours = parseFloat(hours) || 0;
    rate = parseFloat(rate) || 0;

    // Validate inputs
    if (hours <= 0 || rate <= 0) {
    console.warn('Invalid labor charge values:', hours, rate);
    showNotification('Hours and rate must be greater than zero', 'error');
    return;
}

    // Add to array with proper data types
    window.laborCharges.push({
    description: description || "Labor Charge",
    hours: hours,
    rate: rate
});

    console.log('Added labor charge:', {
    description: description || "Labor Charge",
    hours: hours,
    rate: rate
});

    renderLaborCharges();
    updateBillSummary();
}

    // Render labor charges
    function renderLaborCharges() {
    const laborChargesList = document.getElementById('laborChargesList');
    if (!laborChargesList) return;

    laborChargesList.innerHTML = '';

    if (window.laborCharges.length === 0) {
    laborChargesList.innerHTML = '<div style="padding: 10px 0; color: var(--gray);">No labor charges added yet.</div>';
    return;
}

    // Number formatter for currency
    const formatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2
});

    window.laborCharges.forEach((charge, index) => {
    const laborItem = document.createElement('div');
    laborItem.className = 'labor-item';

    const total = charge.hours * charge.rate;

    laborItem.innerHTML = `
            <div class="labor-details">
                <div class="labor-title">Labor Charge</div>
                <div class="labor-subtitle">${charge.hours} hours @ ${formatter.format(charge.rate)}/hr</div>
            </div>
            <div class="labor-price">${formatter.format(total)}</div>
            <div class="labor-actions">
                <button class="btn-remove" onclick="removeLaborCharge(${index})">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;

    laborChargesList.appendChild(laborItem);
});
}

    // Remove labor charge
    function removeLaborCharge(index) {
    window.laborCharges.splice(index, 1);
    renderLaborCharges();
    updateBillSummary();
}

    // Update bill summary
    function updateBillSummary() {
    // Calculate parts subtotal
    let partsSubtotal = 0;
    window.inventoryItems.forEach(item => {
    partsSubtotal += item.price * item.quantity;
});

    // Calculate labor subtotal
    let laborSubtotal = 0;
    window.laborCharges.forEach(charge => {
    laborSubtotal += charge.hours * charge.rate;
});

    // Calculate totals
    const subtotal = partsSubtotal + laborSubtotal;
    const tax = subtotal * 0.07; // 7% tax
    const total = subtotal + tax;

    // Number formatter for currency
    const formatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2
});

    // Update the display
    document.getElementById('partsSubtotal').textContent = formatter.format(partsSubtotal);
    document.getElementById('laborSubtotal').textContent = formatter.format(laborSubtotal);
    document.getElementById('subtotalAmount').textContent = formatter.format(subtotal);
    document.getElementById('taxAmount').textContent = formatter.format(tax);
    document.getElementById('totalAmount').textContent = formatter.format(total);
}

    // Update bill preview
    function updateBillPreview() {
    // Update the invoice items table
    const invoiceItemsList = document.getElementById('invoiceItemsList');
    if (!invoiceItemsList) {
    console.error('Invoice items list element not found');
    return;
}

    invoiceItemsList.innerHTML = '';

    // Number formatter for currency
    const formatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2
});

    // Add inventory items
    window.inventoryItems.forEach(item => {
    const row = document.createElement('tr');
    const total = item.price * item.quantity;

    row.innerHTML = `
        <td>${item.name} (Parts)</td>
        <td>${item.quantity}</td>
        <td>${formatter.format(item.price)}</td>
        <td>${formatter.format(total)}</td>
    `;

    invoiceItemsList.appendChild(row);
});

    // Add labor charges
    window.laborCharges.forEach(charge => {
    const row = document.createElement('tr');
    const total = charge.hours * charge.rate;

    row.innerHTML = `
        <td>Labor Charge</td>
        <td>${charge.hours} hrs</td>
        <td>${formatter.format(charge.rate)}/hr</td>
        <td>${formatter.format(total)}</td>
    `;

    invoiceItemsList.appendChild(row);
});

    // If no items, show message
    if (window.inventoryItems.length === 0 && window.laborCharges.length === 0) {
    const row = document.createElement('tr');
    row.innerHTML = `
        <td colspan="4" style="text-align: center; padding: 20px;">
            No service items added yet.
        </td>
    `;
    invoiceItemsList.appendChild(row);
}

    // Calculate totals
    let subtotal = 0;
    window.inventoryItems.forEach(item => {
    subtotal += item.price * item.quantity;
});

    window.laborCharges.forEach(charge => {
    subtotal += charge.hours * charge.rate;
});

    const tax = subtotal * 0.07;
    const total = subtotal + tax;

    // Update the invoice totals using the correct element IDs
    const invoiceSubtotal = document.getElementById('invoiceSubtotal');
    const invoiceTax = document.getElementById('invoiceTax');
    const invoiceTotal = document.getElementById('invoiceTotal');

    if (invoiceSubtotal) invoiceSubtotal.textContent = formatter.format(subtotal);
    if (invoiceTax) invoiceTax.textContent = formatter.format(tax);
    if (invoiceTotal) invoiceTotal.textContent = formatter.format(total);

    // Update invoice information fields
    updateInvoiceInfoFields();
}

    // Update invoice information fields
    // Update invoice information fields
    function updateInvoiceInfoFields() {
    // Get customer name from the vehicle summary
    const customerName = document.querySelector('.vehicle-summary .vehicle-info-summary p')?.textContent?.replace('Customer: ', '') || 'Unknown Customer';

    // Get customer email and phone from the details tab
    const customerEmailElement = document.querySelector('.detail-card:nth-of-type(2) .detail-row:nth-child(2) .detail-value');
    const customerPhoneElement = document.querySelector('.detail-card:nth-of-type(2) .detail-row:nth-child(3) .detail-value');

    const customerEmail = customerEmailElement?.textContent || 'Not available';
    const customerPhone = customerPhoneElement?.textContent || 'Not available';

    // Get vehicle info and properly extract registration number
    const vehicleInfoElement = document.querySelector('.vehicle-summary .vehicle-info-summary h4');
    let vehicleModel = 'Unknown Vehicle';
    let registrationNumber = 'Unknown';

    if (vehicleInfoElement) {
    const vehicleText = vehicleInfoElement.textContent;
    // Extract parts - format is typically "Brand Model (Registration)"
    const regMatch = vehicleText.match(/\(([^)]+)\)/);
    if (regMatch && regMatch[1]) {
    registrationNumber = regMatch[1];
    // Get the vehicle model by removing the registration part
    vehicleModel = vehicleText.replace(/\s*\([^)]+\)/, '').trim();
} else {
    vehicleModel = vehicleText;
}
}

    // Alternatively, try to get registration from the vehicle details card
    if (registrationNumber === 'Unknown') {
    const regElement = document.querySelector('.detail-card:nth-of-type(1) .detail-row:nth-child(2) .detail-value');
    if (regElement) {
    registrationNumber = regElement.textContent.trim();
}
}

    // Get today's date formatted
    const today = new Date();
    const formattedDate = today.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
});

    // Generate invoice number if not exists
    if (!window.currentInvoiceNumber) {
    window.currentInvoiceNumber = 'INV-' + today.getFullYear() + '-' + String(Math.floor(Math.random() * 10000)).padStart(4, '0');
}

    // Update customer information in the invoice
    const customerNameElement = document.querySelector('.invoice-customer .invoice-detail:nth-child(2)');
    if (customerNameElement) {
    customerNameElement.innerHTML = `<span>Name:</span> ${customerName}`;
}

    // Update customer email
    const customerEmailInvoiceElement = document.querySelector('.invoice-customer .invoice-detail:nth-child(3)');
    if (customerEmailInvoiceElement) {
    customerEmailInvoiceElement.innerHTML = `<span>Email:</span> ${customerEmail}`;
}

    // Update customer phone
    const customerPhoneInvoiceElement = document.querySelector('.invoice-customer .invoice-detail:nth-child(4)');
    if (customerPhoneInvoiceElement) {
    customerPhoneInvoiceElement.innerHTML = `<span>Phone:</span> ${customerPhone}`;
}

    // Update vehicle information in the invoice
    const vehicleInfoInvoiceElement = document.querySelector('.invoice-service .invoice-detail:nth-child(2)');
    if (vehicleInfoInvoiceElement) {
    vehicleInfoInvoiceElement.innerHTML = `<span>Vehicle:</span> ${vehicleModel}`;
}

    // Update registration number separately
    const regInvoiceElement = document.querySelector('.invoice-service .invoice-detail:nth-child(3)');
    if (regInvoiceElement) {
    regInvoiceElement.innerHTML = `<span>Registration:</span> ${registrationNumber}`;
}

    // Update invoice date
    const invoiceDateElement = document.querySelector('.invoice-service .invoice-detail:nth-child(4)');
    if (invoiceDateElement) {
    invoiceDateElement.innerHTML = `<span>Invoice Date:</span> ${formattedDate}`;
}

    // Update invoice number
    const invoiceNumberElement = document.querySelector('.invoice-service .invoice-detail:nth-child(5)');
    if (invoiceNumberElement) {
    invoiceNumberElement.innerHTML = `<span>Invoice #:</span> ${window.currentInvoiceNumber}`;
}
}

    // Initialize status update events
    function initializeStatusEvents() {
    const statusSelect = document.getElementById('statusSelect');
    if (statusSelect) {
    statusSelect.addEventListener('change', function() {
    updateStatusPreview(this.value);
});
}
}

    // Update status preview
    function updateStatusPreview(status) {
    const currentStatusBadge = document.getElementById('currentStatusBadge');
    if (currentStatusBadge) {
    // Remove all status classes
    currentStatusBadge.classList.remove('new', 'in-progress', 'completed', 'diagnosis', 'pending-parts', 'repair', 'quality-check');

    // Add the selected status class
    let statusClass = 'new';
    if (status === 'Diagnosis' || status === 'Repair') {
    statusClass = 'in-progress';
} else if (status === 'Completed') {
    statusClass = 'completed';
}

    currentStatusBadge.classList.add(statusClass);

    // Update the status text
    let statusText = status.replace(/-/g, ' ');
    statusText = statusText.charAt(0).toUpperCase() + statusText.slice(1);
    currentStatusBadge.innerHTML = `<i class="fas fa-circle"></i> ${statusText}`;
}
}

    function updateServiceStatus() {
    const statusSelect = document.getElementById('statusSelect');

    if (!statusSelect) {
    console.error('Status select element not found');
    return;
}

    const status = statusSelect.value;

    // Show a saving indicator
    showNotification('Updating status...', 'info');

    // Disable save button to prevent double submission
    const saveButton = document.getElementById('saveServiceItemsBtn');
    if (saveButton) saveButton.disabled = true;

    // Get token and headers
    const token = getAuthToken();
    const headers = createAuthHeaders();

    // Prepare simplified data for API call - explicitly set notifyCustomer to false
    const statusData = {
    status: status,
    notes: document.getElementById('serviceNotes')?.value || "",
    notifyCustomer: false // Ensure this is always false
};

    console.log(`Updating service ${window.currentRequestId} status to ${status}`);

    // Send to server using the correct endpoint
    return fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/status`, {
    method: 'PUT',
    headers: headers,
    body: JSON.stringify(statusData)
})
    .then(response => {
    if (!response.ok) {
    throw new Error(`Failed to update status: ${response.status}`);
}
    return response.json();
})
    .then(data => {
    console.log('Status update response:', data);

    // Show success notification
    showNotification(`Service status updated to ${status}!`);

    // Re-enable save button
    if (saveButton) saveButton.disabled = false;

    // Update the status in the main table and in all status badges
    updateStatusInTable(window.currentRequestId, status);
    updateAllStatusBadges(status);

    return data;
})
    .catch(error => {
    console.error('Error updating service status:', error);
    showNotification('Error updating status: ' + error.message, 'error');

    // Re-enable save button
    if (saveButton) saveButton.disabled = false;

    throw error;
});
}

    // Update all status badges in the modal
    function updateAllStatusBadges(status) {
    let statusClass = getStatusClass(status);

    // Update current status badge
    const currentStatusBadge = document.getElementById('currentStatusBadge');
    if (currentStatusBadge) {
    // Remove all status classes
    currentStatusBadge.classList.remove('new', 'in-progress', 'completed');
    currentStatusBadge.classList.add(statusClass);
    currentStatusBadge.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
}

    // Update all status displays
    const statusDisplays = document.querySelectorAll('.status-display .status-badge');
    statusDisplays.forEach(badge => {
    badge.classList.remove('new', 'in-progress', 'completed');
    badge.classList.add(statusClass);
    badge.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
});

    // Update status in details tab
    const detailStatus = document.querySelector('.detail-card:nth-of-type(3) .detail-value:nth-of-type(3) .status-badge');
    if (detailStatus) {
    detailStatus.classList.remove('new', 'in-progress', 'completed');
    detailStatus.classList.add(statusClass);
    detailStatus.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
}
}

    // Update status in the table
    function updateStatusInTable(requestId, status) {
    const row = document.querySelector(`#vehiclesTableBody tr[data-id="${requestId}"]`);
    if (row) {
    const statusCell = row.querySelector('td:nth-child(5)');
    if (statusCell) {
    let statusClass = 'new';
    if (status === 'Diagnosis' || status === 'Repair') {
    statusClass = 'in-progress';
} else if (status === 'Completed') {
    statusClass = 'completed';
}

    statusCell.innerHTML = `
                <span class="status-badge ${statusClass}">
                    <i class="fas fa-circle"></i> ${status}
                </span>
            `;
}
}
}

    // Save service items with improved error handling and feedback
    // Add debugging to saveServiceItems function
    // Fixed version of saveServiceItems function
    function saveServiceItems() {
    // Only proceed if we have items to save
    if (window.inventoryItems.length === 0 && window.laborCharges.length === 0) {
    showNotification('No service items to save', 'info');
    return;
}

    // Debug output to see what we're saving
    console.log('Saving service items:');
    console.log('- Labor charges:', JSON.stringify(window.laborCharges));
    console.log('- Inventory items:', JSON.stringify(window.inventoryItems));

    // Show a saving indicator
    showNotification('Saving service items...', 'info');

    // Disable save button to prevent double submission
    const saveButton = document.getElementById('saveInvoiceBtn');
    if (saveButton) saveButton.disabled = true;

    // Get token and headers
    const token = getAuthToken();
    const headers = createAuthHeaders();

    // Track promises for inventory items and labor charges
    const promises = [];

    // Save labor charges if there are any
    if (window.laborCharges.length > 0) {
    console.log('Preparing to save labor charges');

    // Ensure each charge has the proper structure and convert to numbers
    const charges = window.laborCharges.map(charge => {
    return {
    description: charge.description || 'Labor Charge',
    hours: parseFloat(charge.hours) || 0,
    ratePerHour: parseFloat(charge.rate) || 0,
    total: parseFloat(charge.hours * charge.rate) || 0
};
});

    console.log('Formatted labor charges for API:', JSON.stringify(charges));

    // Add labor charges promise
    const laborPromise = fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/labor-charges`, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(charges)
})
    .then(response => {
    console.log('Labor charges response status:', response.status);
    if (!response.ok) {
    return response.text().then(text => {
    console.error('Labor charges error response:', text);
    throw new Error(`Failed to save labor charges: ${response.status} - ${text}`);
});
}
    return response.json();
})
    .then(data => {
    console.log('Labor charges saved successfully:', data);
    return data;
});

    promises.push(laborPromise);
}

    // Save inventory items if there are any
    if (window.inventoryItems.length > 0) {
    console.log('Preparing to save inventory items');

    // Format inventory items for API
    const items = window.inventoryItems.map(item => {
    return {
    itemId: Number(item.key),
    name: item.name,
    quantity: Number(item.quantity),
    unitPrice: Number(item.price)
};
});

    // Create service materials request
    const materialsRequest = {
    items: items,
    replaceExisting: true
};

    console.log('Sending materials request:', JSON.stringify(materialsRequest));

    // Add inventory items promise
    const inventoryPromise = fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/inventory-items`, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(materialsRequest)
})
    .then(response => {
    console.log('Inventory items response status:', response.status);
    if (!response.ok) {
    return response.text().then(text => {
    console.error('Inventory items error response:', text);
    throw new Error(`Failed to save inventory items: ${response.status} - ${text}`);
});
}
    return response.json();
})
    .then(data => {
    console.log('Inventory items saved successfully:', data);
    return data;
});

    promises.push(inventoryPromise);
}

    // Wait for all promises to resolve
    Promise.all(promises)
    .then(results => {
    console.log('Save results:', results);

    // Show success notification
    showNotification('Service items saved successfully!', 'success');

    // Re-enable save button
    if (saveButton) saveButton.disabled = false;

    // Refresh service details to ensure everything is in sync
    setTimeout(() => {
    openVehicleDetails(window.currentRequestId);
}, 1000);
})
    .catch(error => {
    console.error('Error saving service items:', error);
    showNotification('Error: ' + error.message, 'error');

    // Re-enable save button
    if (saveButton) saveButton.disabled = false;
});
}


    // Generate bill with improved error handling
    function generateBill() {
    // Get service notes
    const serviceNotes = document.getElementById('serviceNotes').value;

    // Validate inventory quantities
    const validation = validateInventoryQuantities();
    if (!validation.isValid) {
    showNotification(validation.message, 'error');
    return;
}

    // Show generating indicator
    showNotification('Generating bill...', 'info');

    // Disable buttons to prevent double submission
    const saveButton = document.getElementById('saveServiceItemsBtn');
    if (saveButton) saveButton.disabled = true;

    // Number formatter for currency
    const formatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

    // Prepare bill request data
    const billRequest = {
    materials: window.inventoryItems.map(item => {
    return {
    itemId: Number(item.key),
    name: item.name,
    quantity: Number(item.quantity),
    unitPrice: Number(item.price),
    total: Number(item.price * item.quantity)
};
}),
    laborCharges: window.laborCharges.map(charge => {
    return {
    description: charge.description || 'Labor Charge',
    hours: Number(charge.hours),
    ratePerHour: Number(charge.rate),
    total: Number(charge.hours * charge.rate)
};
}),
    materialsTotal: Number(window.inventoryItems.reduce((sum, item) => sum + (item.price * item.quantity), 0)),
    laborTotal: Number(window.laborCharges.reduce((sum, charge) => sum + (charge.hours * charge.rate), 0)),
    subtotal: Number(
    window.inventoryItems.reduce((sum, item) => sum + (item.price * item.quantity), 0) +
    window.laborCharges.reduce((sum, charge) => sum + (charge.hours * charge.rate), 0)
    ),
    notes: serviceNotes,
    sendEmail: true
};

    // Calculate tax and grand total
    billRequest.gst = Number(billRequest.subtotal * 0.07);
    billRequest.grandTotal = Number(billRequest.subtotal + billRequest.gst);

    console.log('Sending bill request:', billRequest);

    // Get token and headers
    const token = getAuthToken();
    const headers = createAuthHeaders();

    // Send to server
    fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/generate-bill`, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(billRequest)
})
    .then(response => {
    if (!response.ok) {
    return response.text().then(text => {
    throw new Error(`Failed to generate bill: ${response.status} - ${text}`);
});
}
    return response.json();
})
    .then(data => {
    console.log('Bill generation response:', data);

    // Show success notification
    showNotification('Bill generated successfully!');

    // If email was sent, show notification
    if (data.emailSent) {
    setTimeout(() => {
    showNotification('Bill email sent to customer');
}, 3000);
}

    // Re-enable buttons
    if (saveButton) saveButton.disabled = false;

    // Update bill preview with the latest data
    updateBillPreview();

    // Refresh inventory after successful bill generation
    fetchInventoryItems();
})
    .catch(error => {
    console.error('Error generating bill:', error);
    showNotification('Error generating bill: ' + error.message, 'error');

    // Re-enable buttons
    if (saveButton) saveButton.disabled = false;
});
}

    function markServiceComplete() {
    // Get the current request ID
    const requestId = window.currentRequestId;
    if (!requestId) {
    showNotification('No service selected', 'error');
    return;
}

    // Show loading notification
    showNotification('Marking service as completed...', 'info');

    // Get auth token
    const token = getAuthToken();
    const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
};

    // Simple request body - just change status to Completed
    const data = {
    status: "Completed",
    notes: "Service completed by " + document.querySelector('.user-info h3').textContent
};

    // Make direct API call to update status
    fetch(`/serviceAdvisor/api/service/${requestId}/status`, {
    method: 'PUT',
    headers: headers,
    body: JSON.stringify(data)
})
    .then(response => {
    if (!response.ok) {
    throw new Error(`Failed to update status: ${response.status}`);
}
    return response.json();
})
    .then(result => {
    console.log('Service completed successfully:', result);

    // Show success notification
    showNotification('Service marked as completed!', 'success');

    // Update UI to show completed status
    updateAllStatusBadges("Completed");

    // Close the modal after a short delay
    setTimeout(() => {
    document.getElementById('vehicleDetailsModal').classList.remove('show');

    // Refresh the dashboard to update the list
    fetchAssignedVehicles();
}, 1500);
})
    .catch(error => {
    console.error('Error completing service:', error);
    showNotification('Error: ' + error.message, 'error');
});
}

    // Filter vehicles by status
    function filterVehicles(filter) {
    const rows = document.querySelectorAll('#vehiclesTableBody tr');

    rows.forEach(row => {
    const statusBadge = row.querySelector('.status-badge');
    if (!statusBadge) {
    row.style.display = filter === 'all' ? '' : 'none';
    return;
}

    const status = statusBadge.textContent.trim().toLowerCase();

    if (filter === 'all') {
    row.style.display = '';
} else if (filter === 'new' && (status.includes('new') || status.includes('received'))) {
    row.style.display = '';
} else if (filter === 'in-progress' && (status.includes('diagnosis') || status.includes('repair') || status.includes('in progress'))) {
    row.style.display = '';
} else {
    row.style.display = 'none';
}
});

    // Update filter button text
    const filterButton = document.getElementById('filterButton');
    if (filterButton) {
    if (filter === 'all') {
    filterButton.innerHTML = '<i class="fas fa-filter"></i> All Vehicles <i class="fas fa-chevron-down" style="font-size: 0.8rem;"></i>';
} else if (filter === 'new') {
    filterButton.innerHTML = '<i class="fas fa-filter"></i> New Assignments <i class="fas fa-chevron-down" style="font-size: 0.8rem;"></i>';
} else if (filter === 'in-progress') {
    filterButton.innerHTML = '<i class="fas fa-filter"></i> In Progress <i class="fas fa-chevron-down" style="font-size: 0.8rem;"></i>';
}
}
}

    // Filter vehicles by search term
    function filterVehiclesBySearch(searchTerm) {
    const rows = document.querySelectorAll('#vehiclesTableBody tr');

    rows.forEach(row => {
    const textContent = row.textContent.toLowerCase();

    if (textContent.includes(searchTerm)) {
    row.style.display = '';
} else {
    row.style.display = 'none';
}
});
}

    // Show notification
    function showNotification(message, type = 'success') {
    const notification = document.getElementById('successNotification');

    // Update notification class based on type
    notification.className = 'notification'; // Reset classes
    notification.classList.add(type);

    document.getElementById('notificationMessage').textContent = message;
    notification.classList.add('show');

    setTimeout(() => {
    notification.classList.remove('show');
}, 3000);
}

    // Make functions available globally
    window.openVehicleDetails = openVehicleDetails;
    window.incrementInventoryQuantity = incrementInventoryQuantity;
    window.decrementInventoryQuantity = decrementInventoryQuantity;
    window.updateInventoryQuantity = updateInventoryQuantity;
    window.removeInventoryItem = removeInventoryItem;
    window.removeLaborCharge = removeLaborCharge;
    window.handleTabClick = handleTabClick;
    window.sendBillToAdmin = sendBillToAdmin;
    window.updateBillPreview = updateBillPreview;
    window.updateInvoiceInfoFields = updateInvoiceInfoFields;
