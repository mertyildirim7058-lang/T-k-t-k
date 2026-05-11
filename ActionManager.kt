package com.example.backtap

import android.content.Context
import android.content.Intent
import android.widget.Toast

object ActionManager {
    private const val PREFS_NAME = "BackTapPrefs"
    private const val KEY_TARGET_PACKAGE = "TARGET_PACKAGE"

    fun saveTargetPackage(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TARGET_PACKAGE, packageName).apply()
    }

    fun getTargetPackage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TARGET_PACKAGE, null)
    }

    fun executeAction(context: Context) {
        val targetPackage = getTargetPackage(context)
        if (targetPackage != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Seçilen uygulama başlatılamadı!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

