package com.applicaster.cleeng.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class ServiceGenerator {
    companion object {

        val TAG = this::class.java.canonicalName
        private val baseUrl = "https://applicaster-cleeng-sso.herokuapp.com/"

        lateinit var retrofit: Retrofit
        private val httpClient: OkHttpClient.Builder = OkHttpClient.Builder()
        private val gson = GsonBuilder().create()
        private var retrofitBuilder: Retrofit.Builder = Retrofit.Builder()

        fun <S> createRetrofitService(serviceClass: Class<S>): S {
            retrofit = retrofitBuilder.apply {
                baseUrl(baseUrl)
                addConverterFactory(GsonConverterFactory.create(gson))
                client(getHttpClient())
                setPublisherId()
            }.build()
            return retrofit.create(serviceClass)
        }

        private fun getHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            httpClient.addInterceptor(loggingInterceptor)
            return httpClient.build()
        }

        private fun setPublisherId() {
            httpClient.addInterceptor { chain ->
                val request: Request = chain.request()
                val requestBody: RequestBody? = request.body()
                val publisherId = ""
                val requestBuilder: Request.Builder = request.newBuilder()
                requestBuilder.post(
                    RequestBody.create(
                        requestBody?.contentType(),
                        "${requestBody?.bodyToString()}&publisherId=$publisherId"
                    )
                ).build()
                chain.proceed(request)
            }
        }

        private fun RequestBody?.bodyToString(): String {
            return this?.run {
                val buffer = okio.Buffer()
                writeTo(buffer)
                buffer.readUtf8()
            } ?: ""
        }
    }
}