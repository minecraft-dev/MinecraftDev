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

public abstract class MixedInWrapOperation implements Comparable<MixedInWrapOperation> {
    public String test;
    public char test2;

    public void caller(char c) {
        callee1("hello", 1);
        callee2('a', (short) 3, 4);
        callee3(1, 2, 3);
        this.test = "hello";
        this.test2 = 'a';
        System.out.println(this.test2);
        System.out.println(Integer.valueOf(5).intValue());
        System.out.println((Object) 5 instanceof Integer);
    }

    public void caller2(int arg) {
        System.out.println(arg == 3);
        System.out.println(arg == 4);
        callee4('a', 'b');
        callee5(1, (short) 2);
        callee6((short) 1, 2);
        callee7((short) 1, true);
    }

    public void caller3(Object arg) {
        callee8(arg);
        callee9(arg, arg);
    }

    private void callee1(String x, int y) {
    }

    private void callee2(char w, short x, int y) {
    }

    private void callee3(int a, int b, int c) {
    }

    private static int callee4(char a, char b) {
        return 0;
    }

    private static boolean callee5(int a, short b) {
        return true;
    }

    private static boolean callee6(short a, int b) {
        return true;
    }

    private static boolean callee7(short a, boolean b) {
        return true;
    }

    private static void callee8(Object arg) {
    }

    private static void callee9(Object arg, Object arg2) {
    }
}
