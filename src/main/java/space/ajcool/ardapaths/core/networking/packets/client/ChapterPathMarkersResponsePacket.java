package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
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
public record ChapterPathMarkersResponsePacket(UUID requestId, ChapterMarkersStatus status,
                                               List<ChapterMarkerEntry> markers) implements IRespondablePacket<ChapterPathMarkersResponsePacket> {

    /**
     * Network channel used for chapter marker chain responses.
     */
    public static final Identifier CHANNEL = ModConstants.modId("chapter_path_markers_response");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<ChapterPathMarkersResponsePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

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
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<ChapterPathMarkersResponsePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeEnum(status);
        buf.writeInt(markers.size());
        for (ChapterMarkerEntry marker : markers) {
            writeMarker(buf, marker);
        }
        return buf;
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
}
