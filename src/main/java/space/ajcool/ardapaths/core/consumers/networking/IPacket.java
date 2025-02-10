package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.network.PacketByteBuf;

public interface IPacket {
    /**
     * Build the packet.
     */
    PacketByteBuf build();

    /**
     * Convert the packet to an instance of the object.
     *
     * @param buf The packet byte buffer to read
     */
    static <T> T read(PacketByteBuf buf) {
        return null;
    }
}
