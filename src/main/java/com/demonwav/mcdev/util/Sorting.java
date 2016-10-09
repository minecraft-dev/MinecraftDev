/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public final class Sorting {

    private Sorting() {
    }

    public static final Comparator<int[]> LEXICOGRAPHICAL_ORDER = (one, two) -> {
        int length = Math.min(one.length, two.length);
        for (int i = 0; i < length; i++) {
            int first = one[i];
            int second = two[i];

            if (first < second) {
                return -1;
            } else if (second < first) {
                return 1;
            }
        }

        // We've got here so they are now equal, if one has more then they are not equal
        if (one.length < two.length) {
            return -1;
        } else if (two.length < one.length) {
            return 1;
        }
        // They are the same
        return 0;
    };

    public static final Comparator<int[]> REVERSE_LEXICOGRAPHICAL_ORDER = (one, two) -> LEXICOGRAPHICAL_ORDER.compare(two, one);

    /**
     * This is the lowest version value we will let users choose, to make our lives easier.
     */
    private static final int[] ARRAY_1_8_8 = new int[] { 1, 8, 8 };

    @NotNull
    public static List<String> sortVersions(@NotNull Collection<?> versions) {
        // Populate a list of the keys (and cast them to String) so they can be sorted
        List<String> list = new ArrayList<>(versions.size());
        list.addAll(versions.stream().map(Object::toString).collect(Collectors.toList()));

        // We map each version string (1.2, 1.9.4, 1.10, etc) to an array of integers {1, 2}, {1, 9, 4}, {1, 10} so we
        // can lexicographically order them. We throw out the odd-balls in the process (like 1.10-pre4)
        List<int[]> intList = list.stream().distinct().map(s -> {
            try {
                return Stream.of(s.split("\\.")).mapToInt(Integer::parseInt).toArray();
            } catch (NumberFormatException e) {
                return null;
            }
        }).filter(array -> array != null).collect(Collectors.toList());

        // Sort them correctly
        intList.sort(Sorting.REVERSE_LEXICOGRAPHICAL_ORDER);
        intList.removeIf(ints -> Sorting.LEXICOGRAPHICAL_ORDER.compare(ints, ARRAY_1_8_8) < 0);

        return intList.stream().map(ints -> Arrays.stream(ints).mapToObj(String::valueOf).collect(Collectors.joining(".")))
            .collect(Collectors.toList());
    }
}
