/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util;

import java.util.Objects;

/**
 * Represents a key to be used to return a data object of type T.
 * @param <T> The type to return.
 */
public abstract class Key<T> {
    public String getKey() {
        return getClass().getCanonicalName();
    }

    @Override
    public String toString() {
        return getKey();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getKey());
    }

    @Override
    public boolean equals(Object o) {
        return (o == this) || ((o instanceof Key) && ((Key) o).getKey().equals(getKey()));
    }
}
