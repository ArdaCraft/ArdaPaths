package space.ajcool.ardapaths.core.networking.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;
import space.ajcool.ardapaths.core.data.ChapterMarkerEntry;
import space.ajcool.ardapaths.core.data.ChapterMarkersStatus;
import space.ajcool.ardapaths.core.data.PathMarkerRemoteDataStatus;
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;
import space.ajcool.ardapaths.core.networking.packets.client.ArdaPathsPermissionCheckResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.ChapterPathMarkersResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerBulkClearResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerTimeSpreadResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.PathDataResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.PathMarkerRemoteDataResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterDeletePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPathMarkersPacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPlayerTeleportPacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartRemovePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.MarkerActionTriggerPacket;
import space.ajcool.ardapaths.core.networking.packets.server.MarkerBulkClearPacket;
import space.ajcool.ardapaths.core.networking.packets.server.MarkerTimeSpreadPacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerLinksUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerRemoteDataPacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Round-trip tests for packet build/read wire contracts.
 */
class PacketRoundTripTest {
    /**
     * Representative request id used to verify respondable packet correlation.
     */
    private static final UUID REQUEST_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    /**
     * Verifies every current packet record can be serialized and decoded back to equivalent data.
     */
    @Test
    void packetsRoundTripThroughPacketByteBuf() {
        CompoundTag markerNbt = markerNbt();

        assertRoundTrip(new ArdaPathsPermissionCheckResponsePacket(true), ArdaPathsPermissionCheckResponsePacket::read);
        assertRoundTrip(new ChapterPathMarkersResponsePacket(
                ChapterMarkersStatus.OK_WITH_BREAK,
                List.of(
                        new ChapterMarkerEntry(123L, 6000, 2, "At the gate", true, false, false),
                        ChapterMarkerEntry.breakEntry()
                )
        ), ChapterPathMarkersResponsePacket::read);
        assertRoundTrip(new MarkerBulkClearResponsePacket(TimeSpreadStatus.OK, 4), MarkerBulkClearResponsePacket::read);
        assertRoundTrip(new MarkerTimeSpreadResponsePacket(TimeSpreadStatus.CHAIN_BROKEN, 2, new BlockPos(7, 8, 9)), MarkerTimeSpreadResponsePacket::read);
        assertRoundTrip(new MarkerTimeSpreadResponsePacket(TimeSpreadStatus.OK, 3, null), MarkerTimeSpreadResponsePacket::read);
        assertRoundTrip(new PathDataResponsePacket("{\"paths\":[]}"), PathDataResponsePacket::read);
        assertRoundTrip(new PathMarkerRemoteDataResponsePacket(PathMarkerRemoteDataStatus.OK, 99L, markerNbt), PathMarkerRemoteDataResponsePacket::read);
        assertRoundTrip(new EmptyPacket(), EmptyPacket::read);

        assertRoundTrip(new ChapterDeletePacket("frodo", "shire"), ChapterDeletePacket::read);
        assertRoundTrip(new ChapterPathMarkersPacket("frodo", "shire", 123L), ChapterPathMarkersPacket::read);
        assertRoundTrip(new ChapterPlayerTeleportPacket("frodo", "shire"), ChapterPlayerTeleportPacket::read);
        assertRoundTrip(new ChapterStartRemovePacket("frodo", "shire", new BlockPos(1, 2, 3)), ChapterStartRemovePacket::read);
        assertRoundTrip(new ChapterStartUpdatePacket("frodo", "shire", new BlockPos(1, 2, 3)), ChapterStartUpdatePacket::read);
        assertRoundTrip(new ChapterUpdatePacket("frodo", "shire", "The Shire", "12 Forelithe", 1, "bag-end"), ChapterUpdatePacket::read);
        assertRoundTrip(new MarkerActionTriggerPacket(new BlockPos(-1, 70, 12), "frodo", "shire"), MarkerActionTriggerPacket::read);
        assertRoundTrip(new MarkerBulkClearPacket(List.of(1L, 2L, 3L), "frodo", "shire", true, false), MarkerBulkClearPacket::read);
        assertRoundTrip(new MarkerTimeSpreadPacket(1L, 2L, 1000, 13000, "frodo", "shire", false), MarkerTimeSpreadPacket::read);
        assertRoundTrip(new PathDataUpdatePacket("frodo", "Frodo's Path", 0x112233, 0x445566, 0x778899), PathDataUpdatePacket::read);
        assertRoundTrip(new PathMarkerLinksUpdatePacket(new BlockPos(10, 20, 30), markerNbt), PathMarkerLinksUpdatePacket::read);
        assertRoundTrip(new PathMarkerRemoteDataPacket(987654321L), PathMarkerRemoteDataPacket::read);
        assertRoundTrip(new PathMarkerUpdatePacket(new BlockPos(10, 20, 30), markerNbt), PathMarkerUpdatePacket::read);
        assertRoundTrip(new PlayerTeleportPacket(1.25D, 64.0D, -9.5D, new ResourceLocation("minecraft", "overworld")), PlayerTeleportPacket::read);
    }

