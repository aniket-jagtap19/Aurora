package com.aurora.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.BatteryManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

val ACCENTS = listOf(0xFF7C5CFF, 0xFFFF6B6B, 0xFF2DD4BF, 0xFFFFB020, 0xFF4C8DFF, 0xFFFF5CA8).map { Color(it) }
data class Cfg(val alpha: Int, val acc: Int, val style: Int)
data class P(val bg: Color, val fg: Color, val ac: Color)
data class WC(val label: String, val big: String, val sub: String)

fun palette(c: Cfg): P {
    val a = ACCENTS[c.acc]; val al = c.alpha / 100f
    return when (c.style) {
        0 -> P(Color(0xFF14151A).copy(al), Color.White, a)
        1 -> P(Color.White.copy(al), Color(0xFF16171B), a)
        else -> P(a.copy(al), Color.White, Color.White)
    }
}
val SIZES = mapOf("clock" to 36, "date" to 56, "battery" to 40, "quote" to 14, "countdown" to 52, "todo" to 13)
val QUOTES = listOf(
    "Well begun is half done." to "Aristotle", "Simplicity is the ultimate sophistication." to "Leonardo da Vinci",
    "What we think, we become." to "Buddha", "Do what you can, with what you have, where you are." to "Theodore Roosevelt",
    "Quality is not an act, it is a habit." to "Will Durant", "The best way out is always through." to "Robert Frost",
    "Whether you think you can or can't, you're right." to "Henry Ford")

object Store {
    private lateinit var p: SharedPreferences
    lateinit var app: Context
    var rev by mutableIntStateOf(0)
    fun init(c: Context) { if (!::p.isInitialized) { app = c.applicationContext; p = app.getSharedPreferences("aurora", 0) } }
    fun int(k: String, d: Int): Int { rev; return p.getInt(k, d) }
    fun str(k: String, d: String): String { rev; return p.getString(k, d) ?: d }
    fun put(k: String, v: Any) {
        p.edit().apply { if (v is Int) putInt(k, v) else putString(k, v.toString()) }.apply()
        rev++
        if (k.startsWith("w_") || k.startsWith("cd_") || k == "quote_i" || k == "todos") refresh()
    }
    fun refresh() { CoroutineScope(Dispatchers.Default).launch { WIDGETS.forEach { AuroraWidget(it.kind).updateAll(app) } } }
    fun cfg(k: String): Cfg {
        val i = WIDGETS.indexOfFirst { it.kind == k }
        return Cfg(int("w_${k}_a", 85), int("w_${k}_c", i % 6), int("w_${k}_s", i % 3))
    }
    private fun list(k: String) = str(k, "").split(",").filter { it.isNotEmpty() }
    fun favs() = list("favs")
    fun toggleFav(id: String) { val f = favs(); put("favs", (if (id in f) f - id else f + id).joinToString(",")) }
    fun recents() = list("recents")
    fun addRecent(id: String) { put("recents", (listOf(id) + recents().filter { it != id }).take(12).joinToString(",")) }
    fun todos() = str("todos", "Plan the week\nDrink water\nRead 10 pages").split("\n").filter { it.isNotBlank() }
}

fun wContent(kind: String): WC {
    val now = LocalDateTime.now()
    fun f(p: String) = now.format(DateTimeFormatter.ofPattern(p))
    return when (kind) {
        "clock" -> WC(f("EEEE").uppercase(), f("HH:mm"), f("d MMMM"))
        "date" -> WC(f("EEEE").uppercase(), f("d"), f("MMMM yyyy"))
        "battery" -> {
            val i = Store.app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val pct = i?.let { it.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / it.getIntExtra(BatteryManager.EXTRA_SCALE, 100) } ?: 0
            val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val ch = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            WC(if (ch) "CHARGING" else "BATTERY", "$pct%", if (ch) "Plugged in" else "On battery")
        }
        "quote" -> { val q = QUOTES[Store.int("quote_i", now.dayOfYear) % QUOTES.size]; WC("DAILY QUOTE", "“${q.first}”", "— ${q.second}") }
        "countdown" -> {
            val d = Store.int("cd_day", LocalDate.now().plusDays(30).toEpochDay().toInt()) - LocalDate.now().toEpochDay().toInt()
            WC(Store.str("cd_name", "Vacation").uppercase(), "${abs(d)}", if (d < 0) "days ago" else if (d == 1) "day to go" else "days to go")
        }
        else -> {
            val t = Store.todos()
            WC("TO-DO · ${t.size}", t.take(3).joinToString("\n") { "○  $it" }.ifEmpty { "All done ✓" }, if (t.size > 3) "+${t.size - 3} more" else "")
        }
    }
}

