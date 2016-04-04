/**
 * Manage Admin Functions
 *
 * Created by tpaulus on 3/30/16.
 */

var clockableStaff = [];
var clockableUsersSet = false;

function staffByID(id) {
    for (var s = 0; s < clockableStaff.length; s++) {
        var staff = clockableStaff[s];
        if (staff.id == id) {
            return staff;
        }
    }
    return null;
}

function getClockableStaff() {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 200) {
                clockableStaff = JSON.parse(response.responseText);
                console.log(clockableStaff);
                showTimeEntry();
            } else {
                doFinish("An Error Occurred processing your request.", "");
            }
        }
    };

    xmlHttp.open('GET', "api/admin/clockableStaff");
    xmlHttp.setRequestHeader("REQUESTER", user.id);
    xmlHttp.send();
}

function showTimeEntry() {
    if (clockableStaff.length == 0) {
        clockableUsersSet = false;
        getClockableStaff();
    }
    if (!clockableUsersSet) {
        var select = document.getElementById('clockableUsers');
        for (var s = 0; s < clockableStaff.length; s++) {
            var staff = clockableStaff[s];
            var opt = document.createElement('option');
            opt.value = staff.id;
            opt.innerHTML = staff.lastName + ", " + staff.firstName;
            select.appendChild(opt);
        }
        clockableUsersSet = true;
        showPage('manualTime');
    }
}

function addTimeEntry() {
    var json = '{' +
        '"user": {' +
        '"id": ' + document.getElementById("clockableUsers").value +
        '},' +
        '"date": "' + document.getElementById("clockTime").value + '",' +
        '"direction": ' + document.getElementById("clockAction").value +
        '}';

    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 201) {
                doFinish("You have successfully " + (document.getElementById("clockAction").value == "true" ? "Clocked In" : "Clocked Out")
                    + " " + staffByID(document.getElementById("clockableUsers").value).firstName, "");
            } else {
                doFinish("An Error Occurred processing your request.", "");
            }
            document.getElementById("manual-time-form").reset();

        }
    };

    xmlHttp.open('POST', "api/admin/timeEntry");
    xmlHttp.setRequestHeader("REQUESTER", user.id);
    xmlHttp.setRequestHeader("Content-type", "application/json");
    xmlHttp.send(json);
}

//noinspection JSUnusedGlobalSymbols
function clockOutAll() {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 202) {
                doFinish("You have successfully Clocked Out all Staff Users", "");
            } else {
                doFinish("An Error Occurred processing your request.", "");
            }
        }
    };

    xmlHttp.open('POST', "api/admin/clockOutAll");
    xmlHttp.setRequestHeader("REQUESTER", user.id);
    xmlHttp.send();
}

function runTimesheetReport() {
    var startDate = document.getElementById("ts-start").value;
    var endDate = document.getElementById("ts-end").value;
    var individual = document.getElementById("ts-type").value;

    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 200) {
                doFinish("Your report is being run and will be emailed when complete", "");
            }
            else {
                doFinish("An Error Occurred processing your request.", "");
            }
            document.getElementById("timesheet-report-form").reset();
        }
    };

    xmlHttp.open('GET', "api/admin/timesheetReport?startDate=" + startDate + "&endDate=" + endDate + "&individual=" + individual);
    xmlHttp.setRequestHeader("REQUESTER", user.id);
    xmlHttp.send();
}

function runUsageReport() {
    var startDate = document.getElementById("ur-start").value;
    var endDate = document.getElementById("ur-end").value;

    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 200) {
                doFinish("Your report is being run and will be emailed when complete", "");
            }
            else {
                doFinish("An Error Occurred processing your request.", "");
            }
            document.getElementById("usage-report-form").reset();
        }
    };

    xmlHttp.open('GET', "api/admin/usageReport?startDate=" + startDate + "&endDate=" + endDate);
    xmlHttp.setRequestHeader("REQUESTER", user.id);
    xmlHttp.send();
}

function manualVisit() {
    altUser = document.getElementById("visit-user").value;
    altTime = document.getElementById("visit-date").value;
    document.getElementById("manual-visit-form").reset();

    backDateMode = true;

    doLogin();
}

function newStaff() {
    var json = '{' +
        '"id": "' + document.getElementById("ns-id").value + '",' +
        '"firstName": "' + document.getElementById("ns-first").value + '",' +
        '"lastName": "' + document.getElementById("ns-last").value + '",' +
        '"email": "' + document.getElementById("ns-email").value + '",' +
        '"clockable": ' + document.getElementById("ns-clockable").value + ',' +
        '"admin": ' + document.getElementById("ns-admin").value + ',' +
        '"instructional_designer": ' + document.getElementById("ns-designer").value +
        '}';

    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 201) {
                doFinish("Created New Staff Account for " + document.getElementById("ns-first").value + " successfully!", "");
            } else {
                doFinish("An Error Occurred processing your request.", "");
            }

            document.getElementById("new-staff-form").reset();
        }
    };

    xmlHttp.open('POST', "api/admin/newStaff");
    xmlHttp.setRequestHeader("Content-type", "application/json");
    xmlHttp.setRequestHeader("REQUESTER", user.id);
    xmlHttp.send(json);
}