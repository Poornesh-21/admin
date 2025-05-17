
let serviceAdvisors = [];
let currentPage = 1;
const itemsPerPage = 8; // Changed from 10 to 8 for grid layout
let currentFilter = 'all';

// Wait for DOM to be loaded
document.addEventListener('DOMContentLoaded', function() {
    // Initialize app
    initializeApp();

    // Set up event listeners
    setupEventListeners();

    // Load service advisors
    loadServiceAdvisors();

    function initializeApp() {
        // Setup mobile menu toggle
        setupMobileMenu();

        // Setup logout button
        setupLogout();

        // Set up token-based authentication
        setupAuthentication();

        // Set up navigation and active menu
        setupNavigation();
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
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', function() {
                // Clear storage
                localStorage.removeItem("jwt-token");
                sessionStorage.removeItem("jwt-token");
                localStorage.removeItem("user-role");
                localStorage.removeItem("user-name");
                sessionStorage.removeItem("user-role");
                sessionStorage.removeItem("user-name");

                // Redirect to logout
                window.location.href = '/admin/logout';
            });
        }
    }

    function setupAuthentication() {
        // Get token from storage
        const tokenFromStorage = localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");

        if (tokenFromStorage) {
            console.log("Token found in storage");
        } else {
            console.warn("No token found in storage");
            // If no token is found, redirect to login
            window.location.href = '/admin/login?error=session_expired';
        }
    }

    function setupNavigation() {
        // Get the current path
        const currentPath = window.location.pathname;

        // Set active class on sidebar menu items based on current path
        document.querySelectorAll('.sidebar-menu-link').forEach(link => {
            const href = link.getAttribute('href');
            if (href && currentPath.includes(href.split('?')[0])) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    function setupEventListeners() {
        // Modal event listeners for clearing validation errors
        const addAdvisorModalEl = document.getElementById('addAdvisorModal');
        if (addAdvisorModalEl) {
            addAdvisorModalEl.addEventListener('hidden.bs.modal', function() {
                clearErrorMessage('addAdvisorForm');

                // Remove is-invalid class from all inputs in the form
                const form = document.getElementById('addAdvisorForm');
                if (form) {
                    form.querySelectorAll('.is-invalid').forEach(input => {
                        input.classList.remove('is-invalid');
                    });

                    // Hide all error messages
                    form.querySelectorAll('.error-message').forEach(error => {
                        error.classList.remove('show');
                        error.textContent = '';
                    });
                }
            });
        }

        const editAdvisorModalEl = document.getElementById('editAdvisorModal');
        if (editAdvisorModalEl) {
            editAdvisorModalEl.addEventListener('hidden.bs.modal', function() {
                clearErrorMessage('editAdvisorForm');

                // Remove is-invalid class from all inputs in the form
                const form = document.getElementById('editAdvisorForm');
                if (form) {
                    form.querySelectorAll('.is-invalid').forEach(input => {
                        input.classList.remove('is-invalid');
                    });

                    // Hide all error messages
                    form.querySelectorAll('.error-message').forEach(error => {
                        error.classList.remove('show');
                        error.textContent = '';
                    });
                }
            });
        }

        // Add service advisor button
        const addAdvisorBtn = document.getElementById('addAdvisorBtn');
        if (addAdvisorBtn) {
            addAdvisorBtn.addEventListener('click', function() {
                // Reset form
                const form = document.getElementById('addAdvisorForm');
                form.reset();

                // Clear any previous error messages
                clearErrorMessage('addAdvisorForm');

                // Remove is-invalid class from all inputs
                form.querySelectorAll('.is-invalid').forEach(input => {
                    input.classList.remove('is-invalid');
                });

                // Hide all error messages
                form.querySelectorAll('.error-message').forEach(error => {
                    error.classList.remove('show');
                    error.textContent = '';
                });

                // Show add advisor modal
                const addAdvisorModal = new bootstrap.Modal(document.getElementById('addAdvisorModal'));
                addAdvisorModal.show();
            });
        }

        // Save service advisor button
        const saveAdvisorBtn = document.getElementById('saveAdvisorBtn');
        if (saveAdvisorBtn) {
            saveAdvisorBtn.addEventListener('click', saveServiceAdvisor);
        }

        // Edit service advisor button from details
        const editAdvisorFromDetailsBtn = document.getElementById('editAdvisorFromDetailsBtn');
        if (editAdvisorFromDetailsBtn) {
            editAdvisorFromDetailsBtn.addEventListener('click', function() {
                const advisorId = this.getAttribute('data-advisor-id');
                showEditModal(advisorId);
            });
        }

        // Update service advisor button
        const updateAdvisorBtn = document.getElementById('updateAdvisorBtn');
        if (updateAdvisorBtn) {
            updateAdvisorBtn.addEventListener('click', updateServiceAdvisor);
        }

        // Reset password button
        const resetPasswordBtn = document.getElementById('resetPasswordBtn');
        if (resetPasswordBtn) {
            resetPasswordBtn.addEventListener('click', function() {
                const generatedPassword = generateRandomPassword();
                document.getElementById('editPassword').value = generatedPassword;

                // Show success message that password will be sent by email
                showConfirmation(
                    "Password Reset",
                    "A new temporary password has been generated and will be sent to the service advisor's email."
                );
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

        // Search functionality
        const searchInput = document.getElementById('advisorSearch');
        if (searchInput) {
            searchInput.addEventListener('keyup', function() {
                filterServiceAdvisors(this.value);
            });
        }

        // Filter pills functionality
        const filterPills = document.querySelectorAll('.filter-pill');
        filterPills.forEach(pill => {
            pill.addEventListener('click', function() {
                // Remove active class from all pills
                filterPills.forEach(p => p.classList.remove('active'));

                // Add active class to clicked pill
                this.classList.add('active');

                // Set current filter
                currentFilter = this.textContent.trim().toLowerCase().replace(' ', '-');

                // Reset to first page
                currentPage = 1;

                // Render with new filter
                renderServiceAdvisors();
            });
        });

        // Pagination
        const prevBtn = document.getElementById('prevBtn');
        if (prevBtn) {
            prevBtn.addEventListener('click', function(e) {
                e.preventDefault();
                if (currentPage > 1) {
                    currentPage--;
                    renderServiceAdvisors();
                    updatePaginationUI();
                }
            });
        }

        const nextBtn = document.getElementById('nextBtn');
        if (nextBtn) {
            nextBtn.addEventListener('click', function(e) {
                e.preventDefault();
                const filteredAdvisors = getFilteredAdvisors();
                const totalPages = Math.ceil(filteredAdvisors.length / itemsPerPage);
                if (currentPage < totalPages) {
                    currentPage++;
                    renderServiceAdvisors();
                    updatePaginationUI();
                }
            });
        }

        // Sort dropdown functionality
        const sortDropdownItems = document.querySelectorAll('.dropdown-menu .dropdown-item');
        sortDropdownItems.forEach(item => {
            item.addEventListener('click', function(e) {
                e.preventDefault();

                // Remove active class from all items
                sortDropdownItems.forEach(i => i.classList.remove('active'));

                // Add active class to clicked item
                this.classList.add('active');

                // Update dropdown button text
                document.getElementById('sortDropdown').innerHTML = `
                  <i class="fas fa-sort"></i>
                  Sort by: ${this.textContent}
              `;

                // Sort service advisors
                sortServiceAdvisors(this.textContent.toLowerCase());
            });
        });
    }
});

// Move all helper functions and other functions that use serviceAdvisors outside the document ready handler,
// but make sure they can access the now-global serviceAdvisors variable

function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

function showSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        spinnerOverlay.classList.add('show');
    }
}

function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        setTimeout(() => {
            spinnerOverlay.classList.remove('show');
        }, 500); // Minimum show time for spinner
    }
}

