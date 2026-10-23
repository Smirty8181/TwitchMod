package net.twitchcraft.config;

/** Профиль стримера: Minecraft-ник + Twitch + DonateX. */
public class StreamerProfile {
    public String minecraftName = "";
    public String twitchChannel = "";
    public String oauthToken = "";
    public String donationToken = "";
    /** Пусто = общие tiers из config.donations.tiers */
    public String donationLabel = "";
    /** Локально: мастер первой настройки пройден (config/twitchcraft-local.json). */
    public boolean setupWizardCompleted = false;

    public StreamerProfile() {}

    public StreamerProfile(String minecraftName, String twitchChannel) {
        this.minecraftName = minecraftName;
        this.twitchChannel = twitchChannel;
    }

    public boolean hasTwitch() {
        return twitchChannel != null && !twitchChannel.isBlank()
                && oauthToken != null && !oauthToken.isBlank();
    }

    public boolean hasDonations() {
        return donationToken != null && !donationToken.isBlank();
    }
}
