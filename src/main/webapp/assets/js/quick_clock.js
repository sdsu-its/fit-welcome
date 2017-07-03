/**
 * Created by tom on 7/2/17.
 */

/**
 * Verify entered ID
 */
function verifyID() {
    var val = $("#idBox").val();

    if (val === "") return false; // Skip if field is blank

    $.ajax({
        method: "GET",
        url: "api/clock/status?id=" + val
    })
        .done(function (resp) {
            sessionStorage.setItem("id", val);

            if (resp.toLowerCase() === 'true') {
                // Currently Clocked IN
                $('#clock-status').text("IN");
                $('#direction').text("OUT");
            } else {
                // Currently Clocked OUT
                $('#clock-status').text("OUT");
                $('#direction').text("IN");
            }

            $("div.card:not(.action)").parent().fadeOut(400, function () {
                $("#back-button, div.card.action").parent().fadeIn(400);
            });
        })
        .fail(function (resp) {
            if (resp.status === 406) {
                sweetAlert("Oops...", "The ID entered is not allowed to use this tool.", "error");
            } else {
                sweetAlert("Shoot!", "Something has gone awry! Please let an admin know.", "warning");
                console.warn(resp);
            }
        });
}

function toggleClock() {
    // TODO Show Loading Indicator

    $.ajax({
        method: "GET",
        url: "api/clock/toggle?id=" + sessionStorage.getItem("id")
    })
        .done(function (resp) {
            if (resp.toLowerCase() === 'true') {
                // Was Clocked In
                $('#clock-result').text("In");
            } else {
                // Was Clocked Out
                $('#clock-result').text("Out");
            }

            $("div.card:not(.confirmation)").parent().fadeOut(400, function () {
                $("#back-button, div.card.confirmation").parent().fadeIn(400);
                setTimeout(reset, 5000);
            });
        })
        .fail(function (resp) {
            if (resp.status === 406) {
                sweetAlert("Oops...", "The ID entered is not allowed to use this tool.", "error");
            } else {
                sweetAlert("Shoot!", "Something has gone awry! Please let an admin know.", "warning");
                console.warn(resp);
            }
        })
        .always(function () {
            sessionStorage.clear();
        });
}

function hideKeyboard() {
    document.activeElement.blur();
}

function reset() {
    $("#back-button, div.card:not(.welcome)").parent().fadeOut(400, function () {
        $("div.card.welcome").parent().fadeIn(400);
    });

    sessionStorage.clear();
    $("#idBox").val("");
}