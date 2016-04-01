/**
 * Render a JSON Document to a set of Button Panels
 *
 * Created by tpaulus on 3/27/16.
 */

const buttonTemplate = '<button class="panelButton" type="button" onclick="{{action}}">{{text}}</button>';
var json;

getMapJSON(); // Generate the Pages

function getMapJSON() {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            json = JSON.parse(xmlHttp.responseText);
            renderMap(json);
        }
    };

    xmlHttp.open('GET', "sitemap.json");
    xmlHttp.send();
}

function renderMap(json) {
    for (var pageName in json) {
        if (!json.hasOwnProperty(pageName)) {
            //The current property is not a direct property of json
            continue;
        }
        makePage(pageName, json[pageName]);
    }
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