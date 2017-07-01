/**
 * Manages Follow Up email Subscriptions
 *
 * Created by tpaulus on 4/1/16.
 */

window.onload = function () {
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
    $.ajax({
        method: "GET",
        url: "../api/followup/unsubscribe?email=" + email
    })
        .done(function (resp) {
            doLoadMessage(resp)
        })
        .fail(function () {
            $("#loading").hide();
            $("#not-found").show();
        });
}

function doLoadMessage(json) {
    $('.firstName').text(json.firstName);
    $('.lastName').text(json.lastName);

    $("#loading").hide();
    $('#unsubscribe').show();
}

function subscribe() {
    var email = $("#email").val();

    $.ajax({
        method: "POST",
        url: "../api/followup/subscribe?email=" + email
    })
        .done(function (resp) {
            doLoadSubscribeMessage(resp);

        })
        .fail(function () {
            $('#unsubscribe').hide();
            $('#not-found').show();
        });
}

function doLoadSubscribeMessage(json) {
    $('.firstName').text(json.firstName);
    $('.lastName').text(json.lastName);

    $("#unsubscribe").hide();
    $('#subscribe').show();
}