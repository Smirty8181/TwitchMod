package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.twitchcraft.network.UpgradePayload;
import net.twitchcraft.stream.ItemUpgrade;

/**
 * Меню апгрейда в стиле upgrader.vip: два предмета, кольцо шанса, кнопка.
 */
public final class UpgradeOverlay {
    private static final int GREEN = 0xFF_2E_CC_71;
    private static final int RED = 0xFF_E7_4C_3C;
    private static final int GOLD = 0xFF_F5_D0_6E;
    private static final int CYAN = 0xFF_5D_AE_FF;

    private static String phase = "";
    private static String viewer = "";
    private static int secondsLeft;
    private static int chance;
    private static String fromId = "";
    private static String fromName = "";
    private static String toId = "";
    private static String toName = "";
    private static int selectedSlot = -1;
    private static boolean canPick;
    private static int landAngle;
    private static boolean won;
    private static long spinStart;
    private static long resultAt;
    private static boolean landedFx;

    private static int goX, goY, goW, goH;
    private static final int[] slotX = new int[36];
    private static final int[] slotY = new int[36];
    private static int slotSize;

    private UpgradeOverlay() {}

    public static boolean isLive() {
        return !"hide".equals(phase) && !phase.isBlank();
    }

    public static boolean canPick() {
        return canPick && "pick".equals(phase);
    }

    public static void apply(UpgradePayload p) {
        if (p == null) return;
        String prev = phase;
        phase = p.phase() == null ? "hide" : p.phase();
        viewer = p.viewer() == null ? "" : p.viewer();
        secondsLeft = p.secondsLeft();
        chance = Mth.clamp(p.chance(), 0, 100);
        fromId = nz(p.fromId());
        fromName = nz(p.fromName());
        toId = nz(p.toId());
        toName = nz(p.toName());
        selectedSlot = p.selectedSlot();
        canPick = p.canPick();
        landAngle = p.landAngle();
        won = p.won();
        if ("spin".equals(phase) && !"spin".equals(prev)) {
            spinStart = System.currentTimeMillis();
            resultAt = spinStart + ItemUpgrade.SPIN_MS;
            landedFx = false;
            HudUiSounds.diceRoll();
        }
        if ("win".equals(phase) && !"win".equals(prev)) {
            HudUiSounds.wheelStop();
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                ConfettiFx.burst(mc.getWindow().getGuiScaledWidth() / 2,
                        mc.getWindow().getGuiScaledHeight() / 2 - 20, 18);
                CommandScreenFx.triggerGood(900);
            }
        }
        if ("lose".equals(phase) && !"lose".equals(prev)) {
            HudUiSounds.denyTick();
            CommandScreenFx.triggerEvil(2, 800);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if ("hide".equals(phase)) {
            if (mc.screen instanceof UpgradeScreen) mc.setScreen(null);
            phase = "";
            return;
        }
        if (canPick && "pick".equals(phase) && !(mc.screen instanceof UpgradeScreen)) {
            mc.setScreen(new UpgradeScreen());
        }
    }

    public static void renderHud(GuiGraphics g, float delta) {
        if (!isLive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.screen instanceof UpgradeScreen) return;
        if (!TwitchUi.shouldDrawHud(mc) && mc.screen != null) return;
        render(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                -1, -1, false);
    }

    public static void render(GuiGraphics g, int sw, int sh, int mouseX, int mouseY, boolean interactive) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        Font font = mc.font;
        float es = 1f;
        int panelW = Math.min(HudLayout.s(340, es), sw - 16);
        int invH = interactive ? HudLayout.s(78, es) : 0;
        int panelH = HudLayout.s(168, es) + invH;
        int px = (sw - panelW) / 2;
        int py = Math.max(8, (sh - panelH) / 2 - (interactive ? 12 : 24));

        g.fill(0, 0, sw, sh, 0xB4_03_04_0A);
        HudCard.drawPremium(g, px, py, panelW, panelH, CYAN, 0.98f, 1f, true, System.currentTimeMillis());

        String head = "§b§lUPGRADER";
        g.drawString(font, head, px + HudLayout.s(12, es), py + HudLayout.s(8, es), GOLD, false);
        String sub = ("pick".equals(phase) ? "§7вложи предмет  §f" + secondsLeft + "с"
                : spinning() ? "§eкрутится…"
                : won ? "§a§lУДАЧА" : "§c§lСГОРЕЛ")
                + (viewer.isBlank() ? "" : "  §8· §d" + viewer);
        g.drawString(font, sub, px + HudLayout.s(12, es), py + HudLayout.s(20, es), TwitchUi.TEXT_DIM, false);

        int card = HudLayout.s(52, es);
        int leftX = px + HudLayout.s(18, es);
        int rightX = px + panelW - HudLayout.s(18, es) - card;
        int cardY = py + HudLayout.s(40, es);
        drawItemCard(g, font, leftX, cardY, card, stackOf(fromId), fromName, false);
        drawItemCard(g, font, rightX, cardY, card, stackOf(toId), toName.isBlank() ? "—" : toName, true);

        int cx = px + panelW / 2;
        int cy = cardY + card / 2;
        int radius = HudLayout.s(36, es);
        float needle = needleAngle();
        drawChanceRing(g, cx, cy, radius, chance, needle);
        String pct = chance + "%";
        int pw = font.width(pct);
        g.drawString(font, "§f§l" + pct, cx - pw / 2, cy - 4, 0xFF_FF_FF_FF, false);

