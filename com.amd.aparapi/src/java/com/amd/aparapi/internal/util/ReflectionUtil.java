package com.amd.aparapi.internal.util;

import java.lang.reflect.Field;

public class ReflectionUtil {
    public static Field getFieldInHierarchy(String fieldName, Class<?> clazz) {
        if (clazz == null) return null;
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return getFieldInHierarchy(fieldName, clazz.getSuperclass());
        }
    }
}
