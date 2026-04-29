package com.hhvvg.anytext.ui

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.app.AlertDialog
import android.util.Log

object TextEditingDialog {
    private const val TAG = "AnyText_DEBUG"

    fun show(context: Context, textView: TextView, onConfirm: (String) -> Unit) {
        Log.d(TAG, "✅ 原生对话框启动，不依赖AppCompat")
        val originalText = textView.text.toString()

        val editText = EditText(context).apply {
            setText(originalText)
            setSelection(originalText.length)
            hint = "请输入新内容"
        }

        // ✅ 用系统原生 AlertDialog，兼容所有主题！
        val dialog = AlertDialog.Builder(context)
            .setTitle("修改文本")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newText = editText.text.toString().trim()
                onConfirm(newText)
                Log.d(TAG, "✅ 修改成功：$newText")
            }
            .setNegativeButton("取消", null)
            .create()

        try {
            dialog.show()
            Log.d(TAG, "✅ 对话框显示成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 对话框显示失败", e)
        }
    }
}
