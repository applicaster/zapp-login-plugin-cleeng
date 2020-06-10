package com.applicaster.cleeng

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import com.applicaster.cam.CamFlow
import com.applicaster.cam.Trigger
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.utils.isNullOrEmpty
import com.applicaster.hook_screen.HookScreen
import com.applicaster.hook_screen.HookScreenListener
import com.applicaster.hook_screen.HookScreenManager
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.login.LoginContract
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.screen.PluginScreen
import com.applicaster.zapp.configfetcher.ZappConfigFetcher
import java.io.Serializable

class CleengLoginPlugin : LoginContract, PluginScreen, HookScreen {

    private val TAG = CleengLoginPlugin::class.java.simpleName

    private val cleengService: CleengService by lazy { CleengService() }

    private var hookListener: HookScreenListener? = null

    //TODO: Test mock call that should emulate flow after click on video
    fun mockStartProcess(
        context: Context,
        mockPluginConfiguration: Map<String, String>
    ) {
        if (Session.getPluginConfigurationParams().isEmpty())
            Session.pluginConfigurator = PluginConfigurator(mockPluginConfiguration)
        Session.setCamFlow(CamFlow.AUTH_AND_STOREFRONT)
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
        if (!cleengService.isUserLogged()) {
            if (isTriggeredByComponent(additionalParams))
                additionalParams?.let {
                    handleUserAccountComponentTrigger(callback, context)
                }
            else {
                setUpLoginContractListener(callback)
                context?.let { cleengService.handleLogin(playable, this, it) }
            }
        }
    }

    private fun setUpLoginContractListener(callback: LoginContract.Callback?) {
        hookListener = object : HookScreenListener {
            override fun hookCompleted(hookProps: MutableMap<String, Any>?) {
                callback?.onResult(true)
            }

            override fun hookFailed(hookProps: MutableMap<String, Any>?) {
                callback?.onResult(false)
            }
        }
    }

    private fun isTriggeredByComponent(params: MutableMap<Any?, Any?>?) =
        params?.containsKey(KEY_USER_ACCOUNT_TRIGGER) ?: false
                && params?.get(KEY_USER_ACCOUNT_TRIGGER).toString().toBoolean()

    private fun handleUserAccountComponentTrigger(
        callback: LoginContract.Callback?,
        context: Context?
    ) {
        val completionListener = HookListener { callback?.onResult(true) }
        Session.triggerStatus = Session.TriggerStatus.USER_ACCOUNT_COMPONENT
        Session.analyticsDataProvider.trigger = Trigger.UAC
        context?.let {
            cleengService.handleLaunchWithoutPlayable(it, completionListener, true)
        }
    }

    override fun isTokenValid(): Boolean = cleengService.getUserToken().isNotEmpty()

    override fun setToken(token: String?) {
        // Empty body
    }

    override fun isItemLocked(model: Any?): Boolean {
        return cleengService.itemAccessHandler.isItemLocked(model)
    }

    override fun isItemLocked(context: Context?, model: Any?, callback: LoginContract.Callback?) {
        cleengService.itemAccessHandler.checkItemLocked(model, callback)
    }

    override fun executeOnStartup(context: Context?, listener: HookListener?) {
        Session.triggerStatus = Session.TriggerStatus.APP_LAUNCH
        Session.analyticsDataProvider.trigger = Trigger.APP_LAUNCH
        executeRequest {
            loadPluginConfig(context)
            if (context != null && listener != null)
                cleengService.handleLaunchWithoutPlayable(
                    context,
                    listener,
                    Session.pluginConfigurator?.isTriggerOnAppLaunch() ?: false
                )
        }
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
        context?.let {
            Session.setCamFlow(CamFlow.LOGOUT)
            cleengService.logout(it, callback)
        }
    }

    override fun generateFragment(
        screenMap: HashMap<String, Any>?,
        dataSource: Serializable?
    ): Fragment? =
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
        set(value) {
            field = value
        }

    override fun executeHook(
        context: Context,
        hookListener: HookScreenListener,
        hookProps: Map<String, Any>?
    ) {
        Session.triggerStatus = Session.TriggerStatus.TAP_CELL
        Session.analyticsDataProvider.trigger = Trigger.TAP_SELL
        this.hookListener = hookListener
        executeRequest {
            val dataSource: Any? = hookProps?.get(HookScreenManager.HOOK_PROPS_DATASOURCE_KEY)
            loadPluginConfig(context)
            cleengService.handleLogin(dataSource, this, context)
        }
    }

    private suspend fun loadPluginConfig(context: Context?) {
        if (Session.pluginConfigurator?.getPluginConfig().isNullOrEmpty()) {
            val configFetcher = ZappConfigFetcher("cleeng")
            val pluginConfig = configFetcher.loadFullConfig(context, true, hook)
            Session.pluginConfigurator = PluginConfigurator(pluginConfig)
        }
    }

    override fun getListener(): HookScreenListener = hookListener ?: object : HookScreenListener {
        override fun hookCompleted(hookProps: MutableMap<String, Any>?) {
        }

        override fun hookFailed(hookProps: MutableMap<String, Any>?) {
        }
    }

    override fun hookDismissed() {
        // empty
    }

    override fun isFlowBlocker(): Boolean = true

    override fun isRecurringHook(): Boolean = true

    override fun shouldPresent(): Boolean = true

    companion object {
        const val KEY_USER_ACCOUNT_TRIGGER = "UserAccountTrigger"
    }
}