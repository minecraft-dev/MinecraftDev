package com.demonwav.mcdev.update;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.ide.plugins.PluginNode;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.text.VersionComparatorUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.function.Function;

public class PluginUpdater {

    private static final PluginUpdater instance = new PluginUpdater();
    public static PluginUpdater getInstance() {
        return instance;
    }

    private PluginUpdateStatus lastUpdateStatus;

    public void runUpdateCheck(final Function<PluginUpdateStatus, Boolean> callback) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> updateCheck(callback));
    }

    private void updateCheck(Function<PluginUpdateStatus, Boolean> callback) {
        PluginUpdateStatus updateStatus;
        try {
            updateStatus = checkUpdatesInMainRepo();

            for (String host : RepositoryHelper.getPluginHosts()) {
                if (host == null) {
                    continue;
                }

                updateStatus = updateStatus.mergeWith(checkUpdatesInCustomRepo(host));
            }

            lastUpdateStatus = updateStatus;

            final PluginUpdateStatus finalUpdate = updateStatus;
            ApplicationManager.getApplication().invokeLater(() -> callback.apply(finalUpdate), ModalityState.any());
        } catch (Exception e) {
            PluginUpdateStatus.fromException("Minecraft Development plugin update check failed", e);
        }

    }

    private PluginUpdateStatus checkUpdatesInMainRepo() throws IOException {
        final String buildNumber = ApplicationInfo.getInstance().getBuild().asString();
        final String currentVersion = PluginUtil.getPluginVersion();
        final String os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8);
        final String url = "https://plugins.jetbrains.com/plugins/list?pluginId=8327&build="
                + buildNumber + "&pluginVersion=" + currentVersion + "&os=" + os;

        Element responseDoc = HttpRequests.request(url).connect(request -> {
            try {
                return JDOMUtil.load(request.getInputStream());
            } catch (JDOMException e) {
                e.printStackTrace();
            }
            return null;
        });

        if (responseDoc == null) {
            return new PluginUpdateStatus.CheckFailed("Unexpected plugin repository response", null);
        }
        if (!responseDoc.getName().equals("plugin-repository")) {
            return new PluginUpdateStatus.CheckFailed("Unexpected plugin repository response", JDOMUtil.writeElement(responseDoc, "\n"));
        }
        if (responseDoc.getChildren().isEmpty()) {
            return new PluginUpdateStatus.LatestVersionInstalled();
        }

        String newVersion;
        try {
            newVersion = responseDoc.getChild("category").getChild("idea-plugin").getChild("version").getText();
        } catch (NullPointerException e) {
            // Okay, look, hate me all you want for catching a NPE, but I don't have a null-safe propagation operator in Java
            // and I really don't feel like checking every one of those calls...
            newVersion = null;
        }

        if (newVersion == null) {
            return new PluginUpdateStatus.CheckFailed("Couldn't find plugin version in repository response", JDOMUtil.writeElement(responseDoc, "\n"));
        }

        final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginUtil.PLUGIN_ID);
        assert plugin != null;
        final PluginNode pluginNode = new PluginNode(PluginUtil.PLUGIN_ID);
        pluginNode.setVersion(newVersion);
        pluginNode.setName(plugin.getName());
        pluginNode.setDescription(plugin.getDescription());

        if (pluginNode.getVersion().equals(PluginUtil.getPluginVersion())) {
            return new PluginUpdateStatus.LatestVersionInstalled();
        }

        return new PluginUpdateStatus.Update(pluginNode, null);
    }

    private PluginUpdateStatus checkUpdatesInCustomRepo(String host) {
        final List<IdeaPluginDescriptor> plugins;
        try {
            plugins = RepositoryHelper.loadPlugins(host, null);
        } catch (IOException e) {
            return PluginUpdateStatus.fromException("Checking custom plugin repository " + host + "  failed", e);
        }

        final IdeaPluginDescriptor minecraftPlugin = plugins.stream()
                .filter(plugin -> plugin.getPluginId().equals(PluginUtil.PLUGIN_ID))
                .findFirst().orElse(null); // Effectively remove isEmpty call
        if (minecraftPlugin == null) {
            return new PluginUpdateStatus.LatestVersionInstalled();
        }

        return updateIfNotLatest(minecraftPlugin, host);
    }

    private PluginUpdateStatus updateIfNotLatest(IdeaPluginDescriptor plugin, String host) {
        if (plugin.getVersion().equals(PluginUtil.getPluginVersion())) {
            return new PluginUpdateStatus.LatestVersionInstalled();
        }
        return new PluginUpdateStatus.Update(plugin, host);
    }

    public void installPluginUpdate(PluginUpdateStatus.Update update) throws IOException {
        final IdeaPluginDescriptor plugin = update.getPluginDescriptor();
        final PluginDownloader downloader = PluginDownloader.createDownloader(plugin, update.getHostToInstallFrom(), null);
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Downloading Plugin", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    if (downloader.prepareToInstall(indicator)) {
                        final IdeaPluginDescriptor descriptor = downloader.getDescriptor();
                        if (descriptor != null) {
                            downloader.install();

                            ApplicationManager.getApplication().invokeLater(() -> PluginManagerMain.notifyPluginsUpdated(null));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
