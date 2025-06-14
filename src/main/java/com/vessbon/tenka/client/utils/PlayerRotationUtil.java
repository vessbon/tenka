package com.vessbon.tenka.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerRotationUtil {

    public PlayerRotation.Rotation startRotation;
    public PlayerRotation.Rotation endRotation;
    public long startTime = 0L;
    public long endTime = 0L;
    public volatile boolean done;

    private final Minecraft mc = Minecraft.getMinecraft();


    public PlayerRotationUtil(PlayerRotation.RotationData rotationData) {

        this.startRotation = rotationData.startRotation;
        this.endRotation = rotationData.endRotation;
        this.startTime = rotationData.startTime;
        this.endTime = rotationData.endTime;
        this.done = rotationData.done;

        System.out.println(this.startRotation);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {

        if (mc.thePlayer == null || mc.theWorld == null) {
            MinecraftForge.EVENT_BUS.unregister(this);
            return;
        }

        if (System.currentTimeMillis() <= endTime) {
            mc.thePlayer.rotationYaw = interpolate(startRotation.yaw, endRotation.yaw);
            mc.thePlayer.rotationPitch = interpolate(startRotation.pitch, endRotation.pitch);

        } else if (!done) {
            mc.thePlayer.rotationYaw = endRotation.yaw;
            mc.thePlayer.rotationPitch = endRotation.pitch;
            this.done = true;

        } else {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    private float interpolate(float start, float end) {
        float spentMillis = (float) (System.currentTimeMillis() - startTime);
        float relativeProgress = spentMillis / (float) (endTime - startTime);

        return MathHelper.wrapAngleTo180_float(end - start) * easeOutCubic(relativeProgress) + start;
    }

    private float easeOutCubic(double number) {
        return (float) (1.0 - Math.pow(1.0 - number, 3.0));
    }
}
