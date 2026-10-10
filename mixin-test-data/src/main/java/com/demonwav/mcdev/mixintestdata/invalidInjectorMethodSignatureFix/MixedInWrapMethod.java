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

import java.util.Collection;
import java.util.List;

public class MixedInWrapMethod {
    public void method1(String arg) {
    }

    public Integer method2() {
        return 1;
    }

    public void method3(CharSequence arg) {
    }

    public Long method4() {
        return 1L;
    }

    public Number method5(List<Integer> list, int b) {
        return null;
    }

    public Integer method6(Collection<Number> list, char c) {
        return null;
    }

    public static void method7(String arg) {
    }

    public static void method8(int arg) {
    }
}
