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

package com.demonwav.mcdev.platform.mixin.debug

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.debugger.MultiRequestPositionManager
import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebuggerUtils
import com.intellij.debugger.engine.PositionManagerImpl.ClsSourcePosition
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest

class MixinPositionManager(private val debugProcess: DebugProcess) : MultiRequestPositionManager {

    override fun getAcceptedFileTypes(): Set<FileType> = setOf(JavaFileType.INSTANCE)

    @Throws(NoDataException::class)
    override fun getSourcePosition(location: Location?): SourcePosition? {
        location ?: throw NoDataException.INSTANCE

        val type = location.declaringType()

        // Check if mixin source map is present (Mixin sets the default stratum to Mixin)
        if (type.defaultStratum() != MixinConstants.SMAP_STRATUM) {
            throw NoDataException.INSTANCE
        }

        // Return the correct PsiFile based on the source path in the SMAP
        try {
            val path = location.sourcePath()

            // The source path is the package (separated by slashes) and class name with the ".java" file extension
            val className = path.removeSuffix(".java")
                .replace('/', '.').replace('\\', '.')

            val psiFile = findAlternativeSource(className, debugProcess.project)
                // Lookup class based on its qualified name (TODO: Support for anonymous classes)
                ?: DebuggerUtils.findClass(
                    className,
                    debugProcess.project,
                    debugProcess.searchScope,
                )?.navigationElement?.containingFile

            if (psiFile != null) {
                // File found, return correct source file
                var line = location.lineNumber() - 1
                if (psiFile is PsiCompiledElement) {
                    val adjustedLine = DebuggerUtilsEx.bytecodeToSourceLine(psiFile, line)
                    if (adjustedLine > -1) {
                        line = adjustedLine
                    } else {
                        // Class is not decompiled yet, return a lazily remapped SourcePosition
                        return ClsSourcePosition(SourcePosition.createFromLine(psiFile, line), line)
                    }
                }

                if (line > -1) {
                    return SourcePosition.createFromLine(psiFile, line)
                }
            }
        } catch (ignored: AbsentInformationException) {
        }

        throw NoDataException.INSTANCE
    }

    private fun findAlternativeSource(className: String, project: Project): PsiFile? {
        val alternativeSource = DebuggerUtilsEx.getAlternativeSourceUrl(className, project) ?: return null
        val alternativeFile = VirtualFileManager.getInstance().findFileByUrl(alternativeSource) ?: return null
        return PsiManager.getInstance(project).findFile(alternativeFile)
    }

    override fun getAllClasses(classPosition: SourcePosition): List<ReferenceType> {
        return runReadAction {
            findMatchingClasses(classPosition)
                .flatMap { name -> debugProcess.virtualMachineProxy.classesByName(name).asSequence() }
                .toList()
        }
    }

    override fun locationsOfLine(type: ReferenceType, position: SourcePosition): List<Location> {
        // Check if mixin source map is present (Mixin sets the default stratum to Mixin)
        if (type.defaultStratum() == MixinConstants.SMAP_STRATUM) {
            try {
                // Return the line numbers from the correct source file
                return type.locationsOfLine(MixinConstants.SMAP_STRATUM, position.file.name, position.line + 1)
            } catch (ignored: AbsentInformationException) {
            }
        }

        throw NoDataException.INSTANCE
    }

    override fun createPrepareRequest(requestor: ClassPrepareRequestor, position: SourcePosition): ClassPrepareRequest {
        throw UnsupportedOperationException(
            "This class implements MultiRequestPositionManager, " +
                "corresponding createPrepareRequests version should be used",
        )
    }

    override fun createPrepareRequests(
        requestor: ClassPrepareRequestor,
        position: SourcePosition,
    ): List<ClassPrepareRequest> {
        return runReadAction {
            findMatchingClasses(position)
                .mapNotNull { name -> debugProcess.requestsManager.createClassPrepareRequest(requestor, name) }
                .toList()
        }
    }

    private fun findMatchingClasses(position: SourcePosition): Sequence<String> {
        val classElement = position.elementAt?.findContainingClass() ?: throw NoDataException.INSTANCE
        return classElement.mixinTargets
            .ifEmpty { throw NoDataException.INSTANCE }
            .asSequence()
            .map { it.name.replace('/', '.') }
    }
}
