package com.abrarshakhi.bricks.core.ui.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

abstract class MviViewModel<S : Any, I : Any, E : Any>(initialState: S) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val effectChannel = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = effectChannel.receiveAsFlow()

    protected val currentState: S
        get() = mutableState.value

    abstract fun onIntent(intent: I)

    protected fun reduce(reducer: S.() -> S) {
        mutableState.update(reducer)
    }

    protected fun emit(effect: E) {
        effectChannel.trySend(effect)
    }
}
