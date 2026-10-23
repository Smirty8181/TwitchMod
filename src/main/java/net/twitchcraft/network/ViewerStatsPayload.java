package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.stream.StreamStats;

import java.util.ArrayList;
import java.util.List;

/** S2C: снимок статистики команд по зрителям. */
public record ViewerStatsPayload(List<ViewerRow> viewers) implements CustomPacketPayload {
    public static final Type<ViewerStatsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "viewer_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ViewerStatsPayload> CODEC =
            StreamCodec.of(ViewerStatsPayload::write, ViewerStatsPayload::read);

    public record CmdUse(String id, String ru, int count) {}

    public record ViewerRow(
            String name,
            int total,
            String favoriteId,
            String favoriteRu,
            int favoriteCount,
            List<CmdUse> top
    ) {}

    private static void write(RegistryFriendlyByteBuf buf, ViewerStatsPayload p) {
        buf.writeVarInt(p.viewers.size());
        for (ViewerRow v : p.viewers) {
            ByteBufCodecs.STRING_UTF8.encode(buf, v.name());
            buf.writeVarInt(v.total());
            ByteBufCodecs.STRING_UTF8.encode(buf, v.favoriteId() != null ? v.favoriteId() : "");
            ByteBufCodecs.STRING_UTF8.encode(buf, v.favoriteRu() != null ? v.favoriteRu() : "-");
            buf.writeVarInt(v.favoriteCount());
            List<CmdUse> top = v.top() != null ? v.top() : List.of();
            buf.writeVarInt(top.size());
            for (CmdUse c : top) {
                ByteBufCodecs.STRING_UTF8.encode(buf, c.id() != null ? c.id() : "");
                ByteBufCodecs.STRING_UTF8.encode(buf, c.ru() != null ? c.ru() : "");
                buf.writeVarInt(c.count());
            }
        }
    }

    private static ViewerStatsPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ViewerRow> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            int total = buf.readVarInt();
            String favId = ByteBufCodecs.STRING_UTF8.decode(buf);
            String favRu = ByteBufCodecs.STRING_UTF8.decode(buf);
            int favCount = buf.readVarInt();
            int tc = buf.readVarInt();
            List<CmdUse> top = new ArrayList<>(tc);
            for (int j = 0; j < tc; j++) {
                top.add(new CmdUse(
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        buf.readVarInt()));
            }
            list.add(new ViewerRow(name, total, favId, favRu, favCount, top));
        }
        return new ViewerStatsPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, StreamStats stats) {
        List<StreamStats.ViewerCommandStat> snap = stats.snapshotViewerStats(80);
        List<ViewerRow> rows = new ArrayList<>(snap.size());
        for (StreamStats.ViewerCommandStat s : snap) {
            List<CmdUse> top = new ArrayList<>();
            for (StreamStats.CommandCount c : s.topCommands()) {
                top.add(new CmdUse(c.id(), c.ru(), c.count()));
            }
            rows.add(new ViewerRow(s.name(), s.total(), s.favoriteId(), s.favoriteRu(),
                    s.favoriteCount(), top));
        }
        ServerPlayNetworking.send(player, new ViewerStatsPayload(rows));
    }
}
