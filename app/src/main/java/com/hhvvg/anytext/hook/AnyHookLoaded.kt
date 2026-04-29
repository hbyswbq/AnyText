package com.hhvvg.anytext.hook

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
                    val application = param.thisObject as Application
                    application.registerActivityLifecycleCallbacks(ActivityLifecycleListener())
                }
            }
        )
    }

    private inner class ActivityLifecycleListener : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activity.window.decorView.post {
                traverseViewTree(activity.window.decorView.findViewById(android.R.id.content))
            }
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private fun traverseViewTree(view: View) {
        when (view) {
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    traverseViewTree(view.getChildAt(i))
                }
                // 修复RecyclerView刷新后监听器丢失问题
                if (view is RecyclerView) {
                    XposedHelpers.findAndHookMethod(
                        RecyclerView::class.java,
                        "setAdapter",
                        RecyclerView.Adapter::class.java,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                view.post { traverseViewTree(view) }
                            }
                        }
                    )
                }
            }
            is TextView -> {
                // 避免重复设置监听器
                if (view.tag !is TextViewOnClickWrapper) {
                    val originalListener = view.setOnClickListener(null)
                    val wrapper = TextViewOnClickWrapper(originalListener, view)
                    view.setOnClickListener(wrapper)
                    view.tag = wrapper
                }
            }
        }
    }

    // 正确实现View.OnClickListener接口
    private inner class TextViewOnClickWrapper(
        private val originalListener: View.OnClickListener?,
        private val textView: TextView
    ) : View.OnClickListener {

        override fun onClick(v: View?) {
            // 先执行原始点击事件
            originalListener?.onClick(v)
            // 弹出编辑对话框
            TextEditingDialog.show(v?.context ?: return, textView) { newText ->
                textView.text = newText
            }
        }
    }
}
