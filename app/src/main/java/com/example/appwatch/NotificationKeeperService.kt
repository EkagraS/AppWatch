package com.example.appwatch.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.appwatch.domain.repository.AppNotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationKeeperService : NotificationListenerService() {

    @Inject
    lateinit var repository: AppNotificationRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // 1. Summary Notifications ko ignore karo (e.g. WhatsApp ka group header)
        // Ye aksar double counting ka sabse bada kaaran hota hai

        // 2. Ongoing notifications ko ignore karo (e.g. Music Player, Downloading, Call)
        // Kyunki ye har second update hote hain aur count hazaron mein pahuncha denge

        // 3. Apni hi app ke notifications ko count mat karo (Optional)

        // 4. Sirf "Alerting" notifications pakadne ke liye (Optional but recommended)
        // Isse silent notifications count nahi honge
        // if (notification.priority < android.app.Notification.PRIORITY_DEFAULT) return
        val packageName = sbn?.packageName ?: "unknown"
        android.util.Log.d("AppWatch_Service", "Raw Notification: $packageName")
        sbn?.let {
            val packageName = it.packageName
            val notification = it.notification
            val isSummary = (notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
            if (isSummary) {
                val activeNotifs = activeNotifications
                val hasChildren = activeNotifs?.any { active ->
                    active.packageName == packageName &&
                            active.groupKey == it.groupKey &&
                            (active.notification.flags and
                                    android.app.Notification.FLAG_GROUP_SUMMARY) == 0
                } ?: false

                if (!hasChildren) {
                    val today = java.time.LocalDate.now().toString()
                    serviceScope.launch {
                        repository.incrementNotificationCount(packageName, today)
                    }
                }
                return
            }
            if (it.isOngoing) return
            if (packageName == "android" || packageName == "com.android.systemui") return
            if (packageName == applicationContext.packageName) return
            val today = java.time.LocalDate.now().toString()
            serviceScope.launch {
                android.util.Log.d("AppWatch_Service", "Final Count for: $packageName")
                repository.incrementNotificationCount(packageName, today)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d("AppWatch_Service", "✅ Service Connected! System ne bind kar liya hai.")
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        sbn?.let {
            val pkg = it.packageName
            val today = java.time.LocalDate.now().toString()

            serviceScope.launch {
                when (reason) {
                    REASON_CLICK -> repository.updateNotificationStats(pkg, today, "OPENED")

                    REASON_CANCEL,REASON_CANCEL_ALL -> repository.updateNotificationStats(pkg, today, "DISMISSED")

                    else -> { /* Optional: Track system-killed notifications */ }
                }
            }
        }
    }
}