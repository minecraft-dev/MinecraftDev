// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.CLASS_NAME_ELEMENT;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgClassName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgVisitor;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CfgClassNameImpl extends ASTWrapperPsiElement implements CfgClassName {

  public CfgClassNameImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CfgVisitor visitor) {
    visitor.visitClassName(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CfgVisitor) accept((CfgVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getClassNameElement() {
    return findNotNullChildByType(CLASS_NAME_ELEMENT);
  }

  public String getClassNameText() {
    return CfgPsiImplUtil.getClassNameText(this);
  }

  public void setClassName(String className) {
    CfgPsiImplUtil.setClassName(this, className);
  }

}
