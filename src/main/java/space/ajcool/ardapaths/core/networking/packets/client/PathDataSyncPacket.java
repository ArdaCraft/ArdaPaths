package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from server to client when authoritative path data changes outside a request.
 *
 * @param json the complete path configuration serialized as JSON
 */
@SuppressWarnings("unused")
public record PathDataSyncPacket(String json) implements IPacket {

    /**
     * Network channel used for server-pushed path data synchronization.
     */
    public static final Identifier CHANNEL = ModConstants.modId("path_data_sync");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<PathDataSyncPacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Deserializes path data sync from a packet byte buffer.
     *
     * @param buf buffer containing serialized path data
     * @return decoded path data sync packet
     */
    public static PathDataSyncPacket read(FriendlyByteBuf buf) {
        return new PathDataSyncPacket(buf.readUtf());
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<PathDataSyncPacket> type() {
        return TYPE;
    }

    /**
     * Builds the packet into a fresh friendly byte buffer.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUtf(json);
        return buf;
    }
}
