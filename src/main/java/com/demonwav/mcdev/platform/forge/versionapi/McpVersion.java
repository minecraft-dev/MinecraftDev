package com.demonwav.mcdev.platform.forge.versionapi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.util.Pair;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;

public class McpVersion {

    @NotNull
    private Map<String, Map<String, List<Integer>>> map = new HashMap<>();

    private McpVersion() {}

    @Nullable
    public static McpVersion downloadData() {
        try (InputStream in = new URL("http://export.mcpbot.bspk.rs/versions.json").openStream()) {
            String text = IOUtils.toString(in);

            Type tokenType = new TypeToken<Map<String, Map<String, List<Integer>>>>() {}.getType();
            Map<String, Map<String, List<Integer>>> map = new Gson().fromJson(text, tokenType);
            McpVersion version = new McpVersion();
            version.map = map;
            return version;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    public Set<String> getVersions() {
        return map.keySet();
    }

    @NotNull
    public Pair<List<Integer>, List<Integer>> getSnapshot(String version) {
        return get(version, "snapshot");
    }

    @NotNull
    public Pair<List<Integer>, List<Integer>> getStable(String version) {
        return get(version, "stable");
    }

    @NotNull
    private Pair<List<Integer>, List<Integer>> get(String version, String type) {
        List<Integer> good = new ArrayList<>();
        List<Integer> bad = new ArrayList<>();

        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            Map<String, List<Integer>> versions = map.get(key);
            if (versions != null) {
                if (key.equals(version)) {
                    good.addAll(versions.get(type));
                } else {
                    bad.addAll(versions.get(type));
                }
            }
        }

        return new Pair<>(good, bad);
    }
}
