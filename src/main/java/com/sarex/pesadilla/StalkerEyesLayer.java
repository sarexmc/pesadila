package com.sarex.pesadilla;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Ojos y sonrisa que brillan en la oscuridad. */
public class StalkerEyesLayer extends EyesLayer<StalkerEntity, HumanoidModel<StalkerEntity>> {
    private static final RenderType EYES = RenderType.eyes(
            new ResourceLocation(PesadillaMod.MODID, "textures/entity/stalker_eyes.png"));

    public StalkerEyesLayer(RenderLayerParent<StalkerEntity, HumanoidModel<StalkerEntity>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
