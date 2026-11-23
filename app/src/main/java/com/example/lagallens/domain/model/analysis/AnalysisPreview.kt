package com.example.lagallens.domain.model.analysis

object AnalysisPreview {
    /**
     * How many entries of a list the free preview shows: 40% rounded up, at least one. The backend cuts its
     * preview the same way (`ceil(n * 0.4)`), so the app never hides an entry the server already sent.
     */
    fun visibleCount(total: Int, isUnlocked: Boolean): Int {
        require(total >= 0)
        if (total == 0 || isUnlocked) return total
        return maxOf(1, ((total.toLong() * 2 + 4) / 5).toInt())
    }
}
