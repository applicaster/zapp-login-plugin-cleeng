package com.applicaster.cleeng.sample

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.applicaster.cleeng.CleengLoginPlugin
import com.applicaster.cleeng.network.NetworkHelper
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.error.Error
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.handleResponse
import com.applicaster.cleeng.network.request.SubscriptionsRequestData
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.OfferResponseData
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private val networkHelper: NetworkHelper by lazy { NetworkHelper("") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_mock_start.setOnClickListener { CleengLoginPlugin().mockStartProcess(this, getMockPluginConfiguration()) }
//        //TODO: test call! Should be removed later!
//        val prefs = SharedPreferencesUtil()
//        prefs.saveUserToken("my-custom-token")
//        login()
//        val token = prefs.getUserToken()
//        Log.w(MainActivity::class.java.simpleName, token)
    }

    private fun getMockPluginConfiguration(): Map<String, String> =
        Gson().fromJson(getConfigFromAssets(this), Map::class.java) as Map<String, String>

    private fun getConfigFromAssets(context: Context): String {
        val inputStream =
            context.resources.openRawResource(R.raw.mock_config)
        val size = inputStream.available()

        val json: String?

        try {
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            json = String(buffer, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }

        return json
    }

    private fun login() {
        executeRequest {
            val response = networkHelper.login("user email", "password")
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseResult: List<AuthResponseData>? = result.value
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
        val request = SubscriptionsRequestData(
            1,
            arrayListOf(),
            token
        )

        executeRequest {
            val response = networkHelper.requestSubscriptions(request)
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseDataResult: List<SubscriptionsResponseData>? = result.value
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
