package com.vessbon.tenka.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class BlockHighlighter {

    private static List<BlockPos> highlightedBlocks = new ArrayList<>();

    public static void setHighlight(BlockPos block) {
        highlightedBlocks.clear();
        highlightedBlocks.add(block);
    }

    public static void setHighlights(List<BlockPos> blocks) {
        highlightedBlocks.clear();
        highlightedBlocks.addAll(blocks);
    }

    public static void clearHighlights() {
        highlightedBlocks.clear();
    }

    public static void render(RenderWorldLastEvent event) {

        if (highlightedBlocks.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        double dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        GlStateManager.translate(-dx, -dy, -dz);

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                1, 0);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        // Highlight selected blocks
        GlStateManager.color(1.0F, 0.0F, 0.0F, 0.4F);
        for (BlockPos pos : highlightedBlocks) {
            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();
            drawBox(wr, tess, x, y, z, 1, 1, 1);
        }

        tess.draw();

        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawBox(WorldRenderer wr, Tessellator tess,
                         double x, double y, double z,
                         double w, double h, double d) {

        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);

        // Bottom
        wr.pos(x, y, z).endVertex();
        wr.pos(x + w, y, z).endVertex();
        wr.pos(x + w, y, z + d).endVertex();
        wr.pos(x, y, z + d).endVertex();
        wr.pos(x, y, z).endVertex();

        // Top
        wr.pos(x, y + h, z).endVertex();
        wr.pos(x + w, y + h, z).endVertex();
        wr.pos(x + w, y + h, z + d).endVertex();
        wr.pos(x, y + h, z + d).endVertex();
        wr.pos(x, y + h, z).endVertex();

        // Vertical edges
        wr.pos(x + w, y + h, z).endVertex();
        wr.pos(x + w, y, z).endVertex();
        wr.pos(x + w, y, z + d).endVertex();
        wr.pos(x + w, y + h, z + d).endVertex();
        wr.pos(x, y + h, z + d).endVertex();
        wr.pos(x, y, z + d).endVertex();
        wr.pos(x, y, z).endVertex();
    }
}
