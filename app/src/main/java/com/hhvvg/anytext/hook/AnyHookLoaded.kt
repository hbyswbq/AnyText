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
        val packageName = lpparam.packageName
        if (packageName == "com.hhvvg.anytext") return

        // 安全 Hook TextView 长按
        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            "setOnLongClickListener",
            View.OnLongClickListener::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val textView = param.thisObject as TextView
                    val originalListener = textView.onLongClickListener

                    textView.setOnLongClickListener { v ->
                        TextEditingDialog.show(v.context, textView) { newText ->
                            textView.text = newText
                        }
                        originalListener?.onLongClick(v) ?: true
                    }
                }
            })
    }
}
