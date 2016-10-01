package com.demonwav.mcdev.platform.forge.cfg;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgArgument;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgClassName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgEntry;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFuncName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFunction;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgKeyword;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgReturnValue;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFileFactory;

public class CfgElementFactory {

    public static CfgFile createFile(Project project, String text) {
        return (CfgFile) PsiFileFactory.getInstance(project).createFileFromText("name", CfgFileType.getInstance(), text);
    }

    public static CfgArgument createArgument(Project project, String text) {
        final String line = "public c.c f(" + text + ")V";
        final CfgFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (CfgArgument) file.getFirstChild().getNode().findChildByType(CfgTypes.FUNCTION).findChildByType(CfgTypes.ARGUMENT).getPsi();
    }

    public static CfgClassName createClassName(Project project, String name) {
        final String line = "public " + name + " f(Z)V";
        final CfgFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (CfgClassName) file.getFirstChild().getNode().findChildByType(CfgTypes.CLASS_NAME).getPsi();
    }

    public static CfgEntry createEntry(Project project, String entry) {
        final CfgFile file = createFile(project, entry);
        return (CfgEntry) file.getFirstChild();
    }

    public static CfgFieldName createFieldName(Project project, String name) {
        final String line = "public c.c " + name;
        final CfgFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (CfgFieldName) file.getFirstChild().getNode().findChildByType(CfgTypes.FIELD_NAME).getPsi();
    }

    public static CfgFuncName createFuncName(Project project, String name) {
        final String line = "public c.c " + name + "(Z)V";
        final CfgFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (CfgFuncName) file.getFirstChild().getNode().findChildByType(CfgTypes.FUNCTION).findChildByType(CfgTypes.FUNC_NAME).getPsi();
    }

    public static CfgFunction createFunction(Project project, String function) {
        final String line = "public c.c " + function;
        final CfgFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (CfgFunction) file.getFirstChild().getNode().findChildByType(CfgTypes.FUNCTION).getPsi();
    }

    public static CfgKeyword createKeyword(Project project, Keyword keyword) {
        final String line = keyword.name().toLowerCase().replace('_', '-') + " c.c f(Z)V";
        final CfgFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (CfgKeyword) file.getFirstChild().getNode().findChildByType(CfgTypes.KEYWORD).getPsi();
    }

    public static CfgReturnValue createReturnValue(Project project, String returnValue) {
        final String line = "public c.c f(Z)" + returnValue;
        final CfgFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (CfgReturnValue) file.getFirstChild().getNode().findChildByType(CfgTypes.FUNCTION).findChildByType(CfgTypes.RETURN_VALUE).getPsi();
    }

    public static PsiComment createComment(Project project, String comment) {
        final String line = "# " + comment;
        final CfgFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (PsiComment) file.getNode().findChildByType(CfgTypes.COMMENT).getPsi();
    }

    public enum Keyword {
        PRIVATE,
        PRIVATE_F,
        PROTECTED,
        PROTECTED_F,
        PUBLIC,
        PUBLIC_F
    }
}
