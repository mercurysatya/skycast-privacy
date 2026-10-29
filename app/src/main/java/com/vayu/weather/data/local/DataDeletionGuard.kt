package com.vayu.weather.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serialises background-worker writes with a destructive local-data wipe.
 *
 * A worker captures [currentGenerationOrNull] before it starts fetching. If a
 * deletion begins afterwards, [tryWrite] refuses the write; if a write was
 * already in progress, [runDeletion] waits for it and then wipes the result.
 * This prevents an in-flight alert, widget or notification job from
 * repopulating stores immediately after the user asked for deletion.
 *
 * Scheduled work stays registered; this guard closes the window where a
 * worker is already running when the user starts the wipe. Workers that start
 * after a wipe are expected to check the reset onboarding/notification state
 * before writing.
 */
@Singleton
class DataDeletionGuard @Inject constructor() {
    private val lock = Mutex()
    private val stateLock = Any()

    @Volatile
    private var generation: Long = 0L

    @Volatile
    private var deleting: Boolean = false

    /**
     * Returns a token for a worker that is starting, or `null` when a wipe is
     * already running. Passing the token to [tryWrite] makes the write a no-op
     * if a wipe starts (or has started) in the meantime.
     */
    fun currentGenerationOrNull(): Long? = synchronized(stateLock) {
        if (deleting) null else generation
    }

    /**
     * Runs [block] while no worker write can overlap. Every token captured
     * before this call becomes invalid; tokens captured while it runs are
     * rejected by [currentGenerationOrNull].
     */
    suspend fun runDeletion(block: suspend () -> Unit) {
        lock.withLock {
            synchronized(stateLock) {
                deleting = true
                generation++
            }
            try {
                block()
            } finally {
                synchronized(stateLock) {
                    deleting = false
                }
            }
        }
    }

    /**
     * Runs [write] only when no deletion started since [capturedGeneration].
     * Returns `false` if the write was skipped.
     */
    suspend fun tryWrite(capturedGeneration: Long, write: suspend () -> Unit): Boolean =
        lock.withLock {
            if (deleting || generation != capturedGeneration) {
                false
            } else {
                write()
                true
            }
        }
}