function showConfirmation(title, message) {
    document.getElementById('confirmationTitle').textContent = title;
    document.getElementById('confirmationMessage').textContent = message;
    const successModal = new bootstrap.Modal(document.getElementById('successModal'));
    successModal.show();
}

function showErrorMessage(formId, message) {
    // Create error container if it doesn't exist
    let errorContainer = document.getElementById(`${formId}-error-container`);

    if (!errorContainer) {
        errorContainer = document.createElement('div');
        errorContainer.id = `${formId}-error-container`;
        errorContainer.className = 'alert alert-danger mt-3';
        errorContainer.role = 'alert';

        const form = document.getElementById(formId);
        if (form) {
            form.parentNode.insertBefore(errorContainer, form.nextSibling);
        }
    }

    errorContainer.textContent = message;
    errorContainer.style.display = 'block';
}

function clearErrorMessage(formId) {
    const errorContainer = document.getElementById(`${formId}-error-container`);
    if (errorContainer) {
        errorContainer.style.display = 'none';
    }
}

function loadServiceAdvisors() {
    showSpinner();

    // Call API to get service advisors
    fetch('/admin/service-advisors/api/advisors', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                    // Redirect to login on auth failure
                    window.location.href = '/admin/login?error=session_expired';
                    throw new Error('Session expired');
                }
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            // Store service advisors globally
            serviceAdvisors = data;

            // Render service advisors
            renderServiceAdvisors();

            // Setup pagination
            setupPagination();
        })
        .catch(error => {
            hideSpinner();
            console.error('Error fetching advisor details:', error);
            alert('Failed to load advisor details. Please try again.');
        });
}

