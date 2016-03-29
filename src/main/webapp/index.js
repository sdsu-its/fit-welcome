/**
 * TODO Docs
 *
 * Created by tpaulus on 3/27/16.
 */
var user = null;

var currentPageID = "login";
var pageHistory = ["login"];

window.onload = function () {
    var ua = navigator.userAgent;
    var entryMethod = document.getElementById("entryMethod");
    var idBox = document.getElementById("idBox");

    if (ua.toLowerCase().indexOf("iPad".toLowerCase()) > -1) {
        entryMethod.innerHTML = "Please Type in your REDID<br>" +
            "to get started, then press go.";
    } else {
        idBox.focus();
        idBox.select();
    }
}

function login() {
    var userID = document.getElementById("idBox").value;
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 200) {
                var login = JSON.parse(response.responseText);
                console.log(login);
                doLogin(login);
            }
            else if (response.status == 404) {
                loginError();
            }

        }
    };

    xmlHttp.open('GET', "api/login?id=" + userID);
    xmlHttp.send();
}

function doLogin(login) {
    user = login.user;

    for (var spanNum = 0; spanNum < document.getElementsByClassName("firstName").length; spanNum++) {
        var span = document.getElementsByClassName("firstName")[spanNum];
        span.innerHTML = user.firstName;
    }

    if (login.appointment != null) {
        document.getElementById("appointmentType").innerHTML = login.appointment.type;
        document.getElementById("appointmentTime").innerHTML = login.appointment.time;

        showPage("appointment")
    } else if (login.isStaff) {
        if (user.clockable) {
            if (user.admin) {
                document.getElementById("admin").style.display = "";
            }
            showPage('staff');
            loadClock();
        } else if (user.admin) {
            showAdmin();
        } else {
            showPage("index");
        }
    }
    else {
        showPage("index");
    }
}

function loginError() {
    document.getElementById('idBox').value = '';
    showPage('login');
    alert("We can't find that ID in our records!\n" +
        "Please try again.");
}

function showPage(pageName) {
    document.getElementById(currentPageID).style.display = "none";
    document.getElementById(pageName).style.display = "";

    currentPageID = pageName;
    if (pageName != "loading") {
        pageHistory.push(pageName);
    }
}

function loadClock() {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 200) {
                console.log(response.responseText);
                doLoadClock(response.responseText);
            }
            else {
                alert("Problem Loading Clock\n" +
                    "Check Console for Errors");
            }
        }
    };

    xmlHttp.open('GET', "api/clock/status?id=" + user.id);
    xmlHttp.send();
}

function doLoadClock(clockStatus) {
    document.getElementById("clockStatus").innerHTML = clockStatus ? "Clocked In" : "Clocked Out";
    document.getElementById("clockText").style.display = "";

    var clockButton = document.getElementById("clock");
    clockButton.className = ""; // Remove Disabled Class

    clockButton.innerHTML = clockStatus ? "Clock OUT" : "Clock IN";
    clockButton.disabled = false; // Enable the Button
}

function toggleClock() {
    alert("Honk!");
    // TODO
}

function showAdmin() {
    // Check User
    // TODO
}

function finish(goal, param) {
    // TODO
}

function scheduler(appointmentID) {
    // TODO
}

function back() {
    // TODO
}