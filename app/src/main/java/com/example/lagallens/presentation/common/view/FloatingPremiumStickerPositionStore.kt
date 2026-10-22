package com.example.lagallens.presentation.common.view

import android.content.Context

internal class FloatingPremiumStickerPositionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): FloatingPremiumStickerPosition {
        val edge = if (preferences.getString(KEY_EDGE, EDGE_RIGHT) == EDGE_LEFT) {
            FloatingPremiumStickerEdge.LEFT
        } else {
            FloatingPremiumStickerEdge.RIGHT
        }
        return FloatingPremiumStickerPosition(
            edge = edge,
            verticalFraction = preferences.getFloat(KEY_VERTICAL_FRACTION, DEFAULT_VERTICAL_FRACTION)
                .coerceIn(0f, 1f)
        )
    }

    fun save(position: FloatingPremiumStickerPosition) {
        preferences.edit()
            .putString(KEY_EDGE, if (position.edge == FloatingPremiumStickerEdge.LEFT) EDGE_LEFT else EDGE_RIGHT)
            .putFloat(KEY_VERTICAL_FRACTION, position.verticalFraction)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "floating_premium_sticker"
        const val KEY_EDGE = "edge"
        const val KEY_VERTICAL_FRACTION = "vertical_fraction"
        const val EDGE_LEFT = "left"
        const val EDGE_RIGHT = "right"
        const val DEFAULT_VERTICAL_FRACTION = 0.28f
    }
}

internal data class FloatingPremiumStickerPosition(
    val edge: FloatingPremiumStickerEdge,
    val verticalFraction: Float
)

internal enum class FloatingPremiumStickerEdge {
    LEFT,
    RIGHT
}
