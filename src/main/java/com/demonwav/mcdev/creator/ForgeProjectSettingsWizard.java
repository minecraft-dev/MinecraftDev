/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.forge.version.ForgeVersion;
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration;
import com.demonwav.mcdev.platform.mcp.version.McpVersion;
import com.demonwav.mcdev.platform.mcp.version.McpVersionEntry;
import com.intellij.openapi.options.ConfigurationException;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

public class ForgeProjectSettingsWizard extends MinecraftModuleWizardStep {

    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JPanel panel;
    private JLabel title;
    private JTextField descriptionField;
    private JTextField authorsField;
    private JTextField websiteField;
    private JTextField dependField;
    private JTextField updateUrlField;
    private JComboBox<String> minecraftVersionBox;
    private JComboBox<String> forgeVersionBox;
    private JComboBox<McpVersionEntry> mcpVersionBox;
    private JProgressBar loadingBar;
    private JCheckBox generateDocsCheckbox;
    private JLabel minecraftVersionLabel;
    private JLabel mcpWarning;

    private ForgeProjectConfiguration settings;
    private final MinecraftProjectCreator creator;

    private McpVersion mcpVersion;
    private ForgeVersion forgeVersion;

    private final ActionListener mcpBoxActionListener = e -> {
        if (((McpVersionEntry) mcpVersionBox.getSelectedItem()).isRed()) {
            mcpWarning.setVisible(true);
        } else {
            mcpWarning.setVisible(false);
        }
    };

    private ForgeWorker apiWorker = new ForgeWorker();

    public ForgeProjectSettingsWizard(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;

        generateDocsCheckbox.setVisible(false);
        mcpWarning.setVisible(false);

        minecraftVersionBox.addActionListener(e -> {
            if (mcpVersion != null) {
                mcpVersion.setMcpVersion(mcpVersionBox, getVersion(), mcpBoxActionListener);
            }
            setForgeVersion();
        });
    }

    @Override
    public JComponent getComponent() {
        settings = (ForgeProjectConfiguration) creator.getSettings().get(PlatformType.FORGE);
        if (settings == null) {
            return null;
        }

        pluginNameField.setText(WordUtils.capitalize(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());

        if (settings != null && !settings.isFirst) {
            pluginNameField.setEditable(false);
            pluginVersionField.setEditable(false);
        }

        mainClassField.setText(this.creator.getGroupId().toLowerCase() + '.' + this.creator.getArtifactId().toLowerCase()
                + '.' + WordUtils.capitalize(this.creator.getArtifactId()));

        if (creator.getSettings().size() > 1) {
            mainClassField.setText(mainClassField.getText() + PlatformType.FORGE.getNormalName());
        }

        loadingBar.setIndeterminate(true);

        if (settings instanceof SpongeForgeProjectConfiguration) {
            title.setIcon(PlatformAssets.SPONGE_FORGE_ICON_2X);
            title.setText("<html><font size=\"5\">SpongeForge Settings</font></html>");
            generateDocsCheckbox.setVisible(true);

            minecraftVersionLabel.setText("    Sponge API");
        }

        apiWorker.execute();

        return panel;
    }

    @Override
    public void updateStep() {
        if ((forgeVersion == null || mcpVersion == null) && (apiWorker.isCancelled() || apiWorker.isDone())) {
            // A SwingWorker will only run once, so we need to create a new instance
            apiWorker = new ForgeWorker();
            apiWorker.execute();
        }
    }

    private void setForgeVersion() {
        if (forgeVersion == null) {
            return;
        }

        String version = getVersion();

        if (version == null) {
            return;
        }

        forgeVersionBox.removeAllItems();
        List<String> versions = forgeVersion.getForgeVersions(version);

        if (versions == null) {
            return;
        }
        versions.stream().sorted((one, two) -> one.compareTo(two) * -1).forEach(forgeVersionBox::addItem);

        Double promo = forgeVersion.getPromo(version);
        if (promo != null) {
            int index = 0;
            for (int i = 0; i < forgeVersionBox.getItemCount(); i++) {
                try {
                    if (forgeVersionBox.getItemAt(i).endsWith(String.valueOf(promo.intValue()))) {
                        index = i;
                    }
                } catch (NumberFormatException ignored) {}
            }
            forgeVersionBox.setSelectedIndex(index);
        }
    }

    private String getVersion() {
        String version;
        if (!(settings instanceof SpongeForgeProjectConfiguration)) {
            version = (String) minecraftVersionBox.getSelectedItem();
        } else {
            if (minecraftVersionBox.getSelectedItem().equals("4.1.0")) {
                version = "1.8.9";
            } else {
                version = "1.10.2";
            }
        }
        return version;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField, pattern) && !loadingBar.isVisible();
    }

    @Override
    public boolean isStepVisible() {
        settings = (ForgeProjectConfiguration) creator.getSettings().get(PlatformType.FORGE);
        return settings != null;
    }

    @Override
    public void onStepLeaving() {
        if (loadingBar.isVisible()) {
            // we're in a cancel state
            apiWorker.cancel(true);
            return;
        }

        settings.pluginName = pluginNameField.getText();
        settings.pluginVersion = pluginVersionField.getText();
        settings.mainClass = mainClassField.getText();

        settings.setAuthors(authorsField.getText());
        settings.setDependencies(dependField.getText());
        settings.description = descriptionField.getText();
        settings.website = websiteField.getText();
        settings.updateUrl = updateUrlField.getText();

        settings.mcpVersion = ((McpVersionEntry) mcpVersionBox.getSelectedItem()).getText();

        if (settings instanceof SpongeForgeProjectConfiguration) {
            SpongeForgeProjectConfiguration configuration = (SpongeForgeProjectConfiguration) settings;
            configuration.generateDocumentation = generateDocsCheckbox.isSelected();
            configuration.spongeApiVersion = (String) minecraftVersionBox.getSelectedItem();
        }

        // If an error occurs while fetching the API, this may prevent the user from closing the dialog.
        try {
            settings.forgeVersion = forgeVersion.getFullVersion((String) forgeVersionBox.getSelectedItem());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateDataModel() {}

    private class ForgeWorker extends SwingWorker<Object, Object> {

        @Override
        protected Object doInBackground() throws Exception {
            mcpVersion = McpVersion.downloadData();
            forgeVersion = ForgeVersion.downloadData();
            return null;
        }

        @Override
        protected void done() {
            if (mcpVersion == null) {
                return;
            }

            minecraftVersionBox.removeAllItems();

            // reverse order the versions
            if (!(settings instanceof SpongeForgeProjectConfiguration)) {
                forgeVersion.getSortedMcVersions().forEach(minecraftVersionBox::addItem);
                final String recommended = forgeVersion.getRecommended(mcpVersion.getVersions());

                int index = 0;
                for (int i = 0; i < minecraftVersionBox.getItemCount(); i++) {
                    if (minecraftVersionBox.getItemAt(i).equals(recommended)) {
                        index = i;
                    }
                }
                minecraftVersionBox.setSelectedIndex(index);
            } else {
                minecraftVersionBox.addItem("4.1.0");
                minecraftVersionBox.addItem("5.0.0");
                minecraftVersionBox.setSelectedIndex(1);
            }

            if (mcpVersion != null) {
                mcpVersion.setMcpVersion(mcpVersionBox, getVersion(), mcpBoxActionListener);
            }

            if (forgeVersion == null) {
                return;
            }

            setForgeVersion();

            loadingBar.setIndeterminate(false);
            loadingBar.setVisible(false);
        }
    }
}
