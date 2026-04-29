package com.hhvvg.anytext.hook

import android.view.View
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.hhvvg.anytext.ui.TextEditingDialog

class AnyHookLoaded : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.hhvvg.anytext") return

        // 给所有 TextView 设置长按事件
        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            "setOnLongClickListener",
            View.OnLongClickListener::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val textView = param.thisObject as TextView
                    hookTextViewLongClick(textView)
                }
            }
        )
    }

    private fun hookTextViewLongClick(textView: TextView) {
        // 系统原有长按监听保留
        val originalListener = textView.onLongClickListener

        textView.setOnLongClickListener { v ->
            // 先弹我们的框
            TextEditingDialog.show(v.context, textView) { newText ->
                textView.text = newText
            }
            // 再执行原来的长按（不影响APP）
            originalListener?.onLongClick(v) ?: true
        }
    }
}
