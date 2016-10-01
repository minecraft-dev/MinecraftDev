package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgArgument;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgClassName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgEntry;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFuncName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFunction;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgKeyword;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgReturnValue;

import com.intellij.psi.PsiElement;

@SuppressWarnings("unused")
public class CfgPsiImplUtil {
    public static Class<?> getPrimitiveArgumentType(CfgArgument argument) {
        final PsiElement primitive = argument.getPrimitive();
        if (primitive == null) {
            return null;
        } else {
            // it's a primitive argument
            switch (primitive.getText()) {
                case "B":
                    return byte.class;
                case "C":
                    return char.class;
                case "D":
                    return double.class;
                case "F":
                    return float.class;
                case "I":
                    return int.class;
                case "J":
                    return long.class;
                case "S":
                    return short.class;
                case "Z":
                    return boolean.class;
                default:
                    return null;
            }
        }
    }

    public static String getArgumentText(CfgArgument argument) {
        return argument.getClassValue() != null ? argument.getClassValue().getText() : argument.getPrimitive() != null ? argument.getPrimitive().getText() : null;
    }

    public static String getClassNameText(CfgClassName className) {
        return className.getClassNameElement().getText();
    }

    public static String getFieldNameText(CfgFieldName fieldName) {
        return fieldName.getNameElement().getText();
    }

    public static String getFuncNameText(CfgFuncName funcName) {
        return funcName.getNameElement() != null ? funcName.getNameElement().getText() : null;
    }

    public static String getKeywordText(CfgKeyword keyword) {
        return keyword.getKeywordElement().getText();
    }

    public static String getReturnValueText(CfgReturnValue returnValue) {
        return returnValue.getClassValue() != null ? returnValue.getClassValue().getText() : returnValue.getPrimitive() != null ? returnValue.getPrimitive().getText() : null;
    }

    // setters
    public static void setArgument(CfgArgument cfgArgument, String argument) {
        cfgArgument.replace(CfgElementFactory.createArgument(cfgArgument.getProject(), argument));
    }

    public static void setClassName(CfgClassName cfgClassName, String className) {
        cfgClassName.replace(CfgElementFactory.createClassName(cfgClassName.getProject(), className));
    }

    public static void setEntry(CfgEntry cfgEntry, String entry) {
        cfgEntry.replace(CfgElementFactory.createEntry(cfgEntry.getProject(), entry));
    }

    public static void setFieldName(CfgFieldName cfgFieldName, String fieldName) {
        cfgFieldName.replace(CfgElementFactory.createFieldName(cfgFieldName.getProject(), fieldName));
    }

    public static void setFuncName(CfgFuncName cfgFuncName, String funcName) {
        cfgFuncName.replace(CfgElementFactory.createFuncName(cfgFuncName.getProject(), funcName));
    }

    public static void setFunction(CfgFunction cfgFunction, String function) {
        cfgFunction.replace(CfgElementFactory.createFunction(cfgFunction.getProject(), function));
    }

    public static void setKeyword(CfgKeyword cfgKeyword, CfgElementFactory.Keyword keyword) {
        cfgKeyword.replace(CfgElementFactory.createKeyword(cfgKeyword.getProject(), keyword));
    }

    public static void setReturnValue(CfgReturnValue cfgReturnValue, String returnValue) {
        cfgReturnValue.replace(CfgElementFactory.createReturnValue(cfgReturnValue.getProject(), returnValue));
    }

    // On Entry
    public static void setKeyword(CfgEntry cfgEntry, CfgElementFactory.Keyword keyword) {
        //noinspection ConstantConditions
        cfgEntry.getKeyword().replace(CfgElementFactory.createKeyword(cfgEntry.getProject(), keyword));
    }

    public static void setClassName(CfgEntry cfgEntry, String className) {
        //noinspection ConstantConditions
        cfgEntry.getClassName().replace(CfgElementFactory.createClassName(cfgEntry.getProject(), className));
    }

    public static void setFieldName(CfgEntry cfgEntry, String fieldName) {
        //noinspection ConstantConditions
        cfgEntry.getFieldName().replace(CfgElementFactory.createFieldName(cfgEntry.getProject(), fieldName));
    }

    public static void setFunction(CfgEntry cfgEntry, String function) {
        //noinspection ConstantConditions
        cfgEntry.getFunction().replace(CfgElementFactory.createFieldName(cfgEntry.getProject(), function));
    }

    // On Function
    public static void setArgumentList(CfgFunction cfgFunction, String arguments) {
        final String funcName = getFuncNameText(cfgFunction.getFuncName());
        cfgFunction.getArgumentList().forEach(PsiElement::delete);
        final String returnValue = getReturnValueText(cfgFunction.getReturnValue());
        cfgFunction.replace(CfgElementFactory.createFunction(cfgFunction.getProject(), funcName + "(" + arguments + ")" + returnValue));
    }

    public static void setReturnValue(CfgFunction cfgFunction, String returnValue) {
        cfgFunction.getReturnValue().replace(CfgElementFactory.createReturnValue(cfgFunction.getProject(), returnValue));
    }

}
