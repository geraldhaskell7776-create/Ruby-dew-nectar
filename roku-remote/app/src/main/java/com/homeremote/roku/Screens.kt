package com.homeremote.roku

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ButtonColor = Color(0xD9203047)
private val PadColor = Color(0xB3152238)
private val PowerColor = Color(0xFFD9434B)

@Composable
fun RemoteApp(vm: RemoteViewModel) {
    val device = vm.device
    if (device == null) {
        ChristmasBackground {
            Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface) { padding ->
                SetupScreen(vm, Modifier.padding(padding))
            }
        }
        return
    }
    var tab by rememberSaveable { mutableStateOf(0) }
    ChristmasBackground {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.SettingsRemote, null) },
                    label = { Text("Remote") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Apps, null) },
                    label = { Text("Channels") },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            DeviceBar(device, onChange = vm::changeRoku)
            ChristmasLights()
            vm.problem?.let { ProblemBanner(it) }
            when (tab) {
                0 -> RemoteScreen(vm)
                else -> ChannelsScreen(vm, onLaunched = { tab = 0 })
            }
        }
    }
    }
}

@Composable
private fun DeviceBar(device: RokuDevice, onChange: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Tv, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                device.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(device.host, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onChange) { Text("Change") }
    }
}

@Composable
private fun ProblemBanner(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.error,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
            .padding(12.dp),
    )
}

// ---------------------------------------------------------------- Remote

