package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.interceptor.PublisherInfoInterceptor
import com.applicaster.cleeng.network.request.RegisterRequest
import com.applicaster.cleeng.network.request.SubscribeRequest
import com.applicaster.cleeng.network.request.SubscriptionsRequest
import com.applicaster.cleeng.network.response.OfferResponse
import com.applicaster.cleeng.network.response.AuthResponse
import com.applicaster.cleeng.network.response.ResetPasswordResponse
import retrofit2.Response

class NetworkHelper(private val publisherId: String) {

    private val retrofitService by lazy {
        ServiceGenerator.setCustomInterceptor(PublisherInfoInterceptor(publisherId))
        ServiceGenerator.createRetrofitService(RestService::class.java)
    }

    suspend fun register(request: RegisterRequest): Response<List<AuthResponse>> =
        with(request) {
            retrofitService.registerEmail(
                email,
                password ?: "",
                country,
                locale,
                currency
            )
        }

    suspend fun registerFacebook(request: RegisterRequest): Response<List<AuthResponse>> =
        with(request) {
            retrofitService.registerFacebook(
                email,
                facebookId ?: "",
                country,
                locale,
                currency
            )
        }

    suspend fun login(email: String, password: String): Response<List<AuthResponse>> =
        retrofitService.loginEmail(email, password)

    suspend fun loginFacebook(email: String, facebookId: String): Response<List<AuthResponse>> =
        retrofitService.loginFacebook(email, facebookId)

    suspend fun resetPassword(email: String): Response<ResetPasswordResponse> =
        retrofitService.resetPassword(email)

    suspend fun extendToken(token: String): Response<AuthResponse> =
        retrofitService.extendToken(token)

    suspend fun requestSubscriptions(request: SubscriptionsRequest): Response<List<OfferResponse>> =
        retrofitService.requestSubscriptions(request)

    suspend fun subscribe(request: SubscribeRequest): Response<Unit> =
        retrofitService.subscribe(request)
}