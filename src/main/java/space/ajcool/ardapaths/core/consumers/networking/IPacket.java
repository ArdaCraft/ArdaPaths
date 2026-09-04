package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Function;

/**
 * Interface for network packets that can be built into a PacketByteBuf.
 * Implementations must expose a static {@code read(FriendlyByteBuf)} factory.
 */
public interface IPacket extends CustomPacketPayload {

    /**
     * Creates a stream codec around this mod's existing buffer serialization convention.
     *
     * @param <T>    packet payload type
     * @param reader function that reads a packet from a friendly byte buffer
     * @return stream codec for Fabric payload registration
     */
    static <T extends IPacket> StreamCodec<RegistryFriendlyByteBuf, T> codec(Function<FriendlyByteBuf, T> reader) {
        return StreamCodec.of((buf, packet) -> buf.writeBytes(packet.build()), reader::apply);
    }

    /**
     * Build the packet.
     *
     * @return the packet as a PacketByteBuf
     */
    FriendlyByteBuf build();
}
