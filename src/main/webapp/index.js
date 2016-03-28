/**
 * TODO Docs
 *
 * Created by tpaulus on 3/27/16.
 */
var currentPageID = "login";

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
    for (var spanNum = 0; spanNum < document.getElementsByClassName("firstName").length; spanNum++) {
        var span = document.getElementsByClassName("firstName")[spanNum];
        span.innerHTML = login.user.firstName;
    }

    if (login.appointment != null) {
        document.getElementById("appointmentType").innerHTML = login.appointment.type;
        document.getElementById("appointmentTime").innerHTML = login.appointment.time;
    } else {
        showPage("index");
    }
}

function loginError() {
    alert("Credentials Incorrect");
    // TODO Display Error Message On Screen
}

function showPage(pageName) {
    document.getElementById(currentPageID).style.display = "none";
    document.getElementById(pageName).style.display = "";

    currentPageID = pageName;
    return false;
}

// TODO functions (finish, scheduler)