package com.vayu.weather.data.local

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataDeletionGuardTest {

    @Test
    fun `write with the current token succeeds`() = runTest {
        val guard = DataDeletionGuard()
        var writes = 0

        val allowed = guard.tryWrite(guard.currentGenerationOrNull()!!) { writes++ }

        assertTrue(allowed)
        assertEquals(1, writes)
    }

    @Test
    fun `write captured before deletion is rejected afterwards`() = runTest {
        val guard = DataDeletionGuard()
        val token = guard.currentGenerationOrNull()!!
        guard.runDeletion { }
        var writes = 0

        val allowed = guard.tryWrite(token) { writes++ }

        assertFalse(allowed)
        assertEquals(0, writes)
    }

    @Test
    fun `worker starting during deletion gets no token`() = runTest {
        val guard = DataDeletionGuard()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val deletion = launch {
            guard.runDeletion {
                entered.complete(Unit)
                release.await()
            }
        }
        entered.await()

        assertNull(guard.currentGenerationOrNull())

        release.complete(Unit)
        deletion.join()
    }

    @Test
    fun `deletion waits for an in-flight write before wiping`() = runTest {
        val guard = DataDeletionGuard()
        val order = mutableListOf<String>()
        val writeStarted = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()

        val writer = launch {
            guard.tryWrite(guard.currentGenerationOrNull()!!) {
                order += "write-start"
                writeStarted.complete(Unit)
                finishWrite.await()
                order += "write-end"
            }
        }
        writeStarted.await()

        val deletion = launch {
            guard.runDeletion { order += "wipe" }
        }
        runCurrent()
        assertEquals(listOf("write-start"), order)

        finishWrite.complete(Unit)
        writer.join()
        deletion.join()

        assertEquals(listOf("write-start", "write-end", "wipe"), order)
    }
}
