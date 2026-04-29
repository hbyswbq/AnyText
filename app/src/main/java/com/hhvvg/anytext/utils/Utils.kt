package com.hhvvg.anytext.utils

import de.robv.android.xposed.XposedHelpers
import kotlin.reflect.KClass

object Utils {
    // 安全的反射获取字段方法，带类型检查
    fun <T> getObjectFieldSafe(obj: Any, fieldName: String, clazz: KClass<T>): T? {
        return try {
            val value = XposedHelpers.getObjectField(obj, fieldName)
            if (clazz.isInstance(value)) {
                @Suppress("UNCHECKED_CAST")
                value as T
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // 如果你之前的代码用了这个方法，保持兼容
    @Deprecated("Use getObjectFieldSafe instead")
    fun <T> getObjectField(obj: Any, fieldName: String): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            XposedHelpers.getObjectField(obj, fieldName) as T?
        } catch (e: Exception) {
            null
        }
    }
}
