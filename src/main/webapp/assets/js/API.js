/**
 * Interact with the Welcome API to verify IDs and Log Events
 *
 * Created by tom on 6/24/17.
 */

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
        "type": goal,
        "params": params,
        "locale": "DEFAULT" // fixme
    };

    $.ajax({
        method: "POST",
        url: "api/client/event",
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function (resp) {
            showFinalConfirmation();
        })
        .fail(function (resp) {
            sweetAlert("Shoot!", "Something has gone awry! Please let a staff member know.", "warning");
            console.warn(resp);
        });
}