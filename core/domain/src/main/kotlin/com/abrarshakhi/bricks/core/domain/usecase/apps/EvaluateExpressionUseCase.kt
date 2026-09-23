package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.search.ExpressionEvaluator

data class CalculatorResult(
    val expression: String,
    val result: String,
)

class EvaluateExpressionUseCase(
    private val evaluator: ExpressionEvaluator,
) {
    operator fun invoke(query: String): CalculatorResult? {
        val value = evaluator.evaluate(query) ?: return null
        return CalculatorResult(expression = query.trim(), result = evaluator.format(value))
    }
}
