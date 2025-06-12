package com.vessbon.tenka.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class ModKeybinds {
    public static final KeyBinding START_FARM_MACRO = new KeyBinding(
            "Start Farming",
            Keyboard.KEY_C,
            "Tenka"
    );

    public static final KeyBinding RESUME_FARM_MACRO = new KeyBinding(
            "Pause and Resume Farming",
            Keyboard.KEY_K,
            "Tenka"
    );

    public static final KeyBinding STOP_FARM_MACRO = new KeyBinding(
            "Stop Farming",
            Keyboard.KEY_X,
            "Tenka"
    );

    public static void register() {
        ClientRegistry.registerKeyBinding(START_FARM_MACRO);
    }
}
