package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.forge.versionapi.ForgeVersion;
import com.demonwav.mcdev.platform.forge.versionapi.McpVersion;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.WordUtils;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.util.List;

public class ForgeProjectSettingsWizard extends ModuleWizardStep {

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

    private final ForgeProjectConfiguration settings = new ForgeProjectConfiguration();
    private final MinecraftProjectCreator creator;

    private McpVersion mcpVersion;
    private ForgeVersion forgeVersion;

    public ForgeProjectSettingsWizard(MinecraftProjectCreator creator) {
        this.creator = creator;
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
                    mcpVersion.getVersions().stream().sorted((one, two) -> one.compareTo(two) * -1).forEach(minecraftVersionBox::addItem);
                    String recommended = forgeVersion.getRecommended(mcpVersion.getVersions());

                    int index = 0;
                    for (int i = 0; i < minecraftVersionBox.getItemCount(); i++) {
                        if (minecraftVersionBox.getItemAt(i).equals(recommended)) {
                            index = i;
                        }
                    }
                    minecraftVersionBox.setSelectedIndex(index);

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
        mainClassField.setText(this.creator.getGroupId() + '.' + this.creator.getArtifactId()
                + '.' + WordUtils.capitalizeFully(this.creator.getArtifactId()));

        loadingBar.setIndeterminate(true);

        return panel;
    }

    private void setMcpVersion() {
        if (mcpVersion == null) {
            return;
        }

        String version = (String) minecraftVersionBox.getSelectedItem();

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

        String version = (String) minecraftVersionBox.getSelectedItem();

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
                    if (((String) forgeVersionBox.getItemAt(i)).endsWith(String.valueOf(promo.intValue()))) {
                        index = i;
                    }
                } catch (NumberFormatException ignored) {}
            }
            forgeVersionBox.setSelectedIndex(index);
        }
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
        settings.forgeVersion = forgeVersion.getFullVersion((String) forgeVersionBox.getSelectedItem());

        creator.setSettings(settings);
    }

    @Override
    public void updateDataModel() {}
}
