package com.hhvvg.anytext.ui

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.app.AlertDialog

object TextEditingDialog {
    fun show(context: Context, textView: TextView, onConfirm: (String) -> Unit) {
        val editText = EditText(context).apply {
            setText(textView.text)
            setSelection(textView.text.length)
        }

        AlertDialog.Builder(context)
            .setTitle("修改文字")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                onConfirm(editText.text.toString())
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
