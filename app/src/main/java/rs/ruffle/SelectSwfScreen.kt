package rs.ruffle

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSwfScreen(
    onSwfSelected: (Uri) -> Unit,
    onGlobalSettingsClicked: () -> Unit,
    onGameSettingsClicked: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    var folderUriString by remember { mutableStateOf(PreferencesManager.getGameFolderUri(context)) }
    var gameList by remember { mutableStateOf<List<GameFile>>(emptyList()) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                Log.e("SelectSwf", "Permission error", e)
            }
            PreferencesManager.setGameFolderUri(context, uri.toString())
            folderUriString = uri.toString()
        }
    }

    LaunchedEffect(folderUriString) {
        if (folderUriString != null) {
            val uri = Uri.parse(folderUriString)
            gameList = GameUtils.scanGames(context, uri)
        }
    }

	if (showAboutDialog) {
	    AlertDialog(
	        onDismissRequest = { showAboutDialog = false }, // 点外部区域/返回键会触发
	        title = { Text("关于") },
	        text = { Text("🐍煲八") },
	        confirmButton = {
	            TextButton(onClick = { showAboutDialog = false }) {
	                Text("确认")
	            }
	        }
	    )
	}

    Scaffold(
        topBar = {
            TopAppBar(
                //title = { Text("游戏列表") },
		title = {
		    val titleStyle = MaterialTheme.typography.titleLarge
		    val iconSize = with(LocalDensity.current) { titleStyle.fontSize.toPx().toDp() } // 与字体等高

		    Row(verticalAlignment = Alignment.CenterVertically) {
		        Image(
		            painter = painterResource(id = R.drawable.list),
		            contentDescription = null,
		            modifier = Modifier.size(iconSize)
		        )
		        Spacer(modifier = Modifier.width(8.dp))
		        Text("游戏列表", style = titleStyle)
		    }
		},
				
                actions = {
		    IconButton(onClick = { showAboutDialog = true }) {
		        Icon(Icons.Filled.Info, contentDescription = "关于")
		    }
                    IconButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "切换文件夹")
                    }
                    IconButton(onClick = onGlobalSettingsClicked) {
                        Icon(Icons.Filled.Settings, contentDescription = "全局按键设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (folderUriString == null) {
                EmptyStateView(onSelectFolder = { folderPicker.launch(null) })
            } else {
                GameListView(
                    games = gameList,
                    currentPath = folderUriString!!,
                    onGameClick = onSwfSelected,
                    onGameLongClick = onGameSettingsClicked
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(onSelectFolder: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
	Image(
	    painter = painterResource(id = R.drawable.favicon),
	    contentDescription = null,
	    modifier = Modifier.clip(RoundedCornerShape(25.dp)) // 圆角半径可调
	)
	Spacer(modifier = Modifier.height(12.dp))
    
        Text("尚未设置游戏目录", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSelectFolder) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择游戏文件夹")
        }
    }
}

@Composable
fun GameListView(
    games: List<GameFile>,
    currentPath: String,
    onGameClick: (Uri) -> Unit,
    onGameLongClick: (Uri, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val displayPath = Uri.decode(currentPath)
        Text(
            text = "路径: $displayPath",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
        )

        if (games.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("该文件夹下没有找到 SWF 文件")
            }
        } else {
            LazyColumn {
                items(games) { game ->
                    GameListItem(
                        game = game, 
                        onClick = { onGameClick(game.uri) },
                        onLongClick = { onGameLongClick(game.uri, game.name) }
                    )
                    Divider(thickness = 0.5.dp, color = Color.LightGray)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameListItem(
    game: GameFile, 
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = game.name, style = MaterialTheme.typography.titleMedium)
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("设置按键映射 (局部)") },
                onClick = {
                    showMenu = false
                    onLongClick()
                }
            )
        }
    }
}