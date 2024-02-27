package co.rsk.peg;

import co.rsk.bitcoinj.core.*;
import co.rsk.bitcoinj.store.BlockStoreException;
import co.rsk.config.BridgeConstants;
import co.rsk.panic.PanicProcessor;
import co.rsk.peg.whitelist.LockWhitelist;
import co.rsk.peg.whitelist.LockWhitelistEntry;
import co.rsk.peg.whitelist.OneOffWhiteListEntry;
import co.rsk.peg.whitelist.UnlimitedWhiteListEntry;
import org.ethereum.core.SignatureCache;
import org.ethereum.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class LockWhitelistSupport {

    public static final Integer LOCK_WHITELIST_GENERIC_ERROR_CODE = -10;
    public static final Integer LOCK_WHITELIST_INVALID_ADDRESS_FORMAT_ERROR_CODE = -2;
    public static final Integer LOCK_WHITELIST_ALREADY_EXISTS_ERROR_CODE = -1;
    public static final Integer LOCK_WHITELIST_UNKNOWN_ERROR_CODE = 0;
    public static final Integer LOCK_WHITELIST_SUCCESS_CODE = 1;
    private static final String INVALID_ADDRESS_FORMAT_MESSAGE = "invalid address format";

    private final BridgeConstants bridgeConstants;
    private final BridgeStorageProvider provider;
    private final SignatureCache signatureCache;
    private static final Logger logger = LoggerFactory.getLogger("LockWhitelistSupport");
    private static final PanicProcessor panicProcessor = new PanicProcessor();

    public LockWhitelistSupport(BridgeConstants bridgeConstants, BridgeStorageProvider provider, SignatureCache signatureCache) {
        this.bridgeConstants = bridgeConstants;
        this.provider = provider;
        this.signatureCache = signatureCache;
    }

    /**
     * Returns the lock whitelist size, that is,
     * the number of whitelisted addresses
     * @return the lock whitelist size
     */
    public Integer getLockWhitelistSize() {
        return provider.getLockWhitelist().getSize();
    }

    /**
     * Returns the lock whitelist address stored
     * at the given index, or null if the
     * index is out of bounds
     * @param index the index at which to get the address
     * @return the base58-encoded address stored at the given index, or null if index is out of bounds
     */
    public LockWhitelistEntry getLockWhitelistEntryByIndex(int index) {
        List<LockWhitelistEntry> entries = provider.getLockWhitelist().getAll();

        if (index < 0 || index >= entries.size()) {
            return null;
        }

        return entries.get(index);
    }

    /**
     *
     * @param addressBase58
     * @return
     */
    public LockWhitelistEntry getLockWhitelistEntryByAddress(String addressBase58) {
        try {
            Address address = getParsedAddress(addressBase58);
            return provider.getLockWhitelist().get(address);
        } catch (AddressFormatException e) {
            logger.warn(INVALID_ADDRESS_FORMAT_MESSAGE, e);
            return null;
        }
    }

    /**
     * Adds the given address to the lock whitelist.
     * Returns 1 upon success, or -1 if the address was
     * already in the whitelist.
     * @param addressBase58 the base58-encoded address to add to the whitelist
     * @param maxTransferValue the max amount of satoshis enabled to transfer for this address
     * @return 1 upon success, -1 if the address was already
     * in the whitelist, -2 if address is invalid
     * LOCK_WHITELIST_GENERIC_ERROR_CODE otherwise.
     */
    public Integer addOneOffLockWhitelistAddress(Transaction tx, String addressBase58, BigInteger maxTransferValue) {
        try {
            Address address = getParsedAddress(addressBase58);
            Coin maxTransferValueCoin = Coin.valueOf(maxTransferValue.longValueExact());
            return this.addLockWhitelistAddress(tx, new OneOffWhiteListEntry(address, maxTransferValueCoin));
        } catch (AddressFormatException e) {
            logger.warn(INVALID_ADDRESS_FORMAT_MESSAGE, e);
            return LOCK_WHITELIST_INVALID_ADDRESS_FORMAT_ERROR_CODE;
        }
    }

    public Integer addUnlimitedLockWhitelistAddress(Transaction tx, String addressBase58) {
        try {
            Address address = getParsedAddress(addressBase58);
            return this.addLockWhitelistAddress(tx, new UnlimitedWhiteListEntry(address));
        } catch (AddressFormatException e) {
            logger.warn(INVALID_ADDRESS_FORMAT_MESSAGE, e);
            return LOCK_WHITELIST_INVALID_ADDRESS_FORMAT_ERROR_CODE;
        }
    }

    /**
     * Sets a delay in the BTC best chain to disable lock whitelist
     * @param tx current RSK transaction
     * @param disableBlockDelayBI block since current BTC best chain height to disable lock whitelist
     * @return 1 if it was successful, -1 if a delay was already set, -2 if disableBlockDelay contains an invalid value
     */
    public Integer setLockWhitelistDisableBlockDelay(Transaction tx, BigInteger disableBlockDelayBI, int bestChainHeight) {
        if (!isLockWhitelistChangeAuthorized(tx)) {
            return LOCK_WHITELIST_GENERIC_ERROR_CODE;
        }
        LockWhitelist lockWhitelist = provider.getLockWhitelist();
        if (lockWhitelist.isDisableBlockSet()) {
            return -1;
        }
        int disableBlockDelay = disableBlockDelayBI.intValueExact();
        if (disableBlockDelay + bestChainHeight <= bestChainHeight) {
            return -2;
        }
        lockWhitelist.setDisableBlockHeight(bestChainHeight + disableBlockDelay);
        return 1;
    }

    /**
     * Removes the given address from the lock whitelist.
     * Returns 1 upon success, or -1 if the address was
     * not in the whitelist.
     * @param addressBase58 the base58-encoded address to remove from the whitelist
     * @return 1 upon success, -1 if the address was not
     * in the whitelist, -2 if the address is invalid,
     * LOCK_WHITELIST_GENERIC_ERROR_CODE otherwise.
     */
    public Integer removeLockWhitelistAddress(Transaction tx, String addressBase58) {
        if (!isLockWhitelistChangeAuthorized(tx)) {
            return LOCK_WHITELIST_GENERIC_ERROR_CODE;
        }

        LockWhitelist whitelist = provider.getLockWhitelist();

        try {
            Address address = getParsedAddress(addressBase58);

            if (!whitelist.remove(address)) {
                return -1;
            }

            return 1;
        } catch (AddressFormatException e) {
            return -2;
        } catch (Exception e) {
            logger.error("Unexpected error in removeLockWhitelistAddress: {}", e.getMessage());
            panicProcessor.panic("lock-whitelist", e.getMessage());
            return 0;
        }
    }

    private Integer addLockWhitelistAddress(Transaction tx, LockWhitelistEntry entry) {
        if (!isLockWhitelistChangeAuthorized(tx)) {
            return LOCK_WHITELIST_GENERIC_ERROR_CODE;
        }

        LockWhitelist whitelist = provider.getLockWhitelist();

        try {
            if (whitelist.isWhitelisted(entry.address())) {
                return LOCK_WHITELIST_ALREADY_EXISTS_ERROR_CODE;
            }
            whitelist.put(entry.address(), entry);
            return LOCK_WHITELIST_SUCCESS_CODE;
        } catch (Exception e) {
            logger.error("Unexpected error in addLockWhitelistAddress: {}", e.getMessage());
            panicProcessor.panic("lock-whitelist", e.getMessage());
            return LOCK_WHITELIST_UNKNOWN_ERROR_CODE;
        }
    }


    private Address getParsedAddress(String base58Address) throws AddressFormatException {
        NetworkParameters networkParameters = bridgeConstants.getBtcParams();

        return Address.fromBase58(networkParameters, base58Address);
    }

    private boolean isLockWhitelistChangeAuthorized(Transaction tx) {
        AddressBasedAuthorizer authorizer = bridgeConstants.getLockWhitelistChangeAuthorizer();

        return authorizer.isAuthorized(tx, signatureCache);
    }
}
