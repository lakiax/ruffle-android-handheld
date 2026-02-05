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
}