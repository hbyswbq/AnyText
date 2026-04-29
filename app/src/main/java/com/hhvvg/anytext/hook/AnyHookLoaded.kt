package com.hhvvg.anytext.hook

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.hhvvg.anytext.ui.TextEditingDialog
import android.util.Log

class AnyHookLoaded : IXposedHookLoadPackage {
    private val TAG = "AnyTextHook"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 跳过自身
        if (lpparam.packageName == "com.hhvvg.anytext") return

        try {
            // Hook Application.onCreate
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as Application
                        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                            // ✅ 最可靠的Hook时机：窗口获得焦点时，布局100%已加载
                            override fun onWindowFocusChanged(activity: Activity, hasFocus: Boolean) {
                                if (hasFocus) {
                                    try {
                                        val root = activity.window.decorView
                                        hookAllTextViews(root)
                                        Log.d(TAG, "成功Hook当前页面所有TextView")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Hook失败", e)
                                    }
                                }
                            }

                            override fun onActivityCreated(a: Activity, b: Bundle?) {}
                            override fun onActivityStarted(a: Activity) {}
                            override fun onActivityResumed(a: Activity) {}
                            override fun onActivityPaused(a: Activity) {}
                            override fun onActivityStopped(a: Activity) {}
                            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
                            override fun onActivityDestroyed(a: Activity) {}
                        })
                        Log.d(TAG, "模块加载成功: ${lpparam.packageName}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "模块加载失败", e)
        }
    }

    // 递归Hook所有TextView
    private fun hookAllTextViews(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i) ?: continue
                hookAllTextViews(child)
            }
        } else if (view is TextView) {
            // 用Tag避免重复设置
            if (view.tag != "anytext_hooked") {
                // ✅ 仅使用长按触发，完全避免点击事件冲突
                view.setOnLongClickListener { v ->
                    try {
                        TextEditingDialog.show(v.context, view) { newText ->
                            view.text = newText
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "弹出对话框失败", e)
                    }
                    true
                }
                view.tag = "anytext_hooked"
                Log.d(TAG, "Hooked: ${view.text}")
            }
        }
    }
}
