package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.network.PacketByteBuf;

/**
 * Interface for network packets that can be built into and read from PacketByteBuf.
 */
public interface IPacket {
    /**
     * Convert the packet to an instance of the object.
     *
     * @param ignoredBuf The packet byte buffer to read
     */
    static <T> T read(PacketByteBuf ignoredBuf) {
        return null;
    }

    /**
     * Build the packet.
     */
    PacketByteBuf build();
}
