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
                .setTitle("修改聊天文本")
                .setView(editText)
                .setPositiveButton("确定") { _, _ ->
                    onConfirm(editText.text.toString())
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    // 全局重置弹窗
    fun showResetDialog(context: Context, onReset: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("重置修改")
            .setMessage("确定要恢复所有改过的文字吗？")
            .setPositiveButton("确定重置") { _, _ -> onReset() }
            .setNegativeButton("取消", null)
            .show()
    }
}
