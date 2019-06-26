package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.request.SubscribeRequestData
import com.applicaster.cleeng.network.request.SubscriptionsRequestData
import com.applicaster.cleeng.network.response.OfferResponseData
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.ResetPasswordResponseData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RestService {

    @FormUrlEncoded
    @POST("register")
    suspend fun registerEmail(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("country") country: String,
        @Field("locale") locale: String,
        @Field("currency") currency: String
    ): Response<List<AuthResponseData>>

    @FormUrlEncoded
    @POST("register")
    suspend fun registerFacebook(
        @Field("email") email: String,
        @Field("facebookId") facebookId: String,
        @Field("country") country: String,
        @Field("locale") locale: String,
        @Field("currency") currency: String
    ): Response<List<AuthResponseData>>

    @FormUrlEncoded
    @POST("login")
    suspend fun loginEmail(
       @Field("email") email: String,
       @Field("password") password: String
    ): Response<List<AuthResponseData>>

    @FormUrlEncoded
    @POST("login")
    suspend fun loginFacebook(
        @Field("email") email: String,
        @Field("FacebookId") facebookId: String
    ): Response<List<AuthResponseData>>

    @FormUrlEncoded
    @POST("passwordReset")
    suspend fun resetPassword(
        @Field("email") email: String
    ): Response<ResetPasswordResponseData>

    @FormUrlEncoded
    @POST("extendToken")
    suspend fun extendToken(
        @Field("token") token: String
    ): Response<AuthResponseData>

    @POST("subscriptions")
    suspend fun requestSubscriptions(
        @Body subscriptionsRequestData: SubscriptionsRequestData
    ): Response<List<OfferResponseData>>

    @POST("subscription")
    suspend fun subscribe(
        @Body subscribeRequestData: SubscribeRequestData
    ): Response<Unit>
}