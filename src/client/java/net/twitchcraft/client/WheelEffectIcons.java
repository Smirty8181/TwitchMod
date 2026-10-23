package net.twitchcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Иконки секторов рулетки — PNG 48x48, целочисленный скейл без размытия. */
public final class WheelEffectIcons {
    public static final int TEX_SIZE = 48;

    private static final List<String> IDS = List.of(
            "meteor", "wither", "army", "tntrain", "boss", "dragon", "prison", "creeperstorm",
            "event", "spin", "curse", "clear", "launch", "trap", "chicken", "anvil",
            "storm", "hype", "dome", "lightning", "phantom", "wheel",
            "lava", "pit", "drop", "titan", "ant", "shuffle", "tnt", "blind",
            "goldrush", "disco", "treasure", "raid",
            "heal", "diamond", "water", "fire", "zombie", "sheep", "coin",
            "creeper", "tp", "morph", "tether", "sub", "follow", "firstmsg",
            "deny_shield", "deny_cd", "deny_hold", "deny_evil"
    );

    private WheelEffectIcons() {}

    public static Set<String> allIds() {
        return Set.copyOf(IDS);
    }

    public static ResourceLocation texture(String commandId) {
        String id = CommandIcons.spriteId(commandId);
        if (!IDS.contains(id)) id = "event";
        return ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "textures/gui/wheel/" + id);
    }

    public static void draw(GuiGraphics g, String commandId, int cx, int cy, int size,
                            boolean highlight, long elapsed) {
        draw(g, commandId, cx, cy, size, 0, highlight, elapsed);
    }

    public static void draw(GuiGraphics g, String commandId, int cx, int cy, int size, int accent,
                            boolean highlight, long elapsed) {
        int drawSize = snapSize(size);
        if (accent != 0) {
            drawSlot(g, cx, cy, drawSize, accent);
        }
        int x = cx - drawSize / 2;
        int y = cy - drawSize / 2;
        if (TwitchTextures.blit(g, texture(commandId), x, y, drawSize, drawSize, TEX_SIZE, TEX_SIZE)) {
            return;
        }
        WheelIconArt.draw(g, commandId, cx, cy, drawSize, highlight, elapsed, false);
    }

    /** Ближайший целый масштаб 32/48/64 — пиксель-арт остаётся чётким. */
    static int snapSize(int size) {
        int d32 = Math.abs(size - 32);
        int d48 = Math.abs(size - 48);
        int d64 = Math.abs(size - 64);
        if (d48 <= d32 && d48 <= d64) return 48;
        if (d64 <= d32) return 64;
        return 32;
    }

    private static void drawSlot(GuiGraphics g, int cx, int cy, int iconSize, int accent) {
        int pad = 5;
        int box = iconSize + pad * 2;
        int x = cx - box / 2;
        int y = cy - box / 2;
        g.fill(x + 2, y + 2, x + box + 2, y + box + 2, 0x66000000);
        TwitchUi.roundedPanel(g, x, y, x + box, y + box, 0xF0_14_12_20);
        g.fill(x + 1, y + 1, x + box - 1, y + 4, accent);
        g.fill(x, y, x + box, y + 1, 0xFF_D4_AF_37);
        g.fill(x, y + box - 1, x + box, y + box, 0xFF_8B_69_14);
    }

    public static String normalize(String id) {
        if (id == null || id.isBlank()) return "event";
        return id.toLowerCase(Locale.ROOT).trim();
    }
}
