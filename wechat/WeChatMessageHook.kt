package com.hbyswbq.anytext.wechat

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.hbyswbq.anytext.ui.TextEditingDialog
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.ujhhgtg.kavaref.KavaRef

object WeChatMessageHook {
    private val messageCache = mutableMapOf<TextView, Any>()
    private lateinit var classNames: WeChatVersionAdapter.WeChatClassNames

    fun hookWeChatMessages(classLoader: ClassLoader) {
        try {
            // 先获取当前微信版本对应的类名和字段名
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.thisObject as android.content.Context
                        classNames = WeChatVersionAdapter.getClassNames(context)
                        XposedBridge.log("AnyText: 检测到微信版本，使用适配配置")
                    }
                }
            )

            // Hook消息项绑定方法（核心）
            XposedHelpers.findAndHookMethod(
                classNames.textMessageItemClass,
                classLoader,
                "bindView",
                Int::class.java,
                Any::class.java,
                View::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.args[2] as View
                        val messageObject = param.args[1]

                        // 找到消息内容TextView
                        val textView = findMessageTextView(view) ?: return

                        // 缓存消息对象（解决RecyclerView复用问题）
                        messageCache[textView] = messageObject

                        // 设置长按修改消息
                        textView.setOnLongClickListener { v ->
                            showMessageEditor(v as TextView, messageObject)
                            true // 消费事件，可改为false同时显示原菜单
                        }
                    }
                }
            )

            XposedBridge.log("AnyText: 微信消息Hook成功")
        } catch (e: Exception) {
            XposedBridge.log("AnyText: 微信消息Hook失败 - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun findMessageTextView(view: View): TextView? {
        if (view is TextView) {
            // 匹配微信消息内容TextView的ID
            val contentId = view.resources.getIdentifier("tv_content", "id", "com.tencent.mm")
            if (view.id == contentId) {
                return view
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findMessageTextView(child)
                if (result != null) return result
            }
        }

        return null
    }

    private fun showMessageEditor(textView: TextView, messageObject: Any) {
        val context = textView.context
        val originalText = textView.text.toString()

        TextEditingDialog.show(context, originalText) { newText ->
            try {
                // 1. 修改界面显示
                textView.text = newText

                // 2. 修改内存中的消息对象（滚动后不会恢复）
                KavaRef.setField(messageObject, classNames.messageContentField, newText)

                Toast.makeText(context, "消息已修改（仅本地可见）", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "修改失败: ${e.message}", Toast.LENGTH_SHORT).show()
                XposedBridge.log("AnyText: 修改消息失败 - ${e.message}")
            }
        }
    }

    fun clearCache() {
        messageCache.clear()
    }
}
