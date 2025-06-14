package com.vessbon.tenka.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;

public class PlayerRotation {

    public Rotation startRotation;
    public Rotation endRotation;
    public long startTime = 0L;
    public long endTime = 0L;
    public volatile boolean done;

    private final Minecraft mc = Minecraft.getMinecraft();


    public PlayerRotation(Rotation rotation, long time) {

        done = false;

        startRotation = new Rotation(
                MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationPitch));
        endRotation = rotation;
        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis() + time;

        MinecraftForge.EVENT_BUS.register(new PlayerRotationUtil(
                new RotationData(startRotation, endRotation, startTime, endTime, done)));
    }

    public static class Rotation {

        public float yaw;
        public float pitch;

        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public String toString() {
            return "Rotation{" +
                    "yaw=" + yaw +
                    ", pitch=" + pitch +
                    '}';
        }
    }

    public static class RotationData {
        public Rotation startRotation;
        public Rotation endRotation;
        public long startTime;
        public long endTime;
        public boolean done;

        public RotationData(Rotation startRotation, Rotation endRotation,
                            long startTime, long endTime, boolean done) {
            this.startRotation = startRotation;
            this.endRotation = endRotation;
            this.startTime = startTime;
            this.endTime = endTime;
            this.done = done;
        }

        @Override
        public String toString() {
            return "RotationData{" +
                    "startRotation=" + startRotation +
                    ", endRotation=" + endRotation +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", done=" + done +
                    '}';
        }
    }
}
