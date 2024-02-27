package co.rsk.peg;

import co.rsk.bitcoinj.core.*;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.wallet.SendRequest;
import co.rsk.bitcoinj.wallet.Wallet;
import co.rsk.config.BridgeConstants;
import co.rsk.crypto.Keccak256;
import co.rsk.peg.federation.ErpFederation;
import co.rsk.peg.federation.Federation;
import co.rsk.peg.federation.FederationMember;
import co.rsk.peg.federation.PendingFederation;
import co.rsk.peg.utils.BridgeEventLogger;
import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.config.blockchain.upgrades.ConsensusRule;
import org.ethereum.core.Block;
import org.ethereum.core.SignatureCache;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static co.rsk.peg.ReleaseTransactionBuilder.BTC_TX_VERSION_2;
import static org.ethereum.config.blockchain.upgrades.ConsensusRule.*;

public class FederationChangeSupport {

    private final BridgeConstants bridgeConstants;
    private final BridgeStorageProvider provider;
    private final FederationSupport federationSupport;
    private final Block rskExecutionBlock;
    private final ActivationConfig.ForBlock activations;
    private final BridgeEventLogger eventLogger;
    private static final Logger logger = LoggerFactory.getLogger("FederationChangeSupport");
    private static final List<String> FEDERATION_CHANGE_FUNCTIONS = Collections.unmodifiableList(Arrays.asList(
        "create",
        "add",
        "add-multi",
        "commit",
        "rollback"
    ));
    public static final Integer FEDERATION_CHANGE_GENERIC_ERROR_CODE = -10;

    public FederationChangeSupport(BridgeConstants bridgeConstants,
                                   BridgeStorageProvider provider,
                                   FederationSupport federationSupport,
                                   Block rskExecutionBlock,
                                   ActivationConfig.ForBlock activations,
                                   BridgeEventLogger eventLogger
    ) {
        this.bridgeConstants = bridgeConstants;
        this.provider = provider;
        this.federationSupport = federationSupport;
        this.rskExecutionBlock = rskExecutionBlock;
        this.activations = activations;
        this.eventLogger = eventLogger;
    }

    /**
     * Creates a new pending federation
     * If there's currently no pending federation and no funds remain
     * to be moved from a previous federation, a new one is created.
     * Otherwise, -1 is returned if there's already a pending federation,
     * -2 is returned if there is a federation awaiting to be active,
     * or -3 if funds are left from a previous one.
     * @param dryRun whether to just do a dry run
     * @return 1 upon success, -1 when a pending federation is present,
     * -2 when a federation is to be activated,
     * and if -3 funds are still to be moved between federations.
     */
    protected Integer createFederation(boolean dryRun) {
        PendingFederation currentPendingFederation = provider.getPendingFederation();
        if (currentPendingFederation != null) {
            return -1;
        }

        boolean awaitingFedActivation = federationSupport.amAwaitingFederationActivation();
        if (awaitingFedActivation) {
            return -2;
        }

        Federation retiringFederation = federationSupport.getRetiringFederation();
        if (retiringFederation != null) {
            return -3;
        }

        if (dryRun) {
            return 1;
        }

        currentPendingFederation = new PendingFederation(Collections.emptyList());
        provider.setPendingFederation(currentPendingFederation);
        // Clear votes on election
        provider.getFederationElection(bridgeConstants.getFederationChangeAuthorizer()).clear();

        return 1;
    }

    /**
     * Adds the given keys to the current pending federation.
     *
     * @param dryRun whether to just do a dry run
     * @param btcKey the BTC public key to add
     * @param rskKey the RSK public key to add
     * @param mstKey the MST public key to add
     * @return 1 upon success, -1 if there was no pending federation, -2 if the key was already in the pending federation
     */
    protected Integer addFederatorPublicKeyMultikey(boolean dryRun, BtcECKey btcKey, ECKey rskKey, ECKey mstKey) {
        PendingFederation currentPendingFederation = provider.getPendingFederation();

        if (currentPendingFederation == null) {
            return -1;
        }

        if (currentPendingFederation.getBtcPublicKeys().contains(btcKey) ||
            currentPendingFederation.getMembers().stream().map(FederationMember::getRskPublicKey).anyMatch(k -> k.equals(rskKey)) ||
            currentPendingFederation.getMembers().stream().map(FederationMember::getMstPublicKey).anyMatch(k -> k.equals(mstKey))) {
            return -2;
        }

        if (dryRun) {
            return 1;
        }

        FederationMember member = new FederationMember(btcKey, rskKey, mstKey);

        currentPendingFederation = currentPendingFederation.addMember(member);

        provider.setPendingFederation(currentPendingFederation);

        return 1;
    }

