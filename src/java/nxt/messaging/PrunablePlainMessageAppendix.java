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

package nxt.messaging;

import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.account.Account;
import nxt.blockchain.Appendix;
import nxt.blockchain.ChildChain;
import nxt.blockchain.ChildTransaction;
import nxt.blockchain.Fee;
import nxt.blockchain.Transaction;
import nxt.blockchain.TransactionImpl;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class PrunablePlainMessageAppendix extends Appendix.AbstractAppendix implements Appendix.Prunable {

    private static final String appendixName = "PrunablePlainMessage";

    private static final Fee PRUNABLE_MESSAGE_FEE = new Fee.SizeBasedFee(Constants.ONE_NXT/10) {
        @Override
        public int getSize(TransactionImpl transaction, Appendix appendix) {
            return appendix.getFullSize();
        }
    };

    public static PrunablePlainMessageAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        return new PrunablePlainMessageAppendix(attachmentData);
    }

    private final byte[] hash;
    private final byte[] message;
    private final boolean isText;
    private volatile PrunableMessageHome.PrunableMessage prunableMessage;

    public PrunablePlainMessageAppendix(ByteBuffer buffer) {
        super(buffer);
        this.hash = new byte[32];
        buffer.get(this.hash);
        this.message = null;
        this.isText = false;
    }

    private PrunablePlainMessageAppendix(JSONObject attachmentData) {
        super(attachmentData);
        String hashString = Convert.emptyToNull((String) attachmentData.get("messageHash"));
        String messageString = Convert.emptyToNull((String) attachmentData.get("message"));
        if (hashString != null && messageString == null) {
            this.hash = Convert.parseHexString(hashString);
            this.message = null;
            this.isText = false;
        } else {
            this.hash = null;
            this.isText = Boolean.TRUE.equals(attachmentData.get("messageIsText"));
            this.message = Convert.toBytes(messageString, isText);
        }
    }

    public PrunablePlainMessageAppendix(byte[] message) {
        this(message, false);
    }

    public PrunablePlainMessageAppendix(String string) {
        this(Convert.toBytes(string), true);
    }

    public PrunablePlainMessageAppendix(String string, boolean isText) {
        this(Convert.toBytes(string, isText), isText);
    }

    public PrunablePlainMessageAppendix(byte[] message, boolean isText) {
        this.message = message;
        this.isText = isText;
        this.hash = null;
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return PRUNABLE_MESSAGE_FEE;
    }

    @Override
    protected int getMySize() {
        return 32;
    }

    @Override
    protected int getMyFullSize() {
        return getMessage() == null ? 0 : getMessage().length;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.put(getHash());
    }

    @Override
    protected void putMyJSON(JSONObject json) {
        if (prunableMessage != null) {
            json.put("message", Convert.toString(prunableMessage.getMessage(), prunableMessage.messageIsText()));
            json.put("messageIsText", prunableMessage.messageIsText());
        } else if (message != null) {
            json.put("message", Convert.toString(message, isText));
            json.put("messageIsText", isText);
        }
        json.put("messageHash", Convert.toHexString(getHash()));
    }

    @Override
    public void validate(Transaction transaction) throws NxtException.ValidationException {
        if (((ChildTransaction)transaction).getMessage() != null) {
            throw new NxtException.NotValidException("Cannot have both message and prunable message attachments");
        }
        byte[] msg = getMessage();
        if (msg != null && msg.length > Constants.MAX_PRUNABLE_MESSAGE_LENGTH) {
            throw new NxtException.NotValidException("Invalid prunable message length: " + msg.length);
        }
        if (msg == null && Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
            throw new NxtException.NotCurrentlyValidException("Message has been pruned prematurely");
        }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MAX_PRUNABLE_LIFETIME) {
            ((ChildChain) transaction.getChain()).getPrunableMessageHome().add((TransactionImpl)transaction, this);
        }
    }

    public byte[] getMessage() {
        if (prunableMessage != null) {
            return prunableMessage.getMessage();
        }
        return message;
    }

    public boolean isText() {
        if (prunableMessage != null) {
            return prunableMessage.messageIsText();
        }
        return isText;
    }

    @Override
    public byte[] getHash() {
        if (hash != null) {
            return hash;
        }
        MessageDigest digest = Crypto.sha256();
        digest.update((byte)(isText ? 1 : 0));
        digest.update(message);
        return digest.digest();
    }

    @Override
    public final void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
        if (!hasPrunableData() && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
            PrunableMessageHome.PrunableMessage prunableMessage = ((ChildChain) transaction.getChain()).getPrunableMessageHome()
                    .getPrunableMessage(transaction.getId());
            if (prunableMessage != null && prunableMessage.getMessage() != null) {
                this.prunableMessage = prunableMessage;
            }
        }
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public final boolean hasPrunableData() {
        return (prunableMessage != null || message != null);
    }

    @Override
    public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        ((ChildChain) transaction.getChain()).getPrunableMessageHome().add((TransactionImpl)transaction, this, blockTimestamp, height);
    }
}