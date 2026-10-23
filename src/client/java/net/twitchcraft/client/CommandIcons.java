package net.twitchcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;

/** Картинка команды для ленты / отказа / рулетки. */
public final class CommandIcons {
    private CommandIcons() {}

    public static String spriteId(String commandId) {
        String raw = commandId == null ? "" : commandId;
        String id = ChatGameCommand.fromId(raw)
                .map(ChatGameCommand::getId)
                .orElseGet(() -> WheelEffectIcons.normalize(raw));
        id = WheelEffectIcons.normalize(id);
        return switch (id) {
            case "creeper" -> "creeper";
            case "zombie", "skeleton", "spider", "slime", "witch", "ghast",
                 "wolf", "buddy", "irongolem", "tck", "goose", "amongus", "oia" -> "zombie";
            case "enderman" -> "phantom";
            case "sheep" -> "sheep";
            case "chicken", "chaoschickens", "goldenhen", "egg", "vieweregg",
                 "piggyback", "carryon" -> "chicken";
            case "heal", "regen", "save", "help", "antidote", "feed", "respect" -> "heal";
            case "follow" -> "follow";
            case "sub" -> "sub";
            case "firstmsg" -> "firstmsg";
            case "diamond", "gapple" -> "diamond";
            case "water", "rain" -> "water";
            case "fire", "fireres" -> "fire";
            case "coin", "bet" -> "coin";
            case "day", "noon", "clearweather" -> "hype";
            case "night" -> "blind";
            case "thunder" -> "storm";
            case "hurt", "poison", "hunger", "weakness", "drunk", "potion" -> "curse";
            case "speed" -> "launch";
            case "slow", "gravity", "invert" -> "spin";
            case "tp" -> "tp";
            case "swap", "up", "down", "knockback", "backward",
                 "novalny", "fpv", "parkour" -> "launch";
            case "roulette", "luckyroulette", "lucky", "unlucky", "verylucky" -> "wheel";
            case "firework", "xp", "hype", "prime", "primerave" -> "hype";
            case "dance" -> "disco";
            case "bomb", "defuse" -> "tnt";
            case "hunt" -> "raid";
            case "morph" -> "morph";
            case "tether" -> "tether";
            case "totem", "endereye", "portalkit" -> "phantom";
            case "shuffle", "swapinv" -> "shuffle";
            case "junk", "steal", "heist", "bow", "elytra", "ragdoll" -> "drop";
            case "bread", "planks", "torches", "upgrade" -> "treasure";
            case "orevein" -> "pit";
            case "giant" -> "titan";
            case "flash" -> "lightning";
            case "viewerarmy" -> "army";
            case "shield", "deny_shield" -> "deny_shield";
            case "hold", "deny_hold" -> "deny_hold";
            case "cooldown", "spam", "rate", "evil_pace", "deny_cd" -> "deny_cd";
            case "evil_block", "deny_evil" -> "deny_evil";
            case "vote", "king", "event", "random", "sign", "voice",
                 "jumpscare", "clown", "slap", "fish", "kva", "vine",
                 "troll", "brainrot", "sixseven", "miss" -> "event";
            default -> id;
        };
    }

    public static void draw(GuiGraphics g, String commandId, int x, int y, int size, int accent, float alpha) {
        draw(g, commandId, x, y, size, accent, alpha, size >= 18);
    }

    public static void draw(GuiGraphics g, String commandId, int x, int y, int size, int accent, float alpha,
                            boolean well) {
        int a = (int) (alpha * 255);
        if (a < 8 || size < 4) return;
        if (well) {
            HudCard.drawIconWell(g, x, y, size, accent, alpha);
        }
        int pad = well ? Math.max(2, size / 9) : 0;
        int inner = Math.max(4, size - pad * 2);
        String sprite = spriteId(commandId);
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(
                TwitchCraftMod.MOD_ID, "textures/gui/wheel/" + sprite);
        int ix = x + (size - inner) / 2;
        int iy = y + (size - inner) / 2;
        if (!TwitchTextures.blit(g, tex, ix, iy, inner, inner, WheelEffectIcons.TEX_SIZE, WheelEffectIcons.TEX_SIZE)) {
            WheelIconArt.draw(g, sprite, x + size / 2, y + size / 2, inner, false, 0L, false);
        }
    }
}
