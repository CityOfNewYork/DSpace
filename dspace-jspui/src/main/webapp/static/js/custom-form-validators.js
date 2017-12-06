$( document ).ready(function() {
    $("#submit-next").click(function (e) {
        if ($("#fiscal-year").val().length === 0 && $("#calender-year").val().length === 0) {
           e.preventDefault();
           $('.fiscal-calender-warning').show();
        }
        else {
           $('.fiscal-calender-warning').hide();
        }
    });
});