package com.hhvvg.anytext.hook

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.hhvvg.anytext.ui.TextEditingDialog

class AnyHookLoaded : IXposedHookLoadPackage {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private val LONG_PRESS_DELAY = 500L // 这里加 L 变成 Long 类型

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.hhvvg.anytext") return

        XposedHelpers.findAndHookMethod(
            View::class.java,
            "dispatchTouchEvent",
            MotionEvent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as View
                    val event = param.args[0] as MotionEvent

                    if (view !is TextView) return

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            longPressRunnable = Runnable {
                                TextEditingDialog.show(view.context, view) { newText ->
                                    view.text = newText
                                }
                            }
                            mainHandler.postDelayed(longPressRunnable!!, LONG_PRESS_DELAY)
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            longPressRunnable?.let {
                                mainHandler.removeCallbacks(it)
                            }
                        }
                    }
                }
            }
        )
    }
}
