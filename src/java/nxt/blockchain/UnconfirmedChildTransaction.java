/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.blockchain;

import nxt.NxtException;
import nxt.messaging.EncryptToSelfMessageAppendix;
import nxt.messaging.EncryptedMessageAppendix;
import nxt.messaging.MessageAppendix;
import nxt.messaging.PrunableEncryptedMessageAppendix;
import nxt.messaging.PrunablePlainMessageAppendix;
import nxt.voting.PhasingAppendix;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UnconfirmedChildTransaction extends UnconfirmedTransaction implements ChildTransaction {

    public UnconfirmedChildTransaction(ChildTransactionImpl transaction, long arrivalTimestamp) {
        super(transaction, arrivalTimestamp);
    }

    public UnconfirmedChildTransaction(ResultSet rs) throws SQLException, NxtException.NotValidException {
        super(ChildTransactionImpl.newTransactionBuilder(
                rs.getBytes("transaction_bytes"),
                rs.getString("prunable_json") != null ? (JSONObject) JSONValue.parse(rs.getString("prunable_json")) : null),
                rs);
    }

    @Override
    ChildTransactionImpl getTransaction() {
        return (ChildTransactionImpl)super.getTransaction();
    }

    @Override
    public ChildChain getChain() {
        return getTransaction().getChain();
    }

    @Override
    public FxtTransaction getFxtTransaction() {
        return getTransaction().getFxtTransaction();
    }

    @Override
    public long getFxtTransactionId() {
        return getTransaction().getFxtTransactionId();
    }

    @Override
    public MessageAppendix getMessage() {
        return getTransaction().getMessage();
    }

    @Override
    public EncryptedMessageAppendix getEncryptedMessage() {
        return getTransaction().getEncryptedMessage();
    }

    @Override
    public EncryptToSelfMessageAppendix getEncryptToSelfMessage() {
        return getTransaction().getEncryptToSelfMessage();
    }

    @Override
    public PhasingAppendix getPhasing() {
        return getTransaction().getPhasing();
    }

    @Override
    public PrunablePlainMessageAppendix getPrunablePlainMessage() {
        return getTransaction().getPrunablePlainMessage();
    }

    @Override
    public PrunableEncryptedMessageAppendix getPrunableEncryptedMessage() {
        return getTransaction().getPrunableEncryptedMessage();
    }

    @Override
    public ChainTransactionId getReferencedTransactionId() {
        return getTransaction().getReferencedTransactionId();
    }

}
