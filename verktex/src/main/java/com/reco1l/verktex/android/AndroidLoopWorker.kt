package com.reco1l.verktex.android

import com.reco1l.verktex.worker.LoopWorker
import java.util.concurrent.ConcurrentLinkedQueue

abstract class AndroidLoopWorker(override val name: String) : LoopWorker {

    @Volatile private var running = true
    @Volatile private var paused = false

    private var isLooping = false

    // Tasks are scheduled from other threads, so the queue must be thread safe.
    private val taskQueue = ConcurrentLinkedQueue<LoopWorker.TaskState>()
    // java.lang.Object on purpose, the thread waits and is notified on it.
    private val lock = Object()

    private val thread = object : Thread(name) {
        override fun run() {
            while (running) {
                isLooping = true

                synchronized(lock) {
                    while (paused) {
                        lock.wait()
                    }
                }

                while (running) {
                    val task = taskQueue.poll() ?: break
                    task.task()
                    task.isCompleted = true
                }

                onLoop()
                isLooping = false
            }
        }
    }

    override fun start() {
        thread.start()
    }

    override fun pause() {
        synchronized(lock) {
            paused = true
        }
    }

    override fun resume() {
        synchronized(lock) {
            paused = false
            lock.notifyAll()
        }
    }

    override fun stop() {
        synchronized(lock) {
            running = false
            // A paused worker would otherwise keep waiting forever.
            paused = false
            lock.notifyAll()
        }

        if (Thread.currentThread() != thread) {
            thread.join()
        }
    }

    override fun check() {
        if (Thread.currentThread() != thread) {
            throw IllegalStateException("This method must be called from the $name thread.")
        }
    }

    override fun isRunning(): Boolean {
        return running
    }

    override fun isLooping(): Boolean {
        return isLooping
    }

    override fun schedule(task: () -> Unit): LoopWorker.TaskState {
        val taskState = LoopWorker.TaskState(task)
        taskQueue.add(taskState)
        return taskState
    }

}