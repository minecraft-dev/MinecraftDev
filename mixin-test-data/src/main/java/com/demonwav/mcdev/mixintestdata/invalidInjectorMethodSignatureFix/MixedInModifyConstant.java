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

public class MixedInModifyConstant {
    public void method1(Class<?> arg) {
        System.out.println(42);
        System.out.println(101);
        System.out.println(Long.class);
        System.out.println((Object) 5 instanceof Integer);
        System.out.println("hello");
    }

    public void method2(String arg) {
        System.out.println(Long.class);
        System.out.println((Object) 5 instanceof Integer);
        System.out.println((Object) null);
    }
}
