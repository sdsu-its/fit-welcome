/**
 * Created by tom on 4/6/17.
 */

window.onload = function () {
    loadView("index");
};

function showHome() {
    if (user !== null) showPage("welcome", "menu-home");
    else showPage("index", "menu-home");
}

function loadView(viewName) {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState === 4) {
            $('#view-container').html(xmlHttp.responseText);
        }
    };

    xmlHttp.open('get', "views/" + viewName + ".view", true);
    xmlHttp.send();
}

function loadChildView(childName) {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState === 4) {
            $('#view-container-child').html(xmlHttp.responseText);
        }
    };

    xmlHttp.open('get', "views/" + pageHistory[pageHistory.length - 1] + "-" + childName + ".view", true);
    xmlHttp.send();
}

function showPage(pageName, navMenuID) {
    // Will only work if the user is logged in.
    if (pageName === "index" || user !== null && pageName !== pageHistory[pageHistory.length - 1]) {
        loadView(pageName);
        pageHistory[pageHistory.length] = pageName;

        if (navMenuID !== null) updateNav(navMenuID);
    }
}

function updateNav(newActivePage) {
    $('#navbar').find('.active').removeClass('active');
    $('#' + newActivePage).addClass('active');
}