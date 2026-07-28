package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from server to client containing the complete path data as JSON.
 * Response to a path data request, transmitted to update or initialize the client's path configuration.
 * @param json the complete path configuration serialized as JSON
 */
public record PathDataResponsePacket(String json) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(json);
        return buf;
    }

    public static PathDataResponsePacket read(PacketByteBuf buf)
    {
        final String json = buf.readString();
        return new PathDataResponsePacket(json);
    }
}
