package co.rsk.jmh.sync;

import co.rsk.trie.TrieDTO;
import co.rsk.trie.TrieDTOInOrderIterator;
import co.rsk.trie.TrieStore;
import com.google.common.collect.Lists;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@Warmup(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 1)
@Fork(1)
public class SnapshotSyncBench {

    private TrieStore trieStore;
    private TrieDTOInOrderIterator iterator;
    private byte[] root;

    @Setup
    public void setup(RskContextState contextState) {
        System.out.println(" -------- Setup...");
        try {
            System.out.println(" -------- Blockchain...");
            this.trieStore = contextState.getContext().getTrieStore();
            System.out.println(" -------- TrieStore...");
            this.root = contextState.getBlockchain().getBestBlock().getStateRoot();
            System.out.println(" -------- StateRoot..." + contextState.getBlockchain().getBestBlock().getNumber());
            this.iterator = new TrieDTOInOrderIterator(this.trieStore, this.root);
            TrieDTO node = this.iterator.next();
            System.out.println(" -------- Iterator...");
            System.out.println(" -------- Bytes size children: " + node.getChildrenSize().value);
            // Reads the entire trie, no sense. Run once only, to know the size of the tree and save the value.
            //System.out.println(" -------- Trie size: " + node.trieSize());
       } catch (Throwable e) {
            System.out.println(" -------- Error:" + e.getMessage());
        }
        System.out.println(" -------- End Setup!");
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        //Every each iteration
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void read(OpCounters counters) {
        if (this.iterator.hasNext()) {
            readNode(this.iterator, counters);
        } else {
            this.iterator = new TrieDTOInOrderIterator(this.trieStore, this.root);
            readNode(this.iterator, counters);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    public void readAll(OpCounters counters) {
        this.iterator = new TrieDTOInOrderIterator(this.trieStore, this.root);
        List<byte[]> nodes = Lists.newArrayList();
        while (this.iterator.hasNext()) {
            nodes.add(readNode(this.iterator, counters).getEncoded());
        }
        System.out.println("----- Final bytesRead:" + counters.bytesRead);
        System.out.println("----- Final bytesSend:" + counters.bytesSend);
        System.out.println("----- Final nodes:" + counters.nodes);
        System.out.println("----- Final nodes bytes:" + nodes.size());
        System.out.println("----- Final nodes terminal:" + counters.terminal);
        System.out.println("----- Final nodes account:" + counters.account);
        System.out.println("----- Final nodes terminalAccount:" + counters.terminalAccount);
    }


    private TrieDTO readNode(TrieDTOInOrderIterator it, OpCounters counters) {
        final TrieDTO element = it.next();
        counters.nodes++;
        counters.bytesRead += element.getSource().length;
        counters.bytesRead += element.getValue() != null ? element.getValue().length : 0;
        counters.bytesSend += element.getEncoded().length;
        counters.terminal += element.isTerminal() ? 1 : 0;
        counters.account += element.isAccountLevel() ? 1 : 0;
        counters.terminalAccount += element.isTerminal() && element.isAccountLevel() ? 1 : 0;
        return element;
    }

}
