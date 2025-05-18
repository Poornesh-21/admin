/**
 * Albany Vehicle Service Management System
 * Home/Dashboard JavaScript functionality
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize tooltips
    const tooltips = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltips.forEach(tooltip => {
        new bootstrap.Tooltip(tooltip);
    });

    // Initialize popovers
    const popovers = document.querySelectorAll('[data-bs-toggle="popover"]');
    popovers.forEach(popover => {
        new bootstrap.Popover(popover);
    });

    // Dashboard statistics animation
    const statElements = document.querySelectorAll('.stat-value');
    if (statElements) {
        statElements.forEach(statEl => {
            const targetValue = parseInt(statEl.getAttribute('data-value'));
            animateCounter(statEl, targetValue);
        });
    }

    // Service schedule date picker
    const scheduleDateInput = document.getElementById('scheduleDate');
    if (scheduleDateInput) {
        // Set minimum date to tomorrow
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        
        const minDate = tomorrow.toISOString().split('T')[0];
        scheduleDateInput.setAttribute('min', minDate);
        
        // Set maximum date to 30 days from now
        const maxDate = new Date();
        maxDate.setDate(maxDate.getDate() + 30);
        scheduleDateInput.setAttribute('max', maxDate.toISOString().split('T')[0]);
    }

    // Service Type Selection - Update price when service type changes
    const serviceTypeSelect = document.getElementById('serviceType');
    const servicePriceElement = document.getElementById('servicePrice');
    
    if (serviceTypeSelect && servicePriceElement) {
        serviceTypeSelect.addEventListener('change', function() {
            updateServicePrice(this.value);
        });
    }

    // Appointment time slot selection
    const timeSlotButtons = document.querySelectorAll('.time-slot-btn');
    if (timeSlotButtons.length > 0) {
        timeSlotButtons.forEach(button => {
            button.addEventListener('click', function() {
                // Remove active class from all buttons
                timeSlotButtons.forEach(btn => btn.classList.remove('active'));
                
                // Add active class to clicked button
                this.classList.add('active');
                
                // Update hidden input with selected time
                document.getElementById('selectedTimeSlot').value = this.getAttribute('data-time');
            });
        });
    }

    // Vehicle selection in service booking form
    const vehicleSelect = document.getElementById('vehicleSelect');
    const addVehicleBtn = document.getElementById('addVehicleBtn');
    const newVehicleForm = document.getElementById('newVehicleForm');
    
    if (vehicleSelect && addVehicleBtn && newVehicleForm) {
        addVehicleBtn.addEventListener('click', function() {
            vehicleSelect.classList.add('d-none');
            addVehicleBtn.classList.add('d-none');
            newVehicleForm.classList.remove('d-none');
        });
        
        document.getElementById('cancelAddVehicle').addEventListener('click', function() {
            vehicleSelect.classList.remove('d-none');
            addVehicleBtn.classList.remove('d-none');
            newVehicleForm.classList.add('d-none');
        });
    }

    // Initialize service history data table
    const serviceHistoryTable = document.getElementById('serviceHistoryTable');
    if (serviceHistoryTable) {
        initializeDataTable(serviceHistoryTable);
    }
});

/**
 * Animates a counter from 0 to target value
 */
function animateCounter(element, targetValue) {
    let currentValue = 0;
    const duration = 1500; // 1.5 seconds
    const interval = 16; // ~60fps
    const steps = duration / interval;
    const increment = targetValue / steps;
    
    const timer = setInterval(() => {
        currentValue += increment;
        if (currentValue >= targetValue) {
            clearInterval(timer);
            currentValue = targetValue;
        }
        element.textContent = Math.floor(currentValue).toLocaleString();
    }, interval);
}

/**
 * Update service price based on selected service type
 */
function updateServicePrice(serviceType) {
    const prices = {
        'oil-change': 49.99,
        'tire-rotation': 39.99,
        'brake-service': 149.99,
        'engine-tune-up': 199.99,
        'ac-service': 129.99,
        'full-inspection': 89.99
    };
    
    const priceElement = document.getElementById('servicePrice');
    if (priceElement) {
        if (prices[serviceType]) {
            priceElement.textContent = `$${prices[serviceType].toFixed(2)}`;
            document.getElementById('servicePriceInput').value = prices[serviceType].toFixed(2);
        } else {
            priceElement.textContent = 'Price varies';
            document.getElementById('servicePriceInput').value = '0.00';
        }
    }
}

/**
 * Initialize data table with sorting and pagination
 */
function initializeDataTable(tableElement) {
    new DataTable(tableElement, {
        paging: true,
        ordering: true,
        info: true,
        responsive: true,
        language: {
            search: "Search records:",
            lengthMenu: "Show _MENU_ entries per page",
            info: "Showing _START_ to _END_ of _TOTAL_ entries",
            paginate: {
                first: "First",
                last: "Last",
                next: "Next",
                previous: "Previous"
            }
        }
    });
}

/**
 * Filter appointments by status
 */
function filterAppointments(status) {
    const appointmentCards = document.querySelectorAll('.appointment-card');
    
    if (status === 'all') {
        appointmentCards.forEach(card => {
            card.classList.remove('d-none');
        });
    } else {
        appointmentCards.forEach(card => {
            if (card.getAttribute('data-status') === status) {
                card.classList.remove('d-none');
            } else {
                card.classList.add('d-none');
            }
        });
    }
    
    // Update active filter button
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    document.querySelector(`.filter-btn[data-filter="${status}"]`).classList.add('active');
}

/**
 * Cancel appointment confirmation
 */
function confirmCancelAppointment(appointmentId) {
    if (confirm('Are you sure you want to cancel this appointment? This action cannot be undone.')) {
        // Send request to cancel appointment
        fetch(`/api/appointments/${appointmentId}/cancel`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            throw new Error('Failed to cancel appointment');
        })
        .then(data => {
            // Show success message
            showNotification('Appointment cancelled successfully', 'success');
            
            // Update UI
            const appointmentCard = document.querySelector(`[data-appointment-id="${appointmentId}"]`);
            if (appointmentCard) {
                appointmentCard.querySelector('.status-badge').textContent = 'Cancelled';
                appointmentCard.querySelector('.status-badge').className = 'badge badge-cancelled status-badge';
                appointmentCard.setAttribute('data-status', 'cancelled');
            }
        })
        .catch(error => {
            showNotification(error.message, 'error');
        });
    }
}

/**
 * Display notification to user
 */
function showNotification(message, type = 'info') {
    const toastContainer = document.getElementById('toast-container');
    
    if (!toastContainer) {
        // Create toast container if it doesn't exist
        const container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(container);
    }
    
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white bg-${type === 'error' ? 'danger' : type === 'success' ? 'success' : 'primary'}" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;
    
    document.getElementById('toast-container').insertAdjacentHTML('beforeend', toastHtml);
    
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { delay: 5000 });
    toast.show();
    
    // Remove toast from DOM after it's hidden
    toastElement.addEventListener('hidden.bs.toast', function() {
        toastElement.remove();
    });
}