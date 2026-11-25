package com.feni.nullskin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NullSkinConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 216;

    private final Screen lastScreen;
    private boolean antiCheatEnabled;
    private boolean deleteSpWorld;
    private boolean closeOnFirstManifest;

    private Button antiCheatBtn;
    private Button wipeWorldBtn;
    private Button closeClientBtn;

    public NullSkinConfigScreen(Screen lastScreen) {
        super(Component.literal("NULLSKIN // SYSTEM CONFIGURATION"));
        this.lastScreen = lastScreen;
        this.antiCheatEnabled = Config.ANTI_CHEAT_ENABLED.get();
        this.deleteSpWorld = Config.DELETE_SINGLEPLAYER_WORLD_ON_CHEAT.get();
        this.closeOnFirstManifest = Config.CLOSE_CLIENT_ON_FIRST_MANIFEST.get();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelTop = Math.max(24, (this.height - PANEL_HEIGHT) / 2);
        int rowWidth = PANEL_WIDTH - 36;
        int rowX = centerX - (rowWidth / 2);
        int startY = panelTop + 46;

        this.antiCheatBtn = this.addRenderableWidget(Button.builder(
                formatToggleLabel("Anti-Cheat Enforcement", this.antiCheatEnabled),
                btn -> {
                    this.antiCheatEnabled = !this.antiCheatEnabled;
                    Config.ANTI_CHEAT_ENABLED.set(this.antiCheatEnabled);
                    Config.syncAndSave();
                    btn.setMessage(formatToggleLabel("Anti-Cheat Enforcement", this.antiCheatEnabled));
                }
        ).bounds(rowX, startY, rowWidth, 22).build());

        this.wipeWorldBtn = this.addRenderableWidget(Button.builder(
                formatToggleLabel("SP World Wipe on Cheat", this.deleteSpWorld),
                btn -> {
                    this.deleteSpWorld = !this.deleteSpWorld;
                    Config.DELETE_SINGLEPLAYER_WORLD_ON_CHEAT.set(this.deleteSpWorld);
                    Config.syncAndSave();
                    btn.setMessage(formatToggleLabel("SP World Wipe on Cheat", this.deleteSpWorld));
                }
        ).bounds(rowX, startY + 28, rowWidth, 22).build());

        this.closeClientBtn = this.addRenderableWidget(Button.builder(
                formatToggleLabel("Close Client on 1st Manifest", this.closeOnFirstManifest),
                btn -> {
                    this.closeOnFirstManifest = !this.closeOnFirstManifest;
                    Config.CLOSE_CLIENT_ON_FIRST_MANIFEST.set(this.closeOnFirstManifest);
                    Config.syncAndSave();
                    btn.setMessage(formatToggleLabel("Close Client on 1st Manifest", this.closeOnFirstManifest));
                }
        ).bounds(rowX, startY + 56, rowWidth, 22).build());

        int bottomY = panelTop + PANEL_HEIGHT - 30;
        int halfWidth = (rowWidth - 8) / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("Reset Defaults").withStyle(ChatFormatting.GRAY),
                btn -> resetToDefaults()
        ).bounds(rowX, bottomY, halfWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Save & Exit").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                btn -> onClose()
        ).bounds(rowX + halfWidth + 8, bottomY, halfWidth, 20).build());
    }

    private void resetToDefaults() {
        this.antiCheatEnabled = true;
        this.deleteSpWorld = false;
        this.closeOnFirstManifest = true;

        Config.ANTI_CHEAT_ENABLED.set(true);
        Config.DELETE_SINGLEPLAYER_WORLD_ON_CHEAT.set(false);
        Config.CLOSE_CLIENT_ON_FIRST_MANIFEST.set(true);
        Config.syncAndSave();

        if (this.antiCheatBtn != null) {
            this.antiCheatBtn.setMessage(formatToggleLabel("Anti-Cheat Enforcement", true));
        }
        if (this.wipeWorldBtn != null) {
            this.wipeWorldBtn.setMessage(formatToggleLabel("SP World Wipe on Cheat", false));
        }
        if (this.closeClientBtn != null) {
            this.closeClientBtn.setMessage(formatToggleLabel("Close Client on 1st Manifest", true));
        }
    }

    private static Component formatToggleLabel(String label, boolean enabled) {
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(enabled ? "[ACTIVE]" : "[DISABLED]")
                        .withStyle(enabled ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int panelLeft = centerX - (PANEL_WIDTH / 2);
        int panelTop = Math.max(24, (this.height - PANEL_HEIGHT) / 2);
        int panelRight = panelLeft + PANEL_WIDTH;
        int panelBottom = panelTop + PANEL_HEIGHT;

        // Outer crimson frame and dark terminal backdrop
        guiGraphics.fill(panelLeft - 2, panelTop - 2, panelRight + 2, panelBottom + 2, 0xFF5A0A0A);
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xEE0B0B0E);

        // Subtle horizontal CRT scanlines inside the panel
        for (int y = panelTop + 2; y < panelBottom - 2; y += 3) {
            guiGraphics.fill(panelLeft + 2, y, panelRight - 2, y + 1, 0x18FFFFFF);
        }

        // Corner accent brackets
        int cornerColor = 0xFFD92626;
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 14, panelTop + 2, cornerColor);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 2, panelTop + 14, cornerColor);
        guiGraphics.fill(panelRight - 14, panelTop, panelRight, panelTop + 2, cornerColor);
        guiGraphics.fill(panelRight - 2, panelTop, panelRight, panelTop + 14, cornerColor);
        guiGraphics.fill(panelLeft, panelBottom - 2, panelLeft + 14, panelBottom, cornerColor);
        guiGraphics.fill(panelLeft, panelBottom - 14, panelLeft + 2, panelBottom, cornerColor);
        guiGraphics.fill(panelRight - 14, panelBottom - 2, panelRight, panelBottom, cornerColor);
        guiGraphics.fill(panelRight - 2, panelBottom - 14, panelRight, panelBottom, cornerColor);

        // Header bar & title
        guiGraphics.fill(panelLeft + 12, panelTop + 34, panelRight - 12, panelTop + 35, 0xFF4A1010);
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("NULLSKIN // SYSTEM CONFIGURATION").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                centerX,
                panelTop + 11,
                0xFFFFFF
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Behavioral Overrides & Escalation Protocols").withStyle(ChatFormatting.DARK_GRAY),
                centerX,
                panelTop + 22,
                0xAAAAAA
        );

        // Description box above bottom buttons
        int infoTop = panelTop + 136;
        int infoBottom = panelBottom - 38;
        guiGraphics.fill(panelLeft + 18, infoTop, panelRight - 18, infoBottom, 0xAA141419);
        guiGraphics.renderOutline(panelLeft + 18, infoTop, PANEL_WIDTH - 36, infoBottom - infoTop, 0xFF331111);

        Component infoLine1;
        Component infoLine2;

        if (this.antiCheatBtn != null && this.antiCheatBtn.isHoveredOrFocused()) {
            infoLine1 = Component.literal("Blocks unauthorized Creative/Spectator & cheat commands.").withStyle(ChatFormatting.GRAY);
            infoLine2 = Component.literal("Safe commands (/nullskin, /help, /msg, etc.) stay allowed.").withStyle(ChatFormatting.DARK_GRAY);
        } else if (this.wipeWorldBtn != null && this.wipeWorldBtn.isHoveredOrFocused()) {
            infoLine1 = Component.literal("WARNING: Permanently deletes your SP world on exit if you cheat!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            infoLine2 = Component.literal("Keep DISABLED while testing builds or commands.").withStyle(ChatFormatting.YELLOW);
        } else if (this.closeClientBtn != null && this.closeClientBtn.isHoveredOrFocused()) {
            infoLine1 = Component.literal("Forces Minecraft to close once when NullSkin first manifests.").withStyle(ChatFormatting.GRAY);
            infoLine2 = Component.literal("Triggers once per session for maximum horror impact.").withStyle(ChatFormatting.DARK_GRAY);
        } else {
            infoLine1 = Component.literal("Hover an option above to inspect its behavior.").withStyle(ChatFormatting.GRAY);
            infoLine2 = Component.literal("Changes save automatically to config/nullskin-common.toml").withStyle(ChatFormatting.DARK_GRAY);
        }

        guiGraphics.drawCenteredString(this.font, infoLine1, centerX, infoTop + 9, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, infoLine2, centerX, infoTop + 22, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}