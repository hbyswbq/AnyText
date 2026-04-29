package com.hhvvg.anytext.hook

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

class AnyHookLoaded : IXposedHookLoadPackage {
    private val saveTextMap = hashMapOf<Int, String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var currentTargetView: View? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 只处理微信，其他APP不生效
        if (lpparam.packageName != "com.tencent.mm") return

        // 1. 防还原：拦截所有setText
        hookSetText()

        // 2. ✅ 终极修复：拦截最底层的触摸事件分发，自己实现长按
        hookDispatchTouchEvent()
    }

    private fun hookSetText() {
        runCatching {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "setText",
                CharSequence::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val tv = param.thisObject as TextView
                        val key = tv.hashCode()
                        if (saveTextMap.containsKey(key)) {
                            param.args[0] = saveTextMap[key]
                        }
                    }
                }
            )
        }
    }

    // ✅ 拦截所有View的触摸事件，自己检测长按
    private fun hookDispatchTouchEvent() {
        runCatching {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "dispatchTouchEvent",
                MotionEvent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val event = param.args[0] as MotionEvent
                        val view = param.thisObject as View

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                // 找到手指下的TextView
                                val targetTv = findTextViewAtPoint(view.rootView, event.rawX, event.rawY)
                                if (targetTv != null) {
                                    currentTargetView = targetTv
                                    // 500ms后触发长按
                                    longPressRunnable = Runnable {
                                        showEditDialog(targetTv.context, targetTv)
                                    }
                                    mainHandler.postDelayed(longPressRunnable!!, 500)
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_MOVE -> {
                                // 手指抬起、取消或移动，取消长按
                                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                                longPressRunnable = null
                                currentTargetView = null
                            }
                        }
                    }
                }
            )
        }
    }

    // 找到指定坐标下的TextView
    private fun findTextViewAtPoint(root: View, x: Float, y: Float): TextView? {
        if (!isPointInView(root, x, y)) return null

        if (root is TextView && root.isShown) {
            return root
        }

        if (root is ViewGroup) {
            // 从顶层子View开始找
            for (i in root.childCount - 1 downTo 0) {
                val child = root.getChildAt(i)
                val result = findTextViewAtPoint(child, x, y)
                if (result != null) return result
            }
        }
        return null
    }

    // 判断坐标是否在View内
    private fun isPointInView(view: View, x: Float, y: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0].toFloat()
        val top = location[1].toFloat()
        val right = left + view.width.toFloat()
        val bottom = top + view.height.toFloat()
        return x >= left && x <= right && y >= top && y <= bottom
    }

    // 弹出修改对话框
    private fun showEditDialog(context: Context, tv: TextView) {
        runCatching {
            com.hhvvg.anytext.ui.TextEditingDialog.show(
                context,
                tv,
                { newStr ->
                    runCatching {
                        saveTextMap[tv.hashCode()] = newStr
                        tv.text = newStr
                    }
                },
                {
                    saveTextMap.clear()
                }
            )
        }
    }
}
