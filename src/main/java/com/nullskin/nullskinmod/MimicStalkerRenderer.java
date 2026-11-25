package com.feni.nullskin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MimicStalkerRenderer extends HumanoidMobRenderer<MimicStalkerEntity, HumanoidModel<MimicStalkerEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NullSkinMod.MODID, "textures/entity/mimic_stalker.png");

    public MimicStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(MimicStalkerEntity entity) {
        return TEXTURE;
    }
}