function getFilteredAdvisors() {
    const searchTerm = document.getElementById('advisorSearch').value.toLowerCase();

    // First filter by search term
    let filtered = serviceAdvisors.filter(advisor => {
        // Search in name, email, ID, and department
        return (
            (advisor.firstName + ' ' + advisor.lastName).toLowerCase().includes(searchTerm) ||
            advisor.email.toLowerCase().includes(searchTerm) ||
            (advisor.formattedId || '').toLowerCase().includes(searchTerm) ||
            (advisor.department || '').toLowerCase().includes(searchTerm)
        );
    });

    // Then filter by selected filter pill
    if (currentFilter !== 'all-advisors') {
        if (currentFilter === 'high-workload') {
            filtered = filtered.filter(advisor => advisor.workloadPercentage >= 75);
        } else if (currentFilter === 'available') {
            filtered = filtered.filter(advisor => advisor.workloadPercentage < 50);
        }
    }

    return filtered;
}

function renderServiceAdvisors() {
    // Get filtered advisors based on current filter and search term
    const filteredAdvisors = getFilteredAdvisors();

    // Calculate pagination
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const paginatedAdvisors = filteredAdvisors.slice(startIndex, endIndex);

    // Get the grid container
    const gridContainer = document.getElementById('advisorsGrid');

    // Clear grid
    gridContainer.innerHTML = '';

    // Check if there are service advisors
    if (paginatedAdvisors.length === 0) {
        gridContainer.innerHTML = `
      <div class="no-advisors-message">
        <i class="fas fa-user-tie fa-3x mb-3 text-muted"></i>
        <h4>No service advisors found</h4>
        <p class="text-muted">Try adjusting your filters or add a new service advisor</p>
      </div>
    `;
        return;
    }

    // Populate grid with service advisor cards
    paginatedAdvisors.forEach(advisor => {
        // Determine workload class
        const workloadClass = getWorkloadClass(advisor.workloadPercentage);

        // Create advisor card
        const card = document.createElement('div');
        card.className = 'advisor-card';
        card.addEventListener('click', function() {
            showAdvisorDetails(advisor);
        });

        // Create card contents
        card.innerHTML = `
      <div class="advisor-card-header">
        <div class="advisor-avatar">${getInitials(advisor.firstName, advisor.lastName)}</div>
        <div class="advisor-header-info">
          <div class="advisor-name">${advisor.firstName} ${advisor.lastName}</div>
          <div class="advisor-id">${advisor.formattedId || ('SA-' + advisor.advisorId)}</div>
        </div>
      </div>
      <div class="advisor-card-body">
        <div class="advisor-detail">
          <div class="advisor-detail-icon">
            <i class="fas fa-envelope"></i>
          </div>
          <div class="advisor-detail-content">
            <div class="advisor-detail-label">Email</div>
            <div class="advisor-detail-value">${advisor.email}</div>
          </div>
        </div>
        <div class="advisor-detail">
          <div class="advisor-detail-icon">
            <i class="fas fa-phone"></i>
          </div>
          <div class="advisor-detail-content">
            <div class="advisor-detail-label">Phone</div>
            <div class="advisor-detail-value">${advisor.phoneNumber}</div>
          </div>
        </div>
        <div class="advisor-detail">
          <div class="advisor-detail-icon">
            <i class="fas fa-building"></i>
          </div>
          <div class="advisor-detail-content">
            <div class="advisor-detail-label">Department</div>
            <div class="advisor-detail-value">${advisor.department || 'Not assigned'}</div>
          </div>
        </div>
      </div>
      <div class="advisor-card-footer">
        <div class="workload-container">
          <div class="workload-progress ${workloadClass}" style="width: ${advisor.workloadPercentage}%;"></div>
        </div>
        <div class="workload-text">
          <span>Workload</span>
          <span class="workload-label">${advisor.workloadPercentage}% (${advisor.activeServices} active)</span>
        </div>
      </div>
    `;

        // Add card to grid
        gridContainer.appendChild(card);
    });

    // Update pagination
    updatePaginationUI();
}

