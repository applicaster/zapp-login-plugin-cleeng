package com.applicaster.cleeng.utils

import com.applicaster.util.asynctask.AsyncTaskListener
import java.lang.Exception

abstract class CleengAsyncTaskListener<T> : AsyncTaskListener<T> {
    override fun handleException(e: Exception?) {
        onError()
    }

    override fun onTaskStart() {

    }

    override fun onTaskComplete(result: T) {
        onComplete(result)
    }

    abstract fun onComplete(result: T)
    abstract fun onError()

}