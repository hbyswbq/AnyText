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

        // 正确 Hook 方式，不报错、不崩溃
        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val textView = param.thisObject as TextView
                    
                    // 直接设置长按，不读取原有监听，彻底避免报错
                    textView.setOnLongClickListener { v ->
                        TextEditingDialog.show(v.context, textView) { newText ->
                            textView.text = newText
                        }
                        true
                    }
                }
            }
        )
    }
}
