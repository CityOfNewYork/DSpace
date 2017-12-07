$(document).ready(function () {
    $("#submit-next").click(function (e) {
        // validator for fiscal and calender year
        if ($("#fiscal-year").val().length === 0 && $("#calender-year").val().length === 0) {
            e.preventDefault();
            $('.fiscal-calender-warning').show();
            document.getElementById("fiscal-year").scrollIntoView();
        }
        else {
            $('.fiscal-calender-warning').hide();
        }

        // validator for subject multiselect
        if ($("#subject-multiselect").val().length > 3) {
            e.preventDefault();
            $('.subject-warning').show();
            $(window).scrollTop(0);
        }
        else {
            $('.subject-warning').hide();
        }
    });
});