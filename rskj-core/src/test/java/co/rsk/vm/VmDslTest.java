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

package co.rsk.vm;

import co.rsk.config.TestSystemProperties;
import co.rsk.test.World;
import co.rsk.test.dsl.DslParser;
import co.rsk.test.dsl.DslProcessorException;
import co.rsk.test.dsl.WorldDslProcessor;
import com.typesafe.config.ConfigValueFactory;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.vm.DataWord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Created by ajlopez on 15/04/2020.
 */
class VmDslTest {
    @Test
    void invokeRecursiveContractsUsing400Levels() throws FileNotFoundException, DslProcessorException {
        System.gc();
        DslParser parser = DslParser.fromResource("dsl/recursive01.txt");
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        Block block = world.getBlockByName("b02");

        Assertions.assertNotNull(block);
        Assertions.assertEquals(1, block.getTransactionsList().size());

        Transaction creationTransaction = world.getTransactionByName("tx01");

        Assertions.assertNotNull(creationTransaction);

        DataWord counterValue = world
                .getRepositoryLocator()
                .snapshotAt(block.getHeader())
                .getStorageValue(creationTransaction.getContractAddress(), DataWord.ZERO);

        Assertions.assertNotNull(counterValue);
        Assertions.assertEquals(200, counterValue.intValue());

        TransactionReceipt transactionReceipt = world.getTransactionReceiptByName("tx02");

        Assertions.assertNotNull(transactionReceipt);

        byte[] status = transactionReceipt.getStatus();

        Assertions.assertNotNull(status);
        Assertions.assertEquals(1, status.length);
        Assertions.assertEquals(1, status[0]);
    }

    @Test
    void invokeRecursiveContractsUsing401Levels() throws FileNotFoundException, DslProcessorException {
        DslParser parser = DslParser.fromResource("dsl/recursive02.txt");
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        processor.processCommands(parser);

        Block block = world.getBlockByName("b02");

        Assertions.assertNotNull(block);
        Assertions.assertEquals(1, block.getTransactionsList().size());

        Transaction creationTransaction = world.getTransactionByName("tx01");

        Assertions.assertNotNull(creationTransaction);

        DataWord counterValue = world
                .getRepositoryLocator()
                .snapshotAt(block.getHeader())
                .getStorageValue(creationTransaction.getContractAddress(), DataWord.ZERO);

        Assertions.assertNull(counterValue);

        TransactionReceipt transactionReceipt = world.getTransactionReceiptByName("tx02");

        Assertions.assertNotNull(transactionReceipt);

        byte[] status = transactionReceipt.getStatus();

        Assertions.assertNotNull(status);
        Assertions.assertEquals(0, status.length);
    }

    @Test
    void testPush0() throws FileNotFoundException, DslProcessorException {
        DslParser parser = DslParser.fromResource("dsl/push0test.txt");
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        processor.processCommands(parser);

        Block block2 = world.getBlockByName("b02");
        Assertions.assertNotNull(block2);
        Assertions.assertEquals(1, block2.getTransactionsList().size());

        Transaction creationTransactionNew = world.getTransactionByName("txCreateNew");
        Assertions.assertNotNull(creationTransactionNew);
        TransactionReceipt creationTransactionReceiptNew = world.getTransactionReceiptByName("txCallNew");
        Assertions.assertNotNull(creationTransactionReceiptNew);
        byte[] statusCreationNew = creationTransactionReceiptNew.getStatus();
        Assertions.assertNotNull(statusCreationNew);
        Assertions.assertEquals(1, statusCreationNew.length);
        Assertions.assertEquals(1, statusCreationNew[0]);

        Transaction callTransactionNew = world.getTransactionByName("txCallNew");
        Assertions.assertNotNull(callTransactionNew);
        TransactionReceipt callTransactionReceiptNew = world.getTransactionReceiptByName("txCallNew");
        Assertions.assertNotNull(callTransactionReceiptNew);
        byte[] statusCallNew = callTransactionReceiptNew.getStatus();
        Assertions.assertNotNull(statusCallNew);
        Assertions.assertEquals(1, statusCallNew.length);
        Assertions.assertEquals(1, statusCallNew[0]);

        short newGas = ByteBuffer.wrap(callTransactionReceiptNew.getGasUsed()).getShort();

        Block block4 = world.getBlockByName("b04");
        Assertions.assertNotNull(block4);
        Assertions.assertEquals(1, block4.getTransactionsList().size());


        Transaction creationTransactionOld = world.getTransactionByName("txCreateOld");
        Assertions.assertNotNull(creationTransactionOld);
        TransactionReceipt creationTransactionReceiptOld = world.getTransactionReceiptByName("txCreateOld");
        Assertions.assertNotNull(creationTransactionReceiptOld);
        byte[] statusCreationOld = creationTransactionReceiptNew.getStatus();
        Assertions.assertNotNull(statusCreationOld);
        Assertions.assertEquals(1, statusCreationOld.length);
        Assertions.assertEquals(1, statusCreationOld[0]);

        Transaction callTransactionOld = world.getTransactionByName("txCallOld");
        Assertions.assertNotNull(callTransactionOld);
        TransactionReceipt callTransactionReceiptOld = world.getTransactionReceiptByName("txCallOld");
        Assertions.assertNotNull(callTransactionReceiptOld);
        byte[] statusCallOld = callTransactionReceiptOld.getStatus();
        Assertions.assertNotNull(statusCallOld);
        Assertions.assertEquals(1, statusCallOld.length);
        Assertions.assertEquals(1, statusCallOld[0]);

        short oldGas = ByteBuffer.wrap(callTransactionReceiptOld.getGasUsed()).getShort();
        assertTrue(newGas < oldGas);
    }

