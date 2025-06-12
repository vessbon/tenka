package com.vessbon.tenka.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class Utils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static PlayerRotation.Rotation blockPosToYawPitch(BlockPos blockPos, Vec3 playerPos) {

        final double diffX = blockPos.getX() - playerPos.xCoord + 0.5;
        final double diffY = blockPos.getY() - (playerPos.yCoord + mc.thePlayer.getEyeHeight()) + 0.5;
        final double diffZ = blockPos.getZ() - playerPos.zCoord + 0.5;

        final double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX))) - 90.0f;
        float pitch = (float) (-(Math.toDegrees(Math.atan2(diffY, dist))));

        final float finalYaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
        final float finalPitch = mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch);

        return new PlayerRotation.Rotation(finalYaw, finalPitch);
    }

    public static PlayerRotation.Rotation vecToYawPitch(Vec3 vec, Vec3 playerPos) {

        final double diffX = vec.xCoord - playerPos.xCoord;
        final double diffY = vec.yCoord - (playerPos.yCoord + mc.thePlayer.getEyeHeight()) + 0.5;
        final double diffZ = vec.zCoord - playerPos.zCoord;

        final double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX))) - 90.0f;
        float pitch = (float) (-(Math.toDegrees(Math.atan2(diffY, distance))));

        final float finalYaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
        final float finalPitch = mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch);

        return new PlayerRotation.Rotation(finalYaw, finalPitch);
    }

    public static void faceNearestCardinal() {
        float yaw = mc.thePlayer.rotationYaw;
        float snappedYaw;

        if (yaw >= -45 && yaw < 45) snappedYaw = 0;           // South
        else if (yaw >= 45 && yaw < 135) snappedYaw = 90;     // West
        else if (yaw >= -135 && yaw < -45) snappedYaw = -90;  // East
        else snappedYaw = 180;                                // North

        mc.thePlayer.rotationYaw = snappedYaw;
    }
}
