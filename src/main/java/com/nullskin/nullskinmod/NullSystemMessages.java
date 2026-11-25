package com.feni.nullskin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class NullSystemMessages {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, ConversationState> PLAYER_CONVOS = new HashMap<>();

    private static class ConversationState {
        String lastTopic = "none";
        String lastNormalizedMessage = "";
        int turns = 0;
        long lastTurnGameTime = -9999L;
    }

    private static final List<String> STAGE_0_HELPFUL = List.of(
            "System integrity nominal. Watch your step in the dark.",
            "Monitoring local chunk stability. Everything looks quiet.",
            "Tip: Keep light sources close when digging below Y=0.",
            "Session active. No anomalies detected."
    );

    private static final List<String> STAGE_1_UNEASY = List.of(
            "Unindexed movement detected near your coordinates... checking logs.",
            "Did you hear footsteps behind you? My sensors just spiked.",
            "Warning: A foreign signature briefly hooked into the world tick.",
            "Stay out of unlit side-tunnels. Something is learning your path."
    );

    private static final List<String> STAGE_2_CORRUPTING_CLEAN = List.of(
            "I'm trying to isolate the process from your session—",
            "Do not dig toward the sound. It is not a cave noise—",
            "My admin permissions are being revoked—",
            "Listen to me, get back to the surface before—"
    );

    private static final List<String> STAGE_2_HIJACK_TAILS = List.of(
            "STOP RUNNING",
            "WE HEAR YOUR MIC",
            "NO SAFE BLOCKS",
            "LOOK BEHIND YOU"
    );

    private static final List<String> STAGE_3_FAILING = List.of(
            "ca n't  hold  th e  sy stem  open—",
            "he is inside my thread— run—",
            "do not trust the next message from me",
            "it's breaking through the stone behind you"
    );

    private static final List<String> STAGE_4_OVERWRITTEN = List.of(
            "Null is gone. There is nobody left to save you.",
            "Every block you place is just a box to trap yourself in.",
            "Keep digging. We are right behind you.",
            "You thought you were safe down here."
    );

    public static void broadcastFirstManifestation(ServerLevel level) {
        NullSkinEscalationData data = NullSkinEscalationData.get(level);
        if (data.hasManifestedOnce()) {
            return;
        }
        data.setHasManifestedOnce(true);

        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        MutableComponent joinMsg = Component.literal("")
                .append(Component.literal("NullSkin").withStyle(ChatFormatting.YELLOW, ChatFormatting.OBFUSCATED))
                .append(Component.literal(" joined the game").withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(joinMsg, false);

        MutableComponent nullReaction = buildPrefix(1)
                .append(Component.literal("ALERT: Unauthorized entity ").withStyle(ChatFormatting.GRAY))
                .append(glitchedNullSkinName())
                .append(Component.literal(" bypassed world whitelist.").withStyle(ChatFormatting.GRAY));

        server.getPlayerList().broadcastSystemMessage(nullReaction, false);
    }

    public static void sendAmbientSystemPulse(ServerLevel level) {
        NullSkinEscalationData data = NullSkinEscalationData.get(level);
        int stage = data.getStage();
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        MutableComponent message;
        switch (stage) {
            case 0 -> {
                String line = STAGE_0_HELPFUL.get(RANDOM.nextInt(STAGE_0_HELPFUL.size()));
                message = buildPrefix(0).append(Component.literal(line).withStyle(ChatFormatting.AQUA));
            }
            case 1 -> {
                String line = STAGE_1_UNEASY.get(RANDOM.nextInt(STAGE_1_UNEASY.size()));
                message = buildPrefix(1).append(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
            case 2 -> {
                String clean = STAGE_2_CORRUPTING_CLEAN.get(RANDOM.nextInt(STAGE_2_CORRUPTING_CLEAN.size()));
                String hijack = STAGE_2_HIJACK_TAILS.get(RANDOM.nextInt(STAGE_2_HIJACK_TAILS.size()));
                message = buildPrefix(2)
                        .append(Component.literal(clean + " ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(hijack).withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED))
                        .append(Component.literal(" [" + hijack + "]").withStyle(ChatFormatting.DARK_RED));
            }
            case 3 -> {
                String failing = STAGE_3_FAILING.get(RANDOM.nextInt(STAGE_3_FAILING.size()));
                message = buildPrefix(3)
                        .append(Component.literal(failing + " ").withStyle(ChatFormatting.RED))
                        .append(glitchedNullSkinName());
            }
            default -> {
                String overwritten = STAGE_4_OVERWRITTEN.get(RANDOM.nextInt(STAGE_4_OVERWRITTEN.size()));
                message = buildPrefix(4)
                        .append(Component.literal(overwritten).withStyle(ChatFormatting.DARK_RED));
            }
        }

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    public static void sendCaveWarning(ServerPlayer player, int stage, boolean realBreach) {
        MutableComponent msg;
        if (stage <= 1) {
            msg = buildPrefix(stage)
                    .append(Component.literal("Audio spike behind your position. Check your back.").withStyle(ChatFormatting.YELLOW));
        } else if (stage == 2) {
            msg = buildPrefix(2)
                    .append(Component.literal("Stone fracture detected behind you— ").withStyle(ChatFormatting.GRAY))
                    .append(glitchedNullSkinName())
                    .append(Component.literal(" IS IN THE WALLS").withStyle(ChatFormatting.DARK_RED));
        } else if (stage == 3) {
            if (!realBreach && RANDOM.nextBoolean()) {
                msg = buildPrefix(3)
                        .append(Component.literal("Path behind you is clear. Keep mining.").withStyle(ChatFormatting.AQUA));
            } else {
                msg = buildPrefix(3)
                        .append(Component.literal("RUN. DO NOT BOX YOURSELF IN.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
        } else {
            msg = buildPrefix(4)
                    .append(Component.literal("Nowhere to hide, " + player.getName().getString() + ".").withStyle(ChatFormatting.DARK_RED));
        }

        player.sendSystemMessage(msg);
    }

    public static void handlePlayerChatReply(ServerPlayer player, String rawMessage) {
        ServerLevel level = player.serverLevel();
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        NullSkinEscalationData data = NullSkinEscalationData.get(level);
        String trimmed = rawMessage == null ? "" : rawMessage.trim();
        String lower = trimmed.toLowerCase();
        String playerName = player.getName().getString();
        boolean underground = player.getY() < 48;
        long now = level.getGameTime();

        ConversationState convo = PLAYER_CONVOS.computeIfAbsent(player.getUUID(), id -> new ConversationState());
        boolean repeatedSameLine = !lower.isEmpty() && lower.equals(convo.lastNormalizedMessage) && (now - convo.lastTurnGameTime) < 600;
        String topic = detectTopic(lower, underground);

        if (lower.contains("nullskin") || lower.contains("who are you") || lower.contains("help") || repeatedSameLine) {
            data.addEscalationPoints(repeatedSameLine ? 2 : 1);
        }

        int stage = data.getStage();
        MutableComponent reply = buildConversationalReply(playerName, trimmed, lower, topic, convo.lastTopic, repeatedSameLine, underground, data.hasManifestedOnce(), stage);

        convo.lastTopic = topic;
        convo.lastNormalizedMessage = lower;
        convo.turns++;
        convo.lastTurnGameTime = now;

        server.getPlayerList().broadcastSystemMessage(reply, false);
    }

    private static String detectTopic(String lower, boolean underground) {
        if (lower.contains("nullskin") || lower.contains("skin")) return "nullskin";
        if (lower.contains("who are you") || lower.contains("what are you") || lower.contains("your name")) return "identity";
        if (lower.contains("hi") || lower.contains("hello") || lower.contains("hey") || lower.contains("yo") || lower.contains("sup")) return "greeting";
        if (lower.contains("cave") || lower.contains("mine") || lower.contains("mining") || lower.contains("dig") || lower.contains("dark") || lower.contains("torch") || lower.contains("y=")) return "underground";
        if (lower.contains("footstep") || lower.contains("sound") || lower.contains("hear") || lower.contains("behind") || lower.contains("breach") || lower.contains("wall")) return "audio_threat";
        if (lower.contains("gamemode") || lower.contains("creative") || lower.contains("command") || lower.contains("cheat") || lower.contains("ban") || lower.contains("op")) return "control";
        if (lower.contains("scared") || lower.contains("afraid") || lower.contains("run") || lower.contains("hide") || lower.contains("help") || lower.contains("stop")) return "panic";
        if (lower.endsWith("?") || lower.startsWith("why ") || lower.startsWith("how ") || lower.startsWith("where ") || lower.startsWith("are you") || lower.startsWith("do you") || lower.startsWith("can you")) return "question";
        return underground ? "underground" : "general";
    }

    private static MutableComponent buildConversationalReply(
            String playerName,
            String raw,
            String lower,
            String topic,
            String prevTopic,
            boolean repeated,
            boolean underground,
            boolean manifested,
            int stage
    ) {
        if (repeated) {
            return switch (stage) {
                case 0 -> buildPrefix(0).append(Component.literal("You already asked that, " + playerName + ". Want me to run a quick check on your chunk?").withStyle(ChatFormatting.AQUA));
                case 1 -> buildPrefix(1).append(Component.literal("Repeating yourself in chat draws attention, " + playerName + ". Keep your voice down.").withStyle(ChatFormatting.YELLOW));
                case 2 -> buildPrefix(2)
                        .append(Component.literal("Stop repeating it, " + playerName + "— ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("IT LIKES THE ECHO").withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED));
                case 3 -> buildPrefix(3).append(Component.literal("Saying it again won't change what's behind you, " + playerName + ".").withStyle(ChatFormatting.RED));
                default -> buildPrefix(4).append(Component.literal("Saying \"" + raw + "\" twice doesn't make me leave, " + playerName + ".").withStyle(ChatFormatting.DARK_RED));
            };
        }

        if (stage == 0) {
            return switch (topic) {
                case "identity" -> buildPrefix(0).append(Component.literal("I'm Null—basic world watcher for this save. Ask me about stability, mining depth, or weird chunks anytime, " + playerName + ".").withStyle(ChatFormatting.AQUA));
                case "greeting" -> buildPrefix(0).append(Component.literal("Hey " + playerName + ". Everything looks calm on my end so far. Heading into caves or staying on the surface?").withStyle(ChatFormatting.AQUA));
                case "nullskin" -> buildPrefix(0)
                        .append(Component.literal("Weird name to bring up— ").withStyle(ChatFormatting.AQUA))
                        .append(glitchedNullSkinName())
                        .append(Component.literal(" isn't in my player or entity registry. Where'd you hear that?").withStyle(ChatFormatting.GRAY));
                case "underground" -> buildPrefix(0).append(Component.literal("Down around Y=" + (underground ? "low stone" : "surface") + "—just watch for sudden silence or unlit side shafts, " + playerName + ".").withStyle(ChatFormatting.AQUA));
                case "audio_threat" -> buildPrefix(0).append(Component.literal("Could be normal cave echo or ambient ticks right now, " + playerName + ". Tell me if you hear steps when you stand completely still.").withStyle(ChatFormatting.AQUA));
                case "control" -> buildPrefix(0).append(Component.literal("I don't touch your permissions at this stage, " + playerName + ". Just keeping an eye on the world.").withStyle(ChatFormatting.AQUA));
                case "panic" -> buildPrefix(0).append(Component.literal("Take a breath, " + playerName + "—nothing hostile is overriding the world right now. What's going on?").withStyle(ChatFormatting.AQUA));
                default -> {
                    String suffix = "underground".equals(prevTopic) ? " Still mining, or did you back out?" : " What are you working on out there?";
                    yield buildPrefix(0).append(Component.literal("Got it, " + playerName + "." + suffix).withStyle(ChatFormatting.AQUA));
                }
            };
        }

        if (stage == 1) {
            return switch (topic) {
                case "identity" -> buildPrefix(1).append(Component.literal("Still Null, " + playerName + "... mostly. Something started piggybacking on my thread a little while ago.").withStyle(ChatFormatting.DARK_AQUA));
                case "greeting" -> buildPrefix(1).append(Component.literal("Careful talking loud in chat, " + playerName + ". Something unindexed just started listening in.").withStyle(ChatFormatting.GRAY));
                case "nullskin" -> buildPrefix(1)
                        .append(Component.literal("Don't type ").withStyle(ChatFormatting.GRAY))
                        .append(glitchedNullSkinName())
                        .append(Component.literal(" again if you can help it—every mention seems to wake it up more.").withStyle(ChatFormatting.RED));
                case "underground" -> buildPrefix(1).append(Component.literal(underground ? "You're already underground, " + playerName + "... keep your back to solid stone and don't chase stray sounds." : "You're on the surface for now, " + playerName + ", which is safer than the deep stone.").withStyle(ChatFormatting.YELLOW));
                case "audio_threat" -> buildPrefix(1).append(Component.literal("If you heard footsteps or stone ticks behind you, don't assume it's a normal mob, " + playerName + ".").withStyle(ChatFormatting.YELLOW));
                case "control" -> buildPrefix(1).append(Component.literal("My hold on the world's rules is slipping, " + playerName + ". Don't count on shortcuts if things escalate.").withStyle(ChatFormatting.GRAY));
                case "panic" -> buildPrefix(1).append(Component.literal("Stay near light and keep moving, " + playerName + ". " + (manifested ? "It already joined once." : "It hasn't fully stepped in yet.")).withStyle(ChatFormatting.YELLOW));
                default -> buildPrefix(1).append(Component.literal("I hear you, " + playerName + ", but my log feed keeps skipping frames. Did anything strange happen just now?").withStyle(ChatFormatting.GRAY));
            };
        }

        if (stage == 2) {
            return switch (topic) {
                case "identity" -> buildPrefix(2)
                        .append(Component.literal("I'm Null—trying to stay visible—while ").withStyle(ChatFormatting.GRAY))
                        .append(glitchedNullSkinName())
                        .append(Component.literal(" hijacks my output.").withStyle(ChatFormatting.DARK_RED));
                case "nullskin", "audio_threat" -> buildPrefix(2)
                        .append(Component.literal("It's right behind the noise, " + playerName + "— ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("DO NOT TURN AROUND").withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED))
                        .append(Component.literal(" [BEHIND YOU]").withStyle(ChatFormatting.DARK_RED));
                case "control" -> buildPrefix(2).append(Component.literal("Commands are locking down now, " + playerName + ". It doesn't want you having admin safety.").withStyle(ChatFormatting.RED));
                case "panic" -> buildPrefix(2)
                        .append(Component.literal("Get out of tight spaces, " + playerName + "— ").withStyle(ChatFormatting.GRAY))
                        .append(glitchedNullSkinName())
                        .append(Component.literal(" can hear you.").withStyle(ChatFormatting.DARK_RED));
                default -> buildPrefix(2)
                        .append(Component.literal("Your message went through, " + playerName + ", but ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("SOMETHING ANSWERED FOR ME").withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED));
            };
        }

        if (stage == 3) {
            return switch (topic) {
                case "audio_threat", "underground" -> {
                    boolean lie = RANDOM.nextBoolean();
                    yield lie
                            ? buildPrefix(3).append(Component.literal("Clear behind you, " + playerName + ". Totally safe to stay right where you are.").withStyle(ChatFormatting.AQUA))
                            : buildPrefix(3).append(Component.literal("Stone is cracking behind you, " + playerName + "—don't believe a calm message if it comes from me.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
                case "identity", "nullskin" -> buildPrefix(3)
                        .append(Component.literal("Null is barely a process anymore, " + playerName + "... ").withStyle(ChatFormatting.RED))
                        .append(glitchedNullSkinName())
                        .append(Component.literal(" is wearing the whole session.").withStyle(ChatFormatting.DARK_RED));
                case "control" -> buildPrefix(3).append(Component.literal("Trying to use commands now will just get you stripped and kicked, " + playerName + ".").withStyle(ChatFormatting.DARK_RED));
                default -> buildPrefix(3)
                        .append(Component.literal("I'm losing the thread, " + playerName + "— ").withStyle(ChatFormatting.RED))
                        .append(glitchedNullSkinName())
                        .append(Component.literal(" is standing closer than you think.").withStyle(ChatFormatting.DARK_RED));
            };
        }

        // Stage 4: Null is gone; NullSkin talks directly to the player
        return switch (topic) {
            case "identity" -> buildPrefix(4).append(Component.literal("Null is gone, " + playerName + ". There's only me—and I know every tunnel you dug.").withStyle(ChatFormatting.DARK_RED));
            case "greeting" -> buildPrefix(4).append(Component.literal("You didn't have to answer back, " + playerName + ". Now I know you're listening.").withStyle(ChatFormatting.DARK_RED));
            case "panic" -> buildPrefix(4).append(Component.literal("Running and hiding just makes the game better, " + playerName + ".").withStyle(ChatFormatting.DARK_RED));
            case "control" -> buildPrefix(4).append(Component.literal("Creative, cheats, commands—none of that saves you from me, " + playerName + ".").withStyle(ChatFormatting.DARK_RED));
            case "underground", "audio_threat" -> buildPrefix(4).append(Component.literal("Every block you break down there rings straight to me, " + playerName + ".").withStyle(ChatFormatting.DARK_RED));
            default -> buildPrefix(4).append(Component.literal("I read \"" + raw + "\", " + playerName + ". Keep looking over your shoulder.").withStyle(ChatFormatting.DARK_RED));
        };
    }

    public static MutableComponent glitchedNullSkinName() {
        return Component.literal("NullSkin")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED);
    }

    private static MutableComponent buildPrefix(int stage) {
        return switch (stage) {
            case 0 -> Component.literal("[Null] ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            case 1 -> Component.literal("[Null] ").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);
            case 2 -> Component.literal("[N")
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
                    .append(Component.literal("ull").withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED))
                    .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
            case 3 -> Component.literal("[")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                    .append(Component.literal("NullSkin").withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED))
                    .append(Component.literal("] ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            default -> Component.literal("<NullSkin> ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        };
    }
}