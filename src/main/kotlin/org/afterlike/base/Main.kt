package org.afterlike.base

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.afterlike.base.font.FontRenderer
import org.lwjgl.input.Keyboard

@Mod(modid = "base", useMetadata = true)
class Main {
    companion object {
        lateinit var openGuiKey: KeyBinding
    }

    @Mod.EventHandler
    fun preInit(evt: FMLPreInitializationEvent) {
        openGuiKey = KeyBinding(
            "Open Test GUI",
            Keyboard.KEY_G,
            "Base"
        )
        ClientRegistry.registerKeyBinding(openGuiKey)
        MinecraftForge.EVENT_BUS.register(this)
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        try {
            println("Initializing font shaders...")
            FontRenderer.initShaders()
            if (FontRenderer.areShadersInitialized()) {
                println("Font shaders initialized successfully in preInit")
            } else {
                println("WARNING: Font shaders didn't initialize properly")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("ERROR: Exception initializing font shaders: ${e.message}")
        }
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        if (openGuiKey.isPressed) {
            Minecraft.getMinecraft().displayGuiScreen(FontTestGui())
        }
    }
}
