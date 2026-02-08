package rs.ruffle

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.InputDevice
import com.google.androidgamesdk.GameActivity
import java.io.DataInputStream
import java.io.File
import java.io.IOException

class PlayerActivity : GameActivity() {
    // 用于缓存按键映射关系
	private var activeKeyMapping: Map<Int, Int> = emptyMap()
	private val pressedMappedKeys = HashSet<Int>()
	private lateinit var toolbar: View
	private lateinit var fabMenu: View
	private lateinit var mask: View

    // [删除] 删除了原来的 keyToNativeTag，现在使用 VirtualKeys.getTag() 动态获取
    
    @Suppress("unused")
    // Used by Rust
    private val swfBytes: ByteArray?
        get() {
            val uri = intent.data
            if (uri?.scheme == "content") {
                try {
                    contentResolver.openInputStream(uri).use { inputStream ->
                        if (inputStream == null) {
                            return null
                        }
                        val bytes = ByteArray(inputStream.available())
                        val dataInputStream = DataInputStream(inputStream)
                        dataInputStream.readFully(bytes)
                        return bytes
                    }
                } catch (ignored: IOException) {
                }
            }
            return null
        }

    @Suppress("unused")
    // Used by Rust
    private val swfUri: String?
        get() {
            return intent.dataString
        }

    @Suppress("unused")
    // Used by Rust
    private val traceOutput: String?
        get() {
            return intent.getStringExtra("traceOutput")
        }

    @Suppress("unused")
    // Used by Rust
    private fun navigateToUrl(url: String?) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private var loc = IntArray(2)

    @Suppress("unused")
    // Handle of an EventLoopProxy over in rust-land
    private val eventLoopHandle: Long = 0

    @Suppress("unused")
    // Used by Rust
    private val locInWindow: IntArray
        get() {
            mSurfaceView.getLocationInWindow(loc)
            return loc
        }

    @Suppress("unused")
    // Used by Rust
    private val surfaceWidth: Int
        get() = mSurfaceView.width

    @Suppress("unused")
    // Used by Rust
    private val surfaceHeight: Int
        get() = mSurfaceView.height

    private external fun keydown(keyTag: String)
    private external fun keyup(keyTag: String)
    private external fun requestContextMenu()
    private external fun runContextMenuCallback(index: Int)
    private external fun clearContextMenu()

