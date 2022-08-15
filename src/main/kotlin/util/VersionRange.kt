/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

/**
 * A [SemanticVersion] range whose [lower] bound is **inclusive** and [upper] bound is **exclusive** (uncapped if `null`)
 */
data class VersionRange(val lower: SemanticVersion, val upper: SemanticVersion? = null) {

    /**
     * Whether this range consists of only one specific version
     */
    val isFixed: Boolean = lower == upper

    private val displayString: String by lazy { "[$lower,${upper ?: ""})" }

    init {
        if (upper != null && !isFixed) {
            check(lower <= upper) { "Upper bound ($upper) must be greater than lower bound ($lower)" }
        }
    }

    operator fun contains(version: SemanticVersion): Boolean {
        if (isFixed) {
            return version == lower
        }
        return version >= lower && (upper == null || version < upper)
    }

    /**
     * Produces a [VersionRange] combining the highest [lower] bound and lowest [upper] bound of
     * `this` and the [other] ranges.
     *
     * If this results in an empty intersection, `null` is returned.
     *
     * E.g. `[1.6,2)` and `[2.1,2.5)` because `2` in the first range is smaller than `2.1` in the second range
     */
    fun intersect(other: VersionRange): VersionRange? {
        val highestLowerBound = maxOf(this.lower, other.lower)
        val lowestUpperBound = minOf(this.upper, other.upper, nullsLast())
        if (lowestUpperBound != null && highestLowerBound > lowestUpperBound) {
            return null
        }
        return VersionRange(highestLowerBound, lowestUpperBound)
    }

    override fun toString(): String = displayString

    companion object {

        /**
         * Creates a fixed range strictly consisting of the given [version]
         * @see [isFixed]
         */
        fun fixed(version: SemanticVersion): VersionRange = VersionRange(version, version)
    }
}

operator fun VersionRange?.contains(version: SemanticVersion): Boolean =
    this == null || this.contains(version)

infix fun SemanticVersion.until(upperExclusive: SemanticVersion?): VersionRange =
    VersionRange(this, upperExclusive)
