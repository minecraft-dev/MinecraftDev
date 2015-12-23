/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.pluginyaml;

import com.demonwav.bukkitplugin.BukkitProject;
import com.demonwav.bukkitplugin.util.BukkitUtil;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLArray;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.YAMLScalarList;
import org.jetbrains.yaml.psi.YAMLScalarText;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.impl.YAMLFileImpl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PluginConfigManager {

    @NotNull
    private BukkitProject project;
    @NotNull
    private PluginConfig config;

    public PluginConfigManager(@NotNull BukkitProject project) {
        // TODO: This doesn't setup when a project is opened
        this.project = project;
        this.config = new PluginConfig(project);

        importConfig();

        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event) {
                // TODO: Make this check more substantial
                if (event.getFile().getName().equals("plugin.yml")) {
                    importConfig();
                    System.out.println(config.toString());
                }
            }
        });
    }

    @NotNull
    public PluginConfig getConfig() {
        return config;
    }

    private void importConfig() {
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiFile pluginYml = BukkitUtil.getPluginYml(project);
            if (BukkitUtil.isUltimate()) {
                YAMLFile file = ((YAMLFileImpl) pluginYml);

                if (file == null)
                    return; // TODO: Show warning to user

                // TODO: Show warning to user if there is more than one document
                YAMLDocument document = file.getDocuments().get(0);
                document.getYAMLElements().forEach(e -> {
                    if (!(e instanceof YAMLKeyValue)) {
                        // TODO: Show warning to user, this would be invalid in a plugin.yml
                        return;
                    }

                    YAMLKeyValue keyValue = ((YAMLKeyValue) e);
                    String key = keyValue.getKeyText();

                    switch (key) {
                        // Single string values
                        case "name":
                        case "version":
                        case "description":
                        case "author":
                        case "website":
                        case "prefix":
                            handleSingleValue(key, keyValue);
                            break;
                        // List values
                        case "authors":
                        case "depend":
                        case "softdepend":
                        case "loadbefore":
                            handleListValue(key, keyValue);
                            break;
                        case "load":
                            // TODO: handle this enum & verify the value is actually valid and warn if not
                            break;
                        case "main":
                            handleSingleValue("main", keyValue);
                            // TODO: verify this is a proper class that exists and extends JavaPlugin
                            break;
                        case "database":
                            if (keyValue.getValueText().matches("y|Y|yes|Yes|YES|n|N|no|No|NO|true|True|TRUE|false|False|FALSE|on|On|ON|off|Off|OFF")) {
                                handleSingleValue(key, keyValue);
                            } else {
                                // TODO: show warning to user, must be a boolean value
                                setValueInConfig(key, false);
                            }
                            break;
                        case "commands":
                            // TODO: handle commands
                            break;
                        case "permissions":
                            // TODO: handle permissions
                            break;
                        default:
                            // TODO: Show warning to user, invalid field in plugin.yml
                            break;
                    }

                    // Temp code for testing
                    System.out.println(e.getClass().getSimpleName());
                    if (e.getYAMLElements().size() != 0)
                        printYamlEles(e.getYAMLElements(), 1);
                });
                System.out.println();
            }
        });
    }

    private void handleSingleValue(String name, YAMLKeyValue keyValue) {
        if (keyValue.getYAMLElements().size() > 1) {
            /*
             * TODO: Show warning to user. This would only be valid as 1 if The only child is a YAMLScalarList or
             * YAMLScalarText, which we will check for below.
             */
            return;
        }

        if ((keyValue.getYAMLElements().size() == 1) && (keyValue.getYAMLElements().get(0) instanceof YAMLScalarList ||
                keyValue.getYAMLElements().get(0) instanceof YAMLScalarText)) {
            // Handle scalar lists here. We will just concatenate them into a string using new lines.
            String lines[] = keyValue.getYAMLElements().get(0).getText().split("\\n");
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < lines.length; i++) {
                sb.append(lines[i].trim());
                if (i + 1 == lines.length) {
                    if (keyValue.getYAMLElements().get(0) instanceof YAMLScalarList) {
                        sb.append("\n");
                    } else {
                        sb.append(" ");
                    }
                }
            }
            setValueInConfig(name, sb.toString());
            return;
        }

        if (keyValue.getYAMLElements().size() > 0) {
            /*
             * TODO: Show warning to user. This would never be valid
             */
            return;
        }

        if (keyValue.getValueText().matches("y|Y|yes|Yes|YES|n|N|no|No|NO|true|True|TRUE|false|False|FALSE|on|On|ON|off|Off|OFF")) {
            if (keyValue.getValueText().matches("y|Y|yes|Yes|YES|true|True|TRUE|on|On|ON")) {
                setValueInConfig(name, true);
            } else {
                setValueInConfig(name, false);
            }
        } else {
            setValueInConfig(name, keyValue.getValueText());
        }
    }

    private void handleListValue(String name, YAMLKeyValue keyValue) {
        if (keyValue.getYAMLElements().size() != 1) {
            // TODO: Show warning to user, should be YAMLCompoundValue with YAMLArray or YAMLSequence child
            return;
        }

        if (!(keyValue.getYAMLElements().get(0) instanceof YAMLCompoundValue)) {
            // TODO: Show warning to user, should be YAMLCompoundValue with YAMLArray or YAMLSequence child
            return;
        }

        YAMLCompoundValue compoundValue = (YAMLCompoundValue) keyValue.getYAMLElements().get(0);

        if (compoundValue.getYAMLElements().size() == 0) {
            // TODO: Show warning to user, should be YAMLArray or YAMLSequence child
            return;
        }

        if (compoundValue.getYAMLElements().get(0) instanceof YAMLArray) {
            if (compoundValue.getYAMLElements().size() > 1) {
                // TODO: Show warning to user, can only be one YAMLArray
                return;
            }
            YAMLArray array = (YAMLArray) compoundValue.getYAMLElements().get(0);
            List<String> text = Arrays.stream(array.getText().replaceAll("\\[|\\]", "").split(", ")).collect(Collectors.toList());
            setValueInConfig(name, text);
            return;
        } else if (compoundValue.getYAMLElements().stream().allMatch(element -> element instanceof YAMLSequence)) {
            List<String> text = compoundValue.getYAMLElements().stream().map(PsiElement::getText).map(s -> s.replaceAll("\\-\\s+|\\n", "")).collect(Collectors.toList());
            setValueInConfig(name, text);
            return;
        }

        // TODO: Show warning to user, the list was not valid
    }

    private boolean setValueInConfig(String name, Object value) {
        try {
            Field field = PluginConfig.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(config, value);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            // This shouldn't happen, as we are setting it to accessible before doing anything
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Temp method for testing purposes
    private void printYamlEles(List<YAMLPsiElement> elementList, int indent) {
        elementList.forEach(e -> {
            for (int i = 0; i < indent; i++) {
                System.out.print("    ");
            }
            System.out.println(e.getClass().getSimpleName());
            if (e.getYAMLElements().size() != 0)
                printYamlEles(e.getYAMLElements(), indent + 1);
            if (e instanceof YAMLScalarList || e instanceof YAMLScalarText)
                System.out.println(e.getText());
        });
    }
}
