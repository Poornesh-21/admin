 // Global variables
    let dashboardStats = null;
    let serviceAdvisors = [];
    let currentPage = {
    due: 1,
    inService: 1,
    completed: 1
};
    const itemsPerPage = {
    due: 5,
    inService: 5,
    completed: 3
};

 $(document).ready(function() {
     // Mobile menu toggle
     $('#mobile-menu-toggle').click(function() {
         $('#sidebar').toggleClass('active');
         $(this).find('i').toggleClass('fa-bars fa-times');
     });

     // Close sidebar when clicking on a menu item on mobile
     $('.sidebar-menu-link').click(function() {
         if ($(window).width() < 992) {
             $('#sidebar').removeClass('active');
             $('#mobile-menu-toggle').find('i').removeClass('fa-times').addClass('fa-bars');
         }
     });

     // Handle window resize
     $(window).resize(function() {
         if ($(window).width() >= 992) {
             $('#sidebar').removeClass('active');
             $('#mobile-menu-toggle').find('i').removeClass('fa-times').addClass('fa-bars');
         }
     });
 });

    // Document ready function
    document.addEventListener('DOMContentLoaded', function() {
    // Initialize app
    initializeApp();

    // Load dashboard data
    loadDashboardData();

    // Load service advisors for assignment
    loadServiceAdvisors();

    // Set up event listeners
    setupEventListeners();
});

    // Initialize app
    function initializeApp() {
    // Setup mobile menu toggle
    setupMobileMenu();

    // Setup logout button
    setupLogout();

    // Set up token-based authentication
    setupAuthentication();

    // Set current date if element exists
    setupDateDisplay();

    // Initialize charts
    initializeCharts();
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

    // Consider adding token to current URL for refresh protection
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

    // Initialize charts
    function initializeCharts() {
    initializeServiceChart();
    initializeDistributionChart();
}

    function initializeServiceChart() {
    const serviceChartCtx = document.getElementById('serviceChart');
    if (!serviceChartCtx) return;

    const serviceChart = new Chart(serviceChartCtx.getContext('2d'), {
    type: 'line',
    data: {
    labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
    datasets: [
{
    label: 'Vehicles Due',
    data: [8, 10, 12, 15],
    borderColor: '#722F37',
    backgroundColor: 'rgba(114, 47, 55, 0.15)',
    tension: 0.4,
    fill: true,
    borderWidth: 2,
    pointBackgroundColor: '#722F37',
    pointBorderColor: '#fff',
    pointRadius: 5,
    pointHoverRadius: 7
},
{
    label: 'Completed Services',
    data: [5, 8, 7, 10],
    borderColor: '#8a3943',
    backgroundColor: 'rgba(138, 57, 67, 0.1)',
    tension: 0.4,
    fill: true,
    borderWidth: 2,
    pointBackgroundColor: '#8a3943',
    pointBorderColor: '#fff',
    pointRadius: 5,
    pointHoverRadius: 7
}
    ]
},
    options: {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
    legend: {
    position: 'top',
    labels: {
    font: {
    family: "'Baloo Bhaijaan 2', sans-serif",
    weight: 500
}
}
},
    tooltip: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    titleColor: '#722F37',
    bodyColor: '#495057',
    bodyFont: {
    family: "'Baloo Bhaijaan 2', sans-serif"
},
    titleFont: {
    family: "'Baloo Bhaijaan 2', sans-serif",
    weight: 600
},
    borderColor: 'rgba(114, 47, 55, 0.2)',
    borderWidth: 1,
    caretPadding: 10,
    caretSize: 8,
    cornerRadius: 8,
    displayColors: true,
    padding: 12
}
},
    scales: {
    y: {
    beginAtZero: true,
    grid: {
    drawBorder: false,
    color: 'rgba(0, 0, 0, 0.05)'
},
    ticks: {
    font: {
    family: "'Baloo Bhaijaan 2', sans-serif"
}
}
},
    x: {
    grid: {
    display: false
},
    ticks: {
    font: {
    family: "'Baloo Bhaijaan 2', sans-serif"
}
}
}
}
}
});

    // Add click event to chart filter buttons
    document.querySelectorAll('.chart-filter').forEach(button => {
    button.addEventListener('click', function() {
    const parent = this.closest('.chart-options');
    parent.querySelectorAll('.chart-filter').forEach(btn => {
    btn.classList.remove('active');
});
    this.classList.add('active');

    const period = this.textContent.trim().toLowerCase();
    updateChartData(this, period);
});
});
}

    function initializeDistributionChart() {
    const distributionChartCtx = document.getElementById('distributionChart');
    if (!distributionChartCtx) return;

    const distributionChart = new Chart(distributionChartCtx.getContext('2d'), {
    type: 'doughnut',
    data: {
    labels: ['Pending', 'In Progress', 'Completed'],
    datasets: [{
    data: [12, 8, 5],
    backgroundColor: [
    '#EFFFBB',
    '#722F37',
    '#66BB6A'
    ],
    borderColor: [
    '#d6e297',
    '#5e262e',
    '#388E3C'
    ],
    borderWidth: 1,
    hoverOffset: 4
}]
},
    options: {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '65%',
    plugins: {
    legend: {
    position: 'bottom',
    labels: {
    padding: 15,
    usePointStyle: true,
    pointStyle: 'circle',
    font: {
    family: "'Baloo Bhaijaan 2', sans-serif",
    weight: 500
}
}
},
    tooltip: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    titleColor: '#722F37',
    bodyColor: '#495057',
    bodyFont: {
    family: "'Baloo Bhaijaan 2', sans-serif"
},
    titleFont: {
    family: "'Baloo Bhaijaan 2', sans-serif",
    weight: 600
},
    borderColor: 'rgba(114, 47, 55, 0.2)',
    borderWidth: 1,
    caretPadding: 10,
    cornerRadius: 8,
    displayColors: true,
    padding: 12
}
}
}
});
}

    function updateChartData(buttonElement, period) {
    const chartContainer = buttonElement.closest('.chart-card').querySelector('.chart-container canvas');
    const chart = Chart.getChart(chartContainer.id);

    if (chartContainer.id === 'serviceChart') {
    let labels, data1, data2;

    if (period === 'week') {
    labels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    data1 = [2, 3, 4, 3, 5, 2, 1];
    data2 = [1, 2, 3, 2, 4, 1, 0];
} else if (period === 'month') {
    labels = ['Week 1', 'Week 2', 'Week 3', 'Week 4'];
    data1 = [8, 10, 12, 15];
    data2 = [5, 8, 7, 10];
} else if (period === 'quarter') {
    labels = ['Jan', 'Feb', 'Mar'];
    data1 = [30, 35, 40];
    data2 = [25, 30, 35];
} else if (period === 'year') {
    labels = ['Q1', 'Q2', 'Q3', 'Q4'];
    data1 = [100, 120, 110, 130];
    data2 = [90, 105, 95, 115];
}

    chart.data.labels = labels;
    chart.data.datasets[0].data = data1;
    chart.data.datasets[1].data = data2;
    chart.update();
} else if (chartContainer.id === 'distributionChart') {
    let data;

    if (period === 'month') {
    data = [12, 8, 5];
} else if (period === 'year') {
    data = [150, 95, 75];
}

    chart.data.datasets[0].data = data;
    chart.update();
}
}

    // Set up event listeners
    function setupEventListeners() {
    // Pagination for Due Table
    setupPagination('dueTable', 'dueTablePagination', 'due');

    // Pagination for Service Table
    setupPagination('serviceTable', 'serviceTablePagination', 'inService');

    // Pagination for Completed Services
    setupPagination('completedServicesGrid', 'completedServicesPagination', 'completed');

    // Search functionality
    setupSearch();

    // Modal functionality
    setupModalListeners();
}

    function setupPagination(tableId, paginationId, type) {
    const tableElement = document.getElementById(tableId);
    const paginationElement = document.getElementById(paginationId);

    if (!tableElement || !paginationElement) return;

    // Add click event to pagination buttons
    paginationElement.querySelectorAll('[data-page]').forEach(button => {
    button.addEventListener('click', function(e) {
    e.preventDefault();
    const page = parseInt(this.getAttribute('data-page'));
    changePage(page, type);
});
});

    // Add click event to previous button
    const prevBtn = document.getElementById(`${type}PrevBtn`);
    if (prevBtn) {
    prevBtn.addEventListener('click', function(e) {
    e.preventDefault();
    if (currentPage[type] > 1) {
    changePage(currentPage[type] - 1, type);
}
});
}

    // Add click event to next button
    const nextBtn = document.getElementById(`${type}NextBtn`);
    if (nextBtn) {
    nextBtn.addEventListener('click', function(e) {
    e.preventDefault();

    let totalItems = 0;
    if (type === 'due' && dashboardStats && dashboardStats.vehiclesDueList) {
    totalItems = dashboardStats.vehiclesDueList.length;
} else if (type === 'inService' && dashboardStats && dashboardStats.vehiclesInServiceList) {
    totalItems = dashboardStats.vehiclesInServiceList.length;
} else if (type === 'completed' && dashboardStats && dashboardStats.completedServicesList) {
    totalItems = dashboardStats.completedServicesList.length;
}

    const totalPages = Math.ceil(totalItems / itemsPerPage[type]);

    if (currentPage[type] < totalPages) {
    changePage(currentPage[type] + 1, type);
}
});
}
}

    function changePage(page, type) {
    currentPage[type] = page;

    // Update UI
    if (type === 'due') {
    updateDueTablePage();
} else if (type === 'inService') {
    updateServiceTablePage();
} else if (type === 'completed') {
    updateCompletedServicesPage();
}

    // Update pagination UI
    updatePaginationUI(type);
}

    function updateDueTablePage() {
    const tableRows = document.querySelectorAll('#dueTable tbody tr');
    const startIndex = (currentPage.due - 1) * itemsPerPage.due;
    const endIndex = startIndex + itemsPerPage.due;

    // Hide all rows
    tableRows.forEach((row, index) => {
    row.classList.remove('active-page');
});

    // Show rows for current page
    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
    tableRows[i].classList.add('active-page');
}
}

    function updateServiceTablePage() {
    const tableRows = document.querySelectorAll('#serviceTable tbody tr');
    const startIndex = (currentPage.inService - 1) * itemsPerPage.inService;
    const endIndex = startIndex + itemsPerPage.inService;

    // Hide all rows
    tableRows.forEach((row, index) => {
    row.classList.remove('active-page');
});

    // Show rows for current page
    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
    tableRows[i].classList.add('active-page');
}
}

    function updateCompletedServicesPage() {
    const cards = document.querySelectorAll('#completedServicesGrid .service-card');
    const startIndex = (currentPage.completed - 1) * itemsPerPage.completed;
    const endIndex = startIndex + itemsPerPage.completed;

    // Hide all cards
    cards.forEach((card, index) => {
    card.classList.remove('active-page');
});

    // Show cards for current page
    for (let i = startIndex; i < endIndex && i < cards.length; i++) {
    cards[i].classList.add('active-page');
}
}

    function updatePaginationUI(type) {
    let totalItems = 0;
    if (type === 'due' && dashboardStats && dashboardStats.vehiclesDueList) {
    totalItems = dashboardStats.vehiclesDueList.length;
} else if (type === 'inService' && dashboardStats && dashboardStats.vehiclesInServiceList) {
    totalItems = dashboardStats.vehiclesInServiceList.length;
} else if (type === 'completed' && dashboardStats && dashboardStats.completedServicesList) {
    totalItems = dashboardStats.completedServicesList.length;
}

    const totalPages = Math.ceil(totalItems / itemsPerPage[type]);
    const paginationElement = document.getElementById(`${type}TablePagination`) ||
    document.getElementById(`${type}ServicesPagination`);

    if (!paginationElement) return;

    // Update active page
    paginationElement.querySelectorAll('[data-page]').forEach(button => {
    button.classList.toggle('active', parseInt(button.getAttribute('data-page')) === currentPage[type]);
});

    // Update prev/next buttons
    const prevBtn = document.getElementById(`${type}PrevBtn`);
    const nextBtn = document.getElementById(`${type}NextBtn`);

    if (prevBtn) {
    prevBtn.classList.toggle('disabled', currentPage[type] === 1);
}

    if (nextBtn) {
    nextBtn.classList.toggle('disabled', currentPage[type] === totalPages || totalPages === 0);
}
}

    function setupSearch() {
    // Due table search
    const dueTableSearch = document.querySelector('#dueTable .search-input');
    if (dueTableSearch) {
    dueTableSearch.addEventListener('keyup', function() {
    filterTable('dueTable', this.value);
});
}

    // Service table search
    const serviceTableSearch = document.querySelector('#serviceTable .search-input');
    if (serviceTableSearch) {
    serviceTableSearch.addEventListener('keyup', function() {
    filterTable('serviceTable', this.value);
});
}

    // Completed services search
    const completedServicesSearch = document.getElementById('completedServiceSearch');
    if (completedServicesSearch) {
    completedServicesSearch.addEventListener('keyup', function() {
    filterCompletedServices(this.value);
});
}
}

    function filterTable(tableId, searchTerm) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr:not([id])');

    searchTerm = searchTerm.toLowerCase();

    if (!searchTerm) {
    // Reset to current page view
    if (tableId === 'dueTable') {
    updateDueTablePage();
} else if (tableId === 'serviceTable') {
    updateServiceTablePage();
}
    return;
}

    // Hide all rows first
    rows.forEach(row => {
    row.classList.remove('active-page');
});

    // Show rows that match search term
    rows.forEach(row => {
    const text = row.textContent.toLowerCase();
    if (text.includes(searchTerm)) {
    row.classList.add('active-page');
}
});
}

    function filterCompletedServices(searchTerm) {
    const cards = document.querySelectorAll('#completedServicesGrid .service-card');

    searchTerm = searchTerm.toLowerCase();

    if (!searchTerm) {
    // Reset to current page view
    updateCompletedServicesPage();
    return;
}

    // Hide all cards first
    cards.forEach(card => {
    card.classList.remove('active-page');
});

    // Show cards that match search term
    cards.forEach(card => {
    const text = card.textContent.toLowerCase();
    if (text.includes(searchTerm)) {
    card.classList.add('active-page');
}
});
}

    function setupModalListeners() {
    // Confirm assign button click
    const confirmAssignBtn = document.getElementById('confirmAssignBtn');
    if (confirmAssignBtn) {
    confirmAssignBtn.addEventListener('click', assignServiceAdvisor);
}

    // Advisor card selection
    document.addEventListener('click', function(e) {
    if (e.target.closest('.advisor-card')) {
    const card = e.target.closest('.advisor-card');
    document.querySelectorAll('.advisor-card').forEach(c => {
    c.classList.remove('selected');
});
    card.classList.add('selected');
}
});
}

    // API Functions
    function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

    function showSpinner() {
    // Create spinner overlay if it doesn't exist
    let spinnerOverlay = document.getElementById('spinnerOverlay');
    if (!spinnerOverlay) {
    spinnerOverlay = document.createElement('div');
    spinnerOverlay.id = 'spinnerOverlay';
    spinnerOverlay.className = 'position-fixed top-0 start-0 w-100 h-100 d-flex justify-content-center align-items-center bg-dark bg-opacity-25';
    spinnerOverlay.style.zIndex = '9999';
    spinnerOverlay.innerHTML = `
            <div class="spinner-border text-wine" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        `;
    document.body.appendChild(spinnerOverlay);
} else {
    spinnerOverlay.classList.add('show');
}
}

    function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
    spinnerOverlay.classList.remove('show');
    // Remove element after animation completes
    setTimeout(() => {
    if (spinnerOverlay.parentNode) {
    spinnerOverlay.parentNode.removeChild(spinnerOverlay);
}
}, 300);
}
}

    function loadDashboardData() {
    showSpinner();

    // Use the correct API endpoint
    fetch('/admin/dashboard/api/data', {
    method: 'GET',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + getToken()
}
})
    .then(response => {
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}
    return response.json();
})
    .then(data => {
    hideSpinner();

    dashboardStats = data;
    console.log("Dashboard data loaded:", dashboardStats);

    // Update stats
    updateDashboardStats();

    // Update tables
    renderDueTable();
    renderServiceTable();
    renderCompletedServices();

    // Update charts with real data
    updateChartsWithRealData();
})
    .catch(error => {
    hideSpinner();
    console.error('Error loading dashboard data:', error);
    showToast('Failed to load dashboard data. Please try again.', 'error');
});
}

    function loadServiceAdvisors() {
    fetch('/admin/service-advisors/api/advisors', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (response.status === 401) {
                window.location.href = '/admin/login?error=session_expired';
                throw new Error('Session expired');
            }
            return response.json();
        })
        .then(data => {
            serviceAdvisors = data;
            console.log("Service advisors loaded:", serviceAdvisors.length);
        })
        .catch(error => {
            console.error('Error loading service advisors:', error);
        });
}

    function updateDashboardStats() {
    if (!dashboardStats) return;

    // Update stats cards
    const vehiclesDueElement = document.querySelector('.stat-card:nth-child(1) .stat-value');
    const inProgressElement = document.querySelector('.stat-card:nth-child(2) .stat-value');
    const completedElement = document.querySelector('.stat-card:nth-child(3) .stat-value');
    const revenueElement = document.querySelector('.stat-card:nth-child(4) .stat-value');

    if (vehiclesDueElement) {
    vehiclesDueElement.textContent = dashboardStats.vehiclesDue || 0;
}

    if (inProgressElement) {
    inProgressElement.textContent = dashboardStats.vehiclesInProgress || 0;
}

    if (completedElement) {
    completedElement.textContent = dashboardStats.vehiclesCompleted || 0;
}

    if (revenueElement) {
    revenueElement.textContent = '₹' + (dashboardStats.totalRevenue || 0);
}
}

    function renderDueTable() {
    const tableBody = document.querySelector('#dueTable tbody');
    if (!tableBody || !dashboardStats || !dashboardStats.vehiclesDueList) return;

    // Clear existing rows
    const existingRows = tableBody.querySelectorAll('tr:not([id])');
    existingRows.forEach(row => row.remove());

    // Hide loading row
    const loadingRow = document.getElementById('loading-row');
    if (loadingRow) {
    loadingRow.style.display = 'none';
}

    if (dashboardStats.vehiclesDueList.length === 0) {
    // Show empty row
    const emptyRow = document.getElementById('empty-row');
    if (emptyRow) {
    emptyRow.style.display = 'table-row';
} else {
    // Create empty row if it doesn't exist
    const row = document.createElement('tr');
    row.id = 'empty-row';
    row.innerHTML = `
                <td colspan="5" class="text-center py-4">
                    <div class="no-data-message">
                        <i class="fas fa-car fa-3x mb-3 text-muted"></i>
                        <h5>No vehicles due for service</h5>
                        <p class="text-muted">All vehicles are currently serviced or no pending service requests.</p>
                    </div>
                </td>
            `;
    tableBody.appendChild(row);
}
    return;
}

    // Add rows for vehicles due
    dashboardStats.vehiclesDueList.forEach((vehicle, index) => {
    const isActivePage = index < itemsPerPage.due;

    const row = document.createElement('tr');
    row.className = isActivePage ? 'active-page' : '';
    row.innerHTML = `
            <td>
                <div class="vehicle-info">
                    <div class="vehicle-icon">
                        <i class="fas fa-${vehicle.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                    </div>
                    <div class="vehicle-details">
                        <h5>${vehicle.vehicleName}</h5>
                        <p>Reg: ${vehicle.registrationNumber}</p>
                    </div>
                </div>
            </td>
            <td>
                <div class="person-info">
                    <div class="person-details">
                        <h5>${vehicle.customerName}</h5>
                        <p>${vehicle.customerEmail || ''}</p>
                    </div>
                    <div class="membership-badge membership-${(vehicle.membershipStatus || 'Standard').toLowerCase()}">
                        <i class="fas fa-${(vehicle.membershipStatus === 'Premium') ? 'crown' : 'user'}"></i>
                        ${vehicle.membershipStatus || 'Standard'}
                    </div>
                </div>
            </td>
            <td>
                <span class="status-badge status-pending">
                    <i class="fas fa-clock"></i>
                    <span>${vehicle.status}</span>
                </span>
            </td>
            <td>${formatDate(vehicle.dueDate)}</td>
            <td class="table-actions-cell">
                <button class="btn-assign"
                        onclick="showAssignAdvisorModal(${vehicle.requestId})">
                    <i class="fas fa-user-plus"></i>
                    Assign
                </button>
            </td>
        `;

    tableBody.appendChild(row);
});

    // Update pagination
    updatePaginationUI('due');
}

    function renderServiceTable() {
    const tableBody = document.querySelector('#serviceTable tbody');
    if (!tableBody || !dashboardStats || !dashboardStats.vehiclesInServiceList) return;

    // Clear existing rows
    const existingRows = tableBody.querySelectorAll('tr:not([id])');
    existingRows.forEach(row => row.remove());

    if (dashboardStats.vehiclesInServiceList.length === 0) {
    // Show empty row if it exists
    const emptyRow = tableBody.querySelector('.empty-row');
    if (emptyRow) {
    emptyRow.style.display = 'table-row';
} else {
    // Create empty row if it doesn't exist
    const row = document.createElement('tr');
    row.className = 'empty-row';
    row.innerHTML = `
                <td colspan="6" class="text-center py-4">
                    <div class="no-data-message">
                        <i class="fas fa-wrench fa-3x mb-3 text-muted"></i>
                        <h5>No vehicles currently in service</h5>
                        <p class="text-muted">There are no vehicles currently being serviced.</p>
                    </div>
                </td>
            `;
    tableBody.appendChild(row);
}
    return;
}

    // Add rows for vehicles in service
    dashboardStats.vehiclesInServiceList.forEach((vehicle, index) => {
    const isActivePage = index < itemsPerPage.inService;

    const row = document.createElement('tr');
    row.className = isActivePage ? 'active-page' : '';
    row.innerHTML = `
            <td>
                <div class="vehicle-info">
                    <div class="vehicle-icon">
                        <i class="fas fa-${vehicle.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                    </div>
                    <div class="vehicle-details">
                        <h5>${vehicle.vehicleName}</h5>
                        <p>Reg: ${vehicle.registrationNumber}</p>
                    </div>
                </div>
            </td>
            <td>
                <div class="person-info">
                    <div class="person-details">
                        <h5>${vehicle.serviceAdvisorName}</h5>
                        <p>ID: ${vehicle.serviceAdvisorId}</p>
                    </div>
                </div>
            </td>
            <td>
                <span class="status-badge status-${vehicle.status.toLowerCase() === 'diagnosis' ? 'progress' : 'progress'}">
                    <i class="fas fa-${vehicle.status.toLowerCase() === 'diagnosis' ? 'stethoscope' : 'wrench'}"></i>
                    <span>${vehicle.status}</span>
                </span>
            </td>
            <td>${formatDate(vehicle.startDate)}</td>
            <td>${formatDate(vehicle.estimatedCompletionDate)}</td>
            <td class="table-actions-cell">
                <button class="btn-premium sm primary"
                        onclick="showServiceRequestDetails(${vehicle.requestId})">
                    <i class="fas fa-eye"></i>
                    Details
                </button>
            </td>
        `;

    tableBody.appendChild(row);
});

    // Update pagination
    updatePaginationUI('inService');
}

    function renderCompletedServices() {
    const container = document.getElementById('completedServicesGrid');
    if (!container || !dashboardStats || !dashboardStats.completedServicesList) return;

    // Clear existing cards
    container.innerHTML = '';

    if (dashboardStats.completedServicesList.length === 0) {
    // Show empty state
    container.innerHTML = `
            <div class="text-center py-5">
                <i class="fas fa-check-circle fa-3x text-muted mb-3"></i>
                <h5>No completed services found</h5>
                <p class="text-muted">There are no completed service requests available.</p>
            </div>
        `;
    return;
}

    // Add cards for completed services
    dashboardStats.completedServicesList.forEach((service, index) => {
    const isActivePage = index < itemsPerPage.completed;

    const card = document.createElement('div');
    card.className = `service-card${isActivePage ? ' active-page' : ''}`;
    card.dataset.page = Math.ceil((index + 1) / itemsPerPage.completed);

    card.innerHTML = `
            <div class="service-card-header">
                <h4 class="service-card-title">
                    <i class="fas fa-car-side"></i>
                    <span>${service.vehicleName}</span>
                </h4>
                <div class="service-status">
                    <div class="status-indicator completed"></div>
                    <div class="status-text completed">Completed</div>
                </div>
            </div>
            <div class="service-card-body">
                <div class="service-meta">
                    <div class="service-meta-item">
                        <div class="service-meta-label">Registration</div>
                        <div class="service-meta-value">${service.registrationNumber}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Completed Date</div>
                        <div class="service-meta-value">${formatDate(service.completedDate)}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Customer</div>
                        <div class="service-meta-value">${service.customerName}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Service Advisor</div>
                        <div class="service-meta-value">${service.serviceAdvisorName}</div>
                    </div>
                </div>
                <div class="price">Total Cost: ₹${service.totalCost.toLocaleString('en-IN', { maximumFractionDigits: 2 })}</div>
            </div>
            <div class="service-card-footer">
                <button class="btn-premium sm secondary view-service-details-btn"
                        data-service-id="${service.serviceId}"
                        onclick="showServiceRequestDetails(${service.serviceId})">
                    <i class="fas fa-eye"></i>
                    View Details
                </button>
                <button class="btn-premium sm primary generate-invoice-btn"
                        data-service-id="${service.serviceId}"
                        data-has-invoice="${service.hasInvoice}"
                        ${service.hasInvoice ? 'onclick="downloadInvoice(' + service.serviceId + ')"' : 'onclick="generateInvoice(' + service.serviceId + ')"'}>
                    <i class="fas fa-file-invoice"></i>
                    ${service.hasInvoice ? 'Download Invoice' : 'Generate Invoice'}
                </button>
            </div>
        `;

    container.appendChild(card);
});

    // Update pagination
    updatePaginationUI('completed');
}

    function updateChartsWithRealData() {
    if (!dashboardStats) return;

    // Update service chart with real data if available
    const serviceChart = Chart.getChart('serviceChart');
    if (serviceChart) {
    // Use real data or keep sample data
    const dueCount = dashboardStats.vehiclesDue || 0;
    const inProgressCount = dashboardStats.vehiclesInProgress || 0;
    const completedCount = dashboardStats.vehiclesCompleted || 0;

    // Generate some trend data based on real counts
    const dueData = [
    Math.max(0, dueCount - Math.floor(Math.random() * 3)),
    Math.max(0, dueCount - Math.floor(Math.random() * 2)),
    dueCount,
    Math.max(0, dueCount + Math.floor(Math.random() * 3))
    ];

    const completedData = [
    Math.max(0, completedCount - Math.floor(Math.random() * 3)),
    Math.max(0, completedCount - Math.floor(Math.random() * 2)),
    completedCount,
    Math.max(0, completedCount + Math.floor(Math.random() * 2))
    ];

    serviceChart.data.datasets[0].data = dueData;
    serviceChart.data.datasets[1].data = completedData;
    serviceChart.update();
}

    // Update distribution chart with real data
    const distributionChart = Chart.getChart('distributionChart');
    if (distributionChart) {
    const dueCount = dashboardStats.vehiclesDue || 0;
    const inProgressCount = dashboardStats.vehiclesInProgress || 0;
    const completedCount = dashboardStats.vehiclesCompleted || 0;

    distributionChart.data.datasets[0].data = [dueCount, inProgressCount, completedCount];
    distributionChart.update();
}
}

    // Function to display and populate the assign modal with real advisors
    function showAssignAdvisorModal(requestId) {
    // Find the request in vehiclesDueList
    const request = dashboardStats.vehiclesDueList.find(req => req.requestId === requestId);

    if (!request) {
    console.error('Request not found:', requestId);
    showToast('Error: Request not found', 'error');
    return;
}

    // Create the modal if it doesn't exist
    let modal = document.getElementById('assignAdvisorModal');

    if (!modal) {
    const modalHtml = `
        <div class="modal fade premium-modal" id="assignAdvisorModal" tabindex="-1" aria-labelledby="assignAdvisorModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="assignAdvisorModalLabel">
                            <i class="fas fa-user-plus"></i>
                            Assign Service Advisor
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form>
                        <div class="modal-body">
                            <div class="mb-4 p-3 bg-light rounded-3">
                                <div class="d-flex align-items-center gap-3">
                                    <div class="vehicle-icon">
                                        <i class="fas fa-car"></i>
                                    </div>
                                    <div>
                                        <h4 class="mb-1" id="assignVehicleName"></h4>
                                        <div class="d-flex flex-wrap gap-3">
                                            <p class="mb-0"><strong>Registration:</strong> <span id="assignVehicleReg"></span></p>
                                            <p class="mb-0"><strong>Customer:</strong> <span id="assignCustomerName"></span></p>
                                            <p class="mb-0"><strong>Request ID:</strong> <span id="assignRequestId"></span></p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <input type="hidden" id="selectedRequestId" value="">

                            <div class="modal-section">
                                <div class="modal-section-title">
                                    <i class="fas fa-user-tie"></i> Available Service Advisors
                                </div>

                                <div id="advisorsList">
                                    <!-- Service advisors will be populated here -->
                                    <div class="text-center py-3">
                                        <div class="spinner-border text-wine" role="status">
                                            <span class="visually-hidden">Loading advisors...</span>
                                        </div>
                                        <p class="mt-2">Loading service advisors...</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn-premium secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="button" class="btn-premium primary" id="confirmAssignBtn">
                                <i class="fas fa-check"></i>
                                Confirm Assignment
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>`;

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    modal = document.getElementById('assignAdvisorModal');

    // Add event listener for the confirm button
    document.getElementById('confirmAssignBtn').addEventListener('click', assignServiceAdvisor);
}

    // Set request details in the modal
    document.getElementById('assignVehicleName').textContent = request.vehicleName;
    document.getElementById('assignVehicleReg').textContent = request.registrationNumber;
    document.getElementById('assignCustomerName').textContent = request.customerName;
    document.getElementById('assignRequestId').textContent = `REQ-${request.requestId}`;
    document.getElementById('selectedRequestId').value = request.requestId;

    // Populate service advisors
    populateServiceAdvisorsList();

    // Show modal
    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

    // Function to populate the list of service advisors from API data
    function populateServiceAdvisorsList() {
    const advisorsList = document.getElementById('advisorsList');
    if (!advisorsList) return;

    if (!serviceAdvisors || serviceAdvisors.length === 0) {
    advisorsList.innerHTML = `
        <div class="alert alert-info">
            <i class="fas fa-info-circle me-2"></i>
            No service advisors available. Please add service advisors first.
        </div>
        `;
    return;
}

    let html = '';

    serviceAdvisors.forEach(advisor => {
    // Calculate workload percentage
    const workloadPercentage = advisor.workloadPercentage ||
    (advisor.activeServices ? Math.min(Math.round((advisor.activeServices / 10) * 100), 100) : 0);

    // Determine workload class
    let workloadClass = 'bg-success';
    if (workloadPercentage > 75) {
    workloadClass = 'bg-danger';
} else if (workloadPercentage > 50) {
    workloadClass = 'bg-warning';
}

    // Get advisor name from firstName and lastName or fall back to name property
    const name = advisor.firstName && advisor.lastName ?
    `${advisor.firstName} ${advisor.lastName}` :
    advisor.name || 'Unnamed Advisor';

    // Get advisor initials for the avatar
    const initials = (advisor.firstName ? advisor.firstName.charAt(0) : '') +
    (advisor.lastName ? advisor.lastName.charAt(0) : '');

    html += `
        <div class="advisor-card" data-advisor-id="${advisor.advisorId || advisor.id}">
            <div class="advisor-avatar">${initials || 'SA'}</div>
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

    // Add click event handlers for advisor selection
    document.querySelectorAll('.advisor-card').forEach(card => {
    card.addEventListener('click', function() {
    document.querySelectorAll('.advisor-card').forEach(c => {
    c.classList.remove('selected');
});
    this.classList.add('selected');
});
});
}

    // Function to assign a service advisor
    function assignServiceAdvisor() {
    const requestId = document.getElementById('selectedRequestId').value;
    const selectedAdvisor = document.querySelector('.advisor-card.selected');

    if (!selectedAdvisor) {
    showToast('Please select a service advisor', 'error');
    return;
}

    const advisorId = selectedAdvisor.getAttribute('data-advisor-id');

    showSpinner();

    fetch(`/admin/dashboard/api/assign/${requestId}?advisorId=${advisorId}`, {
    method: 'PUT',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + getToken()
}
})
    .then(response => {
    if (response.status === 401) {
    window.location.href = '/admin/login?error=session_expired';
    throw new Error('Session expired');
}

    if (!response.ok) {
    throw new Error('Failed to assign service advisor');
}
    return response.json();
})
    .then(data => {
    hideSpinner();

    // Close modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('assignAdvisorModal'));
    if (modal) {
    modal.hide();
}

    // Show success message
    showToast('Service advisor assigned successfully!', 'success');

    // Refresh the page to update the dashboard
    setTimeout(() => {
    window.location.reload();
}, 2000);
})
    .catch(error => {
    hideSpinner();
    console.error('Error assigning service advisor:', error);
    showToast('Failed to assign service advisor. Please try again.', 'error');
});
}

    function showServiceRequestDetails(requestId) {
    // Find the request in one of the lists (due, in-service, completed)
    let request;

    // Check in vehiclesDueList
    if (dashboardStats && dashboardStats.vehiclesDueList) {
    request = dashboardStats.vehiclesDueList.find(req => req.requestId === requestId);
}

    // If not found, check in vehiclesInServiceList
    if (!request && dashboardStats && dashboardStats.vehiclesInServiceList) {
    request = dashboardStats.vehiclesInServiceList.find(req => req.requestId === requestId);
}

    // If not found, check in completedServicesList
    if (!request && dashboardStats && dashboardStats.completedServicesList) {
    request = dashboardStats.completedServicesList.find(service => service.serviceId === requestId);
}

    if (!request) {
    console.error('Request not found:', requestId);
    showToast('Error: Request not found', 'error');
    return;
}

    console.log("Request details for modal:", request);

    // Create modal if it doesn't exist
    let modal = document.getElementById('viewServiceRequestModal');
    if (!modal) {
    const modalHtml = `
        <div class="modal fade premium-modal" id="viewServiceRequestModal" tabindex="-1" aria-labelledby="viewServiceRequestModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="viewServiceRequestModalLabel">
                            <i class="fas fa-clipboard-list"></i>
                            Service Request Details
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row mb-4">
                            <div class="col-md-6">
                                <div class="card h-100">
                                    <div class="card-header">
                                        <h6 class="card-title mb-0">Request Information</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Request ID</label>
                                            <div id="viewRequestId" class="fw-bold"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Status</label>
                                            <div id="viewStatus"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Created Date</label>
                                            <div id="viewCreatedDate"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="card h-100">
                                    <div class="card-header">
                                        <h6 class="card-title mb-0">Vehicle Information</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Vehicle Name</label>
                                            <div id="viewVehicleName" class="fw-bold"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Registration Number</label>
                                            <div id="viewRegistrationNumber"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Category</label>
                                            <div id="viewVehicleCategory"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="row mb-4">
                            <div class="col-md-6">
                                <div class="card h-100">
                                    <div class="card-header">
                                        <h6 class="card-title mb-0">Customer Information</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Customer Name</label>
                                            <div id="viewCustomerName" class="fw-bold"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Contact</label>
                                            <div id="viewCustomerContact"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Membership</label>
                                            <div id="viewMembership"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="card h-100">
                                    <div class="card-header">
                                        <h6 class="card-title mb-0">Service Information</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Service Type</label>
                                            <div id="viewServiceType" class="fw-bold"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Delivery Date</label>
                                            <div id="viewDeliveryDate"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Service Advisor</label>
                                            <div id="viewServiceAdvisor"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="card mb-4">
                            <div class="card-header">
                                <h6 class="card-title mb-0">Service Description</h6>
                            </div>
                            <div class="card-body">
                                <p id="viewDescription"></p>
                            </div>
                        </div>
                        <div class="card mb-4" id="materialsSection" style="display: none;">
                            <div class="card-header">
                                <h6 class="card-title mb-0">Materials Used</h6>
                            </div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-striped">
                                        <thead>
                                            <tr>
                                                <th>Item</th>
                                                <th>Quantity</th>
                                                <th>Unit Price</th>
                                                <th>Total</th>
                                            </tr>
                                        </thead>
                                        <tbody id="materialsTableBody">
                                            <!-- Materials will be populated here -->
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn-premium secondary" data-bs-dismiss="modal">Close</button>
                    </div>
                </div>
            </div>
        </div>`;

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    modal = document.getElementById('viewServiceRequestModal');
}

    // Set request details in the modal
    document.getElementById('viewRequestId').textContent = `REQ-${requestId}`;

    // Set status with appropriate badge
    const statusElement = document.getElementById('viewStatus');
    const status = request.status || 'Received';
    statusElement.innerHTML = `
        <span class="status-badge status-${status.toLowerCase() === 'completed' ? 'completed' :
    status.toLowerCase() === 'diagnosis' || status.toLowerCase() === 'repair' ? 'progress' : 'pending'}">
            <i class="fas fa-${
    status === 'Received' ? 'clock' :
    status === 'Diagnosis' ? 'stethoscope' :
    status === 'Repair' ? 'wrench' :
    'check-circle'
}"></i>
            ${status}
        </span>
    `;

    // Created Date
    const createdDateElement = document.getElementById('viewCreatedDate');
    if (createdDateElement) {
    createdDateElement.textContent = request.createdAt ? formatDate(request.createdAt) :
    request.startDate ? formatDate(request.startDate) : 'N/A';
}

    // Vehicle Name
    const vehicleNameElement = document.getElementById('viewVehicleName');
    if (vehicleNameElement) {
    vehicleNameElement.textContent = request.vehicleName ||
    (request.vehicleBrand && request.vehicleModel ?
    request.vehicleBrand + ' ' + request.vehicleModel : 'N/A');
}

    // Registration Number
    const registrationNumberElement = document.getElementById('viewRegistrationNumber');
    if (registrationNumberElement) {
    registrationNumberElement.textContent = request.registrationNumber || 'N/A';
}

    // Vehicle Category
    const vehicleCategoryElement = document.getElementById('viewVehicleCategory');
    if (vehicleCategoryElement) {
    vehicleCategoryElement.textContent = request.category || 'Car';
}

    // Customer Name
    const customerNameElement = document.getElementById('viewCustomerName');
    if (customerNameElement) {
    customerNameElement.textContent = request.customerName || 'N/A';
}

    // Customer Contact
    const customerContactElement = document.getElementById('viewCustomerContact');
    if (customerContactElement) {
    customerContactElement.textContent = request.customerEmail || 'N/A';
}

    // Membership
    const membershipElement = document.getElementById('viewMembership');
    if (membershipElement) {
    const membershipStatus = request.membershipStatus || 'Standard';
    membershipElement.innerHTML = `
            <span class="membership-badge membership-${membershipStatus.toLowerCase()}">
                <i class="fas fa-${membershipStatus === 'Premium' ? 'crown' : 'user'}"></i>
                ${membershipStatus}
            </span>
        `;
}

    // Service Type
    const serviceTypeElement = document.getElementById('viewServiceType');
    if (serviceTypeElement) {
    serviceTypeElement.textContent = request.serviceType || 'N/A';
}

    // FIX 1: Delivery/Due Date
    const deliveryDateElement = document.getElementById('viewDeliveryDate');
    if (deliveryDateElement) {
    // Try to find the due date in different possible property names
    const dueDate = request.dueDate ||
    request.deliveryDate ||
    request.estimatedCompletionDate;

    deliveryDateElement.textContent = dueDate ? formatDate(dueDate) : 'N/A';

    // Add a label to explain what the date means based on status
    const label = deliveryDateElement.parentElement.querySelector('.form-label');
    if (label) {
    if (request.status === 'Received') {
    label.textContent = 'Due Date';
} else if (request.status === 'Completed') {
    label.textContent = 'Completed Date';
} else {
    label.textContent = 'Expected Delivery Date';
}
}
}

    // FIX 2: Service Advisor
    const serviceAdvisorElement = document.getElementById('viewServiceAdvisor');
    if (serviceAdvisorElement) {
    // Check multiple possible property names where service advisor might be stored
    const advisorName = request.serviceAdvisorName ||
    request.advisorName ||
    (request.serviceAdvisor ? request.serviceAdvisor.name : null);

    // If we have an advisor ID, add it to the display
    const advisorId = request.serviceAdvisorId ||
    request.advisorId ||
    (request.serviceAdvisor ? request.serviceAdvisor.id : null);

    if (advisorName) {
    if (advisorId) {
    serviceAdvisorElement.textContent = `${advisorName} (ID: ${advisorId})`;
} else {
    serviceAdvisorElement.textContent = advisorName;
}
} else {
    serviceAdvisorElement.textContent = 'Not Assigned';
}

    // Add a class to highlight if not assigned
    if (serviceAdvisorElement.textContent === 'Not Assigned') {
    serviceAdvisorElement.classList.add('text-warning');
} else {
    serviceAdvisorElement.classList.remove('text-warning');
}
}

    // Service Description
    const descriptionElement = document.getElementById('viewDescription');
    if (descriptionElement) {
    descriptionElement.textContent = request.additionalDescription ||
    request.description ||
    'No additional description provided.';
}

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
    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

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

    function generateInvoice(serviceId) {
    console.log(`Generating invoice for service ID: ${serviceId}`);
    showToast('Invoice generation initiated. Please wait...', 'info');

    // In a real application, you would make an API call to generate the invoice
    // For now, simulate a successful response after a delay
    setTimeout(() => {
    showToast('Invoice generated successfully!', 'success');

    // Update the button to download
    const button = document.querySelector(`.generate-invoice-btn[data-service-id="${serviceId}"]`);
    if (button) {
    button.innerHTML = '<i class="fas fa-file-invoice"></i> Download Invoice';
    button.setAttribute('onclick', `downloadInvoice(${serviceId})`);
    button.setAttribute('data-has-invoice', 'true');
}
}, 2000);
}

    function downloadInvoice(serviceId) {
    console.log(`Downloading invoice for service ID: ${serviceId}`);
    showToast('Invoice download initiated. Please wait...', 'info');

    // In a real application, you would make an API call to download the invoice
    // For now, simulate a successful response after a delay
    setTimeout(() => {
    showToast('Invoice downloaded successfully!', 'success');
}, 1000);
}

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
                <strong class="me-auto">
                    <i class="fas fa-${type === 'success' ? 'check-circle text-success' :
    type === 'error' ? 'exclamation-circle text-danger' :
    'info-circle text-info'} me-2"></i>
                    ${type.charAt(0).toUpperCase() + type.slice(1)}
                </strong>
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

    function formatDate(dateString) {
    if (!dateString) return 'N/A';

    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    return new Date(dateString).toLocaleDateString('en-US', options);
}

    // Function to select an advisor
    function selectAdvisor(card) {
    document.querySelectorAll('.advisor-card').forEach(c => {
        c.classList.remove('selected');
    });
    card.classList.add('selected');
}

    // Make functions available globally for onclick handlers
    window.showAssignAdvisorModal = showAssignAdvisorModal;
    window.assignServiceAdvisor = assignServiceAdvisor;
    window.showServiceRequestDetails = showServiceRequestDetails;
    window.generateInvoice = generateInvoice;
    window.downloadInvoice = downloadInvoice;
    window.selectAdvisor = selectAdvisor;