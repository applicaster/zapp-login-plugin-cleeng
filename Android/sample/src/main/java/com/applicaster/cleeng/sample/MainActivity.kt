package com.applicaster.cleeng.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.applicaster.cleeng.network.NetworkHelper
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.error.Error
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.handleResponse
import com.applicaster.cleeng.network.request.SubscriptionsRequest
import com.applicaster.cleeng.network.response.OfferResponse
import com.applicaster.cleeng.network.response.AuthResponse
import com.applicaster.cleeng.utils.SharedPreferencesUtil

class MainActivity : AppCompatActivity() {

    private val networkHelper: NetworkHelper by lazy { NetworkHelper("") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //TODO: test call! Should be removed later!
        val prefs = SharedPreferencesUtil()
        prefs.saveUserToken("my-custom-token")
        login()
        val token = prefs.getUserToken()
        Log.w(MainActivity::class.java.simpleName, token)
    }

    private fun login() {
        executeRequest {
            val response = networkHelper.login("user email", "password")
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseResult: List<AuthResponse>? = result.value
                    subscriptions(responseResult?.get(0)?.token ?: "")
                }
                is Result.Failure -> {
                    val error: Error? = result.value
                    //error handling logic
                }
            }
        }
    }

    private fun subscriptions(token: String) {
        val request = SubscriptionsRequest(
            1,
            arrayListOf(),
            token
        )

        executeRequest {
            val response = networkHelper.requestSubscriptions(request)
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseResult: List<OfferResponse>? = result.value
                    //response handling logic
                }

                is Result.Failure -> {
                    val error: Error? = result.value
                    //error handling logic
                }
            }
        }
    }
}
