package zero.feature.streaks

import kotlin.random.Random
import zero.foundation.design.DesignTokens

object StreakPalette {
    const val SIZE: Int = 7

    val COLORS: List<Int> = listOf(
        0xFF5DD4FF.toInt(),
        0xFF34D399.toInt(),
        0xFFFFB454.toInt(),
        0xFFFF6B8F.toInt(),
        0xFF9F7AEA.toInt(),
        0xFF7EE787.toInt(),
        0xFFFFD166.toInt(),
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
