
    $(document).ready(function() {
    // Check for error parameter in URL
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("error")) {
    const errorType = urlParams.get("error");

    // Check for specific error types or provide more helpful messages
    if (errorType === "invalid_grant" || errorType === "bad_credentials") {
    $("#alertMessage").text("Invalid password. Please check your password and try again.");
} else if (errorType === "invalid_token" || errorType === "invalid_client") {
    $("#alertMessage").text("Email not found. Please check your email address.");
} else if (errorType === "access_denied") {
    $("#alertMessage").text("Access denied. Your account may not have appropriate permissions.");
} else if (errorType === "unauthorized") {
    $("#alertMessage").text("Your account is not authorized to access this system.");
} else if (errorType === "session_expired") {
    $("#alertMessage").text("Your session has expired. Please log in again.");
} else {
    $("#alertMessage").text("Authentication failed. Please verify your email and password.");
}

    $("#loginAlert").show();
}

    // Password toggle functionality
    $("#togglePassword").click(function() {
    const passwordField = $("#password");
    const eyeIcon = $("#eyeIcon");

    if (passwordField.attr("type") === "password") {
    passwordField.attr("type", "text");
    eyeIcon.removeClass("fa-eye").addClass("fa-eye-slash");
} else {
    passwordField.attr("type", "password");
    eyeIcon.removeClass("fa-eye-slash").addClass("fa-eye");
}
});

    // Clear any previous tokens
    localStorage.removeItem("jwt-token");
    sessionStorage.removeItem("jwt-token");

    // Handle form submission via AJAX
    $("#loginForm").submit(function(e) {
    e.preventDefault(); // Prevent standard form submission

    const email = $("#email").val();
    const password = $("#password").val();
    const rememberMe = $("#rememberMe").is(":checked");

    // Basic form validation
    if (!isValidEmail(email)) {
    $("#alertMessage").text("Please enter a valid email address.");
    $("#loginAlert").show();
    return false;
}

    // Password length check
    if (password.length < 6) {
    $("#alertMessage").text("Password must be at least 6 characters long.");
    $("#loginAlert").show();
    return false;
}

    // Show loading button
    $(".btn-login").html('<span class="spinner-border spinner-border-sm mr-2" role="status" aria-hidden="true"></span> Signing in...');
    $(".btn-login").prop("disabled", true);

    // Send AJAX request
    $.ajax({
    url: "/admin/api/login",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify({
    email: email,
    password: password
}),
    success: function(response) {
    // Store token in local storage or session storage based on remember me
    const storage = rememberMe ? localStorage : sessionStorage;

    // Store token and user info in both storages for redundancy
    storage.setItem("jwt-token", response.token);
    storage.setItem("user-role", response.role);
    storage.setItem("user-name", response.firstName + " " + response.lastName);
    storage.setItem("user-email", response.email);

    // Store user info in session storage always
    sessionStorage.setItem("jwt-token", response.token);

    // Add token to redirect URL
    window.location.href = "/admin/dashboard?token=" + encodeURIComponent(response.token);
},
    error: function(xhr) {
    let errorMsg = "Authentication failed. Please check your credentials.";

    if (xhr.responseJSON && xhr.responseJSON.message) {
    errorMsg = xhr.responseJSON.message;

    // Check if the error message contains the JSON string with "Invalid email/password combination"
    if (errorMsg.includes("Invalid email/password combination")) {
    errorMsg = "Invalid email or password. Please try again.";
}
} else if (xhr.responseText) {
    try {
    // Try to parse the response text as JSON
    const responseObj = JSON.parse(xhr.responseText);

    // Check if the parsed object has a message property
    if (responseObj.message) {
    // Check if the message contains a JSON string
    if (responseObj.message.includes("{")) {
    try {
    // Try to parse the inner JSON string
    const innerJson = JSON.parse(responseObj.message.substring(
    responseObj.message.indexOf("{"),
    responseObj.message.lastIndexOf("}") + 1
    ));

    if (innerJson.message) {
    errorMsg = innerJson.message;

    // Make the message more user-friendly
    if (errorMsg === "Invalid email/password combination") {
    errorMsg = "Invalid email or password. Please try again.";
}
}
} catch (e) {
    // If parsing fails, use the original message
    errorMsg = responseObj.message;
}
} else {
    errorMsg = responseObj.message;
}
}
} catch (e) {
    // If parsing fails, check if the response contains the specific error message
    if (xhr.responseText.includes("Invalid email/password combination")) {
    errorMsg = "Invalid email or password. Please try again.";
}
}
}

    $("#alertMessage").text(errorMsg);
    $("#loginAlert").show();

    // Reset button state
    $(".btn-login").html('<span class="btn-text"><span>Sign in to Dashboard</span><i class="fas fa-arrow-right"></i></span>');
    $(".btn-login").prop("disabled", false);
}
});
});

    // Email validation helper function
    function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}
});
