package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.interceptor.PublisherInfoInterceptor
import com.applicaster.cleeng.network.request.RegisterRequest
import com.applicaster.cleeng.network.request.SubscribeRequest
import com.applicaster.cleeng.network.request.SubscriptionsRequest
import com.applicaster.cleeng.network.response.OfferResponse
import com.applicaster.cleeng.network.response.RegisterResponce
import com.applicaster.cleeng.network.response.ResetPasswordResponse
import retrofit2.Response

object NetworkService {

    private val retrofitService by lazy {
        ServiceGenerator.setCustomInterceptor(PublisherInfoInterceptor("obtained publisher id"))
        ServiceGenerator.createRetrofitService(RestService::class.java)
    }

    suspend fun register(request: RegisterRequest): Response<List<RegisterResponce>> =
        with(request) {
            retrofitService.register(
                email,
                password ?: "",
                country,
                locale,
                currency
            )
        }

    suspend fun registerFacebook(request: RegisterRequest): Response<List<RegisterResponce>> =
        with(request) {
            retrofitService.registerFacebook(
                email,
                facebookId ?: "",
                country,
                locale,
                currency
            )
        }

    suspend fun login(email: String, password: String): Response<List<RegisterResponce>> =
        retrofitService.login(email, password)


    suspend fun loginFacebook(email: String, facebookId: String): Response<List<RegisterResponce>> =
        retrofitService.loginFacebook(email, facebookId)


    suspend fun resetPassword(email: String): Response<ResetPasswordResponse> =
        retrofitService.resetPassword(email)


    suspend fun extendToken(token: String): Response<RegisterResponce> =
        retrofitService.extendToken(token)


    suspend fun requestSubscriptions(request: SubscriptionsRequest): Response<List<OfferResponse>> =
        retrofitService.requestSubscriptions(request)


    suspend fun subscribe(request: SubscribeRequest): Response<Unit> =
        retrofitService.subscribe(request)
}