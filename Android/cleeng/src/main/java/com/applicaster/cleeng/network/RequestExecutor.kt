package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.error.ErrorUtil
import com.applicaster.cleeng.network.error.WebServiceError
import kotlinx.coroutines.*
import retrofit2.Response

fun executeRequest(request: suspend () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
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
