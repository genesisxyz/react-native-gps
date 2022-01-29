package com.reactnativegps

import androidx.core.app.NotificationCompat
import android.content.Context
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import java.lang.ref.WeakReference

object Notification {
    private const val TAG = "Notification"
    private const val NOTIFICATION_ID = 12345678
    private const val NOTIFICATION_CHANNEL_ID = "location"
    private const val NOTIFICATION_CONTENT_TITLE = "GPS"
    private const val NOTIFICATION_CONTENT_TEXT = "Tracking enabled"
    private const val NOTIFICATION_SMALL_ICON = "ic_launcher"

    var notificationBuilder: WeakReference<NotificationCompat.Builder>? = null
        private set

    private var notification: Notification? = null

    var notificationId = NOTIFICATION_ID
        private set
    var notificationChannelId = NOTIFICATION_CHANNEL_ID
        private set
    var notificationContentTitle = NOTIFICATION_CONTENT_TITLE
        private set
    var notificationContentText = NOTIFICATION_CONTENT_TEXT
        private set
    var notificationChannelName = NOTIFICATION_CONTENT_TEXT
        private set
    var notificationChannelDescription = NOTIFICATION_CONTENT_TEXT
        private set
    var notificationSmallIcon = NOTIFICATION_SMALL_ICON
        private set

    fun getSmallIconId(context: Context): Int {
        var id = context.resources.getIdentifier(notificationSmallIcon, "drawable", context.packageName)
        if (id == 0) {
            id = context.resources.getIdentifier(notificationSmallIcon, "mipmap", context.packageName)
        }
        return id
    }

    fun getNotification(context: Context): Notification? {
        if (notification == null) {
            notificationBuilder = WeakReference(NotificationCompat.Builder(context, notificationChannelId)
                .setContentTitle(notificationContentTitle)
                .setContentText(notificationContentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
            )

            val smallIconId = getSmallIconId(context)
            if (smallIconId != 0) {
                notificationBuilder!!.get()!!.setSmallIcon(smallIconId);
            }

            notification = notificationBuilder!!.get()!!.build()
        }
        return notification
    }

    fun removeNotification() {
        notificationBuilder = null
        notification = null
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(notificationChannelId, notificationChannelName, importance)
            channel.description = notificationChannelDescription;
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(context: Context, options: HashMap<String, Any>?) {
        val androidOptions = options?.get("android") as HashMap<String, Any>?;

        val notificationOptions = androidOptions?.get("notification") as HashMap<String, Any>?;
        val newNotificationId = notificationOptions?.get("id") as Double?;
        val newNotificationContentTitle = notificationOptions?.get("contentTitle") as String?;
        val newNotificationContentText = notificationOptions?.get("contentText") as String?;
        val newNotificationSmallIcon = notificationOptions?.get("smallIcon") as String?;

        val notificationChannelOptions = androidOptions?.get("channel") as HashMap<String, Any>?;
        val newNotificationChannelId = notificationChannelOptions?.get("id") as String?;
        val newNotificationChannelName = notificationChannelOptions?.get("name") as String?;
        val newNotificationChannelDescription = notificationChannelOptions?.get("description") as String?;

        if (newNotificationId != null) {
            notificationId = newNotificationId.toInt()
        }

        if (newNotificationChannelId != null) {
            notificationChannelId = newNotificationChannelId
        }

        if (newNotificationContentTitle != null) {
            notificationContentTitle = newNotificationContentTitle
        }

        if (newNotificationContentText != null) {
            notificationContentText = newNotificationContentText
        }

        if (newNotificationChannelName != null) {
            notificationChannelName = newNotificationChannelName
        }

        if (newNotificationChannelDescription != null) {
            notificationChannelDescription = newNotificationChannelDescription
        }

        if (newNotificationSmallIcon != null) {
            notificationSmallIcon = newNotificationSmallIcon
        }
    }
}
