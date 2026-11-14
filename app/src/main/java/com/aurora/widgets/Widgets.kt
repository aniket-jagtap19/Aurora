package com.aurora.widgets

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class App : Application() { override fun onCreate() { super.onCreate(); Store.init(this) } }

class AuroraWidget(private val kind: String) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        provideContent {
            val p = palette(Store.cfg(kind)); val w = wContent(kind); val big = kind in listOf("clock", "date", "battery", "countdown")
            Box(GlanceModifier.fillMaxSize().background(p.bg).cornerRadius(26.dp).padding(14.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))), contentAlignment = Alignment.CenterStart) {
                if (kind == "clock") {
                    val rv = RemoteViews(context.packageName, R.layout.w_clock).apply {
                        setTextColor(R.id.tc_day, p.ac.toArgb()); setTextColor(R.id.tc_time, p.fg.toArgb())
                        setTextColor(R.id.tc_date, p.fg.copy(alpha = .7f).toArgb())
                    }
                    AndroidRemoteViews(rv, GlanceModifier.fillMaxWidth())
                } else Column(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.label, maxLines = 1, style = TextStyle(color = ColorProvider(p.ac), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Text(w.big, style = TextStyle(color = ColorProvider(p.fg), fontSize = SIZES[kind]!!.sp,
                        fontWeight = if (big) FontWeight.Bold else FontWeight.Normal))
                    if (w.sub.isNotEmpty()) Text(w.sub, maxLines = 1, style = TextStyle(color = ColorProvider(p.fg.copy(alpha = .7f)), fontSize = 12.sp))
                }
            }
        }
    }
}

class ClockReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = AuroraWidget("clock") }
class DateReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = AuroraWidget("date") }
class BatteryReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = AuroraWidget("battery") }
class QuoteReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = AuroraWidget("quote") }
class CountdownReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = AuroraWidget("countdown") }
class TodoReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = AuroraWidget("todo") }

data class WDef(val kind: String, val name: String, val desc: String, val receiver: Class<out GlanceAppWidgetReceiver>)
val WIDGETS = listOf(
    WDef("clock", "Minimal Clock", "Live digital clock with weekday and date", ClockReceiver::class.java),
    WDef("date", "Big Date", "Bold day number with month and year", DateReceiver::class.java),
    WDef("battery", "Battery", "Battery level and charging state", BatteryReceiver::class.java),
    WDef("quote", "Daily Quote", "A new motivational quote every day", QuoteReceiver::class.java),
    WDef("countdown", "Countdown", "Days left until your event", CountdownReceiver::class.java),
    WDef("todo", "To-Do", "Your top tasks at a glance", TodoReceiver::class.java))

fun pin(ctx: Context, d: WDef): Boolean {
    val m = AppWidgetManager.getInstance(ctx)
    return m.isRequestPinAppWidgetSupported && m.requestPinAppWidget(ComponentName(ctx, d.receiver), null, null)
}
