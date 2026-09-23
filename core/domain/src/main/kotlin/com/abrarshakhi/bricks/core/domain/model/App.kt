package com.abrarshakhi.bricks.core.domain.model

data class App(
    val key: AppKey,
    val label: String,
    val systemLabel: String,
    val profile: ProfileType,
    val installedAt: Long,
    val isSystem: Boolean,
    val isHidden: Boolean,
    val isSuspended: Boolean,
    val usage: AppUsage,
) {
    val hasCustomLabel: Boolean
        get() = label != systemLabel
}

data class AppUsage(
    val launchCount: Int = 0,
    val lastLaunchedAt: Long = 0L,
) {
    fun score(now: Long): Double {
        if (launchCount == 0) return 0.0
        val ageHours = ((now - lastLaunchedAt).coerceAtLeast(0L)) / HOUR_MILLIS.toDouble()
        val recency = 1.0 / (1.0 + ageHours / RECENCY_HALF_LIFE_HOURS)
        return launchCount * (0.35 + 0.65 * recency)
    }

    private companion object {
        const val HOUR_MILLIS = 3_600_000L
        const val RECENCY_HALF_LIFE_HOURS = 72.0
    }
}
