package com.vessbon.tenka.client;

import com.vessbon.tenka.client.features.farming.FarmHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class KeyInputHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private FarmHelper farmHelperInstance;

    public KeyInputHandler() {
        this.farmHelperInstance = FarmHelper.getInstance();
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (mc.currentScreen == null) {
            if (ModKeybinds.START_FARM_MACRO.isPressed()) {
                farmHelperInstance.start();
            }
            if (ModKeybinds.PAUSE_FARM_MACRO.isPressed()) {
                farmHelperInstance.pause();
            }
            if (ModKeybinds.STOP_FARM_MACRO.isPressed()) {
                farmHelperInstance.stop();
            }
        }
    }
}
