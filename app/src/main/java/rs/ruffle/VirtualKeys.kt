package rs.ruffle

import android.view.KeyEvent

data class VirtualKey(val name: String, val keyCode: Int)

object VirtualKeys {
    val ALL_KEYS: List<VirtualKey> by lazy {
        val list = mutableListOf<VirtualKey>()

        // 1. 方向键
        list.add(VirtualKey("方向键: 上 (UP)", KeyEvent.KEYCODE_DPAD_UP))
        list.add(VirtualKey("方向键: 下 (DOWN)", KeyEvent.KEYCODE_DPAD_DOWN))
        list.add(VirtualKey("方向键: 左 (LEFT)", KeyEvent.KEYCODE_DPAD_LEFT))
        list.add(VirtualKey("方向键: 右 (RIGHT)", KeyEvent.KEYCODE_DPAD_RIGHT))

        // 2. 常用功能键
        list.add(VirtualKey("功能键: 确认/回车 (ENTER)", KeyEvent.KEYCODE_ENTER))
        list.add(VirtualKey("功能键: 跳跃/空格 (SPACE)", KeyEvent.KEYCODE_SPACE))
        list.add(VirtualKey("功能键: 退出 (ESC)", KeyEvent.KEYCODE_ESCAPE))
        list.add(VirtualKey("功能键: 退格 (DEL)", KeyEvent.KEYCODE_DEL))
        list.add(VirtualKey("功能键: Shift", KeyEvent.KEYCODE_SHIFT_LEFT))
        list.add(VirtualKey("功能键: Ctrl", KeyEvent.KEYCODE_CTRL_LEFT))

        // 3. 数字键 0-9
        for (i in 0..9) {
            list.add(VirtualKey("数字键: $i", KeyEvent.KEYCODE_0 + i))
        }

        // 4. 字母键 A-Z
        for (char in 'A'..'Z') {
            val code = KeyEvent.keyCodeFromString("KEYCODE_$char")
            list.add(VirtualKey("字母键: $char", code))
        }

        list
    }

    /**
     * [新增] 根据 KeyCode 获取底层引擎需要的 Tag 字符串 (全大写)
     * 用于 PlayerActivity 中动态分发事件
     */
    fun getTag(keyCode: Int): String? {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> "UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
            KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
            KeyEvent.KEYCODE_ENTER -> "ENTER"
            KeyEvent.KEYCODE_SPACE -> "SPACE"
            KeyEvent.KEYCODE_ESCAPE -> "ESC"
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> "DELETE"
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "SHIFT"
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> "CTRL"
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> "ALT"
            else -> {
                // 处理数字 0-9
                if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
                    return (keyCode - KeyEvent.KEYCODE_0).toString()
                }
                // 处理字母 A-Z
                if (keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
                    // 将 KeyCode 转换为字符 (A=29... 对应 ASCII 'A'=65)
                    // Android KeyCode A-Z 是连续的，可以直接计算
                    return (keyCode - KeyEvent.KEYCODE_A + 65).toChar().toString()
                }
                null
            }
        }
    }
}