package com.applicaster.cleeng.network.interceptor

import com.google.gson.Gson
import okhttp3.*
import okio.ByteString

class PublisherInfoInterceptor(private val publisherId: String) : Interceptor {

    private val TAG = PublisherInfoInterceptor::class.java.canonicalName

    // supported media types
    private val MEDIA_TYPE_FORM_URL_ENCODED = "application/x-www-form-urlencoded"
    private val MEDIA_TYPE_APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8"

    private val KEY_PUBLISHER_ID = "publisherId"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        val newRequest = when (val contentType = originalRequest.body()?.contentType()) {
            MediaType.get(MEDIA_TYPE_FORM_URL_ENCODED) -> {
                val newRequestBody: String = StringBuffer(
                    originalRequest.body().bodyToString()
                ).apply {
                    append("&")
                    append(KEY_PUBLISHER_ID)
                    append("=")
                    append(publisherId)
                }.toString()
                originalRequest.newBuilder().post(RequestBody.create(contentType, newRequestBody)).build()
            }

            MediaType.get(MEDIA_TYPE_APPLICATION_JSON_UTF8) -> {
                val originalRequestBody = originalRequest.body().bodyToString()
                val json = Gson().fromJson(originalRequestBody, Map::class.java)
                val jsonMap: MutableMap<Any?, Any?> = json.toMutableMap()
                jsonMap[KEY_PUBLISHER_ID] = publisherId
                val newRequestBody: ByteString = Gson().toJson(jsonMap).stringToByteString()
                originalRequest.newBuilder().post(RequestBody.create(contentType, newRequestBody)).build()
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