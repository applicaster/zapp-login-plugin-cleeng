package com.applicaster.cleeng.network

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

class CleengCoroutineExceptionHandler(private val handler: (Throwable) -> Unit) :
    CoroutineExceptionHandler {

    private val TAG = CleengCoroutineExceptionHandler::class.java.simpleName

    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        Log.e(TAG, exception.message)
        handler.invoke(exception)
    }
}