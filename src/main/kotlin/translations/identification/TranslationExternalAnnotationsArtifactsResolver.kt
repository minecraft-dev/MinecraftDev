/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.translations.identification

import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.invokeLater
import com.intellij.codeInsight.ExternalAnnotationsArtifactsResolver
import com.intellij.codeInsight.externalAnnotation.location.AnnotationsLocation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AnnotationOrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.openapi.roots.ui.configuration.libraryEditor.ExistingLibraryEditor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.ide.impl.legacyBridge.library.LibraryBridge
import com.intellij.workspaceModel.ide.impl.legacyBridge.library.ProjectLibraryTableBridgeImpl.Companion.findLibraryEntity
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class TranslationExternalAnnotationsArtifactsResolver : ExternalAnnotationsArtifactsResolver {
    override fun resolve(project: Project, library: Library, mavenId: String?): Boolean {
        if (!Util.isOurFakeMavenLocation(mavenId)) {
            return false
        }

        return invokeAndWait {
            doResolve(library)
        }
    }

    override fun resolve(project: Project, library: Library, annotationsLocation: AnnotationsLocation): Boolean {
        if (annotationsLocation != Util.fakeMavenLocation) {
            return false
        }
        return invokeAndWait {
            doResolve(library)
        }
    }

    override fun resolve(
        project: Project,
        library: Library,
        annotationsLocation: AnnotationsLocation,
        diff: MutableEntityStorage
    ): Boolean {
        if (library !is LibraryBridge || library.isDisposed) {
            return true
        }

        val annotationsPath = findAnnotationsPath(false) ?: findAnnotationsPath(true) ?: return false

        val libraryEntity = diff.findLibraryEntity(library) ?: return true
        val vfUrlManager = VirtualFileUrlManager.getInstance(project)
        val newUrl = annotationsPath.toVirtualFileUrl(vfUrlManager)
        val annotationsRootType = LibraryRootTypeId(AnnotationOrderRootType.ANNOTATIONS_ID)

        val alreadyExists = libraryEntity.roots.any { it.type == annotationsRootType && it.url == newUrl }
        if (alreadyExists) {
            return true
        }

        diff.modifyEntity(libraryEntity) {
            roots += LibraryRoot(newUrl, annotationsRootType)
        }
        return true
    }

    override fun resolveAsync(project: Project, library: Library, mavenId: String?): Promise<Library> {
        if (!Util.isOurFakeMavenLocation(mavenId)) {
            return resolvedPromise(library)
        }

        val promise = AsyncPromise<Library>()
        ApplicationManager.getApplication().executeOnPooledThread {
            invokeLater {
                doResolve(library)
                promise.setResult(library)
            }
        }

        return promise
    }

    private fun doResolve(library: Library): Boolean {
        if (library !is LibraryEx || library.isDisposed) {
            return false
        }

        val annotationsPath = runReadAction { findAnnotationsPath(false) ?: findAnnotationsPath(true) }
            ?: return false

        val editor = ExistingLibraryEditor(library, null)
        val type = AnnotationOrderRootType.getInstance()
        val alreadyExists = editor.getUrls(type).contains(annotationsPath.url)
        if (!alreadyExists) {
            editor.addRoots(listOf(OrderRoot(annotationsPath, type)))
            runWriteAction {
                editor.commit()
            }
        } else {
            Disposer.dispose(editor)
        }

        return true
    }

    private fun findAnnotationsPath(refresh: Boolean): VirtualFile? {
        val mcdevClassesRootPath =
            PathManager.getJarForClass(TranslationExternalAnnotationsArtifactsResolver::class.java)?.toAbsolutePath()
        if (!log.assertTrue(mcdevClassesRootPath != null)) {
            return null
        }
        mcdevClassesRootPath!!

        val vfm = VirtualFileManager.getInstance()
        val annotationsJarPath = mcdevClassesRootPath.resolveSibling("resources/externalAnnotations.jar")
        val annotationsJarPathString = FileUtil.toSystemIndependentName(annotationsJarPath.toString())
        val url = "jar://$annotationsJarPathString!/"
        return if (refresh) {
            vfm.refreshAndFindFileByUrl(url)
        } else {
            vfm.findFileByUrl(url)
        }
    }

    companion object {
        val log = logger<TranslationExternalAnnotationsArtifactsResolver>()
    }

    object Util {
        val fakeMavenLocation = AnnotationsLocation("com.demonwav.mcdev", "external_annotations", "1.0")

        fun isOurFakeMavenLocation(mavenId: String?): Boolean {
            return mavenId == "com.demonwav.mcdev:external_annotations:1.0"
        }
    }
}
