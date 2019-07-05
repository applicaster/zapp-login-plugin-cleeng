package com.applicaster.cleeng.utils

import android.content.Context
import android.content.SharedPreferences
import com.applicaster.app.CustomApplication

class SharedPreferencesUtil {
    private val KEY_PREFERENCES = "cleeng_prefs"
    private val KEY_USER_TOKEN = "user_token"

    private val sharedPreferences: SharedPreferences? =
        CustomApplication.getApplication().applicationContext.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor? = sharedPreferences?.edit()
    private val cryptoUtils = CryptoUtil()

    fun saveUserToken(token: String) {
        editor?.remove(cryptoUtils.encode(KEY_USER_TOKEN))
        editor?.putString(cryptoUtils.encode(KEY_USER_TOKEN), cryptoUtils.encryptToken(token))
        editor?.apply()
    }

    fun getUserToken(): String =
        cryptoUtils.decryptToken(sharedPreferences?.getString(cryptoUtils.encode(KEY_USER_TOKEN), "").orEmpty())

    fun removeUserToken() {
        editor?.remove(cryptoUtils.encode(KEY_USER_TOKEN))
        editor?.apply()
    }

 }