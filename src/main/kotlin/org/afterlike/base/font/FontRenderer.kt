package org.afterlike.base.font

import org.afterlike.base.font.data.Font
import org.afterlike.base.font.data.Glyph
import org.afterlike.base.font.shader.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import java.awt.Color
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

/**
 * [MSDF](https://github.com/Chlumsky/msdfgen) Font Renderer for Minecraft 1.8.9
 * Standalone implementation based on the original FontRenderer
 */
class FontRenderer(
    private val regularFont: Font,
    private val boldFont: Font = regularFont
) : FontProvider {
    private var underline: Boolean = false
    private var strikethrough: Boolean = false
    private var bold: Boolean = false
    private var italics: Boolean = false
    private var obfuscated: Boolean = false
    private var textColor: Color? = null
    private var shadowColor: Color? = null
    private var drawingShadow = false
    private var activeFont: Font = regularFont
    override var cachedValue: FontProvider = this
    override var recalculate: Boolean = false
    override var constrainTo: Any? = null
    private var obfuscationCounter = 0
    private var lastObfuscationUpdate = System.currentTimeMillis()
    private val obfuscationRefreshRate = 50L

    override fun getStringWidth(string: String, pointSize: Float): Float {
        return getStringInformation(string, pointSize).first
    }

    override fun getStringHeight(string: String, pointSize: Float): Float {
        return getStringInformation(string, pointSize).second
    }

    private fun getStringInformation(string: String, pointSize: Float): Pair<Float, Float> {
        var width = 0f
        var height = 0f
        var currentPointSize = pointSize * 1.3623059867f
        var tempBold = false
        var tempFont = regularFont

        var i = 0
        while (i < string.length) {
            val char = string[i]

            // parse formatting codes
            if (char == '\u00a7' && i + 1 < string.length) {
                val j = ("0123456789abcdefklm" +
                        "nor").indexOf(string[i + 1])
                if (j == 17) {
                    tempBold = true
                    tempFont = boldFont
                } else if (j == 21) {
                    tempBold = false
                    tempFont = regularFont
                }
                i += 2
                continue
            }

            val glyph = tempFont.fontInfo.glyphs[char.code]
            if (glyph == null) {
                i++
                continue
            }
            val planeBounds = glyph.planeBounds

            if (planeBounds != null) {
                height = max((planeBounds.top - planeBounds.bottom) * currentPointSize, height)
            }

            val advanceScale = if (tempBold) 1.05f else 1.0f
            width += (glyph.advance * currentPointSize * advanceScale)
            i++
        }

        return Pair(width, height)
    }

    fun getLineHeight(pointSize: Float): Float {
        return activeFont.fontInfo.metrics.lineHeight * pointSize
    }

    /**
     * Changes font context. Use 1 for regular and 2 for bold
     */
    private fun switchFont(type: Int) {
        val tmp = activeFont
        when (type) {
            1 -> {
                activeFont = regularFont
            }

            2 -> {
                activeFont = boldFont
            }
        }

        if (activeFont != tmp) {
            val textureId = activeFont.getTexture().dynamicGlId
            samplerUniform.setValue(textureId)

            UGraphics.configureTexture(textureId) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP)
            }

            sdfTexel.setValue(1f / activeFont.fontInfo.atlas.width, 1f / activeFont.fontInfo.atlas.height)
        }
    }

    /**
     * Draw string with optional shadow
     */
    override fun drawString(
        matrixStack: UMatrixStack,
        string: String,
        color: Color,
        x: Float,
        y: Float,
        originalPointSize: Float,
        scale: Float,
        shadow: Boolean,
        shadowColor: Color?
    ) {
        if (!areShadersInitialized() || !shader.usable) {
            return
        }

        val effectiveSize = originalPointSize * scale * 1.3623059867f
        val adjustedY = y - effectiveSize * 0.2f

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastObfuscationUpdate > obfuscationRefreshRate) {
            obfuscationCounter++
            lastObfuscationUpdate = currentTime
        }

        if (shadow) {
            drawingShadow = true
            val effectiveShadow: Color? = shadowColor
            var baseColor = color.rgb

            if (effectiveShadow == null) {
                if (baseColor and -67108864 == 0) {
                    baseColor = baseColor or -16777216
                }
                baseColor = baseColor and 0xFCFCFC shr 2 or (baseColor and -16777216)
            }
            this.shadowColor = Color(baseColor)
            val shadowOffset = effectiveSize / 10
            matrixStack.translate(shadowOffset, shadowOffset, 0f)
            drawStringNow(matrixStack, string, Color(baseColor), x, adjustedY, effectiveSize)
            matrixStack.translate(-shadowOffset, -shadowOffset, 0f)
        }

        drawingShadow = false
        drawStringNow(matrixStack, string, color, x, adjustedY, effectiveSize)
    }

    override fun getBaseLineHeight(): Float {
        return regularFont.fontInfo.atlas.baseCharHeight
    }

    override fun getShadowHeight(): Float {
        return regularFont.fontInfo.atlas.shadowHeight
    }

    override fun getBelowLineHeight(): Float {
        return regularFont.fontInfo.atlas.belowLineHeight
    }

    private fun refreshColor(pointSize: Float) {
        val current = if (drawingShadow) shadowColor else textColor
        hintAmountUniform.setValue(0f)
    }

    /**
     * Determines if a character is a letter
     */
    private fun isLetter(c: Char): Boolean {
        return c in 'A'..'Z' || c in 'a'..'z'
    }

    /**
     * Determines if a character is a digit
     */
    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    /**
     * Gets a random character of the same type as the input
     */
    private fun getRandomCharacterOfSameType(c: Char, random: Random): Char {
        return when {
            isLetter(c) -> {
                if (c.isUpperCase()) {
                    ('A'..'Z').random(random)
                } else {
                    ('a'..'z').random(random)
                }
            }

            isDigit(c) -> ('0'..'9').random(random)
            else -> c
        }
    }

    private fun drawStringNow(
        matrixStack: UMatrixStack,
        string: String,
        color: Color,
        x: Float,
        y: Float,
        originalPointSize: Float
    ) {
        if (!areShadersInitialized() || !shader.usable) {
            return
        }

        var currentPointSize = originalPointSize

        // Bind shader
        shader.bind()

        // Switch to regular font
        switchFont(1)

        // Set uniforms
        doffsetUniform.setValue(3.5f / currentPointSize)

        val guiScale = UResolution.scaleFactor.toFloat()

        // Reset formatting
        obfuscated = false
        bold = false
        italics = false
        strikethrough = false
        underline = false
        textColor = color

        refreshColor(originalPointSize)

        val metrics = activeFont.fontInfo.metrics
        val rawBaseline = y + ((metrics.lineHeight + metrics.descender) * currentPointSize)
        val basePx = floor(rawBaseline * guiScale) / guiScale

        var penX = x
        var i = 0

        val obfuscationSeed = string.hashCode().toLong() + obfuscationCounter
        val obfuscatedRandom = Random(obfuscationSeed)

        while (i < string.length) {
            val char = string[i]

            // parse formatting codes
            if (char == '\u00a7' && i + 1 < string.length) {
                val j = "0123456789abcdefklmnor".indexOf(string[i + 1])
                when {
                    j < 16 -> {
                        switchFont(1)
                        obfuscated = false
                        bold = false
                        italics = false
                        strikethrough = false
                        underline = false
                        if (j < 0) {
                            textColor = colors[15]
                            shadowColor = colors[31]
                        } else {
                            textColor = colors[j]
                            shadowColor = colors[j + 16]
                        }
                        currentPointSize = originalPointSize
                        doffsetUniform.setValue(3.5f / currentPointSize)
                        refreshColor(originalPointSize)
                    }

                    j == 16 -> obfuscated = true
                    j == 17 -> {
                        switchFont(2)
                        bold = true
                    }

                    j == 18 -> strikethrough = true
                    j == 19 -> underline = true
                    j == 20 -> italics = true
                    else -> {
                        currentPointSize = originalPointSize
                        switchFont(1)
                        doffsetUniform.setValue(3.5f / currentPointSize)
                        obfuscated = false
                        bold = false
                        italics = false
                        strikethrough = false
                        underline = false
                        textColor = color
                        refreshColor(originalPointSize)
                    }
                }
                i += 2
                continue
            }

            val origGlyph = activeFont.fontInfo.glyphs[char.code]
            if (origGlyph == null) {
                i++
                continue
            }

            var glyph = origGlyph

            if (obfuscated && char != ' ') {
                val randomChar = getRandomCharacterOfSameType(char, obfuscatedRandom)
                val replacement = activeFont.fontInfo.glyphs[randomChar.code]
                if (replacement != null) {
                    glyph = replacement
                }
            }

            val planeBounds = glyph.planeBounds

            if (planeBounds != null) {
                val glyphX = penX + (planeBounds.left * currentPointSize)
                val glyphY = basePx - (planeBounds.top * currentPointSize)

                val width = (planeBounds.right - planeBounds.left) * currentPointSize
                val height = (planeBounds.top - planeBounds.bottom) * currentPointSize

                drawGlyph(
                    matrixStack,
                    glyph,
                    color,
                    glyphX,
                    glyphY,
                    width,
                    height
                )
            }

            val advanceScale = if (bold) 1.05f else 1.0f
            penX += (glyph.advance * currentPointSize * advanceScale)
            i++
        }

        shader.unbind()
        activeFont = regularFont
    }

    private fun drawGlyph(
        matrixStack: UMatrixStack,
        glyph: Glyph,
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val atlasBounds = glyph.atlasBounds ?: return
        val atlas = activeFont.fontInfo.atlas
        val textureTop = 1.0 - (atlasBounds.top / atlas.height)
        val textureBottom = 1.0 - (atlasBounds.bottom / atlas.height)
        val textureLeft = (atlasBounds.left / atlas.width).toDouble()
        val textureRight = (atlasBounds.right / atlas.width).toDouble()

        val drawColor = if (drawingShadow) (shadowColor ?: color) else (textColor ?: color)
        fgColorUniform.setValue(
            drawColor.red / 255f,
            drawColor.green / 255f,
            drawColor.blue / 255f,
            1f
        )

        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, activeFont.getTexture().dynamicGlId)

        val worldRenderer = UGraphics.getFromTessellator()
        worldRenderer.beginWithActiveShader(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE)

        val doubleX = x.toDouble()
        val doubleY = y.toDouble()
        worldRenderer.pos(matrixStack, doubleX, doubleY + height, 0.0).tex(textureLeft, textureBottom).endVertex()
        worldRenderer.pos(matrixStack, doubleX + width, doubleY + height, 0.0).tex(textureRight, textureBottom)
            .endVertex()
        worldRenderer.pos(matrixStack, doubleX + width, doubleY, 0.0).tex(textureRight, textureTop).endVertex()
        worldRenderer.pos(matrixStack, doubleX, doubleY, 0.0).tex(textureLeft, textureTop).endVertex()
        worldRenderer.drawDirect()
    }

    companion object {
        private val colors: Map<Int, Color> = mapOf(
            0 to Color.BLACK,
            1 to Color(0, 0, 170),
            2 to Color(0, 170, 0),
            3 to Color(0, 170, 170),
            4 to Color(170, 0, 0),
            5 to Color(170, 0, 170),
            6 to Color(255, 170, 0),
            7 to Color(170, 170, 170),
            8 to Color(85, 85, 85),
            9 to Color(85, 85, 255),
            10 to Color(85, 255, 85),
            11 to Color(85, 255, 255),
            12 to Color(255, 85, 85),
            13 to Color(255, 85, 255),
            14 to Color(255, 255, 85),
            15 to Color(255, 255, 255),
            // shadows for (i - 16)
            16 to Color.BLACK,
            17 to Color(0, 0, 42),
            18 to Color(0, 42, 0),
            19 to Color(0, 42, 42),
            20 to Color(42, 0, 0),
            21 to Color(42, 0, 42),
            22 to Color(42, 42, 0),
            23 to Color(42, 42, 42),
            24 to Color(21, 21, 21),
            25 to Color(21, 21, 63),
            26 to Color(21, 63, 21),
            27 to Color(21, 63, 63),
            28 to Color(63, 21, 21),
            29 to Color(63, 21, 63),
            30 to Color(63, 63, 21),
            31 to Color(63, 63, 63)
        )

        private lateinit var shader: UShader
        private lateinit var samplerUniform: SamplerUniform
        private lateinit var doffsetUniform: FloatUniform
        private lateinit var hintAmountUniform: FloatUniform
        private lateinit var sdfTexel: Float2Uniform
        private lateinit var fgColorUniform: Float4Uniform

        fun areShadersInitialized() = Companion::shader.isInitialized

        /**
         * Initializes the shader for font rendering
         * Must be called before any text rendering takes place
         */
        fun initShaders() {
            if (areShadersInitialized())
                return

            shader = UShader.readFromLegacyShader("font", "font", BlendState.NORMAL)
            if (!shader.usable) {
                println("Failed to load font shader")
                return
            }
            samplerUniform = shader.getSamplerUniform("msdf")
            doffsetUniform = shader.getFloatUniform("doffset")
            hintAmountUniform = shader.getFloatUniform("hint_amount")
            sdfTexel = shader.getFloat2Uniform("sdf_texel")
            fgColorUniform = shader.getFloat4Uniform("fgColor")
        }
    }
} 