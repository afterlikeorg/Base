package org.afterlike.base.font.data

import com.google.gson.JsonParser.parseReader
import org.afterlike.base.font.ReleasedDynamicTexture
import org.afterlike.base.font.UGraphics
import java.io.InputStream
import java.io.InputStreamReader

class Font(
    val fontInfo: FontInfo,
    private val atlas: InputStream
) {
    private lateinit var texture: ReleasedDynamicTexture

    fun getTexture(): ReleasedDynamicTexture {
        if (!::texture.isInitialized) {
            texture = UGraphics.getTexture(atlas)
        }
        return texture
    }

    companion object {
        fun fromResource(path: String): Font {
            println("Loading font from resource: $path")

            val jsonStream = findResource("$path.json")
                ?: throw IllegalArgumentException("Could not find font JSON at $path.json")

            println("Found font JSON: $path.json")
            val fontInfo = FontInfo.fromJson(parseReader(InputStreamReader(jsonStream)).asJsonObject)
            jsonStream.close()

            val atlasStream = findResource("$path.png")
                ?: throw IllegalArgumentException("Could not find font atlas at $path.png")

            println("Found font PNG: $path.png")

            return Font(fontInfo, atlasStream)
        }

        private fun findResource(path: String): InputStream? {
            val inputStream = Font::class.java.getResourceAsStream(path)
            if (inputStream != null) {
                println("Found resource: $path")
                return inputStream
            }

            println("ERROR: Resource not found: $path")
            return null
        }
    }
}