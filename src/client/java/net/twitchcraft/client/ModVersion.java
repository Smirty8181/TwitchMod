package net.twitchcraft.client;

import net.fabricmc.loader.api.FabricLoader;
import net.twitchcraft.TwitchCraftMod;

public final class ModVersion {
    private static String cached;

    private ModVersion() {}

    public static String friendly() {
        if (cached == null) {
            cached = FabricLoader.getInstance().getModContainer(TwitchCraftMod.MOD_ID)
                    .map(m -> m.getMetadata().getVersion().getFriendlyString())
                    .orElse("dev");
        }
        return cached;
    }
}
