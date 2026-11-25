package com.feni.nullskin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(NullSkinMod.MODID)
public class NullSkinMod {
    public static final String MODID = "nullskin";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<NullSkinEntity>> NULLSKIN = ENTITY_TYPES.register("nullskin",
            () -> EntityType.Builder.of(NullSkinEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .build("nullskin"));

    public static final RegistryObject<EntityType<MimicStalkerEntity>> MIMIC_STALKER = ENTITY_TYPES.register("mimic_stalker",
            () -> EntityType.Builder.of(MimicStalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .build("mimic_stalker"));

    public static final RegistryObject<EntityType<TunnelBreacherEntity>> TUNNEL_BREACHER = ENTITY_TYPES.register("tunnel_breacher",
            () -> EntityType.Builder.of(TunnelBreacherEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 1.95F)
                    .build("tunnel_breacher"));

    public NullSkinMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(NullSkinMod::onAttributeCreate);
        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(NULLSKIN.get(), NullSkinEntity.createAttributes().build());
        event.put(MIMIC_STALKER.get(), MimicStalkerEntity.createAttributes().build());
        event.put(TUNNEL_BREACHER.get(), TunnelBreacherEntity.createAttributes().build());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NullSkin common setup initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NullSkin server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new NullSkinConfigScreen(parent))
            ));
            LOGGER.info("NullSkin client setup initialized for {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(NULLSKIN.get(), NullSkinRenderer::new);
            event.registerEntityRenderer(MIMIC_STALKER.get(), MimicStalkerRenderer::new);
            event.registerEntityRenderer(TUNNEL_BREACHER.get(), TunnelBreacherRenderer::new);
        }
    }
}