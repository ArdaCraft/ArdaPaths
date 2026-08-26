package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.data.ChapterMarkerEntry;
import space.ajcool.ardapaths.core.data.ChapterMarkersStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from server to client with the resolved chapter marker chain.
 *
 * @param status  status code describing the result
 * @param markers ordered marker rows and optional chain-break separator
 */
public record ChapterPathMarkersResponsePacket(ChapterMarkersStatus status, List<ChapterMarkerEntry> markers) implements IPacket {
    /**
     * Serializes this response for server-to-client transmission.
     *
     * @return packet data buffer
     */
    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(status);
        buf.writeInt(markers.size());
        for (ChapterMarkerEntry marker : markers) {
            writeMarker(buf, marker);
        }
        return buf;
    }

    /**
     * Reads a chapter marker list response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static ChapterPathMarkersResponsePacket read(PacketByteBuf buf) {
        ChapterMarkersStatus status = buf.readEnumConstant(ChapterMarkersStatus.class);
        int markerCount = buf.readInt();
        List<ChapterMarkerEntry> markers = new ArrayList<>(markerCount);
        for (int index = 0; index < markerCount; index++) {
            markers.add(readMarker(buf));
        }
        return new ChapterPathMarkersResponsePacket(status, markers);
    }

    /**
     * Writes one chapter marker row to a network buffer.
     *
     * @param buf    destination packet buffer
     * @param marker marker row to write
     */
    private static void writeMarker(PacketByteBuf buf, ChapterMarkerEntry marker) {
        buf.writeLong(marker.packedPos());
        buf.writeInt(marker.timeOfDay());
        buf.writeInt(marker.weather());
        buf.writeString(marker.proximityMessage());
        buf.writeBoolean(marker.hasMiscData());
        buf.writeBoolean(marker.chapterStart());
        buf.writeBoolean(marker.chainBreak());
    }

    /**
     * Reads one chapter marker row from a network buffer.
     *
     * @param buf source packet buffer
     * @return decoded marker row
     */
    private static ChapterMarkerEntry readMarker(PacketByteBuf buf) {
        return new ChapterMarkerEntry(
                buf.readLong(),
                buf.readInt(),
                buf.readInt(),
                buf.readString(ChapterMarkerEntry.MAX_PROXIMITY_MESSAGE_LENGTH),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
