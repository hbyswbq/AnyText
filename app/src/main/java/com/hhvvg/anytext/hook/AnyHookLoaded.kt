package com.hhvvg.anytext.hook

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.hhvvg.anytext.ui.TextEditingDialog
import android.util.Log

class AnyHookLoaded : IXposedHookLoadPackage {
    private val TAG = "AnyText_DEBUG"
    private var mainHandler: Handler? = null
    private var longPressRunnable: Runnable? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 延迟到模块入口再初始化主线程Handler，避免Looper为空
        mainHandler = Handler(Looper.getMainLooper())

        Log.d(TAG, "==================== 模块被调用 ====================")
        Log.d(TAG, "当前应用包名: ${lpparam.packageName}")

        if (lpparam.packageName == "com.hhvvg.anytext") {
            Log.d(TAG, "跳过自身应用")
            return
        }

        try {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "dispatchTouchEvent",
                MotionEvent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        val event = param.args[0] as MotionEvent
                        val handler = mainHandler ?: return

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                longPressRunnable = Runnable {
                                    val touchedTextView = findTouchedTextView(view.rootView, event.rawX, event.rawY)
                                    if (touchedTextView != null) {
                                        try {
                                            TextEditingDialog.show(touchedTextView.context, touchedTextView) { newText ->
                                                touchedTextView.text = newText
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "弹出对话框失败", e)
                                        }
                                    }
                                }
                                handler.postDelayed(longPressRunnable!!, 500)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                longPressRunnable?.let { handler.removeCallbacks(it) }
                            }
                        }
                    }
                }
            )
            Log.d(TAG, "✅ 全局触摸事件Hook成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 全局触摸事件Hook失败", e)
        }
    }

    private fun findTouchedTextView(root: View, x: Float, y: Float): TextView? {
        val location = IntArray(2)
        root.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + root.width
        val bottom = top + root.height

        if (x < left || x > right || y < top || y > bottom) return null

        if (root is ViewGroup) {
            for (i in root.childCount - 1 downTo 0) {
                val child = root.getChildAt(i)
                if (child.isShown) {
                    val res = findTouchedTextView(child, x, y)
                    if (res != null) return res
                }
            }
        } else if (root is TextView && root.isShown) {
            return root
        }
        return null
    }
}
