/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

fun inModsTomlKey(): PsiElementPattern.Capture<PsiElement> =
    inModsToml<PsiElement>().withParent(TomlKeySegment::class.java)

fun tomlKeyValue(key: String): PsiElementPattern.Capture<TomlKeyValue> =
    PlatformPatterns.psiElement(TomlKeyValue::class.java)
        .withChild(PlatformPatterns.psiElement(TomlKey::class.java).withText(key))

fun inModsTomlValueWithKey(key: String): PsiElementPattern.Capture<PsiElement> =
    inModsToml<PsiElement>().inside(tomlKeyValue(key))

fun inDependenciesHeaderId(): PsiElementPattern.Capture<PsiElement> =
    inModsToml<PsiElement>().inside(PlatformPatterns.psiElement(TomlTableHeader::class.java))
        // [[dependencies.<caret>]]
        .afterLeaf(PlatformPatterns.psiElement().withText(".").afterLeaf("dependencies"))
