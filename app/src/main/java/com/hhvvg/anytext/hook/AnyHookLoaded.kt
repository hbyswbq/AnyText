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

        //  Hook TextView setText 方法 → 永久拦截文本
        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            "setText",
            CharSequence::class.java,
            TextView.BufferType::class.java,
            Boolean::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val tv = param.thisObject as TextView
                    val newText = param.args[0] as CharSequence?

                    // 如果有我们保存的自定义文本，强制替换回去
                    val custom = tv.getTag(10001) as? String
                    if (custom != null) {
                        param.args[0] = custom
                    }
                }
            }
        )

        // 长按弹出编辑框
        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val tv = param.thisObject as TextView
                    tv.setOnLongClickListener {
                        TextEditingDialog.show(it.context, tv) { newText ->
                            // 保存到 Tag，永久生效
                            tv.setTag(10001, newText)
                            tv.text = newText
                        }
                        true
                    }
                }
            }
        )
    }
}
