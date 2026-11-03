package zero.feature.streaks

import kotlin.random.Random
import zero.foundation.design.DesignTokens

object StreakPalette {
    const val SIZE: Int = 7

    val COLORS: List<Int> = listOf(
        0xFFF0F01B.toInt(),
        0xFFB1E80D.toInt(),
        0xFFA3D8FF.toInt(),
        0xFF0065F8.toInt(),
        0xFFFF2DF1.toInt(),
        0xFFF93827.toInt(),
        0xFFFF55BB.toInt(),
    )

    fun pickIndex(used: List<Int>, random: Random): Int {
        val counts = used.groupingBy { it }.eachCount()
        val available = (COLORS.indices).filter { it !in used }
        val candidates = if (available.isNotEmpty()) {
            val lowestUse = available.minOf { counts[it] ?: 0 }
            available.filter { (counts[it] ?: 0) == lowestUse }
        } else {
            COLORS.indices.toList()
        }
        return if (candidates.isEmpty()) 0 else candidates[random.nextInt(candidates.size)]
    }
}
