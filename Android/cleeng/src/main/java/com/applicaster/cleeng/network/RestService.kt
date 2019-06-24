package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.request.SubscribeRequest
import com.applicaster.cleeng.network.request.SubscriptionsRequest
import com.applicaster.cleeng.network.response.OfferResponse
import com.applicaster.cleeng.network.response.RegisterResponce
import com.applicaster.cleeng.network.response.ResetPasswordResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RestService {

    @FormUrlEncoded
    @POST("register")
    suspend fun register(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("country") country: String,
        @Field("locale") locale: String,
        @Field("currency") currency: String
    ): Response<List<RegisterResponce>>

    @FormUrlEncoded
    @POST("register")
    suspend fun registerFacebook(
        @Field("email") email: String,
        @Field("facebookId") facebookId: String,
        @Field("country") country: String,
        @Field("locale") locale: String,
        @Field("currency") currency: String
    ): Response<List<RegisterResponce>>

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
       @Field("email") email: String,
       @Field("password") password: String
    ): Response<List<RegisterResponce>>

    @FormUrlEncoded
    @POST("login")
    suspend fun loginFacebook(
        @Field("email") email: String,
        @Field("FacebookId") facebookId: String
    ): Response<List<RegisterResponce>>

    @FormUrlEncoded
    @POST("passwordReset")
    suspend fun resetPassword(
        @Field("email") email: String
    ): Response<ResetPasswordResponse>

    @FormUrlEncoded
    @POST("extendToken")
    suspend fun extendToken(
        @Field("token") token: String
    ): Response<RegisterResponce>

    @POST("subscriptions")
    suspend fun requestSubscriptions(
        @Body subscriptionsRequest: SubscriptionsRequest
    ): Response<List<OfferResponse>>

    @POST("subscription")
    suspend fun subscribe(
        @Body subscribeRequest: SubscribeRequest
    ): Response<Unit>

}