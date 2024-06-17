/*
 * This file is part of RskJ
 * Copyright (C) 2024 RSK Labs Ltd.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
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
package co.rsk.net.sync;

import co.rsk.net.NodeID;
import co.rsk.net.Peer;
import co.rsk.net.SnapshotProcessor;
import co.rsk.net.messages.SnapBlocksResponseMessage;
import co.rsk.net.messages.SnapStateChunkResponseMessage;
import co.rsk.net.messages.SnapStatusResponseMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SnapSyncStateTest {

    private static final long THREAD_JOIN_TIMEOUT = 10_000; // 10 secs

    private final SyncConfiguration syncConfiguration = SyncConfiguration.IMMEDIATE_FOR_TESTING;
    private final SyncEventsHandler syncEventsHandler = mock(SyncEventsHandler.class);
    private final SnapshotPeersInformation peersInformation = mock(SnapshotPeersInformation.class);
    private final SnapshotProcessor snapshotProcessor = mock(SnapshotProcessor.class);
    private final SyncMessageHandler.Listener listener = mock(SyncMessageHandler.Listener.class);

    private final SnapSyncState underTest = new SnapSyncState(syncEventsHandler, snapshotProcessor, syncConfiguration, listener);

    @BeforeEach
    void setUp(){
        reset(syncEventsHandler,peersInformation, snapshotProcessor);
    }

    @AfterEach
    void tearDown() {
        underTest.finish();
    }

    @Test
    void givenOnEnterWasCalledAndNotRunningYet_thenSyncingStartsWithTestObjectAsParameter(){
        //given-when
        underTest.onEnter();
        //then
        verify(snapshotProcessor, times(1)).startSyncing();
    }

    @Test
    void givenFinishWasCalledTwice_thenStopSyncingOnlyOnce(){
        //given-when
        underTest.setRunning();
        underTest.finish();
        underTest.finish();
        //then
        verify(syncEventsHandler, times(1)).stopSyncing();
    }

    @Test
    void givenOnEnterWasCalledTwice_thenSyncingStartsOnlyOnce(){
        //given-when
        underTest.onEnter();
        underTest.onEnter();
        //then
        verify(snapshotProcessor, times(1)).startSyncing();
    }

    @Test
    void givenOnMessageTimeOutCalled_thenSyncingStops(){
        //given-when
        underTest.setRunning();
        underTest.onMessageTimeOut();
        //then
        verify(syncEventsHandler, times(1)).stopSyncing();
    }

    @Test
    void givenNewChunk_thenTimerIsReset(){
        //given
        underTest.timeElapsed = Duration.ofMinutes(1);
        assertThat(underTest.timeElapsed, greaterThan(Duration.ZERO));

        // when
        underTest.onNewChunk();
        //then
        assertThat(underTest.timeElapsed, equalTo(Duration.ZERO));
    }

    @Test
    void givenTickIsCalledBeforeTimeout_thenTimerIsUpdated_andNoTimeoutHappens(){
        //given
        Duration elapsedTime = Duration.ofMillis(10);
        underTest.timeElapsed = Duration.ZERO;
        // when
        underTest.tick(elapsedTime);
        //then
        assertThat(underTest.timeElapsed, equalTo(elapsedTime));
        verify(syncEventsHandler, never()).stopSyncing();
        verify(syncEventsHandler, never()).onErrorSyncing(any(),any(),any(),any());
    }

    @Test
    void givenTickIsCalledAfterTimeout_thenTimerIsUpdated_andTimeoutHappens() throws UnknownHostException {
        //given
        Duration elapsedTime = Duration.ofMinutes(1);
        underTest.timeElapsed = Duration.ZERO;
        Peer mockedPeer = mock(Peer.class);
        NodeID nodeID = mock(NodeID.class);
        when(mockedPeer.getPeerNodeID()).thenReturn(nodeID);
        when(mockedPeer.getAddress()).thenReturn(InetAddress.getByName("127.0.0.1"));
        when(peersInformation.getBestSnapPeer()).thenReturn(Optional.of(mockedPeer));
        underTest.setRunning();
        // when
        underTest.tick(elapsedTime);
        //then
        assertThat(underTest.timeElapsed, equalTo(elapsedTime));
        verify(syncEventsHandler, times(1)).stopSyncing();
    }

    @Test
    void givenFinishIsCalled_thenSyncEventHandlerStopsSync(){
        //given-when
        underTest.setRunning();
        underTest.finish();
        //then
        verify(syncEventsHandler, times(1)).stopSyncing();
    }

    @Test
    void givenOnSnapStatusIsCalled_thenJobIsAddedAndRun() throws InterruptedException {
        //given
        Peer peer = mock(Peer.class);
        SnapStatusResponseMessage msg = mock(SnapStatusResponseMessage.class);
        CountDownLatch latch = new CountDownLatch(1);
        doCountDownOnQueueEmpty(listener, latch);
        underTest.onEnter();

        //when
        underTest.onSnapStatus(peer, msg);

        //then
        assertTrue(latch.await(THREAD_JOIN_TIMEOUT, TimeUnit.MILLISECONDS));

        ArgumentCaptor<SyncMessageHandler.Job> jobArg = ArgumentCaptor.forClass(SyncMessageHandler.Job.class);
        verify(listener, times(1)).onJobRun(jobArg.capture());

        assertEquals(peer, jobArg.getValue().getSender());
        assertEquals(msg, jobArg.getValue().getMsg());
    }

    @Test
    void givenOnSnapBlocksIsCalled_thenJobIsAddedAndRun() throws InterruptedException {
        //given
        Peer peer = mock(Peer.class);
        SnapBlocksResponseMessage msg = mock(SnapBlocksResponseMessage.class);
        CountDownLatch latch = new CountDownLatch(1);
        doCountDownOnQueueEmpty(listener, latch);
        underTest.onEnter();

        //when
        underTest.onSnapBlocks(peer, msg);

        //then
        assertTrue(latch.await(THREAD_JOIN_TIMEOUT, TimeUnit.MILLISECONDS));

        ArgumentCaptor<SyncMessageHandler.Job> jobArg = ArgumentCaptor.forClass(SyncMessageHandler.Job.class);
        verify(listener, times(1)).onJobRun(jobArg.capture());

        assertEquals(peer, jobArg.getValue().getSender());
        assertEquals(msg, jobArg.getValue().getMsg());
    }

    @Test
    void givenOnSnapStateChunkIsCalled_thenJobIsAddedAndRun() throws InterruptedException {
        //given
        Peer peer = mock(Peer.class);
        SnapStateChunkResponseMessage msg = mock(SnapStateChunkResponseMessage.class);
        CountDownLatch latch = new CountDownLatch(1);
        doCountDownOnQueueEmpty(listener, latch);
        underTest.onEnter();

        //when
        underTest.onSnapStateChunk(peer, msg);

        //then
        assertTrue(latch.await(THREAD_JOIN_TIMEOUT, TimeUnit.MILLISECONDS));

        ArgumentCaptor<SyncMessageHandler.Job> jobArg = ArgumentCaptor.forClass(SyncMessageHandler.Job.class);
        verify(listener, times(1)).onJobRun(jobArg.capture());

        assertEquals(peer, jobArg.getValue().getSender());
        assertEquals(msg, jobArg.getValue().getMsg());
    }

    private static void doCountDownOnQueueEmpty(SyncMessageHandler.Listener listener, CountDownLatch latch) {
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(listener).onQueueEmpty();
    }
}
