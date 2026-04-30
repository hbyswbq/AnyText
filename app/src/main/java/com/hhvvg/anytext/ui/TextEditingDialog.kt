package com.hhvvg.anytext.ui

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.app.AlertDialog

object TextEditingDialog {

    fun show(
        context: Context,
        textView: TextView,
        onEdit: (String) -> Unit,
        onReset: () -> Unit
    ) {
        val editText = EditText(context)
        editText.setText(textView.text.toString())

        runCatching {
            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog)
                .setTitle("修改微信文本")
                .setView(editText)
                .setPositiveButton("确定修改") { _, _ ->
                    onEdit(editText.text.toString())
                }
                .setNeutralButton("全部重置") { _, _ ->
                    onReset()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}
