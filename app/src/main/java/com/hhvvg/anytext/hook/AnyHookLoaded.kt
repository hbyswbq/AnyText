package com.hhvvg.anytext.hook

import android.app.Application
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

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.hhvvg.anytext") return

        XposedHelpers.findAndHookMethod(
            View::class.java,
            "dispatchTouchEvent",
            MotionEvent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val event = param.args[0] as MotionEvent
                    val view = param.thisObject as View

                    if (event.action == MotionEvent.ACTION_DOWN) {
                        findTextViewAndShowDialog(view, event.rawX, event.rawY)
                    }
                }
            }
        )
    }

    private fun findTextViewAndShowDialog(view: View, x: Float, y: Float) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (isTouchInView(child, x, y)) {
                    findTextViewAndShowDialog(child, x, y)
                }
            }
        } else if (view is TextView && view.isShown) {
            TextEditingDialog.show(view.context, view) { newText ->
                view.text = newText
            }
        }
    }

    private fun isTouchInView(view: View, x: Float, y: Float): Boolean {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val left = loc[0].toFloat()
        val top = loc[1].toFloat()
        val right = left + view.width.toFloat()
        val bottom = top + view.height.toFloat()
        return x >= left && x <= right && y >= top && y <= bottom
    }
}
