package com.hhvvg.anytext.hook

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AnyHookLoaded : IXposedHookLoadPackage {
    private val saveTextMap = hashMapOf<Int, String>()

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        // 只作用微信
        if (pkg != "com.tencent.mm") return

        val classLoader = lpparam.classLoader

        // 拦截setText，防止滑动还原
        runCatching {
            XposedHelpers.findAndHookMethod(
                "android.widget.TextView",
                classLoader,
                "setText",
                CharSequence::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val tv = param.thisObject as TextView
                        val key = tv.hashCode()
                        if (saveTextMap.containsKey(key)) {
                            param.args[0] = saveTextMap[key]
                        }
                    }
                }
            )
        }

        // 页面控件加载时遍历绑定长按
        runCatching {
            XposedHelpers.findAndHookMethod(
                "android.view.View",
                classLoader,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        view.postDelayed({
                            scanAllTextVeiws(view)
                        }, 150)
                    }
                }
            )
        }
    }

    private fun scanAllTextVeiws(view: View) {
        if (view is TextView) {
            bindLongClick(view)
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                scanAllTextVeiws(view.getChildAt(i))
            }
        }
    }

    private fun bindLongClick(tv: TextView) {
        tv.setOnLongClickListener {
            com.hhvvg.anytext.ui.TextEditingDialog.show(tv.context, tv){ newTxt ->
                saveTextMap[tv.hashCode()] = newTxt
                tv.text = newTxt
            }
            true
        }
    }
}
