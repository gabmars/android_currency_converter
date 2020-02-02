package com.example.tinkoff_android_exam

import android.content.SharedPreferences

class SharedPrefsCache(private val sharedPreferences: SharedPreferences){
    fun get(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun set(key: String, value: String){
        sharedPreferences.edit().let {
            it.putString(key, value)
            it.apply()
        }
    }

    fun has(key: String) : Boolean {
        return sharedPreferences.contains(key)
    }
}