package rs.ruffle

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class GameFile(
    val name: String,
    val uri: Uri
)

object GameUtils {
    /**
     * 扫描指定 URI 文件夹下的所有 .swf 文件
     */
    fun scanGames(context: Context, directoryUri: Uri): List<GameFile> {
        val gameList = mutableListOf<GameFile>()
        
        // 使用 DocumentFile 构建文件夹对象
        val rootDir = DocumentFile.fromTreeUri(context, directoryUri)
        
        if (rootDir != null && rootDir.isDirectory) {
            // 遍历文件
            val files = rootDir.listFiles()
            for (file in files) {
                // 简单的过滤逻辑：必须是文件，且后缀是 .swf (忽略大小写)
                if (file.isFile && file.name?.lowercase()?.endsWith(".swf") == true) {
                    gameList.add(
                        GameFile(
                            name = file.name ?: "Unknown",
                            uri = file.uri
                        )
                    )
                }
            }
        }
        return gameList
    }
}