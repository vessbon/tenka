package com.vessbon.tenka.client.utils;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.*;

import java.util.Random;

public class Utils {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

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

    public static float returnNearestCardinalYaw() {
        float yaw = mc.thePlayer.rotationYaw;
        yaw = MathHelper.wrapAngleTo180_float(yaw);

        System.out.println(yaw);

        float snappedYaw;

        if (yaw >= -45 && yaw < 45) snappedYaw = 0f;        // SOUTH
        else if (yaw >= 45 && yaw < 135) snappedYaw = 90f;     // WEST
        else if (yaw >= -135 && yaw < -45) snappedYaw = -90f; // EAST
        else snappedYaw = 180f;                                // NORTH

        return snappedYaw;
    }

    public static BlockMatch getHoveredBlock() {

        MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;

        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos targetBlockPos = mop.getBlockPos();
            Block targetBlock = Minecraft.getMinecraft().theWorld.getBlockState(targetBlockPos).getBlock();
            if (targetBlockPos == null && targetBlock == null) return null;

            System.out.println("Looking at block: " + targetBlock.getLocalizedName());
            return new BlockMatch(targetBlock, targetBlockPos);
        }

        return null;
    }

    public static float getRandomFloat(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public static EnumFacing getClockwise(EnumFacing facing) {
        switch (facing) {
            case NORTH: return EnumFacing.EAST;
            case EAST: return EnumFacing.SOUTH;
            case SOUTH: return EnumFacing.WEST;
            case WEST: return EnumFacing.NORTH;
            default: return facing;
        }
    }

    public static EnumFacing getCounterClockwise(EnumFacing facing) {
        switch (facing) {
            case NORTH: return EnumFacing.WEST;
            case WEST: return EnumFacing.SOUTH;
            case SOUTH: return EnumFacing.EAST;
            case EAST: return EnumFacing.NORTH;
            default: return facing;
        }
    }
}
