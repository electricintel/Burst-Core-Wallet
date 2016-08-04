/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function (NRS) {

    var isDesktopApplication = navigator.userAgent.indexOf("JavaFX") >= 0;
    var isPromiseSupported = (typeof Promise !== "undefined" && Promise.toString().indexOf("[native code]") !== -1);
    var isMobileApp = window["cordova"] !== undefined;
    var remoteNodeUrl;

    NRS.isIndexedDBSupported = function() {
        return window.indexedDB !== undefined;
    };

    NRS.isCoinExchangePageAvailable = function() {
        return !isDesktopApplication; // JavaFX does not support CORS required by ShapeShift
    };

    NRS.isExternalLinkVisible = function() {
        // When using JavaFX add a link to a web wallet except on Linux since on Ubuntu it sometimes hangs
        return isDesktopApplication && navigator.userAgent.indexOf("Linux") == -1;
    };

    NRS.isMobileSettingsModalAvailable = function() {
        return isMobileApp;
    };

    NRS.isPollGetState = function() {
        // When using JavaFX do not poll the server unless it's a working as a proxy
        return !isDesktopApplication || NRS.state && NRS.state.apiProxy;
    };

    NRS.isExportContactsAvailable = function() {
        return !isDesktopApplication; // When using JavaFX you cannot export the contact list
    };

    NRS.isShowDummyCheckbox = function() {
        return isDesktopApplication && navigator.userAgent.indexOf("Linux") >= 0; // Correct rendering problem of checkboxes on Linux
    };

    NRS.isDecodePeerHallmark = function() {
        return isPromiseSupported;
    };

    NRS.getRemoteNode = function() {
        if (remoteNodeUrl) {
            return remoteNodeUrl;
        }
        var url = "";
        if (isMobileApp) {
            url += NRS.getRandomNodeUrl(NRS.mobileSettings.is_testnet, NRS.mobileSettings.is_ssl);
        }
        NRS.logConsole("Remote node url: " + url);
        remoteNodeUrl = url;
        return url;
    };

    NRS.resetRemoteNode = function() {
        remoteNodeUrl = null;
    };

    NRS.getDownloadLink = function(url, link) {
        if (isMobileApp) {
            var script = "NRS.openMobileBrowser(\"" + url + "\");";
            if (link) {
                link.attr("onclick", script);
                return;
            }
            return "<a onclick='" + script +"' class='btn btn-xs btn-default'>" + $.t("download") + "</a>";
        } else {
            if (link) {
                link.attr("href", url);
                return;
            }
            return "<a href='" + url + "' class='btn btn-xs btn-default'>" + $.t("download") + "</a>";
        }
    };

    NRS.openMobileBrowser = function(url) {
        try {
            // Works on Android 6.0 (does not work in 5.1)
            cordova.InAppBrowser.open(url, '_system');
        } catch(e) {
            NRS.logConsole(e.message);
        }
    };

    return NRS;
}(NRS || {}, jQuery));