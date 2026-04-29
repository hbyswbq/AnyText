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
        // 只处理微信，其他APP不生效，避免冲突
        if (lpparam.packageName != "com.tencent.mm") return

        // 1. 防还原：拦截所有setText
        hookSetText()

        // 2. 核心修复：永久拦截所有长按事件设置
        hookAllLongClick()
    }

    private fun hookSetText() {
        runCatching {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
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

    // 永久拦截所有View的长按事件设置
    private fun hookAllLongClick() {
        runCatching {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "setOnLongClickListener",
                View.OnLongClickListener::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        // 延迟500ms，确保微信的长按事件完全设置完再覆盖
                        view.postDelayed({
                            bindOurLongClick(view)
                        }, 500)
                    }
                }
            )
        }

        // 兜底：页面加载时主动给所有控件绑定
        runCatching {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        view.postDelayed({
                            bindOurLongClick(view)
                        }, 500)
                    }
                }
            )
        }
    }

    // 递归绑定我们的长按事件
    private fun bindOurLongClick(view: View) {
        runCatching {
            // 如果是TextView，直接绑定
            if (view is TextView) {
                view.setOnLongClickListener {
                    showEditDialog(it.context, view)
                    true
                }
                return
            }

            // 如果是ViewGroup（微信聊天气泡本身），递归绑定子View
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    bindOurLongClick(view.getChildAt(i))
                }
            }
        }
    }

    // 弹出修改对话框
    private fun showEditDialog(context: Context, tv: TextView) {
        runCatching {
            com.hhvvg.anytext.ui.TextEditingDialog.show(
                context,
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
    }
}