        goW = HudLayout.s(120, es);
        goH = HudLayout.s(16, es);
        goX = px + (panelW - goW) / 2;
        goY = cardY + card + HudLayout.s(14, es);
        boolean goHover = interactive && canPick && over(mouseX, mouseY, goX, goY, goW, goH);
        boolean ready = canPick && !toId.isBlank();
        int btn = !ready ? 0xFF_3A_3A_48 : (goHover ? 0xFF_3D_DC_8A : GREEN);
        HudCard.draw(g, goX, goY, goW, goH, btn, 0.95f, goHover ? 1.06f : 1f);
        String btnText = !ready ? "§8выбери предмет" : (goHover ? "§f§lАПРГЕЙД" : "§a§lАПРГЕЙД");
        int bw = font.width(TwitchUi.stripFormat(btnText));
        g.drawString(font, btnText, goX + (goW - bw) / 2, goY + 4, 0xFF_FF_FF_FF, false);

        if (interactive && mc.player != null) {
            drawInventory(g, font, mc.player.getInventory(), px, goY + goH + HudLayout.s(10, es), panelW, mouseX, mouseY);
        }

        if ("win".equals(phase) || (!spinning() && "spin".equals(phase) && won && System.currentTimeMillis() >= resultAt)) {
            ConfettiFx.render(g, sw, sh);
        }
        fireLandFx();
    }

    public static int hitSlot(double mx, double my) {
        if (!canPick) return -2;
        if (over(mx, my, goX, goY, goW, goH)) return -1;
        for (int i = 0; i < 36; i++) {
            if (slotSize > 0 && over(mx, my, slotX[i], slotY[i], slotSize, slotSize)) return i;
        }
        return -2;
    }

    private static void drawInventory(GuiGraphics g, Font font, Inventory inv, int px, int y, int panelW, int mx, int my) {
        int pad = HudLayout.s(14);
        slotSize = HudLayout.s(16);
        int gap = 2;
        int rowW = 9 * (slotSize + gap) - gap;
        int startX = px + (panelW - rowW) / 2;
        g.drawString(font, "§8хотбар и инвентарь — кликни предмет", startX, y - 10, TwitchUi.TEXT_DIM, false);
        for (int i = 0; i < 36; i++) {
            int row = i < 9 ? 3 : (i - 9) / 9;
            int col = i < 9 ? i : (i - 9) % 9;
            int x = startX + col * (slotSize + gap);
            int sy = y + row * (slotSize + gap);
            slotX[i] = x;
            slotY[i] = sy;
            boolean sel = i == selectedSlot;
            boolean hover = over(mx, my, x, sy, slotSize, slotSize);
            g.fill(x, sy, x + slotSize, sy + slotSize, sel ? 0xAA_2E_CC_71 : (hover ? 0x66_5D_AE_FF : 0x66_10_12_18));
            if (sel) {
                g.fill(x, sy, x + slotSize, sy + 1, GREEN);
                g.fill(x, sy + slotSize - 1, x + slotSize, sy + slotSize, GREEN);
            }
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty()) {
                g.renderItem(st, x, sy);
            }
        }
    }

    private static void drawItemCard(GuiGraphics g, Font font, int x, int y, int size, ItemStack stack, String name, boolean target) {
        int accent = target ? GOLD : CYAN;
        HudCard.draw(g, x, y, size, size, accent, 0.95f, 1f);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + (size - 16) / 2, y + (size - 16) / 2 - 4);
        }
        String label = name == null || name.isBlank() ? "—" : name;
        if (label.length() > 12) label = label.substring(0, 11) + "…";
        int lw = font.width(label);
        g.drawString(font, (target ? "§e" : "§7") + label, x + (size - lw) / 2, y + size - 11, TwitchUi.TEXT, false);
    }

    private static void drawChanceRing(GuiGraphics g, int cx, int cy, int radius, int chancePct, float needleDeg) {
        int ticks = 72;
        float winShare = Mth.clamp(chancePct / 100f, 0f, 1f);
        for (int i = 0; i < ticks; i++) {
            float t = i / (float) ticks;
            double a = Math.toRadians(t * 360.0 - 90);
            int x = cx + (int) Math.round(Math.cos(a) * radius);
            int y = cy + (int) Math.round(Math.sin(a) * radius);
            boolean winZone = t < winShare;
            int c = winZone ? GREEN : 0xFF_8A_2A_2A;
            int s = winZone ? 2 : 1;
            g.fill(x - s, y - s, x + s + 1, y + s + 1, c);
        }
        double na = Math.toRadians(needleDeg - 90);
        int nx = cx + (int) Math.round(Math.cos(na) * (radius - 3));
        int ny = cy + (int) Math.round(Math.sin(na) * (radius - 3));
        for (int i = 0; i < 8; i++) {
            float t = i / 7f;
            int x = cx + Math.round((nx - cx) * t);
            int y = cy + Math.round((ny - cy) * t);
            g.fill(x - 1, y - 1, x + 2, y + 2, 0xFF_FF_F6_D8);
        }
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, GOLD);
    }

    private static float needleAngle() {
        if ("pick".equals(phase)) {
            return (System.currentTimeMillis() / 18f) % 360f;
        }
        long now = System.currentTimeMillis();
        if (spinStart <= 0) return landAngle;
        float t = Mth.clamp((now - spinStart) / (float) ItemUpgrade.SPIN_MS, 0f, 1f);
        float eased = 1f - (float) Math.pow(1f - t, 4);
        float travel = 360f * 5f + landAngle;
        return (travel * eased) % 360f;
    }

    private static boolean spinning() {
        return "spin".equals(phase) && System.currentTimeMillis() < resultAt;
    }

    private static void fireLandFx() {
        if (landedFx || !"spin".equals(phase)) return;
        if (System.currentTimeMillis() < resultAt) return;
        landedFx = true;
        if (won) HudUiSounds.wheelStop();
        else HudUiSounds.denyTick();
    }

    private static ItemStack stackOf(String id) {
        if (id == null || id.isBlank()) return ItemStack.EMPTY;
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(loc);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
