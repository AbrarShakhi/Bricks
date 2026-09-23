package com.abrarshakhi.bricks.core.domain.model

enum class ProfileType { Personal, Work, Private }

data class UserProfile(
    val serial: Long,
    val type: ProfileType,
    val isPaused: Boolean,
)
