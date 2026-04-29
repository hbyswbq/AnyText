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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        Log.d(TAG, "==================== 模块被调用 ====================")
        Log.d(TAG, "当前应用包名: ${lpparam.packageName}")

        // 跳过自身
        if (lpparam.packageName == "com.hhvvg.anytext") {
            Log.d(TAG, "跳过自身应用")
            return
        }

        try {
            // Hook全局触摸事件
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "dispatchTouchEvent",
                MotionEvent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        val event = param.args[0] as MotionEvent

                        Log.d(TAG, "✅ 捕获到触摸事件: 动作=${event.action}, 坐标=(${event.rawX}, ${event.rawY})")

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                Log.d(TAG, "  ↳ 按下事件，准备触发长按")
                                longPressRunnable = Runnable {
                                    Log.d(TAG, "  ✅ 长按事件触发")
                                    val touchedTextView = findTouchedTextView(view.rootView, event.rawX, event.rawY)
                                    if (touchedTextView != null) {
                                        Log.d(TAG, "  ✅ 找到触摸的TextView: 文本='${touchedTextView.text}'")
                                        try {
                                            TextEditingDialog.show(touchedTextView.context, touchedTextView) { newText ->
                                                Log.d(TAG, "  ✅ 用户输入新文本: '$newText'")
                                                touchedTextView.text = newText
                                            }
                                            Log.d(TAG, "  ✅ 编辑对话框已弹出")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "  ❌ 弹出对话框失败", e)
                                        }
                                    } else {
                                        Log.d(TAG, "  ❌ 该位置没有找到TextView")
                                    }
                                }
                                mainHandler.postDelayed(longPressRunnable!!, 500)
                            }
                            MotionEvent.ACTION_UP -> {
                                Log.d(TAG, "  ↳ 抬起事件，取消长按")
                                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                Log.d(TAG, "  ↳ 触摸取消，取消长按")
                                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
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

    // 根据触摸坐标找到对应的TextView（从顶层到底层遍历）
    private fun findTouchedTextView(root: View, x: Float, y: Float): TextView? {
        val location = IntArray(2)
        root.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + root.width
        val bottom = top + root.height

        Log.v(TAG, "    检查View: ${root.javaClass.simpleName}, 范围=[$left, $top, $right, $bottom]")

        if (x < left || x > right || y < top || y > bottom) {
            Log.v(TAG, "    ↳ 坐标不在View范围内")
            return null
        }

        if (root is ViewGroup) {
            Log.v(TAG, "    ↳ 是ViewGroup，遍历子View（从顶层到底层）")
            // 从后往前遍历，因为后面的子View在最上层
            for (i in root.childCount - 1 downTo 0) {
                val child = root.getChildAt(i)
                if (child.isShown) {
                    val result = findTouchedTextView(child, x, y)
                    if (result != null) {
                        return result
                    }
                } else {
                    Log.v(TAG, "    ↳ 子View $i 不可见，跳过")
                }
            }
        } else if (root is TextView && root.isShown) {
            Log.v(TAG, "    ✅ 找到TextView")
            return root
        }

        return null
    }
}
