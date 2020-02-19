package com.applicaster.cleeng.network

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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

        private val customInterceptorList: ArrayList<Interceptor> = arrayListOf()

        fun <S> createRetrofitService(serviceClass: Class<S>): S {
            retrofit = retrofitBuilder.apply {
                baseUrl(baseUrl)
                addConverterFactory(GsonConverterFactory.create(gson))
                client(getHttpClient())
            }.build()
            return retrofit.create(serviceClass)
        }

        private fun getHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            httpClient.apply {
                customInterceptorList.forEach { addInterceptor(it) }
                addInterceptor(loggingInterceptor)
            }
            return httpClient.build()
        }

        fun setCustomInterceptor(interceptor: Interceptor) {
            customInterceptorList.add(interceptor)
        }

        fun clearCustomInterseptors() {
            customInterceptorList.clear()
        }
    }
}