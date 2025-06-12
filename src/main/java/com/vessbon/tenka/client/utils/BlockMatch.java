package com.vessbon.tenka.client.utils;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;

public class BlockMatch {

    public final Block block;
    public final BlockPos pos;

    public BlockMatch(Block block, BlockPos pos) {
        this.block = block;
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "BlockMatch{" +
                "block=" + block +
                ", pos=" + pos +
                '}';
    }
}
