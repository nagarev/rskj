/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.rsk.peg;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.UTXO;
import co.rsk.config.BridgeConstants;
import co.rsk.peg.federation.Federation;
import co.rsk.peg.federation.FederationMember;
import co.rsk.peg.federation.PendingFederation;
import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.core.Block;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FederationSupport {

    private enum StorageFederationReference { NONE, NEW, OLD, GENESIS }

    private final BridgeStorageProvider provider;
    private final BridgeConstants bridgeConstants;
    private final Block executionBlock;
    private final ActivationConfig.ForBlock activations;

    public FederationSupport(BridgeConstants bridgeConstants, BridgeStorageProvider provider, Block executionBlock, ActivationConfig.ForBlock activations) {
        this.provider = provider;
        this.bridgeConstants = bridgeConstants;
        this.executionBlock = executionBlock;
        this.activations = activations;
    }

    /**
     * Returns the currently active federation.
     * See getActiveFederationReference() for details.
     *
     * @return the currently active federation.
     */
    public Federation getActiveFederation() {
        switch (getActiveFederationReference()) {
            case NEW:
                return provider.getNewFederation();
            case OLD:
                return provider.getOldFederation();
            case GENESIS:
            default:
                return bridgeConstants.getGenesisFederation();
        }
    }

    /**
     * Returns the federation bitcoin address.
     * @return the federation bitcoin address.
     */
    public Address getFederationAddress() {
        return getActiveFederation().getAddress();
    }

    /**
     * Returns the federation's size
     * @return the federation size
     */
    public int getFederationSize() {
        return getActiveFederation().getBtcPublicKeys().size();
    }

    /**
     * Returns the federation's minimum required signatures
     * @return the federation minimum required signatures
     */
    public Integer getFederationThreshold() {
        return getActiveFederation().getNumberOfSignaturesRequired();
    }

    /**
     * Returns the federation's creation time
     * @return the federation creation time
     */
    public Instant getFederationCreationTime() {
        return getActiveFederation().getCreationTime();
    }

    /**
     * Returns the federation's creation block number
     * @return the federation creation block number
     */
    public long getFederationCreationBlockNumber() {
        return getActiveFederation().getCreationBlockNumber();
    }

    /**
     * Returns the BTC public key of the federation's federator at the given index
     * @param index the federator's index (zero-based)
     * @return the federator's public key
     */
    public byte[] getFederatorBtcPublicKey(int index) {
        List<BtcECKey> publicKeys = getActiveFederation().getBtcPublicKeys();

        if (index < 0 || index >= publicKeys.size()) {
            throw new IndexOutOfBoundsException(String.format("Federator index must be between 0 and %d", publicKeys.size() - 1));
        }

        return publicKeys.get(index).getPubKey();
    }

    /**
     * Returns the public key of given type of the federation's federator at the given index
     * @param index the federator's index (zero-based)
     * @param keyType the key type
     * @return the federator's public key
     */
    public byte[] getFederatorPublicKeyOfType(int index, FederationMember.KeyType keyType) {
        return getMemberPublicKeyOfType(getActiveFederation().getMembers(), index, keyType, "Federator");
    }

    /**
     * Returns the compressed public key of given type of the member list at the given index
     * Throws a custom index out of bounds exception when appropiate
     * @param members the list of federation members
     * @param index the federator's index (zero-based)
     * @param keyType the key type
     * @param errorPrefix the index out of bounds error prefix
     * @return the federation member's public key
     */
    public byte[] getMemberPublicKeyOfType(List<FederationMember> members, int index, FederationMember.KeyType keyType, String errorPrefix) {
        if (index < 0 || index >= members.size()) {
            throw new IndexOutOfBoundsException(String.format("%s index must be between 0 and %d", errorPrefix, members.size() - 1));
        }

        return members.get(index).getPublicKey(keyType).getPubKey(true);
    }

    public List<UTXO> getActiveFederationBtcUTXOs() throws IOException {
        switch (getActiveFederationReference()) {
            case OLD:
                return provider.getOldFederationBtcUTXOs();
            case NEW:
            case GENESIS:
            default:
                return provider.getNewFederationBtcUTXOs();
        }
    }

    /**
     * Returns the currently retiring federation.
     * See getRetiringFederationReference() for details.
     *
     * @return the retiring federation.
     */
    @Nullable
    public Federation getRetiringFederation() {
        switch (getRetiringFederationReference()) {
            case OLD:
                return provider.getOldFederation();
            case NONE:
            default:
                return null;
        }
    }

    /**
     * Returns the retiring federation bitcoin address.
     * @return the retiring federation bitcoin address, null if no retiring federation exists
     */
    public Address getRetiringFederationAddress() {
        Federation retiringFederation = getRetiringFederation();
        if (retiringFederation == null) {
            return null;
        }

        return retiringFederation.getAddress();
    }

    /**
     * Returns the retiring federation's size
     * @return the retiring federation size, -1 if no retiring federation exists
     */
    public Integer getRetiringFederationSize() {
        Federation retiringFederation = getRetiringFederation();
        if (retiringFederation == null) {
            return -1;
        }

        return retiringFederation.getBtcPublicKeys().size();
    }

    /**
     * Returns the retiring federation's minimum required signatures
     * @return the retiring federation minimum required signatures, -1 if no retiring federation exists
     */
    public Integer getRetiringFederationThreshold() {
        Federation retiringFederation = getRetiringFederation();
        if (retiringFederation == null) {
            return -1;
        }

        return retiringFederation.getNumberOfSignaturesRequired();
    }

    /**
     * Returns the retiring federation's creation time
     * @return the retiring federation creation time, null if no retiring federation exists
     */
    public Instant getRetiringFederationCreationTime() {
        Federation retiringFederation = getRetiringFederation();
        if (retiringFederation == null) {
            return null;
        }

        return retiringFederation.getCreationTime();
    }

    /**
     * Returns the retiring federation's creation block number
     * @return the retiring federation creation block number,
     * -1 if no retiring federation exists
     */
    public long getRetiringFederationCreationBlockNumber() {
        Federation retiringFederation = getRetiringFederation();
        if (retiringFederation == null) {
            return -1L;
        }
        return retiringFederation.getCreationBlockNumber();
    }

    /**
     * Returns the public key of the retiring federation's federator at the given index
     * @param index the retiring federator's index (zero-based)
     * @return the retiring federator's public key, null if no retiring federation exists
     */
    public byte[] getRetiringFederatorPublicKey(int index) {
        Federation retiringFederation = getRetiringFederation();
        if (retiringFederation == null) {
            return null;
        }

        List<BtcECKey> publicKeys = retiringFederation.getBtcPublicKeys();

        if (index < 0 || index >= publicKeys.size()) {
            throw new IndexOutOfBoundsException(String.format("Retiring federator index must be between 0 and %d", publicKeys.size() - 1));
        }

        return publicKeys.get(index).getPubKey();
    }

    /**
     * Returns the public key of the given type of the retiring federation's federator at the given index
     * @param index the retiring federator's index (zero-based)
     * @param keyType the key type
     * @return the retiring federator's public key of the given type, null if no retiring federation exists
     */
    public byte[] getRetiringFederatorPublicKeyOfType(int index, FederationMember.KeyType keyType) {
        Federation retiringFederation = getRetiringFederation();
        if (retiringFederation == null) {
            return null;
        }

        return getMemberPublicKeyOfType(retiringFederation.getMembers(), index, keyType, "Retiring federator");
    }

    public List<UTXO> getRetiringFederationBtcUTXOs() throws IOException {
        switch (getRetiringFederationReference()) {
            case OLD:
                return provider.getOldFederationBtcUTXOs();
            case NONE:
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Returns the currently pending federation hash, or null if none exists
     * @return the currently pending federation hash, or null if none exists
     */
    public byte[] getPendingFederationHash() {
        PendingFederation currentPendingFederation = provider.getPendingFederation();

        if (currentPendingFederation == null) {
            return null;
        }

        return currentPendingFederation.getHash().getBytes();
    }

    /**
     * Returns the currently pending federation size, or -1 if none exists
     * @return the currently pending federation size, or -1 if none exists
     */
    public Integer getPendingFederationSize() {
        PendingFederation currentPendingFederation = provider.getPendingFederation();

        if (currentPendingFederation == null) {
            return -1;
        }

        return currentPendingFederation.getBtcPublicKeys().size();
    }

    /**
     * Returns the currently pending federation federator's public key at the given index, or null if none exists
     * @param index the federator's index (zero-based)
     * @return the pending federation's federator public key
     */
    public byte[] getPendingFederatorPublicKey(int index) {
        PendingFederation currentPendingFederation = provider.getPendingFederation();

        if (currentPendingFederation == null) {
            return null;
        }

        List<BtcECKey> publicKeys = currentPendingFederation.getBtcPublicKeys();

        if (index < 0 || index >= publicKeys.size()) {
            throw new IndexOutOfBoundsException(String.format("Federator index must be between 0 and %d", publicKeys.size() - 1));
        }

        return publicKeys.get(index).getPubKey();
    }

    /**
     * Returns the public key of the given type of the pending federation's federator at the given index
     * @param index the federator's index (zero-based)
     * @param keyType the key type
     * @return the pending federation's federator public key of given type
     */
    public byte[] getPendingFederatorPublicKeyOfType(int index, FederationMember.KeyType keyType) {
        PendingFederation currentPendingFederation = provider.getPendingFederation();

        if (currentPendingFederation == null) {
            return null;
        }

        return getMemberPublicKeyOfType(currentPendingFederation.getMembers(), index, keyType, "Federator");
    }

    protected Optional<Federation> getFederationFromPublicKey(BtcECKey federatorPublicKey) {
        Federation retiringFederation = getRetiringFederation();
        Federation activeFederation = getActiveFederation();

        if (activeFederation.hasBtcPublicKey(federatorPublicKey)) {
            return Optional.of(activeFederation);
        }
        if (retiringFederation != null && retiringFederation.hasBtcPublicKey(federatorPublicKey)) {
            return Optional.of(retiringFederation);
        }

        return Optional.empty();
    }

    public boolean amAwaitingFederationActivation() {
        Federation newFederation = provider.getNewFederation();
        Federation oldFederation = provider.getOldFederation();

        return newFederation != null && oldFederation != null && !shouldFederationBeActive(newFederation);
    }

    /**
     * Returns the currently active federation reference.
     * Logic is as follows:
     * When no "new" federation is recorded in the blockchain, then return GENESIS
     * When a "new" federation is present and no "old" federation is present, then return NEW
     * When both "new" and "old" federations are present, then
     * 1) If the "new" federation is at least bridgeConstants::getFederationActivationAge() blocks old,
     * return the NEW
     * 2) Otherwise, return OLD
     *
     * @return a reference to where the currently active federation is stored.
     */
    private StorageFederationReference getActiveFederationReference() {
        Federation newFederation = provider.getNewFederation();

        // No new federation in place, then the active federation
        // is the genesis federation
        if (newFederation == null) {
            return StorageFederationReference.GENESIS;
        }

        Federation oldFederation = provider.getOldFederation();

        // No old federation in place, then the active federation
        // is the new federation
        if (oldFederation == null) {
            return StorageFederationReference.NEW;
        }

        // Both new and old federations in place
        // If the minimum age has gone by for the new federation's
        // activation, then that federation is the currently active.
        // Otherwise, the old federation is still the currently active.
        if (shouldFederationBeActive(newFederation)) {
            return StorageFederationReference.NEW;
        }

        return StorageFederationReference.OLD;
    }

    /**
     * Returns the currently retiring federation reference.
     * Logic is as follows:
     * When no "new" or "old" federation is recorded in the blockchain, then return empty.
     * When both "new" and "old" federations are present, then
     * 1) If the "new" federation is at least bridgeConstants::getFederationActivationAge() blocks old,
     * return OLD
     * 2) Otherwise, return empty
     *
     * @return the retiring federation.
     */
    private StorageFederationReference getRetiringFederationReference() {
        Federation newFederation = provider.getNewFederation();
        Federation oldFederation = provider.getOldFederation();

        if (oldFederation == null || newFederation == null) {
            return StorageFederationReference.NONE;
        }

        // Both new and old federations in place
        // If the minimum age has gone by for the new federation's
        // activation, then the old federation is the currently retiring.
        // Otherwise, there is no retiring federation.
        if (shouldFederationBeActive(newFederation)) {
            return StorageFederationReference.OLD;
        }

        return StorageFederationReference.NONE;
    }

    private boolean shouldFederationBeActive(Federation federation) {
        long federationAge = executionBlock.getNumber() - federation.getCreationBlockNumber();
        return federationAge >= bridgeConstants.getFederationActivationAge(activations);
    }
}
