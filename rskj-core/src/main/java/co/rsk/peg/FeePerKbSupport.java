package co.rsk.peg;

import co.rsk.bitcoinj.core.Coin;
import co.rsk.config.BridgeConstants;
import co.rsk.core.RskAddress;
import org.ethereum.core.Repository;
import org.ethereum.core.SignatureCache;
import org.ethereum.core.Transaction;
import org.ethereum.vm.PrecompiledContracts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FeePerKbSupport {
    public static final Integer FEE_PER_KB_GENERIC_ERROR_CODE = -10;
    public static final Integer NEGATIVE_FEE_PER_KB_ERROR_CODE = -1;
    public static final Integer EXCESSIVE_FEE_PER_KB_ERROR_CODE = -2;
    private final FeePerKbStorageProvider provider;
    private final BridgeConstants bridgeConstants;
    private static final Logger logger = LoggerFactory.getLogger("FeePerKbSupport");

    public FeePerKbSupport(BridgeConstants bridgeConstants, FeePerKbStorageProvider provider) {
        this.provider = provider;
        this.bridgeConstants = bridgeConstants;
    }

    /**
     * @return Current fee per kb in BTC.
     */
    public Coin getFeePerKb() {
        Coin currentFeePerKb = provider.getFeePerKb();

        if (currentFeePerKb == null) {
            currentFeePerKb = bridgeConstants.getGenesisFeePerKb();
        }

        return currentFeePerKb;
    }

    /**
     * Votes for a fee per kb value.
     *
     * @return 1 upon successful vote, -1 when the vote was unsuccessful,
     * FEE_PER_KB_GENERIC_ERROR_CODE when there was an un expected error.
     */
    public Integer voteFeePerKbChange(Transaction tx, Coin feePerKb, SignatureCache signatureCache) {

        AddressBasedAuthorizer authorizer = bridgeConstants.getFeePerKbChangeAuthorizer();
        Coin maxFeePerKb = bridgeConstants.getMaxFeePerKb();

        if (!authorizer.isAuthorized(tx, signatureCache)) {
            return FEE_PER_KB_GENERIC_ERROR_CODE;
        }

        if(!feePerKb.isPositive()){
            return NEGATIVE_FEE_PER_KB_ERROR_CODE;
        }

        if(feePerKb.isGreaterThan(maxFeePerKb)) {
            return EXCESSIVE_FEE_PER_KB_ERROR_CODE;
        }

        ABICallElection feePerKbElection = provider.getFeePerKbElection(authorizer);
        ABICallSpec feeVote = new ABICallSpec("setFeePerKb", new byte[][]{BridgeSerializationUtils.serializeCoin(feePerKb)});
        boolean successfulVote = feePerKbElection.vote(feeVote, tx.getSender(signatureCache));
        if (!successfulVote) {
            return -1;
        }

        ABICallSpec winner = feePerKbElection.getWinner();
        if (winner == null) {
            logger.info("Successful fee per kb vote for {}", feePerKb);
            return 1;
        }

        Coin winnerFee;
        try {
            winnerFee = BridgeSerializationUtils.deserializeCoin(winner.getArguments()[0]);
        } catch (Exception e) {
            logger.warn("Exception deserializing winner feePerKb", e);
            return FEE_PER_KB_GENERIC_ERROR_CODE;
        }

        if (winnerFee == null) {
            logger.warn("Invalid winner feePerKb: feePerKb can't be null");
            return FEE_PER_KB_GENERIC_ERROR_CODE;
        }

        if (!winnerFee.equals(feePerKb)) {
            logger.debug("Winner fee is different than the last vote: maybe you forgot to clear winners");
        }

        logger.info("Fee per kb changed to {}", winnerFee);
        provider.setFeePerKb(winnerFee);
        feePerKbElection.clear();

        return 1;
    }
}
