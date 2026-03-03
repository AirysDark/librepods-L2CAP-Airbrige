    private fun showSocketConnectionFailureNotification(errorMessage: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "socket_connection_failure")
            .setSmallIcon(R.drawable.airpods)
            .setContentTitle("AirPods Connection Issue")
            .setContentText("Unable to connect to AirPods over L2CAP")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your AirPods are connected via Bluetooth, but LibrePods couldn't connect to AirPods using L2CAP. " +
                         "Error: $errorMessage"))
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3, notification)
    }
