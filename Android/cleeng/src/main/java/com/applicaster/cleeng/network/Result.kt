package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.error.Error

sealed class Result<out R, out E: Error> {

    data class Success<out R>(val value: R?) : Result<R, Nothing>()

    data class Failure<out E: Error>(val value: E?) : Result<Nothing, E>()
}