    /**
     * Verifies every respondable packet preserves its request id as part of the payload.
     */
    @Test
    void respondablePacketsRoundTripRequestIds() {
        CompoundTag markerNbt = markerNbt();

        assertRespondableRoundTrip(new ArdaPathsPermissionCheckResponsePacket(true), ArdaPathsPermissionCheckResponsePacket::read);
        assertRespondableRoundTrip(new ChapterPathMarkersResponsePacket(ChapterMarkersStatus.OK, List.of()), ChapterPathMarkersResponsePacket::read);
        assertRespondableRoundTrip(new MarkerBulkClearResponsePacket(TimeSpreadStatus.OK, 1), MarkerBulkClearResponsePacket::read);
        assertRespondableRoundTrip(new MarkerTimeSpreadResponsePacket(TimeSpreadStatus.OK, 1, new BlockPos(1, 2, 3)), MarkerTimeSpreadResponsePacket::read);
        assertRespondableRoundTrip(new PathDataResponsePacket("{\"paths\":[]}"), PathDataResponsePacket::read);
        assertRespondableRoundTrip(new PathMarkerRemoteDataResponsePacket(PathMarkerRemoteDataStatus.OK, 4L, markerNbt), PathMarkerRemoteDataResponsePacket::read);
        assertRespondableRoundTrip(new EmptyPacket(), EmptyPacket::read);
        assertRespondableRoundTrip(new ChapterPathMarkersPacket("frodo", "shire", 123L), ChapterPathMarkersPacket::read);
        assertRespondableRoundTrip(new MarkerBulkClearPacket(List.of(1L, 2L), "frodo", "shire", true, false), MarkerBulkClearPacket::read);
        assertRespondableRoundTrip(new MarkerTimeSpreadPacket(1L, 2L, 1000, 12000, "frodo", "shire", false), MarkerTimeSpreadPacket::read);
        assertRespondableRoundTrip(new PathMarkerRemoteDataPacket(5L), PathMarkerRemoteDataPacket::read);
    }

    /**
     * Asserts one packet survives build/read with equal record data.
     *
     * @param packet packet instance to encode
     * @param reader static packet reader
     * @param <T> packet type
     */
    private static <T extends IPacket> void assertRoundTrip(T packet, Function<FriendlyByteBuf, T> reader) {
        assertEquals(packet, reader.apply(packet.build()));
    }

    /**
     * Asserts one respondable packet carries a non-default request id through build/read.
     *
     * @param packet packet instance to encode
     * @param reader static packet reader
     * @param <T> packet type
     */
    private static <T extends IRespondablePacket<T>> void assertRespondableRoundTrip(T packet, Function<FriendlyByteBuf, T> reader) {
        T correlatedPacket = packet.withRequestId(REQUEST_ID);
        T decodedPacket = reader.apply(correlatedPacket.build());
        assertEquals(correlatedPacket, decodedPacket);
        assertEquals(REQUEST_ID, decodedPacket.requestId());
    }

    /**
     * Creates representative nested marker data for NBT-carrying packets.
     *
     * @return marker NBT payload
     */
    private static CompoundTag markerNbt() {
        CompoundTag markerNbt = new CompoundTag();
        CompoundTag chapterNbt = new CompoundTag();
        chapterNbt.putString("proximity_message", "Look east");
        chapterNbt.putInt("activation_range", 8);
        markerNbt.put("default", chapterNbt);
        return markerNbt;
    }
}
