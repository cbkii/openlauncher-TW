package com.openlauncher.app.headunit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.ComponentName

class LaunchTargetResolver(private val context: Context) {

    fun resolve(target: LaunchTarget): Intent? {
        val pm = context.packageManager

        // 1. Explicit component
        if (target.packageName != null && target.className != null) {
            val intent = Intent().apply {
                component = ComponentName(target.packageName, target.className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                return intent
            }
        }

        // 2. Action or URI
        if (target.action != null || target.uri != null) {
            val intent = Intent().apply {
                if (target.action != null) action = target.action
                if (target.uri != null) data = Uri.parse(target.uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                return intent
            }
        }

        // 3. Package launch intent
        if (target.packageName != null) {
            val intent = pm.getLaunchIntentForPackage(target.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return intent
            }
        }

        // 4. ACTION_MAIN fallback
        if (target.packageName != null) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setPackage(target.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                 return intent
            }
        }

        return null
    }

    fun isAvailable(target: LaunchTarget): Boolean {
        return resolve(target) != null
    }
}