function updatePaginationUI() {
    const filteredAdvisors = getFilteredAdvisors();
    const totalPages = Math.ceil(filteredAdvisors.length / itemsPerPage);

    // Get pagination container
    const pagination = document.getElementById('pagination');

    // Clear pagination
    pagination.innerHTML = '';

    // Add previous button
    const prevBtn = document.createElement('li');
    prevBtn.className = 'page-item ' + (currentPage === 1 ? 'disabled' : '');
    prevBtn.innerHTML = `
  <a class="page-link" href="#" aria-label="Previous">
      <i class="fas fa-chevron-left"></i>
  </a>
  `;
    prevBtn.addEventListener('click', function(e) {
        e.preventDefault();
        if (currentPage > 1) {
            currentPage--;
            renderServiceAdvisors();
        }
    });
    pagination.appendChild(prevBtn);

    // Add page numbers
    for (let i = 1; i <= totalPages; i++) {
        const pageItem = document.createElement('li');
        pageItem.className = 'page-item ' + (i === currentPage ? 'active' : '');
        pageItem.innerHTML = `<a class="page-link" href="#">${i}</a>`;
        pageItem.addEventListener('click', function(e) {
            e.preventDefault();
            currentPage = i;
            renderServiceAdvisors();
        });
        pagination.appendChild(pageItem);
    }

    // Add next button
    const nextBtn = document.createElement('li');
    nextBtn.className = 'page-item ' + (currentPage === totalPages || totalPages === 0 ? 'disabled' : '');
    nextBtn.innerHTML = `
  <a class="page-link" href="#" aria-label="Next">
      <i class="fas fa-chevron-right"></i>
  </a>
  `;
    nextBtn.addEventListener('click', function(e) {
        e.preventDefault();
        if (currentPage < totalPages) {
            currentPage++;
            renderServiceAdvisors();
        }
    });
    pagination.appendChild(nextBtn);
}

function setupPagination() {
    const filteredAdvisors = getFilteredAdvisors();
    const totalPages = Math.ceil(filteredAdvisors.length / itemsPerPage);

    // Update pagination UI
    updatePaginationUI();
}

function getInitials(firstName, lastName) {
    const firstInitial = firstName ? firstName.charAt(0).toUpperCase() : '';
    const lastInitial = lastName ? lastName.charAt(0).toUpperCase() : '';
    return firstInitial + lastInitial;
}

function getWorkloadClass(percentage) {
    if (percentage >= 75) {
        return 'high';
    } else if (percentage >= 50) {
        return 'medium';
    } else {
        return 'low';
    }
}

function getStatusIcon(status) {
    switch (status.toLowerCase()) {
        case 'received':
            return 'clipboard-check';
        case 'diagnosis':
            return 'stethoscope';
        case 'repair':
            return 'tools';
        case 'completed':
            return 'check-circle';
        default:
            return 'circle';
    }
}


function getWorkloadText(percentage) {
    if (percentage >= 75) {
        return 'High workload';
    } else if (percentage >= 50) {
        return 'Moderate workload';
    } else {
        return 'Available for new services';
    }
}

function getActiveServicesClass(count) {
    if (count >= 6) {
        return 'danger';
    } else if (count >= 3) {
        return 'warning';
    } else {
        return 'success';
    }
}

