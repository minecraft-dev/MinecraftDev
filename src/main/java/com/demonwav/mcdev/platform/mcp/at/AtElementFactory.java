package com.demonwav.mcdev.platform.mcp.at;

import com.demonwav.mcdev.platform.mcp.at.psi.AtArgument;
import com.demonwav.mcdev.platform.mcp.at.psi.AtAsterisk;
import com.demonwav.mcdev.platform.mcp.at.psi.AtClassName;
import com.demonwav.mcdev.platform.mcp.at.psi.AtEntry;
import com.demonwav.mcdev.platform.mcp.at.psi.AtFieldName;
import com.demonwav.mcdev.platform.mcp.at.psi.AtFuncName;
import com.demonwav.mcdev.platform.mcp.at.psi.AtFunction;
import com.demonwav.mcdev.platform.mcp.at.psi.AtKeyword;
import com.demonwav.mcdev.platform.mcp.at.psi.AtReturnValue;
import com.demonwav.mcdev.platform.mcp.at.psi.AtTypes;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFileFactory;

public class AtElementFactory {

    public static AtFile createFile(Project project, String text) {
        return (AtFile) PsiFileFactory.getInstance(project).createFileFromText("name", AtFileType.getInstance(), text);
    }

    public static AtArgument createArgument(Project project, String text) {
        final String line = "public c.c f(" + text + ")V";
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (AtArgument) file.getFirstChild().getNode().findChildByType(AtTypes.FUNCTION).findChildByType(AtTypes.ARGUMENT).getPsi();
    }

    public static AtClassName createClassName(Project project, String name) {
        final String line = "public " + name + " f(Z)V";
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (AtClassName) file.getFirstChild().getNode().findChildByType(AtTypes.CLASS_NAME).getPsi();
    }

    public static AtEntry createEntry(Project project, String entry) {
        final AtFile file = createFile(project, entry);
        return (AtEntry) file.getFirstChild();
    }

    public static AtFieldName createFieldName(Project project, String name) {
        final String line = "public c.c " + name;
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (AtFieldName) file.getFirstChild().getNode().findChildByType(AtTypes.FIELD_NAME).getPsi();
    }

    public static AtFuncName createFuncName(Project project, String name) {
        final String line = "public c.c " + name + "(Z)V";
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (AtFuncName) file.getFirstChild().getNode().findChildByType(AtTypes.FUNCTION).findChildByType(AtTypes.FUNC_NAME).getPsi();
    }

    public static AtFunction createFunction(Project project, String function) {
        final String line = "public c.c " + function;
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (AtFunction) file.getFirstChild().getNode().findChildByType(AtTypes.FUNCTION).getPsi();
    }

    public static AtAsterisk createAsterisk(Project project) {
        final String line = "public c.c *";
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (AtAsterisk) file.getFirstChild().getNode().findChildByType(AtTypes.FUNCTION).findChildByType(AtTypes.ASTERISK).getPsi();
    }

    public static AtKeyword createKeyword(Project project, Keyword keyword) {
        final String line = keyword.name().toLowerCase().replace('_', '-') + " c.c f(Z)V";
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (AtKeyword) file.getFirstChild().getNode().findChildByType(AtTypes.KEYWORD).getPsi();
    }

    public static AtReturnValue createReturnValue(Project project, String returnValue) {
        final String line = "public c.c f(Z)" + returnValue;
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (AtReturnValue) file.getFirstChild().getNode().findChildByType(AtTypes.FUNCTION).findChildByType(AtTypes.RETURN_VALUE).getPsi();
    }

    public static PsiComment createComment(Project project, String comment) {
        final String line = "# " + comment;
        final AtFile file = createFile(project, line);
        //noinspection ConstantConditions
        return (PsiComment) file.getNode().findChildByType(AtTypes.COMMENT).getPsi();
    }

    public enum Keyword {
        PRIVATE,
        PRIVATE_F,
        PROTECTED,
        PROTECTED_F,
        PUBLIC,
        PUBLIC_F;

        public static Keyword match(String s) {
            for (Keyword keyword : values()) {
                if (keyword.name().equals(s)) {
                    return keyword;
                }
            }
            return null;
        }
    }
}
