package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.twitchcraft.TwitchCraftMod;

/**
 * Полёт предметов на хотбаре.
 * mode 0 — shuffle (dest[i] = куда летит слот i).
 * mode 1 — обмен: incoming = хотбар напарника, свои улетают, чужие прилетают.
 */
public record HotbarShufflePayload(int mode, int[] dest, ItemStack[] incoming) implements CustomPacketPayload {
    public static final int MODE_SHUFFLE = 0;
    public static final int MODE_SWAP = 1;
    /** ~1.1 с: дуги успевают приземлиться до применения на сервере. */
    public static final int SHUFFLE_TICKS = 22;
    /** ~1.4 с: всасывание + выброс из портала. */
    public static final int SWAP_TICKS = 28;

    public static int clientDurationMs(int mode) {
        int ticks = mode == MODE_SWAP ? SWAP_TICKS : SHUFFLE_TICKS;
        return ticks * 50 + 120;
    }

    public static final Type<HotbarShufflePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "hotbar_shuffle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HotbarShufflePayload> CODEC =
            StreamCodec.of(HotbarShufflePayload::write, HotbarShufflePayload::read);

    private static void write(RegistryFriendlyByteBuf buf, HotbarShufflePayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.mode());
        int[] d = p.dest() == null ? new int[9] : p.dest();
        ItemStack[] in = p.incoming() == null ? new ItemStack[9] : p.incoming();
        for (int i = 0; i < 9; i++) {
            ByteBufCodecs.VAR_INT.encode(buf, i < d.length ? d[i] : i);
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = i < in.length && in[i] != null ? in[i] : ItemStack.EMPTY;
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static HotbarShufflePayload read(RegistryFriendlyByteBuf buf) {
        int mode = ByteBufCodecs.VAR_INT.decode(buf);
        int[] d = new int[9];
        for (int i = 0; i < 9; i++) {
            d[i] = ByteBufCodecs.VAR_INT.decode(buf);
        }
        ItemStack[] in = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            in[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            if (in[i] == null) in[i] = ItemStack.EMPTY;
        }
        return new HotbarShufflePayload(mode, d, in);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendShuffle(ServerPlayer player, int[] dest) {
        if (player == null || dest == null || dest.length < 9) return;
        int[] copy = new int[9];
        System.arraycopy(dest, 0, copy, 0, 9);
        ItemStack[] empty = new ItemStack[9];
        java.util.Arrays.fill(empty, ItemStack.EMPTY);
        ServerPlayNetworking.send(player, new HotbarShufflePayload(MODE_SHUFFLE, copy, empty));
    }

    public static void sendSwap(ServerPlayer player, ItemStack[] partnerHotbar) {
        if (player == null) return;
        ItemStack[] in = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            in[i] = partnerHotbar != null && i < partnerHotbar.length && partnerHotbar[i] != null
                    ? partnerHotbar[i].copy() : ItemStack.EMPTY;
        }
        int[] dest = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
        ServerPlayNetworking.send(player, new HotbarShufflePayload(MODE_SWAP, dest, in));
    }
}
