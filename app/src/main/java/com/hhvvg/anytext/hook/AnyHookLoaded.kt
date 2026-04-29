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
                    application.registerActivityLifecycleCallbacks(GlobalActivityLifecycleListener())
                }
            }
        )
    }

    private inner class GlobalActivityLifecycleListener : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activity.window.decorView.post {
                val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                traverseViewTree(rootView)
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

                if (view is RecyclerView) {
                    hookRecyclerViewAdapter(view)
                }
            }

            is TextView -> {
                if (view.tag !is TextViewClickWrapper) {
                    hookSingleTextView(view)
                }
            }
        }
    }

    // ✅ 修复：Kotlin中setOnClickListener返回Unit，改用Xposed获取原始监听器
    private fun hookSingleTextView(textView: TextView) {
        val originalListener = XposedHelpers.getObjectField(textView, "mOnClickListener") as View.OnClickListener?
        val wrapper = TextViewClickWrapper(originalListener, textView)
        textView.setOnClickListener(wrapper)
        textView.tag = wrapper
    }

    private fun hookRecyclerViewAdapter(recyclerView: RecyclerView) {
        XposedHelpers.findAndHookMethod(
            RecyclerView::class.java,
            "setAdapter",
            RecyclerView.Adapter::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    recyclerView.post { traverseViewTree(recyclerView) }
                }
            }
        )
    }

    private inner class TextViewClickWrapper(
        private val originalListener: View.OnClickListener?,
        private val targetTextView: TextView
    ) : View.OnClickListener {

        override fun onClick(v: View?) {
            originalListener?.onClick(v)
            val context = v?.context ?: return
            TextEditingDialog.show(context, targetTextView) { newText ->
                targetTextView.text = newText
            }
        }
    }
}
