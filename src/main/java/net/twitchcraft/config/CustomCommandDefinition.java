package net.twitchcraft.config;

/** Пользовательская команда чата, создаётся в игре (клавиша U). */
public class CustomCommandDefinition {
    public String id = "";
    /** Триггер без «!», напр. «тотем2». */
    public String trigger = "";
    /** effect | item | mob */
    public String actionType = "effect";
    public boolean enabled = true;
    public boolean positive = false;
    public int cooldownSeconds = 30;
    public int maxUsesPerMinute = 0;

    public String effectId = "minecraft:speed";
    public int durationSeconds = 30;
    public int amplifier = 0;

    public String itemId = "minecraft:diamond";
    public int itemCount = 1;

    public String entityId = "minecraft:zombie";
    public int mobCount = 1;

    public String displayName() {
        return trigger == null || trigger.isBlank() ? "?" : trigger;
    }

    public String cooldownKey() {
        return "custom:" + (id == null || id.isBlank() ? trigger : id);
    }
}
