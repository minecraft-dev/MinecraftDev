package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.forge.versionapi.ForgeVersion;
import com.demonwav.mcdev.platform.forge.versionapi.McpVersion;
import com.demonwav.mcdev.platform.forge.versionapi.McpVersionEntry;
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.WordUtils;

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

    public boolean spongeForge = false;

    public ForgeProjectSettingsWizard(MinecraftProjectCreator creator, int index) {
        this.creator = creator;
        this.settings = (ForgeProjectConfiguration) creator.getSettings().get(index);

        generateDocsCheckbox.setVisible(false);
        mcpWarning.setVisible(false);

        minecraftVersionBox.addActionListener(e -> {
            setMcpVersion();
            setForgeVersion();
        });

        try {
            new SwingWorker() {
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
                    if (!spongeForge) {
                        forgeVersion.getSortedMcVersions().forEach(minecraftVersionBox::addItem);
                        String recommended = forgeVersion.getRecommended(mcpVersion.getVersions());

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
                    }

                    setMcpVersion();

                    if (forgeVersion == null) {
                        return;
                    }

                    setForgeVersion();

                    loadingBar.setIndeterminate(false);
                    loadingBar.setVisible(false);
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public JComponent getComponent() {
        pluginNameField.setText(WordUtils.capitalize(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());

        if (creator.index != 0) {
            pluginNameField.setEditable(false);
            pluginVersionField.setEditable(false);
        }

        mainClassField.setText(this.creator.getGroupId().toLowerCase() + '.' + this.creator.getArtifactId().toLowerCase()
                + '.' + WordUtils.capitalize(this.creator.getArtifactId()));

        if (creator.getSettings().size() > 1) {
            mainClassField.setText(mainClassField.getText() + creator.getSettings().get(creator.index).type.getNormalName());
        }

        loadingBar.setIndeterminate(true);

        if (spongeForge) {
            if (UIUtil.isUnderDarcula()) {
                title.setIcon(PlatformAssets.SPONGE_FORGE_ICON_2X);
            } else {
                title.setIcon(PlatformAssets.SPONGE_FORGE_ICON_DARK_2X);
            }
            title.setText("<html><font size=\"5\">Sponge Forge Settings</font></html>");
            generateDocsCheckbox.setVisible(true);

            minecraftVersionLabel.setText("    Sponge API");
        }

        return panel;
    }

    private void setMcpVersion() {
        if (mcpVersion == null) {
            return;
        }

        String version = getVersion();

        mcpVersionBox.removeActionListener(mcpBoxActionListener);
        mcpVersionBox.removeAllItems();

        Pair<List<Integer>, List<Integer>> stable = mcpVersion.getStable(version);
        stable.getFirst().stream().sorted((one, two) -> one.compareTo(two) * -1).map(s -> new McpVersionEntry("stable_" + s)).forEach(mcpVersionBox::addItem);

        Pair<List<Integer>, List<Integer>> snapshot = mcpVersion.getSnapshot(version);
        snapshot.getFirst().stream().sorted((one, two) -> one.compareTo(two) * -1).map(s -> new McpVersionEntry("snapshot_" + s)).forEach(mcpVersionBox::addItem);

        // The "seconds" in the pairs are bad, but still available to the user
        // We will color them read

        stable.getSecond().stream().sorted((one, two) -> one.compareTo(two) * -1).map(s -> new McpVersionEntry("stable_" + s, true)).forEach(mcpVersionBox::addItem);
        snapshot.getSecond().stream().sorted((one, two) -> one.compareTo(two) * -1).map(s -> new McpVersionEntry("snapshot_" + s, true)).forEach(mcpVersionBox::addItem);

        mcpVersionBox.addActionListener(mcpBoxActionListener);
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
        if (!spongeForge) {
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
        return validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField) && !loadingBar.isVisible();
    }

    @Override
    public void onStepLeaving() {
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

    @Override
    public void setIndex(int index) {
        this.settings = (ForgeProjectConfiguration) creator.getSettings().get(index);
    }
}