function showAdvisorDetails(advisor) {
    // Set advisor ID on edit button for future reference
    document.getElementById('editAdvisorFromDetailsBtn').setAttribute('data-advisor-id', advisor.advisorId);

    // Personal Info Tab
    document.getElementById('viewAdvisorInitials').textContent = getInitials(advisor.firstName, advisor.lastName);
    document.getElementById('viewAdvisorName').textContent = advisor.firstName + ' ' + advisor.lastName;
    document.getElementById('viewAdvisorEmail').textContent = advisor.email;
    document.getElementById('viewAdvisorPhone').textContent = advisor.phoneNumber;
    document.getElementById('viewAdvisorId').textContent = advisor.formattedId || ('SA-' + advisor.advisorId);
    document.getElementById('viewAdvisorDepartment').textContent = advisor.department || 'Not assigned';
    document.getElementById('viewAdvisorHireDate').textContent = advisor.hireDate ? new Date(advisor.hireDate).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'Not available';

    // Workload Tab
    document.getElementById('viewAdvisorWorkloadValue').textContent = advisor.workloadPercentage + '%';

    const workloadBar = document.getElementById('viewAdvisorWorkloadBar');
    workloadBar.style.width = advisor.workloadPercentage + '%';
    workloadBar.className = 'workload-progress ' + getWorkloadClass(advisor.workloadPercentage);

    document.getElementById('viewAdvisorWorkloadText').textContent = getWorkloadText(advisor.workloadPercentage);

    // Show modal
    const detailsModal = new bootstrap.Modal(document.getElementById('advisorDetailsModal'));
    detailsModal.show();
}

// Validation functions
function validateField(fieldId, errorId, validationFn, errorMessage) {
    const field = document.getElementById(fieldId);
    const errorElement = document.getElementById(errorId);

    // Skip validation if field doesn't exist
    if (!field || !errorElement) return true;

    const isValid = validationFn(field.value);

    if (!isValid) {
        field.classList.add('is-invalid');
        errorElement.textContent = errorMessage;
        errorElement.classList.add('show');
        return false;
    } else {
        field.classList.remove('is-invalid');
        errorElement.textContent = '';
        errorElement.classList.remove('show');
        return true;
    }
}

// Validation rules
function validateName(value) {
    return value.trim().length >= 1;
}

function validateEmail(value) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(value);
}

function validatePhone(value) {
    const phoneRegex = /^\d{10}$/;
    return phoneRegex.test(value.replace(/\D/g, ''));
}

function validateDepartment(value) {
    return value.trim() !== '';
}

// Add event listeners for real-time validation
function setupFormValidation(formId, fieldValidations) {
    const form = document.getElementById(formId);
    if (!form) return;

    // Add blur event listeners to each field
    Object.keys(fieldValidations).forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) {
            field.addEventListener('blur', () => {
                const validation = fieldValidations[fieldId];
                validateField(fieldId, validation.errorId, validation.validationFn, validation.errorMessage);
            });
        }
    });
}

// Setup validation for both forms
document.addEventListener('DOMContentLoaded', function() {
    // Add Advisor Form Validations
    const addAdvisorValidations = {
        'firstName': {
            errorId: 'firstName-error',
            validationFn: validateName,
            errorMessage: 'First name must be at least 2 characters'
        },
        'lastName': {
            errorId: 'lastName-error',
            validationFn: validateName,
            errorMessage: 'Last name must be at least 1 characters'
        },
        'email': {
            errorId: 'email-error',
            validationFn: validateEmail,
            errorMessage: 'Please enter a valid email address'
        },
        'phone': {
            errorId: 'phone-error',
            validationFn: validatePhone,
            errorMessage: 'Please enter a valid 10-digit phone number'
        },
        'department': {
            errorId: 'department-error',
            validationFn: validateDepartment,
            errorMessage: 'Please select a department'
        }
    };

    // Edit Advisor Form Validations
    const editAdvisorValidations = {
        'editFirstName': {
            errorId: 'editFirstName-error',
            validationFn: validateName,
            errorMessage: 'First name must be at least 2 characters'
        },
        'editLastName': {
            errorId: 'editLastName-error',
            validationFn: validateName,
            errorMessage: 'Last name must be at least 2 characters'
        },
        'editEmail': {
            errorId: 'editEmail-error',
            validationFn: validateEmail,
            errorMessage: 'Please enter a valid email address'
        },
        'editPhone': {
            errorId: 'editPhone-error',
            validationFn: validatePhone,
            errorMessage: 'Please enter a valid 10-digit phone number'
        },
        'editDepartment': {
            errorId: 'editDepartment-error',
            validationFn: validateDepartment,
            errorMessage: 'Please select a department'
        }
    };

    setupFormValidation('addAdvisorForm', addAdvisorValidations);
    setupFormValidation('editAdvisorForm', editAdvisorValidations);
});

