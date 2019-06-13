package com.applicaster.cleeng.network.interceptor

import com.google.gson.Gson
import okhttp3.*
import okio.ByteString

class PublisherInfoInterceptor(private val publisherId: String) : Interceptor {

    private val TAG = PublisherInfoInterceptor::class.java.canonicalName

    // supported media types
    private val FORM_URL_ENCODED = "application/x-www-form-urlencoded"
    private val APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val contentType = originalRequest.body()?.contentType()

        val newRequest = when (contentType) {
            MediaType.get(FORM_URL_ENCODED) -> {
                val newRequestBody = "${originalRequest.body().bodyToString()}&publisherId=$publisherId"
                originalRequest.newBuilder().post(RequestBody.create(contentType, newRequestBody)).build()
            }

            MediaType.get(APPLICATION_JSON_UTF8) -> {
                val newRequestBody = originalRequest.body().bodyToString()
                val gson = Gson().fromJson(newRequestBody, Map::class.java)
                val gsonMap: MutableMap<Any?, Any?> = gson.toMutableMap()
                gsonMap["publisherId"] = publisherId
                val body: ByteString = Gson().toJson(gsonMap).stringToByteString()
                originalRequest.newBuilder().post(RequestBody.create(contentType, body)).build()
            }

            else -> {
                originalRequest
            }
        }
        return chain.proceed(newRequest)
    }

    private fun RequestBody?.bodyToString(): String {
        return this?.run {
            val buffer = okio.Buffer()
            writeTo(buffer)
            buffer.readUtf8()
        } ?: ""
    }

    private fun String.stringToByteString(): ByteString = ByteString.encodeUtf8(this)
}