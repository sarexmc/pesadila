package com.sarex.pesadilla;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StalkerRenderer extends HumanoidMobRenderer<StalkerEntity, HumanoidModel<StalkerEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PesadillaMod.MODID, "textures/entity/stalker.png");

    public StalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.4F);
        this.addLayer(new StalkerEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(StalkerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(StalkerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.95F, 1.3F, 0.95F);   // alto y delgado
    }
}
