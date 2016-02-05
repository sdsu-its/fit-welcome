/**
 * Scripts used for Live Dashboard
 *
 * Created by tpaulus on 1/5/16.
 */

const refreshRate = 2500; // Refresh event list every X milliseconds
const flashDuration = 4000; // How long a notified row should flash
var notifyChime = new Audio("Alert.mp3");

var latestEvent = 0;

var loggedIn = false;
var userID = 0;
var ready = false;

window.onload = function () {
    // Select the ID Input Area automatically on NON-iPads
    var ua = navigator.userAgent;
    var idBox = document.getElementById('userID');

    if (!ua.toLowerCase().indexOf("iPad".toLowerCase()) > -1) {
        idBox.focus();
        idBox.select();
    }
};

/**
 * OnSubmit Action
 * @returns {boolean} Always false to prevent refresh
 */
function login() {
    userID = document.getElementById("userID").value;
    checkLogin(userID);

    return false; // Used to not change page
}

/**
 * Check the user's ID
 *
 * @param userID {int} User's Supplied ID
 */
function checkLogin(userID) {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = xmlHttp;
            console.log(response.status);
            if (response.status != 403) {
                setLogIn();
                loadEvents();
            }
            else {
                setBadCred();
            }
        }
    };

    xmlHttp.open('GET', "../pages/live/getEvents?id=" + userID);
    xmlHttp.send();
}

/**
 * Update document to display successful login
 */
function setLogIn() {
    if (!loggedIn) {
        document.getElementById("login").style.display = "none";
        document.getElementById("badCred").style.display = "none";
        document.getElementById("loading").style.visibility = "visible";
        loggedIn = true;
    }
}

/**
 * Update document to display non-successful login
 */
function setBadCred() {
    document.getElementById("badCred").style.display = "";
    document.getElementById("userID").value = "";
}

/**
 * Page is Fully Loaded, display contents
 */
function setReady() {
    if (!ready) {
        document.getElementById("loading").style.display = "none";
        document.getElementById("events").style.visibility = "visible";
        ready = true;
    }
}

/**
 * Load Recent Events
 */
function loadEvents() {
    get("../pages/live/getEvents");
    window.setInterval(function () {
        if (ready) {
            get("../pages/live/getEvents");
        }
    }, refreshRate);
}

/**
 * Notify this event row
 * @param rowId (int) Row to Flash - Commonly the Event ID
 */
function notify(rowId) {
    notifyChime.play();
    flashRow(rowId);
}

/**
 * Flash a Row
 *
 * @param rowId (int) Row to Flash - Commonly the Event ID
 */
function flashRow(rowId) {
    var row = document.getElementById("e-" + rowId);
    row.classList.add("flashing");

    window.setInterval(function () {
        row.classList.remove("flashing");
    }, flashDuration)
}

/**
 * Make an HTTP GET Request
 *
 * @param url (String) URL to which the request should be made
 */
function get(url) {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            var response = JSON.parse(xmlHttp.responseText);
            console.info("Last Fetch Returned " + response.length + " events");

            for (var i = 0; i < response.length; i++) {
                var obj = response[i];
                console.log(obj);

                if (latestEvent < obj.id) {
                    latestEvent = obj.id;
                }

                insert(obj.id, obj.owner.firstName + ' ' + obj.owner.lastName, obj.timeString, obj.type, obj.params);

                if (obj.notify && ready) {
                    notify(obj.id);
                }
            }

            setReady()
        }
    };

    xmlHttp.open('GET', url + "?id=" + userID + "&last=" + latestEvent);
    xmlHttp.send();
}

/**
 * Insert a New Event into the Event's Table
 *
 * @param eventID (int) Event ID
 * @param name (String) User's Name
 * @param time (String) Check In Time
 * @param goal (String) User's Goal for visit
 * @param params (String) Parameters for Visit
 */
function insert(eventID, name, time, goal, params) {
    var table = document.getElementById("events");
    var row = table.insertRow(1);
    row.id = "e-" + eventID;

    row.insertCell(0).innerHTML = name;
    row.insertCell(1).innerHTML = time;
    row.insertCell(2).innerHTML = goal;
    row.insertCell(3).innerHTML = params;

}