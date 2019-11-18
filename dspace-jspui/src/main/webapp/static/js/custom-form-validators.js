"use strict";

$(function () {
    var titleField = $("#dc_title"),
        descriptionField = $("#dc_description_abstract_id"),
        agency = $("#agency"),
        requiredReport = $("#required-report-name"),
        requiredReportID = $('#dc_identifier_required-report-id');

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

    // On initial page load
    var initialRequiredReport = requiredReport.val();
    requiredReport.empty();
    // Add blank option
    requiredReport.append(new Option('', ''));
    if (agency.val() in requiredReports) {
        requiredReports[agency.val()].forEach(function (report) {
            requiredReport.append(new Option(report['report_name'], report['report_name']));
        });
    }
    // Add Not Required option
    requiredReport.append(new Option('Not Required', 'Not Required'));
    requiredReport.val(initialRequiredReport);

    // On Agency change
    agency.change(function () {
        var selectedAgency = agency.val();
        requiredReport.empty();
        requiredReportID.val('');
        // Add blank option
        requiredReport.append(new Option('', ''));
        if (selectedAgency in requiredReports) {
            requiredReports[selectedAgency].forEach(function (report) {
                requiredReport.append(new Option(report['report_name'], report['report_name']));
            });
        }
        // Add Not Required option
        requiredReport.append(new Option('Not Required', 'Not Required'));
    });

    // On Required Report change
    requiredReport.change(function () {
        var selectedAgency = agency.val();
        var selectedReport = requiredReport.val();
        if (selectedReport === '' || selectedReport === 'Not Required') {
            requiredReportID.val('');
        } else {
            for (var i = 0; i < requiredReports[selectedAgency].length; i++) {
                if (requiredReports[selectedAgency][i]['report_name'] === selectedReport) {
                    requiredReportID.val(requiredReports[selectedAgency][i]['report_id']);
                    break;
                }
            }
        }
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