// ---------- Wallpapers (generated offline, rendered at any resolution) ----------
data class Wp(val id: String, val name: String, val cat: String, val kind: Int, val a: Long, val b: Long, val c: Long)
val CATS = listOf("Nature", "Abstract", "Minimal", "AMOLED", "Space", "Architecture", "Aesthetic")
val WALLS = listOf(
    Wp("forest", "Misty Forest", "Nature", 0, 0xFF0B3D2E, 0xFF1F7A5A, 0xFFA8D5BA),
    Wp("dunes", "Golden Dunes", "Nature", 0, 0xFF3B1E08, 0xFFD98A2B, 0xFFFFE0A3),
    Wp("ocean", "Ocean Dusk", "Nature", 0, 0xFF0A1F44, 0xFF2B6CB0, 0xFFF6AD7B),
    Wp("orbit", "Neon Orbit", "Abstract", 3, 0xFF1A0B3B, 0xFF7C3AED, 0xFFEC4899),
    Wp("wave", "Liquid Wave", "Abstract", 3, 0xFF021B3A, 0xFF06B6D4, 0xFF3B82F6),
    Wp("sand", "Paper Sand", "Minimal", 0, 0xFFF3EDE4, 0xFFE5D9C8, 0xFFD2C0A8),
    Wp("mist", "Soft Mist", "Minimal", 0, 0xFFE8EEF5, 0xFFCBD8E8, 0xFFB3C5DD),
    Wp("slate", "Mono Slate", "Minimal", 0, 0xFF2B2F36, 0xFF3C424B, 0xFF59616D),
    Wp("violet", "Violet Glow", "AMOLED", 5, 0, 0, 0xFF6D28D9),
    Wp("ember", "Black Ember", "AMOLED", 5, 0, 0, 0xFFEF4444),
    Wp("teal", "Teal Void", "AMOLED", 5, 0, 0, 0xFF14B8A6),
    Wp("nebula", "Nebula", "Space", 2, 0xFF05010F, 0xFF3B0A6E, 0xFFB5179E),
    Wp("cosmic", "Cosmic Blue", "Space", 2, 0xFF000814, 0xFF001D3D, 0xFF2E6FDB),
    Wp("aurora", "Aurora Sky", "Space", 2, 0xFF02111B, 0xFF0F766E, 0xFF7DF9C9),
    Wp("concrete", "Concrete Lines", "Architecture", 4, 0xFF1C1F24, 0xFF3A4049, 0xFFB8C0CC),
    Wp("glass", "Glass Tower", "Architecture", 4, 0xFF0B2239, 0xFF1E5A8A, 0xFF9ED2F2),
    Wp("peach", "Peach Fizz", "Aesthetic", 0, 0xFFFFC3A0, 0xFFFFAFBD, 0xFFC9A7FF),
    Wp("lavender", "Lavender Haze", "Aesthetic", 0, 0xFF9D8CFF, 0xFFF3A6D9, 0xFFFFE3F1),
    Wp("sunset", "Sunset Cloud", "Aesthetic", 1, 0xFF2D1B69, 0xFFFF6A88, 0xFFFFD1A1))

object Img {
    private val cache = HashMap<String, ImageBitmap>()
    fun get(w: Wp, width: Int, height: Int) = cache.getOrPut("${w.id}$width") { render(w, width, height).asImageBitmap() }
    private fun al(col: Int, a: Int) = (col and 0xFFFFFF) or (a shl 24)
    fun render(wp: Wp, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = java.util.Random(wp.id.hashCode().toLong()); val W = w.toFloat(); val H = h.toFloat()
        val a = wp.a.toInt(); val b = wp.b.toInt(); val cc = wp.c.toInt(); val clamp = Shader.TileMode.CLAMP
        when (wp.kind) {
            1 -> p.shader = RadialGradient(W / 2, H * .8f, H * .9f, intArrayOf(cc, b, a), floatArrayOf(0f, .5f, 1f), clamp)
            3 -> p.shader = LinearGradient(0f, 0f, W * .35f, H, intArrayOf(a, a, b), null, clamp)
            5 -> p.color = 0xFF000000.toInt()
            else -> p.shader = LinearGradient(0f, 0f, W * .35f, H, intArrayOf(a, b, cc), null, clamp)
        }
        c.drawRect(0f, 0f, W, H, p); p.shader = null
        when (wp.kind) {
            2 -> {
                repeat(220) { p.color = al(0xFFFFFF, 60 + r.nextInt(190)); c.drawCircle(r.nextFloat() * W, r.nextFloat() * H, (0.4f + r.nextFloat() * 1.8f) * W / 540f, p) }
                p.shader = RadialGradient(W * .7f, H * .3f, W * .8f, al(cc, 110), al(cc, 0), clamp); c.drawRect(0f, 0f, W, H, p)
            }
            3 -> repeat(6) {
                val cx = r.nextFloat() * W; val cy = r.nextFloat() * H; val rad = W * (.3f + r.nextFloat() * .4f)
                p.shader = RadialGradient(cx, cy, rad, al(if (it % 2 == 0) b else cc, 170), al(b, 0), clamp); c.drawCircle(cx, cy, rad, p)
            }
            4 -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = W / 180
                repeat(16) { i -> p.color = al(cc, if (i % 3 == 0) 70 else 30); val x = W * i / 15f; c.drawLine(x, 0f, x - W * .5f, H, p) }
            }
            5 -> { p.shader = RadialGradient(W / 2, H * 1.02f, H * .55f, al(cc, 170), al(cc, 0), clamp); c.drawRect(0f, 0f, W, H, p) }
        }
        return bmp
    }
}
