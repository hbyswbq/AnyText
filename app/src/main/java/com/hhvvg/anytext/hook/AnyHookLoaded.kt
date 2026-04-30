package com.hhvvg.anytext.hook

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
                            val tv = findTextViewAtPoint(view.rootView, event.rawX, event.rawY)
                            if (tv != null) {
                                longPressRunnable = Runnable {
                                    showEditDialog(tv.context, tv)
                                }
                                mainHandler.postDelayed(longPressRunnable!!, 600)
                            }
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

    private fun findTextViewAtPoint(root: View, x: Float, y: Float): TextView? {
        if (!isPointInView(root, x, y)) return null
        if (root is TextView && root.isShown && root.text.isNotEmpty()) return root

        if (root is ViewGroup) {
            for (i in root.childCount - 1 downTo 0) {
                val res = findTextViewAtPoint(root.getChildAt(i), x, y)
                if (res != null) return res
            }
        }
        return null
    }

    private fun isPointInView(view: View, x: Float, y: Float): Boolean {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return x >= loc[0] && x <= loc[0]+view.width && y >= loc[1] && y <= loc[1]+view.height
    }

    private fun showEditDialog(context: Context, tv: TextView) {
        val edit = EditText(context)
        edit.setText(tv.text)

        AlertDialog.Builder(context)
            .setTitle("修改微信文字")
            .setView(edit)
            .setPositiveButton("确定") { _, _ ->
                tv.text = edit.text.toString()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
