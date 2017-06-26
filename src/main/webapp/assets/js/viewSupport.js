/**
 * Support the Client UI (Animations, Fill dynamic content, etc.)
 *
 * Created by tom on 6/24/17.
 */

/**
 * Show Visit Type Card (Appointment vs. Walk in)
 */
function showTypeSelect() {
    $("div.card").fadeOut(400, function () {
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
    $("div.card").fadeOut(400, function () {
        $("div.card.appointment-list").parent().fadeIn(400);
    });
}

function showIDEntry(e) {
    $("div.card").fadeOut(400, function () {
        $("div.card.id-entry").parent().fadeIn(400);
    });
}

function showGoalList() {

}

/**
 * Reset the UI if the session has expired
 */
function reset() {
    if (!$("div.card.welcome").is(":visible")) {
        $("div.card").parent().fadeOut(400, function () {
            $("div.card.welcome").parent().fadeIn(400);
        });
    }
}