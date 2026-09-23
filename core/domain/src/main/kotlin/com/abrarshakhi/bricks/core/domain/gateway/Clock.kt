package com.abrarshakhi.bricks.core.domain.gateway

fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}
