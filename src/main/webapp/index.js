/**
 * TODO Docs
 *
 * Created by tpaulus on 3/27/16.
 */
var user = null;
var appointment = null;

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
    
    setQuote();
};

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
        appointment = login.appointment;
        document.getElementById("appointmentType").innerHTML = appointment.type;
        document.getElementById("appointmentTime").innerHTML = appointment.time;

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
    if (pageName != "loading" && pageHistory[pageHistory.length - 1] != pageName) {
        pageHistory.push(pageName);
    }

    if (pageHistory.length > 2 && pageName != "loading" && pageName != "login" && pageName != "conf") {
        showFooter();
    } else {
        hideFooter();
    }
}

function showFooter() {
    document.getElementById("footerWrapper").style.display = "";
}

function hideFooter() {
    document.getElementById("footerWrapper").style.display = "none";
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
    var notice = "Let us know if there is anything we can<br>" +
        "do to make your visit more productive!";

    if (goal == "Use ParScore") {
        if (appointment == null) {
            param = "Walk In";
            notice = "ParScore Scanning is in High Demand!</ br> We recommend that you schedule an appointment ahead of time. <br><br>" +
                "Please check with the FIT Center Consultant regarding machine availability.";
        }
        else {
            param = "Appointment ID: " + appointment.id;
        }
    } else if (goal == "Meet an ID") {
        notice = "A FIT Consultant will be with you shortly!<br>";
    }

    var json = '{' +
        '"owner": {"id": ' + user.id + '},' +
        '"type": "' + goal + '"';

    if (param != null) {
        json += ',"params": "' + param + '"';
    }
    json += '}';

    console.log(json);

    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 201) {
                console.log(response.responseText);
                doFinish('Sounds Good ' + user.firstName + '!', notice);
            }
            else {
                alert("Problem Saving Event");
                doFinish('Sounds Good ' + user.firstName + '!', notice);
            }
        }
    };

    xmlHttp.open('POST', "api/event");
    xmlHttp.setRequestHeader("Content-type", "application/json");
    xmlHttp.send(json);
}

function doFinish(confMessage, notice) {
    document.getElementById("confMessage").innerHTML = confMessage;
    document.getElementById("confNote").innerHTML = notice;

    showPage("conf");
    pageHistory = [];
    user = null;
    document.getElementById("idBox").value = "";

    window.setTimeout(function () {
        showPage("login");
    }, 10000);
}

function scheduler(appointmentID) {
    // TODO
}

function getQuote() {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 200) {
                var quoteJSON = JSON.parse(response.responseText);
                console.log(quoteJSON);
                setQuote(quoteJSON.author, quoteJSON.text);
            }
        }
    };

    xmlHttp.open('GET', "api/quote");
    xmlHttp.send();
}

function setQuote(author, text) {
    if (getCookie("quoteAuthor") == "" || getCookie("quoteText") == "") {
        if (author == null || text == null) {
            getQuote();
        }
        else {
            setCookie("quoteAuthor", author, getMidnight());
            setCookie("quoteText", text, getMidnight());
        }
    }

    if (author == null) author = getCookie("quoteAuthor");
    if (text == null) text = getCookie("quoteText");

    document.getElementById("quoteAuthor").innerHTML = author;
    document.getElementById("quoteText").innerHTML = text;
}

function back() {
    pageHistory.pop(); // Remove the current page from the history
    showPage(pageHistory.pop()); // Go to the previous page, the removal is used because showPage will add it back.
}