// Validate all fields in a form
function validateForm(formId, fieldValidations) {
    let isValid = true;

    Object.keys(fieldValidations).forEach(fieldId => {
        const validation = fieldValidations[fieldId];
        const fieldValid = validateField(fieldId, validation.errorId, validation.validationFn, validation.errorMessage);
        isValid = isValid && fieldValid;
    });

    return isValid;
}

function generateRandomPassword() {
    // Generate a password in the format: SA2025-XXXNNN (where X is a letter and N is a number)
    const letters = 'ABCDEFGHJKLMNPQRSTUVWXYZ'; // Excluded I and O to avoid confusion
    const numbers = '123456789'; // Excluded 0 to avoid confusion

    let password = 'SA2025-';

    // Add 3 random letters
    for (let i = 0; i < 3; i++) {
        password += letters.charAt(Math.floor(Math.random() * letters.length));
    }

    // Add 3 random numbers
    for (let i = 0; i < 3; i++) {
        password += numbers.charAt(Math.floor(Math.random() * numbers.length));
    }

    return password;
}

function saveServiceAdvisor() {
    // Get form
    const form = document.getElementById('addAdvisorForm');

    // Custom form validation
    const addAdvisorValidations = {
        'firstName': {
            errorId: 'firstName-error',
            validationFn: validateName,
            errorMessage: 'First name must be at least 2 characters'
        },
        'lastName': {
            errorId: 'lastName-error',
            validationFn: validateName,
            errorMessage: 'Last name must be at least 2 characters'
        },
        'email': {
            errorId: 'email-error',
            validationFn: validateEmail,
            errorMessage: 'Please enter a valid email address'
        },
        'phone': {
            errorId: 'phone-error',
            validationFn: validatePhone,
            errorMessage: 'Please enter a valid 10-digit phone number'
        },
        'department': {
            errorId: 'department-error',
            validationFn: validateDepartment,
            errorMessage: 'Please select a department'
        }
    };

    // Validate all fields
    if (!validateForm('addAdvisorForm', addAdvisorValidations)) {
        return;
    }

    // Generate a random password for the new advisor
    const generatedPassword = generateRandomPassword();

    // Get form data
    const formData = {
        firstName: document.getElementById('firstName').value,
        lastName: document.getElementById('lastName').value,
        email: document.getElementById('email').value,
        phoneNumber: document.getElementById('phone').value,
        department: document.getElementById('department').value,
        password: generatedPassword // Add the generated password to the request
    };

    showSpinner();

    // Call API to create new service advisor
    fetch('/admin/service-advisors', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (!response.ok) {
                // Log the status for debugging
                console.error(`Server responded with status: ${response.status}`);

                // Try to get more detailed error message
                return response.text().then(text => {
                    throw new Error(`Server responded with status: ${response.status}` +
                        (text ? `, message: ${text}` : ''));
                });
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            // Close modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('addAdvisorModal'));
            modal.hide();

            // Show success message about email
            showConfirmation(
                "Service Advisor Added Successfully",
                "The service advisor has been created and login credentials have been sent to their email address."
            );

            // Reset form
            form.reset();

            // Reload service advisors
            loadServiceAdvisors();
        })
        .catch(error => {
            hideSpinner();
            console.error('Error creating service advisor:', error);

            // Show user-friendly error message in the form
            showErrorMessage('addAdvisorForm', 'Failed to create service advisor. ' +
                (error.message || 'Please check your connection and try again.'));
        });
}

