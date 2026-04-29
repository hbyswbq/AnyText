package com.hhvvg.anytext.hook

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.hhvvg.anytext.ui.TextEditingDialog
import android.util.Log

class AnyHookLoaded : IXposedHookLoadPackage {
    private val TAG = "AnyTextHook"
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 跳过自身应用，避免无限循环
        if (lpparam.packageName == "com.hhvvg.anytext") return

        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val application = param.thisObject as Application
                            application.registerActivityLifecycleCallbacks(GlobalActivityLifecycleListener())
                            Log.d(TAG, "成功注册Activity生命周期回调: ${lpparam.packageName}")
                        } catch (e: Exception) {
                            Log.e(TAG, "注册生命周期回调失败", e)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Hook Application.onCreate失败", e)
        }
    }

    private inner class GlobalActivityLifecycleListener : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // 延迟200ms遍历，确保布局完全加载完成
            mainHandler.postDelayed({
                try {
                    val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                    traverseViewTree(rootView)
                    Log.d(TAG, "Activity布局遍历完成: ${activity.javaClass.simpleName}")
                } catch (e: Exception) {
                    Log.e(TAG, "遍历View树失败", e)
                }
            }, 200)
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private fun traverseViewTree(view: View) {
        try {
            when (view) {
                is ViewGroup -> {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i) ?: continue
                        traverseViewTree(child)
                    }

                    // 特殊处理RecyclerView
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
        } catch (e: Exception) {
            Log.e(TAG, "处理单个View失败", e)
        }
    }

    // ✅ 修复：多Android版本兼容获取点击监听器
    private fun getOnClickListener(textView: TextView): View.OnClickListener? {
        val fieldNames = arrayOf("mOnClickListener", "mOnClickLister", "onClickListener")
        for (fieldName in fieldNames) {
            try {
                return XposedHelpers.getObjectField(textView, fieldName) as View.OnClickListener?
            } catch (e: Exception) {
                continue
            }
        }
        Log.w(TAG, "无法获取TextView的点击监听器")
        return null
    }

    private fun hookSingleTextView(textView: TextView) {
        try {
            val originalListener = getOnClickListener(textView)
            val wrapper = TextViewClickWrapper(originalListener, textView)
            textView.setOnClickListener(wrapper)
            textView.tag = wrapper
        } catch (e: Exception) {
            Log.e(TAG, "Hook TextView失败", e)
        }
    }

    private fun hookRecyclerViewAdapter(recyclerView: RecyclerView) {
        try {
            XposedHelpers.findAndHookMethod(
                RecyclerView::class.java,
                "setAdapter",
                RecyclerView.Adapter::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // 适配器更新后延迟遍历
                        recyclerView.postDelayed({
                            try {
                                traverseViewTree(recyclerView)
                            } catch (e: Exception) {
                                Log.e(TAG, "RecyclerView刷新后遍历失败", e)
                            }
                        }, 100)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Hook RecyclerView失败", e)
        }
    }

    private inner class TextViewClickWrapper(
        private val originalListener: View.OnClickListener?,
        private val targetTextView: TextView
    ) : View.OnClickListener {

        override fun onClick(v: View?) {
            try {
                // 先执行原始点击事件
                originalListener?.onClick(v)
                
                // 弹出编辑对话框
                val context = v?.context ?: return
                TextEditingDialog.show(context, targetTextView) { newText ->
                    targetTextView.text = newText
                }
            } catch (e: Exception) {
                Log.e(TAG, "点击事件处理失败", e)
            }
        }
    }
}
