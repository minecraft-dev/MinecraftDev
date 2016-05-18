package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;

public abstract class AbstractTemplate {

    public static void applyBuildGradleTemplate(@NotNull Module module,
                                                @NotNull VirtualFile file,
                                                @NotNull String groupId,
                                                @NotNull String pluginVersion,
                                                @Nullable String description,
                                                @NotNull String buildVersion,
                                                @NotNull String repoName,
                                                @NotNull String repoUrl,
                                                @NotNull String depGroupId,
                                                @NotNull String depArtifactId,
                                                @NotNull String depVersion) {
        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", buildVersion);
        properties.setProperty("PLUGIN_VERSION", pluginVersion);
        properties.setProperty("GROUP_ID", groupId);
        if (description != null) {
            properties.setProperty("HAS_DESCRIPTION", "true");
            properties.setProperty("DESCRIPTION", description);
        }
        properties.setProperty("REPO_NAME", repoName);
        properties.setProperty("REPO_URL", repoUrl);
        properties.setProperty("DEP_GROUP_ID", depGroupId);
        properties.setProperty("DEP_ARTIFACT_ID", depArtifactId);
        properties.setProperty("DEP_VERSION", depVersion);

        try {
            applyTemplate(module, file, MinecraftFileTemplateGroupFactory.BUILD_GRADLE_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void applyTemplate(Module module, VirtualFile file, String templateName, Properties properties) throws IOException {
        applyTemplate(module, file, templateName, properties, false);
    }

    protected static void applyTemplate(Module module, VirtualFile file, String templateName, Properties properties, boolean trimNewlines) throws IOException {
        FileTemplateManager manager = FileTemplateManager.getInstance(module.getProject());
        FileTemplate template = manager.getJ2eeTemplate(templateName);

        Properties allProperties = manager.getDefaultProperties();
        allProperties.putAll(properties);

        String text = template.getText(allProperties);
        if (trimNewlines) {
            text = text.replaceAll("\\n+", "\n");
        }
        VfsUtil.saveText(file, text);

        PsiFile psiFile = PsiManager.getInstance(module.getProject()).findFile(file);
        if (psiFile != null) {
            new ReformatCodeProcessor(module.getProject(), psiFile, null, false).run();
        }
    }
}
