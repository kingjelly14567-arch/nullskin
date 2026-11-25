package com.feni.nullskin;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NullSkinModel extends GeoModel<NullSkinEntity> {
    @Override
    public ResourceLocation getModelResource(NullSkinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(NullSkinMod.MODID, "geo/nullskin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NullSkinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(NullSkinMod.MODID, "textures/entity/nullskin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NullSkinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(NullSkinMod.MODID, "animations/nullskin.animation.json");
    }
}