$(document).ready(function() {
    var successHandler = function(result, status, xhr) {
        // look for Authorization header & save to a MyHalBrowserToken cookie
        document.cookie = "MyHalBrowserToken=" + xhr.getResponseHeader('Authorization').split(" ")[1];
        // Check for an update to the CSRF Token & save to a MyHalBrowserCsrfToken cookie (if found)
        checkForUpdatedCSRFTokenInResponse(xhr);
        toastr.success('You are now logged in. Please wait while we redirect you...', 'Login Successful');
        setTimeout(function() {
            window.location.href = window.location.pathname.replace("login.html", "");
        }, 2000);
    };
    toastr.options = {
        "closeButton": false,
        "debug": false,
        "newestOnTop": false,
        "progressBar": false,
        "positionClass": "toast-top-center",
        "preventDuplicates": false,
        "showDuration": "300",
        "hideDuration": "1000",
        "timeOut": "3000",
        "extendedTimeOut": "1000",
        "showEasing": "swing",
        "hideEasing": "linear",
        "showMethod": "fadeIn",
        "hideMethod": "fadeOut",
        "onclick" : function() { toastr.remove(); }
    }

    // retrieves a valid CSRF token (please note that this method works both in DS 7 and DS 8)
    // HTTP response code 403 is expected at this point (the response contains the DSPACE-XSRF-TOKEN header)
    $.ajax({
        url : window.location.href.replace("login.html", "") + 'api/authn/login',
        type : 'POST',
        error : function(xhr) {
            // Check for an update to the CSRF Token & save to a MyHalBrowserCsrfToken cookie (if found)
            checkForUpdatedCSRFTokenInResponse(xhr);
        }
    });

    // When the login page loads, we do *two* AJAX requests.
    // (1) Call GET /api/authn/status. This call checks to see if you are logged in
    //     (if not, WWW-Authenticate will return login options).
    // (2) If that /api/authn/status call finds authentication data, call POST /api/authn/login.
    //     This scenario occurs when you log in via an external authentication system (e.g. Shibboleth)
    //     in which case the main role of /api/authn/login is to simply ensure the "Authorization" header
    //     is sent back to the client (based on your authentication data).
    $.ajax({
        url : window.location.href.replace("login.html", "") + 'api/authn/status',
        type : 'GET',
        success : function(result, status, xhr) {

            // Check for WWW-Authenticate header. If found, this means we are not yet authenticated, and
            // therefore we need to display available authentication options.
            var authenticate = xhr.getResponseHeader("WWW-Authenticate");
            if (authenticate !== null && authenticate.includes('location=')) {
                var element = $('div.other-login-methods');
                var realms = authenticate.match(/(\w+ (\w+=((".*?")|[^,]*)(, )?)*)/g);
                if (realms.length === 1){
                    var loc = /location="([^,]*)"/.exec(authenticate);
                    if (loc !== null && loc.length === 2) {
                        document.location = loc[1];
                    }
                } else if (realms.length > 1){
                    for (var i = 0; i < realms.length; i++){
                        addLocationButton(realms[i], element);
                    }
                }
            } else {
                // If Authentication data was found, do a POST /api/authn/login to ensure that data's JWT
                // is sent back in the "Authorization" header. This simply completes an external authentication
                // process (e.g. Shibboleth)
                $.ajax({
                    url : window.location.href.replace("login.html", "") + 'api/authn/login',
                    type : 'POST',
                    beforeSend: function (xhr) {
                        // If CSRF token found in cookie, send it back as X-XSRF-Token header
                        var csrfToken = getCSRFToken();
                        if (csrfToken != null) {
                            xhr.setRequestHeader('X-XSRF-Token', csrfToken);
                        }
                    },
                    success : successHandler,
                    error : function(xhr) {
                        // Check for an update to the CSRF Token & save to a MyHalBrowserCsrfToken cookie (if found)
                        checkForUpdatedCSRFTokenInResponse(xhr);
                        toastr.error('Failed to logged in. Please check for errors in Javascript console.', 'Login Failed');
                    }
                });
            }
        },
        error : function() {
            toastr.error('Failed to connect with backend. Please check for errors in Javascript console.', 'Could Not Load');
        }
    });

    function addLocationButton(realm, element){
        element.removeClass("d-none");
        var loc = /location="([^,]*)"/.exec(realm);
        var name = /(\w+) (\w+=((".*?")|[^,]*)(, )?)*/.exec(realm);
        if (loc !== null && loc.length === 2) {
            element.append('<a href="' + loc[1] + '" class="btn btn-large btn-primary alternative-login">' + capitalizeFirstLetter(name[1]) + '</a>');
        }
    }

    function capitalizeFirstLetter(string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

    /**
     * Check current response headers to see if the CSRF Token has changed. If a new value is found in headers,
     * save the new value into our "MyHalBrowserCsrfToken" cookie.
     **/
    function checkForUpdatedCSRFTokenInResponse(jqxhr) {
        // look for DSpace-XSRF-TOKEN header & save to our MyHalBrowserCsrfToken cookie (if found)
        var updatedCsrfToken = jqxhr.getResponseHeader('DSPACE-XSRF-TOKEN');
        if (updatedCsrfToken != null) {
            document.cookie = "MyHalBrowserCsrfToken=" + updatedCsrfToken;
        }
    }

    /**
     * Get CSRF Token by parsing it out of the "MyHalBrowserCsrfToken" cookie.
     * This cookie is set in login.html after a successful login occurs.
     **/
    function getCSRFToken() {
        var cookie = document.cookie.match('(^|;)\\s*' + 'MyHalBrowserCsrfToken' + '\\s*=\\s*([^;]+)');
        if (cookie != null) {
            return cookie.pop();
        } else {
            return null;
        }
    }

    // When the Username/Password Login form is submitted, POST that data directly to /api/authn/login.
    // This logs the user in and ensures the "Authorization" header is set with the JWT.
    $("#login-form").submit(function(event) {
        event.preventDefault();
        $.ajax({
            //This depends on this file to be called login.html
            url : window.location.href.replace("login.html", "") + 'api/authn/login',
            type : 'POST',
            async : false,
            data: {
                user: $("#username").val(),
                password: $("#password").val()
            },
            beforeSend: function (xhr) {
                // If CSRF token found in cookie, send it back as X-XSRF-Token header
                var csrfToken = getCSRFToken();
                if (csrfToken != null) {
                    xhr.setRequestHeader('X-XSRF-Token', csrfToken);
                }
            },
            success : successHandler,
            error : function(xhr) {
                // Check for an update to the CSRF Token & save to a MyHalBrowserCsrfToken cookie (if found)
                checkForUpdatedCSRFTokenInResponse(xhr);
                toastr.error('The credentials you entered are invalid. Please try again.', 'Login Failed');
            }
        });
    });
});
