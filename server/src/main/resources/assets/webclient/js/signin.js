$(function(){
    var form = $('form.form-signin');
    var error = form.find('.error');
    var showError = function() {
        form.find('.credentials-container').addClass('has-error');
        error.removeClass('hidden');
    };
    var hideError = function() {
        form.find('.credentials-container').removeClass('has-error');
        error.addClass('hidden');
    };

    form.on("submit", function(e) {
        e.preventDefault();
        hideError();
        $.post("/auth", $(this).serialize(), function() {
            window.location.replace("/")
        })
        .fail(function() {
            showError();
        });
    });
});