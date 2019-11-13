"use strict";

$(function () {
    var titleField = $("#dc_title"),
        descriptionField = $("#dc_description_abstract_id"),
        agency = $("#agency"),
        requiredReport = $("#required-report-type");

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

    // JSON WITH REQUIRED REPORTS
    var requiredReports = {
        'Actuary, NYC Office of the (NYCOA)': ['Report on Open Data Compliance','Language Access Implementation Plan/Local Law 30 of 2017 Report; required at least every 3 years','Report on Agency Policies on Identifying Information','Quarterly Equal Employment Opportunity and Diversity Plan Implementation','Project Initiation, Commitment Plan','Report on Proposed Scope of Projects'],
        'Administrative Trials and Hearings, Office of (OATH)': ['Annual Report on Adjudications of Engine Idling Violations (with Environmental Control Board (ECB))','Report on Open Data Compliance','Language Access Implementation Plan/Local Law 30 of 2017 Report; required at least every 3 years','Report on Agency Policies on Identifying Information','Report on Agency Policies on Identifying Information','BenchNotes','Adjudications of Specified Violations/"Criminal Justice Reform Act Quarterly Report"','Quarterly Equal Employment Opportunity and Diversity Plan Implementation','Report on ECB Adjudications of Summonses Issued to Vendors','Project Initiation, Commitment Plan','Report on Proposed Scope of Projects']
    };

    // INITIAL LOAD
    requiredReport.empty();
    // Add blank option
    requiredReport.append(new Option('', ''));
    if (agency.val() in requiredReports) {
        requiredReports[agency.val()].forEach(function (report) {
            requiredReport.append(new Option(report, report));
        });
    }
    // Add Not Required option
    requiredReport.append(new Option('Not Required', 'Not Required'));
    requiredReport.prop('disabled', false);

    // ON AGENCY CHANGE
    agency.change(function () {
        var selectedAgency = agency.val();
            if (selectedAgency !== '') {
                requiredReport.empty();
                // Add blank option
                requiredReport.append(new Option('', ''));
                if (selectedAgency in requiredReports) {
                    requiredReports[selectedAgency].forEach(function (report) {
                        requiredReport.append(new Option(report, report));
                    });
                }
                // Add Not Required option
                requiredReport.append(new Option('Not Required', 'Not Required'));
                requiredReport.prop('disabled', false);
            } else {
                requiredReport.empty();
                requiredReport.prop('disabled', true);
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