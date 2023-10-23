package co.rsk.net.messages;


import com.google.common.collect.Lists;
import org.bouncycastle.util.BigIntegers;
import org.ethereum.core.Block;
import org.ethereum.core.BlockFactory;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SnapStatusResponseMessage extends Message {
    private final List<Block> blocks;
    private final long trieSize;

    public List<Block> getBlocks() {
        return this.blocks;
    }

    public long getTrieSize() {
        return this.trieSize;
    }

    public SnapStatusResponseMessage(List<Block> blocks, long trieSize) {
        this.blocks = blocks;
        this.trieSize = trieSize;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SNAP_STATUS_RESPONSE_MESSAGE;
    }

    @Override
    public byte[] getEncodedMessage() {
        List<byte[]> rlpBlocks = this.blocks.stream().map(Block::getEncoded).map(RLP::encode).collect(Collectors.toList());
        byte[] rlpTrieSize = RLP.encodeBigInteger(BigInteger.valueOf(this.trieSize));

        return RLP.encodeList(RLP.encodeList(rlpBlocks.toArray(new byte[][]{})), rlpTrieSize);
    }

    public static Message decodeMessage(BlockFactory blockFactory, RLPList list) {
        RLPList rlpBlocks = RLP.decodeList(list.get(0).getRLPData());
        List<Block> blocks = Lists.newArrayList();
        for (int i = 0; i < rlpBlocks.size(); i++) {
            blocks.add(blockFactory.decodeBlock(rlpBlocks.get(i).getRLPData()));
        }

        byte[] rlpTrieSize = list.get(1).getRLPData();
        long trieSize = rlpTrieSize == null ? 0 : BigIntegers.fromUnsignedByteArray(rlpTrieSize).longValue();

        return new SnapStatusResponseMessage(blocks, trieSize);
    }

    @Override
    public void accept(MessageVisitor v) {
        v.apply(this);
    }
}
