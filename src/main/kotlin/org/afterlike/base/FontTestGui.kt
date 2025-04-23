package org.afterlike.base

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.afterlike.base.font.FontRenderer
import org.afterlike.base.font.UMatrixStack
import org.afterlike.base.font.data.Font
import org.lwjgl.input.Keyboard
import java.awt.Color

class FontTestGui : GuiScreen() {
    private lateinit var fontRenderer: FontRenderer
    private lateinit var semiBoldRenderer: FontRenderer
    private lateinit var mediumRenderer: FontRenderer
    private lateinit var extraBoldRenderer: FontRenderer

    private var pointSize = 8f
    private var errorMessage: String? = null

    override fun initGui() {
        super.initGui()
        if (!initializeShaders()) return
        loadFonts()
    }

    private fun initializeShaders(): Boolean {
        return try {
            if (!FontRenderer.areShadersInitialized()) {
                println("Initializing font shaders...")
                FontRenderer.initShaders()
                if (!FontRenderer.areShadersInitialized()) {
                    errorMessage = "Failed to initialize font shaders. Check logs."
                    println("ERROR: Font shaders failed to initialize!")
                    false
                } else {
                    println("Font shaders initialized successfully.")
                    true
                }
            } else true
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error initializing shaders: ${e.message}"
            false
        }
    }

    private fun loadFonts() {
        try {
            println("Loading font files from resources...")

            val regular = Font.fromResource("/assets/base/font/Inter-Regular")
            val bold = Font.fromResource("/assets/base/font/Inter-Bold")
            val semiBold = Font.fromResource("/assets/base/font/Inter-SemiBold")
            val medium = Font.fromResource("/assets/base/font/Inter-Medium")
            val extraBold = Font.fromResource("/assets/base/font/Inter-ExtraBold")

            fontRenderer = FontRenderer(regular, bold)
            semiBoldRenderer = FontRenderer(semiBold, extraBold)
            mediumRenderer = FontRenderer(medium, bold)
            extraBoldRenderer = FontRenderer(extraBold, extraBold)

            println("Font renderers created successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error loading fonts: ${e.message}"
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        GlStateManager.enableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)

        val matrixStack = UMatrixStack()
        drawCenteredString(mc.fontRendererObj, "Font Renderer Test", width / 2, 10, 0xFFFFFF)
        drawCenteredString(mc.fontRendererObj, "Press ESC to exit", width / 2, 22, 0xAAAAAA)

        if (errorMessage != null) {
            drawCenteredString(mc.fontRendererObj, "§cError: $errorMessage", width / 2, height / 2 - 10, 0xFFFFFF)
            drawCenteredString(mc.fontRendererObj, "§cCheck console for details", width / 2, height / 2 + 10, 0xFFFFFF)
            super.drawScreen(mouseX, mouseY, partialTicks)
            return
        }

        if (::fontRenderer.isInitialized) {
            try {
                val leftMargin = 20f
                var y = 45

                y = drawFontExamples(matrixStack, leftMargin, y, true)
                y = drawFontExamples(matrixStack, leftMargin, y, false)

                fontRenderer.drawString(matrixStack, "Text Formatting Examples:", Color.WHITE, leftMargin, y.toFloat(), pointSize, 1f, true)
                y += 30

                val formattingLines = listOf(
                    "§aGreen Text §9Blue Text §cRed Text §eYellow Text",
                    "§lBold §r§oItalic §r§nUnderline §r§mStrikethrough §r§kObfuscated",
                    "§cR§6a§ei§an§bb§9o§5w §cT§6e§ex§at"
                )

                for (line in formattingLines) {
                    fontRenderer.drawString(matrixStack, line, Color.WHITE, leftMargin, y.toFloat(), pointSize, 1f, true)
                    y += 25
                }

            } catch (e: Exception) {
                e.printStackTrace()
                drawCenteredString(mc.fontRendererObj, "§cError during rendering: ${e.message}", width / 2, height / 2, 0xFFFFFF)
                drawCenteredString(mc.fontRendererObj, "§cCheck console for stack trace", width / 2, height / 2 + 12, 0xFFFFFF)
            }
        } else {
            drawCenteredString(mc.fontRendererObj, "§cFont renderer failed to initialize", width / 2, height / 2, 0xFFFFFF)
            drawCenteredString(mc.fontRendererObj, "§cCheck console for errors", width / 2, height / 2 + 12, 0xFFFFFF)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawFontExamples(matrixStack: UMatrixStack, leftMargin: Float, startY: Int, withShadow: Boolean): Int {
        var y = startY
        val shadowLabel = if (withShadow) "with Shadow:" else "without Shadow:"
        fontRenderer.drawString(matrixStack, "Font Weights $shadowLabel", Color.WHITE, leftMargin, y.toFloat(), pointSize, 1f, withShadow)
        y += 30

        val lines = listOf(
            Triple(fontRenderer, "Regular - The quick brown fox jumps over the lazy dog", false),
            Triple(mediumRenderer, "Medium - The quick brown fox jumps over the lazy dog", false),
            Triple(semiBoldRenderer, "SemiBold - The quick brown fox jumps over the lazy dog", false),
            Triple(fontRenderer, "§lBold - The quick brown fox jumps over the lazy dog", false),
            Triple(extraBoldRenderer, "ExtraBold - The quick brown fox jumps over the lazy dog", false)
        )

        for ((renderer, text, _) in lines) {
            renderer.drawString(matrixStack, text, Color.WHITE, leftMargin, y.toFloat(), pointSize, 1f, withShadow)
            y += 25
        }

        return y + 15
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
        }
    }

    override fun doesGuiPauseGame(): Boolean = false
}
