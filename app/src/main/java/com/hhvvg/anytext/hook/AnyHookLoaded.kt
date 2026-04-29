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

        // 全局 Hook 所有触摸 → 不管什么控件，长按都能取文字
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

    // 按坐标找最上层的文字（通杀所有界面）
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

    // 判断坐标是否在控件上
    private fun isTouchInView(view: View, x: Float, y: Float): Boolean {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val left = loc[0]
        val top = loc[1]
        val right = left + view.width
        val bottom = top + view.height
        return x in left..right && y in top..bottom
    }
}
