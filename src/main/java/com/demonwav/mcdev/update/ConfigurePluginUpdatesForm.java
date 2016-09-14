package com.demonwav.mcdev.update;

import com.intellij.util.ui.AsyncProcessIcon;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ConfigurePluginUpdatesForm {
    private JComboBox<String> channelBox;
    private JButton checkForUpdatesNowButton;
    private JPanel panel;
    private AsyncProcessIcon updateCheckInProgressIcon;
    private JLabel updateStatusLabel;
    private JButton installButton;

    public JPanel getPanel() {
        return panel;
    }

    public JComboBox getChannelBox() {
        return channelBox;
    }

    public JButton getCheckForUpdatesNowButton() {
        return checkForUpdatesNowButton;
    }

    public AsyncProcessIcon getUpdateCheckInProgressIcon() {
        return updateCheckInProgressIcon;
    }

    public JLabel getUpdateStatusLabel() {
        return updateStatusLabel;
    }

    public JButton getInstallButton() {
        return installButton;
    }

    private void createUIComponents() {
        updateCheckInProgressIcon = new AsyncProcessIcon("Plugin update check in progress");
    }
}
