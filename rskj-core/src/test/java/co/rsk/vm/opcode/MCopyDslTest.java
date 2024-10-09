package co.rsk.vm.opcode;

import co.rsk.config.TestSystemProperties;
import co.rsk.test.World;
import co.rsk.test.dsl.DslParser;
import co.rsk.test.dsl.DslProcessorException;
import co.rsk.test.dsl.WorldDslProcessor;
import com.typesafe.config.ConfigValueFactory;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.core.util.TransactionReceiptUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

public class MCopyDslTest {

    @Test
    void testMCOPY_whenNotActivated_behavesAsExpected() throws FileNotFoundException, DslProcessorException {

        // Test Config Setup

        TestSystemProperties configWithRskip445Disabled = new TestSystemProperties(rawConfig ->
                rawConfig.withValue("blockchain.config.hardforkActivationHeights.lovell700", ConfigValueFactory.fromAnyRef(-1))
        );

        // Test Setup

        DslParser parser = DslParser.fromResource("dsl/opcode/mcopy/testRSKIPNotActivatedTest.txt");
        World world = new World(configWithRskip445Disabled);
        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        // Assertions

        // There's one block (b01) containing only 1 transaction
        Block block1 = world.getBlockByName("b01");
        Assertions.assertNotNull(block1);
        Assertions.assertEquals(1, block1.getTransactionsList().size());

        // There's a transaction called txTestMCopy
        Transaction txTestMCopy = world.getTransactionByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopy);

