package com.hbyswbq.anytext;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.hbyswbq.anytext.ui.TextEditingDialog;
import com.hbyswbq.anytext.wechat.WeChatMessageHook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 原有的全局TextView修改功能（保留不变）
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Application app = (Application) param.thisObject;
                
                app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                        hookAllTextViews(activity.getWindow().getDecorView());
                    }

                    @Override public void onActivityStarted(Activity activity) {}
                    @Override public void onActivityResumed(Activity activity) {}
                    @Override public void onActivityPaused(Activity activity) {}
                    @Override public void onActivityStopped(Activity activity) {}
                    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
                    @Override public void onActivityDestroyed(Activity activity) {
                        // 清理微信消息缓存
                        if (lpparam.packageName.equals("com.tencent.mm")) {
                            WeChatMessageHook.INSTANCE.clearCache();
                        }
                    }
                });
            }
        });

        // 添加微信专属消息修改功能
        if (lpparam.packageName.equals("com.tencent.mm")) {
            WeChatMessageHook.INSTANCE.hookWeChatMessages(lpparam.classLoader);
        }
    }

    // 原有的全局TextView Hook方法（保留不变）
    private void hookAllTextViews(View view) {
        if (view instanceof TextView) {
            final TextView textView = (TextView) view;
            textView.setOnClickListener(v -> {
                TextEditingDialog.show(v.getContext(), textView.getText().toString(), newText -> {
                    textView.setText(newText);
                });
            });
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                hookAllTextViews(viewGroup.getChildAt(i));
            }
        }
    }
}
