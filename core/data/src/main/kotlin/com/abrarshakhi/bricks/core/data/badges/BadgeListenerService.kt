package com.abrarshakhi.bricks.core.data.badges

import android.app.Notification
import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.abrarshakhi.bricks.core.domain.model.PackageUserKey
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BadgeListenerService : NotificationListenerService(), KoinComponent {

    private val store: NotificationBadgeStore by inject()
    private val userManager: UserManager by lazy { getSystemService(UserManager::class.java) }

    override fun onListenerConnected() {
        store.setConnected(true)
        publish()
    }

    override fun onListenerDisconnected() {
        store.setConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        publish()
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        publish()
    }

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val rankings = runCatching { currentRanking }.getOrNull()
        val ranking = Ranking()
        val counts = active.asSequence()
            .filter { sbn ->
                val flags = sbn.notification.flags
                (flags and Notification.FLAG_GROUP_SUMMARY) == 0 &&
                    (flags and Notification.FLAG_ONGOING_EVENT) == 0 &&
                    (rankings == null || !rankings.getRanking(sbn.key, ranking) || ranking.canShowBadge())
            }
            .groupingBy { sbn -> PackageUserKey(sbn.packageName, userManager.getSerialNumberForUser(sbn.user)) }
            .eachCount()
        store.publish(counts)
    }
}
