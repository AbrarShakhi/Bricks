package com.abrarshakhi.bricks.core.data.apps

import kotlinx.serialization.Serializable

@Serializable
internal data class AppMetadataDto(
    val version: Int = 1,
    val hidden: Set<String> = emptySet(),
    val labels: Map<String, String> = emptyMap(),
    val usage: Map<String, UsageDto> = emptyMap(),
    val known: Set<String>? = null,
)

@Serializable
internal data class UsageDto(
    val count: Int = 0,
    val last: Long = 0L,
)
