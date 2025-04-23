package org.afterlike.base.font

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexFormat
import org.lwjgl.opengl.GL11
import java.io.InputStream
import javax.imageio.ImageIO

/**
 * Simplified graphics utilities for font rendering
 */
object UGraphics {
    enum class DrawMode(val glMode: Int) {
        QUADS(GL11.GL_QUADS),
        TRIANGLES(GL11.GL_TRIANGLES),
        TRIANGLE_STRIP(GL11.GL_TRIANGLE_STRIP)
    }

    object CommonVertexFormats {
        val POSITION = DefaultVertexFormats.POSITION
        val POSITION_TEXTURE = DefaultVertexFormats.POSITION_TEX
    }

    fun getFromTessellator(): WorldRendererAdapter {
        val worldRenderer = Tessellator.getInstance().worldRenderer
        return WorldRendererAdapter(worldRenderer)
    }

    fun getTexture(inputStream: InputStream): ReleasedDynamicTexture {
        val bufferedImage = ImageIO.read(inputStream)
        return ReleasedDynamicTexture(bufferedImage)
    }

    inline fun configureTexture(glId: Int, configure: () -> Unit) {
        GlStateManager.bindTexture(glId)
        configure()
        GlStateManager.bindTexture(0)
    }

    class WorldRendererAdapter(private val worldRenderer: WorldRenderer) {
        fun beginWithActiveShader(drawMode: DrawMode, format: Any) {
            when (format) {
                is VertexFormat -> worldRenderer.begin(drawMode.glMode, format)
                CommonVertexFormats.POSITION -> worldRenderer.begin(drawMode.glMode, DefaultVertexFormats.POSITION)
                CommonVertexFormats.POSITION_TEXTURE -> worldRenderer.begin(
                    drawMode.glMode,
                    DefaultVertexFormats.POSITION_TEX
                )

                else -> throw IllegalArgumentException("Unsupported vertex format: $format")
            }
        }

        fun pos(matrixStack: UMatrixStack, x: Double, y: Double, z: Double): WorldRendererAdapter {
            worldRenderer.pos(x, y, z)
            return this
        }

        fun tex(u: Double, v: Double): WorldRendererAdapter {
            worldRenderer.tex(u, v)
            return this
        }

        fun endVertex() {
            worldRenderer.endVertex()
        }

        fun drawDirect() {
            Tessellator.getInstance().draw()
        }
    }
} 