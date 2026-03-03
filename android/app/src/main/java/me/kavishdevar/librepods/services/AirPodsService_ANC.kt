'package me.kavishdevar.librepods.services

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.constants.AirPodsNotifications
import me.kavishdevar.librepods.utils.AACPManager
import me.kavishdevar.librepods.widgets.NoiseControlWidget

/**
 * ANC + Noise Control logic for AirPodsService
 * This file uses extension functions and MUST NOT redeclare the service class.
 */

fun AirPodsService.sendANCBroadcast() {
    sendBroadcast(
        Intent(AirPodsNotifications.ANC_DATA).apply {
            putExtra("data", ancNotification.status)
        }
    )
}

fun AirPodsService.updateNoiseControlWidget() {
    val appWidgetManager = AppWidgetManager.getInstance(this)
    val componentName = ComponentName(this, NoiseControlWidget::class.java)
    val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

    val remoteViews = RemoteViews(packageName, R.layout.noise_control_widget).also { views ->

        val ancStatus = ancNotification.status

        val allowOffModeValue =
            aacpManager.controlCommandStatusList.find {
                it.identifier ==
                        AACPManager.Companion.ControlCommandIdentifiers.ALLOW_OFF_OPTION
            }

        val allowOffMode =
            allowOffModeValue?.value?.takeIf { it.isNotEmpty() }?.get(0) == 0x01.toByte()

        views.setInt(
            R.id.widget_off_button,
            "setBackgroundResource",
            if (ancStatus == 1)
                R.drawable.widget_button_checked_shape_start
            else
                R.drawable.widget_button_shape_start
        )

        views.setInt(
            R.id.widget_transparency_button,
            "setBackgroundResource",
            if (ancStatus == 3)
                if (allowOffMode)
                    R.drawable.widget_button_checked_shape_middle
                else
                    R.drawable.widget_button_checked_shape_start
            else
                if (allowOffMode)
                    R.drawable.widget_button_shape_middle
                else
                    R.drawable.widget_button_shape_start
        )

        views.setInt(
            R.id.widget_adaptive_button,
            "setBackgroundResource",
            if (ancStatus == 4)
                R.drawable.widget_button_checked_shape_middle
            else
                R.drawable.widget_button_shape_middle
        )

        views.setInt(
            R.id.widget_anc_button,
            "setBackgroundResource",
            if (ancStatus == 2)
                R.drawable.widget_button_checked_shape_end
            else
                R.drawable.widget_button_shape_end
        )

        views.setViewVisibility(
            R.id.widget_off_button,
            if (allowOffMode) View.VISIBLE else View.GONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewLayoutMargin(
                R.id.widget_transparency_button,
                RemoteViews.MARGIN_START,
                if (allowOffMode) 2f else 12f,
                TypedValue.COMPLEX_UNIT_DIP
            )
        } else {
            views.setViewPadding(
                R.id.widget_transparency_button,
                if (allowOffMode) 2.dpToPx() else 12.dpToPx(),
                12.dpToPx(),
                2.dpToPx(),
                12.dpToPx()
            )
        }
    }

    appWidgetManager.updateAppWidget(widgetIds, remoteViews)
}

fun AirPodsService.getANC(): Int {
    return ancNotification.status
}