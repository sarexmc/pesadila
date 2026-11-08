package com.sarex.pesadilla;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class JumpscareOverlay implements IGuiOverlay {
    private static final ResourceLocation FACE =
            new ResourceLocation(PesadillaMod.MODID, "textures/gui/jumpscare.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        if (ClientScare.ticks <= 0 || ClientScare.type < 0) return;
        float prog = Mth.clamp(1.0F - (ClientScare.ticks - partialTick) / (float) ClientScare.total, 0F, 1F);
        float fade = prog > 0.7F ? 1.0F - (prog - 0.7F) / 0.3F : 1.0F;
        RenderSystem.enableBlend();
        switch (ClientScare.type) {
            case 0 -> drawFace(g, w, h, prog, fade, true);
            case 2 -> drawFace(g, w, h, 0.5F, 1.0F, false);
            case 1 -> {
                float a = prog < 0.1F ? prog / 0.1F : fade;
                g.fill(0, 0, w, h, ((int) (Mth.clamp(a, 0F, 1F) * 255) << 24));
            }
            default -> { }
        }
        RenderSystem.disableBlend();
    }

    private void drawFace(GuiGraphics g, int w, int h, float prog, float fade, boolean full) {
        if (full && prog < 0.05F) {           // destello blanco inicial
            g.fill(0, 0, w, h, 0xFFFFFFFF);
            return;
        }
        g.fill(0, 0, w, h, ((int) (fade * 255) << 24));
        float scale = 0.85F + 0.45F * prog;   // la cara se te echa encima
        int size = (int) (h * scale);
        float amp = full ? 14F * (1F - prog * 0.6F) : 0F;
        int dx = (int) ((ClientScare.RNG.nextFloat() - 0.5F) * 2F * amp);
        int dy = (int) ((ClientScare.RNG.nextFloat() - 0.5F) * 2F * amp);
        int x = (w - size) / 2 + dx;
        int y = (h - size) / 2 + dy;
        g.setColor(1F, 1F, 1F, fade);
        g.blit(FACE, x, y, size, size, 0F, 0F, 256, 256, 256, 256);
        g.setColor(1F, 1F, 1F, 1F);
        if (full) {                           // vineta roja
            g.fill(0, 0, w, h, ((int) (0.18F * fade * 255) << 24) | 0x00AA0000);
        }
    }
}
