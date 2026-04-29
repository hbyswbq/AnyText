package com.hhvvg.anytext.hook

import android.app.Activity
import android.app.Application
import android.content.Context
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
    // ✅ 修复：用唯一的Int值作为Tag Key，不能用字符串
    private val HOOKED_KEY = 0x7f0a0001

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.hhvvg.anytext") return

        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val application = param.thisObject as Application
                        application.registerActivityLifecycleCallbacks(GlobalActivityLifecycleListener())
                        Log.d(TAG, "模块加载成功: ${lpparam.packageName}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "模块加载失败", e)
        }
    }

    private inner class GlobalActivityLifecycleListener : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            scheduleTraverse(activity, 200)
        }

        // 每次页面恢复都重新Hook，防止被应用覆盖
        override fun onActivityResumed(activity: Activity) {
            scheduleTraverse(activity, 100)
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private fun scheduleTraverse(activity: Activity, delay: Long) {
        mainHandler.postDelayed({
            try {
                val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                traverseViewTree(rootView)
                Log.d(TAG, "View树遍历完成")
            } catch (e: Exception) {
                Log.e(TAG, "View树遍历失败", e)
            }
        }, delay)
    }

    private fun traverseViewTree(view: View) {
        try {
            if (view.getTag(HOOKED_KEY) == true) return

            when (view) {
                is ViewGroup -> {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i) ?: continue
                        traverseViewTree(child)
                    }

                    if (view is RecyclerView) {
                        hookRecyclerViewAdapter(view)
                    }
                }

                is TextView -> {
                    hookTextView(view)
                }
            }

            view.setTag(HOOKED_KEY, true)
        } catch (e: Exception) {
            Log.e(TAG, "处理View失败", e)
        }
    }

    // 同时设置点击+长按触发，解决点击事件被覆盖问题
    private fun hookTextView(textView: TextView) {
        // 先获取原始监听器
        val originalClickListener = getOnClickListener(textView)
        val originalLongClickListener = getOnLongClickListener(textView)

        // 点击触发
        textView.setOnClickListener { v ->
            originalClickListener?.onClick(v)
            showEditDialog(v.context, textView)
        }

        // 长按触发（备用，点击被覆盖时用这个）
        textView.setOnLongClickListener { v ->
            val handled = originalLongClickListener?.onLongClick(v) ?: false
            if (!handled) {
                showEditDialog(v.context, textView)
            }
            true
        }

        Log.d(TAG, "成功Hook TextView: ${textView.text}")
    }

    // 多版本兼容获取点击监听器
    private fun getOnClickListener(textView: TextView): View.OnClickListener? {
        val fieldNames = arrayOf("mOnClickListener", "mOnClickLister", "onClickListener")
        for (fieldName in fieldNames) {
            try {
                return XposedHelpers.getObjectField(textView, fieldName) as View.OnClickListener?
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    // ✅ 修复：用反射获取长按监听器
    private fun getOnLongClickListener(textView: TextView): View.OnLongClickListener? {
        val fieldNames = arrayOf("mOnLongClickListener", "mOnLongClickLister", "onLongClickListener")
        for (fieldName in fieldNames) {
            try {
                return XposedHelpers.getObjectField(textView, fieldName) as View.OnLongClickListener?
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun hookRecyclerViewAdapter(recyclerView: RecyclerView) {
        try {
            XposedHelpers.findAndHookMethod(
                RecyclerView::class.java,
                "setAdapter",
                RecyclerView.Adapter::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        recyclerView.postDelayed({
                            // 清除RecyclerView的Hook标记，重新遍历所有子项
                            recyclerView.setTag(HOOKED_KEY, null)
                            traverseViewTree(recyclerView)
                        }, 150)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Hook RecyclerView失败", e)
        }
    }

    private fun showEditDialog(context: Context, textView: TextView) {
        try {
            TextEditingDialog.show(context, textView) { newText ->
                textView.text = newText
            }
        } catch (e: Exception) {
            Log.e(TAG, "弹出对话框失败", e)
        }
    }
}
