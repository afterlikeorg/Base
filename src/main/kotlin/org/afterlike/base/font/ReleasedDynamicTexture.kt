package org.afterlike.base.font

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import java.awt.image.BufferedImage

/**
 * Simplified dynamic texture for font rendering
 */
class ReleasedDynamicTexture(image: BufferedImage) {
    private val texture: DynamicTexture = DynamicTexture(image)
    private val location = Minecraft.getMinecraft().textureManager.getDynamicTextureLocation("font", texture)
    val dynamicGlId: Int

    init {
        Minecraft.getMinecraft().textureManager.loadTexture(location, texture)
        dynamicGlId = texture.glTextureId
    }
} 