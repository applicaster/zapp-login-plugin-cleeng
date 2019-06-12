package com.applicaster.cleeng

import android.content.Context
import android.support.v4.app.Fragment
import com.applicaster.cleeng.network.error.Error
import com.applicaster.cleeng.network.NetworkService
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.handleResponse
import com.applicaster.cleeng.network.launchRequest
import com.applicaster.cleeng.network.response.RegisterResponce
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.login.LoginContract
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.screen.PluginScreen
import java.io.Serializable
import java.util.HashMap

class CleengLoginPlugin : LoginContract, PluginScreen {

    override fun login(
        context: Context?,
        additionalParams: MutableMap<Any?, Any?>?,
        callback: LoginContract.Callback?
    ) {
        //TODO: test call! Should be removed later!
        launchRequest {
            val response = NetworkService.login("", "")
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val body: List<RegisterResponce>? = result.value
                }
                is Result.Failure -> {
                    val error: Error? = result.value
                }
            }
        }
    }

    override fun login(
        context: Context?,
        playable: Playable?,
        additionalParams: MutableMap<Any?, Any?>?,
        callback: LoginContract.Callback?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isTokenValid(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setToken(token: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isItemLocked(model: Any?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun executeOnStartup(context: Context?, listener: HookListener?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getToken(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setPluginConfigurationParams(params: MutableMap<Any?, Any?>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handlePluginScheme(context: Context?, data: MutableMap<String, String>?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun executeOnApplicationReady(context: Context?, listener: HookListener?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun logout(
        context: Context?,
        additionalParams: MutableMap<Any?, Any?>?,
        callback: LoginContract.Callback?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun generateFragment(screenMap: HashMap<String, Any>?, dataSource: Serializable?): Fragment {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun present(
        context: Context?,
        screenMap: HashMap<String, Any>?,
        dataSource: Serializable?,
        isActivity: Boolean
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}