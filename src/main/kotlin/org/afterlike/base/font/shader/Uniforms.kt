package org.afterlike.base.font.shader

import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20

/**
 * Base class for all shader uniforms
 */
abstract class Uniform(protected val location: Int)

/**
 * Float uniform for shaders
 */
class FloatUniform(location: Int) : Uniform(location) {
    private var currentValue = 0f

    fun setValue(value: Float) {
        if (value != currentValue) {
            currentValue = value
            GL20.glUniform1f(location, value)
        }
    }
}

/**
 * Float2 (vec2) uniform for shaders
 */
class Float2Uniform(location: Int) : Uniform(location) {
    private var currentX = 0f
    private var currentY = 0f

    fun setValue(x: Float, y: Float) {
        if (x != currentX || y != currentY) {
            currentX = x
            currentY = y
            GL20.glUniform2f(location, x, y)
        }
    }
}

/**
 * Float4 (vec4) uniform for shaders
 */
class Float4Uniform(location: Int) : Uniform(location) {
    private var currentX = 0f
    private var currentY = 0f
    private var currentZ = 0f
    private var currentW = 0f

    fun setValue(x: Float, y: Float, z: Float, w: Float) {
        if (x != currentX || y != currentY || z != currentZ || w != currentW) {
            currentX = x
            currentY = y
            currentZ = z
            currentW = w
            GL20.glUniform4f(location, x, y, z, w)
        }
    }
}

/**
 * Sampler uniform for shaders
 */
class SamplerUniform(location: Int) : Uniform(location) {
    private var textureId: Int = 0
    private val textureUnit: Int = 0

    init {
        GL20.glUniform1i(location, textureUnit)
    }

    fun setValue(textureId: Int) {
        if (this.textureId == textureId) {
            return // Skip if texture hasn't changed
        }

        this.textureId = textureId

        // Save current texture state
        val prevActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
        val prevTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)

        // Bind the new texture to texture unit 0
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnit)
        GlStateManager.bindTexture(textureId)

        // Configure texture parameters for better text rendering
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP)

        // Restore previous state
        GL13.glActiveTexture(prevActiveTexture)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTextureBinding)
    }
} 