/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.debug

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.mapNotNull
import com.intellij.debugger.MultiRequestPositionManager
import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebuggerUtils
import com.intellij.debugger.engine.JVMNameUtil
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.FileType
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest
import java.util.stream.Stream
import kotlin.streams.toList

class MixinPositionManager(private val debugProcess: DebugProcess) : MultiRequestPositionManager {

    override fun getAcceptedFileTypes(): Set<FileType> = setOf(JavaFileType.INSTANCE)

    override fun getSourcePosition(location: Location?): SourcePosition? {
        if (location != null) {
            val type = location.declaringType()

            // Check if mixin source map is present (Mixin sets the default stratum to Mixin)
            if (type.defaultStratum() == MixinConstants.SMAP_STRATUM) {
                // Return the correct PsiFile based on the source path in the SMAP
                try {
                    val path = location.sourcePath()

                    // The source path is the package (separated by slashes) and class name with the ".java" file extension
                    val className = path.removeSuffix(".java").replace('/', '.')

                    // Lookup class based on its qualified name (TODO: Support for anonymous classes)
                    val psiClass = DebuggerUtils.findClass(className, debugProcess.project, debugProcess.searchScope)
                    if (psiClass != null) {
                        // Mixin class found, return correct source file
                        return SourcePosition.createFromLine(psiClass.navigationElement.containingFile, location.lineNumber() - 1)
                    }
                } catch (ignored: AbsentInformationException) {
                }
            }
        }

        throw NoDataException.INSTANCE
    }

    override fun getAllClasses(classPosition: SourcePosition): List<ReferenceType> {
        return runReadAction {
            findMatchingClasses(classPosition)
                .flatMap { name -> debugProcess.virtualMachineProxy.classesByName(name).stream() }
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
        throw UnsupportedOperationException("This class implements MultiRequestPositionManager, corresponding createPrepareRequests version should be used")
    }

    override fun createPrepareRequests(requestor: ClassPrepareRequestor, position: SourcePosition): List<ClassPrepareRequest> {
        return runReadAction {
            findMatchingClasses(position)
                .mapNotNull { name -> debugProcess.requestsManager.createClassPrepareRequest(requestor, name) }
                .toList()
        }
    }

    private fun findMatchingClasses(position: SourcePosition): Stream<String> {
        val classElement = position.elementAt?.findContainingClass() ?: throw NoDataException.INSTANCE
        val targets = classElement.mixinTargets
        if (targets.isEmpty()) {
            throw NoDataException.INSTANCE
        }

        return targets.stream()
            // TODO: Support for anonymous classes
            .mapNotNull(JVMNameUtil::getNonAnonymousClassName)
    }

}
