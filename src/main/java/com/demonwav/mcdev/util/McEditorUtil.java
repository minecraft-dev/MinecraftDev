package com.demonwav.mcdev.util;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class McEditorUtil {

    public static void gotoTargetElement(@NotNull PsiElement element, @NotNull Editor currentEditor, @NotNull PsiFile currentFile) {
        if (element.getContainingFile() == currentFile) {
            int offset = element.getTextOffset();
            PsiElement leaf = currentFile.findElementAt(offset);
            // check that element is really physically inside the file
            // there are fake elements with custom navigation (e.g. opening URL in browser) that override getContainingFile for various reasons
            if (leaf != null && PsiTreeUtil.isAncestor(element, leaf, false)) {
                Project project = element.getProject();
                IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
                new OpenFileDescriptor(project, currentFile.getViewProvider().getVirtualFile(), offset).navigateIn(currentEditor);
                return;
            }
        }

        Navigatable navigatable = element instanceof Navigatable ? (Navigatable)element : EditSourceUtil.getDescriptor(element);
        if (navigatable != null && navigatable.canNavigate()) {
            navigatable.navigate(true);
        }
    }
}
