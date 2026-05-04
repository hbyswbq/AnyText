package com.hbyswbq.anytext.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.hbyswbq.anytext.R;

public class TextEditingDialog {

    public interface OnTextEditedListener {
        void onTextEdited(String newText);
    }

    public static void show(Context context, String originalText, final OnTextEditedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_text_edit, null);
        final EditText editText = view.findViewById(R.id.edit_text);
        editText.setText(originalText);
        editText.setSelection(originalText.length());

        new AlertDialog.Builder(context)
                .setTitle("编辑文本")
                .setView(view)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newText = editText.getText().toString();
                    listener.onTextEdited(newText);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
