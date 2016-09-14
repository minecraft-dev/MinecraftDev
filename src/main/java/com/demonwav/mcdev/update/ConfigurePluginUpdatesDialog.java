package com.demonwav.mcdev.update;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;

public class ConfigurePluginUpdatesDialog extends DialogWrapper {

    private ConfigurePluginUpdatesForm form = new ConfigurePluginUpdatesForm();
    private PluginUpdateStatus.Update update;
    private int initialSelectedChannel;

    @SuppressWarnings("unchecked")
    public ConfigurePluginUpdatesDialog() {
        super(true);

        setTitle("Configure Minecraft Development Plugin Updates");
        form.getUpdateCheckInProgressIcon().suspend();
        form.getUpdateCheckInProgressIcon().setPaintPassiveIcon(false);

        form.getChannelBox().addItem("Stable");
        for (Channels channels : Channels.orderedList()) {
            form.getChannelBox().addItem(channels.getTitle());
        }

        form.getCheckForUpdatesNowButton().addActionListener(e -> {
            saveSettings();
            form.getUpdateCheckInProgressIcon().resume();
            resetUpdateStatus();
            PluginUpdater.getInstance().runUpdateCheck(pluginUpdateStatus -> {
                form.getUpdateCheckInProgressIcon().suspend();

                if (pluginUpdateStatus instanceof PluginUpdateStatus.LatestVersionInstalled) {
                    form.getUpdateStatusLabel().setText("You have the latest version of the plugin (" + PluginUtil.getPluginVersion() + ") installed.");
                } else if (pluginUpdateStatus instanceof PluginUpdateStatus.Update) {
                    update = (PluginUpdateStatus.Update) pluginUpdateStatus;
                    form.getInstallButton().setVisible(true);
                    form.getUpdateStatusLabel().setText("A new version (" + ((PluginUpdateStatus.Update) pluginUpdateStatus).getPluginDescriptor().getVersion() + ") is available");
                } else {
                    // CheckFailed
                    form.getUpdateStatusLabel().setText("Update check failed: " + ((PluginUpdateStatus.CheckFailed) pluginUpdateStatus).getMessage());
                }

                return false;
            });
        });

        form.getInstallButton().setVisible(false);
        form.getInstallButton().addActionListener(action -> {
            if (update != null) {
                close(OK_EXIT_CODE);
                try {
                    PluginUpdater.getInstance().installPluginUpdate(update);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        form.getChannelBox().addActionListener(e -> resetUpdateStatus());

        for (Channels channels : Channels.values()) {
            if (channels.hasChannel()) {
                initialSelectedChannel = channels.getIndex();
                break;
            }
        }

        form.getChannelBox().setSelectedIndex(initialSelectedChannel);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return form.getPanel();
    }

    private void saveSelectedChannel(int channel) {
        final List<String> hosts = UpdateSettings.getInstance().getStoredPluginHosts();
        for (Channels channels : Channels.values()) {
            hosts.remove(channels.getUrl());
        }

        Channels channels = Channels.getChannel(channel);
        if (channels == null) {
            // This really shouldn't happen
            return;
        }

        hosts.add(channels.getUrl());
    }

    private void saveSettings() {
        saveSelectedChannel(form.getChannelBox().getSelectedIndex());
    }

    private void resetUpdateStatus() {
        form.getUpdateStatusLabel().setText(" ");
        form.getInstallButton().setVisible(false);
    }

    @Override
    protected void doOKAction() {
        saveSettings();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        saveSelectedChannel(initialSelectedChannel);
        super.doCancelAction();
    }
}
