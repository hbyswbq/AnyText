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

        runCatching {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val tv = param.thisObject as TextView
                        tv.setOnLongClickListener {
                            runCatching {
                                TextEditingDialog.show(it.context, tv) { newText ->
                                    runCatching {
                                        tv.text = newText
                                    }
                                }
                            }
                            true
                        }
                    }
                }
            )
        }
    }
}
