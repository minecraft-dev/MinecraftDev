package com.demonwav.mcdev.platform.forge.versionapi;

import com.google.common.base.Objects;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.xerces.impl.dv.xs.IntegerDV;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForgeVersion {

    @NotNull
    private Map map = new HashMap();

    private final Comparator<int[]> lexicographicalOrder = (one, two) -> {
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
    private final Comparator<int[]> reverseLexicographicalOrder = (one, two) -> lexicographicalOrder.compare(two, one);

    private final int[] ARRAY_1_8_8 = new int[] { 1, 8, 8 };

    private ForgeVersion() {}

    @Nullable
    public static ForgeVersion downloadData() {
        try (InputStream in = new URL("http://files.minecraftforge.net/maven/net/minecraftforge/forge/json").openStream()) {
            String text = IOUtils.toString(in);

            Map map = new Gson().fromJson(text, Map.class);
            ForgeVersion version = new ForgeVersion();
            version.map = map;
            return version;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getSortedMcVersions() {
        Map mcversion = (Map) map.get("mcversion");
        Set keySet = mcversion.keySet();
        // Populate a list of the keys (and cast them to String) so they can be sorted
        List<String> list = new ArrayList<>(keySet.size());
        for (Object key : keySet) {
            list.add((String) key);
        }

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
        intList.sort(reverseLexicographicalOrder);
        intList.removeIf(ints -> lexicographicalOrder.compare(ints, ARRAY_1_8_8) < 0);

        return intList.stream().map(ints -> Arrays.stream(ints).mapToObj(String::valueOf).collect(Collectors.joining("."))).collect(Collectors.toList());
    }

    @Nullable
    public String getRecommended(Set<String> versions) {
        String recommended = "1.7";
        for (String version : versions) {
            Double promo = getPromo(version);
            if (promo == null) {
                continue;
            }

            if (recommended.compareTo(version) < 0) {
                recommended = version;
            }
        }

        return recommended;
    }

    @Nullable
    public Double getPromo(String version) {
        Map promos = (Map) map.get("promos");
        if (promos != null) {
            return (Double) promos.get(version + "-recommended");
        }
        return null;
    }

    public List<String> getForgeVersions(String version) {
        List<String> list = new ArrayList<>();
        Map numbers = (Map) map.get("number");
        numbers.forEach((k, v) -> {
            if (v instanceof Map) {
                Map number = (Map) v;
                String currentVersion = (String) number.get("mcversion");

                if (Objects.equal(currentVersion, version)) {
                    list.add((String) number.get("version"));
                }
            }
        });
        return list;
    }

    @Nullable
    public String getFullVersion(String version) {
        Map numbers = (Map) map.get("number");
        String[] parts = version.split("\\.");
        String versionSmall = parts[parts.length - 1];
        Map number = (Map) numbers.get(versionSmall);
        if (number == null) {
            return null;
        }

        String branch = (String) number.get("branch");
        String mcVersion = (String) number.get("mcversion");
        String finalVersion = (String) number.get("version");
        if (branch == null) {
            return mcVersion + "-" + finalVersion;
        } else {
            return mcVersion + "-" + finalVersion + "-" + branch;
        }
    }
}
