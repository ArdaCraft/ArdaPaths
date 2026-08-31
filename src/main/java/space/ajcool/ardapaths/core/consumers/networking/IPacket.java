package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Interface for network packets that can be built into a PacketByteBuf.
 * Implementations must expose a static {@code read(FriendlyByteBuf)} factory.
 */
public interface IPacket {
    /**
     * Build the packet.
     *
     * @return the packet as a PacketByteBuf
     */
    FriendlyByteBuf build();
}
