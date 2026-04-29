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
        val pkg = lpparam.packageName
        if (pkg == "com.hhvvg.anytext") return

        // 给所有TextView添加长按事件（最稳定写法）
        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            lpparam.classLoader,
            "onAttachedToWindow"
        )
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val tv = param.thisObject as TextView
                if (tv.onLongClickListener == null) {
                    tv.setOnLongClickListener { v ->
                        TextEditingDialog.show(v.context, tv) { newText ->
                            tv.text = newText
                        }
                        true
                    }
                }
            }
        }
    }
}
