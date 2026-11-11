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

package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.intellij.openapi.diagnostic.thisLogger
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingUtil
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.EmptyElementFilter
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor
import net.fabricmc.mappingio.format.enigma.EnigmaFileReader
import net.fabricmc.mappingio.format.enigma.EnigmaFileWriter
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.fabricmc.mappingio.tree.VisitOrder

/**
 * Reads and writes the Javadoc comments stored in an annotater `mappings` directory. Comments are stored in Enigma
 * format, with one file per top level class at `<mappings>/<internal/class/Name>.mapping`. The files are written
 * in the same way as annotater formats them, so that saving doesn't reformat unrelated parts of a file.
 *
 * Use [forDirectory] rather than the constructor outside of tests, so that every checkout of a directory sees the
 * comments the others write.
 */
class AnnotaterJavadocStore(val mappingsDir: Path) {
    private val cache = ConcurrentHashMap<String, MemoryMappingTree>()

    fun fileFor(outerClass: String): Path = mappingsDir.resolve("$outerClass.mapping")

    @Throws(IOException::class)
    fun get(target: AnnotaterJavadocTarget): String? {
        val clazz = treeFor(target.outerClass).getClass(target.owner) ?: return null
        return when (target) {
            is AnnotaterJavadocTarget.ClassTarget -> clazz.comment
            is AnnotaterJavadocTarget.FieldTarget -> clazz.getField(target.name, target.descriptor)?.comment
            is AnnotaterJavadocTarget.MethodTarget -> clazz.getMethod(target.name, target.descriptor)?.comment
        }
    }

    /**
     * Like [get], but returns null if the `.mapping` file can't be read, for displaying Javadocs.
     */
    fun getOrNull(target: AnnotaterJavadocTarget): String? {
        return try {
            if (fileFor(target.outerClass).exists()) get(target) else null
        } catch (e: IOException) {
            thisLogger().info("Failed to read annotater Javadoc of $target", e)
            null
        }
    }

    /**
     * Sets the comment for [target], or removes it when [comment] is null or empty, and writes the `.mapping` file
     * of its outer class. The file is deleted when it no longer contains any comments.
     */
    @Throws(IOException::class)
    fun set(target: AnnotaterJavadocTarget, comment: String?) {
        setAll(mapOf(target to comment))
    }

    /**
     * Like [set], but for several targets at once. All targets must share the same outer class.
     */
    @Throws(IOException::class)
    @Synchronized
    fun setAll(comments: Map<out AnnotaterJavadocTarget, String?>) {
        if (comments.isEmpty()) {
            return
        }
        val outerClass = comments.keys.first().outerClass
        require(comments.keys.all { it.outerClass == outerClass }) { "All targets must share the same outer class" }

        val tree = copyTree(treeFor(outerClass))
        for ((target, comment) in comments) {
            setInTree(tree, target, comment?.takeIf { it.isNotEmpty() })
        }

        val text = writeTree(tree)
        val file = fileFor(outerClass)
        if (text.isEmpty()) {
            file.deleteIfExists()
        } else {
            Files.createDirectories(file.parent)
            file.writeText(text)
        }
        cache[outerClass] = tree
    }

    fun invalidate(outerClass: String) {
        cache.remove(outerClass)
    }

    @Throws(IOException::class)
    private fun treeFor(outerClass: String): MemoryMappingTree {
        cache[outerClass]?.let { return it }
        val file = fileFor(outerClass)
        val tree = if (file.exists()) readFile(file.readText(), file.toString(), outerClass) else newTree()
        return cache.putIfAbsent(outerClass, tree) ?: tree
    }

