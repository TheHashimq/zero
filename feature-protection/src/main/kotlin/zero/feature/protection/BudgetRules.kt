/*
 * MODULE: feature-protection
 * PURPOSE: Per-app time budgets: access time per window, cooldown after a window, and a daily cap.
 * TAKES: A Budget, its running BudgetState, the day key, the current time, and the time spent in front.
 * GIVES: The new state, and whether the user must be sent Home now. Also the tighten-only check for edits.
 * OWNS: The limits on each value and the window and cooldown arithmetic.
 * DOES NOT OWN: Measuring time in front, saving state, or showing the block screen. The app does those.
 * DEPENDENCIES: None.
 * CONTRACT: A budget can only be tightened, never loosened or removed. Access shrinks or stays, cooldown lengthens or
 *           stays, and the daily cap drops or stays. The day key resets usage but keeps any running cooldown.
 * FAILURE BEHAVIOR: Invalid values throw IllegalArgumentException. Tightening an unrelated budget throws.
 */
package zero.feature.protection

/** One app's budget. Values are set by the user. Nothing here is a default. */
data class Budget(
    val packageName: String,
    val accessMinutes: Int,
    val cooldownMinutes: Int,
    val dailyCapMinutes: Int,
) {
    init {
        require(packageName.isNotBlank()) { "package name must not be blank" }
        require(accessMinutes in 1..60) { "access must be 1 to 60 minutes" }
        require(cooldownMinutes in 5..240) { "cooldown must be 5 to 240 minutes" }
        require(dailyCapMinutes in accessMinutes..480) { "daily cap must be from the access time up to 480 minutes" }
    }
}

/** A budget's running state. Times are milliseconds. */
data class BudgetState(
    val dayKey: String,
    val usedTodayMillis: Long = 0L,
    val accessLeftMillis: Long = 0L,
    val cooldownUntilMillis: Long = 0L,
)

/** The result of time spent in the app: the new state, and whether the user must be sent Home now. */
data class BudgetStep(val state: BudgetState, val sendHome: Boolean)

object BudgetRules {
    private const val MINUTE_MILLIS = 60_000L

    /** True when [proposed] is no looser than [old] on any value. */
    fun isTightening(old: Budget, proposed: Budget): Boolean {
        require(old.packageName == proposed.packageName) { "budgets must be for the same app" }
        return proposed.accessMinutes <= old.accessMinutes &&
            proposed.cooldownMinutes >= old.cooldownMinutes &&
            proposed.dailyCapMinutes <= old.dailyCapMinutes
    }

    /** Applies an edit only if it tightens the budget. Anything looser is refused. */
    fun tighten(old: Budget, proposed: Budget): Result<Budget> =
        if (isTightening(old, proposed)) Result.success(proposed)
        else Result.failure(IllegalArgumentException("a budget can only be made stricter"))

    /** True when the app is in cooldown or has reached its daily cap. [state] must be for the current day. */
    fun isBlocked(state: BudgetState, budget: Budget, nowMillis: Long): Boolean =
        nowMillis < state.cooldownUntilMillis ||
            state.usedTodayMillis >= budget.dailyCapMinutes * MINUTE_MILLIS

    /**
     * Records [elapsedMillis] of time in front. Starts a new access window when none is left.
     * When the window ends, the cooldown starts. When the daily cap is reached, the app is blocked for the day.
     */
    fun advance(
        state: BudgetState,
        budget: Budget,
        dayKey: String,
        nowMillis: Long,
        elapsedMillis: Long,
    ): BudgetStep {
        val current = if (state.dayKey != dayKey) {
            state.copy(dayKey = dayKey, usedTodayMillis = 0L, accessLeftMillis = 0L)
        } else {
            state
        }
        if (isBlocked(current, budget, nowMillis)) return BudgetStep(current, sendHome = true)

        val cap = budget.dailyCapMinutes * MINUTE_MILLIS
        val window = if (current.accessLeftMillis > 0L) {
            current.accessLeftMillis
        } else {
            budget.accessMinutes * MINUTE_MILLIS
        }
        val room = cap - current.usedTodayMillis
        val spent = minOf(elapsedMillis.coerceAtLeast(0L), window, room)
        val used = current.usedTodayMillis + spent
        val left = window - spent
        val cooldownUntil = if (left <= 0L) {
            nowMillis + budget.cooldownMinutes * MINUTE_MILLIS
        } else {
            current.cooldownUntilMillis
        }
        val next = BudgetState(dayKey, used, left, cooldownUntil)
        return BudgetStep(next, sendHome = used >= cap || left <= 0L)
    }
}
