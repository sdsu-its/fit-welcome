/**
 * Support the Client UI (Animations, Fill dynamic content, etc.)
 *
 * Created by tom on 6/24/17.
 */

/**
 * Show Visit Type Card (Appointment vs. Walk in)
 */
function showTypeSelect() {
    $("div.card:not(.type)").parent().fadeOut(400, function () {
        $("div.card.type").parent().fadeIn(400);
    });

    loadAppointmentList();
}

/**
 * Load Upcoming appointments from the Welcome API
 */
function loadAppointmentList() {

}

/**
 * Show list of upcoming appointments
 */
function showAppointmentList(e) {
    $("div.card:not(.appointment-list)").parent().fadeOut(400, function () {
        $("div.card.appointment-list").parent().fadeIn(400);
    });
}

function showIDEntry(e) {
    $("div.card:not(.id-entry)").parent().fadeOut(400, function () {
        $("div.card.id-entry").parent().fadeIn(400);
    });
    $('#idBox').focus();
}

function showGoalList() {
    hideKeyboard();
    $("div.card:not(.goal-list)").parent().fadeOut(400, function () {
        $("div.card.goal-list").parent().fadeIn(400);
    });
}

/**
 * Reset the UI if the session has expired
 */
function reset() {
    if (!$("div.card.welcome").is(":visible")) {
        $("div.card:not(.welcome)").parent().fadeOut(400, function () {
            $("div.card.welcome").parent().fadeIn(400);
        });
        hideKeyboard();
        swal.close();
        $("#idEntryForm")[0].reset();
    }
}

function hideKeyboard() {
    document.activeElement.blur();
}