package com.applicaster.cleeng

class PluginConfigurator(private val pluginConfig: Map<String, String>) {
    fun getPluginConfig() = pluginConfig

    fun getPublisherId(): String {
        return getOrEmpty(KEY_PUBLISHER_ID) ?: ""
    }

    fun isTriggerOnAppLaunch(): Boolean {
        return getOrEmpty(KEY_TRIGGER_ON_APP_LAUNCH)?.toBoolean() ?: false
    }

    private fun getOrEmpty(key: String): String? {
        return if (key in pluginConfig)
            pluginConfig[key]
        else
            null
    }
}

const val KEY_PUBLISHER_ID = "cleeng_login_publisher_id"
const val KEY_TRIGGER_ON_APP_LAUNCH = "trigger_on_app_launch"