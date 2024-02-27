package co.rsk.peg;

import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.UTXO;
import co.rsk.config.BridgeConstants;
import co.rsk.core.RskAddress;
import co.rsk.peg.federation.Federation;
import co.rsk.peg.federation.PendingFederation;
import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.core.Repository;
import org.ethereum.vm.DataWord;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static co.rsk.peg.BridgeStorageIndexKey.*;
import static co.rsk.peg.BridgeStorageIndexKey.OLD_FEDERATION_KEY;
import static co.rsk.peg.federation.FederationFormatVersion.*;
import static org.ethereum.config.blockchain.upgrades.ConsensusRule.RSKIP284;
import static org.ethereum.config.blockchain.upgrades.ConsensusRule.RSKIP293;

public class FederationStorageProvider {
    private final Repository repository;
    private final RskAddress contractAddress;
    private final BridgeConstants bridgeConstants;
    private final NetworkParameters networkParameters;
    private final ActivationConfig.ForBlock activations;
    private Federation newFederation;
    private Federation oldFederation;
    private List<UTXO> newFederationBtcUTXOs;
    private List<UTXO> oldFederationBtcUTXOs;
    private boolean shouldSaveOldFederation = false;
    private PendingFederation pendingFederation;
    private boolean shouldSavePendingFederation = false;
    private HashMap<DataWord, Optional<Integer>> storageVersionEntries;
    
    public FederationStorageProvider(
        Repository repository,
        RskAddress contractAddress,
        BridgeConstants bridgeConstants,
        ActivationConfig.ForBlock activations) {
        this.repository = repository;
        this.contractAddress = contractAddress;
        this.bridgeConstants = bridgeConstants;
        this.activations = activations;
        this.networkParameters = bridgeConstants.getBtcParams();
    }
    public Federation getNewFederation() {
        if (newFederation != null) {
            return newFederation;
        }

        Optional<Integer> storageVersion = getStorageVersion(NEW_FEDERATION_FORMAT_VERSION.getKey());

        newFederation = safeGetFromRepository(
            NEW_FEDERATION_KEY,
            data -> {
                if (data == null) {
                    return null;
                }
                if (storageVersion.isPresent()) {
                    return deserializeFederationAccordingToVersion(data, storageVersion.get(), bridgeConstants);
                }

                return BridgeSerializationUtils.deserializeStandardMultisigFederationOnlyBtcKeys(data, networkParameters);
            }
        );

        return newFederation;
    }

    public List<UTXO> getNewFederationBtcUTXOs() throws IOException {
        if (newFederationBtcUTXOs != null) {
            return newFederationBtcUTXOs;
        }

        DataWord key = getStorageKeyForNewFederationBtcUtxos();
        newFederationBtcUTXOs = getFromRepository(key, BridgeSerializationUtils::deserializeUTXOList);
        return newFederationBtcUTXOs;
    }

    private DataWord getStorageKeyForNewFederationBtcUtxos() {
        DataWord key = NEW_FEDERATION_BTC_UTXOS_KEY.getKey();
        if (networkParameters.getId().equals(NetworkParameters.ID_TESTNET)) {
            if (activations.isActive(RSKIP284)) {
                key = NEW_FEDERATION_BTC_UTXOS_KEY_FOR_TESTNET_PRE_HOP.getKey();
            }
            if (activations.isActive(RSKIP293)) {
                key = NEW_FEDERATION_BTC_UTXOS_KEY_FOR_TESTNET_POST_HOP.getKey();
            }
        }

        return key;
    }
    public Federation getOldFederation() {
        if (oldFederation != null || shouldSaveOldFederation) {
            return oldFederation;
        }

        Optional<Integer> storageVersion = getStorageVersion(OLD_FEDERATION_FORMAT_VERSION.getKey());

        oldFederation = safeGetFromRepository(
            OLD_FEDERATION_KEY,
            data -> {
                if (data == null) {
                    return null;
                }
                if (storageVersion.isPresent()) {
                    return deserializeFederationAccordingToVersion(data, storageVersion.get(), bridgeConstants);
                }

                return BridgeSerializationUtils.deserializeStandardMultisigFederationOnlyBtcKeys(data, networkParameters);
            }
        );

        return oldFederation;
    }
    public void saveNewFederationBtcUTXOs() throws IOException {
        if (newFederationBtcUTXOs == null) {
            return;
        }

        DataWord key = getStorageKeyForNewFederationBtcUtxos();
        saveToRepository(key, newFederationBtcUTXOs, BridgeSerializationUtils::serializeUTXOList);
    }

