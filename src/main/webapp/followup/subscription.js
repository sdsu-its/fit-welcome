/**
 * Manages Follow Up email Subscriptions
 *
 * Created by tpaulus on 4/1/16.
 */

window.onload = function() {
    unsubscribe(getParameterByName("email"));
};

function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));

    // ex: var foo = getParameterByName('foo');
}

function unsubscribe(email) {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 202) {
                var user = JSON.parse(response.responseText);
                console.log(user);
                doLoadMessage(user);
            }
            else {
                document.getElementById("loading").style.display = "none";
                document.getElementById("not-found").style.display = "";
            }

        }
    };

    xmlHttp.open('GET', "../api/followup/unsubscribe?email=" + email);
    xmlHttp.send();
}

function doLoadMessage(json) {
    for (var f = 0; f < document.getElementsByClassName("firstName").length; f++) {
        var firstField = document.getElementsByClassName("firstName")[f];
        firstField.innerHTML = json.firstName;
    }
    for (var g = 0; g < document.getElementsByClassName("lastName").length; g++) {
        var lastField = document.getElementsByClassName("lastName")[g];
        lastField.innerHTML = json.lastName;
    }

    document.getElementById("loading").style.display = "none";
    document.getElementById("unsubscribe").style.display = "";
}

function subscribe() {
    var xmlHttp = new XMLHttpRequest();
    var email = document.getElementById("email").value;

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status == 202) {
                var user = JSON.parse(response.responseText);
                console.log(user);
                doLoadSubscribeMessage(user);
            }
            else {
                document.getElementById("unsubscribe").style.display = "none";
                document.getElementById("not-found").style.display = "";
            }

        }
    };

    xmlHttp.open('POST', "../api/followup/subscribe?email=" + email);
    xmlHttp.send();
}

function doLoadSubscribeMessage(json) {
    for (var f = 0; f < document.getElementsByClassName("firstName").length; f++) {
        var firstField = document.getElementsByClassName("firstName")[f];
        firstField.innerHTML = json.firstName;
    }
    for (var g = 0; g < document.getElementsByClassName("lastName").length; g++) {
        var lastField = document.getElementsByClassName("lastName")[g];
        lastField.innerHTML = json.lastName;
    }
    
    document.getElementById("unsubscribe").style.display = "none";
    document.getElementById("subscribe").style.display = "";
}