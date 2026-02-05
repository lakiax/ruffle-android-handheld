package rs.ruffle

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val SELECT_SWF = "select_swf"
    const val KEY_MAPPING_ROUTE = "key_mapping?uri={uri}&name={name}"
    
    fun buildKeyMappingRoute(uri: String? = null, name: String? = null): String {
        return if (uri != null && name != null) {
            "key_mapping?uri=${Uri.encode(uri)}&name=${Uri.encode(name)}"
        } else {
            "key_mapping"
        }
    }
}

@Composable
fun RuffleNavHost(
    navController: NavHostController = rememberNavController()
) {
    // 获取当前 Composable 的 Context，这通常是 Activity Context，启动 Activity 最稳妥
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Routes.SELECT_SWF
    ) {
        // 1. 主界面
        composable(Routes.SELECT_SWF) {
            SelectSwfScreen(
                onSwfSelected = { uri ->
                    // 启动游戏 Activity
                    val intent = Intent(Intent.ACTION_VIEW, uri, context, PlayerActivity::class.java)
                    // [关键] 赋予读权限，否则 Android 10+ 会因权限拒绝而闪退
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                },
                onGlobalSettingsClicked = {
                    navController.navigate(Routes.buildKeyMappingRoute())
                },
                onGameSettingsClicked = { uri, name ->
                    navController.navigate(Routes.buildKeyMappingRoute(uri.toString(), name))
                }
            )
        }

        // 2. 按键映射界面
        composable(
            route = Routes.KEY_MAPPING_ROUTE,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType; nullable = true },
                navArgument("name") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val gameUri = backStackEntry.arguments?.getString("uri")
            val gameName = backStackEntry.arguments?.getString("name")
            
            KeyMappingScreen(
                navController = navController,
                targetGameUri = gameUri,
                targetGameName = gameName
            )
        }
    }
}