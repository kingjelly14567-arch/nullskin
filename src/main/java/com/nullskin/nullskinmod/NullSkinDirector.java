package com.feni.nullskin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = NullSkinMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NullSkinDirector {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, Integer> PLAYER_MINING_COOLDOWN = new HashMap<>();

    private static long worldTickCounter = 0;
    private static int nextAmbientPulseTick = 2400;
    private static int nextSpawnCheckTick = 1200;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        NullSkinEscalationData data = NullSkinEscalationData.get(level);

        if (player.getY() < 45 && RANDOM.nextInt(100) < 18) {
            data.addEscalationPoints(1);
            triggerCaveAudioOrBreach(level, player, data);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        worldTickCounter++;
        NullSkinEscalationData data = NullSkinEscalationData.get(overworld);
        int offset = Math.max(200, data.getRngOffsetTicks() / 12);

        if (worldTickCounter % (600 + offset) == 0) {
            int gain = 1;
            for (ServerPlayer p : players) {
                if (p.getY() < 32 || !p.level().isDay()) {
                    gain = 2;
                    break;
                }
            }
            data.addEscalationPoints(gain);
        }

        if (worldTickCounter >= nextAmbientPulseTick) {
            NullSystemMessages.sendAmbientSystemPulse(overworld);
            nextAmbientPulseTick = (int) worldTickCounter + 1800 + RANDOM.nextInt(2400) + offset;
        }

        for (ServerPlayer player : players) {
            int cd = PLAYER_MINING_COOLDOWN.getOrDefault(player.getUUID(), 0);
            if (cd > 0) {
                PLAYER_MINING_COOLDOWN.put(player.getUUID(), cd - 1);
                continue;
            }

            boolean underground = player.getY() < 48;
            if (underground && RANDOM.nextInt(900) == 0) {
                triggerCaveAudioOrBreach(overworld, player, data);
                PLAYER_MINING_COOLDOWN.put(player.getUUID(), 300 + RANDOM.nextInt(400));
            }
        }

        if (data.getStage() >= 1 && worldTickCounter >= nextSpawnCheckTick) {
            NullSystemMessages.broadcastFirstManifestation(overworld);
            trySpawnNearRandomPlayer(overworld, players, data);
            nextSpawnCheckTick = (int) worldTickCounter + 1400 + RANDOM.nextInt(2000) + offset;
        }
    }

    public static void triggerCaveAudioOrBreach(ServerLevel level, ServerPlayer player, NullSkinEscalationData data) {
        int stage = data.getStage();

        double yawRad = Math.toRadians(player.getYRot());
        double behindX = player.getX() + Math.sin(yawRad) * (4.0 + RANDOM.nextDouble() * 4.0);
        double behindZ = player.getZ() - Math.cos(yawRad) * (4.0 + RANDOM.nextDouble() * 4.0);
        BlockPos behindPos = BlockPos.containing(behindX, player.getY(), behindZ);

        SoundEvent stepSound = RANDOM.nextBoolean() ? SoundEvents.DEEPSLATE_STEP : SoundEvents.STONE_STEP;
        level.playSound(null, behindPos, stepSound, SoundSource.HOSTILE, 1.35F, 0.75F + RANDOM.nextFloat() * 0.2F);

        boolean realBreach = stage >= 2 && RANDOM.nextInt(100) < (stage * 18);

        BlockPos targetBlockPos = findBreakableCandidate(level, behindPos, realBreach);
        if (targetBlockPos != null) {
            BlockState state = level.getBlockState(targetBlockPos);
            SoundEvent breakSound = state.getSoundType(level, targetBlockPos, null).getBreakSound();

            level.playSound(null, targetBlockPos, breakSound, SoundSource.HOSTILE, 1.5F, 0.85F);
            level.destroyBlockProgress(player.getId() ^ targetBlockPos.hashCode(), targetBlockPos, realBreach ? 9 : 5);

            if (realBreach) {
                level.destroyBlock(targetBlockPos, false);
                knockOutNearbyTorch(level, player.blockPosition());
                NullSystemMessages.sendCaveWarning(player, stage, true);
            } else if (stage >= 1 && RANDOM.nextInt(100) < 45) {
                NullSystemMessages.sendCaveWarning(player, stage, false);
            }
        }
    }

    public static void forceCaveScare(ServerLevel level, ServerPlayer player, NullSkinEscalationData data, boolean forceReal) {
        double yawRad = Math.toRadians(player.getYRot());
        double behindX = player.getX() + Math.sin(yawRad) * 4.5;
        double behindZ = player.getZ() - Math.cos(yawRad) * 4.5;
        BlockPos behindPos = BlockPos.containing(behindX, player.getY(), behindZ);

        level.playSound(null, behindPos, SoundEvents.DEEPSLATE_STEP, SoundSource.HOSTILE, 1.4F, 0.8F);
        BlockPos candidate = findBreakableCandidate(level, behindPos, forceReal);
        if (candidate != null) {
            BlockState state = level.getBlockState(candidate);
            level.playSound(null, candidate, state.getSoundType(level, candidate, null).getBreakSound(), SoundSource.HOSTILE, 1.5F, 0.85F);
            level.destroyBlockProgress(player.getId() ^ candidate.hashCode(), candidate, forceReal ? 9 : 5);
            if (forceReal) {
                level.destroyBlock(candidate, false);
                knockOutNearbyTorch(level, player.blockPosition());
            }
        }
        NullSystemMessages.sendCaveWarning(player, Math.max(2, data.getStage()), forceReal);
    }

    private static BlockPos findBreakableCandidate(ServerLevel level, BlockPos center, boolean realBreach) {
        for (int i = 0; i < 8; i++) {
            BlockPos candidate = center.offset(
                    RANDOM.nextInt(5) - 2,
                    RANDOM.nextInt(3) - 1,
                    RANDOM.nextInt(5) - 2
            );
            BlockState state = level.getBlockState(candidate);
            if (state.isAir() || state.liquid()) {
                continue;
            }
            if (!realBreach) {
                return candidate;
            }
            if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                    || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SAND)
                    || state.is(Blocks.OAK_PLANKS) || state.is(Blocks.SPRUCE_PLANKS)) {
                return candidate;
            }
        }
        return null;
    }

    private static void knockOutNearbyTorch(ServerLevel level, BlockPos playerPos) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                        level.destroyBlock(pos, true);
                        return;
                    }
                }
            }
        }
    }

    private static void trySpawnNearRandomPlayer(ServerLevel level, List<ServerPlayer> players, NullSkinEscalationData data) {
        ServerPlayer target = players.get(RANDOM.nextInt(players.size()));
        int stage = data.getStage();
        boolean underground = target.getY() < 48;

        int distance = stage >= 3 ? 10 : 18;
        double angle = RANDOM.nextDouble() * Math.PI * 2;

        int spawnX = (int) Math.round(target.getX() + Math.cos(angle) * distance);
        int spawnZ = (int) Math.round(target.getZ() + Math.sin(angle) * distance);
        int spawnY = target.blockPosition().getY();

        BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);
        while (spawnY > level.getMinBuildHeight() + 1 && level.getBlockState(spawnPos).isAir()
                && level.getBlockState(spawnPos.below()).isAir()) {
            spawnY--;
            spawnPos = new BlockPos(spawnX, spawnY, spawnZ);
        }

        int roll = RANDOM.nextInt(100);
        if (stage == 1) {
            if (roll < 75) {
                NullSkinMod.MIMIC_STALKER.get().spawn(level, spawnPos, MobSpawnType.EVENT);
            } else {
                NullSkinMod.NULLSKIN.get().spawn(level, spawnPos, MobSpawnType.EVENT);
            }
        } else if (stage == 2) {
            if (underground && roll < 45) {
                NullSkinMod.TUNNEL_BREACHER.get().spawn(level, spawnPos, MobSpawnType.EVENT);
            } else if (roll < 75) {
                NullSkinMod.MIMIC_STALKER.get().spawn(level, spawnPos, MobSpawnType.EVENT);
            } else {
                NullSkinMod.NULLSKIN.get().spawn(level, spawnPos, MobSpawnType.EVENT);
            }
        } else {
            if (underground && roll < 50) {
                NullSkinMod.TUNNEL_BREACHER.get().spawn(level, spawnPos, MobSpawnType.EVENT);
            } else {
                NullSkinMod.NULLSKIN.get().spawn(level, spawnPos, MobSpawnType.EVENT);
            }
        }
    }
}