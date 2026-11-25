package com.feni.nullskin;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NullSkinRenderer extends GeoEntityRenderer<NullSkinEntity> {
    public NullSkinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NullSkinModel());
    }
}