package com.feni.nullskin;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@ForgeVoicechatPlugin
public class NullSkinVoiceAddon implements VoicechatPlugin {
    private static final int MAX_BUFFERED_FRAMES = 150; // ~3 seconds of 20ms PCM frames

    @Nullable
    private static VoicechatServerApi serverApi;

    private static final Map<UUID, Deque<short[]>> RECORDED_PCM_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, OpusDecoder> DECODERS = new ConcurrentHashMap<>();
    private static final Map<UUID, AudioPlayer> ACTIVE_PLAYERS = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "nullskin_voice";
    }

    @Override
    public void initialize(VoicechatApi api) {
        // Client/common init hook
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, NullSkinVoiceAddon::onMicrophonePacket);
    }

    private static void onMicrophonePacket(MicrophonePacketEvent event) {
        if (serverApi == null) {
            serverApi = event.getVoicechat();
        }

        VoicechatConnection senderConn = event.getSenderConnection();
        if (senderConn == null || !(senderConn.getPlayer().getPlayer() instanceof Player speakingPlayer)) {
            return;
        }

        UUID speakerId = speakingPlayer.getUUID();
        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData == null || opusData.length == 0) {
            return;
        }

        OpusDecoder decoder = DECODERS.computeIfAbsent(speakerId, id -> serverApi.createDecoder());
        short[] pcm = decoder.decode(opusData);
        if (pcm == null || pcm.length == 0) {
            return;
        }

        // Record audio if this player is currently possessed by a nearby NullSkinEntity
        List<NullSkinEntity> skins = speakingPlayer.level().getEntitiesOfClass(
                NullSkinEntity.class,
                speakingPlayer.getBoundingBox().inflate(48.0D)
        );

        for (NullSkinEntity skin : skins) {
            if (skin.isAlive() && speakerId.equals(skin.getPossessedPlayerId())) {
                Deque<short[]> buffer = RECORDED_PCM_BY_PLAYER.computeIfAbsent(speakerId, id -> new ArrayDeque<>());
                synchronized (buffer) {
                    if (buffer.size() >= MAX_BUFFERED_FRAMES) {
                        buffer.pollFirst();
                    }
                    buffer.addLast(pcm);
                }

                // Trigger playback to the linked friend if they are near the NullSkin entity
                Player friend = skin.getFriendPlayerId() != null
                        ? speakingPlayer.level().getPlayerByUUID(skin.getFriendPlayerId())
                        : null;
                if (friend != null && friend.isAlive() && friend.distanceToSqr(skin) <= 256.0D) {
                    triggerPlaybackToFriend(skin, speakerId);
                }
            }
        }
    }

    public static void triggerPlaybackToFriend(NullSkinEntity skin, UUID possessedPlayerId) {
        if (serverApi == null || !(skin.level() instanceof ServerLevel mcServerLevel)) {
            return;
        }

        AudioPlayer existing = ACTIVE_PLAYERS.get(skin.getUUID());
        if (existing != null && existing.isPlaying()) {
            skin.setPlayingBackVoice(true);
            return;
        }

        Deque<short[]> buffer = RECORDED_PCM_BY_PLAYER.get(possessedPlayerId);
        if (buffer == null || buffer.isEmpty()) {
            return;
        }

        LocationalAudioChannel channel = serverApi.createLocationalAudioChannel(
                UUID.randomUUID(),
                serverApi.fromServerLevel(mcServerLevel),
                serverApi.createPosition(skin.getX(), skin.getEyeY(), skin.getZ())
        );
        if (channel == null) {
            return;
        }

        channel.setDistance(24.0F);
        OpusEncoder encoder = serverApi.createEncoder();

        Supplier<short[]> frameSupplier = () -> {
            synchronized (buffer) {
                short[] next = buffer.pollFirst();
                if (next == null) {
                    skin.setPlayingBackVoice(false);
                    ACTIVE_PLAYERS.remove(skin.getUUID());
                    return null;
                }
                return next;
            }
        };

        AudioPlayer player = serverApi.createAudioPlayer(channel, encoder, frameSupplier);
        ACTIVE_PLAYERS.put(skin.getUUID(), player);
        skin.setPlayingBackVoice(true);
        player.startPlaying();
    }
}