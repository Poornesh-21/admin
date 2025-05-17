 // Service Advisor Login JavaScript

    document.addEventListener('DOMContentLoaded', function() {
    // ===== UI Element Handlers =====

    // Password visibility toggle for login form
    const togglePassword = document.getElementById('togglePassword');
    const password = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');

    if (togglePassword && password && eyeIcon) {
    togglePassword.addEventListener('click', function() {
    const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
    password.setAttribute('type', type);

    eyeIcon.classList.toggle('fa-eye');
    eyeIcon.classList.toggle('fa-eye-slash');
});
}

    // New password toggle for password change modal
    const toggleNewPassword = document.getElementById('toggleNewPassword');
    const newPassword = document.getElementById('newPassword');
    const newEyeIcon = document.getElementById('newEyeIcon');

    if (toggleNewPassword && newPassword && newEyeIcon) {
    toggleNewPassword.addEventListener('click', function() {
    const type = newPassword.getAttribute('type') === 'password' ? 'text' : 'password';
    newPassword.setAttribute('type', type);

    newEyeIcon.classList.toggle('fa-eye');
    newEyeIcon.classList.toggle('fa-eye-slash');
});
}

    // Confirm password toggle for password change modal
    const toggleConfirmPassword = document.getElementById('toggleConfirmPassword');
    const confirmPassword = document.getElementById('confirmPassword');
    const confirmEyeIcon = document.getElementById('confirmEyeIcon');

    if (toggleConfirmPassword && confirmPassword && confirmEyeIcon) {
    toggleConfirmPassword.addEventListener('click', function() {
    const type = confirmPassword.getAttribute('type') === 'password' ? 'text' : 'password';
    confirmPassword.setAttribute('type', type);

    confirmEyeIcon.classList.toggle('fa-eye');
    confirmEyeIcon.classList.toggle('fa-eye-slash');
});
}

    // ===== Password Strength Check =====

    // Password strength validation
    if (newPassword) {
    newPassword.addEventListener('input', function() {
    const value = newPassword.value;
    let strength = 0;

    // Check length
    const lengthCheck = document.getElementById('length');
    if (value.length >= 8) {
    lengthCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 8 characters';
    lengthCheck.classList.add('requirement-met');
    strength += 20;
} else {
    lengthCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 8 characters';
    lengthCheck.classList.remove('requirement-met');
}

    // Check uppercase
    const uppercaseCheck = document.getElementById('uppercase');
    if (/[A-Z]/.test(value)) {
    uppercaseCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 1 uppercase letter';
    uppercaseCheck.classList.add('requirement-met');
    strength += 20;
} else {
    uppercaseCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 1 uppercase letter';
    uppercaseCheck.classList.remove('requirement-met');
}

    // Check lowercase
    const lowercaseCheck = document.getElementById('lowercase');
    if (/[a-z]/.test(value)) {
    lowercaseCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 1 lowercase letter';
    lowercaseCheck.classList.add('requirement-met');
    strength += 20;
} else {
    lowercaseCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 1 lowercase letter';
    lowercaseCheck.classList.remove('requirement-met');
}

    // Check number
    const numberCheck = document.getElementById('number');
    if (/[0-9]/.test(value)) {
    numberCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 1 number';
    numberCheck.classList.add('requirement-met');
    strength += 20;
} else {
    numberCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 1 number';
    numberCheck.classList.remove('requirement-met');
}

    // Check special character
    const specialCheck = document.getElementById('special');
    if (/[^A-Za-z0-9]/.test(value)) {
    specialCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 1 special character';
    specialCheck.classList.add('requirement-met');
    strength += 20;
} else {
    specialCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 1 special character';
    specialCheck.classList.remove('requirement-met');
}

    // Update strength meter
    const strengthProgress = document.getElementById('strengthProgress');
    const strengthText = document.getElementById('strengthText');

    strengthProgress.style.width = strength + '%';

    if (strength <= 20) {
    strengthText.textContent = 'Very Weak';
    strengthProgress.className = 'strength-progress very-weak';
} else if (strength <= 40) {
    strengthText.textContent = 'Weak';
    strengthProgress.className = 'strength-progress weak';
} else if (strength <= 60) {
    strengthText.textContent = 'Medium';
    strengthProgress.className = 'strength-progress medium';
} else if (strength <= 80) {
    strengthText.textContent = 'Strong';
    strengthProgress.className = 'strength-progress strong';
} else {
    strengthText.textContent = 'Very Strong';
    strengthProgress.className = 'strength-progress very-strong';
}
});
}

    // Password match validation
    if (confirmPassword) {
    confirmPassword.addEventListener('input', function() {
    const passwordMismatch = document.getElementById('passwordMismatch');

    if (newPassword.value !== confirmPassword.value) {
    passwordMismatch.classList.remove('d-none');
} else {
    passwordMismatch.classList.add('d-none');
}
});
}

    // ===== Form Handlers =====

    // Login form submission handler
    const loginForm = document.getElementById('loginForm');

    if (loginForm) {
    loginForm.addEventListener('submit', function(e) {
    e.preventDefault();

    // Get form data
    const email = document.getElementById('email').value;
    const passwordValue = document.getElementById('password').value;

    // Hide any previous error messages
    hideErrorMessage();

    // Show loading state
    const loginBtn = document.querySelector('.btn-login');
    const originalText = loginBtn.innerHTML;
    loginBtn.disabled = true;
    loginBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Signing in...';

    // Prepare request payload
    const requestData = {
    email: email,
    password: passwordValue
};

    // Send authentication request
    fetch('/serviceAdvisor/api/login', {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json'
},
    body: JSON.stringify(requestData)
})
    .then(response => {
    if (!response.ok) {
    return response.json().then(errorData => {
    throw new Error(errorData.message || 'Authentication failed');
});
}
    return response.json();
})
    .then(data => {
    // Authentication successful
    console.log('Login successful', data);

    // Check if user is a service advisor
    if (data.role && data.role.toLowerCase() !== 'serviceadvisor') {
    throw new Error('You do not have permission to access the Service Advisor portal');
}

    // Store auth data
    localStorage.setItem('jwtToken', data.token);
    localStorage.setItem('userRole', data.role);
    localStorage.setItem('userEmail', data.email);
    localStorage.setItem('userName', data.firstName + ' ' + data.lastName);

    // Check if this is a temporary password that needs to be changed
    const isTemporaryPassword = checkIfTemporaryPassword(passwordValue);

    if (isTemporaryPassword) {
    // Set the current password in the hidden field for the API request
    document.getElementById('currentPassword').value = passwordValue;

    // Show change password modal
    const changePasswordModal = new bootstrap.Modal(document.getElementById('changePasswordModal'));
    changePasswordModal.show();

    // Reset button state
    loginBtn.disabled = false;
    loginBtn.innerHTML = originalText;
} else {
    // Redirect to dashboard with token
    window.location.href = '/serviceAdvisor/dashboard?token=' + data.token;
}
})
    .catch(error => {
    console.error('Login error:', error);

    // Show error message
    showErrorMessage(error.message || 'Authentication failed. Please check your credentials.');

    // Reset button state
    loginBtn.disabled = false;
    loginBtn.innerHTML = originalText;
});
});
}

    // Password change form submission handler
    const changePasswordForm = document.getElementById('changePasswordForm');

    if (changePasswordForm) {
    changePasswordForm.addEventListener('submit', function(e) {
    e.preventDefault();

    const newPasswordValue = newPassword.value;
    const confirmPasswordValue = confirmPassword.value;
    const currentPasswordValue = document.getElementById('currentPassword').value;
    const passwordMismatch = document.getElementById('passwordMismatch');

    // Hide any previous error messages
    hideErrorMessage();
    passwordMismatch.classList.add('d-none');

    // Validate password match
    if (newPasswordValue !== confirmPasswordValue) {
    passwordMismatch.classList.remove('d-none');
    return;
}

    // Validate password strength
    if (validatePasswordStrength(newPasswordValue) < 60) {
    showErrorMessage('Password is not strong enough. Please follow the requirements.');
    return;
}

    // Show loading state
    const submitBtn = changePasswordForm.querySelector('button[type="submit"]');
    const originalBtnText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Setting password...';

    // Get the token from localStorage
    const token = localStorage.getItem('jwtToken');

    // Prepare request data
    const requestData = {
    currentPassword: currentPasswordValue,
    newPassword: newPasswordValue,
    confirmPassword: confirmPasswordValue,
    isTemporaryPassword: true  // This is a first-time login password change
};

    // Send the password change request to our new endpoint
    fetch('/serviceAdvisor/api/change-password?token=' + token, {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
},
    body: JSON.stringify(requestData)
})
    .then(response => {
    if (!response.ok) {
    return response.json().then(errorData => {
    throw new Error(errorData.error || 'Failed to change password');
});
}
    return response.json();
})
    .then(data => {
    // Password change successful
    console.log('Password changed successfully', data);

    // Update token in localStorage if a new one was returned
    if (data.token) {
    localStorage.setItem('jwtToken', data.token);
}

    // Show success message briefly
    showSuccessMessage('Password changed successfully! Redirecting to dashboard...');

    // Redirect to dashboard after a short delay
    setTimeout(() => {
    window.location.href = '/serviceAdvisor/dashboard?token=' + (data.token || token);
}, 1500);
})
    .catch(error => {
    console.error('Password change error:', error);

    // Reset button state
    submitBtn.disabled = false;
    submitBtn.innerHTML = originalBtnText;

    // Show error message
    showErrorMessage(error.message || 'Failed to change password. Please try again.');
});
});
}

    // ===== Utility Functions =====

    // Calculate password strength (returns 0-100)
    function validatePasswordStrength(password) {
    let strength = 0;

    // Length check
    if (password.length >= 8) strength += 20;

    // Uppercase check
    if (/[A-Z]/.test(password)) strength += 20;

    // Lowercase check
    if (/[a-z]/.test(password)) strength += 20;

    // Number check
    if (/[0-9]/.test(password)) strength += 20;

    // Special character check
    if (/[^A-Za-z0-9]/.test(password)) strength += 20;

    return strength;
}

    // Show error message
    function showErrorMessage(message) {
    // Create error alert if it doesn't exist
    let errorAlert = document.getElementById('loginError');

    if (!errorAlert) {
    errorAlert = document.createElement('div');
    errorAlert.className = 'alert alert-danger mt-3';
    errorAlert.id = 'loginError';
    errorAlert.role = 'alert';

    const formEl = document.querySelector('.glass-card') || document.body;
    formEl.prepend(errorAlert);
}

    errorAlert.textContent = message;
    errorAlert.classList.remove('d-none');
}

    // Function to display success message
    function showSuccessMessage(message) {
    // Create success alert if it doesn't exist
    let successAlert = document.getElementById('passwordSuccess');

    if (!successAlert) {
    successAlert = document.createElement('div');
    successAlert.className = 'alert alert-success mt-3';
    successAlert.id = 'passwordSuccess';
    successAlert.role = 'alert';

    const formEl = document.querySelector('#changePasswordForm') || document.body;
    formEl.prepend(successAlert);
}

    successAlert.textContent = message;
    successAlert.classList.remove('d-none');
}

    // Hide error message
    function hideErrorMessage() {
    const errorAlert = document.getElementById('loginError');
    if (errorAlert) {
    errorAlert.classList.add('d-none');
}

    const passwordError = document.getElementById('passwordError');
    if (passwordError) {
    passwordError.classList.add('d-none');
}

    const successAlert = document.getElementById('passwordSuccess');
    if (successAlert) {
    successAlert.classList.add('d-none');
}
}

    // Check if password is temporary
    function checkIfTemporaryPassword(password) {
    // This is a simplified check - in a real scenario, you'd have specific patterns
    // for temporary passwords or check with the server

    // Example: Consider passwords starting with SA2025- as temporary
    return password.startsWith('SA2025-');
}

    // ===== Auto-login with token if available =====

    // Check if we have a valid token already
    const existingToken = localStorage.getItem('jwtToken');
    const role = localStorage.getItem('userRole');

    if (existingToken && role && role.toLowerCase() === 'serviceadvisor') {
    // Check if token is still valid by making a request to a protected endpoint
    fetch('/serviceAdvisor/api/validate-token', {
    method: 'GET',
    headers: {
    'Authorization': 'Bearer ' + existingToken
}
})
    .then(response => {
    if (response.ok) {
    // Token is valid, redirect to dashboard
    window.location.href = '/serviceAdvisor/dashboard?token=' + existingToken;
} else {
    // Token is invalid, clear storage
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userName');
}
})
    .catch(error => {
    console.error('Token validation error:', error);
    // Clear storage on error
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userName');
});
}
});
(function() {
    console.log('Login error handler loaded');
    
    // Function to patch the fetch API to intercept error responses
    function patchFetch() {
        // Store the original fetch function
        const originalFetch = window.fetch;
        
        // Override the fetch function
        window.fetch = function(url, options) {
            // Call the original fetch function
            return originalFetch(url, options)
                .then(response => {
                    // If this is a login request and it failed, handle it specially
                    if (url === '/serviceAdvisor/api/login' && !response.ok) {
                        // Clone the response so we can read it twice
                        const clonedResponse = response.clone();
                        
                        // Process the cloned response
                        return clonedResponse.json().then(errorData => {
                            // Check if this is the specific error we're looking for
                            if (errorData && errorData.message && 
                                errorData.message.includes('Invalid email/password combination')) {
                                
                                // Create a modified response with a user-friendly message
                                const modifiedData = {
                                    ...errorData,
                                    message: 'Invalid email or password. Please try again.'
                                };
                                
                                // Create a new response with the modified data
                                const modifiedResponse = new Response(
                                    JSON.stringify(modifiedData),
                                    {
                                        status: response.status,
                                        statusText: response.statusText,
                                        headers: response.headers
                                    }
                                );
                                
                                return modifiedResponse;
                            }
                            
                            // If it's not the specific error we're looking for, return the original response
                            return response;
                        }).catch(() => {
                            // If there was an error processing the JSON, return the original response
                            return response;
                        });
                    }
                    
                    // For all other requests, return the response as-is
                    return response;
                });
        };
        
        console.log('Fetch API patched successfully');
    }
    
    // Wait for the DOM to be fully loaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', patchFetch);
    } else {
        patchFetch();
    }
})();