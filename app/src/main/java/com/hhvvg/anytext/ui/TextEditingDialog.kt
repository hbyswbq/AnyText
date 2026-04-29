package com.hhvvg.anytext.ui

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.app.AlertDialog

object TextEditingDialog {
    fun show(context: Context, textView: TextView, onConfirm: (String) -> Unit) {
        val editText = EditText(context)
        editText.setText(textView.text.toString())

        runCatching {
            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog)
                .setTitle("修改文字")
                .setView(editText)
                .setPositiveButton("确定") { _, _ ->
                    onConfirm(editText.text.toString())
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}
