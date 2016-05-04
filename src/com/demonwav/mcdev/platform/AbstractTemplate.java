package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.IOException;
import java.util.Properties;

public abstract class AbstractTemplate {

    // TODO: move to platform dependant
    public static String applyPomTemplate(Project project, String version) {
        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", version);

        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate fileTemplate = manager.getTemplate(MinecraftFileTemplateGroupFactory.BUKKIT_POM_TEMPLATE);
        try {
            return fileTemplate.getText(properties);
        } catch (IOException e) {
            // TODO what to do when this fails?
            e.printStackTrace();
            return "";
        }
    }

    public static void applyTemplate(Project project, VirtualFile file, String templateName, Properties properties) throws IOException {
        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate template = manager.getTemplate(templateName);

        // remove comments (they leave empty lines
        template.setText(template.getText().replaceAll("#\\*.*\\*#\\n", "").trim());

        Properties allProperties = manager.getDefaultProperties();
        allProperties.putAll(properties);

        VfsUtil.saveText(file, template.getText(allProperties));

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            new ReformatCodeProcessor(project, psiFile, null, false).run();
        }
    }
}
