"use strict";

$(function () {
    var titleField = $("#dc_title"),
        descriptionField = $("#dc_description_abstract_id");

    $("#submit-next").click(function (e) {
        // validator for fiscal and calendar year
        if ($("#fiscal-year").val().length === 0 && $("#calendar-year").val().length === 0) {
            e.preventDefault();
            $(".fiscal-calendar-warning").show();
            $(window).scrollTop($("#fiscal-year").offset().top - 110);
        }
        else {
            $(".fiscal-calendar-warning").hide();
        }

        // validator for date published
        if ($("#submission-month").val() === "" ||
            $("#submission-day").val().length === 0 ||
            $("#submission-year").val().length === 0) {
            e.preventDefault();
            $(".date-published-warning").show();
            $(window).scrollTop($("#date-published").offset().top - 110);
        }
        else {
            $(".date-published-warning").hide();
        }

        // validator for subject multiselect
        if ($("#subject-multiselect").val().length > 3) {
            e.preventDefault();
            $(".subject-warning").show();
            $(window).scrollTop($("#subject-multiselect").offset().top - 110);
        }
        else {
            $(".subject-warning").hide();
        }
    });

    // Set character counter text
    characterCounter("#title-character-count", 150, titleField.val().length, 10);
    characterCounter("#description-character-count", 300, descriptionField.val().length, 100);

    titleField.keyup(function () {
        characterCounter("#title-character-count", 150, $(this).val().length, 10)
    });

    descriptionField.keyup(function () {
        characterCounter("#description-character-count", 300, $(this).val().length, 100)
    });
});

function characterCounter (target, limit, currentLength, minLength) {
    /* Global character counter
     *
     * Parameters:
     * - target: string of target selector
     * - limit: integer of maximum character length
     * - currentLength: integer value of keyed in content
     * - minLength: integer of minimum character length (default = 0)
     *
     * Ex:
     * {
     *     target: "#dc_title",
     *     charLength: 150,
     *     contentLength: $(this).val().length,
     *     minLength: 0
     * }
     *
     * */
    var length = limit - currentLength;
    minLength = (typeof minLength !== 'undefined') ? minLength : 0;
    var s = length === 1 ? "" : "s";
    $(target).text(length + " character" + s + " remaining");
    if (length == 0) {
        $(target).css("color", "red");
    } else if (currentLength < minLength) {
        $(target).css("color", "red");
    }
    else {
        $(target).css("color", "black");
    }
}