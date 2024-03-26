/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.mixintestdata.meExpression;

public class MEExpressionTestData {
    private static final SynchedData<Integer> STINGER_COUNT = null;
    private SynchedDataManager synchedData;

    public void complexFunction() {
        int one = 1;
        String local1 = "Hello";
        String local2 = "World";

        System.out.println(new StringBuilder(local1).append(", ").append(local2));
        System.out.println(one);

        InaccessibleType varOfInaccessibleType = new InaccessibleType();
        acceptInaccessibleType(varOfInaccessibleType);
        noArgMethod();

        String[] strings1 = new String[] { local1, local2 };
        String[] strings2 = new String[one];
    }

    private static void acceptInaccessibleType(InaccessibleType type) {
    }

    private static void noArgMethod() {
    }

    public int getStingerCount() {
        return (Integer) this.synchedData.get(STINGER_COUNT);
    }

    private static class InaccessibleType {

    }

    public static class SynchedDataManager {
        public <V> V get(SynchedData<V> data) {
            return null;
        }
    }

    public static class SynchedData<V> {
    }
}
