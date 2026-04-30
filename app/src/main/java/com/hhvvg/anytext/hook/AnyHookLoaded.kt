package com.hhvvg.anytext.hook

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.app.AlertDialog
import android.widget.EditText
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AnyHookLoaded : IXposedHookLoadPackage {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.tencent.mm") return

        XposedHelpers.findAndHookMethod(
            "android.view.View",
            lpparam.classLoader,
            "dispatchTouchEvent",
            MotionEvent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val event = param.args[0] as MotionEvent
                    val view = param.thisObject as View

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // ✅ 先不管找TextView，长按任何地方都弹框
                            longPressRunnable = Runnable {
                                showTestDialog(view.context)
                            }
                            mainHandler.postDelayed(longPressRunnable!!, 600)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                            longPressRunnable = null
                        }
                    }
                }
            }
        )
    }

    private fun showTestDialog(context: Context) {
        val edit = EditText(context)
        edit.setText("测试文字")

        AlertDialog.Builder(context)
            .setTitle("测试对话框")
            .setView(edit)
            .setPositiveButton("确定") { _, _ -> }
            .setNegativeButton("取消", null)
            .show()
    }
}
