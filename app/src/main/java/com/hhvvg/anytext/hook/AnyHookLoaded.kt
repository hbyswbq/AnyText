package com.hhvvg.anytext.hook

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AnyHookLoaded : IXposedHookLoadPackage {
    // 缓存：保存所有修改后的文字
    private val saveTextMap = hashMapOf<Int, String>()

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        if (pkg == "com.hhvvg.anytext") return

        // ✅ 关键修复：传入目标APP的ClassLoader！
        // 之前用模块ClassLoader，根本hook不到微信的自定义类
        hookSetText(lpparam.classLoader)
        hookViewAttach(lpparam.classLoader)
    }

    // 拦截所有TextView（包括微信自定义MMTextView）的setText
    private fun hookSetText(classLoader: ClassLoader) {
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
    }

    // 遍历所有View，给所有文字控件绑定长按
    private fun hookViewAttach(classLoader: ClassLoader) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                "android.view.View",
                classLoader,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        // ✅ 微信气泡延迟绑定，覆盖原长按事件
                        view.postDelayed({
                            traverseFindTextView(view)
                        }, 100)
                    }
                }
            )
        }
    }

    private fun traverseFindTextView(view: View) {
        if (view is TextView) {
            bindLongClickEdit(view)
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                traverseFindTextView(view.getChildAt(i))
            }
        }
    }

    // 长按修改逻辑
    private fun bindLongClickEdit(tv: TextView) {
        runCatching {
            tv.setOnLongClickListener {
                runCatching {
                    com.hhvvg.anytext.ui.TextEditingDialog.show(
                        tv.context,
                        tv,
                        { newStr ->
                            runCatching {
                                saveTextMap[tv.hashCode()] = newStr
                                tv.text = newStr
                            }
                        },
                        {
                            saveTextMap.clear()
                        }
                    )
                }
                true
            }
        }
    }
}
