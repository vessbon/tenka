package com.vessbon.tenka.client;

import com.vessbon.tenka.client.utils.BlockHighlighter;
import com.vessbon.tenka.client.features.farming.FarmHelper;
import com.vessbon.tenka.common.CommonProxy;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init() {

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new FarmHelper());
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {

        ItemStack heldItem = event.player.getHeldItem();

        if (heldItem != null) {
            // System.out.println("Player is holding: " + heldItem.getDisplayName());
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        BlockHighlighter.render(event);
    }

    @SubscribeEvent
    public void onItemPickup(EntityItemPickupEvent event) {

        ItemStack pickedUpItem = event.item.getEntityItem();
        System.out.println("Picked up item: " + pickedUpItem.getDisplayName());
    }
}
