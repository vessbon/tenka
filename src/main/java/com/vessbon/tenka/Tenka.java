package com.vessbon.tenka;

import com.vessbon.tenka.common.CommonProxy;
import com.vessbon.tenka.client.ModKeybinds;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Tenka.MODID, version = Tenka.VERSION)
public class Tenka {

    public static final String MODID = "tenka";
    public static final String VERSION = "1.0.0";

    @SidedProxy(clientSide = "com.vessbon.tenka.client.ClientProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModKeybinds.register();
        proxy.init();
    }
}
