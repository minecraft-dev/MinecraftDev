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

    @Nullable
    public static String applyBuildGradleTemplate(@NotNull Module module,
                                                @NotNull String groupId,
                                                @NotNull String pluginVersion,
                                                @Nullable String description,
                                                @NotNull String buildVersion) {
        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", buildVersion);
        properties.setProperty("PLUGIN_VERSION", pluginVersion);
        properties.setProperty("GROUP_ID", groupId);
        if (description != null) {
            properties.setProperty("HAS_DESCRIPTION", "true");
            properties.setProperty("DESCRIPTION", description);
        }

        FileTemplateManager manager = FileTemplateManager.getInstance(module.getProject());
        FileTemplate template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUILD_GRADLE_TEMPLATE);

        try {
            return template.getText(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