    @Suppress("unused")
    // Used by Rust
    private fun showContextMenu(items: Array<String>) {
        runOnUiThread {
            val popup = PopupMenu(this, findViewById(R.id.button_cm))
            val menu = popup.menu
            if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
                menu.setGroupDividerEnabled(true)
            }
            var group = 1
            for (i in items.indices) {
                val elements = items[i].split(" ".toRegex(), limit = 4).toTypedArray()
                val enabled = elements[0].toBoolean()
                val separatorBefore = elements[1].toBoolean()
                val checked = elements[2].toBoolean()
                val caption = elements[3]
                if (separatorBefore) group += 1
                val item = menu.add(group, i, Menu.NONE, caption)
                item.setEnabled(enabled)
                if (checked) {
                    item.setCheckable(true)
                    item.setChecked(true)
                }
            }
            val exitItemId: Int = items.size
            menu.add(group, exitItemId, Menu.NONE, "Exit")
            popup.setOnMenuItemClickListener { item: MenuItem ->
                if (item.itemId == exitItemId) {
                    finish()
                } else {
                    runContextMenuCallback(item.itemId)
                }
                true
            }
            popup.setOnDismissListener { clearContextMenu() }
            popup.show()
        }
    }

    @Suppress("unused")
    // Used by Rust
    private fun getAndroidDataStorageDir(): String {
        // TODO It can also be placed in an external storage path in the future to share archived content
        val storageDirPath = "${filesDir.absolutePath}/ruffle/shared_objects"
        val storageDir = File(storageDirPath)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return storageDirPath
    }

	private fun showMenu() {
	    toolbar.visibility = View.VISIBLE
	    mask.visibility = View.VISIBLE
	    fabMenu.visibility = View.GONE
	}

	private fun hideMenu() {
	    toolbar.visibility = View.GONE
	    mask.visibility = View.GONE
	    fabMenu.visibility = View.VISIBLE
	}
	
    override fun onCreateSurfaceView() {
        val inflater = layoutInflater

        @SuppressLint("InflateParams")
        val layout = inflater.inflate(R.layout.keyboard, null) as ConstraintLayout

        contentViewId = View.generateViewId()
        layout.id = contentViewId
        setContentView(layout)
	
    	toolbar = layout.findViewById(R.id.toolbar)
    	fabMenu = layout.findViewById(R.id.fab_menu)
    	mask = layout.findViewById(R.id.menu_mask)
    
    	fabMenu.setOnClickListener { showMenu() }
    	mask.setOnClickListener { hideMenu() }
	
        mSurfaceView = InputEnabledSurfaceView(this)

        mSurfaceView.contentDescription = "Ruffle Player"

        val placeholder = findViewById<View>(R.id.placeholder)
        val pars = placeholder.layoutParams as ConstraintLayout.LayoutParams
        val parent = placeholder.parent as ViewGroup
        val index = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        parent.addView(mSurfaceView, index)
        mSurfaceView.setLayoutParams(pars)
        val keys = gatherAllDescendantsOfType<Button>(
            layout.getViewById(R.id.keyboard),
            Button::class.java
        )
        for (b in keys) {
            b.setOnTouchListener { view: View, motionEvent: MotionEvent ->
                val tag = view.tag as String
                if (motionEvent.action == MotionEvent.ACTION_DOWN) keydown(tag)
                if (motionEvent.action == MotionEvent.ACTION_UP) keyup(tag)
                view.performClick()
                false
            }
        }
        layout.findViewById<View>(R.id.button_kb).setOnClickListener {
            val keyboard = layout.getViewById(R.id.keyboard)
            if (keyboard.visibility == View.VISIBLE) {
                keyboard.visibility = View.GONE
            } else {
                keyboard.visibility = View.VISIBLE
            }
        }
        layout.findViewById<View>(R.id.button_cm)
            .setOnClickListener { requestContextMenu()}
        layout.requestLayout()
        layout.requestFocus()
        mSurfaceView.holder.addCallback(this)
        ViewCompat.setOnApplyWindowInsetsListener(mSurfaceView, this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val keyboard = findViewById<View>(R.id.keyboard)
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        keyboard.visibility = if (isLandscape) View.GONE else View.VISIBLE
    }

    private fun hideSystemUI() {
        // This will put the game behind any cutouts and waterfalls on devices which have
        // them, so the corresponding insets will be non-zero.
        if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        // From API 30 onwards, this is the recommended way to hide the system UI, rather than
        // using View.setSystemUiVisibility.
        val decorView = window.decorView
        val controller = WindowInsetsControllerCompat(
            window,
            decorView
        )
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.displayCutout())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        nativeInit { message ->
            Log.e("ruffle", "Handling panic: $message")
            startActivity(
                Intent(this, PanicActivity::class.java).apply {
                    putExtra("message", message)
                }
            )
        }
        // When true, the app will fit inside any system UI windows.
        // When false, we render behind any system UI windows.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUI()
        // You can set IME fields here or in native code using GameActivity_setImeEditorInfoFields.
        // We set the fields in native_engine.cpp.
        // super.setImeEditorInfoFields(InputType.TYPE_CLASS_TEXT,
        //     IME_ACTION_NONE, IME_FLAG_NO_FULLSCREEN );
        requestNoStatusBarFeature()
        supportActionBar?.hide()
	
        // [新增] 启动前加载按键映射配置
        loadKeyMappings()
	
        super.onCreate(savedInstanceState)
    }   

