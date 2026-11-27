/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickElementFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.util.IncorrectOperationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Unpick identifiers are ASCII only, but the platform and Java accept any Java identifier as a new name.
 */
@ExtendWith(EdtInterceptor::class)
@DisplayName("Unpick Invalid Name Tests")
class UnpickInvalidNameTest : ProjectBuilderTest() {

    @Test
    @DisplayName("Group Rename Rejects Non-ASCII Names")
    fun groupRenameRejectsNonAsciiNames() {
        val file = fixture.configureByText("groups.unpick", "unpick v4\ngroup int Color\n    1\n")
        val definition = PsiTreeUtil.findChildOfType(file, UnpickGroupDefinition::class.java)!!

        Assertions.assertTrue(RenameUtil.isValidName(project, definition, "Colour"))
        Assertions.assertFalse(RenameUtil.isValidName(project, definition, "Colör"))
    }

    @Test
    @DisplayName("Element Factory Rejects Non-ASCII Names")
    fun elementFactoryRejectsNonAsciiNames() {
        Assertions.assertAll(
            { assertThrows<IncorrectOperationException> { UnpickElementFactory.createIdentifier(project, "Colör") } },
            { assertThrows<IncorrectOperationException> { UnpickElementFactory.createIdentifier(project, "Écran") } },
            {
                assertThrows<IncorrectOperationException> {
                    UnpickElementFactory.createClassName(project, "com.example.Écran")
                }
            },
            { assertThrows<IncorrectOperationException> { UnpickElementFactory.createFieldName(project, "VALÜE") } },
        )
    }
}
