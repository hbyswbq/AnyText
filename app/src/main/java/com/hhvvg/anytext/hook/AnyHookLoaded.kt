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
        // 跳过自身应用，避免无限循环
        if (lpparam.packageName == "com.hhvvg.anytext") return

        // Hook Application.onCreate，全局注册生命周期回调
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

    // 全局Activity生命周期监听器
    private inner class GlobalActivityLifecycleListener : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // 延迟执行，确保布局加载完成
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

    // 递归遍历View树，Hook所有TextView
    private fun traverseViewTree(view: View) {
        when (view) {
            is ViewGroup -> {
                // 遍历所有子View
                for (i in 0 until view.childCount) {
                    traverseViewTree(view.getChildAt(i))
                }

                // 特殊处理RecyclerView：修复刷新后监听器丢失问题
                if (view is RecyclerView) {
                    hookRecyclerViewAdapter(view)
                }
            }

            is TextView -> {
                // 用Tag标记避免重复设置监听器
                if (view.tag !is TextViewClickWrapper) {
                    hookSingleTextView(view)
                }
            }
        }
    }

    // Hook单个TextView
    private fun hookSingleTextView(textView: TextView) {
        // 先获取并移除原始监听器（Xposed标准写法，避免类型错误）
        val originalListener = textView.setOnClickListener(null)
        // 创建包装类，同时保留原始点击事件
        val wrapper = TextViewClickWrapper(originalListener, textView)
        textView.setOnClickListener(wrapper)
        textView.tag = wrapper
    }

    // Hook RecyclerView的setAdapter方法
    private fun hookRecyclerViewAdapter(recyclerView: RecyclerView) {
        XposedHelpers.findAndHookMethod(
            RecyclerView::class.java,
            "setAdapter",
            RecyclerView.Adapter::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // 适配器更新后，重新遍历所有子View
                    recyclerView.post { traverseViewTree(recyclerView) }
                }
            }
        )
    }

    // ✅ 核心修复：正确实现View.OnClickListener接口
    private inner class TextViewClickWrapper(
        private val originalListener: View.OnClickListener?,
        private val targetTextView: TextView
    ) : View.OnClickListener {

        override fun onClick(v: View?) {
            // 先执行原始点击事件，不影响应用原有功能
            originalListener?.onClick(v)

            // 弹出文本编辑对话框
            val context = v?.context ?: return
            TextEditingDialog.show(context, targetTextView) { newText ->
                targetTextView.text = newText
            }
        }
    }
}