    @Test
    void testInitCodeSizeValidationSuccessWithoutInitcodeCostViaCreateOpcodeCREATE() throws FileNotFoundException, DslProcessorException {
        TestSystemProperties rskip438Disabled = new TestSystemProperties(rawConfig ->
                rawConfig.withValue("blockchain.config.hardforkActivationHeights.lovell700", ConfigValueFactory.fromAnyRef(-1))
        );
        DslParser parser = DslParser.fromResource("dsl/initcode_rskip438/create_opcode_test_without_initcode_cost.txt");
        World world = new World(rskip438Disabled);
        WorldDslProcessor processor = new WorldDslProcessor(world);

        processor.processCommands(parser);

        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractFactory", "b01");
        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractViaOpCode", "b02");
    }

    @Test
    void testInitCodeSizeValidationSuccessWithInitCodeCostViaCreateOpcodeCREATE() throws FileNotFoundException, DslProcessorException {
        DslParser parser = DslParser.fromResource("dsl/initcode_rskip438/create_opcode_test_with_initcode_cost.txt");
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        processor.processCommands(parser);

        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractFactory", "b01");
        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractViaOpCode", "b02");
    }

    @Test
    void testInitCodeSizeValidationDoesntFailIfRSKIP438DeactivatedViaCREATE() throws FileNotFoundException, DslProcessorException {
        TestSystemProperties rskip438Disabled = new TestSystemProperties(rawConfig ->
                rawConfig.withValue("blockchain.config.hardforkActivationHeights.lovell700", ConfigValueFactory.fromAnyRef(-1))
        );

        DslParser parser = DslParser.fromResource("dsl/initcode_rskip438/create_opcode_test_with_higher_initcode_size_rskip_NOT_ACTIVE.txt");
        World world = new World(rskip438Disabled);

        WorldDslProcessor processor = new WorldDslProcessor(world);

        Exception ex = assertThrows(RuntimeException.class, () -> processor.processCommands(parser));
        assertTrue(ex.getMessage().contains("Maximum contract size allowed 24576 but actual 49174")); // It will fail due to max contract size, but not due initcode size
    }

    @Test
    void testInitCodeSizeValidationFailIfRSKIP438ActivatedViaCREATE() throws FileNotFoundException, DslProcessorException {
        DslParser parser = DslParser.fromResource("dsl/initcode_rskip438/create_opcode_test_with_higher_initcode_size_rskip_ACTIVE.txt");
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        processor.processCommands(parser);

        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractFactory", "b01");
        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractViaOpCode", "b02");
    }

    @Test
    void testInitCodeSizeValidationSuccessWithoutInitcodeCostViaCreateOpcodeCREATE2() throws FileNotFoundException, DslProcessorException {
        TestSystemProperties rskip438Disabled = new TestSystemProperties(rawConfig ->
                rawConfig.withValue("blockchain.config.hardforkActivationHeights.lovell700", ConfigValueFactory.fromAnyRef(-1))
        );
        DslParser parser = DslParser.fromResource("dsl/initcode_rskip438/create2_opcode_test_without_initcode_cost.txt");
        World world = new World(rskip438Disabled);
        WorldDslProcessor processor = new WorldDslProcessor(world);

        processor.processCommands(parser);

        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractFactory", "b01");
        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractViaOpCodeCreate2", "b02");
    }

    @Test
    void testInitCodeSizeValidationSuccessWithInitCodeCostViaCreateOpcodeCREATE2() throws FileNotFoundException, DslProcessorException {
        DslParser parser = DslParser.fromResource("dsl/initcode_rskip438/create2_opcode_test_with_initcode_cost.txt");
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        processor.processCommands(parser);

        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractFactory", "b01");
        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractViaOpCodeCreate2", "b02");
    }

    @Test
    void testInitCodeSizeValidationDoesntFailIfRSKIP438DeactivatedViaCREATE2() throws FileNotFoundException, DslProcessorException {
        TestSystemProperties rskip438Disabled = new TestSystemProperties(rawConfig ->
                rawConfig.withValue("blockchain.config.hardforkActivationHeights.lovell700", ConfigValueFactory.fromAnyRef(-1))
        );

        DslParser parser = DslParser.fromResource("dsl/initcode_rskip438/create2_opcode_test_with_higher_initcode_size_rskip_NOT_ACTIVE.txt");
        World world = new World(rskip438Disabled);

        WorldDslProcessor processor = new WorldDslProcessor(world);

        Exception ex = assertThrows(RuntimeException.class, () -> processor.processCommands(parser));
        assertTrue(ex.getMessage().contains("Maximum contract size allowed 24576 but actual 49174")); // It will fail due to max contract size, but not due initcode size
    }

    @Test
    void testInitCodeSizeValidationFailIfRSKIP438ActivatedViaCREATE2() throws FileNotFoundException, DslProcessorException {
        DslParser parser = DslParser.fromResource("dsl/initcode_rskip438/create2_opcode_test_with_higher_initcode_size_rskip_ACTIVE.txt");
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        processor.processCommands(parser);

        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractFactory", "b01");
        assertTransactionExecutedWithSuccessAndWasAddedToBlock(world, "txCreateContractViaOpCodeCreate2", "b02");
    }

    private void assertTransactionExecutedWithSuccessAndWasAddedToBlock(World world, String transactionName, String blockName) {
        Transaction contractTransaction = world.getTransactionByName(transactionName);
        Assertions.assertNotNull(contractTransaction);
        Block bestBlock = world.getBlockByName(blockName);
        Assertions.assertEquals(1, bestBlock.getTransactionsList().size());
    }
}
