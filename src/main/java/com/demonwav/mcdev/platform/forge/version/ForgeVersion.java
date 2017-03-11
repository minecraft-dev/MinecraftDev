/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.version;

import com.demonwav.mcdev.util.Sorting;
import com.google.common.base.Objects;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForgeVersion {

    @NotNull
    private Map<?, ?> map = new HashMap<>();

    private ForgeVersion() {}

    @Nullable
    public static ForgeVersion downloadData() {
        try (InputStream in = new URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/json").openStream()) {
            final String text = IOUtils.toString(in);

            final Map map = new Gson().fromJson(text, Map.class);
            final ForgeVersion version = new ForgeVersion();
            version.map = map;
            return version;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getSortedMcVersions() {
        final Map mcversion = (Map) map.get("mcversion");
        return Sorting.sortVersions(mcversion.keySet());
    }

    @Nullable
    public String getRecommended(List<String> versions) {
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
        final Map promos = (Map) map.get("promos");
        if (promos != null) {
            return (Double) promos.get(version + "-recommended");
        }
        return null;
    }

    public List<String> getForgeVersions(String version) {
        final List<String> list = new ArrayList<>();
        final Map<?, ?> numbers = (Map) map.get("number");
        numbers.forEach((k, v) -> {
            if (v instanceof Map) {
                final Map number = (Map) v;
                final String currentVersion = (String) number.get("mcversion");

                if (Objects.equal(currentVersion, version)) {
                    list.add((String) number.get("version"));
                }
            }
        });
        return list;
    }

    @Nullable
    public String getFullVersion(String version) {
        final Map<?, ?> numbers = (Map<?, ?>) map.get("number");
        final String[] parts = version.split("\\.");
        final String versionSmall = parts[parts.length - 1];
        final Map<?, ?> number = (Map<?, ?>) numbers.get(versionSmall);
        if (number == null) {
            return null;
        }

        final String branch = (String) number.get("branch");
        final String mcVersion = (String) number.get("mcversion");
        final String finalVersion = (String) number.get("version");
        if (branch == null) {
            return mcVersion + "-" + finalVersion;
        } else {
            return mcVersion + "-" + finalVersion + "-" + branch;
        }
    }
}
