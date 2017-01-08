/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.version;

import com.demonwav.mcdev.util.Sorting;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.util.Pair;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComboBox;

@SuppressWarnings("WeakerAccess")
public class McpVersion {

    @NotNull
    private Map<String, Map<String, List<Integer>>> map = new HashMap<>();

    private McpVersion() {}

    @Nullable
    public static McpVersion downloadData() {
        try (InputStream in = new URL("http://export.mcpbot.bspk.rs/versions.json").openStream()) {
            String text = IOUtils.toString(in);

            final Type tokenType = new TypeToken<Map<String, Map<String, List<Integer>>>>() {}.getType();
            final Map<String, Map<String, List<Integer>>> map = new Gson().fromJson(text, tokenType);
            final McpVersion version = new McpVersion();
            version.map = map;
            return version;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    public List<String> getVersions() {
        return Sorting.sortVersions(map.keySet());
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
        final List<Integer> good = new ArrayList<>();
        final List<Integer> bad = new ArrayList<>();

        final Set<String> keySet = map.keySet();
        for (String key : keySet) {
            final Map<String, List<Integer>> versions = map.get(key);
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

    public void setMcpVersion(JComboBox<McpVersionEntry> mcpVersionBox, String version, ActionListener actionListener) {
        mcpVersionBox.removeActionListener(actionListener);
        mcpVersionBox.removeAllItems();

        final Pair<List<Integer>, List<Integer>> stable = getStable(version);
        stable.getFirst().stream().sorted((one, two) -> one.compareTo(two) * -1)
            .map(s -> new McpVersionEntry("stable_" + s)).forEach(mcpVersionBox::addItem);

        final Pair<List<Integer>, List<Integer>> snapshot = getSnapshot(version);
        snapshot.getFirst().stream().sorted((one, two) -> one.compareTo(two) * -1)
            .map(s -> new McpVersionEntry("snapshot_" + s)).forEach(mcpVersionBox::addItem);

        // The "seconds" in the pairs are bad, but still available to the user
        // We will color them read

        stable.getSecond().stream().sorted((one, two) -> one.compareTo(two) * -1)
            .map(s -> new McpVersionEntry("stable_" + s, true)).forEach(mcpVersionBox::addItem);
        snapshot.getSecond().stream().sorted((one, two) -> one.compareTo(two) * -1)
            .map(s -> new McpVersionEntry("snapshot_" + s, true)).forEach(mcpVersionBox::addItem);

        mcpVersionBox.addActionListener(actionListener);
    }
}
