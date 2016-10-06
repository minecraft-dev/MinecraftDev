package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.liteloader.LiteLoaderProjectConfiguration;
import com.demonwav.mcdev.platform.mcp.version.McpVersion;
import com.demonwav.mcdev.platform.mcp.version.McpVersionEntry;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

public class LiteLoaderProjectSettingsWizard extends MinecraftModuleWizardStep {

    private static final String LITEMOD = "LiteMod";
    private static final Pattern javaClassPattern = Pattern.compile("\\s+|-|\\$");

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

    private boolean mainClassModified = false;

    @NotNull
    private final ActionListener mcpBoxActionListener = e -> {
        if (((McpVersionEntry) mcpVersionBox.getSelectedItem()).isRed()) {
            mcpWarning.setVisible(true);
        } else {
            mcpWarning.setVisible(false);
        }
    };

    @NotNull
    private final DocumentListener listener = new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent e) {
            // Make sure they don't try to add spaces or whatever
            if (javaClassPattern.matcher(mainClassField.getText()).find()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    mainClassField.getDocument().removeDocumentListener(this);
                    ((AbstractDocument.DefaultDocumentEvent) e).undo();
                    mainClassField.getDocument().addDocumentListener(this);
                });
                return;
            }

            // We just need to make sure they aren't messing up the LiteMod text
            String[] words = mainClassField.getText().split("\\.");
            if (!words[words.length - 1].startsWith(LITEMOD)) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    mainClassField.getDocument().removeDocumentListener(this);

                    mainClassModified = true;
                    ((AbstractDocument.DefaultDocumentEvent) e).undo();
                    mainClassField.getDocument().addDocumentListener(this);
                });
            }
        }
    };

    public LiteLoaderProjectSettingsWizard(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;

        mcpWarning.setVisible(false);

        minecraftVersionBox.addActionListener(e -> {
            if (mcpVersion != null) {
                mcpVersion.setMcpVersion(mcpVersionBox, (String) minecraftVersionBox.getSelectedItem(), mcpBoxActionListener);
            }
        });

        pluginNameField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (mainClassModified) {
                    return;
                }

                String[] words = pluginNameField.getText().split("\\s+");
                String word = Arrays.stream(words).map(WordUtils::capitalize).collect(Collectors.joining());

                String[] mainClassWords = mainClassField.getText().split("\\.");
                mainClassWords[mainClassWords.length - 1] = LITEMOD + word;

                mainClassField.getDocument().removeDocumentListener(listener);
                mainClassField.setText(Arrays.stream(mainClassWords).collect(Collectors.joining(".")));
                mainClassField.getDocument().addDocumentListener(listener);
            }
        });

        mainClassField.getDocument().addDocumentListener(listener);

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
        settings = (LiteLoaderProjectConfiguration) creator.getSettings().get(PlatformType.LITELOADER);
        if (settings == null) {
            return panel;
        }

        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());

        if (settings != null && !settings.isFirst) {
            pluginNameField.setEditable(false);
            pluginVersionField.setEditable(false);
        }

        mainClassField.getDocument().removeDocumentListener(listener);
        mainClassField.setText(this.creator.getGroupId() + '.' + this.creator.getArtifactId()
                + "." + LITEMOD + WordUtils.capitalizeFully(creator.getArtifactId()));
        mainClassField.getDocument().addDocumentListener(listener);

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
    public boolean isStepVisible() {
        settings = (LiteLoaderProjectConfiguration) creator.getSettings().get(PlatformType.LITELOADER);
        return settings != null;
    }

    @Override
    public void onStepLeaving() {
        settings.pluginName = pluginNameField.getText();
        settings.pluginVersion = pluginVersionField.getText();
        settings.mainClass = mainClassField.getText();

        settings.mcVersion = (String) minecraftVersionBox.getSelectedItem();
        settings.mcpVersion = ((McpVersionEntry) mcpVersionBox.getSelectedItem()).getText();
    }

    @Override
    public void updateDataModel() {}
}