    /**
     * Commits the currently pending federation.
     * That is, the retiring federation is set to be the currently active federation,
     * the active federation is replaced with a new federation generated from the pending federation,
     * and the pending federation is wiped out.
     * Also, UTXOs are moved from active to retiring so that the transfer of funds can
     * begin.
     * @param dryRun whether to just do a dry run
     * @param hash the pending federation's hash. This is checked the execution block's pending federation hash for equality.
     * @return 1 upon success, -1 if there was no pending federation, -2 if the pending federation was incomplete,
     * -3 if the given hash doesn't match the current pending federation's hash.
     */
    protected Integer commitFederation(boolean dryRun, Keccak256 hash) throws IOException {
        PendingFederation currentPendingFederation = provider.getPendingFederation();

        if (currentPendingFederation == null) {
            return -1;
        }

        if (!currentPendingFederation.isComplete()) {
            return -2;
        }

        if (!hash.equals(currentPendingFederation.getHash())) {
            return -3;
        }

        if (dryRun) {
            return 1;
        }

        // Move UTXOs from the new federation into the old federation
        // and clear the new federation's UTXOs
        List<UTXO> utxosToMove = new ArrayList<>(provider.getNewFederationBtcUTXOs());
        provider.getNewFederationBtcUTXOs().clear();
        List<UTXO> oldFederationUTXOs = provider.getOldFederationBtcUTXOs();
        oldFederationUTXOs.clear();
        oldFederationUTXOs.addAll(utxosToMove);

        // Network parameters for the new federation are taken from the bridge constants.
        // Creation time is the block's timestamp.
        Instant creationTime = Instant.ofEpochMilli(rskExecutionBlock.getTimestamp());
        Federation activeFederation = federationSupport.getActiveFederation();
        Federation oldFederation = activeFederation;

        provider.setOldFederation(oldFederation);
        provider.setNewFederation(
            currentPendingFederation.buildFederation(
                creationTime,
                rskExecutionBlock.getNumber(),
                bridgeConstants,
                activations
            )
        );
        provider.setPendingFederation(null);

        // Clear votes on election
        provider.getFederationElection(bridgeConstants.getFederationChangeAuthorizer()).clear();

        if (activations.isActive(RSKIP186)) {
            // Preserve federation change info
            long nextFederationCreationBlockHeight = rskExecutionBlock.getNumber();
            provider.setNextFederationCreationBlockHeight(nextFederationCreationBlockHeight);
            Script oldFederationP2SHScript = activations.isActive(RSKIP377) && oldFederation instanceof ErpFederation ?
                ((ErpFederation) oldFederation).getDefaultP2SHScript() : oldFederation.getP2SHScript();
            provider.setLastRetiredFederationP2SHScript(oldFederationP2SHScript);
        }

        logger.debug("[commitFederation] New Federation committed: {}", provider.getNewFederation().getAddress());
        eventLogger.logCommitFederation(rskExecutionBlock, provider.getOldFederation(), provider.getNewFederation());

        return 1;
    }

    /**
     * Rolls back the currently pending federation
     * That is, the pending federation is wiped out.
     * @param dryRun whether to just do a dry run
     * @return 1 upon success, 1 if there was no pending federation
     */
    protected Integer rollbackFederation(boolean dryRun) {
        PendingFederation currentPendingFederation = provider.getPendingFederation();

        if (currentPendingFederation == null) {
            return -1;
        }

        if (dryRun) {
            return 1;
        }

        provider.setPendingFederation(null);

        // Clear votes on election
        provider.getFederationElection(bridgeConstants.getFederationChangeAuthorizer()).clear();

        return 1;
    }

