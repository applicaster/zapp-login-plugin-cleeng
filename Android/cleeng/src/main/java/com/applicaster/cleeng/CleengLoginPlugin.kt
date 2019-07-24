package com.applicaster.cleeng

import android.content.Context
import android.support.v4.app.Fragment
import android.util.Log
import com.applicaster.cam.CamFlow
import com.applicaster.cam.ContentAccessManager
import com.applicaster.hook_screen.HookScreen
import com.applicaster.hook_screen.HookScreenListener
import com.applicaster.hook_screen.HookScreenManager
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.login.LoginContract
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.screen.PluginScreen
import com.google.gson.Gson
import java.io.Serializable

class CleengLoginPlugin : LoginContract, PluginScreen, HookScreen {

    private val TAG = CleengLoginPlugin::class.java.simpleName

    private val cleengService: CleengService by lazy { CleengService() }

    private lateinit var hookListener: HookScreenListener

    //TODO: Test mock call that should emulate flow after click on video
    fun mockStartProcess(
        context: Context,
        mockPluginConfiguration: Map<String, String>
    ) {
        if (Session.getPluginConfigurationParams().isEmpty())
            Session.pluginConfigurator = PluginConfigurator(mockPluginConfiguration)
        cleengService.mockStart(context)
    }

    override fun login(
        context: Context?,
        additionalParams: MutableMap<Any?, Any?>?,
        callback: LoginContract.Callback?
    ) {
        // Empty body
    }

    override fun login(
        context: Context?,
        playable: Playable?,
        additionalParams: MutableMap<Any?, Any?>?,
        callback: LoginContract.Callback?
    ) {
        context?.let {
            cleengService.fetchProductData(playable)
            if (cleengService.camContract.getCamFlow() != CamFlow.EMPTY) {
                if (!isTokenValid) {
                    ContentAccessManager.onProcessStarted(cleengService.camContract, it)
                } else {
                    if (Session.isAccessGranted())
                        hookListener.hookCompleted(hashMapOf())
                    else
                        ContentAccessManager.onProcessStarted(cleengService.camContract, it)
                }
            } else {
                hookListener.hookCompleted(mutableMapOf())
            }
        }
    }

    private fun login(
            context: Context?,
            model: Any?,
            additionalParams: MutableMap<Any?, Any?>? = mutableMapOf()
    ) {
        context?.let {
            cleengService.fetchProductData(model)
            if (cleengService.camContract.getCamFlow() != CamFlow.EMPTY) {
                if (!isTokenValid) {
                    ContentAccessManager.onProcessStarted(cleengService.camContract, it)
                } else {
                    if (Session.isAccessGranted())
                        hookListener.hookCompleted(hashMapOf())
                    else
                        ContentAccessManager.onProcessStarted(cleengService.camContract, it)
                }
            } else {
                hookListener.hookCompleted(mutableMapOf())
            }
        }
    }

    override fun isTokenValid(): Boolean = cleengService.getUserToken().isNotEmpty()

    override fun setToken(token: String?) {
        // Empty body
    }

    override fun isItemLocked(model: Any?): Boolean {
        return cleengService.isItemLocked(model)
    }

    override fun isItemLocked(context: Context?, model: Any?, callback: LoginContract.Callback?) {
        cleengService.checkItemLocked(model, callback)
    }

    override fun executeOnStartup(context: Context?, listener: HookListener?) {
        if (context != null && listener != null)
            cleengService.handleStartupHook(context, listener)
    }

    override fun getToken(): String = cleengService.getUserToken()

    override fun setPluginConfigurationParams(params: MutableMap<Any?, Any?>?) {
        // empty
    }

    override fun handlePluginScheme(context: Context?, data: MutableMap<String, String>?): Boolean =
        false

    override fun executeOnApplicationReady(context: Context?, listener: HookListener?) {
        listener?.onHookFinished()
    }

    override fun logout(
        context: Context?,
        additionalParams: MutableMap<Any?, Any?>?,
        callback: LoginContract.Callback?
    ) {
        Session.drop()
        cleengService.logout()
    }

    override fun generateFragment(screenMap: HashMap<String, Any>?, dataSource: Serializable?): Fragment? =
        null

    override fun present(
        context: Context?,
        screenMap: HashMap<String, Any>?,
        dataSource: Serializable?,
        isActivity: Boolean
    ) {
        Log.i(TAG, "Present screen")
    }

    override var hook: HashMap<String, String?> = hashMapOf()
        get() = field
        set(value) { field = value }

    override fun executeHook(
        context: Context,
        hookListener: HookScreenListener,
        hookProps: Map<String, Any>?
    ) {
        this.hookListener = hookListener
        val dataSource: Any? = hookProps?.get(HookScreenManager.HOOK_PROPS_DATASOURCE_KEY)
        val config: MutableMap<Any?, Any?>? =
                getPluginConfiguration(hook["screenMap"].orEmpty())?.get("general") as? MutableMap<Any?, Any?>

        //transform MutableMap<Any?, Any?>? to Map<String, String>?
        val pluginConfig = config?.entries?.associate { entry ->
            entry.key.toString() to entry.value.toString()
        }

        if (pluginConfig != null)
            Session.pluginConfigurator = PluginConfigurator(pluginConfig)
        login(context, dataSource)
    }

    private fun getPluginConfiguration(data: String): Map<String, Any>? =
            Gson().fromJson(data, Map::class.java) as? Map<String, Any>

    override fun getListener(): HookScreenListener = hookListener

    override fun hookDismissed() {
        // empty
    }

    override fun isFlowBlocker(): Boolean = true

    override fun isRecurringHook(): Boolean = true

    override fun shouldPresent(): Boolean = true
}