    public List<UTXO> getOldFederationBtcUTXOs() throws IOException {
        if (oldFederationBtcUTXOs != null) {
            return oldFederationBtcUTXOs;
        }

        oldFederationBtcUTXOs = getFromRepository(OLD_FEDERATION_BTC_UTXOS_KEY, BridgeSerializationUtils::deserializeUTXOList);
        return oldFederationBtcUTXOs;
    }

    private Optional<Integer> getStorageVersion(DataWord versionKey) {
        if (!storageVersionEntries.containsKey(versionKey)) {
            Optional<Integer> version = safeGetFromRepository(versionKey, data -> {
                if (data == null || data.length == 0) {
                    return Optional.empty();
                }

                return Optional.of(BridgeSerializationUtils.deserializeInteger(data));
            });

            storageVersionEntries.put(versionKey, version);
            return version;
        }

        return storageVersionEntries.get(versionKey);
    }

    private Federation deserializeFederationAccordingToVersion(
        byte[] data,
        int version,
        BridgeConstants bridgeConstants
    ) {
        if (version == STANDARD_MULTISIG_FEDERATION.getFormatVersion()) {
            return BridgeSerializationUtils.deserializeStandardMultisigFederation(
                data,
                networkParameters
            );
        }
        if (version == NON_STANDARD_ERP_FEDERATION.getFormatVersion()) {
            return BridgeSerializationUtils.deserializeNonStandardErpFederation(
                data,
                bridgeConstants,
                activations
            );
        }
        if (version == P2SH_ERP_FEDERATION.getFormatVersion()) {
            return BridgeSerializationUtils.deserializeP2shErpFederation(
                data,
                bridgeConstants
            );
        }
        // To keep backwards compatibility
        return BridgeSerializationUtils.deserializeStandardMultisigFederation(
            data,
            networkParameters
        );
    }

    public PendingFederation getPendingFederation() {
        if (pendingFederation != null || shouldSavePendingFederation) {
            return pendingFederation;
        }

        Optional<Integer> storageVersion = getStorageVersion(PENDING_FEDERATION_FORMAT_VERSION.getKey());

        pendingFederation = safeGetFromRepository(
            PENDING_FEDERATION_KEY,
            data -> {
                if (data == null) {
                    return null;
                }
                if (storageVersion.isPresent()) {
                    return PendingFederation.deserialize(data); // Assume this is the multi-key version
                }

                return PendingFederation.deserializeFromBtcKeysOnly(data);
            }
        );

        return pendingFederation;
    }

    private <T> T safeGetFromRepository(BridgeStorageIndexKey keyAddress, RepositoryDeserializer<T> deserializer) {
        return safeGetFromRepository(keyAddress.getKey(), deserializer);
    }

    protected <T> T safeGetFromRepository(DataWord keyAddress, RepositoryDeserializer<T> deserializer) {
        try {
            return getFromRepository(keyAddress, deserializer);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to get from repository: " + keyAddress, ioe);
        }
    }

    private <T> T getFromRepository(BridgeStorageIndexKey keyAddress, RepositoryDeserializer<T> deserializer) throws IOException {
        return getFromRepository(keyAddress.getKey(), deserializer);
    }

    private <T> T getFromRepository(DataWord keyAddress, RepositoryDeserializer<T> deserializer) throws IOException {
        byte[] data = repository.getStorageBytes(contractAddress, keyAddress);
        return deserializer.deserialize(data);
    }

    private <T> void safeSaveToRepository(BridgeStorageIndexKey addressKey, T object, RepositorySerializer<T> serializer) {
        safeSaveToRepository(addressKey.getKey(), object, serializer);
    }
    private <T> void safeSaveToRepository(DataWord addressKey, T object, RepositorySerializer<T> serializer) {
        try {
            saveToRepository(addressKey, object, serializer);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to save to repository: " + addressKey, ioe);
        }
    }

    private <T> void saveToRepository(BridgeStorageIndexKey indexKeys, T object, RepositorySerializer<T> serializer) throws IOException {
        saveToRepository(indexKeys.getKey(), object, serializer);
    }

    private <T> void saveToRepository(DataWord addressKey, T object, RepositorySerializer<T> serializer) throws IOException {
        byte[] data = null;
        if (object != null) {
            data = serializer.serialize(object);
        }
        repository.addStorageBytes(contractAddress, addressKey, data);
    }

    private interface RepositoryDeserializer<T> {
        T deserialize(byte[] data) throws IOException;
    }

    private interface RepositorySerializer<T> {
        byte[] serialize(T object) throws IOException;
    }
}
