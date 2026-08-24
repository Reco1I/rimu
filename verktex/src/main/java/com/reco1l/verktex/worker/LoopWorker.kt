package com.reco1l.verktex.worker

interface LoopWorker {

    /**
     * The name of the worker thread, which can be used for identification and debugging purposes.
     */
    val name: String

    /**
     * Starts the worker thread, initiating its execution.
     */
    fun start()

    /**
     * Pauses the worker thread, temporarily halting its execution.
     */
    fun pause()

    /**
     * Resumes the worker thread, allowing it to continue executing tasks after being paused.
     * This method should be called when the worker is ready to resume its operations.
     */
    fun resume()

    /**
     * Stops the worker thread, terminating its execution. This method should be called when the worker is no longer needed or when the application is shutting down.
     * It ensures that the worker thread is properly terminated and any resources it was using are released.
     */
    fun stop()

    /**
     * Checks if the current worker thread is the same as the executing thread. If not, it throws an
     * exception to indicate that the method is being called from the wrong thread.
     *
     * @throws IllegalStateException
     */
    fun check()

    /**
     * Whether the worker thread is currently running.
     */
    fun isRunning(): Boolean

    /**
     * Whether the worker thread is currently looping, this can be used to prevent circular calls to
     * the loop method.
     */
    fun isLooping(): Boolean

    /**
     * Schedules a task to be executed on the worker thread.
     *
     * @return A [TaskState] object representing the scheduled task, which can be used to check if
     * the task has completed.
     */
    fun schedule(task: () -> Unit): TaskState

    /**
     * Called on each iteration of the worker thread's loop when the worker is running.
     */
    fun onLoop()


    /**
     * A data class representing a task to be executed on the loop thread. It contains the task itself
     * as a lambda function and a flag indicating whether the task has completed.
     */
    data class TaskState(

        /**
         * The task to be executed on the loop thread, represented as a lambda function.
         */
        val task: () -> Unit,

        /**
         * A flag indicating whether the task has completed. This is marked as volatile to ensure
         * visibility across threads.
         */
        @Volatile
        var isCompleted: Boolean = false
    )
}

