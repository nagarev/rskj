package co.rsk.peg.storage;

import co.rsk.bitcoinj.core.Coin;
import co.rsk.peg.BridgeSerializationUtils;
import co.rsk.peg.vote.ABICallElection;
import co.rsk.peg.vote.AddressBasedAuthorizer;

import static co.rsk.peg.storage.FeePerKbStorageIndexKey.FEE_PER_KB_ELECTION;
import static co.rsk.peg.storage.FeePerKbStorageIndexKey.FEE_PER_KB;

public class FeePerKbStorageProvider {
    private final BridgeStorageAccessor bridgeStorageAccessor;
    private Coin feePerKb;
    private ABICallElection feePerKbElection;

    public FeePerKbStorageProvider(BridgeStorageAccessor bridgeStorageAccessor) {
        this.bridgeStorageAccessor = bridgeStorageAccessor;
    }

    public void setFeePerKb(Coin feePerKb) {
        this.feePerKb = feePerKb;
    }

    private void saveFeePerKb() {
        if (feePerKb == null) {
            return;
        }

        bridgeStorageAccessor.safeSaveToRepository(FEE_PER_KB.getKey(), feePerKb, BridgeSerializationUtils::serializeCoin);
    }

    public Coin getFeePerKb() {
        if (feePerKb != null) {
            return feePerKb;
        }

        feePerKb = bridgeStorageAccessor.safeGetFromRepository(FEE_PER_KB.getKey(), BridgeSerializationUtils::deserializeCoin);
        return feePerKb;
    }
    private void saveFeePerKbElection() {
        if (feePerKbElection == null) {
            return;
        }

        bridgeStorageAccessor.safeSaveToRepository(FEE_PER_KB_ELECTION.getKey(), feePerKbElection, BridgeSerializationUtils::serializeElection);
    }

    public ABICallElection getFeePerKbElection(AddressBasedAuthorizer authorizer) {
        if (feePerKbElection != null) {
            return feePerKbElection;
        }

        feePerKbElection = bridgeStorageAccessor.safeGetFromRepository(FEE_PER_KB_ELECTION.getKey(), data -> BridgeSerializationUtils.deserializeElection(data, authorizer));
        return feePerKbElection;
    }

    public void save() {
        saveFeePerKb();
        saveFeePerKbElection();
    }
}