    public Integer voteFederationChange(Transaction tx, ABICallSpec callSpec, SignatureCache signatureCache) throws BridgeIllegalArgumentException {
        // Must be on one of the allowed functions
        if (!FEDERATION_CHANGE_FUNCTIONS.contains(callSpec.getFunction())) {
            return FEDERATION_CHANGE_GENERIC_ERROR_CODE;
        }

        AddressBasedAuthorizer authorizer = bridgeConstants.getFederationChangeAuthorizer();

        // Must be authorized to vote (checking for signature)
        if (!authorizer.isAuthorized(tx, signatureCache)) {
            return FEDERATION_CHANGE_GENERIC_ERROR_CODE;
        }

        // Try to do a dry-run and only register the vote if the
        // call would be successful
        ABICallVoteResult result;
        try {
            result = executeVoteFederationChangeFunction(true, callSpec);
        } catch (IOException | BridgeIllegalArgumentException e) {
            result = new ABICallVoteResult(false, FEDERATION_CHANGE_GENERIC_ERROR_CODE);
        }

        // Return if the dry run failed, or we are on a reversible execution
        if (!result.wasSuccessful()) {
            return (Integer) result.getResult();
        }

        ABICallElection election = provider.getFederationElection(authorizer);
        // Register the vote. It is expected to succeed, since all previous checks succeeded
        if (!election.vote(callSpec, tx.getSender(signatureCache))) {
            logger.warn("Unexpected federation change vote failure");
            return FEDERATION_CHANGE_GENERIC_ERROR_CODE;
        }

        // If enough votes have been reached, then actually execute the function
        ABICallSpec winnerSpec = election.getWinner();
        if (winnerSpec != null) {
            try {
                result = executeVoteFederationChangeFunction(false, winnerSpec);
            } catch (IOException e) {
                logger.warn("Unexpected federation change vote exception: {}", e.getMessage());
                return FEDERATION_CHANGE_GENERIC_ERROR_CODE;
            } finally {
                // Clear the winner so that we don't repeat ourselves
                election.clearWinners();
            }
        }

        return (Integer) result.getResult();
    }

    protected ABICallVoteResult executeVoteFederationChangeFunction(boolean dryRun, ABICallSpec callSpec) throws IOException, BridgeIllegalArgumentException {
        // Try to do a dry-run and only register the vote if the
        // call would be successful
        ABICallVoteResult result;
        Integer executionResult;
        switch (callSpec.getFunction()) {
            case "create":
                executionResult = createFederation(dryRun);
                result = new ABICallVoteResult(executionResult == 1, executionResult);
                break;
            case "add":
                byte[] publicKeyBytes = callSpec.getArguments()[0];
                BtcECKey publicKey;
                ECKey publicKeyEc;
                try {
                    publicKey = BtcECKey.fromPublicOnly(publicKeyBytes);
                    publicKeyEc = ECKey.fromPublicOnly(publicKeyBytes);
                } catch (Exception e) {
                    throw new BridgeIllegalArgumentException("Public key could not be parsed " + ByteUtil.toHexString(publicKeyBytes), e);
                }
                executionResult = addFederatorPublicKeyMultikey(dryRun, publicKey, publicKeyEc, publicKeyEc);
                result = new ABICallVoteResult(executionResult == 1, executionResult);
                break;
            case "add-multi":
                BtcECKey btcPublicKey;
                ECKey rskPublicKey;
                ECKey mstPublicKey;
                try {
                    btcPublicKey = BtcECKey.fromPublicOnly(callSpec.getArguments()[0]);
                } catch (Exception e) {
                    throw new BridgeIllegalArgumentException("BTC public key could not be parsed " + ByteUtil.toHexString(callSpec.getArguments()[0]), e);
                }

                try {
                    rskPublicKey = ECKey.fromPublicOnly(callSpec.getArguments()[1]);
                } catch (Exception e) {
                    throw new BridgeIllegalArgumentException("RSK public key could not be parsed " + ByteUtil.toHexString(callSpec.getArguments()[1]), e);
                }

                try {
                    mstPublicKey = ECKey.fromPublicOnly(callSpec.getArguments()[2]);
                } catch (Exception e) {
                    throw new BridgeIllegalArgumentException("MST public key could not be parsed " + ByteUtil.toHexString(callSpec.getArguments()[2]), e);
                }
                executionResult = addFederatorPublicKeyMultikey(dryRun, btcPublicKey, rskPublicKey, mstPublicKey);
                result = new ABICallVoteResult(executionResult == 1, executionResult);
                break;
            case "commit":
                Keccak256 hash = new Keccak256(callSpec.getArguments()[0]);
                executionResult = commitFederation(dryRun, hash);
                result = new ABICallVoteResult(executionResult == 1, executionResult);
                break;
            case "rollback":
                executionResult = rollbackFederation(dryRun);
                result = new ABICallVoteResult(executionResult == 1, executionResult);
                break;
            default:
                // Fail by default
                result = new ABICallVoteResult(false, FEDERATION_CHANGE_GENERIC_ERROR_CODE);
        }

        return result;
    }

}
