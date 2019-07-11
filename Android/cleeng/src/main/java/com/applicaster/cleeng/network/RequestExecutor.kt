package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.error.ErrorUtil
import com.applicaster.cleeng.network.error.WebServiceError
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.Response

private lateinit var exceptionHandler: CleengCoroutineExceptionHandler

fun executeRequest(request: suspend () -> Unit) {
    launch(UI) {
        request()
    }
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