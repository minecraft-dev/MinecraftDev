/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.util.text.VersionComparatorUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PluginUpdateStatus {

    private PluginUpdateStatus() {}

    public PluginUpdateStatus mergeWith(PluginUpdateStatus other) {
        // Jesus wept. https://github.com/JetBrains/kotlin/blob/master/idea/src/org/jetbrains/kotlin/idea/KotlinPluginUpdater.kt#L61-L63
        if (
            other instanceof Update &&
                (this instanceof LatestVersionInstalled ||
                    (this instanceof Update && VersionComparatorUtil.compare(
                        ((Update) other).getPluginDescriptor().getVersion(),
                        ((Update) this).getPluginDescriptor().getVersion()) > 0
                    )
                )
        ) {
            return other;
        }
        return this;
    }

    public static class LatestVersionInstalled extends PluginUpdateStatus {}

    public static PluginUpdateStatus fromException(String message, Exception e) {
        final StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return new CheckFailed(message, stringWriter.toString());
    }

    public static class Update extends PluginUpdateStatus {
        final IdeaPluginDescriptor pluginDescriptor;
        final String hostToInstallFrom;

        public Update(IdeaPluginDescriptor pluginDescriptor, String hostToInstallFrom) {
            this.pluginDescriptor = pluginDescriptor;
            this.hostToInstallFrom = hostToInstallFrom;
        }

        public IdeaPluginDescriptor getPluginDescriptor() {
            return pluginDescriptor;
        }

        public String getHostToInstallFrom() {
            return hostToInstallFrom;
        }
    }

    public static class CheckFailed extends PluginUpdateStatus {
        final String message;
        final String detail;

        public CheckFailed(String message, String detail) {
            this.message = message;
            this.detail = detail;
        }

        public String getMessage() {
            return message;
        }

        public String getDetail() {
            return detail;
        }
    }
}
