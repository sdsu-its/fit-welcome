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
            // TODO Save Response User for final event submission

            console.log(resp);
            sweetAlert("Schweet!", "You coo!", "success");
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

    $("#idEntryForm")[0].reset();
}