package com.hhvvg.anytext.hook

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
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
    private val TAG = "AnyTextHook"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.hhvvg.anytext") return

        try {
            // ✅ 终极方案：Hook全局触摸事件，完全绕过所有拦截
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "dispatchTouchEvent",
                MotionEvent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        val event = param.args[0] as MotionEvent

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                longPressRunnable = Runnable {
                                    val touchedTextView = findTouchedTextView(view.rootView, event.rawX, event.rawY)
                                    if (touchedTextView != null) {
                                        try {
                                            TextEditingDialog.show(touchedTextView.context, touchedTextView) { newText ->
                                                touchedTextView.text = newText
                                            }
                                            Log.d(TAG, "✅ 弹出编辑对话框成功")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ 弹出编辑对话框失败", e)
                                        }
                                    }
                                }
                                mainHandler.postDelayed(longPressRunnable!!, 500)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                            }
                        }
                    }
                }
            )

            Log.d(TAG, "✅ 模块加载成功: ${lpparam.packageName}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 模块加载失败", e)
        }
    }

    // 根据触摸坐标找到对应的TextView
    private fun findTouchedTextView(root: View, x: Float, y: Float): TextView? {
        val location = IntArray(2)
        root.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + root.width
        val bottom = top + root.height

        if (x < left || x > right || y < top || y > bottom) {
            return null
        }

        if (root is ViewGroup) {
            for (i in root.childCount - 1 downTo 0) {
                val child = root.getChildAt(i)
                if (child.isShown) {
                    val result = findTouchedTextView(child, x, y)
                    if (result != null) {
                        return result
                    }
                }
            }
        } else if (root is TextView && root.isShown) {
            Log.d(TAG, "✅ 找到触摸的TextView: ${root.text}")
            return root
        }

        return null
    }
}
