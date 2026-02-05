package rs.ruffle

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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyMappingScreen(
    navController: NavController,
    targetGameUri: String? = null,
    targetGameName: String? = null
) {
    val context = LocalContext.current
    val isGlobalMode = targetGameUri == null
    
    // 读取数据
    // 1. 全局配置 (总是需要读取，用于回退显示)
    val globalMapping = remember { PreferencesManager.getGlobalMapping(context) }
    
    // 2. 局部配置 (如果存在)
    var localMapping by remember { 
        mutableStateOf(
            if (!isGlobalMode) PreferencesManager.getGameMapping(context, targetGameUri!!) 
            else mutableMapOf()
        )
    }

    // 状态：当前正在编辑的配置（用于 UI 更新）
    // 如果是全局模式，我们操作 globalMapping (此时 localMapping 为空)
    // 如果是局部模式，我们操作 localMapping
    
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
            items(VirtualKeys.ALL_KEYS) { virtualKey ->
                
                // === 逻辑核心：计算显示的物理键 ===
                var displayCode: Int? = null
                var isLocalDefined = false

                if (isGlobalMode) {
                    // 全局模式：直接查全局表
                    displayCode = globalMapping.entries.find { it.value == virtualKey.keyCode }?.key
                    isLocalDefined = true // 在全局模式下，只要有值就算"Defined"
                } else {
                    // 局部模式：
                    // 1. 先查局部表
                    val localPhys = localMapping.entries.find { it.value == virtualKey.keyCode }?.key
                    if (localPhys != null) {
                        displayCode = localPhys
                        isLocalDefined = true
                    } else {
                        // 2. 局部没有，查全局表 (Inherit)
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
            targetKey = editingKey!!,
            onDismiss = { editingKey = null },
            onKeyPressed = { physCode ->
                if (isGlobalMode) {
                    // === 全局模式保存 ===
                    updateMapping(globalMapping, physCode, editingKey!!.keyCode)
                    PreferencesManager.saveGlobalMapping(context, globalMapping)
                    // 强制刷新界面 (Compose有时候对Map内部变动监听不灵敏，这里通过重新读取简单处理，或者由于remember是引用类型，只需触发重绘)
                    navController.navigate(Routes.buildKeyMappingRoute()) { popUpTo(Routes.KEY_MAPPING_ROUTE) { inclusive = true } } 
                } else {
                    // === 局部模式保存 ===
                    val newMap = localMapping.toMutableMap()
                    updateMapping(newMap, physCode, editingKey!!.keyCode)
                    localMapping = newMap
                    PreferencesManager.saveGameMapping(context, targetGameUri!!, newMap)
                }
                editingKey = null
            },
            onClear = {
                if (isGlobalMode) {
                    // 全局模式清除 -> 变为空
                    removeFromMapping(globalMapping, editingKey!!.keyCode)
                    PreferencesManager.saveGlobalMapping(context, globalMapping)
                    navController.navigate(Routes.buildKeyMappingRoute()) { popUpTo(Routes.KEY_MAPPING_ROUTE) { inclusive = true } }
                } else {
                    // 局部模式清除 -> 变为继承全局
                    val newMap = localMapping.toMutableMap()
                    removeFromMapping(newMap, editingKey!!.keyCode)
                    localMapping = newMap
                    PreferencesManager.saveGameMapping(context, targetGameUri!!, newMap)
                }
                editingKey = null
            }
        )
    }
}

// 辅助：Map更新逻辑
fun updateMapping(map: MutableMap<Int, Int>, physCode: Int, virtCode: Int) {
    // 1. 如果这个物理键之前绑定了其他功能，移除
    map.remove(physCode)
    // 2. 如果这个虚拟键之前被其他物理键绑定，移除 (保证一对一)
    val oldPhys = map.entries.find { it.value == virtCode }?.key
    if (oldPhys != null) map.remove(oldPhys)
    // 3. 绑定
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
    isLocal: Boolean, // 如果是局部覆盖，或者全局模式下的已设置
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
            isLocal -> MaterialTheme.colorScheme.tertiary // 局部高亮
            else -> Color.DarkGray // 继承显示的颜色
        }

        Text(text = statusText, color = statusColor)
    }
}

@Composable
fun KeyListenDialog(
    targetKey: VirtualKey,
    onDismiss: () -> Unit,
    onKeyPressed: (Int) -> Unit,
    onClear: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                .padding(24.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val code = keyEvent.nativeKeyEvent.keyCode
                        if (code != KeyEvent.KEYCODE_BACK) {
                            onKeyPressed(code)
                            return@onKeyEvent true
                        }
                    }
                    false
                }
                .focusable()
        ) {
            LaunchedEffect(Unit) {} // Request Focus

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("正在设置: ${targetKey.name}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("请按下手柄上的物理按键...", color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    Button(onClick = onClear) { Text("清除 / 复位") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }
}