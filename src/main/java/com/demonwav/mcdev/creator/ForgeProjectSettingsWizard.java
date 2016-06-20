package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.forge.versionapi.ForgeVersion;
import com.demonwav.mcdev.platform.forge.versionapi.McpVersion;
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.WordUtils;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.util.List;

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
    private JComboBox<String> mcpVersionBox;
    private JProgressBar loadingBar;
    private JCheckBox generateDocsCheckbox;
    private JLabel minecraftVersionLabel;

    private ForgeProjectConfiguration settings;
    private final MinecraftProjectCreator creator;

    private McpVersion mcpVersion;
    private ForgeVersion forgeVersion;

    public boolean spongeForge = false;

    public ForgeProjectSettingsWizard(MinecraftProjectCreator creator, int index) {
        this.creator = creator;
        this.settings = (ForgeProjectConfiguration) creator.getSettings().get(index);
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
                        mcpVersion.getVersions().stream().sorted((one, two) -> one.compareTo(two) * -1).filter(s -> !s.equals("1.8") && !s.equals("1.7.10")).forEach(minecraftVersionBox::addItem);
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
        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());

        if (creator.index != 0) {
            pluginNameField.setEditable(false);
            pluginVersionField.setEditable(false);
        }

        mainClassField.setText(this.creator.getGroupId() + '.' + this.creator.getArtifactId()
                + '.' + WordUtils.capitalizeFully(this.creator.getArtifactId()));

        if (creator.getSettings().size() > 1) {
            mainClassField.setText(mainClassField.getText() + WordUtils.capitalizeFully(creator.getSettings().get(creator.index).type.name()));
        }

        loadingBar.setIndeterminate(true);

        if (spongeForge) {
            title.setIcon(PlatformAssets.SPONGE_FORGE_ICON_2X);
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

        mcpVersionBox.removeAllItems();
        List<Integer> stable = mcpVersion.getStable(version);
        if (stable == null) {
            return;
        }

        stable.stream().sorted((one, two) -> one.compareTo(two) * -1).map(s -> "stable_" + s).forEach(mcpVersionBox::addItem);
        List<Integer> snapshot = mcpVersion.getSnapshot(version);
        if (snapshot == null) {
            return;
        }

        snapshot.stream().sorted((one, two) -> one.compareTo(two) * -1).map(s -> "snapshot_" + s).forEach(mcpVersionBox::addItem);
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
                version = "1.9.4";
            }
        }
        return version;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        try {
            if (pluginNameField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("empty", pluginNameField);
            }

            if (pluginVersionField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("empty", pluginVersionField);
            }

            if (mainClassField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("empty", mainClassField);
            }
            if (!authorsField.getText().matches(ProjectSettingsWizardStep.pattern)) {
                throw new MinecraftSetupException("bad", authorsField);
            }

            if (!dependField.getText().matches(ProjectSettingsWizardStep.pattern)) {
                throw new MinecraftSetupException("bad", dependField);
            }
        } catch (MinecraftSetupException e) {
            String message = e.getError();
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                    .setFadeoutTime(4000)
                    .createBalloon()
                    .show(RelativePoint.getSouthWestOf(e.getJ()), Balloon.Position.below);
            return false;
        }
        return !loadingBar.isVisible();
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

        settings.mcpVersion = (String) mcpVersionBox.getSelectedItem();

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
