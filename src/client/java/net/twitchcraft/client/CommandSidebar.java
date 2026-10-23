package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.hud.SidebarPages;

import java.util.ArrayList;
import java.util.List;

/** Простой sidebar справа — список !команд, страницы листаются сами. */
public final class CommandSidebar {
    private static final String TITLE = "§d§lTwitchCraft";
    private static int ticks;

    private CommandSidebar() {
    }

    public static void tick() {
        ticks++;
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) return;
        if (mc.options.hideGui) return;
        if (mc.screen != null) return;

        TwitchCraftConfig cfg = ConfigManager.get();
        if (!cfg.scoreboard.enabled || !cfg.scoreboard.showCommands) return;

        List<String> lines = buildLines(cfg);
        if (lines.isEmpty()) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int maxW = mc.font.width(TITLE);
        for (String line : lines) {
            maxW = Math.max(maxW, mc.font.width(line));
        }

        int pad = 5;
        int lineH = 9;
        int boxW = maxW + pad * 2;
        int boxH = pad * 2 + lineH * (lines.size() + 1);
        int x = screenW - boxW - 2;
        int y = 6;

        g.fill(x - 1, y - 1, screenW, y + boxH + 1, 0x90000000);
        g.fill(x, y, screenW - 1, y + boxH, 0xC0000000);
        g.fill(x, y, screenW - 1, y + 2, 0xFF9146FF);

        int ty = y + pad;
        int tw = mc.font.width(TITLE);
        g.drawString(mc.font, TITLE, x + boxW - tw - pad, ty, 0xFFFFFF, false);
        ty += lineH + 1;

        for (String line : lines) {
            int lw = mc.font.width(line);
            g.drawString(mc.font, line, x + boxW - lw - pad, ty, 0xFFFFFF, false);
            ty += lineH;
        }
    }

    private static List<String> buildLines(TwitchCraftConfig cfg) {
        List<String> lines = new ArrayList<>(SidebarPages.featuredLine(cfg));
        lines.addAll(SidebarPages.build(cfg, ticks / 20));
        return lines;
    }
}
