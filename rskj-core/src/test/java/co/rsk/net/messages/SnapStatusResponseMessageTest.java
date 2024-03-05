package co.rsk.net.messages;

import co.rsk.blockchain.utils.BlockGenerator;
import co.rsk.config.TestSystemProperties;
import co.rsk.core.BlockDifficulty;
import co.rsk.db.HashMapBlocksIndex;
import org.ethereum.core.Block;
import org.ethereum.core.BlockFactory;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.db.BlockStore;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.util.RLP;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SnapStatusResponseMessageTest {

    private final TestSystemProperties config = new TestSystemProperties();
    private final BlockFactory blockFactory = new BlockFactory(config.getActivationConfig());
    private final BlockStore indexedBlockStore = new IndexedBlockStore(blockFactory, new HashMapDB(), new HashMapBlocksIndex());
    private final Block block4Test = new BlockGenerator().getBlock(1);
    private final List<Block> blockList = Collections.singletonList(new BlockGenerator().getBlock(1));
    private final List<BlockDifficulty> blockDifficulties = Collections.singletonList(indexedBlockStore.getTotalDifficultyForHash(block4Test.getHash().getBytes()));
    private final long trieSize = 1L;
    private final SnapStatusResponseMessage underTest = new SnapStatusResponseMessage(blockList, blockDifficulties, trieSize);


    @Test
    void getMessageType_returnCorrectMessageType() {
        //given-when
        MessageType messageType = underTest.getMessageType();

        //then
        assertEquals(MessageType.SNAP_STATUS_RESPONSE_MESSAGE, messageType);
    }

    @Test
    void getEncodedMessage_returnExpectedByteArray() {
        //given default block 4 test
        byte[] expectedEncodedMessage = RLP.encodeList(
                RLP.encodeList(RLP.encode(block4Test.getEncoded())),
                RLP.encodeList(RLP.encode(blockDifficulties.get(0).getBytes())),
                RLP.encodeBigInteger(BigInteger.valueOf(this.trieSize)));
        //when
        byte[] encodedMessage = underTest.getEncodedMessage();

        //then
        assertThat(encodedMessage, equalTo(expectedEncodedMessage));
    }

    @Test
    void getDifficulties_returnTheExpectedValue() {
        //given default block 4 test

        //when
        List<BlockDifficulty> difficultiesReturned = underTest.getDifficulties();
        //then
        assertThat(difficultiesReturned, equalTo(blockDifficulties));
    }

    @Test
    void getBlocks_returnTheExpectedValue() {
        //given default block 4 test

        //when
        List<Block> blocksReturned = underTest.getBlocks();
        //then
        assertThat(blocksReturned, equalTo(blockList));
    }

    @Test
    void getTrieSize_returnTheExpectedValue() {
        //given default block 4 test

        //when
        long trieSizeReturned = underTest.getTrieSize();
        //then
        assertThat(trieSizeReturned, equalTo(trieSize));
    }

    @Test
    void givenAcceptIsCalled_messageVisitorIsAppliedForMessage() {
        //given
        SnapStatusResponseMessage message = new SnapStatusResponseMessage(blockList, blockDifficulties, trieSize);
        MessageVisitor visitor = mock(MessageVisitor.class);

        //when
        message.accept(visitor);

        //then
        verify(visitor, times(1)).apply(message);
    }
}