@Composable
private fun RemoteScreen(vm: RemoteViewModel) {
    var showKeyboard by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (showKeyboard) TypingBox(vm)

        ButtonRow {
            IconKey(Icons.Filled.PowerSettingsNew, "Power", color = PowerColor) { vm.press("Power") }
            IconKey(Icons.AutoMirrored.Filled.ArrowBack, "Back") { vm.press("Back") }
            IconKey(Icons.Filled.Home, "Home") { vm.press("Home") }
        }

        DPad(vm)

        ButtonRow {
            IconKey(Icons.Filled.Replay, "Instant replay") { vm.press("InstantReplay") }
            IconKey(Icons.Filled.Star, "Options") { vm.press("Info") }
            IconKey(
                Icons.Filled.Keyboard,
                "Keyboard",
                color = if (showKeyboard) RokuPurple else ButtonColor,
            ) { showKeyboard = !showKeyboard }
        }

        ButtonRow {
            IconKey(Icons.Filled.FastRewind, "Rewind") { vm.press("Rev") }
            IconKey(Icons.Filled.PlayArrow, "Play or pause", color = ChristmasGreen) { vm.press("Play") }
            IconKey(Icons.Filled.FastForward, "Fast forward") { vm.press("Fwd") }
        }

        ButtonRow {
            IconKey(Icons.AutoMirrored.Filled.VolumeDown, "Volume down", repeat = true) { vm.press("VolumeDown") }
            IconKey(Icons.AutoMirrored.Filled.VolumeOff, "Mute") { vm.press("VolumeMute") }
            IconKey(Icons.AutoMirrored.Filled.VolumeUp, "Volume up", repeat = true) { vm.press("VolumeUp") }
        }

        Text(
            "Tip: your phone's volume buttons control the TV too.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ButtonRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun DPad(vm: RemoteViewModel) {
    Box(
        Modifier
            .size(250.dp)
            .clip(CircleShape)
            .background(PadColor),
    ) {
        val arrow = Color.Transparent
        IconKey(Icons.Filled.KeyboardArrowUp, "Up", Modifier.align(Alignment.TopCenter), size = 80.dp, color = arrow, repeat = true) { vm.press("Up") }
        IconKey(Icons.Filled.KeyboardArrowDown, "Down", Modifier.align(Alignment.BottomCenter), size = 80.dp, color = arrow, repeat = true) { vm.press("Down") }
        IconKey(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", Modifier.align(Alignment.CenterStart), size = 80.dp, color = arrow, repeat = true) { vm.press("Left") }
        IconKey(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", Modifier.align(Alignment.CenterEnd), size = 80.dp, color = arrow, repeat = true) { vm.press("Right") }
        RemoteKey(onPress = { vm.press("Select") }, modifier = Modifier.align(Alignment.Center), size = 90.dp, color = RokuPurple) {
            Text("OK", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

/** Letters you type here appear in the search box on the TV, one by one. */
@Composable
private fun TypingBox(vm: RemoteViewModel) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            val kept = text.commonPrefixWith(new).length
            repeat(text.length - kept) { vm.press("Backspace") }
            new.substring(kept).forEach(vm::type)
            text = new
        },
        label = { Text("Type on the TV") },
        placeholder = { Text("Open a search box on the TV first") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { vm.press("Enter") }),
        trailingIcon = {
            if (text.isNotEmpty()) {
                TextButton(onClick = {
                    repeat(text.length) { vm.press("Backspace") }
                    text = ""
                }) { Text("Clear") }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
    )
}

@Composable
private fun IconKey(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    color: Color = ButtonColor,
    repeat: Boolean = false,
    onPress: () -> Unit,
) {
    RemoteKey(onPress, modifier, size, color, CircleShape, repeat) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(size * 0.45f))
    }
}

/**
 * A remote button. A tap sends the key once. When [repeat] is on, holding it
 * keeps sending the key (handy for scrolling lists or volume).
 * Keys fire when your finger lifts, so scrolling the screen never presses anything.
 */
@Composable
private fun RemoteKey(
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    color: Color = ButtonColor,
    shape: Shape = CircleShape,
    repeat: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val press by rememberUpdatedState(onPress)
    var pressed by remember { mutableStateOf(false) }
    val tap = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        press()
    }
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (pressed) Color.White.copy(alpha = 0.18f) else color)
            .pointerInput(repeat) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    var repeated = false
                    val holding = if (repeat) {
                        scope.launch {
                            delay(400)
                            repeated = true
                            while (true) {
                                tap()
                                delay(110)
                            }
                        }
                    } else {
                        null
                    }
                    val up = waitForUpOrCancellation()
                    holding?.cancel()
                    pressed = false
                    if (up != null && !repeated) tap()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}

// ---------------------------------------------------------------- Channels

@Composable
private fun ChannelsScreen(vm: RemoteViewModel, onLaunched: () -> Unit) {
    val view = LocalView.current
    if (vm.apps.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("No channels loaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = vm::loadApps) { Text("Load channels") }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(vm.apps, key = { it.id }) { app ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    vm.launch(app)
                    onLaunched()
                },
            ) {
                val icon = vm.icons[app.id]
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = app.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(app.name, textAlign = TextAlign.Center, fontSize = 13.sp, maxLines = 3)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Setup

@Composable
private fun SetupScreen(vm: RemoteViewModel, modifier: Modifier = Modifier) {
    var ip by rememberSaveable { mutableStateOf("") }
    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Find your Roku", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Make sure this phone is on the same Wi-Fi as the Roku.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (vm.scanning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.width(12.dp))
                Text("Searching your Wi-Fi…")
            }
        }

        for (roku in vm.found) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.use(roku) },
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Tv, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(roku.name, fontWeight = FontWeight.SemiBold)
                        Text(roku.host, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Connect", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        OutlinedButton(onClick = vm::scan, enabled = !vm.scanning) { Text("Search again") }

        vm.problem?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        HorizontalDivider(Modifier.padding(vertical = 6.dp))

        Text("Or type its IP address", fontWeight = FontWeight.SemiBold)
        Text(
            "On the Roku: Settings > Network > About. It looks like 192.168.1.42.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("IP address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.connectTo(ip) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { vm.connectTo(ip) }, enabled = ip.isNotBlank() && !vm.connecting) {
            Text(if (vm.connecting) "Connecting…" else "Connect")
        }

        Text(
            "Not connecting? On the Roku turn on Settings > System > Advanced system settings > " +
                "Control by mobile apps.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
