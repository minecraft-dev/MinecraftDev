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

public class MixedInModifyExpressionValue {
    public void method1() {
        System.out.println(42);
        System.out.println(101);
        System.out.println(callee1());
        System.out.println(callee2());
        System.out.println(callee5());
    }

    public void method2(String a, long b) {
        System.out.println('a');
        System.out.println(callee3());
        System.out.println(callee4());
    }

    private static char callee1() {
        return 'a';
    }

    private static int callee2() {
        return 1;
    }

    private static Long[] callee3() {
        return new Long[0];
    }

    private static Double[] callee4() {
        return new Double[0];
    }

    private static short callee5() {
        return (short) 1;
    }
}
