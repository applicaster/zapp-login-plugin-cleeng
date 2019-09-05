package com.applicaster.cleeng.network

import com.applicaster.cleeng.network.request.RestoreSubscriptionsRequestData
import com.applicaster.cleeng.network.request.SubscribeRequestData
import com.applicaster.cleeng.network.request.SubscriptionsRequestData
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.ResetPasswordResponseData
import com.applicaster.cleeng.network.response.RestoreSubscriptionsResponseData
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import kotlinx.coroutines.experimental.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RestService {

    @FormUrlEncoded
    @POST("register")
    fun registerEmail(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("country") country: String,
        @Field("locale") locale: String,
        @Field("currency") currency: String
    ): Deferred<Response<List<AuthResponseData>>>

    @FormUrlEncoded
    @POST("register")
    fun registerFacebook(
        @Field("email") email: String,
        @Field("facebookId") facebookId: String,
        @Field("country") country: String,
        @Field("locale") locale: String,
        @Field("currency") currency: String
    ): Deferred<Response<List<AuthResponseData>>>

    @FormUrlEncoded
    @POST("login")
    fun loginEmail(
        @Field("email") email: String,
        @Field("password") password: String
    ): Deferred<Response<List<AuthResponseData>>>

    @FormUrlEncoded
    @POST("login")
    fun loginFacebook(
        @Field("email") email: String,
        @Field("facebookId") facebookId: String
    ): Deferred<Response<List<AuthResponseData>>>

    @FormUrlEncoded
    @POST("passwordReset")
    fun resetPassword(
        @Field("email") email: String
    ): Deferred<Response<ResetPasswordResponseData>>

    @FormUrlEncoded
    @POST("extendToken")
    fun extendToken(
        @Field("token") token: String
    ): Deferred<Response<List<AuthResponseData>>>

    @POST("subscriptions")
    fun requestSubscriptions(
        @Body subscriptionsRequestData: SubscriptionsRequestData
    ): Deferred<Response<List<SubscriptionsResponseData>>>

    @POST("subscription")
    fun subscribe(
        @Body subscribeRequestData: SubscribeRequestData
    ): Deferred<Response<Unit>>

    @POST("restoreSubscriptions")
    fun restoreSubscriptions(
        @Body restoreSubscriptionsData: RestoreSubscriptionsRequestData
    ): Deferred<Response<List<RestoreSubscriptionsResponseData>>>
}