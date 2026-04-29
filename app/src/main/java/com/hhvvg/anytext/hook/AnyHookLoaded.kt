package com.hhvvg.anytext.hook

import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.hhvvg.anytext.ui.TextEditingDialog

class AnyHookLoaded : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.hhvvg.anytext") return

        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val app = param.thisObject as Application
                    app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                        override fun onActivityResumed(activity: android.app.Activity) {
                            hookAllTextViews(activity.window.decorView)
                        }
                        override fun onActivityCreated(a: android.app.Activity,b:Bundle?) {}
                        override fun onActivityStarted(a: android.app.Activity) {}
                        override fun onActivityPaused(a: android.app.Activity) {}
                        override fun onActivityStopped(a: android.app.Activity) {}
                        override fun onActivitySaveInstanceState(a:android.app.Activity,b:Bundle) {}
                        override fun onActivityDestroyed(a:android.app.Activity) {}
                    })
                }
            }
        )
    }

    private fun hookAllTextViews(root: View) {
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                hookAllTextViews(root.getChildAt(i))
            }
        } else if (root is TextView) {
            root.setOnLongClickListener { v ->
                TextEditingDialog.show(v.context, root) { newText ->
                    root.text = newText
                }
                true
            }
        }
    }
}
