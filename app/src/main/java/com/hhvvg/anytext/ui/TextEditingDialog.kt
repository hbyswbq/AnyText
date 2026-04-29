package com.hhvvg.anytext.ui

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.app.AlertDialog

object TextEditingDialog {
    fun show(context: Context, textView: TextView, onConfirm: (String) -> Unit) {
        val edit = EditText(context)
        edit.setText(textView.text)
        edit.setSelection(textView.text.length)

        AlertDialog.Builder(context)
            .setTitle("修改文字")
            .setView(edit)
            .setPositiveButton("确定") { _, _ ->
                onConfirm(edit.text.toString())
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
