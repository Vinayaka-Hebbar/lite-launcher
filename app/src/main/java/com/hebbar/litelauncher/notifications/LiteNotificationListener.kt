package com.hebbar.litelauncher.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class LiteNotificationListener : NotificationListenerService() {

    companion object {
        var activeNotificationPackages = mutableSetOf<String>()
        var onNotificationChangedListener: (() -> Unit)? = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.packageName?.let { pkg ->
            activeNotificationPackages.add(pkg)
            onNotificationChangedListener?.invoke()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateActiveNotifications()
    }

    private fun updateActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            val packages = active.map { it.packageName }.toSet()
            activeNotificationPackages.clear()
            activeNotificationPackages.addAll(packages)
            onNotificationChangedListener?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
