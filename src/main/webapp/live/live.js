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

var events = [];
var SSEsource = null;

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
    getPastEvents(userID);
    return false; // Used to not change page
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
 * Load Events via Server Sent Events
 */
function loadEvents() {
    SSEsource = new EventSource("../api/live/stream");
    SSEsource.onmessage = function (event) {
        console.log(event);
        var obj = JSON.parse(event.data);

        insert(obj.id, obj.owner.firstName + ' ' + obj.owner.lastName, obj.timeString, obj.type, obj.params);

        if (obj.notify && ready) {
            notify(obj.id);
        }
    };
}

/**
 * Check if the Stream is closed, and if so, recreate and open it.
 */
function checkStream() {
    if (SSEsource.readyState == 2) {  // CLOSED == 2
        loadEvents();
    }
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
 * Get Historical Events. The number is determined by the Server.
 * This also checks if the user has entered a valid ID, which is supplied as a param.
 *
 * @param userID (String) User ID, must be listed as Staff in the DB
 */
function getPastEvents(userID) {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            if (xmlHttp.readyState == 4) {
                var response = xmlHttp;
                console.log(response.status);
                if (response.status != 403) {
                    setLogIn();

                    var responseJSON = JSON.parse(xmlHttp.responseText);
                    console.info("Server returned " + response.length + " events");
                    for (var i = 0; i < responseJSON.length; i++) {
                        var obj = responseJSON[i];
                        console.log(obj);

                        if (latestEvent < obj.id) {
                            latestEvent = obj.id;
                        }

                        insert(obj.id, obj.owner.firstName + ' ' + obj.owner.lastName, obj.timeString, obj.type, obj.params);

                        if (obj.notify && ready) {
                            notify(obj.id);
                        }
                    }


                    loadEvents();
                    window.setInterval(checkStream, refreshRate);
                    setReady();

                }
                else {
                    setBadCred();
                }
            }
        }
    };

    xmlHttp.open('GET', "../api/live/getEvents" + "?id=" + userID);
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
    if (events.indexOf(eventID) != -1) {
        console.log("Event Already Added to Page");
    } else {
        var table = document.getElementById("events");
        var row = table.insertRow(1);
        row.id = "e-" + eventID;

        row.insertCell(0).innerHTML = name;
        row.insertCell(1).innerHTML = time;
        row.insertCell(2).innerHTML = goal;
        row.insertCell(3).innerHTML = params;

        events.push(eventID);
    }
}