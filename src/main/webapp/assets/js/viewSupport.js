/**
 * Support the Client UI (Animations, Fill dynamic content, etc.)
 *
 * Created by tom on 6/24/17.
 */

var hist = [reset];

function addToHistory(pageFunction) {
    hist.push(pageFunction);
}

function previous() {
    hist.pop(); // Remove the Last Function from the stack, since it is for the current page
    hist.pop()(); // Call the last display function called
}

/**
 * Show Visit Type Card (Appointment vs. Walk in)
 */
function showTypeSelect() {
    $("div.card:not(.type)").parent().fadeOut(400, function () {
        $("div.card.type").parent().fadeIn(400);
    });

    // Load Appointment List and Final Quote in the background when an interaction starts
    loadAppointmentList();
    loadQuote();
    addToHistory(showTypeSelect);
}

/**
 * Show list of upcoming appointments
 */
function showAppointmentList() {
    if (sessionStorage.getItem("appointments").length > 0) {
        $("div.card:not(.appointment-list)").parent().fadeOut(400, function () {
            $("div.card.appointment-list").parent().fadeIn(400);
        });
        addToHistory(showAppointmentList);
    } else {
        // If there are no upcoming appointments, treat it as a "Not-Found" situation
        apptNotFound();
    }
}

/**
 * If the client indicates that their appointment cannot be found, note their condition and
 * treat them like a non-appointment user. We will check again on the backend for a matching event.
 */
function apptNotFound() {
    appointmentMade = true;
    showIDEntry();
}

/**
 * Show Appointment Detail Page. Prompting the user to confirm the appointment that they have selected.
 * @param e Selected Appointment Row in the #appointment-list
 */
function showAppointmentDetail(e) {
    $('.info-parent .appt-time').html($(e).data("time"));
    $('.info-parent .appt-type').html($(e).data("type"));
    $('.info-parent .appt-own-name').html($(e).data("name"));
    $('.info-parent .appt-own-id').html(String($(e).data("owner-id")).replace(/\*/g, "&bull;"));
    $('.appointment-detail .finish-button').data("appt-id", $(e).data("appt-id"));

    $("div.card:not(.appointment-detail)").parent().fadeOut(400, function () {
        $("div.card.appointment-detail").parent().fadeIn(400, function () {
        });
    });
    addToHistory(showAppointmentDetail);
}

/**
 * Show ID Entry Prompt
 */
function showIDEntry() {
    $("div.card:not(.id-entry)").parent().fadeOut(400, function () {
        $("div.card.id-entry").parent().fadeIn(400, function () {
            $('#idBox').focus();
        });
    });
    addToHistory(showIDEntry);
}

/**
 * Show Initial list of all Goals
 */
function showGoalList() {
    $("#back-button, div.card:not(.goal-list)").parent().fadeOut(400, function () {
        $("div.card.goal-list").parent().fadeIn(400);
        hideKeyboard();
        $("#idEntryForm")[0].reset();
    });
    addToHistory(showGoalList);
}

/**
 * Show Final Confirmation Card. Includes Quote.
 */
function showFinalConfirmation() {
    $("#back-button, div.card:not(.confirmation)").parent().fadeOut(400, function () {
        $("div.card.confirmation").parent().fadeIn(400);
    });
    addToHistory(showFinalConfirmation);

    // Show final message for 10 seconds, before resetting the page
    setTimeout(reset, 10000);
}

/**
 * Reset the UI if the session has expired
 */
function reset() {
    if (!$("div.card.welcome").is(":visible")) {
        $("#back-button, div.card:not(.welcome)").parent().fadeOut(400, function () {
            $("div.card.welcome").parent().fadeIn(400);
        });
        hideKeyboard();
        swal.close();
        $("#idEntryForm")[0].reset();
    }
    appointmentMade = false;
    sessionStorage.clear();
    hist = [reset];
}

function hideKeyboard() {
    document.activeElement.blur();
}

//noinspection JSUnusedGlobalSymbols
/**
 * Show a Goal Page via its name. Used by buttons in the sitemap.json file.
 *
 * @param pageName Page name to display
 */
function showPage(pageName) {
    $("div.card:not(.goal-" + pageName.toLowerCase() + ")").parent().fadeOut(400, function () {
        $("#back-button, div.card.goal-" + pageName.toLowerCase()).parent().fadeIn(400);
    });

    addToHistory(function () {
        showPage(pageName.toLowerCase())
    });
}

/**
 * Prepare and Show Appointment Scheduler Card.
 * Uses Acuity Provided iframe.
 *
 * @param appointmentID Appointment ID being booked
 */
function scheduler(appointmentID) {
    var user = JSON.parse(sessionStorage.getItem("user"));

    $("#scheduler")[0].src = "https://fitcenter.acuityscheduling.com/schedule.php?appointmentType={{AID}}&first_name={{FIRST}}&last_name={{LAST}}&email={{EMAIL}}&field:2114596={{REDID}}"
        .replace("{{AID}}", appointmentID)
        .replace("{{FIRST}}", user.firstName)
        .replace("{{LAST}}", user.lastName)
        .replace("{{EMAIL}}", user.email)
        .replace("{{REDID}}", user.id);
    showPage("acuity-scheduler");
}

//noinspection JSUnusedGlobalSymbols
/**
 * Log an event. Used by buttons in the sitemap.json file.
 *
 * @param goal Visit Goal
 * @param params Visit Params
 */
function finish(goal, params) {
    logEvent(goal, params);
}