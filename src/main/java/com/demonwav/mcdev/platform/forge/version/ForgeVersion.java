/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.forge.version;

import com.demonwav.mcdev.util.Sorting;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeVersion {

    @NotNull
    private Map<?, ?> map = new HashMap<>();

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
        Map promos = (Map) map.get("promos");
        if (promos != null) {
            return (Double) promos.get(version + "-recommended");
        }
        return null;
    }

    public List<String> getForgeVersions(String version) {
        List<String> list = new ArrayList<>();
        Map<?, ?> numbers = (Map) map.get("number");
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
        Map<?, ?> numbers = (Map<?, ?>) map.get("number");
        String[] parts = version.split("\\.");
        String versionSmall = parts[parts.length - 1];
        Map<?, ?> number = (Map<?, ?>) numbers.get(versionSmall);
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
