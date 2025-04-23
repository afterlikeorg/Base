package org.afterlike.base.font.shader

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.io.*

/**
 * Simplified shader implementation for font rendering
 */
class UShader(
    private val programId: Int,
    val usable: Boolean
) {
    private val uniforms = mutableMapOf<String, Int>()

    fun bind() {
        if (usable) {
            GL20.glUseProgram(programId)
        } else {
            println("WARNING: Attempted to bind unusable shader!")
        }
    }

    fun unbind() {
        GL20.glUseProgram(0)
    }

    fun getFloatUniform(name: String): FloatUniform {
        val location = getUniformLocation(name)
        if (location == -1) {
            println("WARNING: Uniform '$name' not found in shader")
        }
        return FloatUniform(location)
    }

    fun getFloat2Uniform(name: String): Float2Uniform {
        val location = getUniformLocation(name)
        if (location == -1) {
            println("WARNING: Uniform '$name' not found in shader")
        }
        return Float2Uniform(location)
    }

    fun getFloat4Uniform(name: String): Float4Uniform {
        val location = getUniformLocation(name)
        if (location == -1) {
            println("WARNING: Uniform '$name' not found in shader")
        }
        return Float4Uniform(location)
    }

    fun getSamplerUniform(name: String): SamplerUniform {
        val location = getUniformLocation(name)
        if (location == -1) {
            println("WARNING: Uniform '$name' not found in shader")
        }
        return SamplerUniform(location)
    }

    private fun getUniformLocation(name: String): Int {
        return uniforms.getOrPut(name) {
            GL20.glGetUniformLocation(programId, name)
        }
    }

    companion object {
        fun readFromLegacyShader(vertexName: String, fragmentName: String, blendState: BlendState): UShader {
            try {
                println("Loading shaders: $vertexName.vsh and $fragmentName.fsh")

                // Load the vertex shader
                val vertexResourcePath = "/shaders/$vertexName.vsh"
                val vertexSource = tryReadShaderResource(vertexResourcePath)
                if (vertexSource == null) {
                    println("ERROR: Failed to load vertex shader from $vertexResourcePath")
                    return UShader(0, false)
                }
                println("Vertex shader loaded: ${vertexSource.lines().count()} lines")

                // Load the fragment shader
                val fragmentResourcePath = "/shaders/$fragmentName.fsh"
                val fragmentSource = tryReadShaderResource(fragmentResourcePath)
                if (fragmentSource == null) {
                    println("ERROR: Failed to load fragment shader from $fragmentResourcePath")
                    return UShader(0, false)
                }
                println("Fragment shader loaded: ${fragmentSource.lines().count()} lines")

                // Create shader program
                println("Creating shader program")
                val programId = GL20.glCreateProgram()

                // Compile vertex shader
                println("Compiling vertex shader")
                val vertexShaderId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER)
                GL20.glShaderSource(vertexShaderId, vertexSource)
                GL20.glCompileShader(vertexShaderId)

                if (GL20.glGetShaderi(vertexShaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                    val log = GL20.glGetShaderInfoLog(vertexShaderId, 32768)
                    println("ERROR: Failed to compile vertex shader: $log")
                    GL20.glDeleteShader(vertexShaderId)
                    return UShader(0, false)
                }

                // Compile fragment shader
                println("Compiling fragment shader")
                val fragmentShaderId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)
                GL20.glShaderSource(fragmentShaderId, fragmentSource)
                GL20.glCompileShader(fragmentShaderId)

                if (GL20.glGetShaderi(fragmentShaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                    val log = GL20.glGetShaderInfoLog(fragmentShaderId, 32768)
                    println("ERROR: Failed to compile fragment shader: $log")
                    GL20.glDeleteShader(vertexShaderId)
                    GL20.glDeleteShader(fragmentShaderId)
                    return UShader(0, false)
                }

                // Link program
                println("Linking shader program")
                GL20.glAttachShader(programId, vertexShaderId)
                GL20.glAttachShader(programId, fragmentShaderId)
                GL20.glLinkProgram(programId)

                if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                    val log = GL20.glGetProgramInfoLog(programId, 32768)
                    println("ERROR: Failed to link shader program: $log")
                    GL20.glDeleteShader(vertexShaderId)
                    GL20.glDeleteShader(fragmentShaderId)
                    GL20.glDeleteProgram(programId)
                    return UShader(0, false)
                }

                // Clean up
                GL20.glDeleteShader(vertexShaderId)
                GL20.glDeleteShader(fragmentShaderId)

                println("Shader program created successfully")
                return UShader(programId, true)
            } catch (e: Exception) {
                e.printStackTrace()
                println("ERROR: Exception during shader creation: ${e.message}")
                return UShader(0, false)
            }
        }

        /**
         * Try to read shader source using multiple methods to ensure resource is found
         */
        private fun tryReadShaderResource(resourcePath: String): String? {
            val inputStream: InputStream?

            try {
                inputStream = UShader::class.java.getResourceAsStream(resourcePath)
                if (inputStream != null) {
                    println("Found shader with UShader class loader: $resourcePath")
                    return readFromStream(inputStream)
                }
            } catch (e: Exception) {
                println("Failed to load shader with UShader class loader: ${e.message}")
            }

            println("ERROR: Could not find shader at $resourcePath using any method")
            return null
        }

        /**
         * Read shader source from an input stream
         */
        private fun readFromStream(inputStream: InputStream): String {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append('\n')
                }

                reader.close()
                return stringBuilder.toString()
            } catch (e: IOException) {
                e.printStackTrace()
                throw RuntimeException("Failed to read shader from stream", e)
            }
        }
    }
}

enum class BlendState {
    NORMAL,
    ADDITIVE,
    MULTIPLICATIVE
} 