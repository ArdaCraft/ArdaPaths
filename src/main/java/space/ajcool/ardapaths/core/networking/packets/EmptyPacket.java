package space.ajcool.ardapaths.core.networking.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * A packet with no data payload, used for simple request/response patterns.
 * Useful for notifications that don't require any additional information.
 */
public record EmptyPacket() implements IPacket {
    /**
     * Shared empty packet buffer instance for all EmptyPacket instances.
     */
    private static final PacketByteBuf EMPTY = PacketByteBufs.create();

    /**
     * Deserializes an EmptyPacket from a PacketByteBuf.
     *
     * @param ignoredBuf the buffer to read from (contents ignored)
     * @return a new EmptyPacket instance
     */
    public static EmptyPacket read(PacketByteBuf ignoredBuf) {
        return new EmptyPacket();
    }

    /**
     * Builds the packet into a PacketByteBuf (returns the empty buffer).
     *
     * @return the empty packet buffer
     */
    @Override
    public PacketByteBuf build() {
        return EMPTY;
    }
}
