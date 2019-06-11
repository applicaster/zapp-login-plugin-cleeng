package com.applicaster.cleeng.network

interface Error {
    fun code(): String
    fun message(): String
}