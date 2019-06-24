package com.applicaster.cleeng

import android.content.Context
import android.support.v4.app.Fragment
import com.applicaster.cam.ContentAccessManager
import com.applicaster.cleeng.cam.CamContract
import com.applicaster.cleeng.network.*
import com.applicaster.cleeng.network.response.RegisterResponce
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.login.LoginContract
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.screen.PluginScreen
import java.io.Serializable
import java.util.HashMap

class CleengLoginPlugin : LoginContract, PluginScreen {

    private val cleengService: CleengService by lazy { CleengService() }
    private val camContract: CamContract by lazy { CamContract() }
    private var pluginConfig: Map<String, String>? = mapOf()

    override fun login(
        context: Context?,
        additionalParams: MutableMap<Any?, Any?>?,
        callback: LoginContract.Callback?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun login(
        context: Context?,
        playable: Playable?,
        additionalParams: MutableMap<Any?, Any?>?,
        callback: LoginContract.Callback?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isTokenValid(): Boolean = !cleengService.getUser().token.isNullOrEmpty()

    override fun setToken(token: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isItemLocked(model: Any?): Boolean {
        return cleengService.isItemLocked(model)
    }

    override fun isItemLocked(context: Context?, model: Any?, callback: LoginContract.Callback?) {
        cleengService.isItemLocked(model) { result ->
            callback?.onResult(result)
        }
    }

    override fun executeOnStartup(context: Context?, listener: HookListener?) {
        executeRequest {
            val response = cleengService.networkHelper.extendToken(cleengService.getUser().token.orEmpty())
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseResult: RegisterResponce? = result.value
                    // save token to prefs?
                    cleengService.saveUserToken(context, responseResult?.token.orEmpty())
                    // finish hook
                    listener?.onHookFinished()
                }

                is Result.Failure -> {
                    // handle error and open login or sign up screen
                    context?.let { ContentAccessManager.onProcessStarted(camContract, it) }
                }
            }
        }
        TODO("Go to the network and get available subscriptions and tokens?")
    }

    override fun getToken(): String = cleengService.getUser().token.orEmpty()

    override fun setPluginConfigurationParams(params: MutableMap<Any?, Any?>?) {
        //transform MutableMap<Any?, Any?>? to Map<String, String>?
        pluginConfig = params?.entries?.associate { entry ->
            entry.key.toString() to entry.value.toString()
        }
    }

    override fun handlePluginScheme(context: Context?, data: MutableMap<String, String>?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun executeOnApplicationReady(context: Context?, listener: HookListener?) {
        listener?.onHookFinished()
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