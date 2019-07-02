package com.applicaster.cleeng

import android.content.Context
import android.support.v4.app.Fragment
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.cam.ContentAccessManager
import com.applicaster.hook_screen.HookScreen
import com.applicaster.hook_screen.HookScreenListener
import com.applicaster.hook_screen.HookScreenManager
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.login.LoginContract
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.screen.PluginScreen
import java.io.Serializable
import kotlin.collections.HashMap

class CleengLoginPlugin : LoginContract, PluginScreen, HookScreen {

    private val cleengService: CleengService by lazy { CleengService(this@CleengLoginPlugin) }
    private var pluginConfig: Map<String, String>? = mapOf()

    private lateinit var hookListener: HookScreenListener

    //TODO: Test mock call that should emulate flow after click on video
    fun mockStartProcess(
        context: Context,
        mockPluginConfiguration: Map<String, String>
    ) {
        if (pluginConfig.isNullOrEmpty())
            pluginConfig = mockPluginConfiguration
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
            cleengService.fetchFeedData(playable)
            if (!isTokenValid) {
                ContentAccessManager.onProcessStarted(cleengService.camContract, it)
            }
        }
    }

    override fun isTokenValid(): Boolean = !cleengService.getUser().token.isNullOrEmpty()

    override fun setToken(token: String?) {
        // Empty body
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
        if (context != null && listener != null)
            cleengService.handleStartupHook(context, listener)
    }

    override fun getToken(): String = cleengService.getUser().token.orEmpty()

    override fun setPluginConfigurationParams(params: MutableMap<Any?, Any?>?) {
        //transform MutableMap<Any?, Any?>? to Map<String, String>?
        pluginConfig = params?.entries?.associate { entry ->
            entry.key.toString() to entry.value.toString()
        }
    }

    fun getPluginConfigurationParams() = pluginConfig.orEmpty()

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
        //TODO: Empty body?
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
        val playable = hookProps?.get(HookScreenManager.HOOK_PROPS_DATASOURCE_KEY) as? APAtomEntry.APAtomEntryPlayable
        login(context, playable, mutableMapOf()) {}
    }

    override fun getListener(): HookScreenListener =
        hookListener

    override fun hookDismissed() {
        //TODO: Empty body?
    }

    override fun isFlowBlocker(): Boolean = true

    override fun isRecurringHook(): Boolean = true

    override fun shouldPresent(): Boolean = true
}