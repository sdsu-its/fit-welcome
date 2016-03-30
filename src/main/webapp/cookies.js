/**
 * Read, Write, and Delete Cookies used by FIT Welcome to store data (mostly the quote of the day) between sessions.
 *
 * Created by tpaulus on 3/29/16.
 */

function setCookie(cname, cvalue, expire_date) {
    var expires;
    if (expire_date != null && expire_date.length > 0) {
        expires = "expires=" + expire_date.toUTCString();
    } else {
        expires = "";
    }
    document.cookie = cname + "=" + cvalue + "; " + expires;
}

function deleteCookie(cname) {
    document.cookie = cname + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC";
}

function getCookie(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') c = c.substring(1);
        if (c.indexOf(name) == 0) return c.substring(name.length, c.length);
    }
    return "";
}

function getMidnight() {
    var d = new Date(new Date().getTime() + 24 * 60 * 60 * 1000);
    d.setHours(0);
    d.setMinutes(0);
    d.setSeconds(0);

    return d;
}