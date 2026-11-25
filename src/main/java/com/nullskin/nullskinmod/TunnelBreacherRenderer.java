package com.feni.nullskin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TunnelBreacherRenderer extends HumanoidMobRenderer<TunnelBreacherEntity, HumanoidModel<TunnelBreacherEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NullSkinMod.MODID, "textures/entity/tunnel_breacher.png");

    public TunnelBreacherRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(TunnelBreacherEntity entity) {
        return TEXTURE;
    }
}