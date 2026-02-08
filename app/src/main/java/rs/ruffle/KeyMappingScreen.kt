package rs.ruffle

import android.net.Uri
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyMappingScreen(
    navController: NavController,
    targetGameUri: String? = null,
    targetGameName: String? = null
) {
    val context = LocalContext.current

    // 关键：route 里 uri 是 Uri.encode 过的，这里解码后作为本地配置 key，避免“存/取不一致”
    val normalizedGameUri = remember(targetGameUri) { targetGameUri?.let { Uri.decode(it) } }
    val isGlobalMode = normalizedGameUri == null

    // 用 state 托管，避免“改了 map 但 UI 不刷新”还要强行 navigate 刷新的情况
    var globalMapping by remember { mutableStateOf(PreferencesManager.getGlobalMapping(context)) }

    var localMapping by remember {
        mutableStateOf(
            if (!isGlobalMode) PreferencesManager.getGameMapping(context, normalizedGameUri!!)
            else mutableMapOf()
        )
    }

    var editingKey by remember { mutableStateOf<VirtualKey?>(null) }
    val title = if (isGlobalMode) "全局按键设置" else "游戏设置: $targetGameName"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    Button(onClick = { navController.popBackStack() }) { Text("返回") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            // [核心修改] 添加 key = { it.keyCode }
            // 指定 key 后，当 globalMapping 更新触发重组时，Compose 能识别出列表项没变，
            // 从而保留滚动位置，不会出现“刷新一下回到顶部”的现象。
            items(
                items = VirtualKeys.ALL_KEYS,
                key = { it.keyCode }
            ) { virtualKey ->
                var displayCode: Int? = null
                var isLocalDefined = false

                if (isGlobalMode) {
                    displayCode = globalMapping.entries.find { it.value == virtualKey.keyCode }?.key
                    isLocalDefined = displayCode != null
                } else {
                    val localPhys = localMapping.entries.find { it.value == virtualKey.keyCode }?.key
                    if (localPhys != null) {
                        displayCode = localPhys
                        isLocalDefined = true
                    } else {
                        val globalPhys = globalMapping.entries.find { it.value == virtualKey.keyCode }?.key
                        displayCode = globalPhys
                        isLocalDefined = false
                    }
                }

                KeyItem(
                    virtualKey = virtualKey,
                    assignedCode = displayCode,
                    isLocal = isLocalDefined,
                    isGlobalMode = isGlobalMode,
                    onClick = { editingKey = virtualKey }
                )
                Divider()
            }
        }
    }

    if (editingKey != null) {
        KeyListenDialog(
            editingKey = editingKey!!,
            onDismiss = { editingKey = null },
            onKeyPressed = { physCode ->
                if (isGlobalMode) {
                    val newMap = globalMapping.toMutableMap()
                    updateMapping(newMap, physCode, editingKey!!.keyCode)
                    globalMapping = newMap
                    PreferencesManager.saveGlobalMapping(context, newMap)
                } else {
                    val newMap = localMapping.toMutableMap()
                    updateMapping(newMap, physCode, editingKey!!.keyCode)
                    localMapping = newMap
                    PreferencesManager.saveGameMapping(context, normalizedGameUri!!, newMap)
                }
                editingKey = null
            },
            onClear = {
                if (isGlobalMode) {
                    val newMap = globalMapping.toMutableMap()
                    removeFromMapping(newMap, editingKey!!.keyCode)
                    globalMapping = newMap
                    PreferencesManager.saveGlobalMapping(context, newMap)
                } else {
                    val newMap = localMapping.toMutableMap()
                    removeFromMapping(newMap, editingKey!!.keyCode)
                    localMapping = newMap
                    PreferencesManager.saveGameMapping(context, normalizedGameUri!!, newMap)
                }
                editingKey = null
            }
        )
    }
}

// Map 更新逻辑：保证 1 对 1
fun updateMapping(map: MutableMap<Int, Int>, physCode: Int, virtCode: Int) {
    map.remove(physCode)
    val oldPhys = map.entries.find { it.value == virtCode }?.key
    if (oldPhys != null) map.remove(oldPhys)
    map[physCode] = virtCode
}

fun removeFromMapping(map: MutableMap<Int, Int>, virtCode: Int) {
    val oldPhys = map.entries.find { it.value == virtCode }?.key
    if (oldPhys != null) map.remove(oldPhys)
}

@Composable
fun KeyItem(
    virtualKey: VirtualKey,
    assignedCode: Int?,
    isLocal: Boolean,
    isGlobalMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = virtualKey.name, style = MaterialTheme.typography.bodyLarge)

        val statusText = when {
            assignedCode == null -> "未设置"
            isGlobalMode -> "物理键: $assignedCode"
            isLocal -> "物理键: $assignedCode (局部)"
            else -> "物理键: $assignedCode (全局)"
        }

        val statusColor = when {
            assignedCode == null -> Color.Gray
            isGlobalMode -> MaterialTheme.colorScheme.primary
            isLocal -> MaterialTheme.colorScheme.tertiary
            else -> Color.DarkGray
        }

        Text(text = statusText, color = statusColor)
    }
}

@Composable
fun KeyListenDialog(
    editingKey: VirtualKey,
    onDismiss: () -> Unit,
    onKeyPressed: (Int) -> Unit,
    onClear: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                .padding(24.dp)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { e ->
                    val ne = e.nativeKeyEvent

                    val isGamepad =
                        ne.isFromSource(InputDevice.SOURCE_GAMEPAD) ||
                        ne.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
                        ne.isFromSource(InputDevice.SOURCE_DPAD)

                    // ne.action 是 Int 类型，KeyEvent.ACTION_DOWN 也是 Int 类型
                    if (ne.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent true
                    if (ne.repeatCount > 0) return@onPreviewKeyEvent true

                    var code = ne.keyCode
                    val scan = ne.scanCode

                    // B 键经常被系统映射成 BACK，映射模式下把它当 BUTTON_B
                    if (isGamepad && code == KeyEvent.KEYCODE_BACK) {
                        code = KeyEvent.KEYCODE_BUTTON_B
                    }

                    // 过滤 UI 导航合成键（避免 A/Y 在局部录成 23/62）
                    if (isGamepad && scan == 0 &&
                        (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                         code == KeyEvent.KEYCODE_ENTER ||
                         code == KeyEvent.KEYCODE_SPACE)
                    ) {
                        return@onPreviewKeyEvent true
                    }

                    if (code != KeyEvent.KEYCODE_UNKNOWN) {
                        onKeyPressed(code)
                    }
                    true
                }
                .focusable()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("请按下要映射到: ${editingKey.name} 的物理按键")
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onClear) { Text("清除") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
