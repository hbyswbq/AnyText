package com.hhvvg.anytext.hook

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.hhvvg.anytext.ui.TextEditingDialog

class AnyHookLoaded : IXposedHookLoadPackage {

    // 缓存自定义文本，抗页面刷新
    private val textCache = hashMapOf<Int, String>()

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        if (pkg == "com.hhvvg.anytext") return

        // 1. 拦截setText，刷新时强制还原我们修改的文字
        runCatching {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "setText",
                CharSequence::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val tv = param.thisObject as TextView
                        val key = tv.hashCode()
                        if (textCache.containsKey(key)) {
                            param.args[0] = textCache[key]
                        }
                    }
                }
            )
        }

        // 2. 全局遍历所有控件，适配聊天气泡、隐藏文本、自定义文本控件
        runCatching {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        scanAllTextView(view)
                    }
                }
            )
        }
    }

    // 递归扫描页面所有文字控件
    private fun scanAllTextView(view: View) {
        if (view is TextView) {
            bindLongClick(view)
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                scanAllTextView(view.getChildAt(i))
            }
        }
    }

    // 绑定长按修改事件
    private fun bindLongClick(tv: TextView) {
        tv.post {
            tv.setOnLongClickListener {
                runCatching {
                    TextEditingDialog.show(tv.context, tv) { newText ->
                        runCatching {
                            // 存入缓存，抗刷新
                            textCache[tv.hashCode()] = newText
                            tv.text = newText
                        }
                    }
                }
                true
            }
        }
    }
}
