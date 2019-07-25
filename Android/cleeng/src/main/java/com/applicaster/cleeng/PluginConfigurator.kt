package com.applicaster.cleeng

import com.applicaster.cleeng.network.error.WebServiceError

class PluginConfigurator(private val pluginConfig: Map<String, String>) {
    fun getPluginConfig() = pluginConfig

    fun getPublisherId(): String {
        return getOrDefault(KEY_PUBLISHER_ID)
    }

    fun isTriggerOnAppLaunch(): Boolean {
        return getOrEmpty(KEY_TRIGGER_ON_APP_LAUNCH)?.toBoolean() ?: false
    }

    fun getAppLevelEntitlements(): List<String> =
    // obtain string with coma separated list like "4, 8, 15, 16, 23, 42" and convert to List<String>
        getOrEmpty(KEY_APP_LEVEL_ENTITLEMENTS)?.split(",")?.map { it.trim() } ?: listOf()

    fun getCleengErrorMessage(webError: WebServiceError): String {
        return when (webError) {
            WebServiceError.DEFAULT -> getOrDefault(KEY_ERROR_DEFAULT)
            WebServiceError.NO_USER_EXISTS -> getOrDefault(KEY_ERROR_NO_EXISTING_USER)
            WebServiceError.USER_ALREADY_EXISTS -> getOrDefault(KEY_ERROR_USER_ALREADY_EXISTS)
            WebServiceError.INVALID_CREDENTIALS -> getOrDefault(KEY_ERROR_CREDENTIALS)
        }
    }

    private fun getOrDefault(key: String) = getOrEmpty(key).orEmpty()

    private fun getOrEmpty(key: String): String? {
        return if (key in pluginConfig)
            pluginConfig[key]
        else
            null
    }
}

const val KEY_PUBLISHER_ID = "cleeng_login_publisher_id"
const val KEY_TRIGGER_ON_APP_LAUNCH = "trigger_on_app_launch"
const val KEY_APP_LEVEL_ENTITLEMENTS = "app_level_product_ids"

const val KEY_ERROR_DEFAULT = "default_alert_text"
const val KEY_ERROR_NO_EXISTING_USER = "nonexistent_user_alert_text"
const val KEY_ERROR_USER_ALREADY_EXISTS = "existing_user_alert_text"
const val KEY_ERROR_CREDENTIALS = "invalid_credentials_alert_text"