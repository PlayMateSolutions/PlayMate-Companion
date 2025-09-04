package com.jsramraj.playmatecompanion.android.utils

import android.content.Context
import android.content.pm.PackageManager

object AppInfo {
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }
}
