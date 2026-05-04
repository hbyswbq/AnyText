package com.hbyswbq.anytext.wechat

import android.content.pm.PackageManager
import android.content.Context

object WeChatVersionAdapter {
    data class WeChatClassNames(
        val chattingUIClass: String = "com.tencent.mm.ui.chatting.ChattingUI",
        val textMessageItemClass: String = "com.tencent.mm.ui.chatting.viewitems.TextMessageItem",
        val messageContentField: String = "field_content",
        val messageObjectField: String = "msg"
    )

    fun getClassNames(context: Context): WeChatClassNames {
        return try {
            val packageInfo = context.packageManager.getPackageInfo("com.tencent.mm", 0)
            val versionName = packageInfo.versionName

            return when {
                versionName.startsWith("8.0.49") -> WeChatClassNames(
                    messageContentField = "content"
                )
                versionName.startsWith("8.0.65") || 
                versionName.startsWith("8.0.68") || 
                versionName.startsWith("8.0.71") -> WeChatClassNames(
                    messageContentField = "content"
                )
                else -> WeChatClassNames()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            WeChatClassNames()
        }
    }
}
