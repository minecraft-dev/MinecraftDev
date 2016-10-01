// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CfgVisitor extends PsiElementVisitor {

  public void visitArgument(@NotNull CfgArgument o) {
    visitPsiElement(o);
  }

  public void visitClassName(@NotNull CfgClassName o) {
    visitPsiElement(o);
  }

  public void visitEntry(@NotNull CfgEntry o) {
    visitPsiElement(o);
  }

  public void visitFieldName(@NotNull CfgFieldName o) {
    visitPsiElement(o);
  }

  public void visitFuncName(@NotNull CfgFuncName o) {
    visitPsiElement(o);
  }

  public void visitFunction(@NotNull CfgFunction o) {
    visitPsiElement(o);
  }

  public void visitKeyword(@NotNull CfgKeyword o) {
    visitPsiElement(o);
  }

  public void visitReturnValue(@NotNull CfgReturnValue o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
