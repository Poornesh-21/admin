/**
 * Login Error Handler
 * This script extracts user-friendly error messages from complex error responses.
 * It's designed to be loaded automatically when the user visits the login page.
 */
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