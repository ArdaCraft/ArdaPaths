package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;
import space.ajcool.ardapaths.core.data.ChapterMarkerEntry;
import space.ajcool.ardapaths.core.data.ChapterMarkersStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet sent from server to client with the resolved chapter marker chain.
 *
 * @param requestId request correlation id
 * @param status    status code describing the result
 * @param markers   ordered marker rows and optional chain-break separator
 */
public record ChapterPathMarkersResponsePacket(UUID requestId, ChapterMarkersStatus status, List<ChapterMarkerEntry> markers) implements IRespondablePacket<ChapterPathMarkersResponsePacket> {
    /**
     * Network channel used for chapter marker chain responses.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("chapter_path_markers_response");

    /**
     * Creates a chapter marker response before request correlation is assigned.
     *
     * @param status  status code describing the result
     * @param markers ordered marker rows and optional chain-break separator
     */
    public ChapterPathMarkersResponsePacket(ChapterMarkersStatus status, List<ChapterMarkerEntry> markers) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, status, markers);
    }

    /**
     * Creates a response with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public ChapterPathMarkersResponsePacket withRequestId(UUID requestId) {
        return new ChapterPathMarkersResponsePacket(requestId, status, markers);
    }

    /**
     * Serializes this response for server-to-client transmission.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeEnum(status);
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
    public static ChapterPathMarkersResponsePacket read(FriendlyByteBuf buf) {
        UUID requestId = buf.readUUID();
        ChapterMarkersStatus status = buf.readEnum(ChapterMarkersStatus.class);
        int markerCount = buf.readInt();
        List<ChapterMarkerEntry> markers = new ArrayList<>(markerCount);
        for (int index = 0; index < markerCount; index++) {
            markers.add(readMarker(buf));
        }
        return new ChapterPathMarkersResponsePacket(requestId, status, markers);
    }

    /**
     * Writes one chapter marker row to a network buffer.
     *
     * @param buf    destination packet buffer
     * @param marker marker row to write
     */
    private static void writeMarker(FriendlyByteBuf buf, ChapterMarkerEntry marker) {
        buf.writeLong(marker.packedPos());
        buf.writeInt(marker.timeOfDay());
        buf.writeInt(marker.weather());
        buf.writeUtf(marker.proximityMessage());
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
    private static ChapterMarkerEntry readMarker(FriendlyByteBuf buf) {
        return new ChapterMarkerEntry(
                buf.readLong(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(ChapterMarkerEntry.MAX_PROXIMITY_MESSAGE_LENGTH),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
