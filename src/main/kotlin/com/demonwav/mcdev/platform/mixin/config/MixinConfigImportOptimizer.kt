/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config

import com.google.common.collect.Comparators
import com.google.gson.Gson
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class MixinConfigImportOptimizer : ImportOptimizer {

    /**
     * Sorts a number of (qualified) class names based on their package names
     * and the following rules:
     *
     *   - Each package name is compared separately
     *   - Default packages come first
     *   - Class names are only compared if in the same package
     *
     * Example:
     *   ```
     *   Main
     *   com.demonwav.mcdev.Example
     *   com.demonwav.mcdev.HelloWorld
     *   com.demonwav.mcdev.asset.Assets
     *   com.demonwav.mcdev.util.ActionData
     *   ```
     */
    private object ClassPackageComparator : Comparator<String> {

        // TODO: Consider implementing with Comparator.lexicographical() (in sorting.kt)
        // Different here is that the class name is considered separately,
        // so that default packages come first
        override fun compare(class1: String, class2: String): Int {
            val parts1 = class1.split('.')
            val parts2 = class2.split('.')

            val end = Math.min(parts1.size - 1, parts2.size - 1)
            for (i in 0 until end) {
                val result = parts1[i].compareTo(parts2[i], ignoreCase = true)
                if (result != 0) {
                    return result
                }
            }

            if (parts1.size != parts2.size) {
                // Default package always comes first
                return Integer.compare(parts1.size, parts2.size)
            }

            // Compare class names
            return parts1[end].compareTo(parts2[end], ignoreCase = true)
        }
    }

    override fun supports(file: PsiFile) = file is JsonFile && file.fileType == MixinConfigFileType

    override fun processFile(file: PsiFile): Runnable {
        if (file !is JsonFile) {
            return EmptyRunnable.getInstance()
        }

        val root = file.topLevelValue as? JsonObject ?: return EmptyRunnable.getInstance()

        val mixins = processMixins(root.findProperty("mixins"))
        val server = processMixins(root.findProperty("server"))
        val client = processMixins(root.findProperty("client"))

        if (mixins != null || server != null || client != null) {
            return Task(file, mixins, server, client)
        }

        return EmptyRunnable.getInstance()
    }

    private fun processMixins(property: JsonProperty?): JsonArray? {
        val mixins = property?.value as? JsonArray ?: return null
        val classes = mixins.valueList.mapNotNull { (it as? JsonStringLiteral)?.value }
        if (Comparators.isInStrictOrder(classes, ClassPackageComparator)) {
            return null
        }

        // Kind of lazy here, serialize the sorted list and let IntelliJ parse it
        val classesSorted = classes.toSortedSet(ClassPackageComparator)
        return JsonElementGenerator(property.project).createValue(Gson().toJson(classesSorted))
    }

    private class Task(
        private val file: JsonFile,
        private val mixins: JsonArray?,
        private val server: JsonArray?,
        private val client: JsonArray?) : Runnable {

        override fun run() {
            val manager = PsiDocumentManager.getInstance(file.project)
            manager.getDocument(file)?.let { manager.commitDocument(it) }

            val root = file.topLevelValue as? JsonObject ?: return
            replaceProperty(root, "mixins", mixins)
            replaceProperty(root, "server", server)
            replaceProperty(root, "client", client)
        }

        private fun replaceProperty(obj: JsonObject, propertyName: String, newValue: PsiElement?) {
            newValue ?: return
            val property = obj.findProperty(propertyName) ?: return
            property.value?.replace(newValue)
        }
    }
}
