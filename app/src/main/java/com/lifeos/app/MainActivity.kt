package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch

private val Ink = Color(0xFF090A12)
private val Panel = Color(0xFF171927)
private val Violet = Color(0xFF9B7BFF)
private val Cyan = Color(0xFF55D6D0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = LifeDatabase.get(this).entryDao()
        val health = HealthReader(this)
        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            Crossfade(targetState = showSplash, label = "splash") { splash ->
                if (splash) SplashScreen { showSplash = false } else LifeOSApp(dao, health)
            }
        }
    }
}

@Composable
fun LifeOSApp(dao: EntryDao, health: HealthReader) {
    val entries by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("Bugün") }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("TASK") }
    var snapshot by remember { mutableStateOf(HealthSnapshot()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { scope.launch { snapshot = health.readToday() } }

    LaunchedEffect(Unit) { snapshot = health.readToday() }
    val xp = entries.filter { it.done }.sumOf { it.xp }
    val level = xp / 100 + 1
    val tasks = entries.filter { it.kind == "TASK" || it.kind == "PROGRAM" }
    val rituals = entries.filter { it.kind == "RITUAL" }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet, secondary = Cyan,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Bugün", "Program", "Ritüel", "Sağlık", "Profil").forEach { name ->
                    NavigationBarItem(selected = tab == name, onClick = { tab = name },
                        icon = { Icon(when(name) {
                            "Bugün" -> Icons.Default.Dashboard
                            "Program" -> Icons.Default.Flag
                            "Ritüel" -> Icons.Default.AutoAwesome
                            "Sağlık" -> Icons.Default.Favorite
                            else -> Icons.Default.Person
                        }, contentDescription = name) }, label = { Text(name) })
                }
            }
        }, floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = Violet, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        }) { padding ->
            when(tab) {
                "Bugün" -> Dashboard(entries, xp, level, snapshot, padding, onDone = { item ->
                    scope.launch { dao.update(item.copy(done = !item.done)) }
                })
                "Program" -> EntryList("Programların", entries.filter { it.kind == "PROGRAM" }, padding) { item ->
                    scope.launch { dao.update(item.copy(done = !item.done)) }
                }
                "Ritüel" -> EntryList("Günlük ritüeller", rituals, padding) { item ->
                    scope.launch { dao.update(item.copy(done = !item.done)) }
                }
                "Sağlık" -> HealthScreen(snapshot, padding, onConnect = {
                    if (health.available()) permissionLauncher.launch(health.requiredPermissions())
                    else snapshot = HealthSnapshot(status = "Health Connect uygulamasını Play Store'dan yükle veya güncelle")
                }, onRefresh = { scope.launch { snapshot = health.readToday() } })
                else -> ProfileScreen(xp, level, entries.count { it.done }, padding)
            }
        }
        if(showAdd) AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Yeni kayıt oluştur") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Başlık") })
                    OutlinedTextField(value = detail, onValueChange = { detail = it }, label = { Text("Açıklama / hedef") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TASK", "PROGRAM", "RITUAL").forEach { value ->
                            FilterChip(selected = kind == value, onClick = { kind = value }, label = { Text(when(value) { "TASK" -> "Görev"; "PROGRAM" -> "Program"; else -> "Ritüel" }) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                if(title.isNotBlank()) scope.launch {
                    dao.insert(Entry(title = title.trim(), detail = detail.trim(), kind = kind, xp = if(kind == "RITUAL") 15 else 10))
                    title = ""; detail = ""; showAdd = false
                }
            }) { Text("Oluştur") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } }
        )
    }
}

@Composable
fun Dashboard(entries: List<Entry>, xp: Int, level: Int, health: HealthSnapshot, padding: PaddingValues, onDone: (Entry)->Unit) {
    val todayTasks = entries.filter { it.kind != "RITUAL" }
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("LIFEOS", style = MaterialTheme.typography.labelLarge, color = Cyan, fontWeight = FontWeight.Bold)
            Text("Kontrol sende.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Bugün küçük adımlar, uzun vadede büyük değişim.", color = Color.LightGray)
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF292044), Color(0xFF172A39)))).padding(20.dp)) {
                    Text("GELİŞİM SEVİYESİ", color = Cyan)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("LVL $level", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(10.dp))
                        Text("$xp XP toplam", color = Color.LightGray)
                    }
                    LinearProgressIndicator(progress = { (xp % 100) / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), color = Violet)
                    Text("${100 - (xp % 100)} XP sonraki seviyeye", color = Color.LightGray, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("ADIM", health.steps.toString(), Modifier.weight(1f))
                Metric("AKTİF KCAL", health.activeKcal.toInt().toString(), Modifier.weight(1f))
                Metric("ALINAN KCAL", health.eatenKcal.toInt().toString(), Modifier.weight(1f))
            }
        }
        item { Text("Günün görevleri", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if(todayTasks.isEmpty()) item { Text("Henüz görev yok. + düğmesiyle ilk hedefini oluştur.", color = Color.LightGray) }
        items(todayTasks, key = { it.id }) { item -> EntryCard(item, onDone) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EntryList(title: String, entries: List<Entry>, padding: PaddingValues, onDone: (Entry)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        if(entries.isEmpty()) item { Text("Henüz kayıt yok. Sağ alttaki + ile oluştur.", color = Color.LightGray) }
        items(entries, key = { it.id }) { EntryCard(it, onDone) }
    }
}

@Composable
fun EntryCard(entry: Entry, onDone: (Entry)->Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = entry.done, onCheckedChange = { onDone(entry) }, colors = CheckboxDefaults.colors(checkedColor = Cyan))
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.SemiBold)
                if(entry.detail.isNotBlank()) Text(entry.detail, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
            Text("+${entry.xp} XP", color = Violet, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HealthScreen(snapshot: HealthSnapshot, padding: PaddingValues, onConnect: ()->Unit, onRefresh: ()->Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Sağlık merkezi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(snapshot.status, color = Cyan)
        Metric("BUGÜNKÜ ADIM", snapshot.steps.toString())
        Metric("AKTİF KALORİ", "${snapshot.activeKcal.toInt()} kcal")
        Metric("BESİN KAYDI ENERJİSİ", "${snapshot.eatenKcal.toInt()} kcal")
        Text("Veriler Health Connect üzerinden okunur. Samsung Health uygulamasında ilgili veri paylaşımını ayrıca açman gerekir.", color = Color.LightGray)
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text("Health Connect izinlerini ver") }
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Verileri yenile") }
    }
}

@Composable
fun ProfileScreen(xp: Int, level: Int, completed: Int, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Karakterin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Seviye $level", style = MaterialTheme.typography.headlineLarge, color = Violet)
        Metric("TOPLAM XP", xp.toString())
        Metric("TAMAMLANAN KAYIT", completed.toString())
        Text("İlerleme verilerin bu cihazdaki yerel Room veritabanında saklanır.", color = Color.LightGray)
    }
}
