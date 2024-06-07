package co.rsk.peg.whitelist;

import static co.rsk.peg.whitelist.WhitelistResponseCode.ADDRESS_ALREADY_WHITELISTED;
import static co.rsk.peg.whitelist.WhitelistResponseCode.ADDRESS_NOT_EXIST;
import static co.rsk.peg.whitelist.WhitelistResponseCode.DELAY_ALREADY_SET;
import static co.rsk.peg.whitelist.WhitelistResponseCode.DISABLE_BLOCK_DELAY_INVALID;
import static co.rsk.peg.whitelist.WhitelistResponseCode.INVALID_ADDRESS_FORMAT;
import static co.rsk.peg.whitelist.WhitelistResponseCode.SUCCESS;
import static co.rsk.peg.whitelist.WhitelistResponseCode.UNAUTHORIZED_CALLER;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.AddressFormatException;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.Context;
import co.rsk.peg.vote.AddressBasedAuthorizer;
import co.rsk.peg.whitelist.constants.WhitelistConstants;
import java.math.BigInteger;
import java.util.List;
import org.ethereum.core.SignatureCache;
import org.ethereum.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhitelistSupportImpl implements WhitelistSupport {

    private static final Logger logger = LoggerFactory.getLogger(WhitelistSupportImpl.class);
    private static final String INVALID_ADDRESS_FORMAT_MESSAGE = "invalid address format";
    private final SignatureCache signatureCache;
    private final Context btcContext;
    private final WhitelistConstants whitelistConstants;
    private final WhitelistStorageProvider whitelistStorageProvider;

    public WhitelistSupportImpl(
        WhitelistStorageProvider whitelistStorageProvider,
        WhitelistConstants whitelistConstants,
        SignatureCache signatureCache,
        Context btcContext) {
        this.whitelistStorageProvider = whitelistStorageProvider;
        this.whitelistConstants = whitelistConstants;
        this.signatureCache = signatureCache;
        this.btcContext = btcContext;
    }

    @Override
    public int getLockWhitelistSize() {
        return whitelistStorageProvider.getLockWhitelist().getSize();
    }

    @Override
    public LockWhitelistEntry getLockWhitelistEntryByIndex(int index) {
        List<LockWhitelistEntry> entries = whitelistStorageProvider.getLockWhitelist().getAll();

        if (index < 0 || index >= entries.size()) {
            return null;
        }
        return entries.get(index);
    }

    @Override
    public LockWhitelistEntry getLockWhitelistEntryByAddress(String addressBase58) {
        try {
            Address address = Address.fromBase58(btcContext.getParams(), addressBase58);

            return whitelistStorageProvider.getLockWhitelist().get(address);
        } catch (AddressFormatException e) {
            logger.warn("[getLockWhitelistEntryByAddress] {}", INVALID_ADDRESS_FORMAT_MESSAGE, e);
            return null;
        }
    }

    @Override
    public int addOneOffLockWhitelistAddress(Transaction tx, String addressBase58, BigInteger maxTransferValue) {
        try {
            Address address = Address.fromBase58(btcContext.getParams(), addressBase58);
            Coin maxTransferValueCoin = Coin.valueOf(maxTransferValue.longValueExact());
            LockWhitelistEntry entry = new OneOffWhiteListEntry(address, maxTransferValueCoin);

            return addLockWhitelistAddress(tx, entry);
        } catch (AddressFormatException e) {
            logger.warn("[addOneOffLockWhitelistAddress] {}", INVALID_ADDRESS_FORMAT_MESSAGE, e);
            return INVALID_ADDRESS_FORMAT.getCode();
        }
    }

    @Override
    public int addUnlimitedLockWhitelistAddress(Transaction tx, String addressBase58) {
        try {
            Address address = Address.fromBase58(btcContext.getParams(), addressBase58);
            LockWhitelistEntry entry = new UnlimitedWhiteListEntry(address);

            return addLockWhitelistAddress(tx, entry);
        } catch (AddressFormatException e) {
            logger.warn("[addUnlimitedLockWhitelistAddress] {}", INVALID_ADDRESS_FORMAT_MESSAGE, e);
            return INVALID_ADDRESS_FORMAT.getCode();
        }
    }

    private Integer addLockWhitelistAddress(Transaction tx, LockWhitelistEntry entry) {
        if (!isLockWhitelistChangeAuthorized(tx)) {
            return UNAUTHORIZED_CALLER.getCode();
        }

        LockWhitelist whitelist = whitelistStorageProvider.getLockWhitelist();
        if (whitelist.isWhitelisted(entry.address())) {
            return ADDRESS_ALREADY_WHITELISTED.getCode();
        }
        whitelist.put(entry.address(), entry);
        return SUCCESS.getCode();
    }

    @Override
    public int removeLockWhitelistAddress(Transaction tx, String addressBase58) {
        if (!isLockWhitelistChangeAuthorized(tx)) {
            return UNAUTHORIZED_CALLER.getCode();
        }

        LockWhitelist whitelist = whitelistStorageProvider.getLockWhitelist();
        try {
            Address address = Address.fromBase58(btcContext.getParams(), addressBase58);
            if (!whitelist.remove(address)) {
                return ADDRESS_NOT_EXIST.getCode();
            }
            return SUCCESS.getCode();
        } catch (AddressFormatException e) {
            logger.error("[removeLockWhitelistAddress] {}", INVALID_ADDRESS_FORMAT_MESSAGE, e);
            return INVALID_ADDRESS_FORMAT.getCode();
        }
    }

    @Override
    public int setLockWhitelistDisableBlockDelay(Transaction tx, BigInteger disableBlockDelayBI, int btcBlockchainBestChainHeight) {

        if (!isLockWhitelistChangeAuthorized(tx)) {
            return UNAUTHORIZED_CALLER.getCode();
        }

        LockWhitelist lockWhitelist = whitelistStorageProvider.getLockWhitelist();
        if (lockWhitelist.isDisableBlockSet()) {
            return DELAY_ALREADY_SET.getCode();
        }

        int disableBlockDelay = disableBlockDelayBI.intValueExact();
        if (disableBlockDelay + btcBlockchainBestChainHeight <= btcBlockchainBestChainHeight) {
            return DISABLE_BLOCK_DELAY_INVALID.getCode();
        }

        lockWhitelist.setDisableBlockHeight(btcBlockchainBestChainHeight + disableBlockDelay);

        return SUCCESS.getCode();
    }

    @Override
    public boolean verifyLockSenderIsWhitelisted(Address senderBtcAddress, Coin totalAmount, int height) {
        final String ADDRESS_NOT_WHITELISTED_MESSAGE = "Rejected lock. Address is not whitelisted. Address: ";
        LockWhitelist lockWhitelist = whitelistStorageProvider.getLockWhitelist();
        if (!lockWhitelist.isWhitelistedFor(senderBtcAddress, totalAmount, height)) {
            logger.info("[verifyLockSenderIsWhitelisted] {} {}", ADDRESS_NOT_WHITELISTED_MESSAGE, senderBtcAddress);
            return false;
        }
        // Consume this whitelisted address
        lockWhitelist.consume(senderBtcAddress);

        return true;
    }

    private boolean isLockWhitelistChangeAuthorized(Transaction tx) {
        AddressBasedAuthorizer authorizer = whitelistConstants.getLockWhitelistChangeAuthorizer();
        return authorizer.isAuthorized(tx, signatureCache);
    }
}
