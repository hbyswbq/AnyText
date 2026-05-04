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

object WeChatMessageHook {
    private val messageCache = mutableMapOf<TextView, Any>()
    private lateinit var classNames: WeChatVersionAdapter.WeChatClassNames

    fun hookWeChatMessages(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.thisObject as android.content.Context
                        classNames = WeChatVersionAdapter.getClassNames(context)
                        XposedBridge.log("AnyText: 微信版本适配完成")
                    }
                }
            )

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

                        val textView = findMessageTextView(view) ?: return
                        messageCache[textView] = messageObject

                        textView.setOnLongClickListener { v ->
                            showMessageEditor(v as TextView, messageObject)
                            true
                        }
                    }
                }
            )

            XposedBridge.log("AnyText: 微信消息Hook成功")
        } catch (e: Exception) {
            XposedBridge.log("AnyText: 微信消息Hook失败 - ${e.message}")
        }
    }

    private fun findMessageTextView(view: View): TextView? {
        if (view is TextView) {
            val contentId = view.resources.getIdentifier("tv_content", "id", "com.tencent.mm")
            if (view.id == contentId) {
                return view
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findMessageTextView(view.getChildAt(i))
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
                // 核心修复：使用 Xposed 原生反射替换 KavaRef
                textView.text = newText
                XposedHelpers.setObjectField(messageObject, classNames.messageContentField, newText)
                
                Toast.makeText(context, "消息已修改（仅本地）", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "修改失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearCache() {
        messageCache.clear()
    }
}
