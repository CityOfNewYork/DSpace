$(document).ready(function () {
    $("#submit-next").click(function (e) {
        // validator for fiscal and calendar year
        if ($("#fiscal-year").val().length === 0 && $("#calendar-year").val().length === 0) {
            e.preventDefault();
            $('.fiscal-calendar-warning').show();
            $(window).scrollTop($("#fiscal-year").offset().top - 110);
        }
        else {
            $('.fiscal-calendar-warning').hide();
        }

        // validator for date published
        if ($("#submission-month").val() === "" ||
            $("#submission-day").val().length === 0 ||
            $("#submission-year").val().length === 0) {
            e.preventDefault();
            $('.date-published-warning').show();
            $(window).scrollTop($("#date-published").offset().top - 110);
        }
        else {
            $('.date-published-warning').hide();
        }

        // validator for subject multiselect
        if ($("#subject-multiselect").val().length > 3) {
            e.preventDefault();
            $('.subject-warning').show();
            $(window).scrollTop($("#subject-multiselect").offset().top - 110);
        }
        else {
            $('.subject-warning').hide();
        }
    });
});