package com.feni.nullskin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = NullSkinMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NullSkinChatHandler {

    private static final List<PendingReply> PENDING_REPLIES = new ArrayList<>();
    private static final Map<UUID, Integer> CHEAT_STRIKES = new HashMap<>();

    private static class PendingReply {
        final UUID playerId;
        final String message;
        int ticksRemaining;

        PendingReply(UUID playerId, String message, int ticksRemaining) {
            this.playerId = playerId;
            this.message = message;
            this.ticksRemaining = ticksRemaining;
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        PENDING_REPLIES.add(new PendingReply(player.getUUID(), event.getRawText(), 24));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_REPLIES.isEmpty()) {
            return;
        }

        MinecraftServer server = event.getServer();
        Iterator<PendingReply> it = PENDING_REPLIES.iterator();
        while (it.hasNext()) {
            PendingReply pending = it.next();
            pending.ticksRemaining--;
            if (pending.ticksRemaining <= 0) {
                ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);
                if (player != null) {
                    NullSystemMessages.handlePlayerChatReply(player, pending.message);
                }
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        NullSkinEscalationData data = NullSkinEscalationData.get(level);
        if (data.getStage() < 2) {
            return;
        }

        String raw = event.getParseResults().getReader().getString().trim().toLowerCase();
        if (raw.startsWith("/")) {
            raw = raw.substring(1);
        }

        if (raw.startsWith("nullskin")) {
            return;
        }

        if (raw.startsWith("gamemode") || raw.startsWith("give") || raw.startsWith("tp")
                || raw.startsWith("kill") || raw.startsWith("op") || raw.startsWith("effect")) {
            event.setCanceled(true);
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }

            server.getPlayerList().deop(player.getGameProfile());
            player.setGameMode(GameType.SURVIVAL);

            int strikes = CHEAT_STRIKES.merge(player.getUUID(), 1, Integer::sum);
            if (strikes == 1) {
                player.sendSystemMessage(Component.literal("<NullSkin> ")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                        .append(Component.literal("No commands. No safety.").withStyle(ChatFormatting.DARK_RED)));
            } else if (strikes == 2) {
                player.connection.disconnect(Component.literal("NullSkin revoked your session."));
            } else {
                server.getPlayerList().getBans().add(new UserBanListEntry(
                        player.getGameProfile(), null, "NullSkin", null, "NullSkin overwrote your access."
                ));
                player.connection.disconnect(Component.literal("You are banned by NullSkin."));
            }
        }
    }
}