package com.hhvvg.anytext.ui

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object TextEditingDialog {

    fun show(context: Context, textView: TextView, onConfirm: (String) -> Unit) {
        val originalText = textView.text.toString()

        val editText = EditText(context).apply {
            setText(originalText)
            setSelection(originalText.length)
            hint = "请输入新文本"
        }

        AlertDialog.Builder(context)
            .setTitle("修改文本")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newText = editText.text.toString()
                onConfirm(newText)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
