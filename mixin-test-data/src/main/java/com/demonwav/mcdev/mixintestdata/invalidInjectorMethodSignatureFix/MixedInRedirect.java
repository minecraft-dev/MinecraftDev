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

package com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix;

public abstract class MixedInRedirect implements Comparable<MixedInRedirect> {
    public String test;
    public char test2;

    public void caller(char c) {
        callee1("hello", 1);
        callee2('a', (short) 3, 4);
        callee3(1, 2, 3);
        callee4("hello", void.class);
        callee5("hello", void.class);
        this.test = "hello";
        this.test2 = 'a';
        System.out.println(this.test2);
        System.out.println(Integer.valueOf(5).intValue());
        System.out.println((Object) 5 instanceof Integer);
    }

    public void caller2(String arg) {
        System.out.println((Object) 5 instanceof Integer);
    }

    private void callee1(String x, int y) {
    }

    private void callee2(char w, short x, int y) {
    }

    private void callee3(int a, int b, int c) {
    }

    private static Class<?> callee4(String instance, Class<?> clazz) {
        return clazz;
    }

    private static Class<?> callee5(String instance, Object clazz) {
        return null;
    }
}
