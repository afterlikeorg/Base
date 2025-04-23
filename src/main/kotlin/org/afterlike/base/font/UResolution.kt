package org.afterlike.base.font

import net.minecraft.client.Minecraft

/**
 * Simplified resolution utility for font rendering
 */
object UResolution {
    val scaleFactor: Int
        get() {
            val mc = Minecraft.getMinecraft()
            val screenWidth = mc.currentScreen?.width ?: 1920
            return if (screenWidth > 0) mc.displayWidth / screenWidth else 1
        }
} 