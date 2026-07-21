/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectUtil {

    private ReflectUtil() {
    }

    public static Object getProperty(Object obj, String name) throws Exception {
        if (obj == null) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);

        // Try getProperty()
        try {
            Method m = clazz.getMethod("get" + capitalized);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (NoSuchMethodException ignored) {
        }

        // Try isProperty()
        try {
            Method m = clazz.getMethod("is" + capitalized);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (NoSuchMethodException ignored) {
        }

        // Try property()
        try {
            Method m = clazz.getMethod(name);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (NoSuchMethodException ignored) {
        }

        // Try field
        try {
            Field f = clazz.getField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (NoSuchFieldException ignored) {
        }

        throw new NoSuchMethodException("Could not find property or method '" + name + "' on " + clazz);
    }

    public static boolean hasProperty(Object obj, String name) {
        if (obj == null) {
            return false;
        }
        Class<?> clazz = obj.getClass();
        String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);

        try {
            clazz.getMethod("get" + capitalized);
            return true;
        } catch (NoSuchMethodException ignored) {
        }

        try {
            clazz.getMethod("is" + capitalized);
            return true;
        } catch (NoSuchMethodException ignored) {
        }

        try {
            clazz.getMethod(name);
            return true;
        } catch (NoSuchMethodException ignored) {
        }

        try {
            clazz.getField(name);
            return true;
        } catch (NoSuchFieldException ignored) {
        }

        return false;
    }

    public static Object callMethod(Object obj, String methodName, Object... args) throws Exception {
        if (obj == null) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        Method bestMatch = null;
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                if (isCompatible(method.getParameterTypes(), args)) {
                    bestMatch = method;
                    break;
                }
            }
        }
        if (bestMatch == null) {
            throw new NoSuchMethodException("No compatible method " + methodName + " with " + args.length + " arguments found on " + clazz);
        }
        bestMatch.setAccessible(true);
        return bestMatch.invoke(obj, args);
    }

    private static boolean isCompatible(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args[i];
            Class<?> paramType = parameterTypes[i];
            if (arg == null) {
                if (paramType.isPrimitive()) {
                    return false;
                }
                continue;
            }
            if (paramType.isPrimitive()) {
                if (paramType == boolean.class && arg instanceof Boolean) continue;
                if (paramType == int.class && arg instanceof Integer) continue;
                if (paramType == long.class && arg instanceof Long) continue;
                if (paramType == double.class && arg instanceof Double) continue;
                if (paramType == float.class && arg instanceof Float) continue;
                if (paramType == char.class && arg instanceof Character) continue;
                if (paramType == byte.class && arg instanceof Byte) continue;
                if (paramType == short.class && arg instanceof Short) continue;
                return false;
            }
            if (!paramType.isInstance(arg)) {
                return false;
            }
        }
        return true;
    }
}
