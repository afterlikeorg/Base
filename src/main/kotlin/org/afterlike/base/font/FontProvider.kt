package org.afterlike.base.font

import java.awt.Color

/**
 * Interface for font providers in the standalone implementation
 */
interface FontProvider {
    val cachedValue: FontProvider
    var recalculate: Boolean
    var constrainTo: Any?

    fun getStringWidth(string: String, pointSize: Float): Float

    fun getStringHeight(string: String, pointSize: Float): Float

    fun drawString(
        matrixStack: UMatrixStack,
        string: String,
        color: Color,
        x: Float,
        y: Float,
        originalPointSize: Float,
        scale: Float,
        shadow: Boolean = true,
        shadowColor: Color? = null
    )

    fun getBaseLineHeight(): Float

    fun getShadowHeight(): Float

    fun getBelowLineHeight(): Float
} 