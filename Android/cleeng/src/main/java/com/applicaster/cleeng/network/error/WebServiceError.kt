package com.applicaster.cleeng.network.error

enum class WebServiceError : Error {
    DEFAULT,
    NO_USER_EXISTS,
    USER_ALREADY_EXISTS,
    INVALID_CREDENTIALS
}