package com.vessbon.tenka.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class InputSimulator {

    private static final Minecraft mc = Minecraft.getMinecraft();


    public static void setKeybindState(KeyBinding keyBinding, boolean pressed) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), pressed);
    }

    public static void attackOnce() {
        setKeybindState(mc.gameSettings.keyBindAttack, true);
        setKeybindState(mc.gameSettings.keyBindAttack, false);
    }

    public static void holdAttack(boolean hold) {
        setKeybindState(mc.gameSettings.keyBindAttack, hold);
    }

    public static void useOnce() {
        setKeybindState(mc.gameSettings.keyBindUseItem, true);
        setKeybindState(mc.gameSettings.keyBindUseItem, false);
    }

    public static void holdUse(boolean hold) {
        setKeybindState(mc.gameSettings.keyBindUseItem, hold);
    }
}