function showEditModal(advisorId) {
    // Close details modal if open
    const detailsModal = bootstrap.Modal.getInstance(document.getElementById('advisorDetailsModal'));
    if (detailsModal) {
        detailsModal.hide();
    }

    // Clear any previous error messages
    const form = document.getElementById('editAdvisorForm');
    if (form) {
        clearErrorMessage('editAdvisorForm');

        // Remove is-invalid class from all inputs
        form.querySelectorAll('.is-invalid').forEach(input => {
            input.classList.remove('is-invalid');
        });

        // Hide all error messages
        form.querySelectorAll('.error-message').forEach(error => {
            error.classList.remove('show');
            error.textContent = '';
        });
    }

    showSpinner();

    // Call API to get advisor details for editing
    fetch(`/admin/service-advisors/${advisorId}`, {
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
        .then(advisor => {
            hideSpinner();

            // Populate edit form
            document.getElementById('editAdvisorId').value = advisor.advisorId;
            document.getElementById('editUserId').value = advisor.userId;
            document.getElementById('editFirstName').value = advisor.firstName;
            document.getElementById('editLastName').value = advisor.lastName;
            document.getElementById('editEmail').value = advisor.email;
            document.getElementById('editPhone').value = advisor.phoneNumber;
            document.getElementById('editDepartment').value = advisor.department;
            document.getElementById('editPassword').value = '';

            // Show edit modal
            const editModal = new bootstrap.Modal(document.getElementById('editAdvisorModal'));
            editModal.show();
        })
        .catch(error => {
            hideSpinner();
            console.error('Error fetching advisor details for editing:', error);
            showErrorMessage('editAdvisorForm', 'Failed to load advisor details for editing. Please try again.');
        });
}

function updateServiceAdvisor() {
    // Get form
    const form = document.getElementById('editAdvisorForm');

    // Custom form validation
    const editAdvisorValidations = {
        'editFirstName': {
            errorId: 'editFirstName-error',
            validationFn: validateName,
            errorMessage: 'First name must be at least 2 characters'
        },
        'editLastName': {
            errorId: 'editLastName-error',
            validationFn: validateName,
            errorMessage: 'Last name must be at least 2 characters'
        },
        'editEmail': {
            errorId: 'editEmail-error',
            validationFn: validateEmail,
            errorMessage: 'Please enter a valid email address'
        },
        'editPhone': {
            errorId: 'editPhone-error',
            validationFn: validatePhone,
            errorMessage: 'Please enter a valid 10-digit phone number'
        },
        'editDepartment': {
            errorId: 'editDepartment-error',
            validationFn: validateDepartment,
            errorMessage: 'Please select a department'
        }
    };

    // Validate all fields
    if (!validateForm('editAdvisorForm', editAdvisorValidations)) {
        return;
    }

    // Get advisor ID
    const advisorId = document.getElementById('editAdvisorId').value;

    // Get form data
    const formData = {
        advisorId: advisorId,
        userId: document.getElementById('editUserId').value,
        firstName: document.getElementById('editFirstName').value,
        lastName: document.getElementById('editLastName').value,
        email: document.getElementById('editEmail').value,
        phoneNumber: document.getElementById('editPhone').value,
        department: document.getElementById('editDepartment').value
    };

    // Add password if provided
    const password = document.getElementById('editPassword').value;
    if (password) {
        formData.password = password;
    }

    showSpinner();

    // Call API to update service advisor
    fetch(`/admin/service-advisors/${advisorId}`, {
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

            // Close modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('editAdvisorModal'));
            modal.hide();

            // Show success message
            showConfirmation(
                'Service Advisor Updated',
                'The service advisor information has been updated successfully.' +
                (password ? ' A new password has been sent to their email.' : '')
            );

            // Reset form
            form.reset();

            // Reload service advisors
            loadServiceAdvisors();
        })
        .catch(error => {
            hideSpinner();
            console.error('Error updating service advisor:', error);
            showErrorMessage('editAdvisorForm', 'Failed to update service advisor. Please try again.');
        });
}

function filterServiceAdvisors(searchTerm) {
    // Reset to first page
    currentPage = 1;

    // Render with new search term (getFilteredAdvisors will use the search input value)
    renderServiceAdvisors();
}

function sortServiceAdvisors(sortBy) {
    // Sort the service advisors array based on the sortBy parameter
    serviceAdvisors.sort((a, b) => {
        switch (sortBy) {
            case 'name':
                return (a.firstName + a.lastName).localeCompare(b.firstName + b.lastName);
            case 'department':
                return (a.department || '').localeCompare(b.department || '');
            case 'workload':
                return b.workloadPercentage - a.workloadPercentage;
            case 'hire date':
                return new Date(a.hireDate || 0) - new Date(b.hireDate || 0);
            default:
                return 0;
        }
    });

    // Re-render the service advisors
    renderServiceAdvisors();
}
