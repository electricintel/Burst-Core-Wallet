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

package nxt.http;

import nxt.Account;
import nxt.AccountMonitor;
import nxt.HoldingType;
import nxt.crypto.Crypto;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.http.JSONResponses.MONITOR_NOT_STARTED;

/**
 * <p>Get an account monitor</p>
 *
 * <p>A single account monitor will be returned when the secret phrase is specified.
 * Otherwise, the administrator password must be specified and all account monitors
 * will be returned.</p>
 *
 * <p>The account monitor holding type and account property name must be specified when the secret
 * phrase is specified. Holding type codes are listed in getConstants.
 * In addition, the holding identifier must be specified when the holding type is ASSET or CURRENCY.</p>
 */
public class GetAccountMonitor extends APIServlet.APIRequestHandler {

    static final GetAccountMonitor instance = new GetAccountMonitor();

    private GetAccountMonitor() {
        super(new APITag[] {APITag.ACCOUNTS}, "holdingType", "holding", "property", "secretPhrase", "adminPassword");
    }
    /**
     * Process the request
     *
     * @param   req                 Client request
     * @return                      Client response
     * @throws  ParameterException        Unable to process request
     */
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        if (secretPhrase != null) {
            long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
            HoldingType holdingType = ParameterParser.getHoldingType(req);
            long holdingId = ParameterParser.getHoldingId(req, holdingType);
            String property = ParameterParser.getAccountProperty(req);
            AccountMonitor monitor = AccountMonitor.getMonitor(holdingType, holdingId, property, accountId);
            if (monitor == null) {
                return MONITOR_NOT_STARTED;
            }
            return JSONData.accountMonitor(monitor);
        } else {
            API.verifyPassword(req);
            List<AccountMonitor> monitors = AccountMonitor.getAllMonitors();
            JSONObject response = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            monitors.forEach(monitor -> jsonArray.add(JSONData.accountMonitor(monitor)));
            response.put("monitors", jsonArray);
            return response;
        }
    }

    @Override
    boolean allowRequiredBlockParameters() {
        return false;
    }
}