package com.feni.nullskin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NullSkinMod.MODID)
public class NullSkinJoinDisclaimer {

    // Replace this with your actual channel handle, YouTube/TikTok/Discord link, or creator tag
    public static final String CHANNEL_INFO = "Sub 2 itskryto on youtube: https://www.youtube.com/channel/UCae8cJA67LyjLXBAFkFwMuQ";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean wipeSp = Config.deleteSingleplayerWorldOnCheat;
        boolean closeOnFirst = Config.closeClientOnFirstManifest;

        player.sendSystemMessage(Component.literal("=== NULLSKIN // SYSTEM NOTICE ===")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("Created by: " + CHANNEL_INFO)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("Psychological horror & escalation active. Adjust options anytime in Mods -> Config.")
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Safe commands (no anti-cheat trigger): /nullskin, /help, /list, /msg, /tell, /me")
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("Avoid /gamemode, /tp, /give, /kill, /effect unless anti-cheat is turned off.")
                .withStyle(ChatFormatting.RED));
        player.sendSystemMessage(Component.literal("Config -> SP World Wipe on Cheat: " + wipeSp + " | Close Client on 1st Manifest: " + closeOnFirst)
                .withStyle(ChatFormatting.YELLOW));
    }
}