/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

@Service(Service.Level.PROJECT)
class ShareUtil(private val project: Project) {
    fun getShares(shareAnnotation: PsiAnnotation): List<PsiAnnotation> {
        val shareClass = shareAnnotation.resolveAnnotationType() ?: return listOf(shareAnnotation)
        val allShares = getAllShares(shareClass)
        return shareAnnotation.getShareKeys(project).flatMap { allShares[it] ?: listOf(shareAnnotation) }.distinct()
    }

    private fun getAllShares(shareClass: PsiClass): Map<ShareKey, List<PsiAnnotation>> {
        return CachedValuesManager.getManager(project).getCachedValue(shareClass) {
            CachedValueProvider.Result.create(computeAllShares(shareClass), PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    private fun computeAllShares(shareClass: PsiClass): Map<ShareKey, List<PsiAnnotation>> {
        val result = hashMapOf<ShareKey, MutableList<PsiAnnotation>>()
        for (annotation in getUsages(shareClass)) {
            for (key in annotation.getShareKeys(project)) {
                result.getOrPut(key) { mutableListOf() } += annotation
            }
        }
        return result
    }

    private fun getUsages(shareClass: PsiClass): List<PsiAnnotation> {
        return when (val useScope = shareClass.useScope) {
            is GlobalSearchScope -> JavaAnnotationIndex.getInstance().getAnnotations("Share", project, useScope).filter { annotation ->
                annotation.nameReferenceElement?.isReferenceTo(shareClass) == true
            }
            is LocalSearchScope -> {
                useScope.scope.flatMap { element ->
                    PsiTreeUtil.collectElementsOfType(element, PsiAnnotation::class.java).filter { annotation ->
                        annotation.nameReferenceElement?.isReferenceTo(shareClass) == true
                    }
                }
            }
            else -> throw IllegalStateException("Unknown scope class ${useScope.javaClass.name}")
        }
    }

    private data class ShareKey(
        private val namespace: String,
        private val id: String,
        private val targetOwner: String,
        private val targetName: String,
        private val targetDesc: String,
    )

    companion object {
        fun getInstance(project: Project): ShareUtil = project.getService(ShareUtil::class.java)

        private val PsiAnnotation.namespace: String?
            get() = findDeclaredAttributeValue("namespace")?.constantStringValue
                ?: findContainingClass()?.internalName
        private val PsiAnnotation.value: String?
            get() = findDeclaredAttributeValue("value")?.constantStringValue

        private fun PsiAnnotation.getShareKeys(project: Project): List<ShareKey> {
            val method = findContainingMethod() ?: return emptyList()
            val (injectorAnnotation, injector) = method.annotations.mapFirstNotNull { annotation ->
                (MixinAnnotationHandler.forMixinAnnotation(annotation, project) as? InjectorAnnotationHandler)?.let { annotation to it }
            } ?: return emptyList()

            val mixinTargets = method.findContainingClass()?.mixinTargets ?: return emptyList()
            val namespace = this.namespace ?: return emptyList()
            val id = this.value ?: return emptyList()

            return mixinTargets.flatMap { targetClass ->
                injector.resolveTarget(injectorAnnotation, targetClass).map { target ->
                    ShareKey(
                        namespace,
                        id,
                        target.classAndMethod.clazz.name,
                        target.classAndMethod.method.name,
                        target.classAndMethod.method.desc,
                    )
                }
            }
        }
    }
}
