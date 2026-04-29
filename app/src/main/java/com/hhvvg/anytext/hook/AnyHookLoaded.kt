package com.hhvvg.anytext.hook

import android.view.MotionEvent
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

        hookSetText()
        hookViewAttach()
    }

    // 拦截setText，防文字还原
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

    // 遍历全局所有文字控件
    private fun hookViewAttach() {
        runCatching {
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val rootView = param.thisObject as View
                        traverseFindTextView(rootView)
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

    // 长按修改逻辑 + 长按菜单增加重置选项
    private fun bindLongClickEdit(tv: TextView) {
        tv.post {
            tv.setOnLongClickListener {
                runCatching {
                    com.hhvvg.anytext.ui.TextEditingDialog.show(tv.context, tv, { newStr ->
                        runCatching {
                            saveTextMap[tv.hashCode()] = newStr
                            tv.text = newStr
                        }
                    }, {
                        // 重置回调
                        saveTextMap.clear()
                    })
                }
                true
            }
        }
    }
}
