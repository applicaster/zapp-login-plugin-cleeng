package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.error.WebServiceError

sealed class Result<out R, out E: WebServiceError> {

    data class Success<out R>(val value: R?) : Result<R, Nothing>()

    data class Failure<out E: WebServiceError>(val value: E?) : Result<Nothing, E>()
}