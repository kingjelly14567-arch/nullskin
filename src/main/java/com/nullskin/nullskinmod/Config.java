package com.feni.nullskin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = NullSkinMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DELETE_SINGLEPLAYER_WORLD_ON_CHEAT = BUILDER
            .comment("If true, cheating in a single-player world marks that world for deletion when exiting.")
            .define("deleteSingleplayerWorldOnCheat", false);

    public static final ForgeConfigSpec.BooleanValue CLOSE_CLIENT_ON_FIRST_MANIFEST = BUILDER
            .comment("If true, closes the Minecraft client once when NullSkin first manifests.")
            .define("closeClientOnFirstManifest", true);

    public static final ForgeConfigSpec.BooleanValue ANTI_CHEAT_ENABLED = BUILDER
            .comment("If true, punishes unauthorized creative/spectator mode or cheat commands.")
            .define("antiCheatEnabled", true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_COMMANDS = BUILDER
            .comment("Command names (without '/') that are safe to run and will NOT trigger anti-cheat.")
            .defineListAllowEmpty(
                    List.of("allowedCommands"),
                    () -> List.of("nullskin", "help", "list", "msg", "tell", "me", "seed", "tps"),
                    Config::validateCommandName
            );

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("Optional item IDs referenced by mod systems.")
            .defineListAllowEmpty(
                    List.of("items"),
                    () -> List.of("minecraft:iron_ingot"),
                    Config::validateItemName
            );

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean deleteSingleplayerWorldOnCheat = false;
    public static boolean closeClientOnFirstManifest = true;
    public static boolean antiCheatEnabled = true;
    public static Set<String> allowedCommands = Set.of("nullskin", "help", "list", "msg", "tell", "me", "seed", "tps");
    public static Set<Item> items = Set.of();

    private static boolean validateCommandName(final Object obj) {
        return obj instanceof String s && !s.isBlank();
    }

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof final String itemName)) {
            return false;
        }
        ResourceLocation loc = ResourceLocation.tryParse(itemName);
        return loc != null && ForgeRegistries.ITEMS.containsKey(loc);
    }

    public static void syncAndSave() {
        syncCachedValues();
        SPEC.save();
    }

    public static boolean isCommandAllowed(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return false;
        }
        String clean = commandName.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        int spaceIdx = clean.indexOf(' ');
        if (spaceIdx >= 0) {
            clean = clean.substring(0, spaceIdx);
        }
        int colonIdx = clean.indexOf(':');
        if (colonIdx >= 0) {
            clean = clean.substring(colonIdx + 1);
        }
        return allowedCommands.contains(clean);
    }

    private static void syncCachedValues() {
        deleteSingleplayerWorldOnCheat = DELETE_SINGLEPLAYER_WORLD_ON_CHEAT.get();
        closeClientOnFirstManifest = CLOSE_CLIENT_ON_FIRST_MANIFEST.get();
        antiCheatEnabled = ANTI_CHEAT_ENABLED.get();

        Set<String> parsedCommands = new HashSet<>();
        for (String cmd : ALLOWED_COMMANDS.get()) {
            if (cmd != null && !cmd.isBlank()) {
                parsedCommands.add(cmd.trim().toLowerCase(Locale.ROOT));
            }
        }
        allowedCommands = Set.copyOf(parsedCommands);

        items = ITEM_STRINGS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(loc -> loc != null && ForgeRegistries.ITEMS.containsKey(loc))
                .map(ForgeRegistries.ITEMS::getValue)
                .collect(Collectors.toSet());
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            syncCachedValues();
        }
    }
}