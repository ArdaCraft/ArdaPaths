package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.network.PacketByteBuf;

/**
 * Interface for network packets that can be built into and read from PacketByteBuf.
 */
public interface IPacket {
    /**
     * Convert the packet to an instance of the object.
     *
     * @param <T> the type of object to read
     * @param ignoredBuf the packet byte buffer to read
     * @return the object instance
     */
    static <T> T read(PacketByteBuf ignoredBuf) {
        return null;
    }

    /**
     * Build the packet.
     *
     * @return the packet as a PacketByteBuf
     */
    PacketByteBuf build();
}
