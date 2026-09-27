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

public class MixedInComplex {
    public void method1(String arg) {
        int local1 = 0;
        CharSequence local2 = null;
        char local3 = 'a';
        Integer local4 = 10;
    }

    public void method2(int arg) {
        int local1 = 0;
    }

    public String method3(Integer arg) {
        return arg.toString();
    }

    public Integer method4() {
        return 1;
    }

    public void method5(CharSequence arg) {
        int local1 = 0;
        String local2 = null;
        int local3 = 0;
        int local4 = 0;
    }
}
