/**
 * Interact with the Welcome API to verify IDs and Log Events
 *
 * Created by tom on 6/24/17.
 */

var appointmentMade = false;

/**
 * Verify entered ID
 */
function verifyID() {
    var val = $("#idBox").val();

    if (val === "") return false; // Skip if field is blank

    $.ajax({
        method: "GET",
        url: "api/client/login?id=" + val
    })
        .done(function (resp) {
            sessionStorage.setItem("user", JSON.stringify(resp));
            showGoalList();
        })
        .fail(function (resp) {
            if (resp.status === 404) {
                sweetAlert("Oops...", "We can't find that ID in our Database", "error");
            } else {
                sweetAlert("Shoot!", "Something has gone awry! Please let a staff member know.", "warning");
                console.warn(resp);
            }
        });
}

/**
 * Load Upcoming appointments from the Welcome API
 */
function loadAppointmentList() {
    $.ajax({
        method: "GET",
        url: "api/client/appointments"
    })
        .done(function (resp) {
            var $table = $('#appointment-list').find('tbody');
            $table.empty();
            for (var a = 0; a < resp.length; a++) {
                var appointment = resp[a];
                var row = $table[0].insertRow(-1);
                row.id = "appointment-" + appointment.id;
                row.insertCell(0).outerHTML = "<th class='row'>" + appointment.time + "</th>";
                row.insertCell(1).innerHTML = appointment.name;
                row.insertCell(2).innerHTML = appointment.type;
                $(row).addClass("appointment");
                $(row).data("appt-id", appointment.id);
                $(row).data("time", appointment.time);
                $(row).data("name", appointment.name);
                $(row).data("type", appointment.type);
                $(row).data("owner-id", appointment.ownerId);
            }

            $('.appointment').click(function () {
                showAppointmentDetail(this);
            });

            sessionStorage.setItem("appointments", resp);
        })
        .fail(function (resp) {
            sweetAlert("Shoot!", "Something has gone awry! Please let a staff member know.", "warning");
            console.warn(resp);
            sessionStorage.setItem("appointments", []);
        });
}

/**
 * Get a Random quote from the API to display on confirmation.
 */
function loadQuote() {
    $.ajax({
        method: "GET",
        url: "api/quote"
    })
        .done(function (resp) {
            $('#quoteText').html(resp.text);
            $('#quoteAuthor').html(resp.author);
        })
        .fail(function (resp) {
            console.warn("Problem getting Quote");
            console.log(resp);
        })
}

/**
 * Log an event to the API
 *
 * @param goal {@link String} Client Goal
 * @param params {@link String} Visit Params
 */
function logEvent(goal, params) {
    var payload = {
        "owner": JSON.parse(sessionStorage.getItem("user")),
        "appointmentMade": appointmentMade,
        "goal": goal,
        "params": params,
        "locale": "DEFAULT" // fixme
    };

    postEvent(payload);
}

function logAppointment(e) {
    var appointmentID = $(e).data("appt-id");
    appointmentMade = true;

    var payload = {
        "appointmentMade": appointmentMade,
        "appointmentId": appointmentID,
        "locale": "DEFAULT" // fixme
    };

    postEvent(payload);
}

function postEvent(payload) {
    $.ajax({
        method: "POST",
        url: "api/client/event",
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function () {
            showFinalConfirmation();
        })
        .fail(function (resp) {
            sweetAlert("Shoot!", "Something has gone awry! Please let a staff member know.", "warning");
            console.warn(resp);
        });
}