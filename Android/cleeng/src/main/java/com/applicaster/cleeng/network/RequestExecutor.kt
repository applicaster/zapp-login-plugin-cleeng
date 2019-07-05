package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.error.ErrorUtil
import com.applicaster.cleeng.network.error.WebServiceError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Response
import kotlin.coroutines.CoroutineContext


private lateinit var job: Job
private lateinit var exceptionHandler: CleengCoroutineExceptionHandler
private val scope by lazy {
    job = Job()
    exceptionHandler = CleengCoroutineExceptionHandler {  }
    val coroutineContext: CoroutineContext = Dispatchers.Main + job + exceptionHandler
    CoroutineScope(coroutineContext)
}

fun executeRequest(request: suspend () -> Unit) {
    scope.launch {
        request()
    }
}

fun cancelCurrentJob() {
    if (::job.isInitialized && job.isActive)
        job.cancel()
}

fun <R> handleResponse(response: Response<R>): Result<R, WebServiceError> {
    return when {
        response.isSuccessful -> {
            Result.Success(response.body())
        }
        else -> {
            Result.Failure(ErrorUtil.handleError(response))
        }
    }
}
