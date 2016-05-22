package com.demonwav.mcdev.platform.forge.versionapi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Nullable
    public List<Integer> getSnapshot(String version) {
        return get(version, "snapshot");
    }

    @Nullable
    public List<Integer> getStable(String version) {
        return get(version, "stable");
    }

    @Nullable
    private List<Integer> get(String version, String type) {
        Map<String, List<Integer>> versions = map.get(version);
        if (versions != null) {
            return versions.get(type);
        }
        return null;
    }
}
