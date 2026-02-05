package com.example.idledisplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi // Fixed missing import
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack // Now works with Extended Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.palette.graphics.Palette // Fixed missing import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        setContent { IdleDisplayScreen() }
    }

    override fun onResume() {
        super.onResume()
        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }
}

val PixelFont = FontFamily(Font(R.font.pixelify_sans))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdleDisplayScreen() {
    val context = LocalContext.current
    val mediaState by MediaRepo.mediaState.collectAsState()
    val notificationState by MediaRepo.notificationState.collectAsState()

    // State
    var showSettings by remember { mutableStateOf(false) }
    var appSettings by remember { mutableStateOf(AppSettings()) }
    var batteryLevel by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(Date()) }
    var ambientColor by remember { mutableStateOf(Color(0xFF121212)) }

    // Burn-in
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Progress
    var currentProgress by remember { mutableFloatStateOf(0f) }

    // Source App Icon
    var sourceAppIcon by remember { mutableStateOf<Bitmap?>(null) }

    // --- Logic ---
    LaunchedEffect(mediaState.packageName) {
        if (mediaState.packageName.isNotEmpty()) {
            launch(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val drawable = pm.getApplicationIcon(mediaState.packageName)
                    sourceAppIcon = drawable.toBitmap()
                } catch (_: Exception) { sourceAppIcon = null }
            }
        } else { sourceAppIcon = null }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF0F2027),
        targetValue = Color(0xFF2C5364),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "idleBreath"
    )

    val targetBgColor = if (mediaState.isPlaying) ambientColor else breathingColor
    val animatedBgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(2000), label = "bgColor")

    val clockSize by animateFloatAsState(
        targetValue = if (mediaState.isPlaying) 80f else 130f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "clockSize"
    )

    // Loops
    LaunchedEffect(mediaState) {
        while (true) {
            if (mediaState.isPlaying && mediaState.duration > 0) {
                val timeDiff = System.currentTimeMillis() - mediaState.lastUpdate
                val projectedPosition = mediaState.position + timeDiff
                currentProgress = (projectedPosition.toFloat() / mediaState.duration.toFloat()).coerceIn(0f, 1f)
            }
            delay(33L)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                batteryLevel = (level * 100 / scale.toFloat()).toInt()
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            if (appSettings.burnInProtection) {
                offsetX = (-5..5).random().toFloat()
                offsetY = (-5..5).random().toFloat()
            } else {
                offsetX = 0f; offsetY = 0f
            }
            delay(1000L * 60)
        }
    }

    LaunchedEffect(mediaState.albumArt) {
        if (mediaState.albumArt != null) {
            launch(Dispatchers.Default) {
                Palette.from(mediaState.albumArt!!).generate { palette ->
                    val colorInt = palette?.getLightVibrantColor(palette.getDominantColor(0xFF121212.toInt())) ?: 0xFF121212.toInt()
                    ambientColor = Color(colorInt)
                }
            }
        } else { ambientColor = Color(0xFF121212) }
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedBgColor.copy(alpha=0.6f), Color.Black)))
    ) {
        val blurRadius by animateDpAsState(if (notificationState != null) 20.dp else 0.dp, label = "blur")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .offset(x = offsetX.dp, y = offsetY.dp)
                .blur(blurRadius)
        ) {
            // Icons
            Box(
                modifier = Modifier.align(Alignment.TopStart).clip(CircleShape).background(Color.Black.copy(0.4f)).clickable { showSettings = true }.padding(12.dp)
            ) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }

            if (appSettings.showBattery) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(50)).background(Color.Black.copy(0.4f)).padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$batteryLevel%", fontFamily = PixelFont, color = if (isCharging) Color.Green else Color.White, fontSize = 16.sp)
                        if (isCharging) Text(" ⚡", fontSize = 14.sp, color = Color.Green)
                    }
                }
            }

            // Center Content (Move Transition)
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessLow)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = mediaState.isPlaying,
                    enter = expandVertically(tween(800)) + fadeIn(tween(500, delayMillis = 600)),
                    exit = shrinkVertically(tween(800)) + fadeOut(tween(300))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(310.dp), color = Color.White.copy(0.1f), strokeWidth = 6.dp)
                            CircularProgressIndicator(progress = { currentProgress }, modifier = Modifier.size(310.dp), color = animatedBgColor, strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
                            Card(
                                shape = RoundedCornerShape(1000.dp),
                                elevation = CardDefaults.cardElevation(12.dp),
                                modifier = Modifier.size(280.dp).clickable {
                                    if (mediaState.packageName.isNotEmpty()) {
                                        try {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(mediaState.packageName)
                                            if (launchIntent != null) context.startActivity(launchIntent)
                                        } catch (_: Exception) { Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            ) {
                                Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
                                    if (mediaState.albumArt != null) {
                                        Image(bitmap = mediaState.albumArt!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(40.dp))
                    }
                }

                // Shared Clock
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentTime),
                    fontFamily = PixelFont,
                    fontSize = clockSize.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    lineHeight = clockSize.sp,
                    textAlign = TextAlign.Center
                )

                // Date (Idle)
                AnimatedVisibility(
                    visible = !mediaState.isPlaying,
                    enter = fadeIn(tween(1000)) + expandVertically(),
                    exit = fadeOut(tween(500)) + shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Text(SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(currentTime).uppercase(), fontFamily = PixelFont, fontSize = 22.sp, color = Color.White.copy(0.7f), letterSpacing = 4.sp, textAlign = TextAlign.Center)
                    }
                }

                // Metadata (Playing)
                AnimatedVisibility(
                    visible = mediaState.isPlaying,
                    enter = fadeIn(tween(500, delayMillis = 600)) + expandVertically(tween(500, delayMillis = 600)),
                    exit = fadeOut(tween(300)) + shrinkVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(20.dp))
                        Text(mediaState.title, fontFamily = PixelFont, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.fillMaxWidth(0.9f).basicMarquee(), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(mediaState.artist, fontFamily = PixelFont, fontSize = 18.sp, color = Color.White.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)

                        Spacer(Modifier.height(20.dp))

                        // Source Icon
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    if (mediaState.packageName.isNotEmpty()) {
                                        try {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(mediaState.packageName)
                                            if (launchIntent != null) context.startActivity(launchIntent)
                                        } catch (_: Exception) {}
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            if (sourceAppIcon != null) {
                                Image(
                                    bitmap = sourceAppIcon!!.asImageBitmap(),
                                    contentDescription = "Source",
                                    modifier = Modifier.size(32.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                            } else {
                                Icon(Icons.Default.Audiotrack, "Audio", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }

        // Settings
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Settings", fontFamily = PixelFont) },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(appSettings.burnInProtection, { appSettings = appSettings.copy(burnInProtection = it) })
                            Text("Burn-in Protection")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(appSettings.showBattery, { appSettings = appSettings.copy(showBattery = it) })
                            Text("Show Battery")
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done") } }
            )
        }

        // Bottom Drawer
        AnimatedVisibility(
            visible = notificationState != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val notification = notificationState ?: return@AnimatedVisibility
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color.Black.copy(0.95f)).padding(32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (notification.icon != null) {
                        Image(bitmap = notification.icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(notification.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Text(notification.text, color = Color.LightGray, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// Inline Helper
fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return bitmap
    val bitmap = Bitmap.createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}