    companion object {
        private val stores = ConcurrentHashMap<Path, AnnotaterJavadocStore>()

        /**
         * The store of [mappingsDir], shared by all of its checkouts. The checkout objects are replaced whenever the
         * checkouts change, such as when a contribution branch is created on the first edit, but an editor started
         * before that keeps saving through the old one.
         */
        fun forDirectory(mappingsDir: Path): AnnotaterJavadocStore =
            stores.computeIfAbsent(mappingsDir.normalize(), ::AnnotaterJavadocStore)

        /**
         * Forgets the cached comments of every store, for when the checkouts change, such as by switching branch.
         */
        fun invalidateAll() {
            for (store in stores.values) {
                store.cache.clear()
            }
        }

        @Throws(IOException::class)
        private fun readFile(text: String, path: String, expectedOuterClass: String? = null): MemoryMappingTree {
            val tree = MemoryMappingTree()
            try {
                EnigmaFileReader.read(StringReader(text), withoutMethodVariables(tree))
            } catch (e: IOException) {
                throw IOException("Invalid Enigma mapping file $path: ${e.message}", e)
            } catch (e: RuntimeException) {
                throw IOException("Invalid Enigma mapping file $path: ${e.message}", e)
            }

            val owners = tree.classes.mapTo(mutableSetOf()) { AnnotaterJavadocTarget.outerClassName(it.srcName) }
            if (owners.size > 1) {
                throw IOException("Enigma mapping file $path contains multiple outer classes")
            }
            if (expectedOuterClass != null && owners.isNotEmpty() && owners.single() != expectedOuterClass) {
                throw IOException("Enigma mapping file $path contains ${owners.single()}, not $expectedOuterClass")
            }
            return tree
        }

        @Throws(IOException::class)
        private fun writeTree(tree: MemoryMappingTree): String {
            val meaningful = newTree()
            for (clazz in tree.classes) {
                if (isMeaningful(clazz)) {
                    meaningful.addClass(clazz)
                }
            }
            if (meaningful.classes.isEmpty()) {
                return ""
            }

            val output = StringWriter()
            // The same as annotater's own formatting
            EnigmaFileWriter(output).use { writer ->
                meaningful.accept(EmptyElementFilter(writer), VisitOrder.createByName())
            }
            return output.toString()
        }

        private fun setInTree(tree: MemoryMappingTree, target: AnnotaterJavadocTarget, comment: String?) {
            if (comment == null) {
                val clazz = tree.getClass(target.owner) ?: return
                when (target) {
                    is AnnotaterJavadocTarget.ClassTarget -> clazz.comment = null
                    is AnnotaterJavadocTarget.FieldTarget -> {
                        val field = clazz.getField(target.name, target.descriptor)
                        if (field != null) {
                            field.comment = null
                            if (!hasContent(field)) {
                                clazz.removeField(target.name, target.descriptor)
                            }
                        }
                    }

                    is AnnotaterJavadocTarget.MethodTarget -> {
                        val method = clazz.getMethod(target.name, target.descriptor)
                        if (method != null) {
                            method.comment = null
                            if (!hasContent(method)) {
                                clazz.removeMethod(target.name, target.descriptor)
                            }
                        }
                    }
                }
                if (!isMeaningful(clazz)) {
                    tree.removeClass(clazz.srcName)
                }
                return
            }

            tree.visitClass(target.owner)
            tree.visitElementContent(MappedElementKind.CLASS)
            when (target) {
                is AnnotaterJavadocTarget.ClassTarget -> tree.visitComment(MappedElementKind.CLASS, comment)
                is AnnotaterJavadocTarget.FieldTarget -> {
                    tree.visitField(target.name, target.descriptor)
                    tree.visitElementContent(MappedElementKind.FIELD)
                    tree.visitComment(MappedElementKind.FIELD, comment)
                }

                is AnnotaterJavadocTarget.MethodTarget -> {
                    tree.visitMethod(target.name, target.descriptor)
                    tree.visitElementContent(MappedElementKind.METHOD)
                    tree.visitComment(MappedElementKind.METHOD, comment)
                }
            }
            tree.visitEnd()
        }

        private fun newTree(): MemoryMappingTree {
            val tree = MemoryMappingTree()
            tree.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK, listOf(MappingUtil.NS_TARGET_FALLBACK))
            tree.visitEnd()
            return tree
        }

        private fun copyTree(tree: MemoryMappingTree): MemoryMappingTree {
            if (tree.srcNamespace == null) {
                return newTree()
            }
            val copy = MemoryMappingTree()
            tree.accept(copy)
            return copy
        }

        private fun withoutMethodVariables(next: MappingVisitor): MappingVisitor {
            return object : ForwardingMappingVisitor(next) {
                override fun visitMethodArg(argPosition: Int, lvIndex: Int, srcName: String?) = false

                override fun visitMethodVar(
                    lvtRowIndex: Int,
                    lvIndex: Int,
                    startOpIdx: Int,
                    endOpIdx: Int,
                    srcName: String?,
                ) = false
            }
        }

        private fun isMeaningful(clazz: MappingTree.ClassMapping): Boolean {
            return hasContent(clazz) || clazz.fields.any(::hasContent) || clazz.methods.any(::hasContent)
        }

        private fun hasContent(element: MappingTree.ElementMapping): Boolean {
            if (element.comment != null) {
                return true
            }
            return (0 until element.tree.maxNamespaceId).any { element.getDstName(it) != null }
        }
    }
}
