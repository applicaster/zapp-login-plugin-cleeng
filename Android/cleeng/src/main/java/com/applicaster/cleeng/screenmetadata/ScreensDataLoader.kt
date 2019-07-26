package com.applicaster.cleeng.screenmetadata

import android.util.Log
import com.applicaster.loader.APLoaderRequestsHelper
import com.applicaster.loader.LoadersConstants
import com.applicaster.loader.json.APAccountLoader
import com.applicaster.util.AppData
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.coroutines.experimental.Deferred
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url


interface ScreenMetaDataService {
    @GET
    fun loadScreensJson(@Url url: String): Deferred<Response<List<ScreenData>>>
}

class ScreensDataLoader {

    private val TAG = ScreensDataLoader::class.java.canonicalName
    private val cleengCamScreenType = "cleeng_cam"

    private lateinit var retrofit: Retrofit
    private val httpClient: OkHttpClient.Builder = OkHttpClient.Builder()
    private var retrofitBuilder: Retrofit.Builder = Retrofit.Builder()

    private var retrofitService: ScreenMetaDataService? = null

    private fun createService(baseUrl: String): ScreenMetaDataService {
        retrofit = retrofitBuilder.apply {
            baseUrl("$baseUrl/")
            addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            addCallAdapterFactory(CoroutineCallAdapterFactory())
            client(getHttpClient())
        }.build()
        return retrofit.create(ScreenMetaDataService::class.java)
    }

    private fun getHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        httpClient.apply {
            addInterceptor(loggingInterceptor)
        }
        return httpClient.build()
    }

    private fun parseScreenConfig(config: Map<String, Any>?): Map<String, String>? {
        //transform MutableMap<Any?, Any?>? to Map<String, String>?
        return config?.entries?.associate { entry ->
            entry.key to entry.value.toString()
        }
    }

    suspend fun loadScreensData(): Map<String, String>? {
        val metaData = APAccountLoader.createMetaData()
        val urlScheme = APLoaderRequestsHelper.getUriScheme()
        val accountsPath = LoadersConstants.ACCOUNTS_PATH
        val accountId = AppData.getCrossDomainAccountId()
        val appsPath = LoadersConstants.APPS_PATH
        val zappMetaData = MetaData(
            urlScheme,
            accountsPath,
            accountId,
            appsPath,
            metaData
        )
        val builder = ScreenUrlBuilder(zappMetaData)

        retrofitService = createService(builder.baseUrl)

        try {
            val response = retrofitService?.loadScreensJson(builder.path)?.await()
            val screensDataList: List<ScreenData>? = response?.body()
            screensDataList?.forEach {
                if (it.type == cleengCamScreenType) {
                    return parseScreenConfig(it.general as? Map<String, Any>)
                }
            }

        } catch (t: Throwable) {
            t.stackTrace
            Log.e(TAG, t.message)
        }
        return mapOf()
    }
}
