/**
 * Initializes the HAL Browser. Moved out of index.html so that no inline script is needed
 * (allows a strict Content-Security-Policy with script-src 'self').
 */
// Remove the loading skeleton placeholder: the HAL Browser renders its own UI into #browser
$('#browser').empty();
var browser = new HAL.Browser({
    container: $('#browser'),
    entryPoint: window.location.pathname + 'api'
});
Backbone.history.start();
