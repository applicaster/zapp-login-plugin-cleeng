package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.interceptor.PublisherInfoInterceptor
import com.applicaster.cleeng.network.request.RegisterRequestData
import com.applicaster.cleeng.network.request.SubscribeRequestData
import com.applicaster.cleeng.network.request.SubscriptionsRequestData
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.ResetPasswordResponseData
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import retrofit2.Response

class NetworkHelper(private val publisherId: String) {

    private val retrofitService by lazy {
        ServiceGenerator.setCustomInterceptor(PublisherInfoInterceptor(publisherId))
        ServiceGenerator.createRetrofitService(RestService::class.java)
    }

    suspend fun register(requestData: RegisterRequestData): Response<List<AuthResponseData>> =
        with(requestData) {
            retrofitService.registerEmail(
                email,
                password ?: "",
                country,
                locale,
                currency
            )
        }

    suspend fun registerFacebook(requestData: RegisterRequestData): Response<List<AuthResponseData>> =
        with(requestData) {
            retrofitService.registerFacebook(
                email,
                facebookId ?: "",
                country,
                locale,
                currency
            )
        }

    suspend fun login(email: String, password: String): Response<List<AuthResponseData>> =
        retrofitService.loginEmail(email, password)

    suspend fun loginFacebook(email: String, facebookId: String): Response<List<AuthResponseData>> =
        retrofitService.loginFacebook(email, facebookId)

    suspend fun resetPassword(email: String): Response<ResetPasswordResponseData> =
        retrofitService.resetPassword(email)

    suspend fun extendToken(token: String): Response<List<AuthResponseData>> =
        retrofitService.extendToken(token)

    suspend fun requestSubscriptions(requestData: SubscriptionsRequestData): Response<List<SubscriptionsResponseData>> =
        retrofitService.requestSubscriptions(requestData)

    suspend fun subscribe(requestData: SubscribeRequestData): Response<Unit> =
        retrofitService.subscribe(requestData)
}