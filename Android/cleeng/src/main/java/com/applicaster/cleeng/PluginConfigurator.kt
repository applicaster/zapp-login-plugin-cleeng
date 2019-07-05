package com.applicaster.cleeng

import com.applicaster.cleeng.network.error.WebServiceError

class PluginConfigurator(private val pluginConfig: Map<String, String>) {
    fun getPluginConfig() = pluginConfig

    fun getPublisherId(): String {
        return getOrEmpty(KEY_PUBLISHER_ID) ?: ""
    }

    fun isTriggerOnAppLaunch(): Boolean {
        return getOrEmpty(KEY_TRIGGER_ON_APP_LAUNCH)?.toBoolean() ?: false
    }

    fun getCleengErrorMessage(webError: WebServiceError): String {
        return when (webError) {
            WebServiceError.DEFAULT -> getOrEmpty(KEY_ERROR_DEFAULT).orEmpty()
            WebServiceError.NO_USER_EXISTS -> getOrEmpty(KEY_ERROR_NO_EXISTING_USER).orEmpty()
            WebServiceError.USER_ALREADY_EXISTS -> getOrEmpty(KEY_ERROR_USER_ALREADY_EXISTS).orEmpty()
            WebServiceError.INVALID_CREDENTIALS -> getOrEmpty(KEY_ERROR_CREDENTIALS).orEmpty()
        }
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

const val KEY_ERROR_DEFAULT = "default_alert_text"
const val KEY_ERROR_NO_EXISTING_USER = "nonexistent_user_alert_text"
const val KEY_ERROR_USER_ALREADY_EXISTS = "existing_user_alert_text"
const val KEY_ERROR_CREDENTIALS = "invalid_credentials_alert_text"