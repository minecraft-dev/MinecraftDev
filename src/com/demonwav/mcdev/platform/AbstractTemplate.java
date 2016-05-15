package com.demonwav.mcdev.platform;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.IOException;
import java.util.Properties;

public abstract class AbstractTemplate {

    protected static void applyTemplate(Module module, VirtualFile file, String templateName, Properties properties) throws IOException {
        FileTemplateManager manager = FileTemplateManager.getInstance(module.getProject());
        FileTemplate template = manager.getJ2eeTemplate(templateName);

        Properties allProperties = manager.getDefaultProperties();
        allProperties.putAll(properties);

        VfsUtil.saveText(file, template.getText(allProperties));

        PsiFile psiFile = PsiManager.getInstance(module.getProject()).findFile(file);
        if (psiFile != null) {
            new ReformatCodeProcessor(module.getProject(), psiFile, null, false).run();
        }
    }
}
