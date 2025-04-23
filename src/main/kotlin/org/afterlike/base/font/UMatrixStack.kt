package org.afterlike.base.font

import org.lwjgl.opengl.GL11

/**
 * Simplified matrix stack for font rendering
 */
class UMatrixStack {
    fun translate(x: Float, y: Float, z: Float) {
        GL11.glTranslatef(x, y, z)
    }

    fun runWithGlobalState(block: () -> Unit) {
        GL11.glPushMatrix()
        try {
            block()
        } finally {
            GL11.glPopMatrix()
        }
    }
} 