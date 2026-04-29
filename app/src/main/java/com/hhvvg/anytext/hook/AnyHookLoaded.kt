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

        // 兼容聊天列表：延迟设置长按，更稳定
        runCatching {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val tv = param.thisObject as TextView
                        tv.post {
                            tv.setOnLongClickListener {
                                showEditDialog(tv)
                                true
                            }
                        }
                    }
                }
            )
        }
    }

    // 安全弹出对话框
    private fun showEditDialog(tv: TextView) {
        runCatching {
            TextEditingDialog.show(tv.context, tv) { newText ->
                runCatching {
                    tv.text = newText
                }
            }
        }
    }
}
