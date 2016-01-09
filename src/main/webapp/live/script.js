/**
 * Scripts used for Live Dashboard
 *
 * Created by tpaulus on 1/5/16.
 */

const refreshRate = 2500; // Refresh event list every X milliseconds
const flashDuration = 4000; // How long a notified row should flash
var notifyChime = new Audio("../live/Alert.mp3");

var latestEvent = 0;
var ready = false;

window.onload = function start() {
    loadEvents();
};

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
    get("live/getEvents");
    window.setInterval(function () {
        if (ready) {
            get("live/getEvents");
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

    xmlHttp.open('GET', url + "?last=" + latestEvent);
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