package com.applicaster.cleeng.network.error

interface Error {
    fun code(): String
    fun message(): String
}