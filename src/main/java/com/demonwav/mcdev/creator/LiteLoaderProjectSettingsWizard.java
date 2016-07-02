package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.mcp.McpVersion;
import com.demonwav.mcdev.platform.mcp.McpVersionEntry;
import com.demonwav.mcdev.platform.liteloader.LiteLoaderProjectConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.WordUtils;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;

public class LiteLoaderProjectSettingsWizard extends MinecraftModuleWizardStep {
    private JPanel panel;
    private JLabel mcpWarning;
    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JComboBox<String> minecraftVersionBox;
    private JComboBox<McpVersionEntry> mcpVersionBox;
    private JProgressBar loadingBar;

    private LiteLoaderProjectConfiguration settings;
    private final MinecraftProjectCreator creator;

    private McpVersion mcpVersion;

    private final ActionListener mcpBoxActionListener = e -> {
        if (((McpVersionEntry) mcpVersionBox.getSelectedItem()).isRed()) {
            mcpWarning.setVisible(true);
        } else {
            mcpWarning.setVisible(false);
        }
    };

    public LiteLoaderProjectSettingsWizard(MinecraftProjectCreator creator, int index) {
        this.creator = creator;
        this.settings = (LiteLoaderProjectConfiguration) creator.getSettings().get(index);

        mcpWarning.setVisible(false);

        minecraftVersionBox.addActionListener(e -> {
            if (mcpVersion != null) {
                mcpVersion.setMcpVersion(mcpVersionBox, (String) minecraftVersionBox.getSelectedItem(), mcpBoxActionListener);
            }
        });

        pluginNameField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                String[] words = pluginNameField.getText().split("\\s+");
                String word = Arrays.stream(words).map(WordUtils::capitalize).collect(Collectors.joining());

                mainClassField.setText(creator.getGroupId() + '.' + creator.getArtifactId()
                        + ".LiteMod" + word);
            }
        });

        try {
            new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    mcpVersion = McpVersion.downloadData();
                    return null;
                }

                @Override
                protected void done() {
                    if (mcpVersion == null) {
                        return;
                    }

                    minecraftVersionBox.removeAllItems();

                    mcpVersion.getVersions().forEach(minecraftVersionBox::addItem);
                    // Always select most recent
                    minecraftVersionBox.setSelectedIndex(0);

                    if (mcpVersion != null) {
                        mcpVersion.setMcpVersion(mcpVersionBox, (String) minecraftVersionBox.getSelectedItem(), mcpBoxActionListener);
                    }

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
                + ".LiteMod" + WordUtils.capitalizeFully(creator.getArtifactId()));

        loadingBar.setIndeterminate(true);

        return panel;
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

        settings.mcpVersion = ((McpVersionEntry) mcpVersionBox.getSelectedItem()).getText();
    }

    @Override
    public void setIndex(int index) {
        this.settings = (LiteLoaderProjectConfiguration) creator.getSettings().get(index);
    }

    @Override
    public void updateDataModel() {}
}
