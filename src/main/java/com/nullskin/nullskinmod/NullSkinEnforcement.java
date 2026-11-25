package com.feni.nullskin;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = NullSkinMod.MODID)
public class NullSkinEnforcement {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> PUNISHED_COMMANDS = Set.of(
            "gamemode", "gm", "tp", "teleport", "give", "effect", "kill", "summon", "setblock", "fill"
    );

    private static final Set<UUID> FLAGGED_PLAYERS = new HashSet<>();
    private static boolean pendingSingleplayerWorldDelete = false;
    private static Path pendingWorldFolder = null;
    private static boolean firstManifestClientExitTriggered = false;

    public static void onFirstManifestation(ServerPlayer targetPlayer) {
        if (!Config.closeClientOnFirstManifest || firstManifestClientExitTriggered) {
            return;
        }
        firstManifestClientExitTriggered = true;
        targetPlayer.sendSystemMessage(Component.literal("NULLSKIN HAS MANIFESTED.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientCloseHelper.requestClientClose();
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!Config.antiCheatEnabled) return;

        var source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;

        String rawInput = event.getParseResults().getReader().getString();
        if (Config.isCommandAllowed(rawInput)) {
            return;
        }

        String rootCmd = extractRootCommand(rawInput);
        if (PUNISHED_COMMANDS.contains(rootCmd)) {
            punishCheater(player, "Used restricted command /" + rootCmd);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.antiCheatEnabled) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        GameType mode = player.gameMode.getGameModeForPlayer();
        if (mode == GameType.CREATIVE || mode == GameType.SPECTATOR) {
            if (FLAGGED_PLAYERS.add(player.getUUID())) {
                punishCheater(player, "Entered " + mode.getName().toUpperCase() + " mode");
            }
        } else {
            FLAGGED_PLAYERS.remove(player.getUUID());
        }
    }

    public static void punishCheater(ServerPlayer player, String reason) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        LOGGER.warn("NullSkin anti-cheat triggered for {} ({}): {}", player.getGameProfile().getName(), player.getUUID(), reason);

        player.setGameMode(GameType.SURVIVAL);
        player.sendSystemMessage(Component.literal("[NULLSKIN] Cheating detected: " + reason)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

        NullSkinEntity entity = NullSkinMod.NULLSKIN.get().create(player.serverLevel());
        if (entity != null) {
            entity.moveTo(player.getX() + 2.0D, player.getY(), player.getZ() + 2.0D, player.getYRot(), 0.0F);
            player.serverLevel().addFreshEntity(entity);
        }

        if (server.isSingleplayer() && Config.deleteSingleplayerWorldOnCheat) {
            pendingSingleplayerWorldDelete = true;
            pendingWorldFolder = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            player.sendSystemMessage(Component.literal("[NULLSKIN] Single-player world marked for deletion upon exit.")
                    .withStyle(ChatFormatting.RED));
        } else if (!server.isSingleplayer()) {
            player.connection.disconnect(Component.literal("Disconnected by NullSkin anti-cheat: " + reason));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FLAGGED_PLAYERS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (!pendingSingleplayerWorldDelete || pendingWorldFolder == null) {
            return;
        }

        Path folderToDelete = pendingWorldFolder;
        pendingSingleplayerWorldDelete = false;
        pendingWorldFolder = null;

        deleteDirectoryQuietly(folderToDelete);
    }

    private static String extractRootCommand(String input) {
        String trimmed = input == null ? "" : input.trim().toLowerCase();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        if (space >= 0) {
            trimmed = trimmed.substring(0, space);
        }
        int colon = trimmed.indexOf(':');
        if (colon >= 0) {
            trimmed = trimmed.substring(colon + 1);
        }
        return trimmed;
    }

    private static void deleteDirectoryQuietly(Path root) {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    LOGGER.error("Failed to delete world file {}: {}", path, ex.getMessage());
                }
            });
            LOGGER.warn("Deleted single-player world folder: {}", root);
        } catch (IOException ex) {
            LOGGER.error("Failed walking world directory {}: {}", root, ex.getMessage());
        }
    }

    private static final class ClientCloseHelper {
        private static void requestClientClose() {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(mc::stop);
        }
    }
}