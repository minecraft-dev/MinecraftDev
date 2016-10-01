// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory.Keyword;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgClassName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgEntry;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFunction;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgKeyword;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgVisitor;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CfgEntryImpl extends ASTWrapperPsiElement implements CfgEntry {

  public CfgEntryImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CfgVisitor visitor) {
    visitor.visitEntry(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CfgVisitor) accept((CfgVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public CfgClassName getClassName() {
    return findNotNullChildByClass(CfgClassName.class);
  }

  @Override
  @Nullable
  public CfgFieldName getFieldName() {
    return findChildByClass(CfgFieldName.class);
  }

  @Override
  @Nullable
  public CfgFunction getFunction() {
    return findChildByClass(CfgFunction.class);
  }

  @Override
  @NotNull
  public CfgKeyword getKeyword() {
    return findNotNullChildByClass(CfgKeyword.class);
  }

  public void setEntry(String entry) {
    CfgPsiImplUtil.setEntry(this, entry);
  }

  public void setKeyword(Keyword keyword) {
    CfgPsiImplUtil.setKeyword(this, keyword);
  }

  public void setClassName(String className) {
    CfgPsiImplUtil.setClassName(this, className);
  }

  public void setFieldName(String fieldName) {
    CfgPsiImplUtil.setFieldName(this, fieldName);
  }

  public void setFunction(String function) {
    CfgPsiImplUtil.setFunction(this, function);
  }

}
