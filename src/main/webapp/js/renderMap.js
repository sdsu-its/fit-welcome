/**
 * Render a JSON Document to a set of Button Panels
 *
 * Created by tpaulus on 3/27/16.
 */

const buttonTemplate = '<button class="panelButton" type="button" onclick="{{action}}">{{text}}</button>';
var json;

const defaultLocale = "FIT";

function getLocale() {
    var locale = getParameterByName("locale");
    if (!locale || locale.length < 1) {
        locale = getCookie("locale");
        if (!locale || locale.length < 1) {
            console.warn("Using the Default Locale - Consider setting via \"?locale=\"");
            console.log("Default Locale: " + defaultLocale);
            locale = defaultLocale;
        }
    }
    return locale;
}

function getMapJSON() {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            json = JSON.parse(xmlHttp.responseText);
            updateIndex(getLocale(), json);
            renderMap(json);
        }
    };

    xmlHttp.open('GET', "locales/" + getLocale() + "/sitemap.json");
    xmlHttp.send();
}

function getType(p) {
    if (Array.isArray(p)) return 'array';
    else if (typeof p == 'string') return 'string';
    else if (p != null && typeof p == 'object') return 'object';
    else return 'other';
}

function renderMap(json) {
    for (var pageName in json) {
        if (!json.hasOwnProperty(pageName) || getType(json[pageName]) == 'string') {
            //The current property is not a direct property of json
            continue;
        }
        makePage(pageName, json[pageName]);
    }
}

function updateIndex(locale, json) {
    document.title = json.title;

    var heads = document.getElementsByClassName("mainHead");
    for (var h = 0; h < heads.length; h++) {
        var head = heads[h];
        head.innerHTML = json.mainHead;
    }

    var touchIconLink = document.createElement('link');
    touchIconLink.type = 'image/x-icon';
    touchIconLink.rel = 'apple-touch-icon';
    touchIconLink.href = "locales/" + locale + "/apple-touch-icon.png";
    document.getElementsByTagName('head')[0].appendChild(touchIconLink);

    document.getElementById("logo").src = "locales/" + locale + "/logo.png";
    document.getElementsByTagName("body")[0].style.display = ""; // Show the Body when Updates are Complete


    setCookie("locale", locale);
}

function makePage(pageName, pageJSON) {
    var tbl = document.createElement('table');
    tbl.style.display = 'none';
    tbl.id = pageName;
    tbl.className = "panel";

    if (pageJSON.pageHead != null) {
        var headTR = tbl.insertRow();
        var headTD = headTR.insertCell(0);
        headTD.className = "pageHead";
        headTD.id = pageName + "-head";
        headTD.colSpan = 2;
        headTD.innerHTML = pageJSON.pageHead;
    }

    if (pageJSON.pageSubHead != null) {
        var subTR = tbl.insertRow();
        var subTD = subTR.insertCell(0);
        subTD.className = "pageSubHead";
        subTD.id = pageName + "-sub";
        subTD.colSpan = 2;
        subTD.innerHTML = pageJSON.pageSubHead;
    }

    for (var button = 0; button < Object.keys(pageJSON['buttons']).length; button += 2) {
        var tr = tbl.insertRow();
        for (var i = 0; i < 2; i++) {
            var buttonName = Object.keys(pageJSON['buttons'])[button + i];
            var buttonAction = pageJSON['buttons'][buttonName];

            var td = tr.insertCell(i);
            td.innerHTML = buttonTemplate.replace("{{action}}", buttonAction).replace("{{text}}", buttonName);

            if ((button + i) == (Object.keys(pageJSON['buttons']).length - 1) && i != 1) {
                td.colSpan = 2;
                td.innerHTML = td.innerHTML.replace("panelButton", "panelButton full");
                break;

            }
        }
    }

    document.body.appendChild(tbl); // Add the table to the Body
}