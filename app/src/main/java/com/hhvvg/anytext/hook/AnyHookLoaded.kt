package com.hhvvg.anytext.hook

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

class AnyHookLoaded : IXposedHookLoadPackage {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentTextView: TextView? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackageParam) {
        if (lpparam.packageName == "com.hhvvg.anytext") return

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
                            currentTextView = findTopTextView(view.rootView, event.rawX, event.rawY)
                            currentTextView?.let { tv ->
                                mainHandler.postDelayed({
                                    TextEditingDialog.show(tv.context, tv) { newText ->
                                        tv.text = newText
                                    }
                                }, 500)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            mainHandler.removeCallbacksAndMessages(null)
                            currentTextView = null
                        }
                    }
                }
            }
        )
    }

    private fun findTopTextView(root: View, x: Float, y: Float): TextView? {
        if (!isInView(root, x, y)) return null
        if (root is TextView && root.isShown) return root

        if (root is ViewGroup) {
            for (i in root.childCount - 1 downTo 0) {
                val res = findTopTextView(root.getChildAt(i), x, y)
                if (res != null) return res
            }
        }
        return null
    }

    private fun isInView(view: View, x: Float, y: Float): Boolean {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val l = loc[0].toFloat()
        val t = loc[1].toFloat()
        val r = l + view.width.toFloat()
        val b = t + view.height.toFloat()
        return x >= l && x <= r && y >= t && y <= b
    }
}