// [修改] 读取按键映射配置 - 增加 URI 解码以匹配设置页面的 Key
    private fun loadKeyMappings() {
        try {
            val rawUri = intent.data?.toString()
            // 修复: 对 URI 进行解码，因为 Navigation 传参时通常是解码后的，需保持 Key 一致
            val gameUri = if (rawUri != null) Uri.decode(rawUri) else null
            
            // 1. 读取全局配置
            val globalMapping = PreferencesManager.getGlobalMapping(this)
            val finalMapping = java.util.HashMap(globalMapping)
            
            // 2. 读取局部配置
            if (gameUri != null) {
                // 优先尝试解码后的 URI
                var localMapping = PreferencesManager.getGameMapping(this, gameUri)
                
                // 保底: 如果解码后没找到，且原始 URI 不同，尝试原始 URI (防止部分情况未编码)
                if (localMapping.isEmpty() && rawUri != gameUri) {
                    val rawMapping = PreferencesManager.getGameMapping(this, rawUri!!)
                    if (rawMapping.isNotEmpty()) localMapping = rawMapping
                }

                if (localMapping.isNotEmpty()) {
                    finalMapping.putAll(localMapping)
                }
            }
            activeKeyMapping = finalMapping
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // [最终优化版] 动态分发按键事件
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // 1. 获取映射后的 KeyCode (如果没有映射则保持原样)
        val mappedKeyCode = activeKeyMapping[event.keyCode] ?: event.keyCode

        // 2. [动态查找] 尝试通过 VirtualKeys 获取对应的 Native Tag
        // 这意味着只要你在设置里映射到了某个虚拟键（如 SPACE, A, 1），这里就能获取到对应的 Tag
        val nativeTag = VirtualKeys.getTag(mappedKeyCode)

        if (nativeTag != null) {
            // [核心逻辑] 拦截并伪装成虚拟按键
            
            // 过滤重复事件：只处理 DOWN 和 UP
            if (event.repeatCount > 0 && event.action == KeyEvent.ACTION_DOWN) {
                return true
            }

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!pressedMappedKeys.contains(mappedKeyCode)) {
                        keydown(nativeTag)
                        pressedMappedKeys.add(mappedKeyCode)
                    }
                }
                KeyEvent.ACTION_UP -> {
                    keyup(nativeTag)
                    pressedMappedKeys.remove(mappedKeyCode)
                }
            }
            // 拦截事件，不再传给底层 InputQueue，彻底解决卡死
            return true 
        }

        // 3. 没在映射表里，也不是已知虚拟键的按键（如音量、Home），走系统默认逻辑
        return super.dispatchKeyEvent(event)
    }

	private fun releaseAllPressedKeys(base: KeyEvent? = null) {
	    if (pressedMappedKeys.isEmpty()) return
	    val keys = pressedMappedKeys.toList()
	    pressedMappedKeys.clear()
        
        // 既然我们现在主要靠 tag 调用 keyup，这里也应该尝试用 tag 释放
	    for (code in keys) {
            val tag = VirtualKeys.getTag(code)
            if (tag != null) {
                 keyup(tag)
            }
	    }
	}
	
	override fun onWindowFocusChanged(hasFocus: Boolean) {
	    super.onWindowFocusChanged(hasFocus)
	    if (!hasFocus) releaseAllPressedKeys(null) else mSurfaceView?.requestFocus()
	}

	override fun onPause() {
	    releaseAllPressedKeys(null)
	    super.onPause()
	}

    
    // Used by Rust
    @Suppress("unused")
    val isGooglePlayGames: Boolean
        get() {
            val pm = packageManager
            return pm.hasSystemFeature("com.google.android.play.feature.HPE_EXPERIENCE")
        }

    private fun requestNoStatusBarFeature() {
        // Hiding the status bar this way makes it see through when pulled down
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowInsetsControllerCompat(
            window,
            mSurfaceView
        ).hide(WindowInsetsCompat.Type.statusBars())
    }

    companion object {
        init {
            // load the native activity
            System.loadLibrary("ruffle_android")
        }

        @JvmStatic
        private external fun nativeInit(crashCallback: CrashCallback)

        private fun <T> gatherAllDescendantsOfType(v: View, t: Class<*>): List<T> {
            val result: MutableList<T> = ArrayList()
            @Suppress("UNCHECKED_CAST")
            if (t.isInstance(v)) result.add(v as T)
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) {
                    result.addAll(gatherAllDescendantsOfType(v.getChildAt(i), t))
                }
            }
            return result
        }
    }

    fun interface CrashCallback {
        fun onCrash(message: String)
    }

}
