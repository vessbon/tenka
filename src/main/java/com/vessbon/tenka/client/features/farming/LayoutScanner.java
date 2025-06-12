package com.vessbon.tenka.client.features.farming;

import com.vessbon.tenka.client.utils.BlockMatch;
import com.vessbon.tenka.client.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.*;

public class LayoutScanner {

    private static final Minecraft mc = Minecraft.getMinecraft();


    public static boolean isRowAlongX(BlockPos center, int scanRange) {

        EnumFacing facing = mc.thePlayer.getHorizontalFacing();
        World world = mc.thePlayer.worldObj;

        int xCount = 0;
        int zCount = 0;

        // Check row along X
        for (int dx = -scanRange; dx <= scanRange; dx++) {
            if (dx == 0) continue; // Skip the center block
            BlockPos checkPos = center.add(dx, 0, 0);
            if (isCrop(world.getBlockState(checkPos).getBlock())) {
                xCount++;
            }
        }

        // Check row along Z
        for (int dz = -scanRange; dz <= scanRange; dz++) {
            if (dz == 0) continue; // Skip the center block
            BlockPos checkPos = center.add(0, 0, dz);
            if (isCrop(world.getBlockState(checkPos).getBlock())) {
                zCount++;
            }
        }

        if (xCount <= 0 && zCount <= 0) {
            if (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH) return true;
            return false;
        }

        return xCount >= zCount;
    }

    public static FarmHelper.FarmCommand initialTurnDirection(
            BlockPos startPos, int maxDistance, boolean isAlongX) {

        // Decide direction vectors based on axis
        int dx = isAlongX ? 1 : 0;
        int dz = isAlongX ? 0 : 1;

        int rightCropCount = countCropsInDirection(startPos, maxDistance, dx, dz);
        int leftCropCount = countCropsInDirection(startPos, maxDistance, -dx, -dz);

        return rightCropCount <= leftCropCount ? FarmHelper.FarmCommand.TURN_RIGHT : FarmHelper.FarmCommand.TURN_LEFT;
    }

    static int countCropsInDirection(BlockPos pos, int maxDist, int dx, int dz) {

        World world = mc.theWorld;
        int count = 0;
        int dryStreak = 0;

        for (int i = 1; i <= maxDist; i++) {

            BlockPos check = pos.add(dx * i, 0, dz * i);
            IBlockState state = world.getBlockState(check);
            Block block = state.getBlock();

            if (isCrop(block)) {
                if (block instanceof BlockCrops) {
                    PropertyInteger ageProperty = BlockCrops.AGE;
                    count += state.getValue(ageProperty);
                }
                else count++;

            } else {
                dryStreak++;
                if (dryStreak > 6) break;
            }
        }

        return count;
    }

    public static boolean checkTurnPointSeedCrops(EntityPlayerSP player, int depth, boolean isAlongX) {

        int dryStreak = 0;

        int x = 0;
        int z = 0;

        World world = mc.theWorld;

        double px = player.posX;
        double py = player.posY;
        double pz = player.posZ;

        if (isAlongX) {
            z = 1;
        } else {
            x = 1;
        }

        for (int offset = 1; offset <= depth; offset++) {

            double targetX = px + (player.getLookVec().xCoord * offset) * x;
            double targetY = py + 1;
            double targetZ = pz + (player.getLookVec().zCoord * offset) * z;

            BlockPos checkPos = new BlockPos(
                    MathHelper.floor_double(targetX),
                    MathHelper.floor_double(targetY),
                    MathHelper.floor_double(targetZ)
            );

            IBlockState state = world.getBlockState(checkPos);
            Block block = state.getBlock();

            if (!(block instanceof BlockCrops)) {
                // Not enough crops here, so this is the turn point
                dryStreak++;
                if (dryStreak > 3) {
                    System.out.println("Turning");
                    return true;
                }
            }
        }

        return false;
    }

    public static BlockMatch getHoveredCrop() {
        BlockMatch matchedBlock = Utils.getHoveredBlock();
        if (matchedBlock == null) return null;
        else if (!isCrop(matchedBlock.block)) return null;
        return matchedBlock;
    }

    public static BlockMatch findNearestCropPos(int radius) {

        if (radius < 0 || radius > 100) return null;

        World world = mc.thePlayer.worldObj;
        BlockPos playerPos = mc.thePlayer.getPosition();
        BlockMatch closestMatch = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos currentPos = playerPos.add(x, y, z);
                    Block block = world.getBlockState(currentPos).getBlock();

                    if (isCrop(block)) {
                        double distance = mc.thePlayer.getDistanceSq(currentPos);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestMatch = new BlockMatch(block, currentPos);
                        }
                    }
                }
            }
        }

        return closestMatch;
    }

    private static boolean isCrop(Block block) {
        return block == Blocks.wheat ||
                block == Blocks.carrots ||
                block == Blocks.potatoes ||
                block == Blocks.nether_wart ||
                block == Blocks.melon_block ||
                block == Blocks.pumpkin ||
                block == Blocks.brown_mushroom ||
                block == Blocks.red_mushroom ||
                block == Blocks.cactus ||
                block == Blocks.cocoa ||
                block == Blocks.reeds;
    }
}
