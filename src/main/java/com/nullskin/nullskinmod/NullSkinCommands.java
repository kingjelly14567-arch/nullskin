package com.feni.nullskin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NullSkinMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NullSkinCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("nullskin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("stage")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 4))
                                .executes(ctx -> {
                                    int stage = IntegerArgumentType.getInteger(ctx, "value");
                                    ServerLevel level = ctx.getSource().getLevel();
                                    NullSkinEscalationData data = NullSkinEscalationData.get(level);
                                    data.setStage(stage);
                                    if (stage >= 1) {
                                        NullSystemMessages.broadcastFirstManifestation(level);
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("NullSkin stage set to " + stage)
                                            .withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })))
                .then(Commands.literal("points")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    int pts = IntegerArgumentType.getInteger(ctx, "value");
                                    ServerLevel level = ctx.getSource().getLevel();
                                    NullSkinEscalationData data = NullSkinEscalationData.get(level);
                                    int current = data.getEscalationPoints();
                                    data.addEscalationPoints(pts - current);
                                    ctx.getSource().sendSuccess(() -> Component.literal("NullSkin escalation points set to " + data.getEscalationPoints() + " (Stage " + data.getStage() + ")")
                                            .withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })))
                .then(Commands.literal("pulse")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            NullSystemMessages.sendAmbientSystemPulse(level);
                            return 1;
                        }))
                .then(Commands.literal("cavescare")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                ServerLevel level = player.serverLevel();
                                NullSkinEscalationData data = NullSkinEscalationData.get(level);
                                NullSkinDirector.triggerCaveAudioOrBreach(level, player, data);
                            }
                            return 1;
                        })
                        .then(Commands.argument("realBreach", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        boolean real = BoolArgumentType.getBool(ctx, "realBreach");
                                        ServerLevel level = player.serverLevel();
                                        NullSkinEscalationData data = NullSkinEscalationData.get(level);
                                        NullSkinDirector.forceCaveScare(level, player, data, real);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            NullSkinEscalationData data = NullSkinEscalationData.get(level);
                            data.setStage(0);
                            data.addEscalationPoints(-data.getEscalationPoints());
                            data.setHasManifestedOnce(false);
                            ctx.getSource().sendSuccess(() -> Component.literal("NullSkin state reset to Stage 0.")
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        })));
    }
}