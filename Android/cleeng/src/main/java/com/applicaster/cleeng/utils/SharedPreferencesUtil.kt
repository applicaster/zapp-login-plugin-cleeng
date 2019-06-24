package com.applicaster.cleeng.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesUtil(private val context: Context?) {
    private val KEY_PREFERENCES = "cleeng_prefs"
    private val KEY_USER_TOKEN = "user_token"

    private val sharedPreferences: SharedPreferences? = context?.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor? = sharedPreferences?.edit()
    private val cryptoUtils = CryptoUtil()

    fun saveUserToken(token: String) {
        editor?.remove(cryptoUtils.encode(KEY_USER_TOKEN))
        editor?.putString(cryptoUtils.encode(KEY_USER_TOKEN), cryptoUtils.encryptToken(token))
        editor?.apply()
    }

    fun getUserToken(): String =
        cryptoUtils.decryptToken(sharedPreferences?.getString(cryptoUtils.decode(KEY_USER_TOKEN), "").orEmpty())

 }