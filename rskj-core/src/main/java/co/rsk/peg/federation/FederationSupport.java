package co.rsk.peg.federation;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.UTXO;
import co.rsk.bitcoinj.script.Script;
import co.rsk.peg.vote.ABICallSpec;
import org.ethereum.core.SignatureCache;
import org.ethereum.core.Transaction;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FederationSupport {

    Federation getActiveFederation();
    Optional<Script> getActiveFederationRedeemScript();
    Address getActiveFederationAddress();
    int getActiveFederationSize();
    Integer getActiveFederationThreshold();
    Instant getActiveFederationCreationTime();
    long getActiveFederationCreationBlockNumber();
    byte[] getActiveFederatorBtcPublicKey(int index);
    byte[] getActiveFederatorPublicKeyOfType(int index, FederationMember.KeyType keyType);
    List<UTXO> getActiveFederationBtcUTXOs();

    @Nullable
    Federation getRetiringFederation();
    Address getRetiringFederationAddress();
    Integer getRetiringFederationSize();
    Integer getRetiringFederationThreshold();
    Instant getRetiringFederationCreationTime();
    long getRetiringFederationCreationBlockNumber();
    byte[] getRetiringFederatorPublicKey(int index);
    byte[] getRetiringFederatorPublicKeyOfType(int index, FederationMember.KeyType keyType);
    List<UTXO> getRetiringFederationBtcUTXOs();

    @Nullable
    PendingFederation getPendingFederation();
    byte[] getPendingFederationHash();
    Integer getPendingFederationSize();
    byte[] getPendingFederatorPublicKey(int index);
    byte[] getPendingFederatorPublicKeyOfType(int index, FederationMember.KeyType keyType);

    Integer voteFederationChange(Transaction tx, ABICallSpec callSpec, SignatureCache signatureCache);
    long getActiveFederationCreationBlockHeight();
}
