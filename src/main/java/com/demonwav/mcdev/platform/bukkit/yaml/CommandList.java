/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
public class CommandList {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    @NotNull
    private List<CommandNode> commands = new ArrayList<>();

    /**
     * Searches for and returns any commands that go by the provided name or are aliased with the provided name. Returns
     * an empty list if none are found. The search is not case sensitive.
     *
     * @param command Command to search for
     * @return Commands whose names or aliases match the given name, or an empty list if none are found
     */
    @NotNull
    public List<CommandNode> getCommand(@Nullable String command) {
        if (command == null) {
            return Collections.emptyList();
        }

        List<CommandNode> result = new ArrayList<>();
        for (CommandNode node : commands) {
            if (node.getName().equalsIgnoreCase(command)) {
                result.add(node);
                continue;
            }
            for (String alias : node.getAliases()) {
                if (alias.equalsIgnoreCase(command)) {
                    result.add(node);
                    break;
                }
            }
        }
        return result;
    }

    @NotNull
    public List<CommandNode> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public boolean addCommand(@NotNull CommandNode node) {
        return false;
    }

    public boolean removeCommand(@NotNull String name) {
        return false;
    }

    public boolean containsCommand(@NotNull String name) {
        return commands.stream().anyMatch(node ->
                node.getName().equalsIgnoreCase(name) ||
                        node.getAliases().stream().anyMatch(alias ->
                                alias.equalsIgnoreCase(name)
                        )
        );
    }
}
