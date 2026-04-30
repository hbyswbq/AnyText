package com.hhvvg.anytext.hook

import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AnyHookLoaded : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 只处理微信
        if (lpparam.packageName != "com.tencent.mm") return

        // 只要触摸屏幕就弹Toast，确认钩子生效
        XposedHelpers.findAndHookMethod(
            "android.view.View",
            lpparam.classLoader,
            "dispatchTouchEvent",
            android.view.MotionEvent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Toast.makeText(
                        param.thisObject.context,
                        "✅ AnyText模块已生效",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
