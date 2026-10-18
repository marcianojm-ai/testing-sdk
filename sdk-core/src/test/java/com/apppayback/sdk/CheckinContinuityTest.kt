package com.apppayback.sdk

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

class CheckinContinuityTest {
    @Test fun noActivityOrNetworkCannotCount() {
        val gate = CheckinContinuity()
        assertFalse(gate.accepts(gate.generation))
        gate.setForeground(true)
        assertFalse(gate.accepts(gate.generation))
        gate.setConnected(true)
        assertTrue(gate.accepts(gate.generation))
    }

    @Test fun shortBackgroundInvalidatesInflightResponse() {
        val gate = CheckinContinuity()
        gate.update(true, true)
        val pending = gate.generation
        gate.setForeground(false)
        gate.setForeground(true)
        assertFalse(gate.accepts(pending))
        assertTrue(gate.accepts(gate.generation))
    }

    @Test fun shortNetworkLossInvalidatesInflightResponse() {
        val gate = CheckinContinuity()
        gate.update(true, true)
        val pending = gate.generation
        gate.setConnected(false)
        gate.setConnected(true)
        assertFalse(gate.accepts(pending))
    }

    @Test fun repeatedOnlineCallbackDoesNotRestartAttempt() {
        val gate = CheckinContinuity()
        gate.update(true, true)
        val pending = gate.generation
        repeat(10) { gate.setConnected(true); gate.setForeground(true) }
        assertTrue(gate.accepts(pending))
    }

    @Test fun concurrentCallbacksDoNotOverwriteOtherSignal() {
        val gate = CheckinContinuity()
        gate.update(true, true)
        val background = Thread { repeat(1000) { gate.setForeground(false) } }
        val network = Thread { repeat(1000) { gate.setConnected(false) } }
        background.start(); network.start(); background.join(); network.join()
        assertFalse(gate.foreground)
        assertFalse(gate.connected)
        assertFalse(gate.accepts(gate.generation))
    }

    @Test fun snapshotNeverObservesHalfAppliedTransition() {
        val gate = CheckinContinuity()
        val failures = ConcurrentLinkedQueue<CheckinContinuitySnapshot>()
        val writer = Thread {
            repeat(20_000) {
                gate.update(true, true)
                gate.update(false, false)
            }
        }
        val reader = Thread {
            repeat(20_000) {
                val snapshot = gate.snapshot()
                if (snapshot.foreground != snapshot.connected) {
                    failures.add(snapshot)
                }
            }
        }

        writer.start()
        reader.start()
        writer.join()
        reader.join()

        assertTrue(failures.isEmpty())
    }
}
