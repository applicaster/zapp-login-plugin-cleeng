package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.error.Error
import com.applicaster.cleeng.network.error.ErrorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Response
import kotlin.coroutines.CoroutineContext


private lateinit var job: Job
private val scope by lazy {
    job = Job()
    val coroutineContext: CoroutineContext = Dispatchers.IO + job
    CoroutineScope(coroutineContext)
}

fun launchRequest(request: suspend () -> Unit) {
    scope.launch {
        request()
    }
}

fun cancelCurrentJob() {
    if (job.isActive)
        job.cancel()
}

fun<R> handleResponse(response: Response<R>): Result<R, Error> {
    return when {
        response.isSuccessful -> {
            Result.Success(response.body())
        }
        response.code() == 400 -> {
            Result.Failure(ErrorUtil.parseError(response))
        }
        response.code() == 401 -> {
            Result.Failure(ErrorUtil.parseError(response))
        }
        response.code() == 422 -> {
            Result.Failure(ErrorUtil.parseError(response))
        }
        response.code() == 500 -> {
            Result.Failure(ErrorUtil.parseError(response))
        }
        else -> {
            Result.Failure(ErrorUtil.parseError(response))
        }
    }
}
