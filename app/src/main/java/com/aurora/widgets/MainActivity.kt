package com.aurora.widgets

import android.app.DatePickerDialog
import android.app.WallpaperManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); enableEdgeToEdge(); Store.refresh()
        setContent { AuroraApp() }
    }
}

fun toast(c: Context, s: String) = Toast.makeText(c, s, Toast.LENGTH_SHORT).show()

@Composable fun AuroraApp() {
    val dark = when (Store.int("theme", 0)) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val accent = ACCENTS[Store.int("accent", 0)]
    val scheme = if (dark) darkColorScheme(primary = accent, background = Color(0xFF0E0F13), surface = Color(0xFF17181D),
        surfaceVariant = Color(0xFF22242B), onBackground = Color.White, onSurface = Color.White)
    else lightColorScheme(primary = accent, background = Color(0xFFF6F5F2), surface = Color.White,
        surfaceVariant = Color(0xFFEAE8E3), onBackground = Color(0xFF16171B), onSurface = Color(0xFF16171B))
    MaterialTheme(colorScheme = scheme) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var filter by rememberSaveable { mutableStateOf("All") }
        var wp by remember { mutableStateOf<Wp?>(null) }
        var wd by remember { mutableStateOf<WDef?>(null) }
        BackHandler(wp != null || wd != null) { if (wp != null) wp = null else wd = null }
        val go = { t: Int, f: String -> tab = t; filter = f.ifEmpty { "All" } }
        val openWp = { w: Wp -> wp = w }
        val openW = { d: WDef -> wd = d }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Scaffold(containerColor = Color.Transparent, bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("Home" to Icons.Rounded.Home, "Widgets" to Icons.Rounded.Widgets, "Wallpapers" to Icons.Rounded.Wallpaper, "Style" to Icons.Rounded.Tune)
                        .forEachIndexed { i, (l, ic) -> NavigationBarItem(tab == i, { tab = i }, { Icon(ic, l) }, label = { Text(l, fontSize = 11.sp) }) }
                }
            }) { pad ->
                Box(Modifier.padding(pad)) {
                    Crossfade(tab, label = "tab") { t ->
                        when (t) {
                            0 -> Home(go, openWp, openW)
                            1 -> WidgetsScreen(openW)
                            2 -> WallsScreen(filter, { filter = it }, openWp)
                            else -> StyleScreen(openWp)
                        }
                    }
                }
            }
            wd?.let { d -> Enter { WidgetEditor(d) { wd = null } } }
            wp?.let { w -> Enter { WallpaperScreen(w) { wp = null } } }
        }
    }
}

@Composable fun Enter(content: @Composable () -> Unit) {
    val s = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(s, enter = slideInVertically { it / 6 } + fadeIn()) { content() }
}
@Composable fun Title(t: String, sub: String? = null) = Column(Modifier.statusBarsPadding().padding(24.dp, 20.dp, 24.dp, 8.dp)) {
    Text(t, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
    if (sub != null) Text(sub, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(.6f))
}
@Composable fun Section(t: String, action: (() -> Unit)? = null) = Row(Modifier.fillMaxWidth().padding(24.dp, 22.dp, 24.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(t, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    if (action != null) Text("See all", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, modifier = Modifier.clickable { action() })
}
@Composable fun Label(t: String) = Text(t, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(.6f), modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
@Composable fun Swatches(sel: Int, pick: (Int) -> Unit) = Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    ACCENTS.forEachIndexed { i, c ->
        Box(Modifier.size(36.dp).clip(CircleShape).background(c).border(if (i == sel) 3.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape).clickable { pick(i) })
    }
}

@Composable fun WidgetPreview(kind: String, m: Modifier = Modifier, cfg: Cfg = Store.cfg(kind)) {
    val p = palette(cfg); val w = wContent(kind); val big = kind in listOf("clock", "date", "battery", "countdown")
    Column(m.clip(RoundedCornerShape(26.dp)).background(p.bg).padding(14.dp), verticalArrangement = Arrangement.Center) {
        Text(w.label, color = p.ac, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1)
        Text(w.big, color = p.fg, fontSize = SIZES[kind]!!.sp, fontWeight = if (big) FontWeight.Bold else FontWeight.Normal, lineHeight = (SIZES[kind]!! * 1.15).sp)
        if (w.sub.isNotEmpty()) Text(w.sub, color = p.fg.copy(.7f), fontSize = 12.sp, maxLines = 1)
    }
}

@Composable fun Thumb(w: Wp, m: Modifier, open: (Wp) -> Unit) {
    val fav = w.id in Store.favs()
    Box(m.clip(RoundedCornerShape(22.dp)).clickable { open(w) }) {
        Image(Img.get(w, 360, 780), w.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.6f to Color.Transparent, 1f to Color(0xAA000000))))
        Text(w.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
        Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(34.dp).clip(CircleShape).clickable { Store.toggleFav(w.id) }.padding(7.dp))
    }
}

@Composable fun Home(go: (Int, String) -> Unit, open: (Wp) -> Unit, openW: (WDef) -> Unit) = LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
    item { Title("Aurora", "Widgets & wallpapers, beautifully made.") }
    item { Section("Featured widgets") { go(1, "") } }
    item { LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(WIDGETS) { d -> WidgetPreview(d.kind, Modifier.size(150.dp).clickable { openW(d) }) } } }
    item { Section("Categories") }
    item { LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(CATS) { c ->
            Box(Modifier.size(132.dp, 72.dp).clip(RoundedCornerShape(18.dp)).clickable { go(2, c) }, contentAlignment = Alignment.BottomStart) {
                Image(Img.get(WALLS.first { it.cat == c }, 360, 780), c, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color(0x66000000)))
                Text(c, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
            }
        } } }
    item { Section("Trending wallpapers") { go(2, "") } }
    item { LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(WALLS.filterIndexed { i, _ -> i % 2 == 0 }) { Thumb(it, Modifier.width(132.dp).aspectRatio(.6f), open) } } }
    item { Section("Recently added") }
    item { LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(WALLS.takeLast(8).reversed()) { Thumb(it, Modifier.width(132.dp).aspectRatio(.6f), open) } } }
}

