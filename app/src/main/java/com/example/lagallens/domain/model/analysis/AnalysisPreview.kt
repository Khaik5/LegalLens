package com.example.lagallens.domain.model.analysis

object AnalysisPreview {
    fun visibleCount(total: Int, isUnlocked: Boolean): Int {
        require(total >= 0)
        if (total == 0 || isUnlocked) return total
        return maxOf(1, (total.toLong() * 2 / 5).toInt())
    }
}
