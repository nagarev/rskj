package co.rsk.peg;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.Context;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.config.BridgeConstants;
import co.rsk.config.BridgeMainNetConstants;
import co.rsk.config.BridgeRegTestConstants;
import co.rsk.peg.bitcoin.BitcoinTestUtils;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.config.blockchain.upgrades.ActivationConfigsForTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static co.rsk.peg.PegTestUtils.createFederation;
import static org.ethereum.config.blockchain.upgrades.ConsensusRule.RSKIP186;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PegUtilsLegacyGetTransactionTypeTest {
    private static final BridgeConstants bridgeMainnetConstants = BridgeMainNetConstants.getInstance();
    private static final NetworkParameters btcMainnetParams = bridgeMainnetConstants.getBtcParams();

    private static final List<BtcECKey> REGTEST_OLD_FEDERATION_PRIVATE_KEYS = Arrays.asList(
        BtcECKey.fromPrivate(Hex.decode("47129ffed2c0273c75d21bb8ba020073bb9a1638df0e04853407461fdd9e8b83")),
        BtcECKey.fromPrivate(Hex.decode("9f72d27ba603cfab5a0201974a6783ca2476ec3d6b4e2625282c682e0e5f1c35")),
        BtcECKey.fromPrivate(Hex.decode("e1b17fcd0ef1942465eee61b20561b16750191143d365e71de08b33dd84a9788"))
    );

    private static final Address oldFederationAddress = Address.fromBase58(
        bridgeMainnetConstants.getBtcParams(),
        bridgeMainnetConstants.getOldFederationAddress()
    );

    private static Context btcContext;

    @BeforeEach
    void setUp() {
        btcContext = new Context(btcMainnetParams);
        Context.propagate(btcContext);
    }

    @Test
    void getTransactionType_sentFromP2SHErpFed() {
        ActivationConfig.ForBlock activations = ActivationConfigsForTest.hop400().forBlock(0);

        // Arrange
        Federation activeFederation = new P2shErpFederation(
            bridgeMainnetConstants.getGenesisFederation().getMembers(),
            bridgeMainnetConstants.getGenesisFederation().getCreationTime(),
            5L,
            bridgeMainnetConstants.getGenesisFederation().getBtcParams(),
            bridgeMainnetConstants.getErpFedPubKeysList(),
            bridgeMainnetConstants.getErpFedActivationDelay(),
            activations
        );

        List<BtcECKey> fedKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
            new String[]{"fa01", "fa02", "fa03"}, true
        );

        P2shErpFederation p2shRetiringFederation = new P2shErpFederation(
            FederationTestUtils.getFederationMembersWithBtcKeys(fedKeys),
            Instant.ofEpochMilli(1000L),
            0L,
            btcMainnetParams,
            bridgeMainnetConstants.getErpFedPubKeysList(),
            bridgeMainnetConstants.getErpFedActivationDelay(),
            activations
        );

        // Create a migrationTx from the p2sh erp fed
        Coin minimumPeginTxValue = bridgeMainnetConstants.getMinimumPeginTxValue(activations);

        BtcTransaction migrationTx = new BtcTransaction(btcMainnetParams);
        migrationTx.addInput(Sha256Hash.ZERO_HASH, 0, p2shRetiringFederation.getRedeemScript());
        migrationTx.addOutput(minimumPeginTxValue, activeFederation.getAddress());

        FederationTestUtils.addSignatures(p2shRetiringFederation, fedKeys, migrationTx);

        // Act
        PegTxType transactionType = PegUtilsLegacy.getTransactionType(
            migrationTx,
            activeFederation,
            p2shRetiringFederation,
            null,
            oldFederationAddress,
            activations,
            minimumPeginTxValue,
            new BridgeBtcWallet(btcContext, Arrays.asList(activeFederation, p2shRetiringFederation))
        );

        // Assert
        Assertions.assertEquals(PegTxType.PEGOUT_OR_MIGRATION, transactionType);
    }

    private static Stream<Arguments> getTransactionType_sentFromRetiredFed_Args() {
        return Stream.of(
            Arguments.of(ActivationConfigsForTest.papyrus200().forBlock(0), PegTxType.PEGIN),
            Arguments.of(ActivationConfigsForTest.iris300().forBlock(0), PegTxType.PEGOUT_OR_MIGRATION)
        );
    }

    @ParameterizedTest
    @MethodSource("getTransactionType_sentFromRetiredFed_Args")
    void getTransactionType_sentFromRetiredFed(ActivationConfig.ForBlock activations, PegTxType expectedTxType) {
        BridgeConstants bridgeRegTestConstants = BridgeRegTestConstants.getInstance();
        NetworkParameters btcRegTestsParams = bridgeRegTestConstants.getBtcParams();
        Context.propagate(new Context(btcRegTestsParams));

        // Arrange
        Federation activeFederation = new Federation(
            bridgeRegTestConstants.getGenesisFederation().getMembers(),
            bridgeRegTestConstants.getGenesisFederation().getCreationTime(),
            5L,
            bridgeRegTestConstants.getGenesisFederation().getBtcParams()
        );

        Federation retiredFederation = createFederation(bridgeRegTestConstants, REGTEST_OLD_FEDERATION_PRIVATE_KEYS);

        // Create a migrationTx from the old fed address to the active fed
        BtcTransaction migrationTx = new BtcTransaction(btcRegTestsParams);
        migrationTx.addInput(BitcoinTestUtils.createHash(1), 0, retiredFederation.getRedeemScript());
        migrationTx.addOutput(Coin.COIN, activeFederation.getAddress());

        FederationTestUtils.addSignatures(retiredFederation, REGTEST_OLD_FEDERATION_PRIVATE_KEYS, migrationTx);

        // Act
        PegTxType transactionType = PegUtilsLegacy.getTransactionType(
            migrationTx,
            activeFederation,
            null,
            activations.isActive(RSKIP186)? retiredFederation.getP2SHScript(): null,
            oldFederationAddress,
            activations,
            bridgeMainnetConstants.getMinimumPeginTxValue(activations),
            new BridgeBtcWallet(btcContext, Collections.singletonList(activeFederation))
        );

        // Assert
        Assertions.assertEquals(bridgeRegTestConstants.getOldFederationAddress(), retiredFederation.getAddress().toString());
        Assertions.assertEquals(expectedTxType, transactionType);
    }

    @Test
    void getTransactionType_sentFromP2SH_pegin() {
        // Arrange
        ActivationConfig.ForBlock activations = ActivationConfigsForTest.hop400().forBlock(0);

        Federation activeFederation = bridgeMainnetConstants.getGenesisFederation();

        List<BtcECKey> multiSigKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
            new String[]{"fa01", "fa02", "fa03"}, true
        );

        multiSigKeys.sort(BtcECKey.PUBKEY_COMPARATOR);
        Script redeemScript = ScriptBuilder.createRedeemScript((multiSigKeys.size() / 2) + 1, multiSigKeys);

        // Create a peginTx from the p2sh erp fed
        Coin minimumPeginTxValue = bridgeMainnetConstants.getMinimumPeginTxValue(activations);

        BtcTransaction peginTx = new BtcTransaction(btcMainnetParams);
        peginTx.addInput(Sha256Hash.ZERO_HASH, 0, redeemScript);
        peginTx.addOutput(minimumPeginTxValue, activeFederation.getAddress());

        // Act
        PegTxType transactionType = PegUtilsLegacy.getTransactionType(
            peginTx,
            activeFederation,
            null,
            null,
            oldFederationAddress,
            activations,
            bridgeMainnetConstants.getMinimumPeginTxValue(activations),
            new BridgeBtcWallet(btcContext, Collections.singletonList(activeFederation))
        );

        // Assert
        Assertions.assertEquals(PegTxType.PEGIN, transactionType);
    }

    private static Stream<Arguments> getTransactionType_pegin_Args() {
        ActivationConfig.ForBlock papyrusActivations = ActivationConfigsForTest.papyrus200().forBlock(0);
        ActivationConfig.ForBlock iris300Activations = ActivationConfigsForTest.iris300().forBlock(0);

        return Stream.of(
            Arguments.of(
                papyrusActivations,
                PegTxType.PEGIN,
                bridgeMainnetConstants.getMinimumPeginTxValue(papyrusActivations)
            ),
            Arguments.of(
                papyrusActivations,
                PegTxType.UNKNOWN,
                bridgeMainnetConstants.getMinimumPeginTxValue(papyrusActivations).minus(Coin.SATOSHI)
            ),
            Arguments.of(
                iris300Activations,
                PegTxType.PEGIN,
                bridgeMainnetConstants.getMinimumPeginTxValue(iris300Activations)
            ),
            Arguments.of(
                iris300Activations,
                PegTxType.UNKNOWN,
                bridgeMainnetConstants.getMinimumPeginTxValue(iris300Activations).minus(Coin.SATOSHI)
            )
        );
    }

    @ParameterizedTest
    @MethodSource("getTransactionType_pegin_Args")
    void getTransactionType_pegin(
        ActivationConfig.ForBlock activations,
        PegTxType expectedTxType,
        Coin amountToSend
    ) {
        // Arrange

        Federation activeFederation = bridgeMainnetConstants.getGenesisFederation();

        BtcTransaction peginTx = new BtcTransaction(btcMainnetParams);
        peginTx.addInput(BitcoinTestUtils.createHash(1), 0, new Script(new byte[]{}));
        peginTx.addOutput(amountToSend, activeFederation.getAddress());

        // Act
        PegTxType transactionType = PegUtilsLegacy.getTransactionType(
            peginTx,
            activeFederation,
            null,
            null,
            oldFederationAddress,
            activations,
            bridgeMainnetConstants.getMinimumPeginTxValue(activations),
            new BridgeBtcWallet(btcContext, Collections.singletonList(activeFederation))
        );

        // Assert
        Assertions.assertEquals(expectedTxType, transactionType);
    }

    @Test
    void getTransactionType_pegout_tx() {
        // Arrange
        ActivationConfig.ForBlock activations = mock(ActivationConfig.ForBlock.class);

        List<BtcECKey> fedKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
            new String[]{"fa01", "fa02", "fa03"}, true
        );

        Federation activeFederation = new Federation(
            FederationTestUtils.getFederationMembersWithBtcKeys(fedKeys),
            Instant.ofEpochMilli(1000L),
            0L,
            btcMainnetParams
        );

        // Create a pegoutBtcTx from active fed to a user btc address
        Address userAddress = BitcoinTestUtils.createP2PKHAddress(btcMainnetParams, "user");

        BtcTransaction pegoutBtcTx = new BtcTransaction(btcMainnetParams);
        pegoutBtcTx.addInput(BitcoinTestUtils.createHash(1), 0, activeFederation.getRedeemScript());
        pegoutBtcTx.addOutput(Coin.COIN, userAddress);

        FederationTestUtils.addSignatures(activeFederation, fedKeys, pegoutBtcTx);

        // Act
        PegTxType transactionType = PegUtilsLegacy.getTransactionType(
            pegoutBtcTx,
            activeFederation,
            null,
            null,
            oldFederationAddress,
            activations,
            bridgeMainnetConstants.getMinimumPeginTxValue(activations),
            new BridgeBtcWallet(btcContext, Collections.singletonList(activeFederation))
        );

        //Assert
        Assertions.assertEquals(PegTxType.PEGOUT_OR_MIGRATION, transactionType);
    }

    @Test
    void getTransactionType_migration_tx() {
        // Arrange
        ActivationConfig.ForBlock activations = mock(ActivationConfig.ForBlock.class);

        Federation activeFederation = FederationTestUtils.getFederation(100, 200, 300);

        List<BtcECKey> retiringFedKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
            new String[]{"fa01", "fa02", "fa03"}, true
        );;

        Federation retiringFederation = new Federation(
            FederationTestUtils.getFederationMembersWithBtcKeys(retiringFedKeys),
            Instant.ofEpochMilli(1000L),
            0L,
            btcMainnetParams
        );

        BtcTransaction migrationTx = new BtcTransaction(btcMainnetParams);
        migrationTx.addInput(BitcoinTestUtils.createHash(1), 0, retiringFederation.getRedeemScript());
        migrationTx.addOutput(Coin.COIN, activeFederation.getAddress());

        FederationTestUtils.addSignatures(retiringFederation, retiringFedKeys, migrationTx);

        // Act
        PegTxType transactionType = PegUtilsLegacy.getTransactionType(
            migrationTx,
            activeFederation,
            retiringFederation,
            null,
            oldFederationAddress,
            activations,
            bridgeMainnetConstants.getMinimumPeginTxValue(activations),
            new BridgeBtcWallet(btcContext, Arrays.asList(activeFederation, retiringFederation))
        );

        // Assert
        Assertions.assertEquals(PegTxType.PEGOUT_OR_MIGRATION, transactionType);
    }

    @Test
    void getTransactionType_unknown_tx() {
        // Arrange
        Federation activeFederation = bridgeMainnetConstants.getGenesisFederation();

        ActivationConfig.ForBlock activations = mock(ActivationConfig.ForBlock.class);

        Address unknownAddress = BitcoinTestUtils.createP2PKHAddress(btcMainnetParams, "unknown");

        BtcTransaction unknownPegTx = new BtcTransaction(btcMainnetParams);
        unknownPegTx.addInput(BitcoinTestUtils.createHash(1), 0, new Script(new byte[]{}));
        unknownPegTx.addOutput(Coin.COIN, unknownAddress);

        // Act
        PegTxType transactionType = PegUtilsLegacy.getTransactionType(
            unknownPegTx,
            activeFederation,
            null,
            null,
            oldFederationAddress,
            activations,
            bridgeMainnetConstants.getMinimumPeginTxValue(activations),
            new BridgeBtcWallet(btcContext, Collections.singletonList(activeFederation))
        );

        // Assert
        Assertions.assertEquals(PegTxType.UNKNOWN, transactionType);
    }
}