package com.feni.nullskin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = NullSkinMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class NullSkinClientOverlay {

    private static final Random RANDOM = new Random();

    private static int alertTicksRemaining = 0;
    private static String activeAlertTitle = "";
    private static String activeAlertBody = "";
    private static float cameraJitterStrength = 0.0F;

    public static void triggerSystemAlert(String title, String body, int durationTicks, float jitterStrength) {
        activeAlertTitle = title;
        activeAlertBody = body;
        alertTicksRemaining = durationTicks;
        cameraJitterStrength = jitterStrength;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (alertTicksRemaining > 0) {
            alertTicksRemaining--;
            if (alertTicksRemaining == 0) {
                cameraJitterStrength = 0.0F;
            }
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (cameraJitterStrength <= 0.0F || alertTicksRemaining <= 0) {
            return;
        }
        float yawNudge = (RANDOM.nextFloat() - 0.5F) * 2.0F * cameraJitterStrength;
        float pitchNudge = (RANDOM.nextFloat() - 0.5F) * 2.0F * cameraJitterStrength;
        event.setYaw(event.getYaw() + yawNudge);
        event.setPitch(event.getPitch() + pitchNudge);
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiOverlayEvent.Post event) {
        if (alertTicksRemaining <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();

        RenderSystem.enableBlend();

        int barHeight = 28;
        graphics.fill(0, 0, width, barHeight, 0xCC120000);
        graphics.fill(0, barHeight, width, barHeight + 1, 0xFF8B0000);

        String titleLine = ChatFormatting.DARK_RED + "" + ChatFormatting.BOLD + "[SYS_HOOK] "
                + ChatFormatting.RED + ChatFormatting.OBFUSCATED + "NULL "
                + ChatFormatting.RESET + ChatFormatting.DARK_RED + activeAlertTitle;

        graphics.drawCenteredString(mc.font, titleLine, width / 2, 6, 0xFFFFFF);
        graphics.drawCenteredString(mc.font, ChatFormatting.GRAY + activeAlertBody, width / 2, 17, 0xCCCCCC);

        RenderSystem.disableBlend();
    }
}