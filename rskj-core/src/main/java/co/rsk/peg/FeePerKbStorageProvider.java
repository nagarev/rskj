package co.rsk.peg;

import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.config.BridgeConstants;
import co.rsk.core.RskAddress;
import co.rsk.peg.*;
import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.core.Repository;
import org.ethereum.vm.DataWord;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static co.rsk.peg.BridgeStorageIndexKey.FEE_PER_KB_ELECTION_KEY;
import static co.rsk.peg.BridgeStorageIndexKey.FEE_PER_KB_KEY;

public class FeePerKbStorageProvider {
    private Coin feePerKb;
    private ABICallElection feePerKbElection;
    private final Repository repository;
    private final RskAddress contractAddress;

    public FeePerKbStorageProvider(
        Repository repository,
        RskAddress contractAddress) {
        this.repository = repository;
        this.contractAddress = contractAddress;
    }

    public void setFeePerKb(Coin feePerKb) {
        this.feePerKb = feePerKb;
    }
    public Coin getFeePerKb() {
        if (feePerKb != null) {
            return feePerKb;
        }

        feePerKb = safeGetFromRepository(FEE_PER_KB_KEY, BridgeSerializationUtils::deserializeCoin);
        return feePerKb;
    }

    public ABICallElection getFeePerKbElection(AddressBasedAuthorizer authorizer) {
        if (feePerKbElection != null) {
            return feePerKbElection;
        }

        feePerKbElection = safeGetFromRepository(FEE_PER_KB_ELECTION_KEY, data -> BridgeSerializationUtils.deserializeElection(data, authorizer));
        return feePerKbElection;
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