        // Transaction txTestMCopy has a transaction receipt
        TransactionReceipt txTestMCopyReceipt = world.getTransactionReceiptByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopyReceipt);

        // Transaction txTestMCopy has been processed correctly
        byte[] creationStatus = txTestMCopyReceipt.getStatus();
        Assertions.assertNotNull(creationStatus);
        Assertions.assertEquals(1, creationStatus.length);
        Assertions.assertEquals(1, creationStatus[0]);

        // There's one block (b02) containing only 1 transaction
        Block block2 = world.getBlockByName("b02");
        Assertions.assertNotNull(block2);
        Assertions.assertEquals(1, block2.getTransactionsList().size());

        // There's a transaction called txTestMCopyNotActivated
        Transaction txTestMCopyNotActivated = world.getTransactionByName("txTestMCopyNotActivated");
        Assertions.assertNotNull(txTestMCopyNotActivated);

        // Transaction txTestMCopyNotActivated has a transaction receipt
        TransactionReceipt txTestMCopyNotActivatedReceipt = world.getTransactionReceiptByName("txTestMCopyNotActivated");
        Assertions.assertNotNull(txTestMCopyNotActivatedReceipt);

        // Transaction txTestMCopyNotActivated has failed
        byte[] txTestMCopyNotActivatedCreationStatus = txTestMCopyNotActivatedReceipt.getStatus();
        Assertions.assertNotNull(txTestMCopyNotActivatedCreationStatus);
        Assertions.assertEquals(0, txTestMCopyNotActivatedCreationStatus.length);

    }

    @Test
    void testMCOPY_testCase1_behavesAsExpected() throws FileNotFoundException, DslProcessorException {

        DslParser parser = DslParser.fromResource("dsl/opcode/mcopy/testCopying32BytesFromOffset32toOffset0.txt");
        World world = new World();
        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        // Assertions

        // There's one block (b01) containing only 1 transaction
        Block block1 = world.getBlockByName("b01");
        Assertions.assertNotNull(block1);
        Assertions.assertEquals(1, block1.getTransactionsList().size());

        // There's a transaction called txTestMCopy
        Transaction txTestMCopy = world.getTransactionByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopy);

        // Transaction txTestMCopy has a transaction receipt
        TransactionReceipt txTestMCopyReceipt = world.getTransactionReceiptByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopyReceipt);

        // Transaction txTestMCopy has been processed correctly
        byte[] creationStatus = txTestMCopyReceipt.getStatus();
        Assertions.assertNotNull(creationStatus);
        Assertions.assertEquals(1, creationStatus.length);
        Assertions.assertEquals(1, creationStatus[0]);

        // There's one block (b02) containing only 1 transaction
        Block block2 = world.getBlockByName("b02");
        Assertions.assertNotNull(block2);
        Assertions.assertEquals(1, block2.getTransactionsList().size());

        // There's a transaction called txTestMCopyOKCall
        Transaction txTestMCopyOKCall = world.getTransactionByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCall);

        // Transaction txTestMCopyOKCall has a transaction receipt
        TransactionReceipt txTestMCopyOKCallReceipt = world.getTransactionReceiptByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCallReceipt);

        // Transaction txTestMCopyOKCall has been processed correctly
        byte[] txTestMCopyOKCallCreationStatus = txTestMCopyOKCallReceipt.getStatus();
        Assertions.assertNotNull(txTestMCopyOKCallCreationStatus);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus.length);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus[0]);

        // Check events
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "OK", null));
        Assertions.assertEquals(0, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "ERROR", null));

    }

    @Test
    void testMCOPY_testCase2_behavesAsExpected() throws FileNotFoundException, DslProcessorException {

        DslParser parser = DslParser.fromResource("dsl/opcode/mcopy/testCopying32BytesFromOffset0toOffset0.txt");
        World world = new World();
        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        // Assertions

        // There's one block (b01) containing only 1 transaction
        Block block1 = world.getBlockByName("b01");
        Assertions.assertNotNull(block1);
        Assertions.assertEquals(1, block1.getTransactionsList().size());

        // There's a transaction called txTestMCopy
        Transaction txTestMCopy = world.getTransactionByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopy);

        // Transaction txTestMCopy has a transaction receipt
        TransactionReceipt txTestMCopyReceipt = world.getTransactionReceiptByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopyReceipt);

        // Transaction txTestMCopy has been processed correctly
        byte[] creationStatus = txTestMCopyReceipt.getStatus();
        Assertions.assertNotNull(creationStatus);
        Assertions.assertEquals(1, creationStatus.length);
        Assertions.assertEquals(1, creationStatus[0]);

        // There's one block (b02) containing only 1 transaction
        Block block2 = world.getBlockByName("b02");
        Assertions.assertNotNull(block2);
        Assertions.assertEquals(1, block2.getTransactionsList().size());

        // There's a transaction called txTestMCopyOKCall
        Transaction txTestMCopyOKCall = world.getTransactionByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCall);

        // Transaction txTestMCopyOKCall has a transaction receipt
        TransactionReceipt txTestMCopyOKCallReceipt = world.getTransactionReceiptByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCallReceipt);

        // Transaction txTestMCopyOKCall has been processed correctly
        byte[] txTestMCopyOKCallCreationStatus = txTestMCopyOKCallReceipt.getStatus();
        Assertions.assertNotNull(txTestMCopyOKCallCreationStatus);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus.length);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus[0]);

        // Check events
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "OK", null));
        Assertions.assertEquals(0, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "ERROR", null));

    }

    @Test
    void testMCOPY_testCase3_behavesAsExpected() throws FileNotFoundException, DslProcessorException {

        DslParser parser = DslParser.fromResource("dsl/opcode/mcopy/testCopying8BytesFromOffset1toOffset0.txt");
        World world = new World();
        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        // Assertions

        // There's one block (b01) containing only 1 transaction
        Block block1 = world.getBlockByName("b01");
        Assertions.assertNotNull(block1);
        Assertions.assertEquals(1, block1.getTransactionsList().size());

        // There's a transaction called txTestMCopy
        Transaction txTestMCopy = world.getTransactionByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopy);

        // Transaction txTestMCopy has a transaction receipt
        TransactionReceipt txTestMCopyReceipt = world.getTransactionReceiptByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopyReceipt);

        // Transaction txTestMCopy has been processed correctly
        byte[] creationStatus = txTestMCopyReceipt.getStatus();
        Assertions.assertNotNull(creationStatus);
        Assertions.assertEquals(1, creationStatus.length);
        Assertions.assertEquals(1, creationStatus[0]);

        // There's one block (b02) containing only 1 transaction
        Block block2 = world.getBlockByName("b02");
        Assertions.assertNotNull(block2);
        Assertions.assertEquals(1, block2.getTransactionsList().size());

        // There's a transaction called txTestMCopyOKCall
        Transaction txTestMCopyOKCall = world.getTransactionByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCall);

        // Transaction txTestMCopyOKCall has a transaction receipt
        TransactionReceipt txTestMCopyOKCallReceipt = world.getTransactionReceiptByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCallReceipt);

        // Transaction txTestMCopyOKCall has been processed correctly
        byte[] txTestMCopyOKCallCreationStatus = txTestMCopyOKCallReceipt.getStatus();
        Assertions.assertNotNull(txTestMCopyOKCallCreationStatus);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus.length);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus[0]);

        // Check events
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "OK", null));
        Assertions.assertEquals(0, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "ERROR", null));

    }

    @Test
    void testMCOPY_testCase4_behavesAsExpected() throws FileNotFoundException, DslProcessorException {

        DslParser parser = DslParser.fromResource("dsl/opcode/mcopy/testCopying8BytesFromOffset0toOffset1.txt");
        World world = new World();
        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        // Assertions

        // There's one block (b01) containing only 1 transaction
        Block block1 = world.getBlockByName("b01");
        Assertions.assertNotNull(block1);
        Assertions.assertEquals(1, block1.getTransactionsList().size());

        // There's a transaction called txTestMCopy
        Transaction txTestMCopy = world.getTransactionByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopy);

        // Transaction txTestMCopy has a transaction receipt
        TransactionReceipt txTestMCopyReceipt = world.getTransactionReceiptByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopyReceipt);

        // Transaction txTestMCopy has been processed correctly
        byte[] creationStatus = txTestMCopyReceipt.getStatus();
        Assertions.assertNotNull(creationStatus);
        Assertions.assertEquals(1, creationStatus.length);
        Assertions.assertEquals(1, creationStatus[0]);

        // There's one block (b02) containing only 1 transaction
        Block block2 = world.getBlockByName("b02");
        Assertions.assertNotNull(block2);
        Assertions.assertEquals(1, block2.getTransactionsList().size());

        // There's a transaction called txTestMCopyOKCall
        Transaction txTestMCopyOKCall = world.getTransactionByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCall);

        // Transaction txTestMCopyOKCall has a transaction receipt
        TransactionReceipt txTestMCopyOKCallReceipt = world.getTransactionReceiptByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCallReceipt);

        // Transaction txTestMCopyOKCall has been processed correctly
        byte[] txTestMCopyOKCallCreationStatus = txTestMCopyOKCallReceipt.getStatus();
        Assertions.assertNotNull(txTestMCopyOKCallCreationStatus);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus.length);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus[0]);

        // Check events
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "OK", null));
        Assertions.assertEquals(0, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "ERROR", null));

    }

    // Advanced Overwrite Test Cases
    // https://github.com/ethereum/execution-spec-tests/blob/c0065176a79f89d93f4c326186fc257ec5b8d5f1/tests/cancun/eip5656_mcopy/test_mcopy.py)

    @Test
    void testMCOPY_overwriteCases_behaveAsExpected() throws FileNotFoundException, DslProcessorException {

        DslParser parser = DslParser.fromResource("dsl/opcode/mcopy/testOverwriteCases.txt");
        World world = new World();
        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        // Assertions

        // There's one block (b01) containing only 1 transaction
        Block block1 = world.getBlockByName("b01");
        Assertions.assertNotNull(block1);
        Assertions.assertEquals(1, block1.getTransactionsList().size());

        // There's a transaction called txTestMCopy
        Transaction txTestMCopy = world.getTransactionByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopy);

        // Transaction txTestMCopy has a transaction receipt
        TransactionReceipt txTestMCopyReceipt = world.getTransactionReceiptByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopyReceipt);

        // Transaction txTestMCopy has been processed correctly
        byte[] creationStatus = txTestMCopyReceipt.getStatus();
        Assertions.assertNotNull(creationStatus);
        Assertions.assertEquals(1, creationStatus.length);
        Assertions.assertEquals(1, creationStatus[0]);

        // There's one block (b02) containing only 1 transaction
        Block block2 = world.getBlockByName("b02");
        Assertions.assertNotNull(block2);
        Assertions.assertEquals(1, block2.getTransactionsList().size());

        // There's a transaction called txTestMCopyOKCall
        Transaction txTestMCopyOKCall = world.getTransactionByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCall);

        // Transaction txTestMCopyOKCall has a transaction receipt
        TransactionReceipt txTestMCopyOKCallReceipt = world.getTransactionReceiptByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCallReceipt);

        // Transaction txTestMCopyOKCall has been processed correctly
        byte[] txTestMCopyOKCallCreationStatus = txTestMCopyOKCallReceipt.getStatus();
        Assertions.assertNotNull(txTestMCopyOKCallCreationStatus);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus.length);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus[0]);

        // Check events
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "ZERO_INPUTS_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "SINGLE_BYTE_REWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "FULL_WORD_REWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "SINGLE_BYTE_FWD_OVERWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "FULL_WORD_FWD_OVERWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "MID_WORD_SINGLE_BYTE_REWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "MID_WORD_SINGLE_WORD_REWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "MID_WORD_MULTY_WORD_REWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "TWO_WORDS_FWD_OVERWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "TWO_WORDS_BWD_OVERWRITE_OK", null));
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "TWO_WORDS_BWD_OVERWRITE_SINGLE_BYTE_OFFSET_OK", null));

        Assertions.assertEquals(0, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "ERROR", null));

    }

    // Full Memory Copy/Rewrite/Clean Tests

    @Test
    void testMCOPY_fullMemoryClean_behaveAsExpected() throws FileNotFoundException, DslProcessorException {

        DslParser parser = DslParser.fromResource("dsl/opcode/mcopy/testFullMemoryClean.txt");
        World world = new World();
        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        // Assertions

        // There's one block (b01) containing only 1 transaction
        Block block1 = world.getBlockByName("b01");
        Assertions.assertNotNull(block1);
        Assertions.assertEquals(1, block1.getTransactionsList().size());

        // There's a transaction called txTestMCopy
        Transaction txTestMCopy = world.getTransactionByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopy);

        // Transaction txTestMCopy has a transaction receipt
        TransactionReceipt txTestMCopyReceipt = world.getTransactionReceiptByName("txTestMCopy");
        Assertions.assertNotNull(txTestMCopyReceipt);

        // Transaction txTestMCopy has been processed correctly
        byte[] creationStatus = txTestMCopyReceipt.getStatus();
        Assertions.assertNotNull(creationStatus);
        Assertions.assertEquals(1, creationStatus.length);
        Assertions.assertEquals(1, creationStatus[0]);

        // There's one block (b02) containing only 1 transaction
        Block block2 = world.getBlockByName("b02");
        Assertions.assertNotNull(block2);
        Assertions.assertEquals(1, block2.getTransactionsList().size());

        // There's a transaction called txTestMCopyOKCall
        Transaction txTestMCopyOKCall = world.getTransactionByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCall);

        // Transaction txTestMCopyOKCall has a transaction receipt
        TransactionReceipt txTestMCopyOKCallReceipt = world.getTransactionReceiptByName("txTestMCopyOKCall");
        Assertions.assertNotNull(txTestMCopyOKCallReceipt);

        // Transaction txTestMCopyOKCall has been processed correctly
        byte[] txTestMCopyOKCallCreationStatus = txTestMCopyOKCallReceipt.getStatus();
        Assertions.assertNotNull(txTestMCopyOKCallCreationStatus);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus.length);
        Assertions.assertEquals(1, txTestMCopyOKCallCreationStatus[0]);

        // Check events
        Assertions.assertEquals(1, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "OK", null));
        Assertions.assertEquals(0, TransactionReceiptUtil.getEventCount(txTestMCopyOKCallReceipt, "ERROR", null));

    }

}
