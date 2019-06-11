package com.applicaster.cleeng.network

sealed class Result<out V, out E: Error> {

    data class Success<out V>(val value: V?) : Result<V, Nothing>()

    data class Failure<out E: Error>(val error: E?) : Result<Nothing, E>()
}