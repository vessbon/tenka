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

    public static final KeyBinding PAUSE_FARM_MACRO = new KeyBinding(
            "Pause Farming",
            Keyboard.KEY_K,
            "Tenka"
    );

    public static final KeyBinding STOP_FARM_MACRO = new KeyBinding(
            "Stop Farming",
            Keyboard.KEY_F7,
            "Tenka"
    );

    public static final KeyBinding CHECK_TURN = new KeyBinding(
            "Stop Farming",
            Keyboard.KEY_H,
            "Tenka"
    );

    public static void register() {
        ClientRegistry.registerKeyBinding(START_FARM_MACRO);
    }
}
