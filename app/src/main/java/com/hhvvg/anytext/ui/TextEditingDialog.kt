package com.hhvvg.anytext.ui

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import android.util.Log

object TextEditingDialog {
    private val TAG = "AnyText_DEBUG"

    fun show(context: Context, textView: TextView, onConfirm: (String) -> Unit) {
        Log.d(TAG, "TextEditingDialog.show 被调用")
        Log.d(TAG, "  上下文类型: ${context.javaClass.name}")

        val originalText = textView.text.toString()
        Log.d(TAG, "  原始文本: '$originalText'")

        val editText = EditText(context).apply {
            setText(originalText)
            setSelection(originalText.length)
            hint = "请输入新文本"
        }

        try {
            val dialog = AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog)
                .setTitle("修改文本")
                .setView(editText)
                .setPositiveButton("确定") { _, _ ->
                    val newText = editText.text.toString()
                    Log.d(TAG, "  用户点击确定，新文本: '$newText'")
                    onConfirm(newText)
                }
                .setNegativeButton("取消") { _, _ ->
                    Log.d(TAG, "  用户点击取消")
                }
                .create()

            dialog.show()
            Log.d(TAG, "  ✅ 对话框成功显示")
        } catch (e: Exception) {
            Log.e(TAG, "  ❌ 创建对话框失败", e)
        }
    }
}
