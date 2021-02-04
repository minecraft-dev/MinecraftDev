/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.toml

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.VirtualFilePattern
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTableHeader

inline fun <reified E : PsiElement> inModsToml(): PsiElementPattern.Capture<E> = inModsToml(E::class.java)

fun <E : PsiElement> inModsToml(clazz: Class<E>): PsiElementPattern.Capture<E> =
    PlatformPatterns.psiElement(clazz).inVirtualFile(VirtualFilePattern().withName(ForgeConstants.MODS_TOML))

fun inModsTomlKey() = inModsToml<PsiElement>().withParent(TomlKeySegment::class.java)

fun tomlKeyValue(key: String) = PlatformPatterns.psiElement(TomlKeyValue::class.java)
    .withChild(PlatformPatterns.psiElement(TomlKey::class.java).withText(key))

fun inModsTomlValueWithKey(key: String) = inModsToml<PsiElement>().inside(tomlKeyValue(key))

fun inDependenciesHeaderId() = inModsToml<PsiElement>().inside(PlatformPatterns.psiElement(TomlTableHeader::class.java))
    // [[dependencies.<caret>]]
    .afterLeaf(PlatformPatterns.psiElement().withText(".").afterLeaf("dependencies"))