@Composable fun WidgetsScreen(openW: (WDef) -> Unit) = LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
    item { Title("Widgets", "Tap one to customise and add it to your home screen.") }
    items(WIDGETS) { d ->
        Row(Modifier.padding(horizontal = 24.dp, vertical = 7.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface).clickable { openW(d) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            WidgetPreview(d.kind, Modifier.size(120.dp))
            Column(Modifier.padding(start = 16.dp)) {
                Text(d.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(d.desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f), modifier = Modifier.padding(top = 4.dp))
                Text("Customise →", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable fun WidgetEditor(d: WDef, close: () -> Unit) {
    val ctx = LocalContext.current; val k = d.kind; val cfg = Store.cfg(k)
    var alpha by remember { mutableIntStateOf(cfg.alpha) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(close) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text(d.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(250.dp).clip(RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
            Image(Img.get(WALLS[WIDGETS.indexOf(d) * 3 % WALLS.size], 360, 780), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            WidgetPreview(k, Modifier.size(180.dp), cfg.copy(alpha = alpha))
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Label("Style")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Dark", "Light", "Accent").forEachIndexed { i, l -> FilterChip(cfg.style == i, { Store.put("w_${k}_s", i) }, { Text(l) }) }
            }
            Label("Accent colour"); Swatches(cfg.acc) { Store.put("w_${k}_c", it) }
            Label("Opacity · $alpha%")
            Slider(alpha.toFloat(), { alpha = it.toInt() }, valueRange = 20f..100f, onValueChangeFinished = { Store.put("w_${k}_a", alpha) })
            when (k) {
                "countdown" -> {
                    Label("Event")
                    OutlinedTextField(Store.str("cd_name", "Vacation"), { Store.put("cd_name", it.take(18)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedButton({
                        val cur = LocalDate.ofEpochDay(Store.int("cd_day", LocalDate.now().plusDays(30).toEpochDay().toInt()).toLong())
                        DatePickerDialog(ctx, { _, y, m, day -> Store.put("cd_day", LocalDate.of(y, m + 1, day).toEpochDay().toInt()) }, cur.year, cur.monthValue - 1, cur.dayOfMonth).show()
                    }, Modifier.padding(top = 8.dp)) { Text("Pick date") }
                }
                "quote" -> {
                    Label("Quote")
                    OutlinedButton({ Store.put("quote_i", (Store.int("quote_i", LocalDate.now().dayOfYear) + 1) % QUOTES.size) }) { Text("Next quote") }
                }
                "todo" -> {
                    Label("Tasks"); var t by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(t, { t = it }, placeholder = { Text("New task") }, singleLine = true, modifier = Modifier.weight(1f))
                        IconButton({ if (t.isNotBlank()) { Store.put("todos", (Store.todos() + t.trim()).joinToString("\n")); t = "" } }) { Icon(Icons.Rounded.Add, "Add") }
                    }
                    Store.todos().forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("○  $item", Modifier.weight(1f))
                            IconButton({ Store.put("todos", (Store.todos() - item).joinToString("\n")) }) { Icon(Icons.Rounded.Delete, "Delete") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button({ if (!pin(ctx, d)) toast(ctx, "Long-press your home screen → Widgets → Aurora") }, Modifier.fillMaxWidth().height(52.dp)) { Text("Add to home screen") }
        }
    }
}

@Composable fun WallsScreen(filter: String, setFilter: (String) -> Unit, open: (Wp) -> Unit) {
    val favs = Store.favs(); val rec = Store.recents()
    val list = when (filter) {
        "All" -> WALLS; "Favorites" -> WALLS.filter { it.id in favs }
        "Recent" -> rec.mapNotNull { id -> WALLS.find { it.id == id } }; else -> WALLS.filter { it.cat == filter }
    }
    Column {
        Title("Wallpapers", "${WALLS.size} HD & 4K wallpapers, ready offline")
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("All", "Favorites", "Recent") + CATS) { c -> FilterChip(filter == c, { setFilter(c) }, { Text(c) }) }
        }
        if (list.isEmpty()) Text(if (filter == "Recent") "Wallpapers you view will show up here." else "Tap the heart on a wallpaper to save it here.",
            Modifier.padding(32.dp), color = MaterialTheme.colorScheme.onBackground.copy(.6f))
        else LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.id }) { Thumb(it, Modifier.fillMaxWidth().aspectRatio(.6f), open) }
        }
    }
}

@Composable fun WallpaperScreen(w: Wp, close: () -> Unit) {
    val ctx = LocalContext.current; val scope = rememberCoroutineScope()
    var dlg by remember { mutableStateOf(false) }; var busy by remember { mutableStateOf(false) }
    val fav = w.id in Store.favs()
    LaunchedEffect(w.id) { Store.addRecent(w.id) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(Img.get(w, 720, 1560), w.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Row(Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth()) {
            IconButton(close, Modifier.background(Color(0x55000000), CircleShape)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            IconButton({ Store.toggleFav(w.id) }, Modifier.background(Color(0x55000000), CircleShape)) {
                Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = Color.White)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
            .navigationBarsPadding().padding(24.dp)) {
            Text(w.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("${w.cat} · 4K (2160×3840)", color = Color.White.copy(.7f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
            Button({ dlg = true }, Modifier.fillMaxWidth().height(52.dp), enabled = !busy) { Text(if (busy) "Applying…" else "Set wallpaper") }
        }
    }
    if (dlg) AlertDialog(onDismissRequest = { dlg = false }, confirmButton = {}, title = { Text("Apply to") }, text = {
        Column {
            listOf("Home screen" to WallpaperManager.FLAG_SYSTEM, "Lock screen" to WallpaperManager.FLAG_LOCK,
                "Home & lock screen" to (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)).forEach { (l, flags) ->
                TextButton({
                    dlg = false; busy = true
                    scope.launch {
                        val ok = withContext(Dispatchers.Default) {
                            runCatching { val b = Img.render(w, 2160, 3840); WallpaperManager.getInstance(ctx).setBitmap(b, null, true, flags); b.recycle() }.isSuccess
                        }
                        busy = false; toast(ctx, if (ok) "Wallpaper applied" else "Couldn't set wallpaper")
                    }
                }) { Text(l) }
            }
        }
    })
}

@Composable fun StyleScreen(open: (Wp) -> Unit) {
    val saved = Store.favs().mapNotNull { id -> WALLS.find { it.id == id } }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Title("Style", "Make Aurora yours.")
        Column(Modifier.padding(horizontal = 24.dp)) {
            Label("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { i, l -> FilterChip(Store.int("theme", 0) == i, { Store.put("theme", i) }, { Text(l) }) }
            }
            Label("Accent colour"); Swatches(Store.int("accent", 0)) { Store.put("accent", it) }
            Label("Saved collection")
            if (saved.isEmpty()) Text("Tap the heart on any wallpaper to save it here.", color = MaterialTheme.colorScheme.onBackground.copy(.6f))
        }
        if (saved.isNotEmpty()) LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(saved) { Thumb(it, Modifier.width(120.dp).aspectRatio(.6f), open) }
        }
    }
}
